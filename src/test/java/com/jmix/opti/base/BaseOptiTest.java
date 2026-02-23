package com.jmix.opti.base;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrInherit;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import com.google.ortools.sat.BoolVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class BaseOptiTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class BaseOptiConstraint extends ConstraintAlgImplTestBase {
        // 硬盘部件分类定义--严格按层级结构定义（顺序很重要），部件的attrs也要按定义的顺序来
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

        // 固态硬盘部件分类，继承driveVar并重写属性
        @PartAnno(code = "sd", fatherCode = "drive")
        @DAttrInherit(fatherCode = "driveVar")
        private PartCategoryVar sd;

        // 机械硬盘部件分类，继承driveVar
        @PartAnno(code = "md", fatherCode = "drive")
        @DAttrInherit(fatherCode = "driveVar")
        private PartCategoryVar md;

        // 固态硬盘实例1
        @PartAnno(fatherCode = "sd", attrs = { "5400", "3" })
        private PartVar sd1;

        // 固态硬盘实例2
        @PartAnno(fatherCode = "sd", attrs = { "7200", "6" })
        private PartVar sd2;

        // 固态硬盘实例3
        @PartAnno(fatherCode = "sd", attrs = { "9000", "9" })
        private PartVar sd3;

        // 机械硬盘实例1
        @PartAnno(fatherCode = "md", attrs = { "5400", "1" })
        private PartVar md1;

        // 机械硬盘实例2
        @PartAnno(fatherCode = "md", attrs = { "7200", "2" })
        private PartVar md2;

        // 机械硬盘实例3
        @PartAnno(fatherCode = "md", attrs = { "9000", "3" })
        private PartVar md3;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar driveSumCapacity;// 输入参数

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar driveSumQuantity; // 输入参数

        // proRule1:固态硬盘必须配置同一种，并且最多配置2块
        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
        private void rule1() {
            // proRule1-natuarl: 固态硬盘必须配置同一种，并且最多配置2块
            // proRule1-dsl: 拆分为proRule11和proRule11两条约束（和isSelected(S)、qty(Q)相关）
            // proRule11-cRule: sd1.S + sd2.S <=1
            AlgCPLinearExpr sumSelected = sum4Selected("fatherCode=sd");
            model.addLessOrEqual(sumSelected, 1);

            // proRule12-cRule: sd1.Q + sd2.Q <= 2
            AlgCPLinearExpr sumQty = sum4Quantity("fatherCode=sd");
            model.addLessOrEqual(sumQty, 2);
        }

        // // proRule2:固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode =
        // "固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量", attrCode = "capacityWeight", strategy =
        // PriorityStrategy.MAX, type = PriorityType.SELECT)
        // private void rule2() {
        // // proRule2-natuarl: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // // proRule2-dsl: 选择的部件capacityWeight总和越大越好( 和qty(Q) * capacityWeight 相关)
        // // proRule2-expr: sd1.S*110 +sd2.S*120 + md1.S*13
        // // proRule2-step1: maximum(expr) ->
        // // proRule2-step2: expr >= 200*(1-30%) = 84
        // } // 优先使用固态硬盘：如果固态硬盘容量已足够，限制机械硬盘使用 TODO，怎么表达

        // proRule2:固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量", strategy = PriorityStrategy.MIN)
        private void rule2() {
            // proRule2-natuarl: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
            // proRule2-dsl: 选择的部件capacityWeight总和越大越好( 和qty(Q) * capacityWeight 相关)
            // proRule2-expr: sd1.S*110 +sd2.S*120 + md1.S*13
            // proRule2-step1: maximum(expr) ->
            // proRule2-step2: expr >= 200*(1-30%) = 84
            applyPriorityRule(model);

        } // 优先使用固态硬盘：如果固态硬盘容量已足够，限制机械硬盘使用 TODO，怎么表达

        // 规则1: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        private void applyPriorityRule(AlgCPModel model) {
            List<PartVar> partVars = getPartVars("");

            PartAlgCPLinearExpr ssTotalCapacity = sum4Quantity("Capacity", "fatherCode=sd");
            PartAlgCPLinearExpr mechTotalCapacity = sum4Quantity("Capacity", "fatherCode=md");
            // 如果是容量需求
            // if ("Capacity".equals(req.getAttrCode())) {
            if (driveSumCapacity.getIsHasInputed()) {
                int requiredCapacity = driveSumCapacity.getInputValue();
                // 创建固态硬盘是否足够的布尔变量
                BoolVar ssSufficient = model.newBoolVar(
                        "ssSufficient");

                // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
                model.addGreaterOrEqual(ssTotalCapacity,
                        requiredCapacity).onlyEnforceIf(ssSufficient);
                model.addLessThan(ssTotalCapacity,
                        requiredCapacity).onlyEnforceIf(ssSufficient.not());

                // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
                List<PartVar> mechanicalParts = getPartVars("fatherCode=md");
                for (PartVar pv : mechanicalParts) {
                    model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficient);
                }

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");

                // 基础目标: 最大化SSD使用（负权重）--容量越大越好
                objectiveExpr.addExpr(ssTotalCapacity, -100);

                // HDD惩罚 = HDD容量 * 惩罚系数S
                objectiveExpr.addExpr(mechTotalCapacity, 1);

                // 3. 惩罚过度配置（重要！）
                PartAlgCPLinearExpr totalCapacityExpr = sum4Selected("Capacity", "").name("totalCapacityExpr");

                // 创建过度配置变量约束：excessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr tExpr = model.newPartLinearExpr("excessCapacityExpr");
                tExpr.addExpr(totalCapacityExpr, 1);
                tExpr.addConstant(-requiredCapacity);
                // 2. 过度配置惩罚
                objectiveExpr.addExpr(tExpr, 500); // 惩罚过度配置

                // 4. 惩罚使用多.个零件（鼓励简洁配置）
                // 总零件数量惩罚
                PartAlgCPLinearExpr totalPartsExpr = sum4Quantity("").name("totalPartsExpr");

                objectiveExpr.addExpr(totalPartsExpr, 500); // 零件数量惩罚
                model.setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("rule2", objectiveExpr);

            } else {// 给的数量qty总数

                PartAlgCPLinearExpr ssTotalQty = sum4Quantity("fatherCode=sd");
                PartAlgCPLinearExpr mechTotalQty = sum4Quantity("fatherCode=md");

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                int requiredQty = driveSumQuantity.getInputValue();
                // 创建固态硬盘是否足够的布尔变量
                BoolVar ssSufficientQty = (BoolVar) model.newBoolVar(
                        "ssSufficientQty");
                // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
                model.addGreaterOrEqual(ssTotalQty,
                        requiredQty).onlyEnforceIf(ssSufficientQty);
                model.addLessThan(ssTotalQty,
                        requiredQty).onlyEnforceIf(ssSufficientQty.not());
                // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
                List<PartVar> mechanicalParts = getPartVars("fatherCode=md");
                for (PartVar pv : mechanicalParts) {
                    model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficientQty);
                }

                // 创建目标函数
                PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFunQty");

                // 基础目标: 最大化SSD使用（负权重）--容量越大越好
                objectiveExpr.addExpr(ssTotalCapacity, -100);
                // objectiveExpr.addExpr(ssTotalQty, -100);
                // HDD惩罚 = HDD容量 * 惩罚系数S，容量越小越好
                objectiveExpr.addExpr(mechTotalCapacity, 1);
                // objectiveExpr.addExpr(mechTotalQty, 1);

                // 3. 惩罚过度配置（重要！）
                PartAlgCPLinearExpr totalQtyExpr = sum4Quantity("");

                // 创建过度配置变量 约束：excessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr excessQyExpr = model.newPartLinearExpr("excessQyExpr");
                excessQyExpr.addExpr(totalQtyExpr, 1);
                excessQyExpr.addConstant(-requiredQty);
                // 2. 过度配置惩罚
                objectiveExpr.addExpr(excessQyExpr, 500); // 惩罚过度配置

                model.setObjectExpr(objectiveExpr); // 分minimize/adddGreaterxx
                // model.minimize(objectiveExpr); // 设置目标函数为最小化（因为SSD有负权重）
                updatePriorityObjectFuntion("rule2", objectiveExpr);
            }
        }

    }
    // ---------------模型的定义end----------------------------------------

    public BaseOptiTest() {
        super(BaseOptiConstraint.class);
    }

    // 要求5400速率的硬盘2块
    @Test
    public void firstBase() {
        // 测试点：父层category，=表达式
        inferRecommend("drive", "drive:sum.Quantity ==2 where Speed=5400");
        // Print solutions for debugging
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1)");
        assertSoluContain(3, "md1(Q:2,H:0,S:1),sd1(0*)");

        // Req:
        // drive:sum.Quantity ==2 where Speed=5400
        // md1.Q* + sd1.Q* == 2

        // proRule1:固态硬盘必须配置同一种，并且最多配置2块
        // sd1.S <= 1
        // sd1.Q <= 2

        // proRule2-固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // objfun 1*(-100*3*sd1.Q_1 + 1*1*md1.Q_1 + 500*(1*(1*sd1.Q_1 + 1*md1.Q_1) - 2))
    }

    // 用例0： 测试点，解读1
    // 输入：
    // strReq = " Capacity >=6 where Speed = 5400"
    // 输出：
    // 解1： sd1.qty=2
    // 解2： sd1.qty=1 md1.qty=3 //增配低速率容量
    // ...
    @Test
    public void testCase0_CapacityGreaterEqual6Speed5400() {
        inferRecommend("drive", "drive:sum.Capacity >=6 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:3,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase1_CapacityGreaterEqual5Speed5400() {
        inferRecommend("drive", "drive:sum.Capacity >=5 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:2,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase2_CapacityGreaterEqual7Speed5400() {
        inferRecommend("drive", "drive:sum.Capacity >=7 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),sd1(Q:2,H:0,S:1)");
    }

    @Test
    public void testCase3_QtyGreaterEqual3Speed5400() {
        inferRecommend("drive", "drive:sum.Quantity >=3 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:2,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase5_CapacityGreaterEqual5NoSpeedFilter() {
        inferRecommend("drive", "drive:sum.Capacity >=5");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),md2(0*),md3(0*),sd1(Q:2,H:0,S:1),sd2(0*),sd3(0*)");
        // issue: sd1.Q=1,md2.Q=2 为什么没有出来
    }

    @Test
    public void testCase6_QuantityGreaterEqual2NoSpeedFilter() {
        inferRecommend("drive", "drive:sum.Quantity >=2");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(0*),md2(0*),md3(0*),sd1(0*),sd2(Q:2,H:0,S:1),sd3(0*)");
        assertSoluContain("md1(Q:1,H:0,S:1),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase7_QuantityGreaterEqual3NoSpeedFilter() {
        inferRecommend("drive", "drive:sum.Quantity >=3");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(0*),md2(Q:1,H:0,S:1),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(3, "md1(0*),md2(0*),md3(Q:1,H:0,S:1),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
    }

    // 要求5400速率的固态硬盘2块
    @Test
    public void testNoSpeedRequirement() {
        inferRecommend("drive", "drive:sum.Quantity ==2 where Speed=3000");
        // resultAssert().assertSolutionSizeEqual(0);
    }
}
