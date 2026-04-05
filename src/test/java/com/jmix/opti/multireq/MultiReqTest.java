package com.jmix.opti.multireq;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrInherit;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class MultiReqTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class MultiReqConstraint extends ConstraintAlgImplTestBase {

        // ==================== 硬盘部件分类定义（基础分类）====================
        @PartAnno(code = "drive")
        @DAttrAnno2(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING, optionExtSchema = "StringUnit", options = {
                "Speed_3000:3000:转",
                "Speed_9000:9000:转",
                "Speed_5400:5400:转",
                "Speed_9900:9900:转",
                "Speed_7200a5400:7200/5400:转",
                "Speed_7200:7200:转" }, instType = 0)
        @DAttrAnno3(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_1T:1:T",
                "Capacity_2T:2:T",
                "Capacity_3T:3:T",
                "Capacity_6T:6:T",
                "Capacity_9T:9:T",
                "Capacity_4T:4:T" }, instType = 0)
        private PartCategoryVar drive;

        // ==================== 硬盘实例1 (driveI0) - 继承drive的属性 ====================
        @PartAnno(code = "driveI0", fatherCode = "drive")
        @DAttrInherit(fatherCode = "drive")
        private PartCategoryVar driveI0;

        // ==================== 硬盘实例2 (driveI1) - 继承drive的属性 ====================
        @PartAnno(code = "driveI1", fatherCode = "drive")
        @DAttrInherit(fatherCode = "drive")
        private PartCategoryVar driveI1;

        // ==================== 部件实例定义 ====================
        // driveI0 下的固态硬盘
        @PartAnno(fatherCode = "driveI0", attrs = { "5400", "3" })
        private PartVar sd1_I0;

        @PartAnno(fatherCode = "driveI0", attrs = { "7200", "6" })
        private PartVar sd2_I0;

        // driveI0 下的机械硬盘
        @PartAnno(fatherCode = "driveI0", attrs = { "5400", "1" })
        private PartVar md1_I0;

        // driveI1 下的固态硬盘
        @PartAnno(fatherCode = "driveI1", attrs = { "5400", "3" })
        private PartVar sd1_I1;

        // driveI1 下的机械硬盘
        @PartAnno(fatherCode = "driveI1", attrs = { "7200", "4" })
        private PartVar md2_I1;

        // ==================== CPU部件分类定义 ====================
        @PartAnno(code = "cpu")
        @DAttrAnno2(code = "CoreNum", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "CoreNum_2:2:核",
                "CoreNum_4:4:核",
                "CoreNum_8:8:核",
                "CoreNum_18:18:核" }, instType = 0)
        @DAttrAnno3(code = "ConfigType", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "ConfigType_2:2:配置",
                "ConfigType_5:5:配置" }, instType = 0)
        private PartCategoryVar cpu;

        // CPU实例
        @PartAnno(fatherCode = "cpu", attrs = { "2", "2" })
        private PartVar cpu1;

        @PartAnno(fatherCode = "cpu", attrs = { "4", "2" })
        private PartVar cpu2;

        @PartAnno(fatherCode = "cpu", attrs = { "8", "5" })
        private PartVar cpu3;

        // ==================== 规则定义 ====================

        @CodeRuleAnno(fatherCode = "cpu", normalNaturalCode = "仅能使用一种CPU")
        private void logicA1() {
            AlgCPLinearExpr cpuSelected = sum4Selected("").name("cpuSelected");
            model.addLessOrEqual(cpuSelected, 1);
        }

        // driveI0 实例规则：仅能使用一种硬盘
        @CodeRuleAnno(fatherCode = "driveI0", normalNaturalCode = "仅能使用一种硬盘")
        private void logicB1_I0() {
            AlgCPLinearExpr totalSelected = sum4Selected("").name("totalSelected_I0");
            model.addLessOrEqual(totalSelected, 1);
        }

        // driveI1 实例规则：仅能使用一种硬盘
        @CodeRuleAnno(fatherCode = "driveI1", normalNaturalCode = "仅能使用一种硬盘")
        private void logicB1_I1() {
            AlgCPLinearExpr totalSelected = sum4Selected("").name("totalSelected_I1");
            model.addLessOrEqual(totalSelected, 1);
        }

        // 多实例整体规则：硬盘总容量目标函数（验证 sum4Quantity 的多实例功能）
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "最大化总容量", strategy = PriorityStrategy.MAX)
        private void logicB2() {
            // 定义容量总和表达式（支持多实例：driveI0,driveI1）
            PartAlgCPLinearExpr totalCapacity = sum4Quantity("driveI0,driveI1", "Capacity", "").name("totalCapacity");

            // 创建目标函数 - 最大化总容量
            PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");
            objectiveExpr.addExpr(totalCapacity, 1);
            model.setObjectExpr(objectiveExpr);
        }
    }
    // ---------------模型的定义end----------------------------------------

    // ---------------测试用例----------------------------------------
    public MultiReqTest() {
        super(MultiReqConstraint.class);
    }

    @Test
    public void oneReq() {
        // 执行优化 - 验证多实例求和功能
        inferRecommend("drive", "drive:sum.Quantity >=1");

        // 打印解决方案
        printSimpleSolutions();

        // 验证结果 - 有解决方案即可（CPU至少选一个）
        assertSoluContain(1, "cpu1");
    }
}
