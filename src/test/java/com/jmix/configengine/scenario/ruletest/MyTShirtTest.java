package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.tool.model.CodeRuleAnno;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ParaAnno;
import com.jmix.tool.model.PartAnno;

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
        private ParaVar p1Var;

        @ParaAnno(options = { "op21", "op22", "op23" })
        private ParaVar p2Var;

        @PartAnno
        private PartVar pt1Var;

        @CodeRuleAnno(normalNaturalCode = "if(p1Var.value == op11 && p2Var.value == op21) then { pt1Var.qty = 1; } else { pt1Var.qty = 3; }")
        protected void rule1() {
            // if(p1Var.value == op11 && p2Var.value == op21) {
            // pt1Var.qty = 1;
            // }
            // else {
            // pt1Var.qty = 3;
            // }

            BoolVar condition = model.newBoolVar("condition");
            model.addBoolAnd(new Literal[] {
                    p1Var.getParaOptionByCode("op11").getIsSelectedVar(),
                    p2Var.getParaOptionByCode("op21").getIsSelectedVar()
            }).onlyEnforceIf(condition);

            model.addBoolOr(new Literal[] {
                    p1Var.getParaOptionByCode("op11").getIsSelectedVar().not(),
                    p2Var.getParaOptionByCode("op21").getIsSelectedVar().not()
            }).onlyEnforceIf(condition.not());

            model.addEquality((IntVar) pt1Var.qty, 1).onlyEnforceIf(condition);
            model.addEquality((IntVar) pt1Var.qty, 3).onlyEnforceIf(condition.not());
        }
    }
    // ---------------?????end----------------------------------------

    /**
     * 构造MyTShirtTest测试类
     */
    public MyTShirtTest() {
        super(MyTShirtConstraint.class);
    }

    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试if条件分支
     */
    @Test
    public void testIfCondition() {
        inferParas("pt1", 1);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p1").valueEqual("op11")
                .assertPara("p2").valueEqual("op21");
    }

    /**
     * 测试else条件分支
     */
    @Test
    public void testElseCondition() {
        inferParas("pt1", 3);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);

        assertSolutionNum("p1:op11,p2:op21", 0);
    }

    /**
     * 测试多参数推理
     */
    @Test
    public void testMultipleParaInference() {
        inferParasByPara("p1", "op11", "p2", "op21");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0).assertPart("pt1").quantityEqual(1);
    }

    /**
     * 测试无解情况
     */
    @Test
    public void testNoSolution() {
        inferParas("pt1", 5);
        resultAssert().assertNoSolution();
    }
}