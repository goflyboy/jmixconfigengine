package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.junit.Test;

/**
 * 兼容性规则测试类
 */
@Slf4j
public class CompatiableRuleRequireTest extends ModuleSecnarioTestBase {
    //---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CompatiableRuleRequireConstraint extends ConstraintAlgImpl {
        @ParaAnno(
            options = {"a1", "a2", "a3", "a4"}
        )
        private ParaVar AVar;
    
        @ParaAnno(
            options = {"b1", "b2", "b3", "b4"}
        )
        private ParaVar BVar;

        @Override
        protected void initConstraint() {
            //natural code: AVar.value in (a1,a3) Requires BVar.value in (b1,b2,b3)
            //A=(a1,a2,a3,a4)
            //B=(b1,b2,b3,b4)
            // 规则内容：(a1,a3) Requires (b1,b2,b3)，则CA=(a1,a3),CB=(b1,b2,b3)
            // 解读：
            // 1、正向
            // 1.1 在CA中，如果AVar.var=a1,则BVar.var=b1 或 b2 或 b3
            // 1.2 不在CA中，如果AVar.var=a1,则BVar.var=b4，是不合法的
            // 2.反向 
            // 2.1 在CB中，如果BVar.var=b1,则AVar.var=a1 或 a2 或 a3 或 a4
            // 2.2 不在CB中，如果BVar.var=b4,则AVar.var=a2 或 a4
            addCompatibleConstraintRequires("rule1", AVar, Arrays.asList("a1","a3"), BVar,
            Arrays.asList("b1","b2","b3"));
         }  
         //  @Override
        protected void initConstraint2() {
            // 创建条件变量：A是a1或a3
            BoolVar a1OrA3 = model.newBoolVar("a1OrA3");
            model.addBoolOr(new Literal[]{
                AVar.getParaOptionByCode("a1").getIsSelectedVar(),
                AVar.getParaOptionByCode("a3").getIsSelectedVar()
            }).onlyEnforceIf(a1OrA3);
            model.addBoolAnd(new Literal[]{
                AVar.getParaOptionByCode("a1").getIsSelectedVar().not(),
                AVar.getParaOptionByCode("a3").getIsSelectedVar().not()
            }).onlyEnforceIf(a1OrA3.not());

            // 创建条件变量：B是b1、b2或b3
            BoolVar b1OrB2OrB3 = model.newBoolVar("b1OrB2OrB3");
            model.addBoolOr(new Literal[]{
                BVar.getParaOptionByCode("b1").getIsSelectedVar(),
                BVar.getParaOptionByCode("b2").getIsSelectedVar(),
                BVar.getParaOptionByCode("b3").getIsSelectedVar()
            }).onlyEnforceIf(b1OrB2OrB3);
            model.addBoolAnd(new Literal[]{
                BVar.getParaOptionByCode("b1").getIsSelectedVar().not(),
                BVar.getParaOptionByCode("b2").getIsSelectedVar().not(),
                BVar.getParaOptionByCode("b3").getIsSelectedVar().not()
            }).onlyEnforceIf(b1OrB2OrB3.not());

            // 添加约束：如果A是a1或a3，则B必须是b1、b2或b3
            model.addImplication(a1OrA3, b1OrB2OrB3);
            
            // 注意：反向约束不需要显式添加，因为这是"Requires"关系的单向约束
            // 反向推理的结果是正向约束的自然结果
        }
    }
   //---------------规则定义end----------------------------------------

    public CompatiableRuleRequireTest() {
        super(CompatiableRuleRequireConstraint.class);
    }

    @Test
    public void testA1RequiresB1B2B3() {
        // 测试正向规则1.1: 如果A选择a1，则B必须是b1、b2或b3
        inferParasByPara("A", "a1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择
        
        // 验证所有解中B都是b1、b2或b3
        for(int i = 0; i < 3; i++) {
            solutions(i).assertPara("B").valueIn("b1", "b2", "b3");
        }
        
        // 验证B=b4的解不存在
        assertSolutionNum("B:b4", 0);
    }

    @Test
    public void testA3RequiresB1B2B3() {
        // 测试正向规则1.1: 如果A选择a3，则B必须是b1、b2或b3
        inferParasByPara("A", "a3");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3); // B有3种选择
        
        // 验证所有解中B都是b1、b2或b3
        for(int i = 0; i < 3; i++) {
            solutions(i).assertPara("B").valueIn("b1", "b2", "b3");
        }
        
        // 验证B=b4的解不存在
        assertSolutionNum("B:b4", 0);
    }

    @Test
    public void testA1WithB4IsInvalid() {
        // 测试正向规则1.2: 如果A选择a1，则B选择b4是不合法的
        // 设置A=a1且B=b4，应该无解
        inferParasByPara("A", "a1"); 
        assertSolutionNum("B:b4", 0);   
    }

    @Test
    public void testB1AllowsAnyA() {
        // 测试反向规则2.1: 如果B选择b1，则A可以是a1、a2、a3或a4
        inferParasByPara("B", "b1");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // A有4种选择
        
        // 验证所有解中A可以是任意值
        assertSolutionNum("A:a1", 1);
        assertSolutionNum("A:a2", 1);
        assertSolutionNum("A:a3", 1);
        assertSolutionNum("A:a4", 1);
    }

    @Test
    public void testB4AllowsA2A4Only() {
        // 测试反向规则2.2: 如果B选择b4，则A只能是a2或a4
        inferParasByPara("B", "b4");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2); // A有2种选择
        
        // 验证A只能是a2或a4
        assertSolutionNum("A:a2", 1);
        assertSolutionNum("A:a4", 1);
        
        // 验证A不能是a1或a3
        assertSolutionNum("A:a1", 0);
        assertSolutionNum("A:a3", 0);
    }

    @Test
    public void testA2A4AllowAnyB() {
        // 测试额外情况: A选择a2或a4时，B可以是任意值
        inferParasByPara("A", "a2");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // B有4种选择
        
        // 验证所有解中B可以是任意值
        assertSolutionNum("B:b1", 1);
        assertSolutionNum("B:b2", 1);
        assertSolutionNum("B:b3", 1);
        assertSolutionNum("B:b4", 1);
        
        // 同样测试a4
        inferParasByPara("A", "a4");
        
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4); // B有4种选择
    }
}