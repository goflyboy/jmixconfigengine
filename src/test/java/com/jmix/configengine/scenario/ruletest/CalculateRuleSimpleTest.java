package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.scenario.base.*;
import org.junit.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class CalculateRuleSimpleTest extends ModuleSecnarioTestBase {

    //---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CalculateRuleConstraint extends ConstraintAlgImpl {
        
        @ParaAnno(
            options = {"op11", "op12", "op13"}
        )
        private ParaVar P1Var;
     
        @PartAnno
        private PartVar PT1Var;

        @Override
        protected void initConstraint() {
            // if(P1Var.var == op11) {
            //     PT1Var.var = 1;
            // }
            // else {
            //     PT1Var.var = 3;
            // }
            
            // 创建条件变量
            BoolVar p1IsOp11 = model.newBoolVar("P1_is_op11");
            BoolVar p1IsNotOp11 = model.newBoolVar("P1_is_not_op11");
            
            // 设置P1Var.var == op11的条件
            // 需要根据P1Var的选项值来设置，op11对应codeId为10
            model.addEquality((IntVar) P1Var.var, 10).onlyEnforceIf(p1IsOp11);
            model.addEquality((IntVar) P1Var.var, 10).onlyEnforceIf(p1IsNotOp11.not());
            
            // 设置PT1Var.var的值
            model.addEquality((IntVar) PT1Var.var, 1).onlyEnforceIf(p1IsOp11);
            model.addEquality((IntVar) PT1Var.var, 3).onlyEnforceIf(p1IsNotOp11);
        }
    } 
   //---------------规则定义end----------------------------------------

    
    public CalculateRuleSimpleTest() {
        super(CalculateRuleConstraint.class);
    }
    
    @Test
    public void test_if_op11() {
        // 测试颜色参数推理
        inferParas("PT1", 1);
        
        // 验证第一个解
        solutions(0).assertPara("P1").valueEqual("op11");
        printSolutions();
    }

    
    @Test
    public void test_else_op11() {
        inferParas("PT1", 3);
        printSolutions();
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(8);
        //if的解肯定不在其中
        assertSolutionNum("P1:op11",0);
        printSolutions();
    }
    
    
    @Test
    public void test_no_if_else() {
        inferParas("PT1", 4);
        printSolutions();
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(0);
        //if的解肯定不在其中
        assertSolutionNum("P1:op11",0);
        printSolutions();
    }
    


}