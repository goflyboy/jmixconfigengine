package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ParaAnno;
import com.jmix.tool.model.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class CalculateRuleIfThenTest extends ModuleScenarioTestBase {
    /**
     * 构造CalculateRuleIfThenTest测试类
     */
    public CalculateRuleIfThenTest() {
        super(CalculateRuleConstraint.class);
    }

    // ---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    public static class CalculateRuleConstraint extends ConstraintAlgImpl {

        @ParaAnno(defaultValue = "op11", options = { "op11", "op12", "op13" })
        private ParaVar p1Var;

        @ParaAnno(defaultValue = "op21", options = { "op21", "op22", "op23" })
        private ParaVar p2Var;

        @PartAnno
        private PartVar pt1Var;

        @Override
        protected void initConstraint() {
            addConstraintRule2();
        }

        /**
         * 添加约束规则2
         */
        public void addConstraintRule2() {

            // if(p1Var.value ==op11 && p2Var.value == op21 ) {
            // pt1Var.qty = 1;
            // }
            // else {
            // pt1Var.qty = 3
            // }

            // 创建条件变量：颜色是红色且尺寸是小号
            BoolVar op11Andop21 = model.newBoolVar("rule2_op11Andop21");

            // 实现条件逻辑：redAndSmall = (p1== op11) AND (p2 == op21)
            model.addBoolAnd(new Literal[] {
                    this.p1Var.getParaOptionByCode("op11").getIsSelectedVar(),
                    this.p2Var.getParaOptionByCode("op21").getIsSelectedVar()
            }).onlyEnforceIf(op11Andop21);

            // 如果不是红色且小号的组合，则op11Andop2为false
            model.addBoolOr(new Literal[] {
                    this.p1Var.getParaOptionByCode("op11").getIsSelectedVar().not(),
                    this.p2Var.getParaOptionByCode("op21").getIsSelectedVar().not()
            }).onlyEnforceIf(op11Andop21.not());

            // 根据条件设置pt1Var的数量
            // 如果op11Andop2为true，则pt1Var数量为1
            model.addEquality(pt1Var.qty, 1).onlyEnforceIf(op11Andop21);

            // 如果op11Andop2为false，则pt1Var数量为3
            model.addEquality(pt1Var.qty, 3).onlyEnforceIf(op11Andop21.not());
        }
    }

    // ---------------规则定义end----------------------------------------
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试规则2的if条件（op11和op21）
     */
    @Test
    public void testRule2IfOp11Op21() {
        // 测试颜色参数推理
        inferParas("pt1", 1);

        // 验证第一个解
        solutions(0).assertPara("p1").valueEqual("op11")
                .assertPara("p2").valueEqual("op21");
        printSolutions();
    }

    /**
     * 测试规则2的else条件（op11和op21）
     */
    @Test
    public void testRule2ElseOp11Op21() {
        inferParas("pt1", 3);
        printSolutions();
        // 反推有8个接口 8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);
        // if的解肯定不在其中
        assertSolutionNum("p1:op11,p2:op21", 0);
        // else的解可能解如下：
        assertSolutionNum("p1:op12,p2:op22", 1);
        printSolutions();
    }

    /**
     * 测试规则2的无if-else情况
     */
    @Test
    public void testRule2NoIfElse() {
        inferParas("pt1", 4);
        printSolutions();
        // 反推有8个接口 8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
        // if的解肯定不在其中
        assertSolutionNum("p1:op11,p2:op21", 0);
        // else的解也不在其中
        assertSolutionNum("p1:op12,p2:op22", 0);
        printSolutions();
    }

}