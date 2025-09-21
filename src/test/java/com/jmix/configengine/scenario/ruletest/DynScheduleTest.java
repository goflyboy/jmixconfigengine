package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.jmix.configengine.scenario.base.CodeRuleAnno;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import com.jmix.executor.artifact.ConstraintAlgImpl;
import com.jmix.executor.artifact.ParaVar;
import com.jmix.executor.artifact.PartVar;
import com.jmix.executor.model.ParaType;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("checkstyle:all")
public class DynScheduleTest extends ModuleScenarioTestBase {

    // ---------------start----------------------------------------
    @ModuleAnno(id = 123L)
    @SuppressWarnings("checkstyle:all")
    static public class DynScheduleConstraint extends ConstraintAlgImpl {
        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "4")
        private ParaVar P0;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "5")
        private ParaVar P11;

        @PartAnno(maxQuantity = 3)
        private PartVar PT1;

        @ParaAnno(options = { "op211", "op212", "op213", "op214" })
        private ParaVar P21;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "3")
        private ParaVar P22;

        @PartAnno(maxQuantity = 5)
        private PartVar PT2;

        @CodeRuleAnno(normalNaturalCode = "if P0.value > 1 then P11.value > P0.value+1 else P11.value < P0.value")
        protected void Rule01() {
            // Create condition variable: P0.value > 1
            BoolVar p0GreaterThan1 = model.newBoolVar("rule01_p0GreaterThan1");
            model.addGreaterThan((IntVar) P0.value, 1).onlyEnforceIf(p0GreaterThan1);
            model.addLessOrEqual((IntVar) P0.value, 1).onlyEnforceIf(p0GreaterThan1.not());

            // Then branch: P11.value > P0.value + 1
            LinearExpr p0Plus1 = LinearExpr.newBuilder().add((IntVar) P0.value).add(1).build();
            model.addGreaterThan((IntVar) P11.value, p0Plus1).onlyEnforceIf(p0GreaterThan1);

            // Else branch: P11.value < P0.value
            model.addLessThan((IntVar) P11.value, (IntVar) P0.value).onlyEnforceIf(p0GreaterThan1.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P0.value != 2 then P21.value in (op211,op212) else P21.value in (op213,op214)")
        protected void Rule02() {
            // Create condition variable: P0.value != 2
            BoolVar p0NotEqual2 = model.newBoolVar("rule02_p0NotEqual2");
            // model.addNotEqual((IntVar)P0.value, 2).onlyEnforceIf(p0NotEqual2);
            model.addEquality((IntVar) P0.value, 2).onlyEnforceIf(p0NotEqual2.not());

            // Then branch: P21.value in (op211, op212)
            BoolVar p21InOp211 = model.newBoolVar("rule02_p21InOp211");
            BoolVar p21InOp212 = model.newBoolVar("rule02_p21InOp212");
            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op211").codeId).onlyEnforceIf(p21InOp211);
            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op212").codeId).onlyEnforceIf(p21InOp212);

            BoolVar p21InFirstGroup = model.newBoolVar("rule02_p21InFirstGroup");
            model.addBoolOr(new BoolVar[] { p21InOp211, p21InOp212 }).onlyEnforceIf(p21InFirstGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp211.not(),
            // p21InOp212.not()}).onlyEnforceIf(p21InFirstGroup.not());

            model.addEquality(p21InFirstGroup, 1).onlyEnforceIf(p0NotEqual2);

            // Else branch: P21.value in (op213, op214)
            BoolVar p21InOp213 = model.newBoolVar("rule02_p21InOp213");
            BoolVar p21InOp214 = model.newBoolVar("rule02_p21InOp214");
            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op213").codeId).onlyEnforceIf(p21InOp213);
            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op214").codeId).onlyEnforceIf(p21InOp214);

            BoolVar p21InSecondGroup = model.newBoolVar("rule02_p21InSecondGroup");
            model.addBoolOr(new BoolVar[] { p21InOp213, p21InOp214 }).onlyEnforceIf(p21InSecondGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp213.not(),
            // p21InOp214.not()}).onlyEnforceIf(p21InSecondGroup.not());

            model.addEquality(p21InSecondGroup, 1).onlyEnforceIf(p0NotEqual2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "PT1.qty = P11.value")
        protected void Rule11() {
            model.addEquality((IntVar) PT1.qty, (IntVar) P11.value);
        }

        @CodeRuleAnno(normalNaturalCode = "if P21.value in (op211,op212) then PT2.qty = 1*P22.value else PT2.qty = 2*P22.value")
        protected void Rule21() {
            // Create condition variable: P21.value in (op211, op212)
            BoolVar p21InFirstGroup = model.newBoolVar("rule21_p21InFirstGroup");
            BoolVar p21InOp211 = model.newBoolVar("rule21_p21InOp211");
            BoolVar p21InOp212 = model.newBoolVar("rule21_p21InOp212");

            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op211").codeId).onlyEnforceIf(p21InOp211);
            // model.addEquality((IntVar)P21.value,
            // P21.getParaOptionByCode("op212").codeId).onlyEnforceIf(p21InOp212);

            model.addBoolOr(new BoolVar[] { p21InOp211, p21InOp212 }).onlyEnforceIf(p21InFirstGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp211.not(),
            // p21InOp212.not()}).onlyEnforceIf(p21InFirstGroup.not());

            // Then branch: PT2.qty = 1 * P22.value
            model.addEquality((IntVar) PT2.qty, (IntVar) P22.value).onlyEnforceIf(p21InFirstGroup);

            // Else branch: PT2.qty = 2 * P22.value
            LinearExpr p22Times2 = LinearExpr.newBuilder().addTerm((IntVar) P22.value, 2).build();
            model.addEquality((IntVar) PT2.qty, p22Times2).onlyEnforceIf(p21InFirstGroup.not());
        }
    }
    // ---------------?????end----------------------------------------

    public DynScheduleTest() {
        super(DynScheduleConstraint.class);
    }

    @Test
    public void testCase1_InferP0P11FromPT1Qty() {
        // Test case 1: Infer P0.value and P11.value from PT1.qty with constraints
        // Rule01 and Rule11
        inferParas("PT1", 2);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPart("PT1").quantityEqual(2)
                .assertPara("P11").valueEqual("2");
        printSolutions(module, solutions);
    }

    @Test
    public void testCase2_InferP0P21FromPT2Qty() {
        // Test case 2: Infer P0.value and P21.value from PT2.qty with constraints
        // Rule02 and Rule21
        inferParas("PT2", 4);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPart("PT2").quantityEqual(4)
                .assertPara("P22").valueEqual("2");
        printSolutions(module, solutions);
    }

    @Test
    public void testRule01_CaseP0GreaterThan1() {
        // Test Rule01 when P0.value > 1: P11.value > P0.value + 1
        inferParasByPara("P0", "3");

        resultAssert()
                .assertSuccess();

        // Verify that P11.value > P0.value + 1 (which is 4)
        assertSolutionNum("P11:0,P11:1,P11:2,P11:3", 0);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule01_CaseP0LessOrEqual1() {
        // Test Rule01 when P0.value <= 1: P11.value < P0.value
        inferParasByPara("P0", "1");

        resultAssert()
                .assertSuccess();

        // Verify that P11.value < P0.value (which is 1)
        assertSolutionNum("P11:1,P11:2,P11:3,P11:4,P11:5", 0);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule02_CaseP0NotEqual2() {
        // Test Rule02 when P0.value != 2: P21.value in (op211, op212)
        inferParasByPara("P0", "1");

        resultAssert()
                .assertSuccess();

        // Verify that P21.value is only in first group
        assertSolutionNum("P21:op213,P21:op214", 0);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule02_CaseP0Equal2() {
        // Test Rule02 when P0.value == 2: P21.value in (op213, op214)
        inferParasByPara("P0", "2");

        resultAssert()
                .assertSuccess();

        // Verify that P21.value is only in second group
        assertSolutionNum("P21:op211,P21:op212", 0);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule11_PT1QtyEqualsP11Value() {
        // Test Rule11: PT1.qty should always equal P11.value
        inferParasByPara("P11", "3");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("PT1").quantityEqual(3);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule21_CaseP21InFirstGroup() {
        // Test Rule21 when P21.value in (op211, op212): PT2.qty = 1 * P22.value
        inferParasByPara("P21", "op211", "P22", "2");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("PT2").quantityEqual(2);
        printSolutions(module, solutions);
    }

    @Test
    public void testRule21_CaseP21InSecondGroup() {
        // Test Rule21 when P21.value in (op213, op214): PT2.qty = 2 * P22.value
        inferParasByPara("P21", "op213", "P22", "2");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("PT2").quantityEqual(4);
        printSolutions(module, solutions);
    }

    @Test
    public void testNoSolutionCase() {
        // Test case with no valid solution
        inferParas("PT1", 10); // PT1 max quantity is 3

        resultAssert()
                .assertNoSolution();
        printSolutions(module, solutions);
    }
}