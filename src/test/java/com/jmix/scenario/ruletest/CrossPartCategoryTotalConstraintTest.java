package com.jmix.scenario.ruletest;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

/**
 * RFC-0010 cross PartCategory total constraint tests.
 */
public class CrossPartCategoryTotalConstraintTest extends ModuleScenarioTestBase {

    public CrossPartCategoryTotalConstraintTest() {
        super(CrossTotalConstraint.class);
    }

    @ModuleAnno(id = 9010L)
    public static class CrossTotalConstraint extends ModuleAlgBase {

        @PartAnno(supportMultiInst = true)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_40:40", "Cap_60:60", "Cap_100:100"})
        private PartCategoryVar disk;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "40"})
        private PartVar d1;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "60"})
        private PartVar d2;

        @PartAnno(fatherCode = "disk", attrs = {"100G", "100"})
        private PartVar d3;

        @PartAnno
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_80:80", "Cap_120:120"})
        private PartCategoryVar mainBoard;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "80"})
        private PartVar b1;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "120"})
        private PartVar b2;

        @PartAnno(fatherCode = "mainBoard", attrs = {"100G", "120"})
        private PartVar b3;

        @PartAnno(required = false)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_60:60"})
        private PartCategoryVar expansionBoard;

        @PartAnno(fatherCode = "expansionBoard", attrs = {"10G", "60"})
        private PartVar e1;

    }

    @Test
    public void testCrossTotal_BoardOnlySatisfiesAggregate() {
        inferRecommendModule(
                "disk:Sum_Quantity ==0 where PortRate=10G",
                "mainBoard:Sum_Quantity ==1 where Capacity=120",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");
        printSimpleSolutions();
        resultAssert().assertSuccess();
        assertSoluContain("b2(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotal_DiskAndBoardTogether() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where Capacity=40",
                "mainBoard:Sum_Quantity ==1 where Capacity=80",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("d1(Q:1,H:0,S:1),b1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotalWhere_DoesNotOverrideSingleCategoryCandidates() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where PortRate=100G",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity <=0 where PortRate=10G");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("d3(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotalWhere_RunsOnFilteredCandidatesOnly() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where PortRate=10G",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity >=100 where Capacity>=60");
        printSimpleSolutions();

        resultAssert().assertNoSolution();
    }

    @Test
    public void testCrossTotal_MultiInstAggregatesAllInstances() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where Capacity=40",
                "disk:Sum_Quantity ==1 where Capacity=60",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("d1(Q:1,H:0,S:1),I1_d2(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotal_InvalidCategoryFails() {
        inferRecommendModule("disk,missingBoard:Sum_Capacity >=100");
        printSimpleSolutions();

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("missingBoard");
    }

    @Test
    public void testCrossTotal_NonNumericAttrFails() {
        inferRecommendModule("disk,mainBoard:Sum_PortRate >=100");
        printSimpleSolutions();

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("PortRate", "not numeric");
    }

    @Test
    public void testCrossTotal_UnknownWhereAttrFails() {
        inferRecommendModule("disk,mainBoard:Sum_Capacity >=100 where UnknownAttr=10G");
        printSimpleSolutions();

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("UnknownAttr");
    }

    @Test
    public void testCrossTotal_OptionalCategoryCanSatisfyPresentBranch() {
        inferRecommendModule(
                "mainBoard:Sum_Quantity ==1 where Capacity=80",
                "mainBoard,expansionBoard:Sum_Capacity >=140 where PortRate=10G");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("b1(Q:1,H:0,S:1),e1(Q:1,H:0,S:1)");
    }
}
