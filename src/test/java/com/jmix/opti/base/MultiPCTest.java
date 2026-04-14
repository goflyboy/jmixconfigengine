package com.jmix.opti.base;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class MultiPCTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class MultiPCConstraint extends ConstraintAlgImplTestBase {

        // 硬盘部件分类定义--严格按层级结构定义（顺序很重要），部件的attrs也要按定义的顺序来
        @PartAnno()
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING, optionExtSchema = "StringUnit", options = {
                "Speed_3000:3000:转",
                "Speed_9000:9000:转",
                "Speed_5400:5400:转",
                "Speed_9900:9900:转",
                "Speed_7200a5400:7200/5400:转",
                "Speed_7200:7200:转" })
        @DAttrAnno2(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_1T:1:T",
                "Capacity_2T:2:T",
                "Capacity_3T:3:T",
                "Capacity_6T:6:T",
                "Capacity_9T:9:T",
                "Capacity_4T:4:T" })
        @DAttrAnno3(code = "Type", dynAttrType = DynamicAttributeType.E_STRING, options = { "SD:sd",
                "MD:md" }, instType = 0)
        private PartCategoryVar drive;

        // 固态硬盘实例1
        @PartAnno(fatherCode = "drive", attrs = { "5400", "3", "sd" }, price = 50)
        private PartVar sd1;

        // 固态硬盘实例2
        @PartAnno(fatherCode = "drive", attrs = { "7200", "6", "sd" }, price = 80)
        private PartVar sd2;

        // 固态硬盘实例3
        @PartAnno(fatherCode = "drive", attrs = { "9000", "9", "sd" }, price = 90)
        private PartVar sd3;

        // 机械硬盘实例1
        @PartAnno(fatherCode = "drive", attrs = { "5400", "1", "md" }, price = 30)
        private PartVar md1;

        // 机械硬盘实例2
        @PartAnno(fatherCode = "drive", attrs = { "7200", "2", "md" }, price = 40)
        private PartVar md2;

        // 机械硬盘实例3
        @PartAnno(fatherCode = "drive", attrs = { "9000", "3", "md" }, price = 60)
        private PartVar md3;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar SumCapacity;// 输入参数

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar SumQuantity; // 输入参数

        // ==================== CPU部件分类定义 ====================
        @PartAnno()
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "CoreNum_2:2:核",
                "CoreNum_4:4:核",
                "CoreNum_8:8:核",
                "CoreNum_18:18:核" })
        @DAttrAnno2(code = "Memory", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "Memory_123:123:G",
                "Memory_256:256:G",
                "Memory_512:512:G",
                "Memory_1024:1024:G" })
        @DAttrAnno3(code = "ConfigType", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "ConfigType_2:2:配置",
                "ConfigType_5:5:配置" })
        private PartCategoryVar cpu;

        // CPU实例1: CoreNum=2, Memory=123, ConfigType=2
        @PartAnno(fatherCode = "cpu", attrs = { "2", "123", "2" }, price = 100)
        private PartVar cpu1;

        // CPU实例2: CoreNum=4, Memory=256, ConfigType=2
        @PartAnno(fatherCode = "cpu", attrs = { "4", "256", "2" }, price = 200)
        private PartVar cpu2;

        // CPU实例3: CoreNum=8, Memory=512, ConfigType=5
        @PartAnno(fatherCode = "cpu", attrs = { "8", "512", "5" }, price = 400)
        private PartVar cpu3;

        // CPU实例4: CoreNum=18, Memory=1024, ConfigType=5
        @PartAnno(fatherCode = "cpu", attrs = { "18", "1024", "5" }, price = 800)
        private PartVar cpu4;

        @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar cpuSumCores; // 输入参数 TODO:不需要建立，应该从引擎干掉

        @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar SumMemory; // 输入参数

        @CodeRuleAnno(normalNaturalCode = "4核的CPU不兼容固态硬盘")
        private void logicAB1() {
            inCompatible("logicAB1", "cpu:CoreNum=4", "drive:Type=sd");
        }

        // TODO：进一步抽取单选型CPU
        @CodeRuleAnno(fatherCode = "cpu", normalNaturalCode = "仅能使用一种CPU")
        private void logicA1() {
            AlgCPLinearExpr cpuSelected = sum4Selected("").name("cpuSelected");
            model.addLessOrEqual(cpuSelected, 1);
        }

        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
        private void logicB1() {
            // proRule1-natuarl: 固态硬盘必须配置同一种，并且最多配置2块
            // proRule1-dsl: 拆分为proRule11和proRule11两条约束（和isSelected(S)、qty(Q)相关）
            // proRule11-cRule: sd1.S + sd2.S <=1
            AlgCPLinearExpr sdTypeSumNum = sum4Selected("Type=sd").name("sdTypeSumNum");
            model.addLessOrEqual(sdTypeSumNum, 1);

            // proRule12-cRule: sd1.Q + sd2.Q <= 2
            AlgCPLinearExpr sdTypeSumQty = sum4Quantity("Type=sd").name("sdTypeSumQty");
            model.addLessOrEqual(sdTypeSumQty, 2);
        }

        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = " 优先使用高容量硬盘", strategy = PriorityStrategy.MIN)
        private void logicB2() {

            PartAlgCPLinearExpr totalCapacity = sum4Quantity("Capacity", "").name("totalCapacity");
            // 如果是容量需求
            if (SumCapacity.getIsHasInputed()) {
                int requiredCapacity = SumCapacity.getInputValue();

                // a1.满足输入容量需求 totalCapacity >= requiredCapacity
                model.addGreaterOrEqual(totalCapacity, requiredCapacity);

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");

                // a2.使用高容量硬盘 -> "被选择部件单容量总和越大越好"
                PartAlgCPLinearExpr highCapacityExpr = sum4Selected("Capacity", "").name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -100);

                // a3.在满足容量需求的前提下，容量越接近需求容量越好
                PartAlgCPLinearExpr excessCapacityExpr = model.newPartLinearExpr("excessCapacityExpr");
                excessCapacityExpr.addExpr(totalCapacity, 1);
                excessCapacityExpr.addConstant(-requiredCapacity);
                objectiveExpr.addExpr(excessCapacityExpr, 1);

                // a4.在满足容量需求的前提下， 配置的部件数量越少越好
                PartAlgCPLinearExpr excessQuantityExpr = sum4Quantity("", "").name("excessQuantityExpr");
                objectiveExpr.addExpr(excessQuantityExpr, 800);
                model.setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);

            } else {// 给的数量qty总数

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                int requiredQuantity = SumQuantity.getInputValue();
                // a1.满足输入总数量需求 totalQuantity >= requiredQuantity
                PartAlgCPLinearExpr totalQuantity = sum4Quantity("", "").name("totalQuantity");
                model.addGreaterOrEqual(totalQuantity, requiredQuantity);

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFunQty");

                // a2.使用高容量硬盘 -> "被选择部件单容量总和越大越好"
                PartAlgCPLinearExpr highCapacityExpr = sum4Selected("Capacity", "").name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -1);

                // a3.在满足数量需求的前提下，数量越接近需求数量越好
                PartAlgCPLinearExpr excessQuantityExpr = sum4Quantity("", "").name("excessQuantityExpr");
                excessQuantityExpr.addConstant(-requiredQuantity);
                model.setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);
            }
        }

    }
    // ---------------模型的定义end----------------------------------------

    public MultiPCTest() {
        super(MultiPCConstraint.class);
    }

    // 用例维度设计
    // 维度1-DA: 分类输入(Input参数)
    // DA1-第一个有要求（前 ）
    // DA2-第二个有要求（后）
    // DA3 两个都有要求
    // 维度2-DB：规则，规则类型，层级，多个优先级规则
    // DB1-分类-非优先LogicB2
    // DB2-分类-优先LogicA1,LogicA2
    // DB3-分类间-LogicAB，左右是否空，Left,right
    @Test
    public void mixInCompatibleLeftYRightY() {
        // Natural-Input: 要求5400速率的硬盘容量>=5T, 要求4核的CPU的总内存>=512G
        // DSL-Input: drive:Sum_Capacity >=5 where Speed=5400, cpu:Sum_Memory >=512
        // where CoreNum=4
        // Struct-Input: req1F,req1C(sd1.Q*3 + md1*1 >=5), req2F,req2C(cpu2.Q*256 >=512)
        // 测试点：
        // 维度1-DA:DA3
        // 维度2-DB:DB3(Left-Y,Right-Y)

        // 预期结果：
        // 解1： cpu2.Q=2 md1.Q=5
        // --过滤，执行req1F,req2F: -->drive:sd1,md1, cpu:cpu2
        // --产品规则实例化:
        // ----LogicA1:cpu2.S<=1
        // ----LogicB1:sd1.S +md1.S <=1,sd1.Q<=2 --生效
        // ----LogicB2: (cpu2.S) InCompatible (sd1.S) (Left-Y,Right-Y)
        // ----目标函数： xxx
        // --关键执行过程：
        // ----1. "4核的CPU的总内存>=512G" -->cpu2.Q =2
        // ---LogicAB1(排除sd1) + "5400速率"(sd1,md1),只有md1，满足要求
        // --根据req1C,的md1.Q = 5
        inferRecommendModule("drive:Sum_Capacity >=5 where Speed=5400", "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
        assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
    }

    @Test
    public void mixInCompatibleLeftYRightN() {
        // Natural-Input: 要求机械硬盘容量>=5T, 要求4核的CPU的总内存>=512G

        inferRecommendModule("drive:Sum_Capacity >=5 where Type=md", "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        assertSoluContain(1, "cpu2(Q:20,H:0,S:1),md1(0*),md2(Q:1,H:0,S:1),md3(Q:1,H:0,S:1)");
        // TODO: 怎么让cpu1从1开始
    }
}
