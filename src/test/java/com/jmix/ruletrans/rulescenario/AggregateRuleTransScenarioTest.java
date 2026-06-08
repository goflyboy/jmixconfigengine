package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.EffectScope;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

class AggregateRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testSingleCategoryAggregateRule() {
        RuleContext context = partCategoryContext(AggregateFacts.class, "drive");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PART_CATEGORY, RuleFamily.AGGREGATE);
        RuleMetadata metadata = metadata("ruleDriveSdAggregate", "SSD at most one selected and two quantity",
                "drive", "");

        String methodBody = """
                model().addLessOrEqual(model().sum4Selected("drive", "", "Type=sd"), 1);
                model().addLessOrEqual(model().sum4Quantity("drive", "", "Type=sd"), 2);
                """;

        assertExecutableScenario(
                methodBody,
                context,
                scenario,
                metadata,
                caseSet(
                        recommendCase("twoSdQuantityHasSolution", "HAS_SOLUTION",
                                "drive:Sum_Quantity ==2 where Type=sd"),
                        recommendCase("threeSdQuantityNoSolution", "NO_SOLUTION",
                                "drive:Sum_Quantity ==3 where Type=sd")),
                "AggregateSingleCategoryScenario");
    }

    @Test
    void testCrossCategoryAggregateRule() {
        RuleContext context = productContext(AggregateFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.AGGREGATE);
        RuleMetadata metadata = metadata("ruleCrossCapacityAtLeast100", "disk and board 10G capacity at least 100",
                "", "");

        String methodBody = """
                PartAlgCPLinearExpr total = model().sum4Quantity("disk,mainBoard", "Capacity", "PortRate=10G")
                        .name("disk_board_capacity");
                model().addGreaterOrEqual(total, 100);
                """;

        assertExecutableScenario(
                methodBody,
                context,
                scenario,
                metadata,
                caseSet(
                        recommendCase("diskAndBoardMeetTotal", "HAS_SOLUTION",
                                "disk:Sum_Quantity ==1 where Capacity=40",
                                "mainBoard:Sum_Quantity ==1 where Capacity=80"),
                        recommendCase("diskOnlyMissesTotal", "NO_SOLUTION",
                                "disk:Sum_Quantity ==1 where Capacity=40",
                                "mainBoard:Sum_Quantity ==0")),
                "AggregateCrossCategoryScenario");
    }

    @Test
    void testMultiInstanceAggregateRule() {
        RuleContext context = partCategoryContext(AggregateFacts.class, "disk");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PART_CATEGORY, RuleFamily.AGGREGATE);
        RuleMetadata metadata = metadata("ruleMultiInstDiskCapacity", "all disk instances total capacity at least 100",
                "disk", "").withEffectScope(EffectScope.AllInst);

        String methodBody = """
                PartAlgCPLinearExpr total = model().sum4Quantity("Capacity", "PortRate=10G")
                        .name("all_disk_capacity");
                model().addGreaterOrEqual(total, 100);
                """;

        assertExecutableScenario(
                methodBody,
                context,
                scenario,
                metadata,
                caseSet(
                        recommendCase("twoInstancesMeetTotal", "HAS_SOLUTION",
                                "disk:Sum_Quantity ==1 where Capacity=40",
                                "disk:Sum_Quantity ==1 where Capacity=60"),
                        recommendCase("oneInstanceMissesTotal", "NO_SOLUTION",
                                "disk:Sum_Quantity ==1 where Capacity=40")),
                "AggregateMultiInstanceScenario");
    }

    @ModuleAnno(id = 811109L)
    public static class AggregateFacts extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Type", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"SD:sd", "MD:md"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_1:1", "Cap_3:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"sd", "3"})
        private PartVar sd1;

        @PartAnno(fatherCode = "drive", attrs = {"sd", "1"})
        private PartVar sd2;

        @PartAnno(fatherCode = "drive", attrs = {"md", "1"})
        private PartVar md1;

        @ParaAnno(fatherCode = "drive", code = "Sum_Quantity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar driveSumQuantity;

        @PartAnno(code = "disk", supportMultiInst = true)
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_40:40", "Cap_60:60", "Cap_100:100"})
        private PartCategoryVar disk;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "40"})
        private PartVar d40;

        @PartAnno(fatherCode = "disk", attrs = {"10G", "60"})
        private PartVar d60;

        @PartAnno(fatherCode = "disk", attrs = {"100G", "100"})
        private PartVar d100;

        @PartAnno(code = "mainBoard")
        @DAttrAnno1(code = "PortRate", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Rate_10G:10G", "Rate_100G:100G"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_80:80", "Cap_120:120"})
        private PartCategoryVar mainBoard;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "80"})
        private PartVar b80;

        @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "120"})
        private PartVar b120;
    }
}
