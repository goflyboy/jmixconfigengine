package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

class SelectionRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testAtMostOneOnOptionalCategory() {
        RuleContext context = partCategoryContext(SelectionFacts.class, "gpu");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PART_CATEGORY, RuleFamily.SELECT);
        RuleMetadata metadata = metadata("ruleGpuAtMostOne", "gpu 最多只能选择一个部件", "gpu", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        validateCase("noGpuIsValidForOptionalCategory", true, "cpu4"),
                        validateCase("oneGpuIsValid", true, "cpu4", "gpuLow"),
                        validateCase("twoGpuInvalid", false, "cpu4", "gpuLow", "gpuHigh")),
                "SelectionAtMostOneScenario");
    }

    @Test
    void testExactlyOneOnRequiredCategory() {
        RuleContext context = partCategoryContext(SelectionFacts.class, "cpu");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PART_CATEGORY, RuleFamily.SELECT);
        RuleMetadata metadata = metadata("ruleCpuExactlyOne", "cpu 必须且只能选择一个部件", "cpu", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        validateCase("noCpuInvalid", false),
                        validateCase("oneCpuValid", true, "cpu4"),
                        validateCase("twoCpuInvalid", false, "cpu4", "cpu8")),
                "SelectionExactlyOneScenario");
    }

    @ModuleAnno(id = 811101L)
    public static class SelectionFacts extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Core_4:4", "Core_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(code = "gpu", required = false)
        @DAttrAnno1(code = "Tier", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Low:low", "High:high"})
        private PartCategoryVar gpu;

        @PartAnno(fatherCode = "gpu", attrs = {"low"})
        private PartVar gpuLow;

        @PartAnno(fatherCode = "gpu", attrs = {"high"})
        private PartVar gpuHigh;
    }
}
