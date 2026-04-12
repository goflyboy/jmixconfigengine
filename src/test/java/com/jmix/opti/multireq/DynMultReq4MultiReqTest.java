package com.jmix.opti.multireq;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.logic.EffectScope;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class DynMultReq4MultiReqTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class DynMultReqMultiInstConstraint extends ConstraintAlgImplTestBase {

        // 硬盘部件分类定义--严格按层级结构定义（顺序很重要），部件的attrs也要按定义的顺序来
        @PartAnno(supportMultiInst = true)
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

        // // 改动4：原来属于分类2（多）的参数，由于属于整个分类2，当前多实例化，只能提升到产品级fatherCode = "drive"-> ""
        // @ParaAnno(fatherCode = "", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar driveSumCapacity;// 输入参数

        // @ParaAnno(fatherCode = "", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar driveSumQuantity; // 输入参数

        // // 改动4：原来属于分类2（多）的参数，由于属于整个分类2，当前多实例化，只能提升到产品级fatherCode = "drive"-> ""
        // @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumCapacity;// 输入参数

        // @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumQuantity; // 输入参数

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

        // @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar cpuSumCores; // 输入参数 TODO:不需要建立，应该从引擎干掉

        // @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumMemory; // 输入参数
        // 改动点3：分类2(多）多个请求的整体要求，需要有整体汇总

        // 改动点1：分类1（单)->分类2（多），需要设置为拆分为多条规则 Cardinality(1:N)
        @CodeRuleAnno(normalNaturalCode = "4核的CPU不兼容固态硬盘", leftProObjsStr = "cpu:CoreNum|Quantity|Select", rightProObjsStr = "drive:Type|Quantity|Select", effectScope = EffectScope.SingleInst)
        private void logicAB1() {
            // TODO： code的动态性怎么保障
            inCompatible("logicAB1", "cpu:CoreNum=4", "drive:Type=sd");
        }

        @CodeRuleAnno(fatherCode = "cpu", normalNaturalCode = "仅能使用一种CPU", leftProObjsStr = "cpu:Selected")
        private void logicA1() {
            AlgCPLinearExpr cpuSelected = sum4Selected("").name("cpuSelected");
            model.addLessOrEqual(cpuSelected, 1);
        }

        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块", leftProObjsStr = "drive:Select|Quantity", effectScope = EffectScope.SingleInst)
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

        // 改动点3：分类2(多）多个请求的整体要求，需要有整体汇总
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = " 优先使用高容量硬盘", strategy = PriorityStrategy.MIN, effectScope = EffectScope.AllInst)
        private void logicB2() {
            // 改动点3-1：增加sum4Quantity的partCatagoryCodesStr参数，支持多实例的情况
            PartAlgCPLinearExpr totalCapacity = sum4Quantity("drive", "Capacity",
                    "").name("totalCapacity");
            // 如果是容量需求
            // 改动点5：怎么判断 driveSumCapacity是否有输入？前置计算+ 输入条件判断？ --
            // if (driveSumCapacity.getIsHasInputed()) {
            if (drive.getSumSumParaByAttr("Capacity").getIsHasInputed()) {
                int requiredCapacity = drive.getSumSumParaByAttr("Capacity").getInputValue();

                // a1.满足输入容量需求 totalCapacity >= requiredCapacity
                model.addGreaterOrEqual(totalCapacity, requiredCapacity);

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");

                // a2.使用高容量硬盘 -> "被选择部件单容量总和越大越好"
                PartAlgCPLinearExpr highCapacityExpr = sum4Selected("drive", "Capacity", "")
                        .name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -100);

                // a3.在满足容量需求的前提下，容量越接近需求容量越好
                PartAlgCPLinearExpr excessCapacityExpr = model.newPartLinearExpr("excessCapacityExpr");
                excessCapacityExpr.addExpr(totalCapacity, 1);
                excessCapacityExpr.addConstant(-requiredCapacity);
                objectiveExpr.addExpr(excessCapacityExpr, 1);

                // a4.在满足容量需求的前提下， 配置的部件数量越少越好
                PartAlgCPLinearExpr excessQuantityExpr = sum4Quantity("drive", "", "")
                        .name("excessQuantityExpr");
                objectiveExpr.addExpr(excessQuantityExpr, 800);
                model.setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);

            } else {// 给的数量qty总数

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                // int requiredQuantity = driveSumQuantity.getInputValue();
                int requiredQuantity = drive.getSumSumParaByAttr("Quantity").getInputValue();
                // a1.满足输入总数量需求 totalQuantity >= requiredQuantity
                PartAlgCPLinearExpr totalQuantity = sum4Quantity("drive", "",
                        "").name("totalQuantity");
                model.addGreaterOrEqual(totalQuantity, requiredQuantity);

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFunQty");

                // a2.使用高容量硬盘 -> "被选择部件单容量总和越大越好"
                PartAlgCPLinearExpr highCapacityExpr = sum4Selected("drive", "Capacity", "")
                        .name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -1);

                // a3.在满足数量需求的前提下，数量越接近需求数量越好
                PartAlgCPLinearExpr excessQuantityExpr = sum4Quantity("drive", "", "")
                        .name("excessQuantityExpr");
                excessQuantityExpr.addConstant(-requiredQuantity);
                model.setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);
            }
        }

    }
    // ---------------模型的定义end----------------------------------------

    public DynMultReq4MultiReqTest() {
        super(DynMultReqMultiInstConstraint.class);
    }

    @Test
    public void oneReq() {
        // Natural-Input: 要求机械硬盘容量>=5T, 要求4核的CPU的总内存>=512G
        inferRecommendModule("drive:sum.Capacity >=5 where Speed=5400", "cpu:sum.Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        // 变化点5-1：要求为保持输入的简洁见，如果仅输出一个实例，则和原来多单实例一样，不加实例名
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
        assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
    }

    @Test
    public void twoReq() {
        // Natural-Input: 要求机械硬盘容量>=5T, 要求机械硬盘容量>=5T, 要求4核的CPU的总内存>=512G
        inferRecommendModule("drive:sum.Capacity >=5 where Speed=5400", "drive:sum.Capacity >=5 where Speed=5400",
                "cpu:sum.Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        // 变化点5-2：要求为保持输入的简洁见，如果输出多个实例，后面的需要加上实名名称I1
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*),I1_md1(Q:5,H:0,S:1),I1_sd1(0*)");
        assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*),I1_md1(Q:5,H:0,S:1),I1_sd1(0*)");
    }

}
