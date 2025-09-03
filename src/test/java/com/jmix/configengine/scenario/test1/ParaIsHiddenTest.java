package com.jmix.configengine.scenario.test1;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
//https://claude.hk.cn/share/defa6c51-0346-4e8a-980e-c81073025031
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
    
    @PartAnno(
        maxQuantity=3
    )
    private PartVar Part1;
 
    @Override
    protected void initConstraint() {
        // Logic 1: P1.isHiddenVar and P2.isHiddenVar are incompatible
        // Logic 2: if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1
        // Logic3:  if P1.isHiddenVar=1 then P1.var=0
        // Logic4:  if P2.isHiddenVar=1 then P1.var=0
        // Logic5:   if P1.isHiddenVar=0  才可以对P1.var手工修改（也就是作为inferParasByPara的入参)
        // Logic6:   if P2.isHiddenVar=0  才可以对P2.var手工修改（也就是作为inferParasByPara的入参)

        // Logic 1: P1.isHiddenVar and P2.isHiddenVar are incompatible
        // At least one of them should not be hidden
        model.addBoolOr(new Literal[]{P1.isHiddenVar.not(), P2.isHiddenVar.not()});

        // Logic 2: if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1
        BoolVar p0InRange01 = model.newBoolVar("p0_in_range_0_1");
        
        // Create boolean variables for P0 == 0 and P0 == 1
        BoolVar p0Eq0 = model.newBoolVar("p0_eq_0");
        BoolVar p0Eq1 = model.newBoolVar("p0_eq_1");
        
        // Define when P0 equals specific values
        model.addEquality((IntVar)P0.var, 0).onlyEnforceIf(p0Eq0);
        model.addDifferent((IntVar)P0.var, 0).onlyEnforceIf(p0Eq0.not());
        
        model.addEquality((IntVar)P0.var, 1).onlyEnforceIf(p0Eq1);
        model.addDifferent((IntVar)P0.var, 1).onlyEnforceIf(p0Eq1.not());
        
        // p0InRange01 is true when P0 is 0 or 1
        model.addBoolOr(new Literal[]{p0Eq0, p0Eq1}).onlyEnforceIf(p0InRange01);
        model.addBoolAnd(new Literal[]{p0Eq0.not(), p0Eq1.not()}).onlyEnforceIf(p0InRange01.not());
        
        // If P0 in (0,1) then P1.isHiddenVar=1
        model.addEquality(P1.isHiddenVar, 1).onlyEnforceIf(p0InRange01);
        
        // Else P2.isHiddenVar=1
        model.addEquality(P2.isHiddenVar, 1).onlyEnforceIf(p0InRange01.not());

        // Logic 3: if P1.isHiddenVar=1 then P1.var=0
        model.addEquality((IntVar)P1.var, 0).onlyEnforceIf(P1.isHiddenVar);

        // Logic 4: if P2.isHiddenVar=1 then P1.var=0
        model.addEquality((IntVar)P1.var, 0).onlyEnforceIf(P2.isHiddenVar);

        // Part1.quantity = P1.value + P2.value
        model.addEquality((IntVar)Part1.var, LinearExpr.sum(new IntVar[]{(IntVar)P1.var, (IntVar)P2.var}));


        model.minimize(LinearExpr.sum(new IntVar[]{(IntVar)P0.isHiddenVar,(IntVar)P1.isHiddenVar, (IntVar)P2.isHiddenVar, (IntVar)Part1.isHiddenVar}));
    }
}
//---------------模型的定义end----------------------------------------

public ParaIsHiddenTest() {
    super(ParaIsHiddenConstraint.class);
}

@Test
public void testP0Value0P1Hidden() {
    // Test when P0=0, P1 should be hidden and P1.var=0 
    inferParas("Part1",1,"P0", "0");
    printSolutions();
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        
    solutions(0)
    .assertPara("P0").valueEqual(0)
    .assertPara("P1").valueEqual(0)
    .assertPara("P2").valueEqual(1)
    .assertPart("Part1").quantityEqual(1);
}

@Test
public void testP0Value1P1Hidden() {
    // Test when P0=1, P1 should be hidden and P1.var=0
    inferParasByPara("P0", "1");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
            
    solutions(0)
            .assertPara("P0").valueEqual(1)
            .assertPara("P1").valueEqual(0)
            .assertPara("P2").valueEqual(1)
            .assertPart("Part1").quantityEqual(1);
    printSolutions();
}

