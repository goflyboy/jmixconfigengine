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
        RuleMetadata metadata = metadata("ruleDriveCapacityPriority",
                "drive 中按属性 Capacity 加权后的总数量必须至少达到参数 Sum_Capacity；优化目标按最小化构建：先减少 drive 的总数量，再在总数量相同时优先选择 Capacity 更高的部件",
                "drive", "");

        assertNaturalLanguageTranslatesAndExecutes(
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
        RuleContext context = productContext(ProductParameterPriorityFacts.class, "accelerator");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.PRIORITY);
        RuleMetadata metadata = metadata("ruleProductParameterPriority",
                "accelerator 的总数量必须至少达到整数参数 target；优化目标按最小化构建：优先选择属性 Score 更高的 accelerator 部件，同时尽量减少 accelerator 总数量",
                "", "");

        assertNaturalLanguageTranslatesAndExecutes(
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
