package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 我的T恤衫测试类
 * 测试T恤衫模块的约束规则和参数推理功能
 * 
 * @since 2025-09-22
 */
@Slf4j
public class MyTShirtTest extends ModuleScenarioTestBase {
    /**
     * 构造MyTShirtTest测试类
     */
    public MyTShirtTest() {
        super(MyTShirtConstraint.class);
    }

    // ---------------start----------------------------------------
    /**
     * 我的T恤衫约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class MyTShirtConstraint extends ModuleAlgBase {
        @ParaAnno(options = { "op11", "op12", "op13" })
        private ParaVar p1Var;

        @ParaAnno(options = { "op21", "op22", "op23" })
        private ParaVar p2Var;

        @PartAnno
        private PartVar pt1Var;

        @CodeRuleAnno(normalNaturalCode = "if(p1Var.valueVar() == op11 && p2Var.valueVar() == op21) then { pt1Var.quantityVar() = 1; } "
                + "else { pt1Var.quantityVar() = 3; }")
        private void rule1() {
            // if(p1Var.valueVar() == op11 && p2Var.valueVar() == op21) {
            // pt1Var.quantityVar() = 1;
            // }
            // else {
            // pt1Var.quantityVar() = 3;
            // }

            AlgCPBoolVar condition = model().newBoolVar("condition");
            model().addBoolAnd(new AlgCPLiteral[] {
                    p1Var.option("op11").selectedVar(),
                    p2Var.option("op21").selectedVar()
            }).onlyEnforceIf(condition);

            model().addBoolOr(new AlgCPLiteral[] {
                    p1Var.option("op11").selectedVar().not(),
                    p2Var.option("op21").selectedVar().not()
            }).onlyEnforceIf(condition.not());

            model().addEquality(pt1Var.quantityVar(), 1).onlyEnforceIf(condition);
            model().addEquality(pt1Var.quantityVar(), 3).onlyEnforceIf(condition.not());
        }
    }
    // ---------------end----------------------------------------

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
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
        resultAssert().assertSuccess()
                .assertSolutionSizeEqual(0);
    }
}
