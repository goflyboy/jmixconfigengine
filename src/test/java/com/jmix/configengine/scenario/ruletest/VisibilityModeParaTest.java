package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;

@Slf4j
public class VisibilityModeParaTest extends ModuleSecnarioTestBase {
    
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
            // 逻辑2：P1.visibilityModeVar 和 P2.visibilityModeVar是不兼容的
            model.addDifferent((IntVar) P1.visibilityModeVar, (IntVar) P2.visibilityModeVar);
        }
    }

    public VisibilityModeParaTest() {
        super(VisibilityModeParaConstraint.class);
    }

    @Test
    public void testVisibilityModeIncompatible() {
        inferParasByPara("P1", "0", "P2", "0");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testVisibilityModeCompatible() {
        inferParasByPara("P1", "0", "P2", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0).assertPara("P1").valueEqual("0").assertPara("P2").valueEqual("1");
    }

    @Test
    public void testAllPossibleCombinations() {
        inferParasByPara("P1", "0");
        resultAssert().assertSuccess();
        assertSolutionNum("P2:0", 0);
        assertSolutionNum("P2:1", 1);
        assertSolutionNum("P2:2", 1);
        assertSolutionNum("P2:3", 1);
    }

    @Test
    public void testReverseInference() {
        inferParasByPara("P2", "3");
        resultAssert().assertSuccess();
        assertSolutionNum("P1:3", 0);
        assertSolutionNum("P1:0", 1);
        assertSolutionNum("P1:1", 1);
        assertSolutionNum("P1:2", 1);
    }

    @Test
    public void testMultipleSolutions() {
        inferParasByPara("P1", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(3);
        assertSolutionNum("P2:1", 0);
    }
}