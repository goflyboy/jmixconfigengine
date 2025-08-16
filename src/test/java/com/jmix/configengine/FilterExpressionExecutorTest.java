package com.jmix.configengine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.jmix.configengine.model.Extensible;
import com.jmix.configengine.model.ModuleBuilder;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.PartType;
import com.jmix.configengine.util.FilterExpressionExecutor;

/**
 * FilterExpressionExecutor测试类
 */
public class FilterExpressionExecutorTest {
    
    @Test
    public void testDoSelectWithCodeFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
            new TestObject("para11", "para11 description", 1, "3G", "1"),
            new TestObject("para12", "para12 description", 2, "4G", "2"),
            new TestObject("para31", "para31 description", 2, "5G", "3"),
            new TestObject("para32", "para32 description", 3, "6G", "4")
        );
        
        // 测试code="para31"的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "code=\"para31\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("para31", result.get(0).getCode());
    }
    
    @Test
    public void testDoSelectWithSortNoFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
            new TestObject("para11", "para11 description", 1, "3G", "1"),
            new TestObject("para12", "para12 description", 2, "4G", "2"),
            new TestObject("para31", "para31 description", 2, "5G", "3"),
            new TestObject("para32", "para32 description", 3, "6G", "4")
        );
        
        // 测试sortNo=2的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "sortNo=2");
        
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("para12", result.get(0).getCode());
        Assert.assertEquals("para31", result.get(1).getCode());
    }
    
    @Test
    public void testDoSelectWithExtAttrFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
            new TestObject("para11", "para11 description", 1, "3G", "1"),
            new TestObject("para12", "para12 description", 2, "4G", "2"),
            new TestObject("para31", "para31 description", 2, "5G", "3"),
            new TestObject("para32", "para32 description", 3, "6G", "4")
        );
        
        // 测试freq="5G"的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "freq=\"5G\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("para31", result.get(0).getCode());
    }
    
    @Test
    public void testDoSelectWithEmptyList() {
        List<TestObject> objects = Arrays.asList();
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "code=\"test\"");
        Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void testDoSelectWithNullFilter() {
        List<TestObject> objects = Arrays.asList(
            new TestObject("para11", "para11 description", 1, "3G", "1")
        );
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, null);
        Assert.assertEquals(1, result.size());
    }
    
    // ========== ParaOption 过滤测试用例 ==========
    
    @Test
    public void testDoSelectParaOptionWithCodeFilter() {
        // 准备测试数据 - 基于样例数据中的颜色选项
        List<ParaOption> colorOptions = Arrays.asList(
            createColorOption(10, "Red", "红色T恤衫", 1, "#FF0000", "high"),
            createColorOption(20, "Black", "黑色T恤衫", 2, "#000000", "veryHigh"),
            createColorOption(30, "White", "白色T恤衫", 3, "#FFFFFF", "high")
        );
        
        // 测试code="Red"的过滤
        List<ParaOption> result = FilterExpressionExecutor.doSelect(colorOptions, "code=\"Red\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Red", result.get(0).getCode());
        Assert.assertEquals(10, result.get(0).getCodeId());
    }
    
    @Test
    public void testDoSelectParaOptionWithCodeIdFilter() {
        // 准备测试数据 - 基于样例数据中的尺寸选项
        List<ParaOption> sizeOptions = Arrays.asList(
            createSizeOption(1, "Big", "大号尺寸", 1, "110-120cm", "70-75cm"),
            createSizeOption(2, "Medium", "中号尺寸", 2, "100-110cm", "65-70cm"),
            createSizeOption(3, "Small", "小号尺寸", 3, "90-100cm", "60-65cm")
        );
        
        // 测试codeId=2的过滤
        List<ParaOption> result = FilterExpressionExecutor.doSelect(sizeOptions, "codeId=2");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Medium", result.get(0).getCode());
        Assert.assertEquals(2, result.get(0).getCodeId());
    }
    
    @Test
    public void testDoSelectParaOptionWithExtAttrFilter() {
        // 准备测试数据 - 基于样例数据中的颜色选项
        List<ParaOption> colorOptions = Arrays.asList(
            createColorOption(10, "Red", "红色T恤衫", 1, "#FF0000", "high"),
            createColorOption(20, "Black", "黑色T恤衫", 2, "#000000", "veryHigh"),
            createColorOption(30, "White", "白色T恤衫", 3, "#FFFFFF", "high")
        );
        
        // 测试popularity="veryHigh"的过滤
        List<ParaOption> result = FilterExpressionExecutor.doSelect(colorOptions, "popularity=\"veryHigh\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Black", result.get(0).getCode());
        Assert.assertEquals("veryHigh", result.get(0).getExtAttr("popularity"));
    }
    
    @Test
    public void testDoSelectParaOptionWithSortNoFilter() {
        // 准备测试数据
        List<ParaOption> colorOptions = Arrays.asList(
            createColorOption(10, "Red", "红色T恤衫", 1, "#FF0000", "high"),
            createColorOption(20, "Black", "黑色T恤衫", 2, "#000000", "veryHigh"),
            createColorOption(30, "White", "白色T恤衫", 3, "#FFFFFF", "high")
        );
        
        // 测试sortNo>1的过滤
        List<ParaOption> result = FilterExpressionExecutor.doSelect(colorOptions, "sortNo>1");
        
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().allMatch(option -> option.getSortNo() > 1));
    }
    
    // ========== Part 过滤测试用例 ==========
    
    @Test
    public void testDoSelectPartWithCodeFilter() {
        // 准备测试数据 - 基于样例数据中的部件
        List<Part> parts = Arrays.asList(
            createTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g", "100%棉", "中等", "适中"),
            createTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g", "印花", "胸前", "10x8cm")
        );
        
        // 测试code="TShirt11"的过滤
        List<Part> result = FilterExpressionExecutor.doSelect(parts, "code=\"TShirt11\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TShirt11", result.get(0).getCode());
        Assert.assertEquals(Long.valueOf(1500L), result.get(0).getPrice());
    }
    
    @Test
    public void testDoSelectPartWithPriceFilter() {
        // 准备测试数据
        List<Part> parts = Arrays.asList(
            createTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g", "100%棉", "中等", "适中"),
            createTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g", "印花", "胸前", "10x8cm")
        );
        
        // 测试price<1000的过滤
        List<Part> result = FilterExpressionExecutor.doSelect(parts, "price<1000");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TShirt12", result.get(0).getCode());
        Assert.assertTrue(result.get(0).getPrice() < 1000);
    }
    
    @Test
    public void testDoSelectPartWithExtAttrFilter() {
        // 准备测试数据
        List<Part> parts = Arrays.asList(
            createTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g", "100%棉", "中等", "适中"),
            createTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g", "印花", "胸前", "10x8cm")
        );
        
        // 测试material="cotton"的过滤
        List<Part> result = FilterExpressionExecutor.doSelect(parts, "material=\"cotton\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TShirt11", result.get(0).getCode());
        Assert.assertEquals("cotton", result.get(0).getExtAttr("material"));
    }
    
    @Test
    public void testDoSelectPartWithAttrsFilter() {
        // 准备测试数据
        List<Part> parts = Arrays.asList(
            createTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g", "100%棉", "中等", "适中"),
            createTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g", "印花", "胸前", "10x8cm")
        );
        
        // 测试fabric="100%棉"的过滤
        List<Part> result = FilterExpressionExecutor.doSelect(parts, "fabric=\"100%棉\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TShirt11", result.get(0).getCode());
        Assert.assertEquals("100%棉", result.get(0).getAttrs().get("fabric"));
    }
    
    @Test
    public void testDoSelectPartWithComplexFilter() {
        // 准备测试数据
        List<Part> parts = Arrays.asList(
            createTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g", "100%棉", "中等", "适中"),
            createTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g", "印花", "胸前", "10x8cm")
        );
        
        // 测试price>=1000 AND material="cotton"的过滤
        List<Part> result = FilterExpressionExecutor.doSelect(parts, "price>=1000");
        result = FilterExpressionExecutor.doSelect(result, "material=\"cotton\"");
        
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("TShirt11", result.get(0).getCode());
        Assert.assertTrue(result.get(0).getPrice() >= 1000);
        Assert.assertEquals("cotton", result.get(0).getExtAttr("material"));
    }
    
    /**
     * 测试用的对象类
     */
    private static class TestObject extends Extensible {
        private String code;
        private String description;
        private int sortNo;
        // private String freq;
        // private String slots;
        
        public TestObject(String code, String description, int sortNo, String freq, String slots) {
            this.code = code;
            this.description = description;
            this.sortNo = sortNo;             
            // 设置扩展属性
            setExtAttr("freq", freq);
            setExtAttr("slots", slots);
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public int getSortNo() { return sortNo; } 
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 创建颜色选项测试对象
     */
    private ParaOption createColorOption(int codeId, String code, String description, int sortNo, String hexCode, String popularity) {
        return ModuleBuilder.ParaOptionBuilder.create()
                .asOption(codeId, code, description)
                .sortNo(sortNo)
                .extAttr("hexCode", hexCode)
                .extAttr("popularity", popularity)
                .build();
    }
    
    /**
     * 创建尺寸选项测试对象
     */
    private ParaOption createSizeOption(int codeId, String code, String description, int sortNo, String chest, String length) {
        return ModuleBuilder.ParaOptionBuilder.create()
                .asOption(codeId, code, description)
                .sortNo(sortNo)
                .extAttr("chest", chest)
                .extAttr("length", length)
                .build();
    }
    
    /**
     * 创建T恤衫部件测试对象
     */
    private Part createTShirtPart(String code, String description, int sortNo, Long price, String material, String weight, 
                                 String fabric, String thickness, String elasticity) {
        return ModuleBuilder.PartBuilder.create()
                .asPart(code, description, sortNo, price, material, weight)
                .extAttr("price", price.toString())
                .attr("fabric", fabric)
                .attr("thickness", thickness)
                .attr("elasticity", elasticity)
                .build();
    }
} 