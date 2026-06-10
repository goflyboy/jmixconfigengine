package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ParameterIncompatibleRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testIncompatibleForwardReverseAndMissedDefaultSemantics() {
        RuleContext context = moduleContext(ParameterFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.COMPATIBLE);
        RuleMetadata metadata = metadata("ruleParameterIncompatible",
                "参数 a 选择 a1 或 a3 时，参数 b 不能选择 b1、b2 或 b3", "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        inferParaCase("a1RejectsB123", List.of("a", "a1"), 2,
                                Map.of("b:b1", 0, "b:b2", 0, "b:b3", 0, "b:b4", 1, "b:b5", 1),
                                null, null, null),
                        inferParaCase("b1RejectsA13", List.of("b", "b1"), 3,
                                Map.of("a:a1", 0, "a:a2", 1, "a:a3", 0, "a:a4", 1, "a:a5", 1),
                                null, null, null),
                        inferParaCase("a2KeepsBUnrestricted", List.of("a", "a2"), 5,
                                Map.of("b:b1", 1, "b:b2", 1, "b:b3", 1, "b:b4", 1, "b:b5", 1),
                                null, null, null)),
                "ParameterIncompatibleScenario");
    }

    @ModuleAnno(id = 811104L)
    public static class ParameterFacts extends ModuleAlgBase {

        @ParaAnno(options = {"a1", "a2", "a3", "a4", "a5"})
        private ParaVar a;

        @ParaAnno(options = {"b1", "b2", "b3", "b4", "b5"})
        private ParaVar b;
    }
}
