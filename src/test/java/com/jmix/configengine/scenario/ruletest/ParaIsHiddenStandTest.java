package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;

@Slf4j
public class ParaIsHiddenStandTest extends ModuleSecnarioTestBase {
    
    @ModuleAnno(id = 123L)
    static public class ParaIsHiddenStandConstraint extends ConstraintAlgImpl {
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P0;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P2;
        
        @PartAnno(
			maxQuantity=3
		)
        private PartVar Part1;
        
        @Override
        protected void initConstraint() {
            rule1();
            rule2();
            rule3();
            rule4();
            rule5();
            rule6();
        }
        
        @CodeRuleAnno()
        protected void rule1() {
            addVarAboutHiddenConstraints(P1, P2);
            model.addBoolOr(new Literal[]{
                P1.isHidden.not(),
                P2.isHidden.not()
            });
        }
        
        @CodeRuleAnno()
        protected void rule2() {
            addVarAboutHiddenConstraints(P0, P1, P2);
            
            BoolVar p0In01 = model.newBoolVar("p0_in_01");
            model.addEquality((IntVar)P0.value, 0).onlyEnforceIf(p0In01);
            model.addEquality((IntVar)P0.value, 1).onlyEnforceIf(p0In01);
            model.addDifferent((IntVar)P0.value, 0).onlyEnforceIf(p0In01.not());
            model.addDifferent((IntVar)P0.value, 1).onlyEnforceIf(p0In01.not());
            
            model.addEquality(P1.isHidden, 1).onlyEnforceIf(p0In01);
            model.addEquality(P2.isHidden, 1).onlyEnforceIf(p0In01.not());
        }
        
        @CodeRuleAnno()
        protected void rule3() {
            addVarAboutHiddenConstraints(P1);
            model.addEquality((IntVar)P1.value, 0).onlyEnforceIf(P1.isHidden);
        }
        
        @CodeRuleAnno()
        protected void rule4() {
            addVarAboutHiddenConstraints(P2);
            model.addEquality((IntVar)P2.value, 0).onlyEnforceIf(P2.isHidden);
        }
        
        @CodeRuleAnno()
        protected void rule5() {
            addVarAboutHiddenConstraints(P1);
            model.addImplication(P1.isHidden.not(), model.newConstant(1));
        }
        
        @CodeRuleAnno()
        protected void rule6() {
            addVarAboutHiddenConstraints(P2);
            model.addImplication(P2.isHidden.not(), model.newConstant(1));
        }
    }

    public ParaIsHiddenStandTest() {
        super(ParaIsHiddenStandConstraint.class);
    }

    @Test
    public void testP0In01ThenP1Hidden() {
        inferParasByPara("P0", "0");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual("1")
                   .assertPara("P1").valueEqual("0");
    }

    @Test
    public void testP0NotIn01ThenP2Hidden() {
        inferParasByPara("P0", "2");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P2").isHiddenEqual("1")
                   .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP1AndP2HiddenIncompatible() {
        inferParasByPara("P0", "0", "P1", "1");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP1ManualModifyWhenNotHidden() {
        inferParasByPara("P0", "3", "P1", "1");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual("0")
                   .assertPara("P1").valueEqual("1");
    }

    @Test
    public void testP2ManualModifyWhenNotHidden() {
        inferParasByPara("P0", "0", "P2", "2");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P2").isHiddenEqual("0")
                   .assertPara("P2").valueEqual("2");
    }

    @Test
    public void testP1HiddenCannotModify() {
        inferParasByPara("P0", "0", "P1", "2");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP2HiddenCannotModify() {
        inferParasByPara("P0", "1", "P2", "1");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testAllPossibleP0Values() {
        for (int i = 0; i <= 3; i++) {
            inferParasByPara("P0", String.valueOf(i));
            resultAssert().assertSuccess();
        }
    }

    @Test
    public void testPart1Inference() {
        inferParas("Part1", 2);
        resultAssert().assertSuccess();
    }
}