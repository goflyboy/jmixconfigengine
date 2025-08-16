package com.jmix.configengine;

import com.jmix.configengine.model.ModuleBuilder;
import com.jmix.configengine.model.ParaOption;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ParaOptionBuilder测试类
 */
public class ParaOptionBuilderTest {
    
    @Test
    public void testBasicBuilder() {
        // 测试基础构建器功能
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(10)
                .code("Red")
                .description("红色选项")
                .sortNo(1)
                .build();
        
        Assert.assertEquals(10, option.getCodeId());
        Assert.assertEquals("Red", option.getCode());
        Assert.assertEquals("红色选项", option.getDescription());
        Assert.assertEquals(Integer.valueOf(1), option.getSortNo());
    }
    
    @Test
    public void testExtAttrBuilder() {
        // 测试扩展属性构建
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(20)
                .code("Blue")
                .extAttr("hexCode", "#0000FF")
                .extAttr("popularity", "medium")
                .build();
        
        Assert.assertEquals("#0000FF", option.getExtAttr("hexCode"));
        Assert.assertEquals("medium", option.getExtAttr("popularity"));
    }
    
    @Test
    public void testBatchExtAttrs() {
        // 测试批量设置扩展属性
        Map<String, String> extAttrs = new HashMap<>();
        extAttrs.put("category", "primary");
        extAttrs.put("season", "all");
        
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(30)
                .code("Green")
                .extAttrs(extAttrs)
                .build();
        
        Assert.assertEquals("primary", option.getExtAttr("category"));
        Assert.assertEquals("all", option.getExtAttr("season"));
    }
    
    @Test
    public void testColorOptionHelper() {
        // 测试颜色选项便捷方法
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .asOption(10, "Red", "红色")
                .sortNo(1)
                .extAttr("hexCode", "#FF0000")
                .extAttr("popularity", "high")
                .build();
        
        Assert.assertEquals(10, option.getCodeId());
        Assert.assertEquals("Red", option.getCode());
        Assert.assertEquals("红色", option.getDescription());
        Assert.assertEquals("#FF0000", option.getExtAttr("hexCode"));
        Assert.assertEquals("high", option.getExtAttr("popularity"));
        Assert.assertEquals(Integer.valueOf(1), option.getSortNo());
    }
    
    @Test
    public void testSizeOptionHelper() {
        // 测试尺寸选项便捷方法
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .asOption(1, "Big", "大号")
                .sortNo(1)
                .extAttr("chest", "110-120cm")
                .extAttr("length", "70-75cm")
                .build();
        
        Assert.assertEquals(1, option.getCodeId());
        Assert.assertEquals("Big", option.getCode());
        Assert.assertEquals("大号", option.getDescription());
        Assert.assertEquals("110-120cm", option.getExtAttr("chest"));
        Assert.assertEquals("70-75cm", option.getExtAttr("length"));
        Assert.assertEquals(Integer.valueOf(1), option.getSortNo());
    }
    
    @Test
    public void testBasicOptionHelper() {
        // 测试基础选项便捷方法
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .asOption(5, "Standard", "标准选项")
                .sortNo(5)
                .defaultValue("Standard")
                .build();
        
        Assert.assertEquals(5, option.getCodeId());
        Assert.assertEquals("Standard", option.getCode());
        Assert.assertEquals("标准选项", option.getDescription());
        Assert.assertEquals("Standard", option.getDefaultValue());
        Assert.assertEquals(Integer.valueOf(5), option.getSortNo());
    }
    
    @Test
    public void testBuildWithDefaults() {
        // 测试带默认值的构建
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(15)
                .code("Yellow")
                .description("黄色选项")
                .buildWithDefaults();
        
        Assert.assertEquals(15, option.getCodeId());
        Assert.assertEquals("Yellow", option.getCode());
        Assert.assertEquals("黄色选项", option.getDescription());
        Assert.assertEquals("Yellow", option.getDefaultValue()); // 自动设置默认值
        Assert.assertEquals(Integer.valueOf(1), option.getSortNo()); // 自动设置排序号
    }
    
