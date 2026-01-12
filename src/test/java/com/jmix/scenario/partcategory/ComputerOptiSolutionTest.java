package com.jmix.scenario.partcategory;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.DynamicAttributeType;
import com.jmix.executor.imodel.PriorityStrategy;
import com.jmix.executor.imodel.PriorityType;
import com.jmix.executor.imodel.anno.CodeRuleAnno;
import com.jmix.executor.imodel.anno.DAttrAnno1;
import com.jmix.executor.imodel.anno.DAttrAnno2;
import com.jmix.executor.imodel.anno.DAttrAnno3;
import com.jmix.executor.imodel.anno.DAttrAnno4;
import com.jmix.executor.imodel.anno.DAttrInherit;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.PartAnno;
import com.jmix.executor.imodel.anno.PriorityRuleAnno;

import com.google.ortools.sat.IntVar;
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
                "Speed_5400:5400:转",
                "Speed_9000:9000:转",
                "Speed_7200a5400:7200/5400:转",
                "Speed_7200:7200:转" }, instType = 0)
        @DAttrAnno3(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_1T:1:T",
                "Capacity_2T:2:T",
                "Capacity_4T:4:T" }, instType = 0)
        @DAttrAnno4(code = "capacityWeight", optionExtSchema = "IntegerUnit", options = { "CW_11:11", "CW_12:12",
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

        // rule1://固态硬盘必须配置同一种，并且最多配置2块
        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "sd1.isSelected + sd2.isSelected + sd3.isSelected <= 1\n "
                +
                "sd1.qty + sd2.qty + sd3.qty <= 2\n if(sd1.isSelected ==1) then sd1.qty >= 0")
        private void rule1() {
            log.info("xxx");
            // Constraint: At most one type of solid drive can be selected
            // sd1.isSelected + sd2.isSelected + sd3.isSelected <= 1
            // LinearExpr sumSelected = LinearExpr
            // .sum(new BoolVar[] { sd1.isSelected, sd2.isSelected, sd3.isSelected });
            LinearExpr sumSelected = sum4Selected();
            model.addLessOrEqual(sumSelected, 1);

            // Constraint: Total quantity of solid drives <= 2
            // sd1.qty + sd2.qty + sd3.qty <= 2
            // LinearExpr sumQty = LinearExpr.sum(new IntVar[] { sd1.qty, sd2.qty, sd3.qty
            // });
            LinearExpr sumQty = sum4Quantity();
            model.addLessOrEqual(sumQty, 2);
        }

        // rule2://固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量", attrCode = "capacityWeight", strategy = PriorityStrategy.MAX, type = PriorityType.SELECT)
        private void rule2() {
            // 优先级约束由系统自动处理，这里可以留空或添加其他逻辑 rawCode="选择的部件capacityWeight总和越大越好"
        }

        // private void rule2() {
        // LinearExpr sumCapacityWeight = sum4Selected("capacityWeight");
        // model.maximize(sumCapacityWeight);
        // }

        private void rule2_backup() {
            final int SOLID1_WEIGHT = 100;
            final int SOLID2_WEIGHT = 200;
            final int SOLID3_WEIGHT = 300;
            final int MECH1_WEIGHT = 30;
            final int MECH2_WEIGHT = 20;
            final int MECH3_WEIGHT = 10;

            // Create contribution variables for solid drives
            IntVar solid1Contrib = model.newIntVar(0, SOLID1_WEIGHT, "solid1Contrib");
            IntVar solid2Contrib = model.newIntVar(0, SOLID2_WEIGHT, "solid2Contrib");
            IntVar solid3Contrib = model.newIntVar(0, SOLID3_WEIGHT, "solid3Contrib");

            // Link contribution to isSelected: contrib = weight * isSelected
            // Implement as: contrib == weight * isSelected
            // Since isSelected is BoolVar, we can add:
            // isSelected => contrib == weight, and not isSelected => contrib == 0
            model.addEquality(solid1Contrib, SOLID1_WEIGHT).onlyEnforceIf(sd1.isSelected);
            model.addEquality(solid1Contrib, 0).onlyEnforceIf(sd1.isSelected.not());

            model.addEquality(solid2Contrib, SOLID2_WEIGHT).onlyEnforceIf(sd2.isSelected);
            model.addEquality(solid2Contrib, 0).onlyEnforceIf(sd2.isSelected.not());

            model.addEquality(solid3Contrib, SOLID3_WEIGHT).onlyEnforceIf(sd3.isSelected);
            model.addEquality(solid3Contrib, 0).onlyEnforceIf(sd3.isSelected.not());

            // Create contribution variables for mechanical drives
            IntVar mech1Contrib = model.newIntVar(0, MECH1_WEIGHT, "mech1Contrib");
            IntVar mech2Contrib = model.newIntVar(0, MECH2_WEIGHT, "mech2Contrib");
            IntVar mech3Contrib = model.newIntVar(0, MECH3_WEIGHT, "mech3Contrib");

            model.addEquality(mech1Contrib, MECH1_WEIGHT).onlyEnforceIf(md1.isSelected);
            model.addEquality(mech1Contrib, 0).onlyEnforceIf(md1.isSelected.not());

            model.addEquality(mech2Contrib, MECH2_WEIGHT).onlyEnforceIf(md2.isSelected);
            model.addEquality(mech2Contrib, 0).onlyEnforceIf(md2.isSelected.not());

            model.addEquality(mech3Contrib, MECH3_WEIGHT).onlyEnforceIf(md3.isSelected);
            model.addEquality(mech3Contrib, 0).onlyEnforceIf(md3.isSelected.not());

            // Total capacity weight
            IntVar totalCapacityWeight = model.newIntVar(0,
                    SOLID1_WEIGHT + SOLID2_WEIGHT + SOLID3_WEIGHT + MECH1_WEIGHT + MECH2_WEIGHT + MECH3_WEIGHT,
                    "totalCapacityWeight");
            model.addEquality(totalCapacityWeight, LinearExpr.sum(new IntVar[] { solid1Contrib, solid2Contrib,
                    solid3Contrib, mech1Contrib, mech2Contrib, mech3Contrib }));

            // Minimize total capacity weight (this is an objective)
            model.maximize(totalCapacityWeight);
        }
    }
    // ---------------模型的定义end----------------------------------------

    public ComputerOptiSolutionTest() {
        super(ComputerOptiSolutionConstraint.class);
    }

    // 要求5400速率的硬盘2块
    @Test
    public void testUserSpecialRequirementConstantAttr() {
        inferRecommend("drive", "drive:sum.Quantity ==2 where Speed=5400");
        // Print solutions for debugging
        printSimpleSolutions();
        // resultAssert()
        // .assertSuccess()
        // .assertSolutionSizeEqual(2); // Expect at least one solution, but we don't
        // know exact number. We'll
        // // check later.

    }

    // 要求5400速率的固态硬盘2块
    @Test
    public void testUserSpecialRequirement2() {
        inferRecommend("drive", "sd:sum.Quantity ==2 where Speed like %5400%");
        // Print solutions for debugging
        printSimpleSolutions();
        // resultAssert()
        // .assertSolutionSizeEqual(1); // Expect at least one solution, but we don't
        // know exact number. We'll
        // solutions(1).assertPara("sd1");
    }

    // 要求5400速率的固态硬盘2块
    @Test
    public void testUserSpecialRequirement3() {

        // drive:sum.Capacity >=5 where Speed like %5400%"-> sd1.Q*6 + sd2.Q*4 + md1.Q*2
        // >= 5
        // rule11: sd1.isSelected + sd2.isSelected + sd3.isSelected <= 1
        // rule12: sd1.qty + sd2.qty + sd3.qty <= 2
        // rule2: expr= sd1.S*110 +sd2.S*120 + md1.S*13
        // rule2-step1: maximum(expr) -> md1(0*)sd1(0*),sd2(Q:2,S:1)=120
        // rule2-step2: expr >= 200*(1-30%) = 84
        // S_1: md1(0*),sd1(Q:1,H:0,S:1),sd2(0*)
        // S_2: md1(0*),sd1(Q:2,H:0,S:1),sd2(0*)
        // S_3: md1(0*),sd1(0*),sd2(Q:2,H:0,S:1)

        inferRecommend("drive", "drive:sum.Capacity >=5 where Speed like %5400%");
        // Print solutions for debugging
        printSimpleSolutions();
        resultAssert().assertSolutionSizeEqual(3);
        solutions(0).assertPart("sd2").quantityEqual(2);
        solutions(0).assertPA("CA").valueEqual(120.0);
        solutions(0).assertPOEqual(120.0);
        solutions(0).assertPSEqual(1);

        soluContain("md1(0*),sd1(0*),sd2(Q:2,H:0,S:1),PAs(CA:120.0) PO:120.0 PS:1");
        soluContain("md1(0*),sd1(Q:1,H:0,S:1),sd2(0*)");
        soluContain("md1(0*),sd1(Q:2,H:0,S:1),sd2(0*),PAs(CA:110.0) PO:110.0 PS:3");

        // PO:120.0 PS:1")
        // assertSolutionStrContain("md1:0,sd1:1,sd2:0,PAs::CA:110.0, PO:120.0")
        // resultAssert()
        // .assertSolutionSizeEqual(1); // Expect at least one solution, but we don't
        // know exact number. We'll
        // solutions(1).assertPara("sd1");
    }
}
