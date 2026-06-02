package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * RFC-0010 cross PartCategory total constraint tests.
 */
public class CrossPartCategoryTotalConstraintTest extends ModuleScenarioTestBase {

    private static final String TOTAL_CODE = "uplink_10g_capacity_total";

    public CrossPartCategoryTotalConstraintTest() {
        super(CrossTotalConstraint.class);
    }

    @ModuleAnno(id = 9010L)
    public static class CrossTotalConstraint extends ModuleAlgBase {

        @PartAnno(code = "disk", supportMultiInst = true)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_40:40", "Cap_60:60", "Cap_100:100"})
        private PartCategoryVar disk;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "40"}, price = 40)
        private PartVar disk10g40;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "60"}, price = 60)
        private PartVar disk10g60;

        @PartAnno(fatherCode = "disk", attrs = {"100G", "100"}, price = 100)
        private PartVar disk100g100;

        @PartAnno(code = "mainBoard")
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_80:80", "Cap_120:120"})
        private PartCategoryVar mainBoard;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "80"}, price = 80)
        private PartVar board10g80;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "120"}, price = 120)
        private PartVar board10g120;

        @PartAnno(fatherCode = "mainBoard", attrs = {"100G", "120"}, price = 120)
        private PartVar board100g120;

        @PartAnno(code = "expansionBoard", required = false)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_60:60"})
        private PartCategoryVar expansionBoard;

        @PartAnno(fatherCode = "expansionBoard", attrs = {"10G", "60"}, price = 60)
        private PartVar exp10g60;
    }

    @Test
    public void testCrossTotal_BoardOnlySatisfiesAggregate() {
        inferRecommendModuleWithReqs(
                singleReqs(
                        "disk:Sum_Quantity ==0 where PortRate=10G",
                        "mainBoard:Sum_Quantity ==1 where Capacity=120"),
                List.of(crossTotal(">=100", "PortRate=10G")));

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("board10g120"));
    }

    @Test
    public void testCrossTotal_DiskAndBoardTogether() {
        inferRecommendModuleWithReqs(
                singleReqs(
                        "disk:Sum_Quantity ==1 where Capacity=40",
                        "mainBoard:Sum_Quantity ==1 where Capacity=80"),
                List.of(crossTotal(">=100", "PortRate=10G")));

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("disk10g40", "board10g80"));
    }

    @Test
    public void testCrossTotalWhere_DoesNotOverrideSingleCategoryCandidates() {
        inferRecommendModuleWithReqs(
                singleReqs(
                        "disk:Sum_Quantity ==1 where PortRate=100G",
                        "mainBoard:Sum_Quantity ==0 where PortRate=10G"),
                List.of(crossTotal("<=0", "PortRate=10G")));

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("disk100g100"));
    }

    @Test
    public void testCrossTotalWhere_RunsOnFilteredCandidatesOnly() {
        inferRecommendModuleWithReqs(
                singleReqs(
                        "disk:Sum_Quantity ==1 where PortRate=10G",
                        "mainBoard:Sum_Quantity ==0 where PortRate=10G"),
                List.of(crossTotal(">=100", "Capacity>=60")));

        resultAssert().assertNoSolution();
    }

    @Test
    public void testCrossTotal_MultiInstAggregatesAllInstances() {
        inferRecommendModuleWithReqs(
                singleReqs(
                        "disk:Sum_Quantity ==1 where Capacity=40",
                        "disk:Sum_Quantity ==1 where Capacity=60",
                        "mainBoard:Sum_Quantity ==0 where PortRate=10G"),
                List.of(crossTotal(">=100", "PortRate=10G")));

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("disk10g40", "disk10g60"));
    }

    @Test
    public void testCrossTotal_InvalidCategoryFails() {
        inferRecommendModuleWithReqs(totalReq(TOTAL_CODE,
                List.of("disk", "missingBoard"),
                "Sum_Capacity >=100",
                null));

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("missingBoard");
    }

    @Test
    public void testCrossTotal_NonNumericAttrFails() {
        inferRecommendModuleWithReqs(totalReq(TOTAL_CODE,
                List.of("disk", "mainBoard"),
                "Sum_PortRate >=100",
                null));

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("PortRate", "not numeric");
    }

    @Test
    public void testCrossTotal_UnknownWhereAttrFails() {
        inferRecommendModuleWithReqs(totalReq(TOTAL_CODE,
                List.of("disk", "mainBoard"),
                "Sum_Capacity >=100",
                "UnknownAttr=10G"));

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("UnknownAttr");
    }

    @Test
    public void testCrossTotal_OptionalCategoryCanSatisfyPresentBranch() {
        inferRecommendModuleWithReqs(
                singleReqs("mainBoard:Sum_Quantity ==1 where Capacity=80"),
                List.of(totalReq(TOTAL_CODE,
                        List.of("mainBoard", "expansionBoard"),
                        "Sum_Capacity >=140",
                        "PortRate=10G")));

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("board10g80", "exp10g60"));
    }

    private List<PartConstraintReq> singleReqs(String... reqs) {
        return toPartConstraintReqs("", reqs);
    }

    private CrossCategoryPartCategoryConstraintReq crossTotal(String comparatorAndValue, String whereCondition) {
        return totalReq(TOTAL_CODE,
                List.of("disk", "mainBoard"),
                "Sum_Capacity " + comparatorAndValue,
                whereCondition);
    }

    private boolean hasSolutionWithSelectedParts(String... partCodes) {
        if (getSolutions() == null) {
            return false;
        }
        for (ModuleInst solution : getSolutions()) {
            boolean matched = true;
            for (String partCode : partCodes) {
                if (!hasSelectedPart(solution, partCode)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSelectedPart(ModuleInst solution, String partCode) {
        for (PartInst part : solution.getAllParts()) {
            String actualCode = part.getCode();
            if ((partCode.equals(actualCode) || actualCode.endsWith("_" + partCode))
                    && part.isSelected()
                    && part.getQuantity() != null && part.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }
}
