package com.jmix.configengine.scenario.hello;

import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import org.junit.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello约束算法测试类
 */
@Slf4j
public class HelloConstraintTest extends ModuleSecnarioTestBase {
    
    public HelloConstraintTest() {
        super(HelloConstraint.class);
    }
    
    @Test
    public void testOnlyOneSolution() {
        // 测试颜色参数推理
        inferParas("TShirt11", 1);
        
        // 使用resultAssert验证执行结果
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1);
        
        // 验证第一个解
        solutions(0).assertPara("Color").valueEqual("Red")
        .assertPara("Size").valueEqual("Small");
        printSolutions();
    }

    
    @Test
    public void testMultiSolution() {
        // "Red-10", "Black-20", "White-30"
        // "Small-10", "Medium-20", "Big-30"
        // if(ColorVar.var ==Red && SizeVar.var == Small  ) {
        //     TShirt11Var.var = 1;
        // }
        // else {
        //     TShirt11Var.var = 3
        // }
        // 测试颜色参数推理
        inferParas("TShirt11", 3);
        printSolutions();
        //反推有8个接口  8=3*3 - 1
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(8);
        //if的解肯定不在其中
        assertSolutionNum("Color:Red,Size:Small",0);
        //else的解可能解如下：
        assertSolutionNum("Color:Black,Size:Big",1);
        assertSolutionNum("Color:White,Size:Medium",1);
        assertSolutionNum("Color:Red,Size:Medium",1);
        assertSolutionNum("Color:Black,Size:Medium",1);
        assertSolutionNum("Color:White,Size:Big",1);
        assertSolutionNum("Color:Red,Size:Big",1);
        assertSolutionNum("Color:Black,Size:Small",1);
        printSolutions();
    }
    
 

}