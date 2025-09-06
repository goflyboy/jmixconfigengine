package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.junit.Test;

/**
 * 兼容性规则测试类
 */
@Slf4j
public class CompatiableRuleCodependentTest extends ModuleSecnarioTestBase {
    //---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CompatiableRuleCodependentConstraint extends ConstraintAlgImpl {
        @ParaAnno(
            options = {"a1", "a2", "a3", "a4","a5"}
        )
        private ParaVar AVar;
    
        @ParaAnno(
            options = {"b1", "b2", "b3", "b4","b5"}
        )
        private ParaVar BVar;

        @Override
        protected void initConstraint() {
            //A=(a1,a2,a3,a4,a5),AVar.value的值域
            //B=(b1,b2,b3,b4,b5),BVar.value的值域
            // 规则内容：(a1,a3) CoDependent (b1,b2,b3)，则CA=(a1,a3),CB=(b1,b2,b3),NCA=(a2,a4,a5),NCB=(b4,b5)
            // 解读：
            // 1、正向
            // 1.1 如果AVar.value取CA的一个值(在CA中)，则BVar.value一定取在CB的一个值(在CB中)，取NCB内的值是非法的，如:如果AVar.var=a1,则BVar.var=b1 或 b2 或 b3,不可以是b4或b5
            // 1.2 如果AVar.value取NCA一个值(不在CA中)，则BVar.value一定取NCB的一个值(不在CB中)，取CB内的值是非法的，如：如果AVar.var=a2,则BVar.var=b4或b5，不可以是b1或b2或b3
            // 2.反向
            // 2.1 如果BVar.value取CB的一个值(在CB中)，则AVar.value一定取在CA的一个值(在CA中)，取NCA内的值是非法的，如:如果BVar.var=b1,则AVar.var=a1 或 a3,不可以是a2或a4或a5
            // 2.2 如果BVar.value取CB外的一个值(不在CB中)，则AVar.value一定取CA外的一个值(不在CA中)，取CA内的值是非法的，如：如果BVar.var=b4,则AVar.var=a2 或 a4或a5
            addCompatibleConstraintCoDependent("rule1", AVar, Arrays.asList("a1","a3"), BVar,
            Arrays.asList("b1","b2","b3"));

        }
        
