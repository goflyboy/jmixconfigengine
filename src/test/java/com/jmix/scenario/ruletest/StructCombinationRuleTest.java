package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.CodependantRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.StructCompareOperator;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.CombinationStructRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PairStructRuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.TripleStructRuleAnno;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC-0007 structured combination rule tests.
 */
public class StructCombinationRuleTest extends ModuleScenarioTestBase {

    public StructCombinationRuleTest() {
        super(StructCombinationConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
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

        @PairStructRuleAnno(
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Values = {"4"},
                relationType = BusinessRelationType.INCOMPATIBLE,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Values = {"7000"})
        public void pairCpu4Drive7000() {
        }

        @PairStructRuleAnno(
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Values = {"4"},
                relationType = BusinessRelationType.INCOMPATIBLE,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Operator = StructCompareOperator.LIKE,
                expr2Values = {"71%"})
        public void pairCpu4Drive54Like() {
        }

        @CombinationStructRuleAnno(
                code = "cpu_drive_white",
                arity = 2,
                dimensionCategoryCodes = {"cpu", "drive"},
                combinationType = PartCombinationType.WHITE,
                subRuleCodes = {"cpu_drive_white_001", "cpu_drive_white_002", "cpu_drive_white_003"})
        public void cpuDriveWhite() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_001",
                parentRuleCode = "cpu_drive_white",
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Values = {"4"},
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Values = {"5400"})
        public void cpuDriveWhite001() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_002",
                parentRuleCode = "cpu_drive_white",
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Values = {"8"},
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Operator = StructCompareOperator.IN,
                expr2Values = {"5400", "7200"})
        public void cpuDriveWhite002() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_003",
                parentRuleCode = "cpu_drive_white",
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Operator = StructCompareOperator.GT,
                expr1Values = {"8"},
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Values = {"7200"})
        public void cpuDriveWhite003() {
        }

        @CombinationStructRuleAnno(
                code = "cpu_drive_monitor_white",
                arity = 3,
                dimensionCategoryCodes = {"cpu", "drive", "monitor"},
                combinationType = PartCombinationType.WHITE,
                subRuleCodes = {"cpu_drive_monitor_white_001"})
        public void cpuDriveMonitorWhite() {
        }

        @TripleStructRuleAnno(
                code = "cpu_drive_monitor_white_001",
                parentRuleCode = "cpu_drive_monitor_white",
                expr1ObjectCode = "cpu",
                expr1AttrCode = "CoreNum",
                expr1Values = {"8"},
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2ObjectCode = "drive",
                expr2AttrCode = "Speed",
                expr2Values = {"7200"},
                expr3ObjectCode = "monitor",
                expr3AttrCode = "Resolution",
                expr3Values = {"4K"})
        public void cpuDriveMonitorWhite001() {
        }
    }

    @Test
    public void testAnnotationGeneratedStructRules() {
        Rule parent = getModule().getRule("cpu_drive_white").orElseThrow();
        assertNotNull(parent.getExeSchema());
        CodependantRuleSchema schema = (CodependantRuleSchema) parent.getExeSchema();
        assertEquals(2, schema.getArity());
        assertEquals(4, schema.getCombinations().size());
    }

    @Test
    public void testPairStructRule_Incompatible() {
        assertNoSolution("cpu4", "drive7000");
        assertPass("cpu4", "drive5400");
        assertPass("cpu4", "drive7200");
    }

    @Test
    public void testCombinationWhiteList_Pair_Generation() {
        assertPass("cpu4", "drive5400");
        assertNoSolution("cpu4", "drive7200");
        assertPass("cpu8", "drive5400");
        assertPass("cpu8", "drive7200");
        assertNoSolution("cpu12", "drive5400");
        assertPass("cpu12", "drive7200");
    }

    @Test
    public void testCombinationWhiteList_Triple_Generation() {
        assertPass("cpu8", "drive7200", "monitor4k");
        assertNoSolution("cpu8", "drive5400", "monitor4k");
    }

    @Test
    public void testValidateCombinationWhiteList_Pair() {
        assertTrue(ModuleConstraintExecutor.INST.validate(moduleInst("cpu4", "drive5400")));
        assertFalse(ModuleConstraintExecutor.INST.validate(moduleInst("cpu4", "drive7200")));

        Result<ModuleValidateResp> valid = validate(moduleInst("cpu4", "drive5400"));
        assertEquals(Result.SUCCESS, valid.getCode());
        assertTrue(valid.getData().isValid());

        Result<ModuleValidateResp> invalid = validate(moduleInst("cpu4", "drive7200"));
        assertEquals(Result.SUCCESS, invalid.getCode());
        assertFalse(invalid.getData().isValid());
        assertTrue(invalid.getData().getViolatedRuleCodes().contains("cpu_drive_white"));
    }

    @Test
    public void testValidateCombination_WithQuantityInput() {
        Result<ModuleValidateResp> result = validate(moduleInst(partInst("cpu4", 1), partInst("drive5400", 2)));
        assertEquals(Result.SUCCESS, result.getCode());
        assertTrue(result.getData().isValid());
    }

    @Test
    public void testCombinationRule_IntersectWithRuntimeFilter() {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(false);

        PartConstraintReq cpuReq = new PartConstraintReq();
        cpuReq.setPartCategoryCode("cpu");
        cpuReq.setAttrType(AttrParaType.Sum);
        cpuReq.setAttrCode("Quantity");
        cpuReq.setAttrComparator("==");
        cpuReq.setAttrValue("1");
        cpuReq.setAttrWhereCondition("CoreNum=4");

        PartConstraintReq driveReq = new PartConstraintReq();
        driveReq.setPartCategoryCode("drive");
        driveReq.setAttrType(AttrParaType.Sum);
        driveReq.setAttrCode("Quantity");
        driveReq.setAttrComparator("==");
        driveReq.setAttrValue("1");
        driveReq.setAttrWhereCondition("Speed=7200");
        req.setPartConstraintReqs(List.of(cpuReq, driveReq));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
        assertTrue(result.getCode() == Result.NO_SOLUTION
                || (result.getCode() == Result.SUCCESS && (result.getData() == null || result.getData().isEmpty())));
    }

    @Test
    public void testChildRule_NotExecutedIndependently() {
        assertPass("cpu4", "drive5400");
        assertNoSolution("cpu4", "drive7200");
    }

    private Result<ModuleValidateResp> validate(ModuleInst moduleInst) {
        ModuleValidateReq req = new ModuleValidateReq();
        req.setModuleId(getModule().getId());
        req.setModuleInst(moduleInst);
        return ModuleConstraintExecutor.INST.validate(req);
    }

    private ModuleInst moduleInst(String... selectedPartCodes) {
        return moduleInst(toPrePartInsts(selectedPartCodes).toArray(PartInst[]::new));
    }

    private ModuleInst moduleInst(PartInst... partInsts) {
        ModuleInst inst = new ModuleInst();
        inst.setId(getModule().getId());
        for (PartInst partInst : partInsts) {
            inst.addPartInst(partInst);
        }
        return inst;
    }

    private PartInst partInst(String code, int quantity) {
        PartInst partInst = new PartInst(code, quantity);
        partInst.setSelected(quantity > 0);
        return partInst;
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

    private List<PartInst> toPrePartInsts(String... selectedPartCodes) {
        List<String> pairs = new ArrayList<>();
        for (String selectedPartCode : selectedPartCodes) {
            pairs.add(selectedPartCode);
            pairs.add("1");
        }
        return toPreParts(pairs.toArray(String[]::new));
    }
}
