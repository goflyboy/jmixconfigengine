package com.jmix.executor.imodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.impl.util.Pair;
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
        speedAttr.setInstType(1); // 实例属性
        schemas.add(speedAttr);

        DynamicAttribute quantityAttr = new DynamicAttribute();
        quantityAttr.setCode(PartConstantAttr.Quantity);
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
        attrs1.put("Capacity", "8T");
        attrs1.put("Speed", "5400");
        item1.setInstAttrs(attrs1);
        instAttrs1.getInstsValues().add(item1);

        drive1.setAttr("instanceAttrs", InstanceDynAttrValue.toJsonString(instAttrs1));
        drive1.setAttr(PartConstantAttr.Quantity, "1");
        drives.add(drive1);

        // 7200转硬盘，16T容量
        Part drive2 = new Part();
        drive2.setCode("drive_7200_16t");
        drive2.setDescription("7200转16T硬盘");

        InstanceDynAttrValue instAttrs2 = new InstanceDynAttrValue();
        InstanceDynAttrValueItem item2 = new InstanceDynAttrValueItem();
        item2.setInstId(1);
        Map<String, String> attrs2 = new HashMap<>();
        attrs2.put("Capacity", "16T");
        attrs2.put("Speed", "7200/5400");
        item2.setInstAttrs(attrs2);
        instAttrs2.getInstsValues().add(item2);

        drive2.setAttr("instanceAttrs", InstanceDynAttrValue.toJsonString(instAttrs2));
        drive2.setAttr(PartConstantAttr.Quantity, "1");
        drives.add(drive2);

        // 9000转硬盘，4T容量
        Part drive3 = new Part();
        drive3.setCode("drive_9000_4t");
        drive3.setDescription("9000转4T硬盘");

        InstanceDynAttrValue instAttrs3 = new InstanceDynAttrValue();
        InstanceDynAttrValueItem item3 = new InstanceDynAttrValueItem();
        item3.setInstId(1);
        Map<String, String> attrs3 = new HashMap<>();
        attrs3.put("Capacity", "4T");
        attrs3.put("Speed", "9000");
        item3.setInstAttrs(attrs3);
        instAttrs3.getInstsValues().add(item3);

        drive3.setAttr("instanceAttrs", InstanceDynAttrValue.toJsonString(instAttrs3));
        drive3.setAttr(PartConstantAttr.Quantity, "1");
        drives.add(drive3);

        driveCategory.addPart(drives);
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
        capacityAttr.setInstType(1); // 实例属性
        schemas.add(capacityAttr);

        DynamicAttribute speedAttr = new DynamicAttribute();
        speedAttr.setCode("Speed");
        speedAttr.setName("速率");
        speedAttr.setInstType(1); // 实例属性
        schemas.add(speedAttr);

        DynamicAttribute quantityAttr = new DynamicAttribute();
        quantityAttr.setCode(PartConstantAttr.Quantity);
        quantityAttr.setName("数量");
        quantityAttr.setInstType(0); // 非实例属性
        schemas.add(quantityAttr);

        solidDriveCategory.setDynAttrSchemas(schemas);

        // 创建固态硬盘实例（5400速率，2块）
        List<Part> solidDrives = new ArrayList<>();

        Part solidDrive = new Part();
        solidDrive.setCode("solid_drive_5400");
        solidDrive.setDescription("5400速率固态硬盘");

        // 创建2个实例
        InstanceDynAttrValue instAttrs = new InstanceDynAttrValue();
        for (int i = 1; i <= 2; i++) {
            InstanceDynAttrValueItem item = new InstanceDynAttrValueItem();
            item.setInstId(i);
            Map<String, String> attrs = new HashMap<>();
            attrs.put("Capacity", "2T");
            attrs.put("Speed", "5400");
            item.setInstAttrs(attrs);
            instAttrs.getInstsValues().add(item);
        }

        solidDrive.setAttr("instanceAttrs", InstanceDynAttrValue.toJsonString(instAttrs));
        solidDrive.setAttr(PartConstantAttr.Quantity, "2");
        solidDrives.add(solidDrive);

        solidDriveCategory.addPart(solidDrives);
    }

    /**
     * 测试查询5400速率的硬盘容量>=5T
     */
    @Test
    public void testQuery5400SpeedDrivesCapacityGreaterThan5T() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCategory("drive");
        req.setAttrCode("sum.Capacity");
        req.setAttrComparator(">=");
        req.setAttrValue("5");
        req.setAttrWhereCondition("Speed like %5400%");

        List<PartCategory> results = driveCategory.query(req);

        assertNotNull(results, "Query result should not be null");
        assertEquals(1, results.size(), "Should return 1 result category");

        PartCategory resultCategory = results.get(0);
        List<Part> parts = new ArrayList<>(resultCategory.getPartMap().values());

        // 应该只返回5400转的硬盘，且容量总和>=5T
        assertEquals(1, parts.size(), "Should return 1 drive part");
        Part drive = parts.get(0);
        assertEquals("drive_5400_8t", drive.getCode(), "Should return the 5400rpm 8T drive");
    }

    /**
     * 测试查询单个硬盘的容量只能是2T
     */
    @Test
    public void testQuerySingleDriveCapacityEqual2T() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCategory("drive");
        req.setAttrCode(""); // 单个硬盘
        req.setAttrComparator("");
        req.setAttrValue("");
        req.setAttrWhereCondition("Capacity=2T");

        List<PartCategory> results = driveCategory.query(req);

        assertNotNull(results, "Query result should not be null");
        // 这个查询应该没有匹配的硬盘，因为所有硬盘的容量都不是2T
        // 根据当前测试数据，应该返回空结果
    }

    /**
     * 测试查询5400速率的固态硬盘2块
     */
    @Test
    public void testQuery5400SpeedSolidDrivesQuantity2() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCategory("solidDrive");
        req.setAttrCode("sum.Quantity");
        req.setAttrComparator("==");
        req.setAttrValue("2");
        req.setAttrWhereCondition("Speed like %5400%");

        List<PartCategory> results = solidDriveCategory.query(req);

        assertNotNull(results, "Query result should not be null");
        assertEquals(1, results.size(), "Should return 1 result category");

        PartCategory resultCategory = results.get(0);
        List<Part> parts = new ArrayList<>(resultCategory.getPartMap().values());

        assertEquals(1, parts.size(), "Should return 1 solid drive part");
        Part solidDrive = parts.get(0);
        assertEquals("solid_drive_5400", solidDrive.getCode(), "Should return the 5400 speed solid drive");
        assertEquals("2", solidDrive.getAttr(PartConstantAttr.Quantity), "Should have quantity 2");
    }

    /**
     * 测试无效的过滤条件
     */
    @Test
    public void testQueryWithInvalidFilterCondition() {
        PartConstraintReq req = new PartConstraintReq();
        req.setPartCategory("drive");
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
}
