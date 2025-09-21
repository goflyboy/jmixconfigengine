package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.CodeRuleAnno;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import com.jmix.executor.artifact.ConstraintAlgImpl;
import com.jmix.executor.artifact.ParaVar;
import com.jmix.executor.artifact.PartVar;
import com.jmix.executor.model.ParaType;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
@SuppressWarnings("checkstyle:all")
public class DynScheduleTest extends ModuleScenarioTestBase {

    // ---------------start----------------------------------------
    @ModuleAnno(id = 123L)
    @SuppressWarnings("checkstyle:all")
    static public class DynScheduleConstraint extends ConstraintAlgImpl {
        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "4")
        private ParaVar p0;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "5")
        private ParaVar p11;

        @PartAnno(maxQuantity = 3)
        private PartVar pt1;

        @ParaAnno(options = { "op211", "op212", "op213", "op214" })
        private ParaVar p21;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "3")
        private ParaVar p22;

        @PartAnno(maxQuantity = 5)
        private PartVar pt2;

        @CodeRuleAnno(normalNaturalCode = "if p0.value > 1 then p11.value > p0.value+1 else p11.value < p0.value")
        private void rule01() {
            // Create condition variable: p0.value > 1
            BoolVar p0GreaterThan1 = model.newBoolVar("rule01_p0GreaterThan1");
            model.addGreaterThan((IntVar) p0.value, 1).onlyEnforceIf(p0GreaterThan1);
            model.addLessOrEqual((IntVar) p0.value, 1).onlyEnforceIf(p0GreaterThan1.not());

            // Then branch: p11.value > p0.value + 1
            LinearExpr p0Plus1 = LinearExpr.newBuilder().add((IntVar) p0.value).add(1).build();
            model.addGreaterThan((IntVar) p11.value, p0Plus1).onlyEnforceIf(p0GreaterThan1);

            // Else branch: p11.value < p0.value
            model.addLessThan((IntVar) p11.value, (IntVar) p0.value).onlyEnforceIf(p0GreaterThan1.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p0.value != 2 then p21.value in (op211,op212) "
                + "else p21.value in (op213,op214)")
        private void rule02() {
            // Create condition variable: p0.value != 2
            BoolVar p0NotEqual2 = model.newBoolVar("rule02_p0NotEqual2");
            // model.addNotEqual((IntVar)p0.value, 2).onlyEnforceIf(p0NotEqual2);
            model.addEquality((IntVar) p0.value, 2).onlyEnforceIf(p0NotEqual2.not());

            // Then branch: p21.value in (op211, op212)
            BoolVar p21InOp211 = model.newBoolVar("rule02_p21InOp211");
            BoolVar p21InOp212 = model.newBoolVar("rule02_p21InOp212");
            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op211").codeId).onlyEnforceIf(p21InOp211);
            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op212").codeId).onlyEnforceIf(p21InOp212);

            BoolVar p21InFirstGroup = model.newBoolVar("rule02_p21InFirstGroup");
            model.addBoolOr(new BoolVar[] { p21InOp211, p21InOp212 }).onlyEnforceIf(p21InFirstGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp211.not(),
            // p21InOp212.not()}).onlyEnforceIf(p21InFirstGroup.not());

            model.addEquality(p21InFirstGroup, 1).onlyEnforceIf(p0NotEqual2);

            // Else branch: p21.value in (op213, op214)
            BoolVar p21InOp213 = model.newBoolVar("rule02_p21InOp213");
            BoolVar p21InOp214 = model.newBoolVar("rule02_p21InOp214");
            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op213").codeId).onlyEnforceIf(p21InOp213);
            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op214").codeId).onlyEnforceIf(p21InOp214);

            BoolVar p21InSecondGroup = model.newBoolVar("rule02_p21InSecondGroup");
            model.addBoolOr(new BoolVar[] { p21InOp213, p21InOp214 }).onlyEnforceIf(p21InSecondGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp213.not(),
            // p21InOp214.not()}).onlyEnforceIf(p21InSecondGroup.not());

            model.addEquality(p21InSecondGroup, 1).onlyEnforceIf(p0NotEqual2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "pt1.qty = p11.value")
        private void rule11() {
            model.addEquality((IntVar) pt1.qty, (IntVar) p11.value);
        }

        @CodeRuleAnno(normalNaturalCode = "if p21.value in (op211,op212) then "
                + "pt2.qty = 1*p22.value else pt2.qty = 2*p22.value")
        private void rule21() {
            // Create condition variable: p21.value in (op211, op212)
            BoolVar p21InFirstGroup = model.newBoolVar("rule21_p21InFirstGroup");
            BoolVar p21InOp211 = model.newBoolVar("rule21_p21InOp211");
            BoolVar p21InOp212 = model.newBoolVar("rule21_p21InOp212");

            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op211").codeId).onlyEnforceIf(p21InOp211);
            // model.addEquality((IntVar)p21.value,
            // p21.getParaOptionByCode("op212").codeId).onlyEnforceIf(p21InOp212);

            model.addBoolOr(new BoolVar[] { p21InOp211, p21InOp212 }).onlyEnforceIf(p21InFirstGroup);
            // model.addBoolAnd(new BoolVar[]{p21InOp211.not(),
            // p21InOp212.not()}).onlyEnforceIf(p21InFirstGroup.not());

            // Then branch: pt2.qty = 1 * p22.value
            model.addEquality((IntVar) pt2.qty, (IntVar) p22.value).onlyEnforceIf(p21InFirstGroup);

            // Else branch: pt2.qty = 2 * p22.value
            LinearExpr p22Times2 = LinearExpr.newBuilder().addTerm((IntVar) p22.value, 2).build();
            model.addEquality((IntVar) pt2.qty, p22Times2).onlyEnforceIf(p21InFirstGroup.not());
        }
    }
    // ---------------?????end----------------------------------------

    public DynScheduleTest() {
        super(DynScheduleConstraint.class);
    }

    @Test
    public void testCase1InferP0P11FromPT1Qty() {
        // Test case 1: Infer P0.value and P11.value from PT1.qty with constraints
        // Rule01 and Rule11
        inferParas("pt1", 2);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPart("pt1").quantityEqual(2)
                .assertPara("p11").valueEqual("2");
        printSolutions();
    }

    @Test
    public void testCase2InferP0P21FromPT2Qty() {
        // Test case 2: Infer P0.value and P21.value from PT2.qty with constraints
        // Rule02 and Rule21
        inferParas("pt2", 4);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPart("pt2").quantityEqual(4)
                .assertPara("p22").valueEqual("2");
        printSolutions();
    }

    @Test
    public void testRule01CaseP0GreaterThan1() {
        // Test Rule01 when P0.value > 1: P11.value > P0.value + 1
        inferParasByPara("p0", "3");

        resultAssert()
                .assertSuccess();

        // Verify that p11.value > p0.value + 1 (which is 4)
        assertSolutionNum("p11:0,p11:1,p11:2,p11:3", 0);
        printSolutions();
    }

    @Test
    public void testRule01CaseP0LessOrEqual1() {
        // Test Rule01 when P0.value <= 1: P11.value < P0.value
        inferParasByPara("p0", "1");

        resultAssert()
                .assertSuccess();

        // Verify that p11.value < p0.value (which is 1)
        assertSolutionNum("p11:1,p11:2,p11:3,p11:4,p11:5", 0);
        printSolutions();
    }

    @Test
    public void testRule02CaseP0NotEqual2() {
        // Test Rule02 when P0.value != 2: P21.value in (op211, op212)
        inferParasByPara("p0", "1");

        resultAssert()
                .assertSuccess();

        // Verify that p21.value is only in first group
        assertSolutionNum("p21:op213,p21:op214", 0);
        printSolutions();
    }

    @Test
    public void testRule02CaseP0Equal2() {
        // Test Rule02 when P0.value == 2: P21.value in (op213, op214)
        inferParasByPara("p0", "2");

        resultAssert()
                .assertSuccess();

        // Verify that p21.value is only in second group
        assertSolutionNum("p21:op211,p21:op212", 0);
        printSolutions();
    }

    @Test
    public void testRule11PT1QtyEqualsP11Value() {
        // Test Rule11: PT1.qty should always equal P11.value
        inferParasByPara("p11", "3");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("pt1").quantityEqual(3);
        printSolutions();
    }

    @Test
    public void testRule21CaseP21InFirstGroup() {
        // Test Rule21 when P21.value in (op211, op212): PT2.qty = 1 * P22.value
        inferParasByPara("p21", "op211", "p22", "2");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("pt2").quantityEqual(2);
        printSolutions();
    }

    @Test
    public void testRule21CaseP21InSecondGroup() {
        // Test Rule21 when P21.value in (op213, op214): PT2.qty = 2 * P22.value
        inferParasByPara("p21", "op213", "p22", "2");

        resultAssert()
                .assertSuccess();

        solutions(0)
                .assertPart("pt2").quantityEqual(4);
        printSolutions();
    }

    @Test
    public void testNoSolutionCase() {
        // Test case with no valid solution
        inferParas("pt1", 10); // pt1 max quantity is 3

        resultAssert()
                .assertNoSolution();
        printSolutions();
    }
}