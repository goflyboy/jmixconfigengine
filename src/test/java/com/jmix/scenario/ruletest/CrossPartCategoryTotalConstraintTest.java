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

        @PartAnno(code = "disk", supportMultiInst = true)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_40:40", "Cap_60:60", "Cap_100:100"})
        private PartCategoryVar disk;

        @PartAnno(code = "disk10g40", fatherCode = "disk", attrs = {"10G", "40"}, price = 40)
        private PartVar disk10g40;

        @PartAnno(code = "disk10g60", fatherCode = "disk", attrs = {"10G", "60"}, price = 60)
        private PartVar disk10g60;

        @PartAnno(code = "disk100g100", fatherCode = "disk", attrs = {"100G", "100"}, price = 100)
        private PartVar disk100g100;

        @PartAnno(code = "mainBoard")
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_80:80", "Cap_120:120"})
        private PartCategoryVar mainBoard;

        @PartAnno(code = "board10g80", fatherCode = "mainBoard", attrs = {"10G", "80"}, price = 80)
        private PartVar board10g80;

        @PartAnno(code = "board10g120", fatherCode = "mainBoard", attrs = {"10G", "120"}, price = 120)
        private PartVar board10g120;

        @PartAnno(code = "board100g120", fatherCode = "mainBoard", attrs = {"100G", "120"}, price = 120)
        private PartVar board100g120;

        @PartAnno(code = "expansionBoard", required = false)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_60:60"})
        private PartCategoryVar expansionBoard;

        @PartAnno(code = "exp10g60", fatherCode = "expansionBoard", attrs = {"10G", "60"}, price = 60)
        private PartVar exp10g60;
    }

    @Test
    public void testCrossTotal_BoardOnlySatisfiesAggregate() {
        inferRecommendModule(
                "disk:Sum_Quantity ==0 where PortRate=10G",
                "mainBoard:Sum_Quantity ==1 where Capacity=120",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");

        resultAssert().assertSuccess();
        assertSoluContain("PT4(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotal_DiskAndBoardTogether() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where Capacity=40",
                "mainBoard:Sum_Quantity ==1 where Capacity=80",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");

        resultAssert().assertSuccess();
        assertSoluContain("PT2(Q:1,H:0,S:1),PT6(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotalWhere_DoesNotOverrideSingleCategoryCandidates() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where PortRate=100G",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity <=0 where PortRate=10G");

        resultAssert().assertSuccess();
        assertSoluContain("PT1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotalWhere_RunsOnFilteredCandidatesOnly() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where PortRate=10G",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity >=100 where Capacity>=60");

        resultAssert().assertNoSolution();
    }

    @Test
    public void testCrossTotal_MultiInstAggregatesAllInstances() {
        inferRecommendModule(
                "disk:Sum_Quantity ==1 where Capacity=40",
                "disk:Sum_Quantity ==1 where Capacity=60",
                "mainBoard:Sum_Quantity ==0 where PortRate=10G",
                "disk,mainBoard:Sum_Capacity >=100 where PortRate=10G");

        resultAssert().assertSuccess();
        assertSoluContain("PT2(Q:1,H:0,S:1),I1_PT3(Q:1,H:0,S:1)");
    }

    @Test
    public void testCrossTotal_InvalidCategoryFails() {
        inferRecommendModule("disk,missingBoard:Sum_Capacity >=100");

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("missingBoard");
    }

    @Test
    public void testCrossTotal_NonNumericAttrFails() {
        inferRecommendModule("disk,mainBoard:Sum_PortRate >=100");

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("PortRate", "not numeric");
    }

    @Test
    public void testCrossTotal_UnknownWhereAttrFails() {
        inferRecommendModule("disk,mainBoard:Sum_Capacity >=100 where UnknownAttr=10G");

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("UnknownAttr");
    }

    @Test
    public void testCrossTotal_OptionalCategoryCanSatisfyPresentBranch() {
        inferRecommendModule(
                "mainBoard:Sum_Quantity ==1 where Capacity=80",
                "mainBoard,expansionBoard:Sum_Capacity >=140 where PortRate=10G");

        resultAssert().assertSuccess();
        assertSoluContain("PT6(Q:1,H:0,S:1),PT7(Q:1,H:0,S:1)");
    }
}
