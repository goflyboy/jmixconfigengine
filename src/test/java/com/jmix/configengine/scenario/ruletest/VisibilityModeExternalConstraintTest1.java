package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class VisibilityModeExternalConstraintTest1 extends ModuleSecnarioTestBase {
    
    //---------------Model definition start----------------------------------------
    @ModuleAnno(id = 1002L)
    static public class VisibilityModeExternalConstraint extends ConstraintAlgImpl {
        @ParaAnno(
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
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
            // Rule C1: if (P0.var==1 or P0.var==2) then P1.visibilityModeVar=3
            // Rule C2: if P1.var==2 then P2.var=2
            // visibilityModeVar=0 means visible, var can take any value
            // visibilityModeVar=3 means invisible, var must be minimum value
            
            // Define visibility mode domains (0 or 3 only)
            setupVisibilityModeDomain(P0);
            setupVisibilityModeDomain(P1);
            setupVisibilityModeDomain(P2);
            
            // P0 is always visible (default behavior)
            model.addEquality((IntVar)P0.visibilityModeVar, 0);
            
            // P2 is always visible (default behavior)
            model.addEquality((IntVar)P2.visibilityModeVar, 0);
            
            // Implement Constraint C1: if P0.var in {1,2} then P1.visibilityModeVar=3, else P1.visibilityModeVar=0
            BoolVar p0Is0 = model.newBoolVar("p0_is_0");
            model.addEquality((IntVar)P0.var, 0).onlyEnforceIf(p0Is0);
            model.addDifferent((IntVar)P0.var, 0).onlyEnforceIf(p0Is0.not());
            
            // When P0.var is 0, P1 is visible
            model.addEquality((IntVar)P1.visibilityModeVar, 0).onlyEnforceIf(p0Is0);
            
            // When P0.var is not 0 (1 or 2), P1 is invisible
            model.addEquality((IntVar)P1.visibilityModeVar, 3).onlyEnforceIf(p0Is0.not());
            
            // Visibility control: when invisible, var must be minimum value
            implementVisibilityControl(P1, 0);
            
            // Implement Constraint C2: if P1.var==2 then P2.var=2
            BoolVar p1Is2 = model.newBoolVar("p1_is_2");
            model.addEquality((IntVar)P1.var, 2).onlyEnforceIf(p1Is2);
            model.addDifferent((IntVar)P1.var, 2).onlyEnforceIf(p1Is2.not());
            
            model.addEquality((IntVar)P2.var, 2).onlyEnforceIf(p1Is2);
        }
        
        private void setupVisibilityModeDomain(ParaVar paraVar) {
            // visibilityModeVar can only be 0 or 3
            BoolVar isVisible = model.newBoolVar(paraVar.getCode() + "_visible");
            model.addEquality((IntVar)paraVar.visibilityModeVar, 0).onlyEnforceIf(isVisible);
            model.addEquality((IntVar)paraVar.visibilityModeVar, 3).onlyEnforceIf(isVisible.not());
        }
        
        private void implementVisibilityControl(ParaVar paraVar, int minValue) {
            // When visibilityModeVar=3, var must be minimum value
            BoolVar isInvisible = model.newBoolVar(paraVar.getCode() + "_invisible");
            model.addEquality((IntVar)paraVar.visibilityModeVar, 3).onlyEnforceIf(isInvisible);
            model.addDifferent((IntVar)paraVar.visibilityModeVar, 3).onlyEnforceIf(isInvisible.not());
            
            model.addEquality((IntVar)paraVar.var, minValue).onlyEnforceIf(isInvisible);
        }
    }
    //---------------Model definition end----------------------------------------

    public VisibilityModeExternalConstraintTest1() {
        super(VisibilityModeExternalConstraint.class);
    }

    @Test
    public void testP0Value1TriggersP1Invisible() {
        // Test Case: P0.var=1 triggers P1.visibilityModeVar=3
        inferParasByPara("P0", "1");
        
        // Should have exactly 3 solutions
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        
        // All solutions should have:
        // P0.var=1, P0.visibilityModeVar=0
        // P1.var=0 (minimum due to invisibility), P1.visibilityModeVar=3
        // P2.var in {0,1,2}, P2.visibilityModeVar=0
        for(int i = 0; i < 3; i++) {
            solutions(i)
                .assertPara("P0").valueEqual(1).visibilityModeEqual(0)
                .assertPara("P1").valueEqual(0).visibilityModeEqual(3)
                .assertPara("P2").visibilityModeEqual(0);
        }
        
        // Verify P2 has all three possible values
        assertSolutionNum("P0:1,P1:0,P2:0", 1);
        assertSolutionNum("P0:1,P1:0,P2:1", 1);
        assertSolutionNum("P0:1,P1:0,P2:2", 1);
        
        printSolutions();
    }
    
    @Test
    public void testP0Value2TriggersP1Invisible() {
        // Test Case: P0.var=2 also triggers P1.visibilityModeVar=3
        inferParasByPara("P0", "2");
        
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        
        // Similar to P0.var=1 case
        for(int i = 0; i < 3; i++) {
            solutions(i)
                .assertPara("P0").valueEqual(2).visibilityModeEqual(0)
                .assertPara("P1").valueEqual(0).visibilityModeEqual(3)
                .assertPara("P2").visibilityModeEqual(0);
        }
        
        assertSolutionNum("P0:2,P1:0,P2:0", 1);
        assertSolutionNum("P0:2,P1:0,P2:1", 1);
        assertSolutionNum("P0:2,P1:0,P2:2", 1);
        
        printSolutions();
    }
    
    @Test
    public void testP0Value0KeepsP1Visible() {
        // Test Case: P0.var=0 does not trigger constraint C1
        inferParasByPara("P0", "0");
        
        // P1 is visible, P1.var can be {0,1,2}
        // When P1.var=2, P2.var must be 2 (C2)
        // When P1.var!=2, P2.var can be {0,1,2}
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(7);
        
        // All solutions should have visibilityModeVar=0 for all parameters
        for(int i = 0; i < 7; i++) {
            solutions(i)
                .assertPara("P0").valueEqual(0).visibilityModeEqual(0)
                .assertPara("P1").visibilityModeEqual(0)
                .assertPara("P2").visibilityModeEqual(0);
        }
        
        // Verify solution distribution
        assertSolutionNum("P0:0,P1:0", 3);  // P2 can be 0,1,2
        assertSolutionNum("P0:0,P1:1", 3);  // P2 can be 0,1,2
        assertSolutionNum("P0:0,P1:2", 1);  // P2 must be 2 (C2)
        
        printSolutions();
    }
    
    // @Test
    // public void testAllSolutions() {
    //     // Test all possible solutions
    //     inferAllSolutions();
        
    //     resultAssert()
    //         .assertSuccess()
    //         .assertSolutionSizeEqual(13);
        
    //     // P0=0: 7 solutions (P1 visible, can be 0,1,2)
    //     assertSolutionNum("P0:0", 7);
        
    //     // P0=1: 3 solutions (P1 invisible, must be 0)
    //     assertSolutionNum("P0:1", 3);
        
    //     // P0=2: 3 solutions (P1 invisible, must be 0)
    //     assertSolutionNum("P0:2", 3);
        
    //     printSolutions();
    // }
    
    @Test
    public void testConstraintC2Impact() {
        // Test the impact of constraint C2
        inferParasByPara("P1", "2");
        
        // P1.var=2 is only possible when P0.var=0 (P1 must be visible)
        // And when P1.var=2, P2.var must be 2
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        
        solutions(0)
            .assertPara("P0").valueEqual(0).visibilityModeEqual(0)
            .assertPara("P1").valueEqual(2).visibilityModeEqual(0)
            .assertPara("P2").valueEqual(2).visibilityModeEqual(0);
        
        printSolutions();
    }
    
    // @Test
    // public void testSearchSpaceReduction() {
    //     // Demonstrate search space reduction due to visibility control
    //     inferAllSolutions();
        
    //     // Total solutions should be 13, not 27
    //     resultAssert()
    //         .assertSuccess()
    //         .assertSolutionSizeEqual(13);
        
    //     // Verify P1 is never 1 or 2 when P0 is 1 or 2
    //     assertSolutionNum("P0:1,P1:1", 0);
    //     assertSolutionNum("P0:1,P1:2", 0);
    //     assertSolutionNum("P0:2,P1:1", 0);
    //     assertSolutionNum("P0:2,P1:2", 0);
        
    //     log.info("Search space reduced from 27 to 13 solutions due to visibility control");
    //     printSolutions();
    // }
    
    @Test
    public void testP1InferenceWithInvisible() {
        // Test inference when P1 is forced to be invisible
        inferParasByPara("P1", "0");
        
        // P1.var=0 can occur in two scenarios:
        // 1. P0=0 and P1 is visible (P1 chooses 0)
        // 2. P0=1 or P0=2 and P1 is invisible (P1 forced to 0)
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(9);
        
        // When P0=0, P1=0 (visible): 3 solutions (P2 can be 0,1,2)
        assertSolutionNum("P0:0,P1:0", 3);
        
        // When P0=1, P1=0 (invisible): 3 solutions (P2 can be 0,1,2)
        assertSolutionNum("P0:1,P1:0", 3);
        
        // When P0=2, P1=0 (invisible): 3 solutions (P2 can be 0,1,2)
        assertSolutionNum("P0:2,P1:0", 3);
        
        printSolutions();
    }
}