package com.jmix.tool.confictdebugifthen;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class ConfictDebugIfThenTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ConfictDebugIfThenConstraint extends ConstraintAlgImplTestBase {
        @PartAnno(maxQuantity = 50)
        private PartVar x;

        @PartAnno(maxQuantity = 50)
        private PartVar y;

        @PartAnno(maxQuantity = 50)
        private PartVar z;

        // rule1: if(x.qty > 10) then y.qty > 7
        @CodeRuleAnno(normalNaturalCode = "if(x.qty > 10) then y.qty > 7")
        private void rule1() {
            // Create condition variable: x quantity greater than 10
            BoolVar xGreaterThan10 = model.newBoolVar("rule1_xGreaterThan10");

            // Set condition: x.qty > 10
            model.addGreaterThan(x.qty, 10).onlyEnforceIf(xGreaterThan10);
            model.addLessOrEqual(x.qty, 10).onlyEnforceIf(xGreaterThan10.not());

            // Then condition: y.qty > 7
            model.addGreaterThan(y.qty, 7).onlyEnforceIf(xGreaterThan10);
        }

        // rule2: x.qty + y.qty > 10
        @CodeRuleAnno(normalNaturalCode = "x.qty + y.qty > 10")
        private void rule2() {
            // Create linear expression for sum of x and y quantities
            AlgCPLinearExpr sumXY = model.newLinearExpr("sum_x_y");
            sumXY.addTerm(x.qty, 1);
            sumXY.addTerm(y.qty, 1);

            // Add constraint: x.qty + y.qty > 10
            model.addGreaterThan(sumXY, 10);
        }

        // rule31: y.qty > 48
        @CodeRuleAnno(normalNaturalCode = "y.qty > 48")
        private void rule31() {
            // Add constraint: y.qty > 48
            model.addGreaterThan(y.qty, 48);
        }

        // rule32: y.qty > 45
        @CodeRuleAnno(normalNaturalCode = "y.qty > 45")
        private void rule32() {
            // Add constraint: y.qty > 45
            model.addGreaterThan(y.qty, 45);
        }

        // rule4: if(x.qty > 5 and y.qty > 5) then z.qty < 10
        @CodeRuleAnno(normalNaturalCode = "if(x.qty > 5 and y.qty > 5) then z.qty < 10")
        private void rule4() {
            // Create condition variable: x quantity greater than 5 AND y quantity greater
            // than 5
            BoolVar xAndYGreaterThan5 = model.newBoolVar("rule4_xAndYGreaterThan5");

            // Set condition: (x.qty > 5) AND (y.qty > 5)
            BoolVar xGreaterThan5 = model.newBoolVar("rule4_xGreaterThan5");
            BoolVar yGreaterThan5 = model.newBoolVar("rule4_yGreaterThan5");

            model.addGreaterThan(x.qty, 5).onlyEnforceIf(xGreaterThan5);
            model.addLessOrEqual(x.qty, 5).onlyEnforceIf(xGreaterThan5.not());

            model.addGreaterThan(y.qty, 5).onlyEnforceIf(yGreaterThan5);
            model.addLessOrEqual(y.qty, 5).onlyEnforceIf(yGreaterThan5.not());

            model.addBoolAnd(new Literal[] { xGreaterThan5, yGreaterThan5 }).onlyEnforceIf(xAndYGreaterThan5);
            model.addBoolOr(new Literal[] { xGreaterThan5.not(), yGreaterThan5.not() })
                    .onlyEnforceIf(xAndYGreaterThan5.not());

            // Then condition: z.qty < 10
            model.addLessThan(z.qty, 10).onlyEnforceIf(xAndYGreaterThan5);
        }

        // rule5: y.qty < 10 （多条B1)
        @CodeRuleAnno(normalNaturalCode = "y.qty < 10")
        private void rule51() {
            // Add constraint: y.qty < 10
            model.addLessThan(y.qty, 10);
        }

        // rule6: y.qty < 15 （多条B2)
        @CodeRuleAnno(normalNaturalCode = "y.qty < 15")
        private void rule52() {
            // Add constraint: y.qty < 15
            model.addLessThan(y.qty, 15);
        }
    }
    // ---------------模型的定义end----------------------------------------

    public ConfictDebugIfThenTest() {
        super(ConfictDebugIfThenConstraint.class);
    }

    @Test
    public void testCase1() {
        // Test case 1: debugByRelaxVar set to false, expected
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
        // result: no_solution, conflict rule is rule51,rule52,rule31,rule32
        cfg.setDebugByRelaxVar(true);
        inferParasByPara();
        printSolutions();
        resultAssert()
                .assertNoSolution().assertMessageContains("rule3", "rule5");
    }

}