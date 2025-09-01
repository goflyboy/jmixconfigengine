package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class VisibilityModeParaTest extends ModuleSecnarioTestBase {
    
    //---------------?????start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class VisibilityModeParaConstraint extends ConstraintAlgImpl {
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
        
        @Override
        protected void initConstraint() {
            // Logic1: P1.visibilityModeVar and P2.visibilityModeVar are incompatible
            // Logic2: if P0.var in (0,1) then P1.visibilityModeVar=3 else P2.visibilityModeVar=3
            // Logic3:  if P1.visibilityModeVar=3 then P1.var=0
            // Logic4:  if P2.visibilityModeVar=3 then P2.var=0
            // Logic5:   if P1.visibilityModeVar=0  才可以对P1.var手工修改（也就是作为inferParasByPara的入参)
            // Logic6:   if P2.visibilityModeVar=0  才可以对P2.var手工修改（也就是作为inferParasByPara的入参)

            // Logic 1: P1.visibilityModeVar and P2.visibilityModeVar are incompatible
            model.addDifferent((IntVar)P1.visibilityModeVar, (IntVar)P2.visibilityModeVar);
            
            // Logic 2: if P0.var in (0,1) then P1.visibilityModeVar=3 else P2.visibilityModeVar=3
            BoolVar p0In01 = model.newBoolVar("p0In01");
            model.addEquality((IntVar)P0.var, 0).onlyEnforceIf(p0In01);
            model.addEquality((IntVar)P0.var, 1).onlyEnforceIf(p0In01);
            model.addDifferent((IntVar)P0.var, 0).onlyEnforceIf(p0In01.not());
            model.addDifferent((IntVar)P0.var, 1).onlyEnforceIf(p0In01.not());
            
            model.addEquality((IntVar)P1.visibilityModeVar, 3).onlyEnforceIf(p0In01);
            model.addEquality((IntVar)P2.visibilityModeVar, 3).onlyEnforceIf(p0In01.not());
            
            // Logic3: if P1.visibilityModeVar=3 then P1.var=0
            BoolVar p1Visibility3 = model.newBoolVar("p1Visibility3");
            model.addEquality((IntVar)P1.visibilityModeVar, 3).onlyEnforceIf(p1Visibility3);
            model.addDifferent((IntVar)P1.visibilityModeVar, 3).onlyEnforceIf(p1Visibility3.not());
            model.addEquality((IntVar)P1.var, 0).onlyEnforceIf(p1Visibility3);
            
            // Logic4: if P2.visibilityModeVar=3 then P2.var=0
            BoolVar p2Visibility3 = model.newBoolVar("p2Visibility3");
            model.addEquality((IntVar)P2.visibilityModeVar, 3).onlyEnforceIf(p2Visibility3);
            model.addDifferent((IntVar)P2.visibilityModeVar, 3).onlyEnforceIf(p2Visibility3.not());
            model.addEquality((IntVar)P2.var, 0).onlyEnforceIf(p2Visibility3);
            
            // Logic5: if P1.visibilityModeVar=0 then P1.var can be manually modified
            // Logic6: if P2.visibilityModeVar=0 then P2.var can be manually modified
            // These are handled by the inference engine, no constraints needed
        }
    }
    //---------------?????end----------------------------------------

    public VisibilityModeParaTest() {
        super(VisibilityModeParaConstraint.class);
    }

    @Test
    public void testP0In01Scenario() {
        inferParasByPara("P0", "0");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P1").visibilityModeEqual(3)
                .assertPara("P1").valueEqual("0")
                .assertPara("P2").visibilityModeNotEqual(3);
    }

    @Test
    public void testP0NotIn01Scenario() {
        inferParasByPara("P0", "2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P2").visibilityModeEqual(3)
                .assertPara("P2").valueEqual("0")
                .assertPara("P1").visibilityModeNotEqual(3);
    }

    @Test
    public void testP0In01WithP1Modification() {
        inferParasByPara("P0", "1", "P1", "1");
        
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP0NotIn01WithP2Modification() {
        inferParasByPara("P0", "3", "P2", "1");
        
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP1VisibilityMode0Modification() {
        inferParasByPara("P0", "2", "P1", "1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P1").valueEqual("1")
                .assertPara("P1").visibilityModeEqual(0);
    }

    @Test
    public void testP2VisibilityMode0Modification() {
        inferParasByPara("P0", "0", "P2", "2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P2").valueEqual("2")
                .assertPara("P2").visibilityModeEqual(0);
    }

    @Test
    public void testIncompatibleVisibilityModes() {
        inferParasByPara("P1", "1", "P2", "2");
        
        resultAssert()
                .assertSuccess();
        
        assertSolutionNum("P1:1,P2:2", 1);
        
        solutions(0)
                .assertPara("P1").visibilityModeNotEqual(3)
                .assertPara("P2").visibilityModeNotEqual(3);
    }
}