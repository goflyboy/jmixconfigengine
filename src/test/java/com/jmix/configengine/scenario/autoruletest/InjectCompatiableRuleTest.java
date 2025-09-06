package com.jmix.configengine.scenario.autoruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class InjectCompatiableRuleTest extends ModuleSecnarioTestBase {
    
    @ModuleAnno(id = 123L)
    static public class InjectCompatiableRuleConstraint extends ConstraintAlgImpl {
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
        }
        @CodeRuleAnno(code = "rule1")
        protected void rule1() {
            
        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == Red",operator = "Requires"
        ,rightExprCode = "SizeVar.value == Small")
        protected void rule2() {    

    //自动生成，请勿编辑--start
    addCompatibleConstraintRequires("rule2", this.ColorVar, listOf("Red"), this.SizeVar, listOf("Small"));
    //自动生成，请勿编辑--end


        }
        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == Black",operator = "CoDependent"
        ,rightExprCode = "SizeVar.value == Medium")
        protected void rule3() {    

    //自动生成，请勿编辑--start
    addCompatibleConstraintCoDependent("rule3", this.ColorVar, listOf("Black"), this.SizeVar, listOf("Medium"));
    //自动生成，请勿编辑--end

        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == White",operator = "InCompatible"
        ,rightExprCode = "SizeVar.value == Big")
        protected void rule4() {    

    //自动生成，请勿编辑--start
    addCompatibleConstraintInCompatible("rule4", this.ColorVar, listOf("White"), this.SizeVar, listOf("Big"));
    //自动生成，请勿编辑--end

        }
    }

    public InjectCompatiableRuleTest() {
        super(InjectCompatiableRuleConstraint.class);
    }

    @Test
    public void testRule2Requires() {
        inferParasByPara("Color", "Red");
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("Color").valueEqual("Red")
            .assertPara("Size").valueEqual("Small");
    }

    @Test
    public void testRule2CoDependent() {
        inferParasByPara("Color", "Black");
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("Color").valueEqual("Black")
            .assertPara("Size").valueEqual("Medium");
    }

    @Test
    public void testRule2InCompatible() {
        inferParasByPara("Color", "White"); 
        assertSolutionNum("Color:White,Size:Big", 0); 
    }
}