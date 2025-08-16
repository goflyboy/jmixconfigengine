package com.jmix.configengine;

import com.jmix.configengine.model.Extensible;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.util.FilterExpressionExecutor;
import org.junit.Test;
import org.junit.Assert;

import java.util.Arrays;
import java.util.List;

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
} 