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

class ParameterCoDependentRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testCoDependentForwardReverseAndMissedDefaultSemantics() {
        RuleContext context = moduleContext(ParameterFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.COMPATIBLE);
        RuleMetadata metadata = metadata("ruleParameterCoDependent",
                "参数 a 选择 a1 或 a3 时，参数 b 只能选择 b1、b2 或 b3；参数 b 选择 b1、b2 或 b3 时，参数 a 也只能选择 a1 或 a3",
                "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        inferParaCase("a1RequiresB123", List.of("a", "a1"), 3,
                                Map.of("b:b1", 1, "b:b2", 1, "b:b3", 1, "b:b4", 0, "b:b5", 0),
                                null, null, null),
                        inferParaCase("a2RequiresB45", List.of("a", "a2"), 2,
                                Map.of("b:b1", 0, "b:b2", 0, "b:b3", 0, "b:b4", 1, "b:b5", 1),
                                null, null, null),
                        inferParaCase("b1RequiresA13", List.of("b", "b1"), 2,
                                Map.of("a:a1", 1, "a:a2", 0, "a:a3", 1, "a:a4", 0, "a:a5", 0),
                                null, null, null)),
                "ParameterCoDependentScenario");
    }

    @ModuleAnno(id = 811105L)
    public static class ParameterFacts extends ModuleAlgBase {

        @ParaAnno(options = {"a1", "a2", "a3", "a4", "a5"})
        private ParaVar a;

        @ParaAnno(options = {"b1", "b2", "b3", "b4", "b5"})
        private ParaVar b;
    }
}
