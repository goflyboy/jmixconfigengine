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
        
        @CodeRuleAnno(normalNaturalCode = "P1.isHiddenVar and P2.isHiddenVar are incompatible")
        protected void Rule1() {
            // Ensure P1 and P2 cannot both be hidden at the same time
            addVarAboutHiddenConstraints(P1, P2);
            model.addBoolOr(new Literal[]{P1.isHidden.not(), P2.isHidden.not()});
        }

        @CodeRuleAnno(normalNaturalCode = "if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1")
        protected void Rule2() {
            // Create condition: P0.value is 0 or 1
            BoolVar p0Is0Or1 = model.newBoolVar("p0_0_or_1");
            model.addEquality((IntVar)P0.value, 0).onlyEnforceIf(p0Is0Or1);
            model.addEquality((IntVar)P0.value, 1).onlyEnforceIf(p0Is0Or1);
            model.addDifferent((IntVar)P0.value, 0).onlyEnforceIf(p0Is0Or1.not());
            model.addDifferent((IntVar)P0.value, 1).onlyEnforceIf(p0Is0Or1.not());

            // If P0 is 0 or 1, then P1 is hidden, else P2 is hidden
            model.addEquality(P1.isHidden, 1).onlyEnforceIf(p0Is0Or1);
            model.addEquality(P2.isHidden, 1).onlyEnforceIf(p0Is0Or1.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P1.isHiddenVar=1 then P1.var=0")
        protected void Rule3() {
            // If P1 is hidden, force P1.value to 0
            model.addEquality((IntVar)P1.value, 0).onlyEnforceIf(P1.isHidden);
        }

        @CodeRuleAnno(normalNaturalCode = "if P2.isHiddenVar=1 then P2.var=0")
        protected void Rule4() {
            // If P2 is hidden, force P2.value to 0
            model.addEquality((IntVar)P2.value, 0).onlyEnforceIf(P2.isHidden);
        }

        @CodeRuleAnno(normalNaturalCode = "if P1.isHiddenVar=0 then P1.var can be manually modified")
        protected void Rule5() {
            // This rule is about inference input conditions, not constraint
            // No constraint code needed as it's handled by inference method logic
        }

        @CodeRuleAnno(normalNaturalCode = "if P2.isHiddenVar=0 then P2.var can be manually modified")
        protected void Rule6() {
            // This rule is about inference input conditions, not constraint
            // No constraint code needed as it's handled by inference method logic
        }
    }
    //---------------?????end----------------------------------------

    public ParaIsHiddenStandTest() {
        super(ParaIsHiddenStandConstraint.class);
    }

    @Test
    public void testRule1IncompatibleHidden() {
        // Test Rule1: P1 and P2 cannot both be hidden
        inferParasByPara("P0", "2", "P1", "0", "P2", "0");
        resultAssert().assertSuccess();
    }

    @Test
    public void testRule2P0Condition() {
        // Test Rule2: P0 value determines which parameter is hidden
        inferParasByPara("P0", "0");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual(true);
        solutions(0).assertPara("P2").isHiddenEqual(false);

        inferParasByPara("P0", "1");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual(true);
        solutions(0).assertPara("P2").isHiddenEqual(false);

        inferParasByPara("P0", "2");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual(false);
        solutions(0).assertPara("P2").isHiddenEqual(true);

        inferParasByPara("P0", "3");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").isHiddenEqual(false);
        solutions(0).assertPara("P2").isHiddenEqual(true);
    }

    @Test
    public void testRule3P1HiddenZero() {
        // Test Rule3: If P1 is hidden, its value must be 0
        inferParasByPara("P0", "0"); // This makes P1 hidden
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").valueEqual("0");
    }

    @Test
    public void testRule4P2HiddenZero() {
        // Test Rule4: If P2 is hidden, its value must be 0
        inferParasByPara("P0", "2"); // This makes P2 hidden
        resultAssert().assertSuccess();
        solutions(0).assertPara("P2").valueEqual("0");
    }

    @Test
    public void testP1ManualModificationWhenNotHidden() {
        // Test Rule5: P1 can be manually modified only when not hidden
        inferParasByPara("P0", "2", "P1", "1"); // P0=2 makes P1 not hidden
        resultAssert().assertSuccess();
        solutions(0).assertPara("P1").valueEqual("1");
    }

    @Test
    public void testP2ManualModificationWhenNotHidden() {
        // Test Rule6: P2 can be manually modified only when not hidden
        inferParasByPara("P0", "0", "P2", "2"); // P0=0 makes P2 not hidden
        resultAssert().assertSuccess();
        solutions(0).assertPara("P2").valueEqual("2");
    }

    @Test
    public void testComplexScenario() {
        // Test comprehensive scenario with multiple constraints
        inferParasByPara("P0", "1", "P1", "0", "P2", "0");
        resultAssert().assertSuccess();
        solutions(0).assertPara("P0").valueEqual("1");
        solutions(0).assertPara("P1").valueEqual("0");
        solutions(0).assertPara("P2").valueEqual("0");
        solutions(0).assertPara("P1").isHiddenEqual(true);
        solutions(0).assertPara("P2").isHiddenEqual(false);
    }
}