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
    public void testOnlyOneSolution() {
        // 测试部件数量推理
        inferParas("TShirt11", 1);
        
        // 验证第一个解决方案
        solutions(0)
            .assertPara("Color")
                .valueEqual("10");
        solutions(0)
                .assertPara("Size")
                    .valueEqual("10");
                
        printSolutions();
    }

} 