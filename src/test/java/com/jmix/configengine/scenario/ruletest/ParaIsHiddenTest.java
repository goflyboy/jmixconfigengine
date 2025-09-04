package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class ParaIsHiddenTest extends ModuleSecnarioTestBase {
    
    //---------------ģ�͵Ķ���start----------------------------------------
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
        
        @PartAnno(
			maxQuantity=3
		)
        private PartVar Part1;
        
        @Override
        protected void initConstraint() {
            // Logic1: P1.isHidden and P2.isHidden are incompatible
            model.addBoolOr(new Literal[]{P1.isHidden.not(), P2.isHidden.not()});
            addVarAboutHiddenConstraints(P1, P2);
            
            // Logic2: if P0.value in (0,1) then P1.isHidden=1 else P2.isHidden=1
            BoolVar p0Eq0 = model.newBoolVar("p0_eq_0");
            BoolVar p0Eq1 = model.newBoolVar("p0_eq_1");
            model.addEquality((IntVar)P0.value, 0).onlyEnforceIf(p0Eq0);
            model.addDifferent((IntVar)P0.value, 0).onlyEnforceIf(p0Eq0.not());
            model.addEquality((IntVar)P0.value, 1).onlyEnforceIf(p0Eq1);
            model.addDifferent((IntVar)P0.value, 1).onlyEnforceIf(p0Eq1.not());
            
            // p0In01 is true iff P0.value == 0 or P0.value == 1
            BoolVar p0In01 = model.newBoolVar("p0_in_01");
            model.addBoolOr(new Literal[]{p0Eq0, p0Eq1}).onlyEnforceIf(p0In01);
            model.addBoolAnd(new Literal[]{p0Eq0.not(), p0Eq1.not()}).onlyEnforceIf(p0In01.not());
            // Ensure not both p0Eq0 and p0Eq1 are true simultaneously
            model.addBoolOr(new Literal[]{p0Eq0.not(), p0Eq1.not()});
            
            model.addEquality(P1.isHidden, 1).onlyEnforceIf(p0In01);
            model.addEquality(P2.isHidden, 1).onlyEnforceIf(p0In01.not());

            // Logic3: if P1.isHidden=1 then P1.value=0
            model.addEquality((IntVar)P1.value, 0).onlyEnforceIf(P1.isHidden);
            
            // Logic4: if P2.isHidden=1 then P2.value=0
            model.addEquality((IntVar)P2.value, 0).onlyEnforceIf(P2.isHidden);
            
            // Logic5: Part1.quantity = P1.value + P2.value
            model.addEquality((IntVar)Part1.qty, LinearExpr.sum(new IntVar[]{(IntVar)P1.value, (IntVar)P2.value}));
        }
    }
    //---------------?????end----------------------------------------

    public ParaIsHiddenTest() {
        super(ParaIsHiddenConstraint.class);
    }

    @Test
    public void testP0Value0P1Hidden() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParas("Part1", 1, "P0", "0");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
            
        solutions(0)
        .assertPara("P0").valueEqual(0).hiddenEqual(false)
        .assertPara("P1").valueEqual(0).hiddenEqual(true)
        .assertPara("P2").valueEqual(1).hiddenEqual(false)
        .assertPara("P2").valueEqual(1).hiddenEqual(false)
        .assertPart("Part1").quantityEqual(1).hiddenEqual(false);
    }
    
    @Test
    public void testP0Value1P1Hidden() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParas("Part1", 1, "P0", "1");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
            
        solutions(0)
        .assertPara("P0").valueEqual(1).hiddenEqual(false)
        .assertPara("P1").valueEqual(0).hiddenEqual(true)
        .assertPara("P2").valueEqual(1).hiddenEqual(false)
        .assertPara("P2").valueEqual(1).hiddenEqual(false)
        .assertPart("Part1").quantityEqual(1).hiddenEqual(false);
    }
    @Test
    public void testP0Value2P2Hidden() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParas("Part1", 1, "P0", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
            
        solutions(0)
        .assertPara("P0").valueEqual(2).hiddenEqual(false)
        .assertPara("P1").valueEqual(1).hiddenEqual(false)
        .assertPara("P2").valueEqual(0).hiddenEqual(true)
        .assertPart("Part1").quantityEqual(1).hiddenEqual(false);
    }

    
    @Test
    public void testP2ToPart1() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParasByPara("P0", "1", "P2", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
            
        solutions(0)
        .assertPara("P0").valueEqual(1).hiddenEqual(false)
        .assertPara("P1").valueEqual(0).hiddenEqual(true)
        .assertPara("P2").valueEqual(2).hiddenEqual(false)
        .assertPart("Part1").quantityEqual(2).hiddenEqual(false);
    }
    @Test
    public void testP1ToPart1() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParasByPara("P0", "2", "P1", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
            
        solutions(0)
        .assertPara("P0").valueEqual(2).hiddenEqual(false)
        .assertPara("P1").valueEqual(2).hiddenEqual(false)
        .assertPara("P2").valueEqual(0).hiddenEqual(true)
        .assertPart("Part1").quantityEqual(2).hiddenEqual(false);
    }

    @Test
    public void testP1ToPart1InvalidInput() {
        // Test when P0=0, P1 should be hidden and P1.var=0
        inferParasByPara("P0", "2", "P1", "3");//超出P1值域
        printSolutions();
        resultAssert().assertNoSolution();
    
    }

}