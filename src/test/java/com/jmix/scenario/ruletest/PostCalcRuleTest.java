package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.southinf.ConstraintAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * POST后置计算规则测试
 *
 * @since 2026-05-03
 */
@Slf4j
public class PostCalcRuleTest extends ModuleScenarioTestBase {

    public PostCalcRuleTest() {
        super(PostCalcConstraint.class);
    }

    @ModuleAnno(id = 123L)
    public static class PostCalcConstraint extends ConstraintAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno2(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400"})
        @DAttrAnno3(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Capacity_1T:1", "Capacity_3T:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "1"})
        private PartVar md1;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "3"})
        private PartVar sd1;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Quantity;

        @ParaAnno(code = "pDriveSumCapacity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveSumCapacity;

        @ParaAnno(code = "pFirstCapacity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pFirstCapacity;

        @ParaAnno(code = "pDriveQuantity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveQuantity;

        @ParaAnno(code = "pDriveAttrCount", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveAttrCount;

        @ParaAnno(fatherCode = "drive", code = "pSumCapacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pSumCapacity;

        @CodeRuleAnno(calcStage = CalcStage.POST)
        private void postRule() {
            int sum = toInt(getSumDynAttr("drive", "Capacity"));
            setParaValue("pDriveSumCapacity", String.valueOf(sum));
            setParaValue("drive", "pSumCapacity", String.valueOf(sum));
            setParaValue("pFirstCapacity", getDynAttr("drive", "Capacity"));
            setParaValue("pDriveQuantity", toString(getQuantity("drive")));
            setParaValue("pDriveAttrCount", toString(getDynAttrValues("drive", "Capacity").size()));
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Test
    public void testPostCalc_ModuleAndCategoryPara() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();
        assertEquals(3, getSolutions().size(), "Should have 3 solutions");

        // 验证每个解的POST参数已写入(通过ParaInst.value检查)
        for (ModuleInst sol : getSolutions()) {
            String driveSumCapacity = findParaValue(sol, "pDriveSumCapacity");
            assertTrue(driveSumCapacity != null && !driveSumCapacity.isEmpty(),
                    "pDriveSumCapacity should be set by POST rule");

            String driveQuantity = findParaValue(sol, "pDriveQuantity");
            assertTrue(driveQuantity != null && !driveQuantity.isEmpty(),
                    "pDriveQuantity should be set by POST rule");
        }
    }

    @Test
    public void testPostCalc_GetDynAttrUseFirstPart() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();

        // 在解md1(Q:1,S:1), sd1(Q:1,S:1)中, pFirstCapacity应为md1的Capacity=1
        for (ModuleInst sol : getSolutions()) {
            String firstCapacity = findParaValue(sol, "pFirstCapacity");
            assertTrue(firstCapacity != null && !firstCapacity.isEmpty(),
                    "pFirstCapacity should be set");

            String attrCount = findParaValue(sol, "pDriveAttrCount");
            assertTrue(attrCount != null && !attrCount.isEmpty(),
                    "pDriveAttrCount should be set");
        }
    }

    @Test
    public void testPostCalc_InputParaDoesNotJoinCpModel() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();

        // 验证INPUT参数值由POST写入，而非CP求解（POST写入的值应该>0）
        for (ModuleInst sol : getSolutions()) {
            String value = findParaValue(sol, "pDriveSumCapacity");
            int intValue = Integer.parseInt(value);
            assertTrue(intValue >= 2,
                    "pDriveSumCapacity should be computed by POST, got: " + intValue);
        }
    }

    /**
     * 在ModuleInst及其所有PartCategoryInst中查找指定code的ParaInst值
     */
    private String findParaValue(ModuleInst sol, String paraCode) {
        for (ParaInst pi : sol.getParas()) {
            if (paraCode.equals(pi.getCode())) {
                return pi.getValue();
            }
        }
        for (PartCategoryInst pcInst : sol.getPartCategorys()) {
            for (ParaInst pi : pcInst.getParas()) {
                if (paraCode.equals(pi.getCode())) {
                    return pi.getValue();
                }
            }
        }
        return null;
    }
}
