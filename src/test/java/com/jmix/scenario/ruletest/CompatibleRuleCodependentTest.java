package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.anno.CodeRuleAnno;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.ParaAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * 兼容性规则测试类
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CompatibleRuleCodependentTest extends ModuleScenarioTestBase {
    // ---------------规则定义start----------------------------------------
    /**
     * 兼容性规则约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CompatibleRuleCodependentConstraint extends ConstraintAlgImplTestBase {
        @ParaAnno(options = { "a1", "a2", "a3", "a4", "a5" })
        private ParaVar aVar;

        @ParaAnno(options = { "b1", "b2", "b3", "b4", "b5" })
        private ParaVar bVar;

        @CodeRuleAnno
        private void initConstraint() {
            // A=(a1,a2,a3,a4,a5),aVar.value的值域
            // B=(b1,b2,b3,b4,b5),bVar.value的值域
            // 规则内容：(a1,a3) CoDependent
            // (b1,b2,b3)，则CA=(a1,a3),CB=(b1,b2,b3),NCA=(a2,a4,a5),NCB=(b4,b5)
            // 解读：
            // 1、正向
            // 1.1
            // 如果aVar.value取CA的一个值(在CA中)，则bVar.value一定取在CB的一个值(在CB中)，取NCB内的值是非法的，如:如果aVar.var=a1,则bVar.var=b1
            // 或 b2 或 b3,不可以是b4或b5
            // 1.2
            // 如果aVar.value取NCA一个值(不在CA中)，则bVar.value一定取NCB的一个值(不在CB中)，取CB内的值是非法的，如：如果aVar.var=a2,则bVar.var=b4或b5，不可以是b1或b2或b3
            // 2.反向
            // 2.1
            // 如果bVar.value取CB的一个值(在CB中)，则aVar.value一定取在CA的一个值(在CA中)，取NCA内的值是非法的，如:如果bVar.var=b1,则aVar.var=a1
            // 或 a3,不可以是a2或a4或a5
            // 2.2
            // 如果bVar.value取CB外的一个值(不在CB中)，则aVar.value一定取CA外的一个值(不在CA中)，取CA内的值是非法的，如：如果bVar.var=b4,则aVar.var=a2
            // 或 a4或a5
            addCompatibleConstraintCoDependent("rule1", aVar, Arrays.asList("a1", "a3"), bVar,
                    Arrays.asList("b1", "b2", "b3"));

        }

        /**
         * 添加兼容性规则：CoDependent关系约束
         */
        public void addCompatibleConstraintCoDependentNote() {
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

            // Codependent 双向约束：
            // 正向1.1：如果A在CA中，则B必须在CB中
            model.addImplication(inCA, inCB);

            // 正向1.2：如果A不在CA中，则B必须不在CB中
            model.addImplication(notInCA, notInCB);

            // 反向2.1：如果B在CB中，则A必须在CA中
            model.addImplication(inCB, inCA);

            // 反向2.2：如果B不在CB中，则A必须不在CA中
            model.addImplication(notInCB, notInCA);
        }
    }
    // ---------------规则定义end----------------------------------------

    /**
     * 构造CompatibleRuleCodependentTest测试类
     */
    public CompatibleRuleCodependentTest() {
        super(CompatibleRuleCodependentConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试在CA中要求在CB中
     */
    @Test
    public void testInCARequiresInCB() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B必须在CB中(b1、b2或b3)
        inferParasByPara("a", "a1");

        // B有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证所有解中B都在CB中
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 验证B不在CB中的解不存在
        assertSolutionNum("b:b4", 0);
        assertSolutionNum("b:b5", 0);
        printSolutions();
    }

    /**
     * 测试在CA中要求在CB中（a3情况）
     */
    @Test
    public void testInCARequiresInCBA3() {
        // 测试正向规则1.1: 如果A在CA中(a3)，则B必须在CB中(b1、b2或b3)
        inferParasByPara("a", "a3");

        // B有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证所有解中B都在CB中
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 验证B不在CB中的解不存在
        assertSolutionNum("b:b4", 0);
        assertSolutionNum("b:b5", 0);
    }

    /**
     * 测试在CA中但不在CB中是无效的
     */
    @Test
    public void testInCAWithNotInCBIsInvalid() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B不在CB中(b4或b5)是不合法的
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b4", 0);
        assertSolutionNum("b:b5", 0);
    }

    /**
     * 测试不在CA中要求不在CB中
     */
    @Test
    public void testNotInCARequiresNotInCB() {
        // 测试正向规则1.2: 如果A不在CA中(a2)，则B必须不在CB中(b4或b5)
        inferParasByPara("a", "a2");

        // B有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证B不在CB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 验证B在CB中的解不存在
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试不在CA中要求不在CB中（a4情况）
     */
    @Test
    public void testNotInCARequiresNotInCBA4() {
        // 测试正向规则1.2: 如果A不在CA中(a4)，则B必须不在CB中(b4或b5)
        inferParasByPara("a", "a4");

        // B有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证B不在CB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 验证B在CB中的解不存在
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试不在CA中要求不在CB中（a5情况）
     */
    @Test
    public void testNotInCARequiresNotInCBA5() {
        // 测试正向规则1.2: 如果A不在CA中(a5)，则B必须不在CB中(b4或b5)
        inferParasByPara("a", "a5");

        // B有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证B不在CB中
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 验证B在CB中的解不存在
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 测试在CB中要求在CA中
     */
    @Test
    public void testInCBRequiresInCA() {
        // 测试反向规则2.1: 如果B在CB中(b1)，则A必须在CA中(a1或a3)
        inferParasByPara("b", "b1");

        // A有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证A在CA中
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 验证A不在CA中的解不存在
        assertSolutionNum("a:a2", 0);
        assertSolutionNum("a:a4", 0);
        assertSolutionNum("a:a5", 0);
    }

    /**
     * 测试在CB中要求在CA中（b2、b3情况）
     */
    @Test
    public void testInCBRequiresInCAB2B3() {
        // 测试反向规则2.1: 如果B在CB中(b2或b3)，则A必须在CA中(a1或a3)
        inferParasByPara("b", "b2");

        // A有2种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 验证A在CA中
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 验证A不在CA中的解不存在
        assertSolutionNum("a:a2", 0);
        assertSolutionNum("a:a4", 0);
        assertSolutionNum("a:a5", 0);
    }

    /**
     * 测试不在CB中要求不在CA中
     */
    @Test
    public void testNotInCBRequiresNotInCA() {
        // 测试反向规则2.2: 如果B不在CB中(b4)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("b", "b4");

        // A有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证A不在CA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 验证A在CA中的解不存在
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 测试不在CB中要求不在CA中（b5情况）
     */
    @Test
    public void testNotInCBRequiresNotInCAB5() {
        // 测试反向规则2.2: 如果B不在CB中(b5)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("b", "b5");

        // A有3种选择
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 验证A不在CA中
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 验证A在CA中的解不存在
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 测试Codependent双向约束
     */
    @Test
    public void testCodependentBidirectionalConstraint() {
        // 测试Codependent双向约束的完整性
        // 验证CA和CB组之间的双向依赖关系

        // 测试1: A在CA中时，B必须在CB中
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);
        assertSolutionNum("b:b4", 0);
        assertSolutionNum("b:b5", 0);

        // 测试2: A不在CA中时，B必须不在CB中
        inferParasByPara("a", "a2");
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 测试3: B在CB中时，A必须在CA中
        inferParasByPara("b", "b1");
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);
        assertSolutionNum("a:a2", 0);
        assertSolutionNum("a:a4", 0);
        assertSolutionNum("a:a5", 0);

        // 测试4: B不在CB中时，A必须不在CA中
        inferParasByPara("b", "b4");
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }
}