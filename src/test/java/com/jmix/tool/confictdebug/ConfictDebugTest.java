package com.jmix.tool.confictdebug;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPIntVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class ConfictDebugTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ConfictDebugConstraint extends ModuleAlgBase {
        @PartAnno(maxQuantity = 50)
        private PartVar x;

        @PartAnno(maxQuantity = 50)
        private PartVar y;

        // rule1: x.quantityVar() + y.quantityVar() < 20
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() + y.quantityVar() < 20")
        private void rule1() {
            // Create linear expression for x.quantityVar() + y.quantityVar()
            AlgCPLinearExpr sumXY = model().newLinearExpr("sum_x_y");
            sumXY.addTerm(x.quantityVar(), 1);
            sumXY.addTerm(y.quantityVar(), 1);
            // Add constraint: x.quantityVar() + y.quantityVar() < 20
            model().addLessThan(sumXY, 20);
        }

        // rule2: x.quantityVar() - y.quantityVar() > 10
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() - y.quantityVar() > 10")
        private void rule2() {
            // Create linear expression for x.quantityVar() - y.quantityVar()
            AlgCPLinearExpr diffXY = AlgCPLinearExpr.weightedSum(new AlgCPIntVar[] { x.quantityVar(), y.quantityVar() },
                    new long[] { 1, -1 });
            // Add constraint: x.quantityVar() - y.quantityVar() > 10
            model().addGreaterThan(diffXY, 10);
        }

        // rule3: y.quantityVar() > 45
        @CodeRuleAnno(normalNaturalCode = "y.quantityVar() > 45")
        private void rule3() {
            // Add constraint: y.quantityVar() > 45
            model().addGreaterThan(y.quantityVar(), 45);
        }

        // rule4: x.quantityVar() + 2 * y.quantityVar() > 10
        @CodeRuleAnno(normalNaturalCode = "x.quantityVar() + 2 * y.quantityVar() > 10")
        private void rule4() {
            // Create linear expression for x.quantityVar() + 2 * y.quantityVar()
            AlgCPLinearExpr sumX2Y = AlgCPLinearExpr.weightedSum(new AlgCPIntVar[] { x.quantityVar(), y.quantityVar() },
                    new long[] { 1, 2 });
            // Add constraint: x.quantityVar() + 2 * y.quantityVar() > 10
            model().addGreaterThan(sumX2Y, 10);
        }
    }
    // ---------------模型的定义end----------------------------------------

    public ConfictDebugTest() {
        super(ConfictDebugConstraint.class);
    }

    @Test
    public void testCase1() {
        // Test case 1: debugByRelaxVar set to false, inferParas("y", 3); expected
        // result: success, assertSolutionNum is 0
        cfg.setDebugByRelaxVar(false);
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
    }

    @Test
    public void testCase2() {
        // Test case 2: debugByRelaxVar set to true, inferParas(); expected
        // result: no_solution, conflict rule is rule3
        cfg.setDebugByRelaxVar(true);
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertNoSolution().assertMessageContains("rule3");
    }
}
