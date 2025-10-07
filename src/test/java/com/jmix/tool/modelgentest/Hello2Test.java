package com.jmix.tool.modelgentest;

import com.jmix.coretest.*; 
import com.jmix.executor.imodel.*;
import com.jmix.executor.imodel.anno.*; 
import com.google.ortools.sat.*; 
import lombok.extern.slf4j.Slf4j; 
import org.junit.jupiter.api.Test;

@Slf4j
public class Hello2Test extends ModuleScenarioTestBase {
    
    //---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class Hello2Constraint extends ConstraintAlgImplTestBase {
        @ParaAnno( 
            options = {"Red", "Black", "White"}
        )
        private ParaVar colorVar;

        @ParaAnno( 
            options = {"Small", "Medium", "Big"}
        )
        private ParaVar sizeVar;

        @PartAnno()
        private PartVar tShirt11Var;
        
        @CodeRuleAnno(normalNaturalCode = "if(colorVar.value == Red && sizeVar.value == Small) { tShirt11Var.qty = 1; } else { tShirt11Var.qty = 3; }")
        private void rule1() {
            // Create condition variable: color is Red and size is Small
            BoolVar redAndSmall = model.newBoolVar("rule1_redAndSmall");

            // Implement condition logic: redAndSmall = (color == Red) AND (size == Small)
            model.addBoolAnd(new Literal[]{
                    this.colorVar.getParaOptionByCode("Red").getIsSelectedVar(),
                    this.sizeVar.getParaOptionByCode("Small").getIsSelectedVar()
            }).onlyEnforceIf(redAndSmall);

            // If not Red and Small combination, set redAndSmall to false
            model.addBoolOr(new Literal[]{
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
    //---------------模型的定义end----------------------------------------

    public Hello2Test() {
        super(Hello2Constraint.class);
    }

    @Test
    public void testIfCondition() {
        // Test inference when tShirt11 quantity is 1 (if condition)
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
        // Test inference when tShirt11 quantity is 3 (else condition)
        inferParas("tShirt11", 3);

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);

        // Verify that Red+Small combination is not in solutions
        assertSolutionNum("color:Red,size:Small", 0);
        
        // Verify other combinations are present
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
    public void testNoSolution() {
        // Test case with no valid solution
        inferParas("tShirt11", 10);
        
        resultAssert().assertNoSolution();
        printSolutions();
    }

    @Test
    public void testOtherColorSizeCombinations() {
        // Test inference for other color and size combinations
        inferParasByPara("color", "Black", "size", "Medium");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0).assertPart("tShirt11").quantityEqual(3);
        printSolutions();
    }
}