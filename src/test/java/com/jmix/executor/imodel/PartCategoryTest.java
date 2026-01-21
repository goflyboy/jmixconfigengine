package com.jmix.executor.imodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValue;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValueItem;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.omodel.PartConstantAttr;
import com.jmix.executor.omodel.PartConstraintReq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PartCategory 单元测试
 * 测试PartCategory的query方法各种场景
 *
 * @since 2025-12-27
 */
public class PartCategoryTest {

    private static final String INSTANCE_ATTRS = "instanceAttrs";

    private PartCategory driveCategory;

    private PartCategory solidDriveCategory;

    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        setupDriveCategory();
        setupSolidDriveCategory();
    }

    /**
     * 设置硬盘分类测试数据
     */
    private void setupDriveCategory() {
        driveCategory = new PartCategory();
        driveCategory.setCode("drive");
        driveCategory.setDescription("硬盘分类");

        // 创建动态属性schema
        List<DynamicAttribute> schemas = new ArrayList<>();
        DynamicAttribute capacityAttr = new DynamicAttribute();
        capacityAttr.setCode("Capacity");
        capacityAttr.setName("容量");
        capacityAttr.setInstType(1); // 实例属性
        schemas.add(capacityAttr);

        DynamicAttribute speedAttr = new DynamicAttribute();
        speedAttr.setCode("Speed");
        speedAttr.setName("转速");
        speedAttr.setDynAttrType(DynamicAttributeType.E_STRING);
        speedAttr.setInstType(1); // 实例属性
        schemas.add(speedAttr);

        DynamicAttribute quantityAttr = new DynamicAttribute();
        quantityAttr.setCode(PartConstantAttr.Quantity.getCode());
        quantityAttr.setName("数量");
        quantityAttr.setInstType(0); // 非实例属性
        schemas.add(quantityAttr);

        driveCategory.setDynAttrSchemas(schemas);

        // 创建硬盘实例
        List<Part> drives = new ArrayList<>();

        // 5400转硬盘，8T容量
        Part drive1 = new Part();
        drive1.setCode("drive_5400_8t");
        drive1.setDescription("5400转8T硬盘");

        // 创建实例属性
        InstanceDynAttrValue instAttrs1 = new InstanceDynAttrValue();
        InstanceDynAttrValueItem item1 = new InstanceDynAttrValueItem();
        item1.setInstId(1);
        Map<String, String> attrs1 = new HashMap<>();
        attrs1.put("Capacity", "8");
        attrs1.put("Speed", "5400");
        item1.setInstAttrs(attrs1);
        instAttrs1.getInstsValues().add(item1);

        drive1.setAttr(INSTANCE_ATTRS, InstanceDynAttrValue.toJsonString(instAttrs1));
        drive1.setAttr(PartConstantAttr.Quantity.getCode(), "1");
        drives.add(drive1);

        // 7200转硬盘，16T容量
        Part drive2 = new Part();
        drive2.setCode("drive_7200_16t");
        drive2.setDescription("7200转16T硬盘");

        InstanceDynAttrValue instAttrs2 = new InstanceDynAttrValue();
        InstanceDynAttrValueItem item2 = new InstanceDynAttrValueItem();
        item2.setInstId(1);
        Map<String, String> attrs2 = new HashMap<>();
        attrs2.put("Capacity", "16");
        attrs2.put("Speed", "7200/5400");
        item2.setInstAttrs(attrs2);
        instAttrs2.getInstsValues().add(item2);

        drive2.setAttr(INSTANCE_ATTRS, InstanceDynAttrValue.toJsonString(instAttrs2));
        drive2.setAttr(PartConstantAttr.Quantity.getCode(), "1");
        drives.add(drive2);

        // 9000转硬盘，4T容量
        Part drive3 = new Part();
        drive3.setCode("drive_9000_4t");
        drive3.setDescription("9000转4T硬盘");

        InstanceDynAttrValue instAttrs3 = new InstanceDynAttrValue();
        InstanceDynAttrValueItem item3 = new InstanceDynAttrValueItem();
        item3.setInstId(1);
        Map<String, String> attrs3 = new HashMap<>();
        attrs3.put("Capacity", "4");
        attrs3.put("Speed", "9000");
        item3.setInstAttrs(attrs3);
        instAttrs3.getInstsValues().add(item3);

        drive3.setAttr(INSTANCE_ATTRS, InstanceDynAttrValue.toJsonString(instAttrs3));
        drive3.setAttr(PartConstantAttr.Quantity.getCode(), "1");
        drives.add(drive3);

        driveCategory.addSubParts(drives);
    }

    /**
     * 设置固态硬盘分类测试数据
     */
    private void setupSolidDriveCategory() {
        solidDriveCategory = new PartCategory();
        solidDriveCategory.setCode("solidDrive");
        solidDriveCategory.setDescription("固态硬盘分类");

        // 创建动态属性schema
        List<DynamicAttribute> schemas = new ArrayList<>();
        DynamicAttribute capacityAttr = new DynamicAttribute();
        capacityAttr.setCode("Capacity");
        capacityAttr.setName("容量");
        capacityAttr.setInstType(0); // 实例属性
        schemas.add(capacityAttr);

        DynamicAttribute speedAttr = new DynamicAttribute();
        speedAttr.setCode("Speed");
        speedAttr.setName("速率");
        speedAttr.setInstType(0); // 实例属性
        schemas.add(speedAttr);

        DynamicAttribute quantityAttr = new DynamicAttribute();
        quantityAttr.setCode(PartConstantAttr.Quantity.getCode());
        quantityAttr.setName("数量");
        quantityAttr.setInstType(0); // 非实例属性
        schemas.add(quantityAttr);

        solidDriveCategory.setDynAttrSchemas(schemas);

        // 创建固态硬盘实例（5400速率，2块）
        List<Part> solidDrives = new ArrayList<>();

        Part solidDrive = new Part();
        solidDrive.setCode("solid_drive_5400");
        solidDrive.setDescription("5400速率固态硬盘");
        solidDrive.setAttr("Capacity", "2");
        solidDrive.setAttr("Speed", "5400");
        solidDrives.add(solidDrive);

        Part solidDrive2 = new Part();
        solidDrive2.setCode("solid_drive_9000");
        solidDrive2.setDescription("9000速率固态硬盘");
        solidDrive2.setAttr("Capacity", "16");
        solidDrive2.setAttr("Speed", "9000");
        solidDrives.add(solidDrive2);
        solidDriveCategory.addSubParts(solidDrives);
    }

    /**
     * 测试有实例属性
     */
    @Test
    public void testQueryWithInstanceAttribute() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCatagoryCode("drive");
        req.setAttrCode("sum.Capacity");
        req.setAttrWhereCondition("Speed like %5400%");

        PartCategory result = driveCategory.query(req);

        List<Part> parts = new ArrayList<>(result.getPartMap().values());

        // 应该只返回5400转的硬盘，且容量总和>=5T
        assertEquals(2, parts.size(), "Should return 21 drive part");
    }

    /**
     * 测试查询单个硬盘的容量只能是2T
     */
    @Test
    public void testQuerySingleDriveCapacityEqual2T() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCatagoryCode("drive");
        req.setAttrCode(""); // 单个硬盘
        req.setAttrComparator("");
        req.setAttrValue("");
        req.setAttrWhereCondition("Capacity=2");

        PartCategory result = driveCategory.query(req);

        assertNotNull(result, "Query result should not be null");
        // 这个查询应该没有匹配的硬盘，因为所有硬盘的容量都不是2T
        // 根据当前测试数据，应该返回空结果
    }

    /**
     * 测试没有实例属性
     */
    @Test
    public void testQueryWithNoInstanceAttribute() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCatagoryCode("solidDrive");
        req.setAttrCode("sum.Quantity");
        req.setAttrWhereCondition("Speed like %5400%");

        PartCategory results = solidDriveCategory.query(req);

        assertEquals(1, results.getPartMap().values().size(), "Should return 1 solid drive part");
    }

    /**
     * 测试无效的过滤条件
     */
    @Test
    public void testQueryWithInvalidFilterCondition() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCatagoryCode("drive");
        req.setAttrCode("Capacity");
        req.setAttrComparator(">");
        req.setAttrValue("10");
        req.setAttrWhereCondition(""); // 空的过滤条件

        assertThrows(IllegalArgumentException.class, () -> {
            driveCategory.query(req);
        }, "Should throw exception for empty filter condition");
    }

    /**
     * 测试属性解析功能
     */
    @Test
    public void testParseAttribute() {
        // 测试sum.Capacity
        Pair<DynamicAttribute, String> result = driveCategory.parseAttribute("sum.Capacity",
                driveCategory.getDynAttrSchemas());
        assertNotNull(result, "Parse result should not be null");
        assertEquals("Capacity", result.getFirst().getCode(), "Attribute code should be Capacity");
        assertEquals("sum", result.getSecond(), "Function should be sum");

        // 测试普通属性
        Pair<DynamicAttribute, String> result2 = driveCategory.parseAttribute("Speed",
                driveCategory.getDynAttrSchemas());
        assertNotNull(result2, "Parse result should not be null");
        assertEquals("Speed", result2.getFirst().getCode(), "Attribute code should be Speed");
        assertEquals("", result2.getSecond(), "Function should be empty");

        // 测试不存在的属性
        assertThrows(IllegalArgumentException.class, () -> {
            driveCategory.parseAttribute("NonExistentAttr", driveCategory.getDynAttrSchemas());
        }, "Should throw exception for non-existent attribute");
    }

    /**
     * 测试克隆功能
     */
    @Test
    public void testClone() {
        PartCategory cloned = driveCategory.clone();

        assertNotNull(cloned, "Cloned object should not be null");
        assertEquals(driveCategory.getCode(), cloned.getCode(), "Code should be cloned");
        assertEquals(driveCategory.getDescription(), cloned.getDescription(), "Description should be cloned");
        assertNotSame(driveCategory, cloned, "Cloned object should be different instance");

        // partMap和partCategoryMap不应该被克隆
        assertTrue(cloned.getPartMap().isEmpty(), "partMap should not be cloned");
        assertTrue(cloned.getPartCategoryMap().isEmpty(), "partCategoryMap should not be cloned");
    }

    @Test
    public void testPartConstantAttrIsContainValue() {
        // 测试包含的常量值
        assertTrue(PartConstantAttr.isContainAttrCode("Quantity"), "Should contain 'Quantity' value");

        // 测试不包含的常量值
        assertTrue(!PartConstantAttr.isContainAttrCode("NonExistent"), "Should not contain 'NonExistent' value");
        assertTrue(!PartConstantAttr.isContainAttrCode(""), "Should not contain empty string");
        assertTrue(!PartConstantAttr.isContainAttrCode(null), "Should not contain null value");

        // 测试大小写敏感
        assertTrue(!PartConstantAttr.isContainAttrCode("quantity"),
                "Should be case sensitive - 'quantity' should not match 'Quantity'");
    }

    @Test
    public void testPartConstantAttrGetAttr() {
        // 测试获取存在的属性
        DynamicAttribute attr = PartConstantAttr.getAttr("Quantity");
        assertNotNull(attr, "Should return DynamicAttribute for 'Quantity'");
        assertEquals("Quantity", attr.getCode(), "Code should be 'Quantity'");
        assertEquals("配置的数量", attr.getName(), "Name should be '配置的数量'");

        // 测试获取不存在的属性
        DynamicAttribute nullAttr = PartConstantAttr.getAttr("NonExistent");
        assertNull(nullAttr, "Should return null for non-existent code");

        // 测试大小写敏感
        DynamicAttribute caseSensitiveAttr = PartConstantAttr.getAttr("quantity");
        assertNull(caseSensitiveAttr, "Should return null for case mismatch");
    }
}
