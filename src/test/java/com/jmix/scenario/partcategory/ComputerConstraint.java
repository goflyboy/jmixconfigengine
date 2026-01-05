package com.jmix.scenario.partcategory;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.executor.imodel.anno.DAttrAnno1;
import com.jmix.executor.imodel.anno.DAttrAnno11;
import com.jmix.executor.imodel.anno.DAttrAnno2;
import com.jmix.executor.imodel.anno.DAttrAnno3;
import com.jmix.executor.imodel.anno.DAttrAnno4;
import com.jmix.executor.imodel.anno.DAttrInherit;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.PartAnno;
import com.jmix.executor.impl.algmodel.PartCategoryVar;

/**
 * 计算机配件约束算法
 * 实现计算机配件配置的约束规则
 *
 * @since 2025-12-27
 */
@ModuleAnno(id = 124L)
public class ComputerConstraint extends ConstraintAlgImplTestBase {

    // 硬盘部件分类定义
    @PartAnno(code = "drive")
    @DAttrAnno1(code = "BrandWidth", optionExtSchema = "IntegerUnit", options = { "BW_8GB:8:GB/S", "BW_16GB:16:GB/S" })
    @DAttrAnno2(code = "Speed", optionExtSchema = "StringUnit", options = { "Speed_5400:5400:转", "Speed_9000:9000:转",
            "Speed_7200a5400:7200/5400:转" }, instType = 0)
    @DAttrAnno3(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_8T:8:T", "Capacity_16T:16:T",
            "Capacity_32T:32:T" }, instType = 0)
    @DAttrAnno4(code = "capacityWeight", optionExtSchema = "IntegerUnit", options = { "CW_10:10", "CW_20:20",
            "CW_30:30",
            "CW_100:100", "CW_200:200", "CW_300:300" }, instType = 0)
    private PartCategoryVar driveVar;

    // 固态硬盘部件分类，继承driveVar并重写属性
    @PartAnno(code = "solidDrive", fatherCode = "drive")
    @DAttrInherit(fatherCode = "driveVar", overrideAttrs = { "Speed:instType=1", "Capacity:instType=1" })
    @DAttrAnno11(code = "maxCapacity", optionExtSchema = "IntegerUnit", options = { "MaxCapacity_8T:8:T",
            "MaxCapacity_16T:16:T" })
    private PartCategoryVar solidDrive;

    // 机械硬盘部件分类，继承driveVar
    @PartAnno(code = "mechDrive", fatherCode = "drive")
    @DAttrInherit(fatherCode = "driveVar")
    private PartCategoryVar mechDrive;

    // 固态硬盘实例1
    @PartAnno(fatherCode = "solidDrive", attrs = { "BW_8GB", "MaxCapacity_8T", "CW_100" }, attrsInst1 = { "Speed_5400",
            "Capacity_8T" }, attrsInst2 = { "Speed_7200a5400", "Capacity_8T" })
    private PartVar solidDrive1;

    // 固态硬盘实例2
    @PartAnno(fatherCode = "solidDrive", attrs = { "BW_16GB", "MaxCapacity_16T", "CW_200" }, attrsInst1 = {
            "Speed_9000",
            "Capacity_16T" }, attrsInst2 = { "Speed_7200a5400", "Capacity_16T" })
    private PartVar solidDrive2;

    // 固态硬盘实例3
    @PartAnno(fatherCode = "solidDrive", attrs = { "BW_16GB", "MaxCapacity_16T", "CW_300" }, attrsInst1 = {
            "Speed_9000",
            "Capacity_16T" })
    private PartVar solidDrive3;

    // 机械硬盘实例1
    @PartAnno(fatherCode = "mechDrive", attrs = { "BW_8GB", "Speed_5400", "Capacity_8T", "CW_30" })
    private PartVar mechDrive1;

    // 机械硬盘实例2
    @PartAnno(fatherCode = "mechDrive", attrs = { "BW_16GB", "Speed_7200a5400", "Capacity_16T", "CW_20" })
    private PartVar mechDrive2;

    // 机械硬盘实例3
    @PartAnno(fatherCode = "mechDrive", attrs = { "BW_16GB", "Speed_9000", "Capacity_16T", "CW_10" })
    private PartVar mechDrive3;
}