    @Test
    public void testComplexBuilder() {
        // 测试复杂构建场景
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(25)
                .code("Purple")
                .fatherCode("ColorGroup")
                .description("紫色选项")
                .sortNo(3)
                .extSchema("ColorOptionSchema")
                .extAttr("hexCode", "#800080")
                .extAttr("rarity", "rare")
                .extAttr("price", "expensive")
                .build();
        
        Assert.assertEquals(25, option.getCodeId());
        Assert.assertEquals("Purple", option.getCode());
        Assert.assertEquals("ColorGroup", option.getFatherCode());
        Assert.assertEquals("紫色选项", option.getDescription());
        Assert.assertEquals(Integer.valueOf(3), option.getSortNo());
        Assert.assertEquals("ColorOptionSchema", option.getExtSchema());
        Assert.assertEquals("#800080", option.getExtAttr("hexCode"));
        Assert.assertEquals("rare", option.getExtAttr("rarity"));
        Assert.assertEquals("expensive", option.getExtAttr("price"));
    }
    
    @Test
    public void testBuilderInTestData() {
        // 测试在测试数据构建中的使用
        List<ParaOption> colorOptions = Arrays.asList(
            ModuleBuilder.ParaOptionBuilder.create()
                .asOption(10, "Red", "红色")
                .sortNo(1)
                .extAttr("hexCode", "#FF0000")
                .extAttr("popularity", "high")
                .build(),
            ModuleBuilder.ParaOptionBuilder.create()
                .asOption(20, "Black", "黑色")
                .sortNo(2)
                .extAttr("hexCode", "#000000")
                .extAttr("popularity", "veryHigh")
                .build(),
            ModuleBuilder.ParaOptionBuilder.create()
                .asOption(30, "White", "白色")
                .sortNo(3)
                .extAttr("hexCode", "#FFFFFF")
                .extAttr("popularity", "high")
                .build()
        );
        
        Assert.assertEquals(3, colorOptions.size());
        
        // 验证第一个选项
        ParaOption redOption = colorOptions.get(0);
        Assert.assertEquals(10, redOption.getCodeId());
        Assert.assertEquals("Red", redOption.getCode());
        Assert.assertEquals("#FF0000", redOption.getExtAttr("hexCode"));
        
        // 验证第二个选项
        ParaOption blackOption = colorOptions.get(1);
        Assert.assertEquals(20, blackOption.getCodeId());
        Assert.assertEquals("Black", blackOption.getCode());
        Assert.assertEquals("veryHigh", blackOption.getExtAttr("popularity"));
        
        // 验证第三个选项
        ParaOption whiteOption = colorOptions.get(2);
        Assert.assertEquals(30, whiteOption.getCodeId());
        Assert.assertEquals("White", whiteOption.getCode());
        Assert.assertEquals("#FFFFFF", whiteOption.getExtAttr("hexCode"));
    }
    
    @Test
    public void testBuilderChaining() {
        // 测试方法链式调用
        ParaOption option = ModuleBuilder.ParaOptionBuilder.create()
                .codeId(100)
                .code("Test")
                .description("测试选项")
                .sortNo(10)
                .extAttr("key1", "value1")
                .extAttr("key2", "value2")
                .extAttr("key3", "value3")
                .build();
        
        Assert.assertEquals(100, option.getCodeId());
        Assert.assertEquals("Test", option.getCode());
        Assert.assertEquals("测试选项", option.getDescription());
        Assert.assertEquals(Integer.valueOf(10), option.getSortNo());
        Assert.assertEquals("value1", option.getExtAttr("key1"));
        Assert.assertEquals("value2", option.getExtAttr("key2"));
        Assert.assertEquals("value3", option.getExtAttr("key3"));
    }
} 