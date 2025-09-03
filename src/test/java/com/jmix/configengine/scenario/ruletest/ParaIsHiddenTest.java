package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class ParaIsHiddenTest extends ModuleSecnarioTestBase {
    
    //---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ParaIsHiddenConstraint extends ConstraintAlgImpl {
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
            // Logic 1: P1.isHiddenVar and P2.isHiddenVar are incompatible
            model.addBoolOr(new Literal[]{P1.isHiddenVar.not(), P2.isHiddenVar.not()});

            // Logic 2: if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1
            BoolVar p0In01 = model.newBoolVar("p0In01");
            model.addLessOrEqual((IntVar)P0.var, 1).onlyEnforceIf(p0In01);
            model.addGreaterThan((IntVar)P0.var, 1).onlyEnforceIf(p0In01.not());
            
            model.addEquality(P1.isHiddenVar, 1).onlyEnforceIf(p0In01);
            model.addEquality(P2.isHiddenVar, 1).onlyEnforceIf(p0In01.not());

            // Logic3: if P1.isHiddenVar=1 then P1.var=0
            model.addEquality((IntVar)P1.var, 0).onlyEnforceIf(P1.isHiddenVar);

            // Logic4: if P2.isHiddenVar=1 then P2.var=0
            model.addEquality((IntVar)P2.var, 0).onlyEnforceIf(P2.isHiddenVar);

            // Logic5: if P1.isHiddenVar=0 then P1.var can be manually modified (as input for inferParasByPara)
            // Logic6: if P2.isHiddenVar=0 then P2.var can be manually modified (as input for inferParasByPara)
            // These are handled by the framework, no constraints needed
        }
    }
    //---------------模型的定义end----------------------------------------

    public ParaIsHiddenTest() {
        super(ParaIsHiddenConstraint.class);
    }

    @Test
    public void testP0In01_HidesP1() {
        inferParasByPara("P0", "0");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("0")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP0In01_HidesP1_Alternative() {
        inferParasByPara("P0", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("1")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP0NotIn01_HidesP2() {
        inferParasByPara("P0", "2");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("2")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP0NotIn01_HidesP2_Alternative() {
        inferParasByPara("P0", "3");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("3")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP1HiddenCannotBeModified() {
        inferParasByPara("P0", "0", "P1", "1");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP2HiddenCannotBeModified() {
        inferParasByPara("P0", "2", "P2", "1");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testP1VisibleCanBeModified() {
        inferParasByPara("P0", "2", "P1", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("2")
            .assertPara("P1").valueEqual("1")
            .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP2VisibleCanBeModified() {
        inferParasByPara("P0", "0", "P2", "1");
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
        solutions(0)
            .assertPara("P0").valueEqual("0")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("1");
    }

    @Test
    public void testBothCannotBeHiddenSimultaneously() {
        inferParasByPara("P0", "0", "P0", "2");
        resultAssert().assertNoSolution();
    }
}