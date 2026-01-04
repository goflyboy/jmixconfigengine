package com.jmix.executor.imodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InstanceDynAttrValue 单元测试
 *
 * @since 2025-12-27
 */
public class InstanceDynAttrValueTest {

    private InstanceDynAttrValue instanceDynAttrValue;
    private InstanceDynAttrValueItem item1;
    private InstanceDynAttrValueItem item2;
    private InstanceDynAttrValueItem item3;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        instanceDynAttrValue = new InstanceDynAttrValue();

        // 创建测试用的 InstanceDynAttrValueItem
        item1 = new InstanceDynAttrValueItem();
        item1.setInstId(1);
        Map<String, String> attrs1 = new HashMap<>();
        attrs1.put("Speed", "5400");
        attrs1.put("Capacity", "8T");
        attrs1.put("BrandWidth", "8GB/S");
        item1.setInstAttrs(attrs1);

        item2 = new InstanceDynAttrValueItem();
        item2.setInstId(2);
        Map<String, String> attrs2 = new HashMap<>();
        attrs2.put("Speed", "7200/5400");
        attrs2.put("Capacity", "16T");
        attrs2.put("BrandWidth", "16GB/S");
        item2.setInstAttrs(attrs2);

        item3 = new InstanceDynAttrValueItem();
        item3.setInstId(3);
        Map<String, String> attrs3 = new HashMap<>();
        attrs3.put("Speed", "9000");
        attrs3.put("Capacity", "4T");
        attrs3.put("BrandWidth", "8GB/S");
        item3.setInstAttrs(attrs3);

