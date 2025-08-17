package com.jmix.configengine.scenario.tshirt;

import com.jmix.configengine.util.ModuleUtils;
import com.jmix.configengine.model.Module;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 测试tshirtdata.json文件的反序列化功能
 */
public class JsonDeserializationTest {
    
    @Test
    public void testTShirtDataJsonDeserialization() throws Exception {
        // 从JSON文件读取T恤衫模块数据
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json";
        
        System.out.println("=== 开始测试tshirtdata.json反序列化 ===");
        System.out.println("JSON文件路径: " + jsonFilePath);
        
        // 使用ModuleUtils.fromJsonFile方法读取JSON文件
        Module tshirtModule = ModuleUtils.fromJsonFile(jsonFilePath);
        
        // 验证模块基本信息
        assertNotNull("T恤衫模块应该创建成功", tshirtModule);
        assertEquals("模块代码应该是TShirt", "TShirt", tshirtModule.getCode());
        assertEquals("模块ID应该是123123", Long.valueOf(123123), tshirtModule.getId());
        assertEquals("模块版本应该是V1.0", "V1.0", tshirtModule.getVersion());
        assertEquals("模块类型应该是GENERAL", "GENERAL", tshirtModule.getType().toString());
        
        System.out.println("✓ 模块基本信息验证通过");
        System.out.println("  模块代码: " + tshirtModule.getCode());
        System.out.println("  模块ID: " + tshirtModule.getId());
        System.out.println("  模块版本: " + tshirtModule.getVersion());
        System.out.println("  模块类型: " + tshirtModule.getType());
        
        // 验证参数信息
        assertNotNull("参数列表不应该为空", tshirtModule.getParas());
        assertEquals("应该有2个参数", 2, tshirtModule.getParas().size());
        
        System.out.println("✓ 参数信息验证通过");
        System.out.println("  参数数量: " + tshirtModule.getParas().size());
        tshirtModule.getParas().forEach(para -> {
            System.out.println("    参数: " + para.getCode() + " - " + para.getDescription());
            if (para.getOptions() != null) {
                System.out.println("      选项数量: " + para.getOptions().size());
            }
        });
        
        // 验证部件信息
        assertNotNull("部件列表不应该为空", tshirtModule.getParts());
        assertEquals("应该有2个部件", 2, tshirtModule.getParts().size());
        
        System.out.println("✓ 部件信息验证通过");
        System.out.println("  部件数量: " + tshirtModule.getParts().size());
        tshirtModule.getParts().forEach(part -> {
            System.out.println("    部件: " + part.getCode() + " - " + part.getDescription());
        });
        
        // 验证规则信息
        assertNotNull("规则列表不应该为空", tshirtModule.getRules());
        assertEquals("应该有3个规则", 3, tshirtModule.getRules().size());
        
        System.out.println("✓ 规则信息验证通过");
        System.out.println("  规则数量: " + tshirtModule.getRules().size());
        tshirtModule.getRules().forEach(rule -> {
            System.out.println("    规则: " + rule.getCode() + " - " + rule.getName());
        });
        
        System.out.println("\n=== JSON反序列化测试完成 ===");
        System.out.println("✓ 所有验证通过，tshirtdata.json文件可以正确反序列化！");
    }
} 