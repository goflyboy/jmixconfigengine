package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class HiddenRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testParameterHiddenValueResetAndInference() {
        RuleContext context = productContext(HiddenFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.HIDDEN);
        RuleMetadata metadata = metadata("ruleParaHidden",
                "p1 和 p2 至少一个可见；当参数 p0 等于 0 或 1 时隐藏 p1，否则隐藏 p2；隐藏参数的值必须为 0；部件 part1 的数量必须等于 p1 加 p2",
                "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        inferPartCase("p0ZeroHidesP1", "part1", 1, List.of("p0", "0"), 1,
                                null,
                                Map.of("p1", "0", "p2", "1"),
                                Map.of("p1", true, "p2", false),
                                Map.of("part1", 1)),
                        inferPartCase("p0TwoHidesP2", "part1", 1, List.of("p0", "2"), 1,
                                null,
                                Map.of("p1", "1", "p2", "0"),
                                Map.of("p1", false, "p2", true),
                                Map.of("part1", 1)),
                        inferParaCase("visibleP2InfersPart", List.of("p0", "1", "p2", "2"), 1,
                                null,
                                Map.of("p1", "0", "p2", "2"),
                                Map.of("p1", true, "p2", false),
                                Map.of("part1", 2))),
                "HiddenParameterScenario");
    }

    @ModuleAnno(id = 811108L)
    public static class HiddenFacts extends ModuleAlgBase {

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "3")
        private ParaVar p0;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar p1;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar part1;
    }
}
