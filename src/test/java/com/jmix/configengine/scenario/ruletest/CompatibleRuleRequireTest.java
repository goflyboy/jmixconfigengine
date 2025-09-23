package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ParaAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import java.util.Arrays;

/**
 * 兼容性规则测试类
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CompatibleRuleRequireTest extends ModuleScenarioTestBase {

    /**
     * 构造CompatibleRuleRequireTest测试类
     */
    public CompatibleRuleRequireTest() {
        super(CompatibleRuleRequireConstraint.class);
    }

    // ---------------规则定义start----------------------------------------
    /**
     * 兼容性规则要求约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CompatibleRuleRequireConstraint extends ConstraintAlgImpl {
        @ParaAnno(options = { "a1", "a2", "a3", "a4" })
        private ParaVar aVar;

        @ParaAnno(options = { "b1", "b2", "b3", "b4" })
        private ParaVar bVar;

        @Override
        protected void initConstraint() {
            // natural code: aVar.value in (a1,a3) Requires bVar.value in (b1,b2,b3)
            // A=(a1,a2,a3,a4)
            // B=(b1,b2,b3,b4)
            // 规则内容：(a1,a3) Requires (b1,b2,b3)，则CA=(a1,a3),CB=(b1,b2,b3)
            // 解读：
            // 1、正向
            // 1.1 在CA中，如果aVar.var=a1,则bVar.var=b1 或 b2 或 b3
            // 1.2 不在CA中，如果aVar.var=a1,则bVar.var=b4，是不合法的
            // 2.反向
            // 2.1 在CB中，如果bVar.var=b1,则aVar.var=a1 或 a2 或 a3 或 a4
            // 2.2 不在CB中，如果bVar.var=b4,则aVar.var=a2 或 a4
            addCompatibleConstraintRequires("rule1", aVar, Arrays.asList("a1", "a3"), bVar,
                    Arrays.asList("b1", "b2", "b3"));
        }

        /**
         * 初始化约束2,参考
         */
        protected void initConstraintNote() {
            // 创建条件变量：A是a1或a3
            BoolVar a1OrA3 = model.newBoolVar("a1OrA3");
            model.addBoolOr(new Literal[] {
                    aVar.getParaOptionByCode("a1").getIsSelectedVar(),
                    aVar.getParaOptionByCode("a3").getIsSelectedVar()
            }).onlyEnforceIf(a1OrA3);
            model.addBoolAnd(new Literal[] {
                    aVar.getParaOptionByCode("a1").getIsSelectedVar().not(),
                    aVar.getParaOptionByCode("a3").getIsSelectedVar().not()
            }).onlyEnforceIf(a1OrA3.not());

            // 创建条件变量：B是b1、b2或b3
            BoolVar b1OrB2OrB3 = model.newBoolVar("b1OrB2OrB3");
            model.addBoolOr(new Literal[] {
                    bVar.getParaOptionByCode("b1").getIsSelectedVar(),
                    bVar.getParaOptionByCode("b2").getIsSelectedVar(),
                    bVar.getParaOptionByCode("b3").getIsSelectedVar()
            }).onlyEnforceIf(b1OrB2OrB3);
            model.addBoolAnd(new Literal[] {
                    bVar.getParaOptionByCode("b1").getIsSelectedVar().not(),
                    bVar.getParaOptionByCode("b2").getIsSelectedVar().not(),
                    bVar.getParaOptionByCode("b3").getIsSelectedVar().not()
            }).onlyEnforceIf(b1OrB2OrB3.not());

            // 添加约束：如果A是a1或a3，则B必须是b1、b2或b3
            model.addImplication(a1OrA3, b1OrB2OrB3);

            // 注意：反向约束不需要显式添加，因为这是"Requires"关系的单向约束
            // 反向推理的结果是正向约束的自然结果
        }
    }

    // ---------------规则定义end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试A1要求B1、B2、B3
     */
    @Test
    public void testA1RequiresB1B2B3() {
        // 测试正向规则1.1: 如果A选择a1，则B必须是b1、b2或b3
        inferParasByPara("a", "a1");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择

        // 验证所有解中B都是b1、b2或b3
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 验证B=b4的解不存在
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 测试A3要求B1、B2、B3
     */
    @Test
    public void testA3RequiresB1B2B3() {
        // 测试正向规则1.1: 如果A选择a3，则B必须是b1、b2或b3
        inferParasByPara("a", "a3");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择

        // 验证所有解中B都是b1、b2或b3
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 验证B=b4的解不存在
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 测试A1与B4组合无效
     */
    @Test
    public void testA1WithB4IsInvalid() {
        // 测试正向规则1.2: 如果A选择a1，则B选择b4是不合法的
        // 设置A=a1且B=b4，应该无解
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 测试B1允许任意A
     */
    @Test
    public void testB1AllowsAnyA() {
        // 测试反向规则2.1: 如果B选择b1，则A可以是a1、a2、a3或a4
        inferParasByPara("b", "b1");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // A有4种选择

        // 验证所有解中A可以是任意值
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a3", 1);
        assertSolutionNum("a:a4", 1);
    }

    /**
     * 测试B4只允许A2、A4
     */
    @Test
    public void testB4AllowsA2A4Only() {
        // 测试反向规则2.2: 如果B选择b4，则A只能是a2或a4
        inferParasByPara("b", "b4");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // A有2种选择

        // 验证A只能是a2或a4
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);

        // 验证A不能是a1或a3
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 测试A2、A4允许任意B
     */
    @Test
    public void testA2A4AllowAnyB() {
        // 测试额外情况: A选择a2或a4时，B可以是任意值
        inferParasByPara("a", "a2");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // B有4种选择

        // 验证所有解中B可以是任意值
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);
        assertSolutionNum("b:b4", 1);

        // 同样测试a4
        inferParasByPara("a", "a4");

        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // B有4种选择
    }
}