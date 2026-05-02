package com.jmix.tool.conflictdiagnosis;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.BoolVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * RFC-0003 CONFLICT-003: 条件规则 If-Then 冲突。
 * 验证带 onlyEnforceIf() 的条件约束在松弛变量叠加后仍可诊断。
 */
@Slf4j
public class ConflictDiagnosisIfThenTest extends ModuleScenarioTestBase {

    @ModuleAnno(id = 1003L)
    static public class Conflict003Constraint extends ConstraintAlgImplTestBase {
        @PartAnno(maxQuantity = 50)
        private PartVar x;

        @PartAnno(maxQuantity = 50)
        private PartVar y;

        // rule1: if x.qty > 10 then y.qty > 7
        @CodeRuleAnno(normalNaturalCode = "if x.qty > 10 then y.qty > 7")
        private void rule1() {
            BoolVar xGt10 = model.newBoolVar("x_gt_10");
            model.addGreaterThan(x.qty, 10).onlyEnforceIf(xGt10);
            model.addLessOrEqual(x.qty, 10).onlyEnforceIf(xGt10.not());
            model.addGreaterThan(y.qty, 7).onlyEnforceIf(xGt10);
        }

        // rule2: x.qty > 20 -> triggers rule1, so y > 7
        @CodeRuleAnno(normalNaturalCode = "x.qty > 20")
        private void rule2() {
            model.addGreaterThan(x.qty, 20);
        }

        // rule3: y.qty < 5 -> conflicts with rule1's consequence
        @CodeRuleAnno(normalNaturalCode = "y.qty < 5")
        private void rule3() {
            model.addLessThan(y.qty, 5);
        }
    }

    public ConflictDiagnosisIfThenTest() {
        super(Conflict003Constraint.class);
    }

    @Test
    public void testConflict003_1_originalInfeasible() {
        // x > 20 triggers rule1 -> y > 7, but rule3 requires y < 5 => original infeasible
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
    }

    @Test
    public void testConflict003_2_relaxSolveTrue_returnsPartialSolution() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertNoSolution()
                .assertStrictFeasible(false)
                .assertPartialSolutionSizeGreaterThanOrEqual(1)
                .assertHasDiagnosticConstraints();
    }

    @Test
    public void testConflict003_3_diagnosticIdentifiesConflictingRule() {
        inferParasByParaRelax();
        printSolutions();

        String message = getResult().getMessage();
        boolean hasConflict = message.contains("rule1")
                || message.contains("rule2")
                || message.contains("rule3");
        if (!hasConflict) {
            throw new AssertionError(
                    "Expected message to contain at least one of rule1/rule2/rule3, got: " + message);
        }
        resultAssert()
                .assertNoSolution()
                .assertHasDiagnosticConstraints();
    }
}