        // 添加到列表中
        instanceDynAttrValue.getInstsValues().add(item1);
        instanceDynAttrValue.getInstsValues().add(item2);
        instanceDynAttrValue.getInstsValues().add(item3);
    }

    /**
     * 测试空条件查询 - 应该返回所有项
     */
    @Test
    public void testQueryEmptyCondition() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("");

        assertEquals(3, result.size(), "Empty condition should return all items");
        assertTrue(result.contains(item1), "Should contain item1");
        assertTrue(result.contains(item2), "Should contain item2");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试null条件查询 - 应该返回所有项
     */
    @Test
    public void testQueryNullCondition() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query(null);

        assertEquals(3, result.size(), "Null condition should return all items");
        assertTrue(result.contains(item1), "Should contain item1");
        assertTrue(result.contains(item2), "Should contain item2");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试Speed字段的like查询 - 匹配5400
     */
    @Test
    public void testQuerySpeedLike5400() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Speed like %5400%");

        assertEquals(2, result.size(), "Should find 2 items with Speed containing 5400");
        assertTrue(result.contains(item1), "Should contain item1 (5400)");
        assertTrue(result.contains(item2), "Should contain item2 (7200/5400)");
        assertFalse(result.contains(item3), "Should not contain item3 (9000)");
    }

    /**
     * 测试Speed字段的like查询 - 匹配9000
     */
    @Test
    public void testQuerySpeedLike9000() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Speed like %9000%");

        assertEquals(1, result.size(), "Should find 1 item with Speed containing 9000");
        assertFalse(result.contains(item1), "Should not contain item1");
        assertFalse(result.contains(item2), "Should not contain item2");
        assertTrue(result.contains(item3), "Should contain item3 (9000)");
    }

    /**
     * 测试Capacity字段的like查询 - 匹配8T
     */
    @Test
    public void testQueryCapacityLike8T() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Capacity like %8T%");

        assertEquals(1, result.size(), "Should find 1 item with Capacity containing 8T");
        assertTrue(result.contains(item1), "Should contain item1 (8T)");
        assertFalse(result.contains(item2), "Should not contain item2");
        assertFalse(result.contains(item3), "Should not contain item3");
    }

    /**
     * 测试BrandWidth字段的like查询 - 匹配8GB/S
     */
    @Test
    public void testQueryBrandWidthLike8GB() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("BrandWidth like %8GB/S%");

        assertEquals(2, result.size(), "Should find 2 items with BrandWidth containing 8GB/S");
        assertTrue(result.contains(item1), "Should contain item1");
        assertFalse(result.contains(item2), "Should not contain item2");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试不存在的字段查询
     */
    @Test
    public void testQueryNonExistentField() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("NonExistentField like %value%");

        assertEquals(0, result.size(), "Should find no items for non-existent field");
    }

    /**
     * 测试无效的查询条件格式
     */
    @Test
    public void testQueryInvalidConditionFormat() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("InvalidConditionFormat");

        assertEquals(3, result.size(), "Invalid condition format should return all items");
        assertTrue(result.contains(item1), "Should contain item1");
        assertTrue(result.contains(item2), "Should contain item2");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试没有通配符的like查询
     */
    @Test
    public void testQueryLikeWithoutWildcards() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Speed like 5400");

        assertEquals(2, result.size(), "Should find 2 items containing 5400 in Speed");
        assertTrue(result.contains(item1), "Should contain item1 (5400)");
        assertTrue(result.contains(item2), "Should contain item2 (7200/5400)");
        assertFalse(result.contains(item3), "Should not contain item3 (9000)");
    }

    /**
     * 测试JSON序列化
     */
    @Test
    public void testJsonSerialization() {
        // 测试toJsonString
        String json = InstanceDynAttrValue.toJsonString(instanceDynAttrValue);
        assertNotNull(json, "JSON string should not be null");
        assertTrue(json.contains("instsValues"), "JSON should contain instsValues field");

        // 测试fromJsonString
        InstanceDynAttrValue deserialized = InstanceDynAttrValue.fromJsonString(json);
        assertNotNull(deserialized, "Deserialized object should not be null");
        assertEquals(3, deserialized.getInstsValues().size(), "Should have 3 items after deserialization");

        // 验证查询功能在反序列化后仍然工作
        List<InstanceDynAttrValueItem> queryResult = deserialized.query("Speed like %5400%");
        assertEquals(2, queryResult.size(), "Query should work after deserialization");
    }

    /**
     * 测试空列表的查询
     */
    @Test
    public void testQueryEmptyList() {
        InstanceDynAttrValue emptyValue = new InstanceDynAttrValue();

        List<InstanceDynAttrValueItem> result = emptyValue.query("Speed like %5400%");
        assertEquals(0, result.size(), "Empty list should return empty result");
    }

    /**
     * 测试多个通配符的查询
     */
    @Test
    public void testQueryMultipleWildcards() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Capacity like %T%");

        assertEquals(3, result.size(), "Should find all items with T in Capacity");
        assertTrue(result.contains(item1), "Should contain item1");
        assertTrue(result.contains(item2), "Should contain item2");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试Capacity字段的精确匹配查询
     */
    @Test
    public void testQueryCapacityEqual8T() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Capacity =8T");

        assertEquals(1, result.size(), "Should find 1 item with Capacity exactly equal to 8T");
        assertTrue(result.contains(item1), "Should contain item1 (8T)");
        assertFalse(result.contains(item2), "Should not contain item2 (16T)");
        assertFalse(result.contains(item3), "Should not contain item3 (4T)");
    }

    /**
     * 测试Speed字段的精确匹配查询
     */
    @Test
    public void testQuerySpeedEqual5400() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Speed =5400");

        assertEquals(1, result.size(), "Should find 1 item with Speed exactly equal to 5400");
        assertTrue(result.contains(item1), "Should contain item1 (5400)");
        assertFalse(result.contains(item2), "Should not contain item2 (7200/5400)");
        assertFalse(result.contains(item3), "Should not contain item3 (9000)");
    }

    /**
     * 测试BrandWidth字段的精确匹配查询
     */
    @Test
    public void testQueryBrandWidthEqual8GBS() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("BrandWidth =8GB/S");

        assertEquals(2, result.size(), "Should find 2 items with BrandWidth exactly equal to 8GB/S");
        assertTrue(result.contains(item1), "Should contain item1");
        assertFalse(result.contains(item2), "Should not contain item2 (16GB/S)");
        assertTrue(result.contains(item3), "Should contain item3");
    }

    /**
     * 测试精确匹配不存在的值
     */
    @Test
    public void testQueryEqualNonExistentValue() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("Capacity =32T");

        assertEquals(0, result.size(), "Should find no items with Capacity equal to 32T");
    }

    /**
     * 测试精确匹配不存在的字段
     */
    @Test
    public void testQueryEqualNonExistentField() {
        List<InstanceDynAttrValueItem> result = instanceDynAttrValue.query("NonExistentField =value");

        assertEquals(0, result.size(), "Should find no items for non-existent field");
    }

    /**
     * 测试精确匹配查询与like查询的对比
     */
    @Test
    public void testQueryEqualVsLikeComparison() {
        // 精确匹配
        List<InstanceDynAttrValueItem> equalResult = instanceDynAttrValue.query("Speed =5400");
        assertEquals(1, equalResult.size(), "Equal query should find exactly 1 item");

        // 模糊匹配
        List<InstanceDynAttrValueItem> likeResult = instanceDynAttrValue.query("Speed like %5400%");
        assertEquals(2, likeResult.size(), "Like query should find 2 items containing 5400");

        // 验证结果差异
        assertTrue(equalResult.size() < likeResult.size(), "Equal should return fewer results than like");
    }
}
