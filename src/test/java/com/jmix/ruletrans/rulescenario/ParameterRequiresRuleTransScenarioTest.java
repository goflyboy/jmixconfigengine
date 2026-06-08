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

class ParameterRequiresRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testRequiresForwardReverseAndMissedDefaultSemantics() {
        RuleContext context = productContext(ParameterFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.COMPATIBLE);
        RuleMetadata metadata = metadata("ruleParameterRequires", "A requires B", "", "");

        assertExecutableScenario(
                "addCompatibleConstraintRequires(ruleCode, a, listOf(\"a1\", \"a3\"), b, "
                        + "listOf(\"b1\", \"b2\", \"b3\"));",
                context,
                scenario,
                metadata,
                caseSet(
                        inferParaCase("a1RequiresB123", List.of("a", "a1"), 3,
                                Map.of("b:b1", 1, "b:b2", 1, "b:b3", 1, "b:b4", 0),
                                null, null, null),
                        inferParaCase("b4AllowsOnlyA2A4", List.of("b", "b4"), 2,
                                Map.of("a:a1", 0, "a:a2", 1, "a:a3", 0, "a:a4", 1),
                                null, null, null),
                        inferParaCase("a2KeepsBUnrestricted", List.of("a", "a2"), 4,
                                Map.of("b:b1", 1, "b:b2", 1, "b:b3", 1, "b:b4", 1),
                                null, null, null)),
                "ParameterRequiresScenario");
    }

    @ModuleAnno(id = 811103L)
    public static class ParameterFacts extends ModuleAlgBase {

        @ParaAnno(options = {"a1", "a2", "a3", "a4"})
        private ParaVar a;

        @ParaAnno(options = {"b1", "b2", "b3", "b4"})
        private ParaVar b;
    }
}
