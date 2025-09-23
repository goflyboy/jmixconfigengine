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
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class CalculateRuleSimpleTest extends ModuleScenarioTestBase {

    // ---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CalculateRuleConstraint extends ConstraintAlgImpl {

        @ParaAnno(options = { "op11", "op12", "op13" })
        private ParaVar p1Var;

        @PartAnno
        private PartVar pt1Var;

        @Override
        protected void initConstraint() {
            addConstraint_rule1();
        }

        /**
         * 添加约束规则1
         */
        public void addConstraint_rule1() {
            // if(p1Var.value == op11) {
            // pt1Var.qty = 1;
            // }
            // else {
            // pt1Var.qty = 3;
            // }

            // 创建条件变量：p1是op11
            BoolVar rule1_op11 = model.newBoolVar("rule1_op11");

            // 实现条件逻辑：rule1_op11 = (p1 == op11)
            model.addBoolAnd(new Literal[] {
                    this.p1Var.getParaOptionByCode("op11").getIsSelectedVar()
            }).onlyEnforceIf(rule1_op11);

            // 如果p1不是op11，则rule1_op11为false
            model.addBoolOr(new Literal[] {
                    this.p1Var.getParaOptionByCode("op11").getIsSelectedVar().not()
            }).onlyEnforceIf(rule1_op11.not());

            // 根据条件设置pt1Var的数量
            // 如果rule1_op11为true，则pt1Var数量为1
            model.addEquality((IntVar) this.pt1Var.qty, 1).onlyEnforceIf(rule1_op11);

            // 如果rule1_op11为false，则pt1Var数量为3
            model.addEquality((IntVar) this.pt1Var.qty, 3).onlyEnforceIf(rule1_op11.not());
        }
    }
    // ---------------规则定义end----------------------------------------

    /**
     * 构造CalculateRuleSimpleTest测试类
     */
    public CalculateRuleSimpleTest() {
        super(CalculateRuleConstraint.class);
    }

    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试if条件（op11）
     */
    @Test
    public void test_if_op11() {
        // 测试颜色参数推理
        inferParas("pt1", 1);

        // 验证第一个解
        solutions(0).assertPara("p1").valueEqual("op11");
        printSolutions();
    }

    /**
     * 测试else条件（op11）
     */
    @Test
    public void test_else_op11() {
        inferParas("pt1", 3);
        printSolutions();
        // 反推有8个接口 8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);
        // if的解肯定不在其中
        assertSolutionNum("p1:op11", 0);
        printSolutions();
    }

    /**
     * 测试无if-else情况
     */
    @Test
    public void test_no_if_else() {
        inferParas("pt1", 4);
        printSolutions();
        // 反推有8个接口 8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
        // if的解肯定不在其中
        assertSolutionNum("p1:op11", 0);
        printSolutions();
    }

}