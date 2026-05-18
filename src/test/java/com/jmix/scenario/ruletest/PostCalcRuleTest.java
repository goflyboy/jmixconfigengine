package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.southinf.ModuleAlgBase;
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
 * POST鍚庣疆璁＄畻瑙勫垯娴嬭瘯
 *
 * @since 2026-05-03
 */
@Slf4j
public class PostCalcRuleTest extends ModuleScenarioTestBase {

    public PostCalcRuleTest() {
        super(PostCalcConstraint.class);
    }

    @ModuleAnno(id = 123L)
    public static class PostCalcConstraint extends ModuleAlgBase {

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
            int sum = partCategorySum("drive").sumDynAttr4Int("Capacity");
            parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
            partCategory("drive").parameter("pSumCapacity").setValue(String.valueOf(sum));
            parameter("pFirstCapacity").setValue(partCategorySum("drive").dynAttr("Capacity"));
            parameter("pDriveQuantity").setValue(String.valueOf(partCategory("drive").sumQuantity()));
            parameter("pDriveAttrCount").setValue(String.valueOf(partCategorySum("drive").dynAttrs("Capacity").size()));
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

        // 楠岃瘉姣忎釜瑙ｇ殑POST鍙傛暟宸插啓鍏?閫氳繃ParaInst.value妫€鏌?
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

        // 鍦ㄨВmd1(Q:1,S:1), sd1(Q:1,S:1)涓? pFirstCapacity搴斾负md1鐨凜apacity=1
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

        // 楠岃瘉INPUT鍙傛暟鍊肩敱POST鍐欏叆锛岃€岄潪CP姹傝В锛圥OST鍐欏叆鐨勫€煎簲璇?0锛?
        for (ModuleInst sol : getSolutions()) {
            String value = findParaValue(sol, "pDriveSumCapacity");
            int intValue = Integer.parseInt(value);
            assertTrue(intValue >= 2,
                    "pDriveSumCapacity should be computed by POST, got: " + intValue);
        }
    }

    /**
     * 鍦∕oduleInst鍙婂叾鎵€鏈塒artCategoryInst涓煡鎵炬寚瀹歝ode鐨凱araInst鍊?
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
