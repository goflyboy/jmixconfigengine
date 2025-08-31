package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.ParaType;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class MyTShirtTest extends ModuleSecnarioTestBase {
    
    @ModuleAnno(id = 123L)
    static public class MyTShirtConstraint extends ConstraintAlgImpl {
        @ParaAnno( 
            options = {"Red", "Black", "White"},
            type = ParaType.INTEGER,
            defaultValue = "0"
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
            // if(ColorVar.var == Red && SizeVar.var == Small) {
            //     TShirt11Var.var = 1;
            // }
            // else {
            //     TShirt11Var.var = 3;
            // }
            
            BoolVar redAndSmall = model.newBoolVar("redAndSmall");
            
            model.addBoolAnd(new Literal[]{
                ColorVar.getParaOptionByCode("Red").getIsSelectedVar(),
                SizeVar.getParaOptionByCode("Small").getIsSelectedVar()
            }).onlyEnforceIf(redAndSmall);
            
            model.addBoolOr(new Literal[]{
                ColorVar.getParaOptionByCode("Red").getIsSelectedVar().not(),
                SizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
            }).onlyEnforceIf(redAndSmall.not());
            
            model.addEquality((IntVar)TShirt11Var.var, 1).onlyEnforceIf(redAndSmall);
            model.addEquality((IntVar)TShirt11Var.var, 3).onlyEnforceIf(redAndSmall.not());
        }
    }

    public MyTShirtTest() {
        super(MyTShirtConstraint.class);
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