package com.jmix.opti.base;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.PriorityType;
import com.jmix.executor.bmodel.para.AssignType;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrAnno4;
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
public class ComputerOptiSolutionTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ComputerOptiSolutionConstraint extends ConstraintAlgImplTestBase {
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
                "Capacity_4T:4:T" }, instType = 0)
        @DAttrAnno4(code = "capacityWeight", optionExtSchema = "IntegerUnit", options = { "CW_10:10", "CW_11:11",
                "CW_12:12",
                "CW_13:13",
                "CW_110:110", "CW_120:120", "CW_130:130" }, instType = 0) // 不同点：带点实现, 权总考虑的30%，建议是15%左右
        private PartCategoryVar drive;

        // 固态硬盘部件分类，继承driveVar并重写属性
        @PartAnno(code = "sd", fatherCode = "drive")
        @DAttrAnno1(code = "BrandWidth", optionExtSchema = "IntegerUnit", options = { "BW_8GB:8:GB/S",
                "BW_16GB:16:GB/S" })
        @DAttrInherit(fatherCode = "driveVar", overrideAttrs = { "Speed:instType=1", "Capacity:instType=1" })
        private PartCategoryVar sd;

        // 机械硬盘部件分类，继承driveVar
        @PartAnno(code = "md", fatherCode = "drive")
        @DAttrInherit(fatherCode = "driveVar")
        private PartCategoryVar md;

        // 固态硬盘实例1
        @PartAnno(fatherCode = "sd", attrs = { "8", "110" }, attrsInst1 = {
                "5400",
                "2" }, attrsInst2 = { "7200/5400", "4" })
        private PartVar sd1;

        // 固态硬盘实例2
        @PartAnno(fatherCode = "sd", attrs = { "8", "120" }, attrsInst1 = {
                "7200/5400", "4" })
        private PartVar sd2;

        // 固态硬盘实例3
        @PartAnno(fatherCode = "sd", attrs = { "8", "130" }, attrsInst1 = {
                "9000",
                "4" })
        private PartVar sd3;

        // 机械硬盘实例1
        @PartAnno(fatherCode = "md", attrs = { "5400", "2", "13" })
        private PartVar md1;

        // 机械硬盘实例2
        @PartAnno(fatherCode = "md", attrs = { "7200", "2", "12" })
        private PartVar md2;

        // 机械硬盘实例3
        @PartAnno(fatherCode = "md", attrs = { "9000", "2", "11" })
        private PartVar md3;

        // 机械硬盘实例3
        @PartAnno(fatherCode = "md", attrs = { "9900", "4", "10" })
        private PartVar md4;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar sumCapacity;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar sumQuantity;

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
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量", attrCode = "capacityWeight", strategy = PriorityStrategy.MIN, type = PriorityType.SUMARIZE)
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

            // // 分离固态硬盘和机械硬盘
            // List<PartVar> solidStateParts = partVars.stream()
            // .filter(PartVar::isSolidState)
            // .collect(Collectors.toList());

            // List<PartVar> mechanicalParts = partVars.stream()
            // .filter(pv -> !pv.isSolidState())
            // .collect(Collectors.toList());

            // if (solidStateParts.isEmpty() || mechanicalParts.isEmpty()) {
            // return;
            // }
            // // 创建固态硬盘总容量表达式
            // AlgCPLinearExpr ssTotalCapacity = model.newLinearExpr("SS_Total_Capacity");
            // for (PartVar pv : solidStateParts) {
            // ssTotalCapacity.addTerm(pv.qty, pv.getCapacity());
            // }

            // 创建机械硬盘总容量表达式
            // AlgCPLinearExpr mechTotalCapacity =
            // model.newLinearExpr("Mech_Total_Capacity");
            // for (PartVar pv : mechanicalParts) {
            // mechTotalCapacity.addTerm(pv.qty, pv.getCapacity());
            // }
            PartAlgCPLinearExpr ssTotalCapacity = sum4Selected("Capacity", "fatherCode=sd");
            PartAlgCPLinearExpr mechTotalCapacity = sum4Selected("Capacity", "fatherCode=md");
            // 如果是容量需求
            // if ("Capacity".equals(req.getAttrCode())) {
            if (sumCapacity.getIsHasInputed()) {

                int requiredCapacity = sumCapacity.getInputValue();

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
                // 创建总容量变量
                // AlgCPLinearExpr totalCapacityExpr = model.newLinearExpr("Total_Capacity");
                // for (PartVar pv : partVars) {
                // totalCapacityExpr.addTerm(pv.qty, pv.getCapacity());
                // }
                PartAlgCPLinearExpr totalCapacityExpr = sum4Selected("Capacity", "");

                // 创建过度配置变量
                // 约束：excessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr tExpr = model.newPartLinearExpr("excessCapacityExpr");
                tExpr.addExpr(totalCapacityExpr, 1);
                tExpr.addConstant(-requiredCapacity);
                // 2. 过度配置惩罚
                objectiveExpr.addExpr(tExpr, 500); // 惩罚过度配置

                // 4. 惩罚使用多个零件（鼓励简洁配置）
                // 总零件数量惩罚
                PartAlgCPLinearExpr totalPartsExpr = model.newPartLinearExpr("Total_Parts");
                for (PartVar pv : partVars) {
                    totalPartsExpr.addTerm(pv.qty, 1);
                }

                objectiveExpr.addExpr(totalPartsExpr, 500); // 零件数量惩罚

                model.addLessOrEqual(objectiveExpr, 2000);
                model.setObjectExpr(objectiveExpr);

                // model.minimize(objectiveExpr); // 设置目标函数为最小化（因为SSD有负权重）

            } else {// 给的数量qty总数
                // 创建固态硬盘总容量表达式
                // AlgCPLinearExpr ssTotalQty = model.newLinearExpr("ssTotalQty");
                // for (PartVar pv : solidStateParts) {
                // ssTotalQty.addTerm(pv.qty, 1);
                // }
                // // 创建机械硬盘总容量表达式
                // AlgCPLinearExpr mechTotalQty = model.newLinearExpr("mechTotalQty");
                // for (PartVar pv : mechanicalParts) {
                // mechTotalQty.addTerm(pv.qty, 1);
                // }
                PartAlgCPLinearExpr ssTotalQty = sum4Quantity("fatherCode=sd");
                PartAlgCPLinearExpr mechTotalQty = sum4Quantity("fatherCode=md");

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                int requiredQty = sumQuantity.getInputValue();
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
                // 创建总容量变量
                // PartAlgCPLinearExpr totalQtyExpr = model.newPartLinearExpr("totalQtyExpr");
                // for (PartVar pv : partVars) {
                // totalQtyExpr.addTerm(pv, pv.qty, 1);
                // }
                PartAlgCPLinearExpr totalQtyExpr = sum4Quantity("");

                // 创建过度配置变量
                // 约束：excessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr excessQyExpr = model.newPartLinearExpr("excessQyExpr");
                excessQyExpr.addExpr(totalQtyExpr, 1);
                excessQyExpr.addConstant(-requiredQty);
                // 2. 过度配置惩罚
                objectiveExpr.addExpr(excessQyExpr, 500); // 惩罚过度配置

                model.addLessOrEqual(objectiveExpr, 1000);
                model.setObjectExpr(objectiveExpr);
                // model.minimize(objectiveExpr); // 设置目标函数为最小化（因为SSD有负权重）

            }
        }

    }
    // ---------------模型的定义end----------------------------------------

    public ComputerOptiSolutionTest() {
        super(ComputerOptiSolutionConstraint.class);
    }

    // 要求5400速率的硬盘2块
    @Test
    public void correct_test_father_category_req() {
        // 测试点：父层category，=表达式
        inferRecommend("drive", "drive:sum.Quantity ==2 where Speed=5400");
        // Print solutions for debugging
        printSimpleSolutions();

        // resultAssert().assertSolutionSizeEqual(2);
        // soluContain("md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1),PAs(CA:123.0) PO:123.0 PS:1");
        // soluContain("md1(0*),sd1(Q:2,H:0,S:1),PAs(CA:110.0) PO:110.0 PS:2");

        // Req:
        // drive:sum.Quantity ==2 where Speed=5400
        // md1.Q* + sd1.Q* == 2

        // proRule1:固态硬盘必须配置同一种，并且最多配置2块
        // sd1.S <= 1
        // sd1.Q <= 2

        // proRule2-固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // PA_capacityWeight: md1.S_1*13 + sd1.S_1*110 = 123.0

        // 最优解：
    }

    // 要求5400速率的固态硬盘2块,
    @Test
    public void correct_test_child_category_req_priority_rule() { // rule2不正确
        // 测试点：子category
        inferRecommend("drive", "sd:sum.Quantity ==2 where Speed like %5400%");
        // Print solutions for debugging
        printSimpleSolutions();

        // resultAssert().assertSolutionSizeEqual(2);
        // soluContain("md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1),PAs(CA:123.0) PO:123.0 PS:1");
        // soluContain("md1(0*),sd1(Q:2,H:0,S:1),PAs(CA:110.0) PO:110.0 PS:2");

        // Req:
        // sd:sum.Quantity ==2 where Speed like %5400%
        // sd1.Q + sd2.Q == 2

        // proRule1:固态硬盘必须配置同一种，并且最多配置2块
        // sd1.S + sd2.S <= 1
        // sd1.Q + sd2.Q <= 2

        // proRule2-固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量 --关键1：
        // PA_capacityWeight: md1.S_1*13 + md2.S_1*12 + md3.S_1*11 + md4.S_1*10 +
        // sd1.S_0*110 + sd2.S_1*120 = 166.0
        // 这个感觉，尽量要配置机械硬盘，和实际的语义不同

    }

    // 要求5400速率的硬盘容量>=5T
    @Test
    public void corrent_test_father_category() {

        // 测试点， “固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量”没有满足，由于的0.3的权重，最优解没有出现
        // 如果有多个 优先类规则，就不好办了？
        // reqRuleA-natuarl: 要求5400速率的硬盘容量>=5T
        // reqRuleA-dsl: drive:sum.Capacity >=5 where Speed like %5400%
        // reqRuleA1-fRule: select * drive where Speed like %5400% -> md1,sd1,sd2
        // reqRuleA2-cRule: md1.Q*2 + sd1.Q*6 + sd2.Q*4 >= 5

        // proRule1-natuarl: 固态硬盘必须配置同一种，并且最多配置2块
        // proRule11-cRule: sd1.S + sd2.S <=1
        // proRule12-cRule: sd1.Q + sd2.Q <= 2

        // proRule2-natuarl: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // proRule2-expr: md1.S*13+sd1.S*110+sd2.S*120
        // proRule2-step1: maximum(expr)->md1.S_1*13+sd1.S_0*110+sd2.S_1*120=133.0
        // proRule2-step2: expr >= 200*(1-30%) = 93

        // 可能的解
        // S_1: md1(0*),sd1(Q:1,H:0,S:1),sd2(0*)
        // S_2: md1(0*),sd1(Q:2,H:0,S:1),sd2(0*)
        // S_3: md1(0*),sd1(0*),sd2(Q:2,H:0,S:1)

        inferRecommend("drive", "drive:sum.Capacity >=5 where Speed like %5400%");
        // Print solutions for debugging
        printSimpleSolutions();
        // resultAssert().assertSolutionSizeEqual(10);
        // solutions(0).assertPart("sd2").quantityEqual(2);
        // solutions(0).assertPA("CA").valueEqual(120.0);
        // solutions(0).assertPOEqual(120.0);
        // solutions(0).assertPSEqual(1);

        // soluContain("md1(0*),sd1(0*),sd2(Q:2,H:0,S:1),PAs(CA:120.0) PO:120.0 PS:1");
        // soluContain("md1(0*),sd1(Q:1,H:0,S:1),sd2(0*)");
        // soluContain("md1(0*),sd1(Q:2,H:0,S:1),sd2(0*),PAs(CA:110.0) PO:110.0 PS:3");

    }

    // 要求5400速率的固态硬盘2块
    @Test
    public void testNoSpeedRequirement() {
        inferRecommend("drive", "drive:sum.Quantity ==2 where Speed=3000");
        // resultAssert().assertSolutionSizeEqual(0);
    }
}
