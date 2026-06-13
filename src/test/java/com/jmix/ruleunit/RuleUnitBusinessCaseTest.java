package com.jmix.ruleunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.AlgCPLiteral;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.testgen.business.BusinessCaseExpect;
import com.jmix.ruletrans.testgen.business.BusinessCaseGiven;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.RuleUnitDiagnostic;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class RuleUnitBusinessCaseTest {

    @AfterEach
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    public void testBusinessJsonParsesBusinessFieldsOnly() {
        BusinessRuleTestCaseSet caseSet = BusinessRuleTestCaseSet.fromJson("""
                ```json
                {
                  "ruleMethod": "assignTshirtQuantity",
                  "cases": [
                    {
                      "id": "ASSIGN-IF-001",
                      "businessFamily": "ASSIGNMENT",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testAssignment",
                      "given": {
                        "parameters": [
                          {"code": "color", "value": "red"},
                          {"code": "size", "value": "M"}
                        ]
                      },
                      "expect": {
                        "parts": [{"code": "tshirt", "quantity": 2}]
                      }
                    }
                  ]
                }
                ```
                """);

        assertEquals("assignTshirtQuantity", caseSet.ruleMethod());
        assertEquals(BusinessRuleFamily.ASSIGNMENT, caseSet.cases().get(0).businessFamily());
        assertEquals("tshirt", caseSet.cases().get(0).expect().parts().get(0).code());
        assertFalse(caseSet.toJson().contains("InferParasReq"));
    }

    @Test
    public void testAssignmentCaseUsesBusinessParameterInput() {
        DefaultRuleUnitTestExecutorService service = startService(AssignmentConstraint.class);

        RuleUnitTestReport report = service.executeCase(assignmentCase());

        assertTrue(report.passed(), report.failures().toString());
        assertEquals(2, report.actual().parts().stream()
                .filter(part -> "tshirt".equals(part.code()))
                .findFirst()
                .orElseThrow()
                .quantity());
    }

    @Test
    public void testCompatibilityCaseSupportsParameterValidationAndDiagnostics() {
        DefaultRuleUnitTestExecutorService service = startService(CompatibilityConstraint.class);

        RuleUnitTestReport report = service.executeCase(invalidCompatibilityCase());

        assertTrue(report.passed(), report.failures().toString());
        assertFalse(report.actual().compatible());
        assertTrue(report.actual().diagnostics().stream()
                .anyMatch(diagnostic -> "aRequiresB1".equals(diagnostic.ruleCode())));
    }

    @Test
    public void testPriorityCaseStrictlyComparesRankedSolutions() {
        DefaultRuleUnitTestExecutorService service = startService(PriorityConstraint.class);

        RuleUnitTestReport report = service.executeCase(priorityCase());

        assertTrue(report.passed(), report.failures().toString());
        assertEquals("ssd", report.actual().solutions().get(0).parts().stream()
                .filter(part -> "ssd".equals(part.code()))
                .findFirst()
                .orElseThrow()
                .code());
    }

    private BusinessRuleTestCase assignmentCase() {
        return new BusinessRuleTestCase(
                "ASSIGN-IF-001",
                "红色 M 码 T 恤数量为 2",
                BusinessRuleFamily.ASSIGNMENT,
                "参数 if-else 推导部件数量",
                TestEnvironment.CONSTRAINT,
                "testAssignment",
                new BusinessCaseGiven(
                        List.of(
                                new RuleUnitParameter("color", "red"),
                                new RuleUnitParameter("size", "M")),
                        List.of(),
                        List.of()),
                new BusinessCaseExpect(
                        null,
                        List.of(),
                        List.of(new RuleUnitPart("tshirt", 2)),
                        List.of(),
                        List.of()),
                null);
    }

    private BusinessRuleTestCase invalidCompatibilityCase() {
        return new BusinessRuleTestCase(
                "COMP-PARA-REQ-002",
                "A=a1 要求 B=b1",
                BusinessRuleFamily.COMPATIBILITY,
                "参数 Requires",
                TestEnvironment.CONSTRAINT,
                "testCompatibility",
                new BusinessCaseGiven(
                        List.of(
                                new RuleUnitParameter("A", "a1"),
                                new RuleUnitParameter("B", "b2")),
                        List.of(),
                        List.of()),
                new BusinessCaseExpect(
                        false,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new RuleUnitDiagnostic("aRequiresB1", null))),
                null);
    }

    private BusinessRuleTestCase priorityCase() {
        return new BusinessRuleTestCase(
                "PRI-DRIVE-001",
                "固态硬盘优先匹配高速率容量",
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
                                1,
                                Map.of("Speed", "5400")))),
                new BusinessCaseExpect(
                        null,
                        List.of(),
                        List.of(),
                        List.of(new RuleUnitSolution(
                                1,
                                List.of(new RuleUnitPart("ssd", 1)),
                                List.of())),
                        List.of()),
                null);
    }

    private DefaultRuleUnitTestExecutorService startService(Class<?> algClass) {
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        assertEquals(Result.SUCCESS, ModuleConstraintExecutor.INST.init(config).getCode());
        Module module = ModuleGenneratorByAnno.buildModule(algClass);
        assertEquals(Result.SUCCESS, ModuleConstraintExecutor.INST.addModule(module.getId(), module).getCode());
        return new DefaultRuleUnitTestExecutorService(module);
    }

    @ModuleAnno(id = 901301L)
    public static class AssignmentConstraint extends ModuleAlgBase {

        @ParaAnno(options = {"red", "blue"})
        private ParaVar color;

        @ParaAnno(options = {"M", "L"})
        private ParaVar size;

        @PartAnno
        private PartVar tshirt;

        @CodeRuleAnno(normalNaturalCode = "红色 M 码 T 恤数量为 2")
        private void assignTshirtQuantity() {
            AlgCPBoolVar isRed = color.option("red").selectedVar();
            AlgCPBoolVar isM = size.option("M").selectedVar();
            AlgCPBoolVar matched = model().newBoolVar("color_red_size_m");
            model().addBoolAnd(new AlgCPLiteral[] {isRed, isM}).onlyEnforceIf(matched);
            model().addBoolOr(new AlgCPLiteral[] {isRed.not(), isM.not()}).onlyEnforceIf(matched.not());
            model().addEquality(tshirt.quantityVar(), 2).onlyEnforceIf(matched);
            model().addEquality(tshirt.quantityVar(), 0).onlyEnforceIf(matched.not());
        }
    }

    @ModuleAnno(id = 901302L)
    public static class CompatibilityConstraint extends ModuleAlgBase {

        @ParaAnno(code = "A", options = {"a1", "a2"})
        private ParaVar A;

        @ParaAnno(code = "B", options = {"b1", "b2"})
        private ParaVar B;

        @CodeRuleAnno(normalNaturalCode = "if A.valueVar() == a1 then B.valueVar() == b1",
                leftProObjsStr = "A:value", rightProObjsStr = "B:value")
        private void aRequiresB1() {
            addCompatibleConstraintRequires(
                    "aRequiresB1",
                    A,
                    List.of("a1"),
                    B,
                    List.of("b1"));
        }
    }

    @ModuleAnno(id = 901303L)
    public static class PriorityConstraint extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Speed_5400:5400", "Speed_7200:7200"})
        private PartCategoryVar drive;

        @PartAnno(code = "sd", fatherCode = "drive")
        private PartCategoryVar sd;

        @PartAnno(code = "md", fatherCode = "drive")
        private PartCategoryVar md;

        @PartAnno(fatherCode = "sd", attrs = {"5400"})
        private PartVar ssd;

        @PartAnno(fatherCode = "md", attrs = {"5400"})
        private PartVar hdd;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Quantity;

        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量",
                strategy = PriorityStrategy.MIN)
        private void preferSsd() {
            AlgCPLinearExpr totalQty = model().sum4Quantity("");
            model().addGreaterOrEqual(totalQty, Sum_Quantity.inputValue());
            PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("preferSsdObjective");
            objectiveExpr.addTerm(ssd.quantityVar(), -100);
            objectiveExpr.addTerm(hdd.quantityVar(), 1);
            model().setObjectExpr(objectiveExpr);
            updatePriorityObjectFuntion("preferSsd", objectiveExpr);
        }
    }
}
