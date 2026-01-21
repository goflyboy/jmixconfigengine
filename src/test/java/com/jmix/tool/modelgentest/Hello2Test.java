package com.jmix.tool.modelgentest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.anno.CodeRuleAnno;
import com.jmix.executor.bmodel.anno.ModuleAnno;
import com.jmix.executor.bmodel.anno.ParaAnno;
import com.jmix.executor.bmodel.anno.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class Hello2Test extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class Hello2Constraint extends ConstraintAlgImplTestBase {
        @ParaAnno(options = { "Red", "Black", "White" })
        private ParaVar colorVar;

        @ParaAnno(options = { "Small", "Medium", "Big" })
        private ParaVar sizeVar;

        @PartAnno()
        private PartVar tShirt11Var;

        @CodeRuleAnno(normalNaturalCode = "如果的颜色是红色且大小是小号是，则tShirt11的数量为1，否者为3 ")
        private void rule1() {
            // Create condition variable: color is Red and size is Small
            BoolVar redAndSmall = model.newBoolVar("rule1_redAndSmall");

            // Implement condition logic: redAndSmall = (color == Red) AND (size == Small)
            model.addBoolAnd(new Literal[] {
                    this.colorVar.getParaOptionByCode("Red").getIsSelectedVar(),
                    this.sizeVar.getParaOptionByCode("Small").getIsSelectedVar()
            }).onlyEnforceIf(redAndSmall);

            // If not Red and Small combination, set redAndSmall to false
            model.addBoolOr(new Literal[] {
                    this.colorVar.getParaOptionByCode("Red").getIsSelectedVar().not(),
                    this.sizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
            }).onlyEnforceIf(redAndSmall.not());

            // Set tShirt11 quantity based on condition
            // If redAndSmall is true, set quantity to 1
            model.addEquality(this.tShirt11Var.qty, 1).onlyEnforceIf(redAndSmall);

            // If redAndSmall is false, set quantity to 3
            model.addEquality(this.tShirt11Var.qty, 3).onlyEnforceIf(redAndSmall.not());
        }
    }
    // ---------------模型的定义end----------------------------------------

    public Hello2Test() {
        super(Hello2Constraint.class);
    }

    @Test
    public void testIfCondition() {
        // Test when color is Red and size is Small, tShirt11 quantity should be 1
        inferParas("tShirt11", 1);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0).assertPara("color").valueEqual("Red")
                .assertPara("size").valueEqual("Small");
        printSolutions();
    }

    @Test
    public void testElseCondition() {
        // Test other combinations, tShirt11 quantity should be 3
        inferParas("tShirt11", 3);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);

        // The if condition solution should not be present
        assertSolutionNum("color:Red,size:Small", 0);

        // Verify some else condition solutions exist
        assertSolutionNum("color:Black,size:Big", 1);
        assertSolutionNum("color:White,size:Medium", 1);
        assertSolutionNum("color:Red,size:Medium", 1);
        printSolutions();
    }

    @Test
    public void testMultipleParaInference() {
        // Test inference with multiple parameters specified
        inferParasByPara("color", "Red", "size", "Small");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0).assertPart("tShirt11").quantityEqual(1);
        printSolutions();
    }

    @Test
    public void testInvalidQuantity() {
        // Test invalid quantity that doesn't satisfy any condition
        inferParas("tShirt11", 2);

        resultAssert().assertSuccess()
                .assertSolutionSizeEqual(0);
        printSolutions();
    }

    @Test
    public void testAllElseCombinations() {
        // Test all possible else condition combinations
        inferParas("tShirt11", 3);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);

        // Verify all 8 combinations are valid (3 colors * 3 sizes - 1 if condition)
        printSolutions();
    }
}