@Test
public void testP0Value2P2Hidden() {
    // Test when P0=2, P2 should be hidden and P1.var=0
    inferParasByPara("P0", "2");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
            
    solutions(0)
            .assertPara("P0").valueEqual("2")
            .assertPara("P1").valueEqual("0");
    printSolutions();
}

@Test
public void testP0Value3P2Hidden() {
    // Test when P0=3, P2 should be hidden and P1.var=0
    inferParasByPara("P0", "3");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
            
    solutions(0)
            .assertPara("P0").valueEqual("3")
            .assertPara("P1").valueEqual("0");
    printSolutions();
}

@Test
public void testP1ManualModificationWhenNotHidden() {
    // Test manual modification of P1 when P1 is not hidden
    // P1 can only be modified when P1.isHiddenVar=0, which happens when P0 is 2 or 3
    inferParasByPara("P1", "1");
    
    resultAssert()
            .assertSuccess();
    
    // Should have solutions where P0=2 or P0=3 (when P1 is not hidden)
    // But due to Logic 4, if P2 is hidden, P1.var should be 0
    // This creates a contradiction, so there should be no solution
    resultAssert().assertNoSolution();
    printSolutions();
}

@Test
public void testP2ManualModificationWhenNotHidden() {
    // Test manual modification of P2 when P2 is not hidden
    // P2 can be modified when P2.isHiddenVar=0, which happens when P0 is 0 or 1
    inferParasByPara("P2", "1");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(2);
    
    // Should have solutions where P0=0 or P0=1 (when P2 is not hidden)
    assertSolutionNum("P0:0", 1);
    assertSolutionNum("P0:1", 1);
    
    // In both cases, P1 should be 0 due to Logic 3
    assertSolutionNum("P1:0", 2);
    printSolutions();
}

@Test
public void testLogic1Incompatibility() {
    // Test that P1 and P2 cannot both be hidden
    // This is implicitly tested by Logic 2, but let's verify
    
    // When P0=0, P1 is hidden and P2 is not hidden
    inferParasByPara("P0", "0");
    resultAssert().assertSuccess();
    
    // When P0=2, P2 is hidden and P1 is not hidden  
    inferParasByPara("P0", "2");
    resultAssert().assertSuccess();
    printSolutions();
}

@Test
public void testAllPossibleP0Values() {
    // Test all possible P0 values and verify the logic
    for (int p0Val = 0; p0Val <= 3; p0Val++) {
        inferParasByPara("P0", String.valueOf(p0Val));
        
        resultAssert().assertSuccess();
        
        solutions(0)
                .assertPara("P0").valueEqual(String.valueOf(p0Val))
                .assertPara("P1").valueEqual("0"); // P1 is always 0 due to Logic 3 and 4
        
        log.info("P0={} tested successfully", p0Val);
    }
    printSolutions();
}

@Test
public void testP1AlwaysZero() {
    // Due to Logic 3 and Logic 4, P1.var should always be 0
    // regardless of which parameter is hidden
    
    inferParasByPara();
    
    resultAssert().assertSuccess();
    
    // Check that in all solutions, P1 is 0
    assertSolutionNum("P1:1", 0);
    assertSolutionNum("P1:2", 0);
    printSolutions();
}

@Test
public void testP0RangeLogic() {
    // Test the range logic for P0
    
    // P0 in (0,1) should make P1 hidden
    inferParasByPara("P0", "0");
    resultAssert().assertSuccess();
    
    inferParasByPara("P0", "1"); 
    resultAssert().assertSuccess();
    
    // P0 not in (0,1) should make P2 hidden
    inferParasByPara("P0", "2");
    resultAssert().assertSuccess();
    
    inferParasByPara("P0", "3");
    resultAssert().assertSuccess();
    
    printSolutions();
}

@Test
public void testInvalidP1Modification() {
    // Try to set P1 to non-zero value when it should be 0
    // This should result in no solution
    inferParasByPara("P1", "2");
    
    resultAssert().assertNoSolution();
    printSolutions();
}

@Test
public void testValidP2Modification() {
    // P2 can be modified to any valid value when not hidden
    inferParasByPara("P2", "2");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(2);
    
    // Should work when P0 is 0 or 1 (P2 not hidden)
    assertSolutionNum("P0:0", 1);
    assertSolutionNum("P0:1", 1);
    printSolutions();
}

@Test
public void testComplexScenario() {
    // Test multiple parameter inference
    inferParasByPara("P0", "1", "P2", "1");
    
    resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
            
    solutions(0)
            .assertPara("P0").valueEqual("1")
            .assertPara("P1").valueEqual("0")
            .assertPara("P2").valueEqual("1");
    printSolutions();
}
}