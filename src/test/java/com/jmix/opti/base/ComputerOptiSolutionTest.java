package com.jmix.opti.base;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.PriorityType;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrAnno4;
import com.jmix.tool.bbuilder.anno.DAttrInherit;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import com.google.ortools.sat.LinearExpr;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

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
        private PartCategoryVar driveVar;

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

        // proRule1:固态硬盘必须配置同一种，并且最多配置2块
        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
        private void rule1() {
            // proRule1-natuarl: 固态硬盘必须配置同一种，并且最多配置2块
            // proRule1-dsl: 拆分为proRule11和proRule11两条约束（和isSelected(S)、qty(Q)相关）
            // proRule11-cRule: sd1.S + sd2.S <=1
            LinearExpr sumSelected = sum4Selected("fatherCode=sd");
            model.addLessOrEqual(sumSelected, 1);

            // proRule12-cRule: sd1.Q + sd2.Q <= 2
            LinearExpr sumQty = sum4Quantity("fatherCode=sd");
            model.addLessOrEqual(sumQty, 2);
        }

        // proRule2:固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量", attrCode = "capacityWeight", strategy = PriorityStrategy.MAX, type = PriorityType.SELECT)
        private void rule2() {
            // proRule2-natuarl: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
            // proRule2-dsl: 选择的部件capacityWeight总和越大越好( 和qty(Q) * capacityWeight 相关)
            // proRule2-expr: sd1.S*110 +sd2.S*120 + md1.S*13
            // proRule2-step1: maximum(expr) ->
            // proRule2-step2: expr >= 200*(1-30%) = 84
        } // 优先使用固态硬盘：如果固态硬盘容量已足够，限制机械硬盘使用 TODO，怎么表达
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
