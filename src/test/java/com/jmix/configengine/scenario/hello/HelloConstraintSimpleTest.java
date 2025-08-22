package com.jmix.configengine.scenario.hello;

import com.jmix.configengine.scenario.base.ModuleGenneratorByAnno;
import com.jmix.configengine.model.Module;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Hello约束算法的简化测试类
 */
public class HelloConstraintSimpleTest {
    
    @Test
    public void testModuleGeneration() {
        // 测试Module生成
        Module module = ModuleGenneratorByAnno.build(HelloConstraint.class, "target/test-temp");
        
        assertNotNull("Module应该被生成", module);
        assertEquals("模块代码应该是Hello", "Hello", module.getCode());
        assertEquals("模块ID应该是123", Long.valueOf(123), module.getId());
        
        assertNotNull("应该有参数", module.getParas());
        assertTrue("应该有颜色参数", module.getParas().size() > 0);
        
        assertNotNull("应该有部件", module.getParts());
        assertTrue("应该有T恤部件", module.getParts().size() > 0);
        
        System.out.println("生成的Module: " + module.getCode());
        System.out.println("参数数量: " + module.getParas().size());
        System.out.println("部件数量: " + module.getParts().size());
    }
} 