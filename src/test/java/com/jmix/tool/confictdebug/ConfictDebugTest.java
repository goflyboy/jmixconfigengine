package com.jmix.tool.confictdebug;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.anno.CodeRuleAnno;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.PartAnno;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class ConfictDebugTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ConfictDebugConstraint extends ConstraintAlgImplTestBase {
        @PartAnno(maxQuantity = 50)
        private PartVar x;

        @PartAnno(maxQuantity = 50)
        private PartVar y;

        // rule1: x.qty + y.qty < 20
        @CodeRuleAnno(normalNaturalCode = "x.qty + y.qty < 20")
        private void rule1() {
            // Create linear expression for x.qty + y.qty
            LinearExpr sumXY = LinearExpr.sum(new IntVar[] { x.qty, y.qty });
            // Add constraint: x.qty + y.qty < 20
            model.addLessThan(sumXY, 20);
        }

        // rule2: x.qty - y.qty > 10
        @CodeRuleAnno(normalNaturalCode = "x.qty - y.qty > 10")
        private void rule2() {
            // Create linear expression for x.qty - y.qty
            LinearExpr diffXY = LinearExpr.weightedSum(new IntVar[] { x.qty, y.qty }, new long[] { 1, -1 });
            // Add constraint: x.qty - y.qty > 10
            model.addGreaterThan(diffXY, 10);
        }

        // rule3: y.qty > 45
        @CodeRuleAnno(normalNaturalCode = "y.qty > 45")
        private void rule3() {
            // Add constraint: y.qty > 45
            model.addGreaterThan(y.qty, 45);
        }

        // rule4: x.qty + 2 * y.qty > 10
        @CodeRuleAnno(normalNaturalCode = "x.qty + 2 * y.qty > 10")
        private void rule4() {
            // Create linear expression for x.qty + 2 * y.qty
            LinearExpr sumX2Y = LinearExpr.weightedSum(new IntVar[] { x.qty, y.qty }, new long[] { 1, 2 });
            // Add constraint: x.qty + 2 * y.qty > 10
            model.addGreaterThan(sumX2Y, 10);
        }
    }
    // ---------------模型的定义end----------------------------------------

    public ConfictDebugTest() {
        super(ConfictDebugConstraint.class);
    }

    @Test
    public void testCase1() {
        // Test case 1: debugByRelaxationVar set to false, inferParas("y", 3); expected
        // result: success, assertSolutionNum is 0
        cfg.setDebugByRelaxationVar(false);
        inferParasByPara();

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
    }

    @Test
    public void testCase2() {
        // Test case 2: debugByRelaxationVar set to true, inferParas("y", 3); expected
        // result: no_solution, conflict rule is rule3
        cfg.setDebugByRelaxationVar(true);
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertNoSolution().assertMessageContains("rule3");
        // Note: Conflict rule detection would be handled by the framework
    }
}