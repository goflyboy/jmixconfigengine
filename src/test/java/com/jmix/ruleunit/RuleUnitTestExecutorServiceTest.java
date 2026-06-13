package com.jmix.ruleunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.ruletrans.testgen.business.BusinessCaseExpect;
import com.jmix.ruletrans.testgen.business.BusinessCaseGiven;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.CompatiableRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RuleUnitTestExecutorServiceTest {

    private DefaultRuleUnitTestExecutorService service;

    @BeforeEach
    public void setUp() {
        Module module = ModuleGenneratorByAnno.build(RuleUnitConstraint.class, "target/ruleunit-test-resources");
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        ModuleConstraintExecutor.INST.init(config);
        ModuleConstraintExecutor.INST.addModule(module.getId(), module);
        service = new DefaultRuleUnitTestExecutorService(module);
    }

    @AfterEach
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    public void testAssignmentComparesExpectedPartQuantity() {
        RuleUnitTestReport report = service.executeCase(new BusinessRuleTestCase(
                "ASSIGN-IF-001",
                "红色 M 码 T 恤数量为 2",
                BusinessRuleFamily.ASSIGNMENT,
                "参数 if-else 推导部件数量",
                TestEnvironment.CONSTRAINT,
                "testAssignment",
                new BusinessCaseGiven(
                        List.of(new RuleUnitParameter("color", "red"),
                                new RuleUnitParameter("size", "M")),
                        List.of(),
                        List.of()),
                new BusinessCaseExpect(
                        null,
                        List.of(),
                        List.of(new RuleUnitPart("tshirt", 2)),
                        List.of(),
                        List.of()),
                null));

        assertTrue(report.passed(), report.failures().toString());
    }

    @Test
    public void testCompatibilitySupportsParameterCombinationValidation() {
        RuleUnitTestReport report = service.executeCaseFile(
                "src/test/resources/rule-unit-cases/ruleUnitTest/compatibility-parameter.json");

        assertTrue(report.passed(), report.failures().toString());
        assertFalse(report.actual().compatible());
    }

    @Test
    public void testPriorityComparesRankedSolutions() {
        RuleUnitTestReport report = service.executeCase(new BusinessRuleTestCase(
                "PRI-DRIVE-001",
                "固态硬盘优先匹配高速率数量",
                BusinessRuleFamily.PRIORITY,
                "优先级目标函数",
                TestEnvironment.CONSTRAINT,
                "testPriority",
                new BusinessCaseGiven(
                        List.of(),
                        List.of(),
                        List.of(new RuleUnitPartCategory(
                                "drive",
                                "Sum_Quantity",
                                "==",
                                2,
                                Map.of("Speed", "5400")))),
                new BusinessCaseExpect(
                        null,
                        List.of(),
                        List.of(),
                        List.of(new RuleUnitSolution(
                                1,
                                List.of(new RuleUnitPart("ssd", 2)),
                                List.of())),
                        List.of()),
                null));

        assertTrue(report.passed(), report.failures().toString());
    }

    @Test
    public void testPostAssignmentComparesWrittenParameter() {
        RuleUnitTestReport report = service.executeCase(new BusinessRuleTestCase(
                "POST-ASSIGN-001",
                "POST 后置计算写回部件数量",
                BusinessRuleFamily.ASSIGNMENT,
                "POST 后置计算",
                TestEnvironment.NON_CONSTRAINT,
                "testPostAssignment",
                new BusinessCaseGiven(
                        List.of(),
                        List.of(new RuleUnitPart("ssd", 2)),
                        List.of()),
                new BusinessCaseExpect(
                        null,
                        List.of(new RuleUnitParameter("totalDriveQuantity", "2")),
                        List.of(),
                        List.of(),
                        List.of()),
                null));

        assertTrue(report.passed(), report.failures().toString());
    }

    @Test
    public void testBusinessCaseSetParsesSingleCaseObject() {
        BusinessRuleTestCaseSet caseSet = BusinessRuleTestCaseSet.fromJson("""
                {
                  "id": "COMP-PARA-OK",
                  "businessFamily": "COMPATIBILITY",
                  "environment": "CONSTRAINT",
                  "serviceMethod": "testCompatibility",
                  "given": {"parameters": [{"code": "a", "value": "a1"}, {"code": "b", "value": "b2"}]},
                  "expect": {"compatible": true}
                }
                """);

        RuleUnitTestCaseSetReport report = service.executeCaseSet(caseSet);

        assertTrue(report.passed(), report.caseReports().toString());
    }

    @Test
    public void testBusinessCaseSetWritesJsonFile() {
        BusinessRuleTestCaseSet.empty().writeTo(Path.of("target/ruleunit-test-empty.json"));
        assertTrue(BusinessRuleTestCaseSet.fromFile(Path.of("target/ruleunit-test-empty.json")).isEmpty());
    }

    @ModuleAnno(id = 13001L)
    public static class RuleUnitConstraint extends ModuleAlgBase {

        @ParaAnno(options = {"red", "blue"})
        private ParaVar color;

        @ParaAnno(options = {"M", "L"})
        private ParaVar size;

        @PartAnno
        private PartVar tshirt;

        @ParaAnno(options = {"a1", "a2"})
        private ParaVar a;

        @ParaAnno(options = {"b1", "b2"})
        private ParaVar b;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar ssd;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar hdd;

        @ParaAnno(code = "totalDriveQuantity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar totalDriveQuantity;

        @CodeRuleAnno(normalNaturalCode = "红色 M 码 T 恤数量为 2")
        private void assignTshirtQuantity() {
            AlgCPBoolVar isRedAndM = model().newBoolVar("red_and_m");
            model().addBoolAnd(new AlgCPLiteral[] {
                    color.option("red").selectedVar(),
                    size.option("M").selectedVar()
            }).onlyEnforceIf(isRedAndM);
            model().addBoolOr(new AlgCPLiteral[] {
                    color.option("red").selectedVar().not(),
                    size.option("M").selectedVar().not()
            }).onlyEnforceIf(isRedAndM.not());
            model().addEquality(tshirt.quantityVar(), 2).onlyEnforceIf(isRedAndM);
            model().addEquality(tshirt.quantityVar(), 0).onlyEnforceIf(isRedAndM.not());
        }

        @CompatiableRuleAnno(normalNaturalCode = "A=a1 与 B=b1 不兼容")
        private void incompatibleA1B1() {
            addCompatibleConstraintInCompatible("incompatibleA1B1", a, List.of("a1"), b, List.of("b1"));
        }

        @PriorityRuleAnno(fatherCode = "drive", strategy = PriorityStrategy.MIN,
                normalNaturalCode = "固态硬盘优先匹配高速率数量")
        private void preferSsd() {
            PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("preferSsd");
            objectiveExpr.addTerm(ssd.quantityVar(), -100);
            objectiveExpr.addTerm(hdd.quantityVar(), 1);
            model().setObjectExpr(objectiveExpr);
            updatePriorityObjectFuntion("preferSsd", objectiveExpr);
        }

        @CodeRuleAnno(calcStage = CalcStage.POST, normalNaturalCode = "统计硬盘数量")
        private void writeDriveQuantity() {
            parameter("totalDriveQuantity").setValue(String.valueOf(partCategory("drive").sumQuantity()));
        }
    }
}
