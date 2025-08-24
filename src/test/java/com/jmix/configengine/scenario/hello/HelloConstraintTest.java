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
    public void testColorParameterInference() {
        // 测试颜色参数推理
        inferParas("TShirt11", 1);
        
        // 使用resultAssert验证执行结果
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeEqual(1)
            .assertCodeEqual(0)
            .assertDataNotNull();
        
        // 验证第一个解决方案
        solutions(0)
            .assertPara("Color")
                .valueEqual("Red");
        solutions(0)
            .assertPart("TShirt11")
                .quantityEqual(1);
                
        printSolutions();
    }
    
    @Test
    public void testResultAssertion() {
        // 测试结果断言功能
        inferParas("TShirt11", 2);
        
        // 链式断言验证
        resultAssert()
            .assertSuccess()
            .assertSolutionSizeGreaterThan(0)
            .assertSolutionSizeLessThanOrEqual(10)
            .assertDataNotNull()
            .assertCodeEqual(0);
        
        log.info("结果断言测试通过");
    }
} 