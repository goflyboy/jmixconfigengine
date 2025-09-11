package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class ParaIsHiddenStandTest extends ModuleSecnarioTestBase {
    
    //---------------?????start----------------------------------------
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
        
        @CodeRuleAnno(normalNaturalCode = "P1.isHidden and P2.isHidden are incompatible")
        protected void Rule1() {
            // Add constraint: P1.isHidden and P2.isHidden cannot both be 1
            addVarAboutHiddenConstraints(P1, P2);
            model.addBoolOr(new Literal[]{P1.isHidden.not(), P2.isHidden.not()});
        }
        
        @CodeRuleAnno(normalNaturalCode = "if P0.value in (0,1) then P1.isHidden=1 else P2.isHidden=1")
        protected void Rule2() {
            // Create condition: P0.value in (0,1)
            BoolVar p0In01 = model.newBoolVar("p0_in_01");
            model.addEquality((IntVar)P0.value, 0).onlyEnforceIf(p0In01);
            model.addEquality((IntVar)P0.value, 1).onlyEnforceIf(p0In01);
            model.addDifferent((IntVar)P0.value, 0).onlyEnforceIf(p0In01.not());
            model.addDifferent((IntVar)P0.value, 1).onlyEnforceIf(p0In01.not());
            
            // If P0.value in (0,1), then P1.isHidden=1
            addVarAboutHiddenConstraints(P1);
            model.addEquality(P1.isHidden, 1).onlyEnforceIf(p0In01);
            
            // Else, P2.isHidden=1
            addVarAboutHiddenConstraints(P2);
            model.addEquality(P2.isHidden, 1).onlyEnforceIf(p0In01.not());
        }
        
        @CodeRuleAnno(normalNaturalCode = "if P1.isHidden=1 then P1.value=0")
        protected void Rule3() {
            // If P1.isHidden=1, then P1.value=0
            addVarAboutHiddenConstraints(P1);
            model.addEquality((IntVar)P1.value, 0).onlyEnforceIf(P1.isHidden);
        }
        
        @CodeRuleAnno(normalNaturalCode = "if P2.isHidden=1 then P2.value=0")
        protected void Rule4() {
            // If P2.isHidden=1, then P2.value=0
            addVarAboutHiddenConstraints(P2);
            model.addEquality((IntVar)P2.value, 0).onlyEnforceIf(P2.isHidden);
        }
        
        @CodeRuleAnno(normalNaturalCode = "if P1.isHidden=0 then P1.value can be manually modified")
        protected void Rule5() {
            // This rule is about manual modification logic, not a constraint
            // No constraint code needed for this rule
        }
        
        @CodeRuleAnno(normalNaturalCode = "if P2.isHidden=0 then P2.value can be manually modified")
        protected void Rule6() {
            // This rule is about manual modification logic, not a constraint
            // No constraint code needed for this rule
        }
    }
    //---------------?????end----------------------------------------

    public ParaIsHiddenStandTest() {
        super(ParaIsHiddenStandConstraint.class);
    }

    @Test
    public void testRule1IncompatibleHidden() {
        // Test Rule1: P1.isHidden and P2.isHidden cannot both be 1
        inferParasByPara("P0", "2", "P1", "1", "P2", "1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
    }
    
    @Test
    public void testRule2P0In01() {
        // Test Rule2: When P0.value in (0,1), P1.isHidden=1
        inferParasByPara("P0", "0");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0).assertPara("P1").isHiddenEqual("1");
    }
    
    @Test
    public void testRule2P0NotIn01() {
        // Test Rule2: When P0.value not in (0,1), P2.isHidden=1
        inferParasByPara("P0", "2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0).assertPara("P2").isHiddenEqual("1");
    }
    
    @Test
    public void testRule3P1Hidden() {
        // Test Rule3: When P1.isHidden=1, P1.value=0
        inferParasByPara("P1", "1", "P1.isHidden", "1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0).assertPara("P1").valueEqual("0");
    }
    
    @Test
    public void testRule4P2Hidden() {
        // Test Rule4: When P2.isHidden=1, P2.value=0
        inferParasByPara("P2", "1", "P2.isHidden", "1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0).assertPara("P2").valueEqual("0");
    }
    
    @Test
    public void testCombinedRules() {
        // Test combined effect of all rules
        inferParasByPara("P0", "0");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P0").valueEqual("0")
                .assertPara("P1").isHiddenEqual("1")
                .assertPara("P1").valueEqual("0")
                .assertPara("P2").isHiddenEqual("0");
    }
    
    @Test
    public void testManualModificationWhenNotHidden() {
        // Test that P1 and P2 can be manually modified when not hidden
        inferParasByPara("P0", "2", "P1", "1", "P2", "2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        
        solutions(0)
                .assertPara("P1").valueEqual("1")
                .assertPara("P2").valueEqual("2");
    }
}