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
        // 测试颜色参数推理
        inferParas("TShirt11", 3);
        printSolutions();
        // 使用resultAssert验证执行结果
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(8);
        printSolutions();
    }
    
    @Test
    public void testSolutionNumAssertion() {
        // 测试解决方案数量断言功能
        inferParas("TShirt11", 1);
        
        // 验证满足条件的解决方案数量
        assertSolutionNum("Color:Red", 1);
        assertSolutionNum("TShirt11:1", 1);
        assertSolutionNum("Color:Red,TShirt11:1", 1);
        
        log.info("解决方案数量断言测试通过");
    }

}