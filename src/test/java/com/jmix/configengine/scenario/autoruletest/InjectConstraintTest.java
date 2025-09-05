package com.jmix.configengine.scenario.autoruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class InjectConstraintTest extends ModuleSecnarioTestBase {
    
    @ModuleAnno(id = 123L)
    static public class InjectConstraint extends ConstraintAlgImpl {
        @ParaAnno( 
            options = {"Red", "Black", "White"}
        )
        private ParaVar ColorVar;

        @ParaAnno( 
            options = {"Small", "Medium", "Big"}
        )
        private ParaVar SizeVar;

        @PartAnno()
        private PartVar TShirt11Var;
        
        @Override
        protected void initConstraint() {
            // "Red-10", "Black-20", "White-30"
            // "Small-10", "Medium-20", "Big-30"
            // if(ColorVar.value == Red && SizeVar.value == Small) {
            //     TShirt11Var.qty = 1;
            // }
            // else {
            //     TShirt11Var.qty = 3;
            // }
            
            // BoolVar redAndSmall = model.newBoolVar("redAndSmall");
            
            // model.addBoolAnd(new Literal[]{
            //     ColorVar.getParaOptionByCode("Red").getIsSelectedVar(),
            //     SizeVar.getParaOptionByCode("Small").getIsSelectedVar()
            // }).onlyEnforceIf(redAndSmall);
            
            // model.addBoolOr(new Literal[]{
            //     ColorVar.getParaOptionByCode("Red").getIsSelectedVar().not(),
            //     SizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
            // }).onlyEnforceIf(redAndSmall.not());
            
            // model.addEquality((IntVar)TShirt11Var.qty, 1).onlyEnforceIf(redAndSmall);
            // model.addEquality((IntVar)TShirt11Var.qty, 3).onlyEnforceIf(redAndSmall.not());
        }
        @CodeRuleAnno(code = "rule1")
        protected void rule1() {
            
        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == Red",
        rightExprCode = "SizeVar.value == Small",operator = "Requires")
        protected void rule2() {
            
        }
    }

    public InjectConstraintTest() {
        super(InjectConstraint.class);
    }

    @Test
    public void testIfCondition() {
        inferParas("TShirt11", 1);
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("Color").valueEqual("Red")
            .assertPara("Size").valueEqual("Small");
    }

    @Test
    public void testElseCondition() {
        inferParas("TShirt11", 3);
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(8);
        assertSolutionNum("Color:Red,Size:Small", 0);
        assertSolutionNum("Color:Black,Size:Big", 1);
        assertSolutionNum("Color:White,Size:Medium", 1);
        assertSolutionNum("Color:Red,Size:Medium", 1);
        assertSolutionNum("Color:Black,Size:Medium", 1);
        assertSolutionNum("Color:White,Size:Big", 1);
        assertSolutionNum("Color:Red,Size:Big", 1);
        assertSolutionNum("Color:Black,Size:Small", 1);
    }

    @Test
    public void testNoSolution() {
        inferParas("TShirt11", 10);
        resultAssert().assertNoSolution();
    }
}