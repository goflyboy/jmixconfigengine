package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
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
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.util.Map;

class PriorityRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testPartAggregatePriorityObjective() {
        RuleContext context = partCategoryContext(PartAggregatePriorityFacts.class, "drive");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PART_CATEGORY, RuleFamily.PRIORITY);
        RuleMetadata metadata = metadata("ruleDriveCapacityPriority", "prefer high capacity drives", "drive", "");

        String methodBody = """
                if (Sum_Capacity.hasInput()) {
                    int requiredCapacity = Sum_Capacity.inputValue();
                    PartAlgCPLinearExpr totalCapacity = model().sum4Quantity("drive", "Capacity", "")
                            .name("totalCapacity");
                    model().addGreaterOrEqual(totalCapacity, requiredCapacity);

                    PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("drive_capacity_objective");
                    PartAlgCPLinearExpr highCapacityExpr = model().sum4Selected("drive", "Capacity", "")
                            .name("highCapacityExpr");
                    objectiveExpr.addExpr(highCapacityExpr, -100);

                    PartAlgCPLinearExpr excessCapacityExpr = model().newPartLinearExpr("excessCapacityExpr");
                    excessCapacityExpr.addExpr(totalCapacity, 1);
                    excessCapacityExpr.addConstant(-requiredCapacity);
                    objectiveExpr.addExpr(excessCapacityExpr, 1);

                    PartAlgCPLinearExpr totalQuantity = model().sum4Quantity("drive", "", "")
                            .name("totalQuantity");
                    objectiveExpr.addExpr(totalQuantity, 800);
                    model().setObjectExpr(objectiveExpr);
                    updatePriorityObjectFuntion(ruleCode, objectiveExpr);
                }
                """;

        assertExecutableScenario(
                methodBody,
                context,
                scenario,
                metadata,
                caseSet(
                        recommendCase("capacityPriorityPutsHighCapacityFirst", "HAS_SOLUTION",
                                null,
                                Map.of("sd1", 2, "md1", 0),
                                "drive:Sum_Capacity >=5")),
                "PriorityPartAggregateScenario");
    }

    @Test
    void testProductParameterPriorityObjective() {
        RuleContext context = productContext(ProductParameterPriorityFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.PRIORITY);
        RuleMetadata metadata = metadata("ruleProductParameterPriority", "prefer fast part for target quantity",
                "", "");

        String methodBody = """
                PartAlgCPLinearExpr totalQuantity = model().sum4Quantity("accelerator", "", "")
                        .name("accelerator_total_quantity");
                PartAlgCPLinearExpr targetGap = model().newPartLinearExpr("target_gap");
                targetGap.addExpr(totalQuantity, 1);
                targetGap.addTerm(target.valueVar(), -1);
                model().addGreaterOrEqual(targetGap, 0);

                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("product_target_objective");
                objectiveExpr.addExpr(model().sum4Selected("accelerator", "Score", ""), -100);
                objectiveExpr.addExpr(totalQuantity, 10);
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion(ruleCode, objectiveExpr);
                """;

        assertExecutableScenario(
                methodBody,
                context,
                scenario,
                metadata,
                caseSet(
                        inferParaCase("targetChoosesFastFirst", java.util.List.of("target", "1"), null,
                                null, null, null, Map.of("fast", 1, "economy", 0))),
                "PriorityProductParameterScenario");
    }

    @ModuleAnno(id = 811110L)
    public static class PartAggregatePriorityFacts extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_1:1", "Cap_3:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"3"})
        private PartVar sd1;

        @PartAnno(fatherCode = "drive", attrs = {"1"})
        private PartVar md1;

        @ParaAnno(fatherCode = "drive", code = "Sum_Capacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Capacity;
    }

    @ModuleAnno(id = 811111L)
    public static class ProductParameterPriorityFacts extends ModuleAlgBase {

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar target;

        @PartAnno(code = "accelerator")
        @DAttrAnno1(code = "Score", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Score_10:10", "Score_1:1"})
        private PartCategoryVar accelerator;

        @PartAnno(fatherCode = "accelerator", attrs = {"10"}, maxQuantity = 1)
        private PartVar fast;

        @PartAnno(fatherCode = "accelerator", attrs = {"1"}, maxQuantity = 1)
        private PartVar economy;
    }
}
