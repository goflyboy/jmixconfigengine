package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * 不兼容性规则测试类
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CompatibleRuleIncompatibleTest extends ModuleScenarioTestBase {
    // ---------------规则定义start----------------------------------------
    /**
     * 不兼容性规则约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CompatibleRuleIncompatibleConstraint extends ConstraintAlgImplTestBase {

        @ParaAnno(options = { "a1", "a2", "a3", "a4", "a5" })
        private ParaVar aVar;

        @ParaAnno(options = { "b1", "b2", "b3", "b4", "b5" })
        private ParaVar bVar;

        @CodeRuleAnno()
        private void initConstraint() {
            // 注意： 一定要选择一个值怎么理解（如果不选择，也设置一个可选值就可以表达，这样模型统一了）
            // A=(a1,a2,a3,a4,a5),aVar.value的值域
            // B=(b1,b2,b3,b4,b5),bVar.value的值域
            // 规则内容：(a1,a3) Incompatible
            // (b1,b2,b3)，则CA=(a1,a3),CB=(b1,b2,b3),NCA=(a2,a4,a5),NCB=(b4,b5)
            // 解读：
            // 1、正向
            // 1.1
            // 如果aVar.value取CA的一个值(在CA中)，则bVar.value一定不是CB的一个值，取CB内的值是非法的，只能是NCB内的值，如:如果aVar.var=a1,则bVar.var=b4或b5(NCB),不可以是b1
            // 或 b2 或 b3(CB)
            // 1.2
            // 如果aVar.value取NCA一个值(不在CA中)，则bVar.value可以是CB的一个值，也可以是NCB的一个值，如：如果aVar.var=a2,则bVar.var=b1或b2或b3(CB),也可以是b4或b5(NCB)
            // 2.反向
            // 2.1
            // 如果bVar.value取CB的一个值(在CB中)，则aVar.value一定不是CA的一个值，取CA内的值是非法的，只能是NCA内的值，如:如果bVar.var=b1,则aVar.var=a2或a4或a5(NCA),不可以是a1
            // 或 a3(CA)
            // 2.2
            // 如果bVar.value取NCB一个值(不在CB中)，则aVar.value可以是CA的一个值，也可以是NCA的一个值，如：如果bVar.var=b4,则aVar.var=a1
            // 或 a3(CA),也可以是a2 或 a4或a5(NCA)
            // 使用泛化的Incompatible约束方法
            addCompatibleConstraintInCompatible("rule1", aVar, Arrays.asList("a1", "a3"), bVar,
                    Arrays.asList("b1", "b2", "b3"));
        }

        /**
         * 添加兼容性规则：Incompatible关系约束, 参考样例
         * 
         */
        public void addCompatibleConstraintIncompatibleNote() {
            // 创建条件变量：A在CA中
            BoolVar inCA = model.newBoolVar("inCA");
            model.addBoolOr(new Literal[] {
                    aVar.getParaOptionByCode("a1").getIsSelectedVar(),
                    aVar.getParaOptionByCode("a3").getIsSelectedVar()
            }).onlyEnforceIf(inCA);
            model.addBoolAnd(new Literal[] {
                    aVar.getParaOptionByCode("a1").getIsSelectedVar().not(),
                    aVar.getParaOptionByCode("a3").getIsSelectedVar().not()
            }).onlyEnforceIf(inCA.not());

            // 创建条件变量：B在CB中
            BoolVar inCB = model.newBoolVar("inCB");
            model.addBoolOr(new Literal[] {
                    bVar.getParaOptionByCode("b1").getIsSelectedVar(),
                    bVar.getParaOptionByCode("b2").getIsSelectedVar(),
                    bVar.getParaOptionByCode("b3").getIsSelectedVar()
            }).onlyEnforceIf(inCB);
            model.addBoolAnd(new Literal[] {
                    bVar.getParaOptionByCode("b1").getIsSelectedVar().not(),
                    bVar.getParaOptionByCode("b2").getIsSelectedVar().not(),
                    bVar.getParaOptionByCode("b3").getIsSelectedVar().not()
            }).onlyEnforceIf(inCB.not());

            // 创建条件变量：A不在CA中
            BoolVar notInCA = model.newBoolVar("notInCA");
            model.addBoolOr(new Literal[] {
                    aVar.getParaOptionByCode("a2").getIsSelectedVar(),
                    aVar.getParaOptionByCode("a4").getIsSelectedVar(),
                    aVar.getParaOptionByCode("a5").getIsSelectedVar()
            }).onlyEnforceIf(notInCA);
            model.addBoolAnd(new Literal[] {
                    aVar.getParaOptionByCode("a2").getIsSelectedVar().not(),
                    aVar.getParaOptionByCode("a4").getIsSelectedVar().not(),
                    aVar.getParaOptionByCode("a5").getIsSelectedVar().not()
            }).onlyEnforceIf(notInCA.not());

            // 创建条件变量：B不在CB中
            BoolVar notInCB = model.newBoolVar("notInCB");
            model.addBoolOr(new Literal[] {
                    bVar.getParaOptionByCode("b4").getIsSelectedVar(),
                    bVar.getParaOptionByCode("b5").getIsSelectedVar()
            }).onlyEnforceIf(notInCB);
            model.addBoolAnd(new Literal[] {
                    bVar.getParaOptionByCode("b4").getIsSelectedVar().not(),
                    bVar.getParaOptionByCode("b5").getIsSelectedVar().not()
            }).onlyEnforceIf(notInCB.not());

            // Incompatible 双向约束：
            // 正向1.1：如果A在CA中，则B必须不在CB中（必须在NCB中）
            model.addImplication(inCA, notInCB);

            // 正向1.2：如果A不在CA中，则B可以在CB中或NCB中（无约束）
            // 这个约束不需要显式添加，因为默认允许

            // 反向2.1：如果B在CB中，则A必须不在CA中（必须在NCA中）
            model.addImplication(inCB, notInCA);

            // 反向2.2：如果B不在CB中，则A可以在CA中或NCA中（无约束）
            // 这个约束不需要显式添加，因为默认允许
        }
    }
    // ---------------规则定义end----------------------------------------

    /**
     * 构造CompatibleRuleIncompatibleTest测试类
     */
    public CompatibleRuleIncompatibleTest() {
        super(CompatibleRuleIncompatibleConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 测试在CA中要求不在CB中
     */
    @Test
    public void testInCARequiresNotInCB() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B必须不在CB中(b4或b5)
        inferParasByPara("a", "a1");

        // B有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证所有解中B都在NCB中
        for (int i = 0; i < 2; i++) {
            solutions(i).assertPara("b").valueIn("b4", "b5");
        }

        // 验证B在CB中的解不存在
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试在CA中要求不在CB中（a3情况）
     */
    @Test
    public void testInCARequiresNotInCBA3() {
        // 测试正向规则1.1: 如果A在CA中(a3)，则B必须不在CB中(b4或b5)
        inferParasByPara("a", "a3");

        // B有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证所有解中B都在NCB中
        for (int i = 0; i < 2; i++) {
            solutions(i).assertPara("b").valueIn("b4", "b5");
        }

        // 验证B在CB中的解不存在
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试在CA中与在CB中组合无效
     */
    @Test
    public void testInCAWithInCBIsInvalid() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B在CB中(b1、b2或b3)是不合法的
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试不在CA中允许任意B
     */
    @Test
    public void testNotInCAAllowsAnyB() {
        // 测试正向规则1.2: 如果A不在CA中(a2)，则B可以是CB或NCB中的任意值
        inferParasByPara("a", "a2");

        // B有5种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 验证B可以在CB中
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 验证B也可以在NCB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 测试不在CA中允许任意B（a4情况）
     */
    @Test
    public void testNotInCAAllowsAnyBA4() {
        // 测试正向规则1.2: 如果A不在CA中(a4)，则B可以是CB或NCB中的任意值
        inferParasByPara("a", "a4");

        // B有5种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 验证B可以在CB中
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 验证B也可以在NCB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 测试不在CA中允许任意B（a5情况）
     */
    @Test
    public void testNotInCAAllowsAnyBA5() {
        // 测试正向规则1.2: 如果A不在CA中(a5)，则B可以是CB或NCB中的任意值
        inferParasByPara("a", "a5");

        // B有5种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 验证B可以在CB中
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 验证B也可以在NCB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 测试在CB中要求不在CA中
     */
    @Test
    public void testInCBRequiresNotInCA() {
        // 测试反向规则2.1: 如果B在CB中(b1)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("b", "b1");

        // A有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证A在NCA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 验证A在CA中的解不存在
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 测试在CB中要求不在CA中（b2、b3情况）
     */
    @Test
    public void testInCBRequiresNotInCAB2B3() {
        // 测试反向规则2.1: 如果B在CB中(b2或b3)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("b", "b2");

        // A有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证A在NCA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 验证A在CA中的解不存在
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 测试不在CB中允许任意A
     */
    @Test
    public void testNotInCBAllowsAnyA() {
        // 测试反向规则2.2: 如果B不在CB中(b4)，则A可以是CA或NCA中的任意值
        inferParasByPara("b", "b4");

        // A有5种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 验证A可以在CA中
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 验证A也可以在NCA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }

    /**
     * 测试不在CB中允许任意A（b5情况）
     */
    @Test
    public void testNotInCBAllowsAnyAB5() {
        // 测试反向规则2.2: 如果B不在CB中(b5)，则A可以是CA或NCA中的任意值
        inferParasByPara("b", "b5");

        // A有5种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 验证A可以在CA中
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 验证A也可以在NCA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }

    /**
     * 测试Incompatible双向约束
     */
    @Test
    public void testIncompatibleBidirectionalConstraint() {
        // 测试Incompatible双向约束的完整性
        // 验证CA和CB组之间的双向不兼容关系

        // 测试1: A在CA中时，B必须不在CB中
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 测试2: A不在CA中时，B可以在CB中或NCB中
        inferParasByPara("a", "a2");
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 测试3: B在CB中时，A必须不在CA中
        inferParasByPara("b", "b1");
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 测试4: B不在CB中时，A可以在CA中或NCA中
        inferParasByPara("b", "b4");
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }
}