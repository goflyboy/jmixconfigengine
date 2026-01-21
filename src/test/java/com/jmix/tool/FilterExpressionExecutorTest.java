package com.jmix.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Extensible;
import com.jmix.executor.impl.util.FilterExpressionExecutor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * FilterExpressionExecutor测试类
 * 
 * @since 2025-09-23
 */
public class FilterExpressionExecutorTest {

    @Test
    public void testDoSelectWithCodeFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"),
                new TestObject("para32", "para32 description", 3, "6G", "4"));

        // 测试code="para31"的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "code=\"para31\"");

        assertEquals(1, result.size());
        assertEquals("para31", result.get(0).getCode());
    }

    @Test
    public void testDoSelectWithSortNoFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"),
                new TestObject("para32", "para32 description", 3, "6G", "4"));

        // 测试sortNo=2的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "sortNo=2");

        assertEquals(2, result.size());
        assertEquals("para12", result.get(0).getCode());
        assertEquals("para31", result.get(1).getCode());
    }

    @Test
    public void testDoSelectWithExtAttrFilter() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"),
                new TestObject("para32", "para32 description", 3, "6G", "4"));

        // 测试freq="5G"的过滤
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "freq=\"5G\"");

        assertEquals(1, result.size());
        assertEquals("para31", result.get(0).getCode());
    }

    @Test
    public void testDoSelectWithEmptyList() {
        List<TestObject> objects = Arrays.asList();
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, "code=\"test\"");
        assertEquals(0, result.size());
    }

    @Test
    public void testDoSelectWithNullFilter() {
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"));
        List<TestObject> result = FilterExpressionExecutor.doSelect(objects, (String) null);
        assertEquals(1, result.size());
    }

    // ========== doDeduct 测试用例 ==========

    @Test
    public void testDoDeductWithEqualsOperator() {
        // 准备测试数据 - 根据用户示例
        List<TestPart> parts = Arrays.asList(
                createTestPart("part1", "5400", "1T"),
                createTestPart("part2", "7200", "2T"),
                createTestPart("part3", "9000", "4T"));

        // 测试扣除 speed = 5400，应该返回 part2 和 part3
        List<TestPart> result = FilterExpressionExecutor.doDeduct(parts, "speed=5400");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> "part2".equals(p.getCode())));
        assertTrue(result.stream().anyMatch(p -> "part3".equals(p.getCode())));
        assertTrue(result.stream().noneMatch(p -> "part1".equals(p.getCode())));
    }

    @Test
    public void testDoDeductWithNotEqualsOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"));

        // 测试扣除 sortNo != 2，应该返回 sortNo = 2 的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "sortNo!=2");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(obj -> obj.getSortNo() == 2));
    }

    @Test
    public void testDoDeductWithGreaterThanOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 3, "5G", "3"),
                new TestObject("para32", "para32 description", 4, "6G", "4"));

        // 测试扣除 sortNo > 2，应该返回 sortNo <= 2 的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "sortNo>2");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(obj -> obj.getSortNo() <= 2));
    }

    @Test
    public void testDoDeductWithLessThanOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 3, "5G", "3"));

        // 测试扣除 sortNo < 3，应该返回 sortNo >= 3 的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "sortNo<3");

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getSortNo());
    }

    @Test
    public void testDoDeductWithGreaterEqualsOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 3, "5G", "3"));

        // 测试扣除 sortNo >= 2，应该返回 sortNo < 2 的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "sortNo>=2");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getSortNo());
    }

    @Test
    public void testDoDeductWithLessEqualsOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 3, "5G", "3"));

        // 测试扣除 sortNo <= 2，应该返回 sortNo > 2 的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "sortNo<=2");

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getSortNo());
    }

    @Test
    public void testDoDeductWithLikeOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"),
                new TestObject("para32", "para32 description", 3, "6G", "4"));

        // 测试扣除 code like "para1%"，应该返回 code 不以 "para1" 开头的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "code like \"para1%\"");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(obj -> !obj.getCode().startsWith("para1")));
    }

    @Test
    public void testDoDeductWithNotLikeOperator() {
        // 准备测试数据
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"),
                new TestObject("para31", "para31 description", 2, "5G", "3"),
                new TestObject("para32", "para32 description", 3, "6G", "4"));

        // 测试扣除 code not like "para1%"，应该返回 code 以 "para1" 开头的对象
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "code not like \"para1%\"");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(obj -> obj.getCode().startsWith("para1")));
    }

    @Test
    public void testDoDeductWithEmptyList() {
        List<TestObject> objects = Arrays.asList();
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "code=\"test\"");
        assertEquals(0, result.size());
    }

    @Test
    public void testDoDeductWithNullFilter() {
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"));
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, (String) null);
        assertEquals(1, result.size());
    }

    @Test
    public void testDoDeductWithInvalidExpression() {
        List<TestObject> objects = Arrays.asList(
                new TestObject("para11", "para11 description", 1, "3G", "1"),
                new TestObject("para12", "para12 description", 2, "4G", "2"));
        // 无效的表达式，应该返回原始列表
        List<TestObject> result = FilterExpressionExecutor.doDeduct(objects, "invalid expression");
        assertEquals(2, result.size());
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

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public int getSortNo() {
            return sortNo;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "code='" + code + '\'' +
                    ", description='" + getDescription() + '\'' +
                    ", sortNo=" + getSortNo() +
                    '}';
        }
    }

    /**
     * 测试用的Part对象类，用于doDeduct测试
     */
    private static class TestPart extends Extensible {
        private String code;
        private String speed;
        private String capacity;

        public TestPart(String code, String speed, String capacity) {
            this.code = code;
            this.speed = speed;
            this.capacity = capacity;
        }

        public String getCode() {
            return code;
        }

        public String getSpeed() {
            return speed;
        }

        public String getCapacity() {
            return capacity;
        }
    }

    /**
     * 创建测试用的Part对象
     */
    private TestPart createTestPart(String code, String speed, String capacity) {
        return new TestPart(code, speed, capacity);
    }
}