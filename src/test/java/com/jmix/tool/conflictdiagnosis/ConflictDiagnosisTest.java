package com.jmix.tool.conflictdiagnosis;

import com.jmix.executor.southinf.ConstraintAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.IntVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * RFC-0003 CONFLICT-001: 基础线性规则冲突。
 * CONFLICT-009: 请求级松弛开关。
 * CONFLICT-010: 输出解释完整性。
 */
@Slf4j
public class ConflictDiagnosisTest extends ModuleScenarioTestBase {

    @ModuleAnno(id = 1001L)
    static public class Conflict001Constraint extends ConstraintAlgBase {
        @PartAnno(maxQuantity = 50)
        private PartVar x;

        @PartAnno(maxQuantity = 50)
        private PartVar y;

        // rule1: x.quantityVar() + y.quantityVar() < 20
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() + y.quantityVar() < 20")
        private void rule1() {
            AlgCPLinearExpr sumXY = new AlgCPLinearExpr("sum_x_y");
            sumXY.addTerm(x.quantityVar(), 1);
            sumXY.addTerm(y.quantityVar(), 1);
            model.addLessThan(sumXY, 20);
        }

        // rule2: x.quantityVar() - y.quantityVar() > 10
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() - y.quantityVar() > 10")
        private void rule2() {
            AlgCPLinearExpr diffXY = AlgCPLinearExpr.weightedSum(
                    new IntVar[]{x.quantityVar(), y.quantityVar()}, new long[]{1, -1});
            model.addGreaterThan(diffXY, 10);
        }

        // rule3: y.quantityVar() > 45
        @CodeRuleAnno(normalNaturalCode = "y.quantityVar() > 45")
        private void rule3() {
            model.addGreaterThan(y.quantityVar(), 45);
        }

        // rule4: x.quantityVar() + 2 * y.quantityVar() > 10
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() + 2 * y.quantityVar() > 10")
        private void rule4() {
            AlgCPLinearExpr sumX2Y = AlgCPLinearExpr.weightedSum(
                    new IntVar[]{x.quantityVar(), y.quantityVar()}, new long[]{1, 2});
            model.addGreaterThan(sumX2Y, 10);
        }
    }

    public ConflictDiagnosisTest() {
        super(Conflict001Constraint.class);
    }

    // ====================== CONFLICT-001: 基础线性规则冲突 ======================

    @Test
    public void testConflict001_1_relaxSolveDefault_noRelaxation() {
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
    }

    @Test
    public void testConflict001_2_relaxSolveTrue_hasPartialSolution() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertNoSolution()
                .assertMessageContains("rule3")
                .assertStrictFeasible(false)
                .assertPartialSolutionSizeGreaterThanOrEqual(1);
    }

    @Test
    public void testConflict001_3_relaxSolveTrue_diagnosticContainsRule3() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertHasDiagnosticConstraints()
                .assertDiagnosticConstraintsContains("rule3");
    }

    // ====================== CONFLICT-009: 请求级松弛开关 ======================

    @Test
    public void testConflict009_1_relaxSolveNotSet_noDiagnosisReturned() {
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0)
                .assertNoDiagnosticConstraints();
    }

    @Test
    public void testConflict009_3_relaxSolveTrue_diagnosisReturned() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertNoSolution()
                .assertStrictFeasible(false)
                .assertHasDiagnosticConstraints()
                .assertPartialSolutionSizeGreaterThanOrEqual(1);
    }

    // ====================== CONFLICT-010: 输出解释完整性 ======================

    @Test
    public void testConflict010_1_messageExplainsConflictAndRelaxation() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertNoSolution()
                .assertMessageContains("infeasible", "partial solution")
                .assertStrictFeasible(false);
    }

    @Test
    public void testConflict010_2_diagnosticConstraintHasCompleteInfo() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertHasDiagnosticConstraints()
                .assertDiagnosticConstraintsContains("rule3");

        var sr = getResult().getSolverResult();
        for (var dc : sr.getDiagnosticConstraints()) {
            if (dc.getCode().startsWith("relax_")) {
                throw new AssertionError(
                        "DiagnosticConstraint code should not expose internal relax var name: " + dc.getCode());
            }
            if (dc.getConstraintType() == null || dc.getConstraintType().isEmpty()) {
                throw new AssertionError("DiagnosticConstraint type should not be empty");
            }
        }
    }

    @Test
    public void testConflict010_3_partialSolutionSatisfiesDomainBounds() {
        inferParasByParaRelax();
        printSolutions();
        resultAssert()
                .assertPartialSolutionSizeGreaterThanOrEqual(1);

        var sr = getResult().getSolverResult();
        for (var solution : sr.getSolutions()) {
            for (var part : solution.getParts()) {
                if (part.getQuantity() > 50) {
                    throw new AssertionError(
                            "Partial solution violates domain bound: " + part.getCode()
                                    + " qty=" + part.getQuantity() + " > 50");
                }
            }
        }
    }
}
