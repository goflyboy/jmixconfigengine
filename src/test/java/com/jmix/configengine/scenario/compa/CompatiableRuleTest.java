package com.jmix.configengine.scenario.compa;

import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.scenario.base.*;
import org.junit.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class CompatiableRuleTest extends ModuleSecnarioTestBase {

    //---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CompatiableRuleConstraint extends ConstraintAlgImpl {
        
        @ParaAnno(
            defaultValue = "op11",
            options = {"op11", "op12", "op13"}
        )
        private ParaVar P1Var;
    
        @ParaAnno(
            defaultValue = "op21",
            options = {"op21", "op22", "op23"}
        )
        private ParaVar P2Var;
        @PartAnno
        private PartVar PT1Var;

        @Override
        protected void initConstraint() {
            addConstraint_rule2();
        }
        public void addConstraint_rule2() {
        
            // if(P1Var.var ==op11 && P2Var.var == op21  ) {
            //     PT1Var.var = 1;
            // }
            // else {
            //     PT1Var.var = 3
            // }
    
            // 创建条件变量：颜色是红色且尺寸是小号
            BoolVar rule2_op11Andop21 = model.newBoolVar("rule2_op11Andop21");
            
            // 实现条件逻辑：redAndSmall = (P1== op11) AND (P2 == op21)
            model.addBoolAnd(new Literal[]{
                this.P1Var.getParaOptionByCode("op11").getIsSelectedVar(),
                this.P2Var.getParaOptionByCode("op21").getIsSelectedVar()
            }).onlyEnforceIf(rule2_op11Andop21);
            
            // 如果不是红色且小号的组合，则op11Andop2为false
            model.addBoolOr(new Literal[]{
                this.P1Var.getParaOptionByCode("op11").getIsSelectedVar().not(),
                this.P2Var.getParaOptionByCode("op21").getIsSelectedVar().not()
            }).onlyEnforceIf(rule2_op11Andop21.not());
            
            // 根据条件设置PT1Var的数量
            // 如果op11Andop2为true，则PT1Var数量为1
            model.addEquality((IntVar)this.PT1Var.var, 1).onlyEnforceIf(rule2_op11Andop21);
            
            // 如果op11Andop2为false，则PT1Var数量为3
            model.addEquality((IntVar)this.PT1Var.var, 3).onlyEnforceIf(rule2_op11Andop21.not());
        }
    } 
   //---------------规则定义end----------------------------------------

    
    public CompatiableRuleTest() {
        super(CompatiableRuleConstraint.class);
    }
    
    @Test
    public void testOnlyOneSolution() {
        log.info("testOnlyOneSolution"+ CompatiableRuleConstraint.class.getName());
        // 测试颜色参数推理
        inferParas("PT1", 1); 
        
        // 验证第一个解
        solutions(0).assertPara("P1").valueEqual("op11")
        .assertPara("P2").valueEqual("op21");
        printSolutions();
    }

    
    @Test
    public void testMultiSolution() { 
        inferParas("PT1", 3);
        printSolutions();
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(8);
        //if的解肯定不在其中
        assertSolutionNum("P1:op11,P2:op21",0);
        //else的解可能解如下：
        assertSolutionNum("P1:op12,P2:op22",1); 
        printSolutions();
    }
    
 

}