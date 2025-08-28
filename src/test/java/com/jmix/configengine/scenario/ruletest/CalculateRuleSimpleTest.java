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
            addConstraint_rule1();
        }
        
        public void addConstraint_rule1() {
            // if(P1Var.var == op11) {
            //     PT1Var.var = 1;
            // }
            // else {
            //     PT1Var.var = 3;
            // }
            
            // 创建条件变量：P1是op11
            BoolVar rule1_op11 = model.newBoolVar("rule1_op11");
            
            // 实现条件逻辑：rule1_op11 = (P1 == op11)
            model.addBoolAnd(new Literal[]{
                this.P1Var.getParaOptionByCode("op11").getIsSelectedVar()
            }).onlyEnforceIf(rule1_op11);
            
            // 如果P1不是op11，则rule1_op11为false
            model.addBoolOr(new Literal[]{
                this.P1Var.getParaOptionByCode("op11").getIsSelectedVar().not()
            }).onlyEnforceIf(rule1_op11.not());
            
            // 根据条件设置PT1Var的数量
            // 如果rule1_op11为true，则PT1Var数量为1
            model.addEquality((IntVar)this.PT1Var.var, 1).onlyEnforceIf(rule1_op11);
            
            // 如果rule1_op11为false，则PT1Var数量为3
            model.addEquality((IntVar)this.PT1Var.var, 3).onlyEnforceIf(rule1_op11.not());    
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