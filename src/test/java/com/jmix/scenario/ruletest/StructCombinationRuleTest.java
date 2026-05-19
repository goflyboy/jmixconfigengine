package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.CombinationStructRuleSchema;
import com.jmix.executor.bmodel.logic.PairStructRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.logic.StructCompareOperator;
import com.jmix.executor.bmodel.logic.StructExprSchema;
import com.jmix.executor.bmodel.logic.TripleStructRuleSchema;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RFC-0007 structured combination rule tests.
 */
public class StructCombinationRuleTest extends ModuleScenarioTestBase {

    private final List<Rule> installedRules = new ArrayList<>();

    public StructCombinationRuleTest() {
        super(StructCombinationConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Override
    protected Module buildModule(Class<?> moduleAlgClazz) {
        Module module = super.buildModule(moduleAlgClazz);
        for (Rule rule : installedRules) {
            module.addRule(rule);
        }
        module.init();
        return module;
    }

    @ModuleAnno(id = 7007L)
    @AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0007-test")
    public static class StructCombinationConstraint extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8", "Core_12:12"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(fatherCode = "cpu", attrs = {"12"})
        private PartVar cpu12;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200", "Speed_7000:7000"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;

        @PartAnno(fatherCode = "drive", attrs = {"7000"})
        private PartVar drive7000;

        @PartAnno(code = "monitor")
        @DAttrAnno1(code = "Resolution", options = {"FHD:FHD", "UHD:4K"})
        private PartCategoryVar monitor;

        @PartAnno(fatherCode = "monitor", attrs = {"FHD"})
        private PartVar monitorFhd;

        @PartAnno(fatherCode = "monitor", attrs = {"4K"})
        private PartVar monitor4k;
    }

    @Test
    public void testPairStructRule_Incompatible() {
        installedRules.add(pairRule("pair_cpu4_drive5400",
                expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                BusinessRelationType.INCOMPATIBLE,
                expr("drive", "Speed", StructCompareOperator.EQ, "5400")));
        reinitWithRules();

        assertNoSolution("cpu4", "drive5400");
        assertPass("cpu4", "drive7200");
    }

    @Test
    public void testCombinationWhiteList_Pair() {
        installedRules.addAll(cpuDriveWhiteListRules("cpu_drive_white"));
        reinitWithRules();

        assertPass("cpu4", "drive5400");
        assertNoSolution("cpu4", "drive7200");
        assertPass("cpu8", "drive5400");
        assertPass("cpu8", "drive7200");
        assertNoSolution("cpu12", "drive5400");
        assertPass("cpu12", "drive7200");
    }

    @Test
    public void testCombinationBlackList_Pair() {
        installedRules.addAll(combinationRules("cpu_drive_black", 2, PartCombinationType.BLACK,
                List.of("cpu", "drive"),
                List.of(pairRule("cpu_drive_black_001",
                        expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                        BusinessRelationType.INCOMPATIBLE,
                        expr("drive", "Speed", StructCompareOperator.EQ, "7000")))));
        reinitWithRules();

        assertNoSolution("cpu4", "drive7000");
        assertPass("cpu4", "drive5400");
        assertPass("cpu8", "drive7000");
    }

    @Test
    public void testPairStructRule_LikeOperator() {
        installedRules.add(pairRule("pair_cpu4_drive54_like",
                expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                BusinessRelationType.INCOMPATIBLE,
                expr("drive", "Speed", StructCompareOperator.LIKE, "54%")));
        reinitWithRules();

        assertNoSolution("cpu4", "drive5400");
        assertPass("cpu4", "drive7200");
    }

    @Test
    public void testCombinationWhiteList_Triple() {
        installedRules.addAll(combinationRules("cpu_drive_monitor_white", 3, PartCombinationType.WHITE,
                List.of("cpu", "drive", "monitor"),
                List.of(tripleRule("cpu_drive_monitor_white_001",
                        expr("cpu", "CoreNum", StructCompareOperator.EQ, "8"),
                        BusinessRelationType.CO_DEPENDENT,
                        expr("drive", "Speed", StructCompareOperator.EQ, "7200"),
                        expr("monitor", "Resolution", StructCompareOperator.EQ, "4K")))));
        reinitWithRules();

        assertPass("cpu8", "drive7200", "monitor4k");
        assertNoSolution("cpu8", "drive5400", "monitor4k");
    }

    @Test
    public void testCombinationRule_IntersectWithRuntimeFilter() {
        installedRules.addAll(combinationRules("cpu_drive_filter_white", 2, PartCombinationType.WHITE,
                List.of("cpu", "drive"),
                List.of(pairRule("cpu_drive_filter_white_001",
                        expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                        BusinessRelationType.CO_DEPENDENT,
                        expr("drive", "Speed", StructCompareOperator.EQ, "5400")))));
        reinitWithRules();

        assertNoSolutionWithDriveFilter("cpu4", "Speed=7200");
    }

    @Test
    public void testCombinationRule_RuntimeExpansionNotCachedAcrossRequests() {
        installedRules.addAll(combinationRules("cpu_drive_cache_white", 2, PartCombinationType.WHITE,
                List.of("cpu", "drive"),
                List.of(pairRule("cpu_drive_cache_white_001",
                        expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                        BusinessRelationType.CO_DEPENDENT,
                        expr("drive", "Speed", StructCompareOperator.EQ, "5400")))));
        reinitWithRules();

        assertNoSolutionWithDriveFilter("cpu4", "Speed=7200");
        assertPass("cpu4", "drive5400");
    }

    @Test
    public void testChildRule_NotExecutedIndependently() {
        Rule child = pairRule("cpu_drive_child_001",
                expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                BusinessRelationType.CO_DEPENDENT,
                expr("drive", "Speed", StructCompareOperator.EQ, "5400"));
        installedRules.addAll(combinationRules("cpu_drive_child_parent", 2, PartCombinationType.WHITE,
                List.of("cpu", "drive"), List.of(child)));
        reinitWithRules();

        assertPass("cpu4", "drive5400");
        assertNoSolution("cpu4", "drive7200");
    }

    private void reinitWithRules() {
        tearDown();
        setUp();
    }

    private List<Rule> cpuDriveWhiteListRules(String code) {
        return combinationRules(code, 2, PartCombinationType.WHITE, List.of("cpu", "drive"),
                List.of(
                        pairRule(code + "_001",
                                expr("cpu", "CoreNum", StructCompareOperator.EQ, "4"),
                                BusinessRelationType.CO_DEPENDENT,
                                expr("drive", "Speed", StructCompareOperator.EQ, "5400")),
                        pairRule(code + "_002",
                                expr("cpu", "CoreNum", StructCompareOperator.EQ, "8"),
                                BusinessRelationType.CO_DEPENDENT,
                                expr("drive", "Speed", StructCompareOperator.IN, "5400", "7200")),
                        pairRule(code + "_003",
                                expr("cpu", "CoreNum", StructCompareOperator.GT, "8"),
                                BusinessRelationType.CO_DEPENDENT,
                                expr("drive", "Speed", StructCompareOperator.EQ, "7200"))));
    }

    private List<Rule> combinationRules(String parentCode, int arity, PartCombinationType type,
            List<String> dimensions, List<Rule> children) {
        CombinationStructRuleSchema schema = new CombinationStructRuleSchema();
        schema.setArity(arity);
        schema.setCombinationType(type);
        schema.setDimensionCategoryCodes(dimensions);
        schema.setSubRuleCodes(children.stream().map(Rule::getCode).toList());

        Rule parent = rule(parentCode, schema, RuleTypeConstants.COMBINATION_STRUCT_RULE_FULL_NAME);
        for (Rule child : children) {
            child.setParentRuleCode(parentCode);
        }

        List<Rule> result = new ArrayList<>();
        result.add(parent);
        result.addAll(children);
        return result;
    }

    private Rule pairRule(String code, StructExprSchema expr1, BusinessRelationType relationType,
            StructExprSchema expr2) {
        PairStructRuleSchema schema = new PairStructRuleSchema();
        schema.setExpr1(expr1);
        schema.setRelationType(relationType);
        schema.setExpr2(expr2);
        return rule(code, schema, RuleTypeConstants.PAIR_STRUCT_RULE_FULL_NAME);
    }

    private Rule tripleRule(String code, StructExprSchema expr1, BusinessRelationType relationType,
            StructExprSchema expr2, StructExprSchema expr3) {
        TripleStructRuleSchema schema = new TripleStructRuleSchema();
        schema.setExpr1(expr1);
        schema.setRelationType(relationType);
        schema.setExpr2(expr2);
        schema.setExpr3(expr3);
        return rule(code, schema, RuleTypeConstants.TRIPLE_STRUCT_RULE_FULL_NAME);
    }

    private Rule rule(String code, com.jmix.executor.bmodel.logic.RuleSchema schema, String typeFullName) {
        Rule rule = new Rule();
        rule.setCode(code);
        rule.setName(code);
        rule.setRawCode(schema);
        rule.setRuleSchemaTypeFullName(typeFullName);
        rule.setCalcStage(CalcStage.MID);
        return rule;
    }

    private StructExprSchema expr(String objectCode, String attrCode, StructCompareOperator operator,
            String... values) {
        StructExprSchema expr = new StructExprSchema();
        expr.setObjectCode(objectCode);
        expr.setAttrCode(attrCode);
        expr.setOperator(operator);
        expr.setValues(Arrays.asList(values));
        return expr;
    }

    private void assertPass(String... selectedPartCodes) {
        runWithSelectedParts(selectedPartCodes);
        resultAssert().assertSuccess();
    }

    private void assertNoSolution(String... selectedPartCodes) {
        runWithSelectedParts(selectedPartCodes);
        resultAssert().assertSolutionSizeEqual(0);
    }

    private void runWithSelectedParts(String... selectedPartCodes) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(false);
        req.setPrePartInsts(toPrePartInsts(selectedPartCodes));
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
    }

    private void assertNoSolutionWithDriveFilter(String cpuPartCode, String driveFilter) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(false);
        req.setPrePartInsts(toPrePartInsts(cpuPartCode));

        PartConstraintReq driveReq = new PartConstraintReq();
        driveReq.setPartCategoryCode("drive");
        driveReq.setAttrType(AttrParaType.Sum);
        driveReq.setAttrCode("Quantity");
        driveReq.setAttrComparator("==");
        driveReq.setAttrValue("1");
        driveReq.setAttrWhereCondition(driveFilter);
        req.setPartConstraintReqs(List.of(driveReq));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
        assertTrue(result.getCode() == Result.NO_SOLUTION
                || (result.getCode() == Result.SUCCESS && (result.getData() == null || result.getData().isEmpty())));
    }

    private List<com.jmix.executor.cmodel.PartInst> toPrePartInsts(String... selectedPartCodes) {
        List<String> pairs = new ArrayList<>();
        for (String selectedPartCode : selectedPartCodes) {
            pairs.add(selectedPartCode);
            pairs.add("1");
        }
        return toPreParts(pairs.toArray(String[]::new));
    }
}
