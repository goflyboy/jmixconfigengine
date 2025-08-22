package com.jmix.configengine.scenario.hello;

import com.jmix.configengine.scenario.base.ModuleSecnarioTestBase;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Hello约束算法测试类
 */
public class HelloConstraintTest extends ModuleSecnarioTestBase {
    
    public HelloConstraintTest() {
        super(HelloConstraint.class);
    }
    
    @Test
    public void testColorParameterInference() {
        // 测试颜色参数推理
        inferParas("TShirt11", 1);
        
        // 验证第一个解决方案
        solution(0)
            .assertPara("Color")
                .valueEqual("Red")
                .optionsEqual("Red", "White")
                .hiddenEqual(false);
        
        printSolutions();
    }
    
    @Test
    public void testPartQuantityInference() {
        // 测试部件数量推理
        inferParas("TShirt11", 5);
        
        // 验证第一个解决方案
        solution(0)
            .assertPart("TShirt11")
                .quantityEqual(5)
                .hiddenEqual(false);
        
        printSolutions();
    }
    
    @Test
    public void testMultipleSolutions() {
        // 测试多解决方案
        inferParas("TShirt11", 1);
        
        // 验证解决方案数量
        assertTrue("应该有多个解决方案", solutions.size() > 1);
        
        // 验证每个解决方案
        for (int i = 0; i < solutions.size(); i++) {
            solution(i)
                .assertPara("Color")
                    .optionsEqual("Red", "Black", "White");
        }
        
        printSolutions();
    }
} 