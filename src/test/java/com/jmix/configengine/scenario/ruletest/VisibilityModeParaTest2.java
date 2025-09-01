package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class VisibilityModeParaTest2 extends ModuleSecnarioTestBase {
    
    //---------------?????start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class VisibilityModeParaConstraint extends ConstraintAlgImpl {
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P2;
        
        @Override
        protected void initConstraint() {
            // Logic 2: P1.visibilityModeVar and P2.visibilityModeVar are incompatible
            // Add constraint that P1 and P2 visibility modes cannot both be 0 (visible and editable)
            model.addDifferent((IntVar)P1.visibilityModeVar, (IntVar)P2.visibilityModeVar);
        }
    }
    //---------------?????end----------------------------------------

    public VisibilityModeParaTest2() {
        super(VisibilityModeParaConstraint.class);
    }

    @Test
    public void testVisibilityModesIncompatible() {
        // Test case 1: Both visibility modes cannot be the same
        inferParasByPara("P1", "0", "P2", "0");
        resultAssert().assertFailure();
    }

    @Test
    public void testDifferentVisibilityModes() {
        // Test case 2: Different visibility modes should be valid
        inferParasByPara("P1", "0", "P2", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        
        solutions(0)
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("1");
    }

    @Test
    public void testAllVisibilityModeCombinations() {
        // Test case 3: Verify all valid combinations
        inferParasByPara("P1", "0");
        resultAssert().assertSuccess();
        
        // Check that P2 cannot have the same visibility mode as P1
        assertSolutionNum("P2:0", 0);
        assertSolutionNum("P2:1", 1);
        assertSolutionNum("P2:2", 1);
        assertSolutionNum("P2:3", 1);
    }

    @Test
    public void testReverseInference() {
        // Test case 4: Reverse inference from P2 to P1
        inferParasByPara("P2", "3");
        resultAssert().assertSuccess();
        
        // P1 cannot have visibility mode 3
        assertSolutionNum("P1:3", 0);
        assertSolutionNum("P1:0", 1);
        assertSolutionNum("P1:1", 1);
        assertSolutionNum("P1:2", 1);
    }

    @Test
    public void testMultipleSolutions() {
        // Test case 5: Multiple valid solutions should exist
        inferParasByPara("P1", "1");
        resultAssert().assertSuccess();
        
        // Should have 3 solutions (P2 can be 0, 2, 3 but not 1)
        resultAssert().assertSolutionSizeEqual(3);
        assertSolutionNum("P2:0", 1);
        assertSolutionNum("P2:2", 1);
        assertSolutionNum("P2:3", 1);
        assertSolutionNum("P2:1", 0);
    }
}