        protected void addCompatibleConstraintCoDependent_note() { 
            // 创建条件变量：A在CA中
            BoolVar inCA = model.newBoolVar("inCA");
            model.addBoolOr(new Literal[]{
                AVar.getParaOptionByCode("a1").getIsSelectedVar(),
                AVar.getParaOptionByCode("a3").getIsSelectedVar()
            }).onlyEnforceIf(inCA);
            model.addBoolAnd(new Literal[]{
                AVar.getParaOptionByCode("a1").getIsSelectedVar().not(),
                AVar.getParaOptionByCode("a3").getIsSelectedVar().not()
            }).onlyEnforceIf(inCA.not());

            // 创建条件变量：B在CB中
            BoolVar inCB = model.newBoolVar("inCB");
            model.addBoolOr(new Literal[]{
                BVar.getParaOptionByCode("b1").getIsSelectedVar(),
                BVar.getParaOptionByCode("b2").getIsSelectedVar(),
                BVar.getParaOptionByCode("b3").getIsSelectedVar()
            }).onlyEnforceIf(inCB);
            model.addBoolAnd(new Literal[]{
                BVar.getParaOptionByCode("b1").getIsSelectedVar().not(),
                BVar.getParaOptionByCode("b2").getIsSelectedVar().not(),
                BVar.getParaOptionByCode("b3").getIsSelectedVar().not()
            }).onlyEnforceIf(inCB.not());

            // 创建条件变量：A不在CA中
            BoolVar notInCA = model.newBoolVar("notInCA");
            model.addBoolOr(new Literal[]{
                AVar.getParaOptionByCode("a2").getIsSelectedVar(),
                AVar.getParaOptionByCode("a4").getIsSelectedVar(),
                AVar.getParaOptionByCode("a5").getIsSelectedVar()
            }).onlyEnforceIf(notInCA);
            model.addBoolAnd(new Literal[]{
                AVar.getParaOptionByCode("a2").getIsSelectedVar().not(),
                AVar.getParaOptionByCode("a4").getIsSelectedVar().not(),
                AVar.getParaOptionByCode("a5").getIsSelectedVar().not()
            }).onlyEnforceIf(notInCA.not());

            // 创建条件变量：B不在CB中
            BoolVar notInCB = model.newBoolVar("notInCB");
            model.addBoolOr(new Literal[]{
                BVar.getParaOptionByCode("b4").getIsSelectedVar(),
                BVar.getParaOptionByCode("b5").getIsSelectedVar()
            }).onlyEnforceIf(notInCB);
            model.addBoolAnd(new Literal[]{
                BVar.getParaOptionByCode("b4").getIsSelectedVar().not(),
                BVar.getParaOptionByCode("b5").getIsSelectedVar().not()
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
   //---------------规则定义end----------------------------------------

    public CompatiableRuleCodependentTest() {
        super(CompatiableRuleCodependentConstraint.class);
    }

    @Test
    public void testInCARequiresInCB() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B必须在CB中(b1、b2或b3)
        inferParasByPara("A", "a1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择
        
        // 验证所有解中B都在CB中
        for(int i = 0; i < 3; i++) {
            solutions(i).assertPara("B").valueIn("b1", "b2", "b3");
        }
        
        // 验证B不在CB中的解不存在
        assertSolutionNum("B:b4", 0);
        assertSolutionNum("B:b5", 0);
    }

    @Test
    public void testInCARequiresInCB_A3() {
        // 测试正向规则1.1: 如果A在CA中(a3)，则B必须在CB中(b1、b2或b3)
        inferParasByPara("A", "a3");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择
        
        // 验证所有解中B都在CB中
        for(int i = 0; i < 3; i++) {
            solutions(i).assertPara("B").valueIn("b1", "b2", "b3");
        }
        
        // 验证B不在CB中的解不存在
        assertSolutionNum("B:b4", 0);
        assertSolutionNum("B:b5", 0);
    }

    @Test
    public void testInCAWithNotInCBIsInvalid() {
        // 测试正向规则1.1: 如果A在CA中(a1)，则B不在CB中(b4或b5)是不合法的
        inferParasByPara("A", "a1"); 
        assertSolutionNum("B:b4", 0);
        assertSolutionNum("B:b5", 0);
    }

    @Test
    public void testNotInCARequiresNotInCB() {
        // 测试正向规则1.2: 如果A不在CA中(a2)，则B必须不在CB中(b4或b5)
        inferParasByPara("A", "a2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // B有2种选择
        
        // 验证B不在CB中
        assertSolutionNum("B:b4", 1);
        assertSolutionNum("B:b5", 1);
        
        // 验证B在CB中的解不存在
        assertSolutionNum("B:b1", 0);
        assertSolutionNum("B:b2", 0);
        assertSolutionNum("B:b3", 0);
    }

    @Test
    public void testNotInCARequiresNotInCB_A4() {
        // 测试正向规则1.2: 如果A不在CA中(a4)，则B必须不在CB中(b4或b5)
        inferParasByPara("A", "a4");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // B有2种选择
        
        // 验证B不在CB中
        assertSolutionNum("B:b4", 1);
        assertSolutionNum("B:b5", 1);
        
        // 验证B在CB中的解不存在
        assertSolutionNum("B:b1", 0);
        assertSolutionNum("B:b2", 0);
        assertSolutionNum("B:b3", 0);
    }

    @Test
    public void testNotInCARequiresNotInCB_A5() {
        // 测试正向规则1.2: 如果A不在CA中(a5)，则B必须不在CB中(b4或b5)
        inferParasByPara("A", "a5");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // B有2种选择
        
        // 验证B不在CB中
        assertSolutionNum("B:b4", 1);
        assertSolutionNum("B:b5", 1);
        
        // 验证B在CB中的解不存在
        assertSolutionNum("B:b1", 0);
        assertSolutionNum("B:b2", 0);
        assertSolutionNum("B:b3", 0);
    }

    @Test
    public void testInCBRequiresInCA() {
        // 测试反向规则2.1: 如果B在CB中(b1)，则A必须在CA中(a1或a3)
        inferParasByPara("B", "b1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // A有2种选择
        
        // 验证A在CA中
        assertSolutionNum("A:a1", 1);
        assertSolutionNum("A:a3", 1);
        
        // 验证A不在CA中的解不存在
        assertSolutionNum("A:a2", 0);
        assertSolutionNum("A:a4", 0);
        assertSolutionNum("A:a5", 0);
    }

    @Test
    public void testInCBRequiresInCA_B2B3() {
        // 测试反向规则2.1: 如果B在CB中(b2或b3)，则A必须在CA中(a1或a3)
        inferParasByPara("B", "b2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // A有2种选择
        
        // 验证A在CA中
        assertSolutionNum("A:a1", 1);
        assertSolutionNum("A:a3", 1);
        
        // 验证A不在CA中的解不存在
        assertSolutionNum("A:a2", 0);
        assertSolutionNum("A:a4", 0);
        assertSolutionNum("A:a5", 0);
    }

    @Test
    public void testNotInCBRequiresNotInCA() {
        // 测试反向规则2.2: 如果B不在CB中(b4)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("B", "b4");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // A有3种选择
        
        // 验证A不在CA中
        assertSolutionNum("A:a2", 1);
        assertSolutionNum("A:a4", 1);
        assertSolutionNum("A:a5", 1);
        
        // 验证A在CA中的解不存在
        assertSolutionNum("A:a1", 0);
        assertSolutionNum("A:a3", 0);
    }

    @Test
    public void testNotInCBRequiresNotInCA_B5() {
        // 测试反向规则2.2: 如果B不在CB中(b5)，则A必须不在CA中(a2、a4或a5)
        inferParasByPara("B", "b5");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // A有3种选择
        
        // 验证A不在CA中
        assertSolutionNum("A:a2", 1);
        assertSolutionNum("A:a4", 1);
        assertSolutionNum("A:a5", 1);
        
        // 验证A在CA中的解不存在
        assertSolutionNum("A:a1", 0);
        assertSolutionNum("A:a3", 0);
    }

    @Test
    public void testCodependentBidirectionalConstraint() {
        // 测试Codependent双向约束的完整性
        // 验证CA和CB组之间的双向依赖关系
        
        // 测试1: A在CA中时，B必须在CB中
        inferParasByPara("A", "a1");
        assertSolutionNum("B:b1", 1);
        assertSolutionNum("B:b2", 1);
        assertSolutionNum("B:b3", 1);
        assertSolutionNum("B:b4", 0);
        assertSolutionNum("B:b5", 0);
        
        // 测试2: A不在CA中时，B必须不在CB中
        inferParasByPara("A", "a2");
        assertSolutionNum("B:b1", 0);
        assertSolutionNum("B:b2", 0);
        assertSolutionNum("B:b3", 0);
        assertSolutionNum("B:b4", 1);
        assertSolutionNum("B:b5", 1);
        
        // 测试3: B在CB中时，A必须在CA中
        inferParasByPara("B", "b1");
        assertSolutionNum("A:a1", 1);
        assertSolutionNum("A:a3", 1);
        assertSolutionNum("A:a2", 0);
        assertSolutionNum("A:a4", 0);
        assertSolutionNum("A:a5", 0);
        
        // 测试4: B不在CB中时，A必须不在CA中
        inferParasByPara("B", "b4");
        assertSolutionNum("A:a1", 0);
        assertSolutionNum("A:a3", 0);
        assertSolutionNum("A:a2", 1);
        assertSolutionNum("A:a4", 1);
        assertSolutionNum("A:a5", 1);
    }
}