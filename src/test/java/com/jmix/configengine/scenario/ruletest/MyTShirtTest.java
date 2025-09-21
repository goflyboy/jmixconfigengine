package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.CodeRuleAnno;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.artifact.ConstraintAlgImpl;
import com.jmix.executor.impl.artifact.ParaVar;
import com.jmix.executor.impl.artifact.PartVar;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class MyTShirtTest extends ModuleScenarioTestBase {

    // ---------------start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class MyTShirtConstraint extends ConstraintAlgImpl {
        @ParaAnno(options = { "op11", "op12", "op13" })
        private ParaVar P1Var;

        @ParaAnno(options = { "op21", "op22", "op23" })
        private ParaVar P2Var;

        @PartAnno
        private PartVar PT1Var;

        @CodeRuleAnno(normalNaturalCode = "if(P1Var.value == op11 && P2Var.value == op21) then { PT1Var.qty = 1; } else { PT1Var.qty = 3; }")
        protected void rule1() {
            // if(P1Var.value == op11 && P2Var.value == op21) {
            // PT1Var.qty = 1;
            // }
            // else {
            // PT1Var.qty = 3;
            // }

            BoolVar condition = model.newBoolVar("condition");
            model.addBoolAnd(new Literal[] {
                    P1Var.getParaOptionByCode("op11").getIsSelectedVar(),
                    P2Var.getParaOptionByCode("op21").getIsSelectedVar()
            }).onlyEnforceIf(condition);

            model.addBoolOr(new Literal[] {
                    P1Var.getParaOptionByCode("op11").getIsSelectedVar().not(),
                    P2Var.getParaOptionByCode("op21").getIsSelectedVar().not()
            }).onlyEnforceIf(condition.not());

            model.addEquality((IntVar) PT1Var.qty, 1).onlyEnforceIf(condition);
            model.addEquality((IntVar) PT1Var.qty, 3).onlyEnforceIf(condition.not());
        }
    }
    // ---------------?????end----------------------------------------

    public MyTShirtTest() {
        super(MyTShirtConstraint.class);
    }

    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    @Test
    public void testIfCondition() {
        inferParas("PT1", 1);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("P1").valueEqual("op11")
                .assertPara("P2").valueEqual("op21");
    }

    @Test
    public void testElseCondition() {
        inferParas("PT1", 3);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);

        assertSolutionNum("P1:op11,P2:op21", 0);
    }

    @Test
    public void testMultipleParaInference() {
        inferParasByPara("P1", "op11", "P2", "op21");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0).assertPart("PT1").quantityEqual(1);
    }

    @Test
    public void testNoSolution() {
        inferParas("PT1", 5);
        resultAssert().assertNoSolution();
    }
}