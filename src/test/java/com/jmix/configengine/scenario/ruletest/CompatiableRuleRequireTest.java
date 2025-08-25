package com.jmix.configengine.scenario.ruletest;

import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.scenario.base.*;

import java.util.Arrays;

import org.junit.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class CompatiableRuleRequireTest extends ModuleSecnarioTestBase {

    //---------------规则定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class CompatiableRuleConstraint extends ConstraintAlgImpl {
        
        @ParaAnno(
            defaultValue = "a1",
            options = {"a1", "a2", "a3","a4"}
        )
        private ParaVar AVar;
    
        @ParaAnno(
            defaultValue = "b1",
            options = {"b1", "b2", "b3","b4"}
        )
        private ParaVar BVar;

        @Override
        protected void initConstraint() {
            // Requires，需要，A Requires B 配置了A，就需要配置B，但是配置了B不一定需要配置A，本质是if-then结果，界面上配置可以不考虑属性，为了调度简单，右边依赖左边（特殊)  
            // A={a1,a2,a3},B={b1,b2,b3}
            // 若A中任一子项（如a1）被选中，则B中至少一个子项（b1/b2/b3）必须被选中； 
            // 反之，不限制。
            // 例如：
            // rawCode:  (a1,a3) Requires (b1,b2,b3),
            // 则-理解1：
            // 若a1被选中，则b1/b2/b3必须有一项被选中； 也就是A.value = a1 then B.value in (b1,b2,b3)
            // 若a3被选中，则b1/b2/b3必须一项被选中； 也就是A.value =a3 then B.value in (b1,b2,b3)
            // 若a1都未被选中，则b1/b2/b3可以任意选择；也就是A.value = a2 then B.value in (b1,b2,b3,b4)
            // 若a3都未被选中，则b1/b2/b3可以任意选择；也就是A.value = a4 then B.value in (b1,b2,b3,b4)

            //反之不成立*
            

            // 则-理解2：--好像不能这么理解？ 选择不同一样的？ TODO：未来options怎么计算呢？
            // 若a1、a3被选中，则b1/b2/b3必须有一项被选中； 也就是A.options= {a1,a3}、A.value=未明确? then B.value in (b1,b2,b3)
            // 若a1、a3都未被选中，则b1/b2/b3可以任意选择；也就是A.options= {a2,a4}、A.value=未明确? then B.value in (b1,b2,b3,b4)
            //TODO  这个怎么表达？
            addCompatibleConstraint("rule1", AVar, Arrays.asList("a1","a3"), BVar,
             Arrays.asList("b1","b2","b3"));
        }
    }
   //---------------规则定义end----------------------------------------

    
    public CompatiableRuleRequireTest() {
        super(CompatiableRuleConstraint.class);
    }
    
    @Test
    public void test_rule1_a1_selected() {
        // 测试颜色参数推理
        inferParasByPara("A", "a1");
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        assertSolutionNum("A:a1,B:b1",1);
        assertSolutionNum("A:a1,B:b2",1);
        assertSolutionNum("A:a1,B:b3",1);
        assertSolutionNum("A:a1,B:b4",0);//不满足要求


        printSolutions();
    }

    @Test
    public void test_rule1_a3_selected() {
        // 测试颜色参数推理
        inferParasByPara("A", "a3");
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(3);
        assertSolutionNum("A:a3,B:b1",1);
        assertSolutionNum("A:a3,B:b2",1);
        assertSolutionNum("A:a3,B:b3",1);
        assertSolutionNum("A:a3,B:b4",0);//不满足要求 
        printSolutions();
    }

    @Test
    public void test_rule1_a1a3_not_selected() {//a1，a3都为未被选中
        // 测试颜色参数推理
        inferParasByPara("A", "a2");
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(4);
        assertSolutionNum("A:a2,B:b1",1);
        assertSolutionNum("A:a2,B:b2",1);
        assertSolutionNum("A:a2,B:b3",1);
        assertSolutionNum("A:a2,B:b4",1);
        printSolutions();
    }
    

}