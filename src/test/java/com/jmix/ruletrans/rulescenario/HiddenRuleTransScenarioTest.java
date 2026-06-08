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
        RuleMetadata metadata = metadata("ruleParaHidden", "p0 controls p1 and p2 hidden states", "", "");

        String methodBody = """
                model().addBoolOr(new AlgCPLiteral[] { p1.hiddenVar().not(), p2.hiddenVar().not() });
                addVarAboutHiddenConstraints(p1, p2);

                AlgCPBoolVar p0Eq0 = model().newBoolVar("p0_eq_0");
                AlgCPBoolVar p0Eq1 = model().newBoolVar("p0_eq_1");
                model().addEquality(p0.valueVar(), 0).onlyEnforceIf(p0Eq0);
                model().addDifferent(p0.valueVar(), 0).onlyEnforceIf(p0Eq0.not());
                model().addEquality(p0.valueVar(), 1).onlyEnforceIf(p0Eq1);
                model().addDifferent(p0.valueVar(), 1).onlyEnforceIf(p0Eq1.not());

                AlgCPBoolVar p0In01 = model().newBoolVar("p0_in_01");
                model().addBoolOr(new AlgCPLiteral[] { p0Eq0, p0Eq1 }).onlyEnforceIf(p0In01);
                model().addBoolAnd(new AlgCPLiteral[] { p0Eq0.not(), p0Eq1.not() }).onlyEnforceIf(p0In01.not());
                model().addBoolOr(new AlgCPLiteral[] { p0Eq0.not(), p0Eq1.not() });

                model().addEquality(p1.hiddenVar(), 1).onlyEnforceIf(p0In01);
                model().addEquality(p2.hiddenVar(), 1).onlyEnforceIf(p0In01.not());
                model().addEquality(p1.valueVar(), 0).onlyEnforceIf(p1.hiddenVar());
                model().addEquality(p2.valueVar(), 0).onlyEnforceIf(p2.hiddenVar());

                AlgCPLinearExpr sumExpr = model().newLinearExpr("sum_p1_p2");
                sumExpr.addTerm(p1.valueVar(), 1);
                sumExpr.addTerm(p2.valueVar(), 1);
                model().addEquality(part1.quantityVar(), sumExpr);
                """;

        assertExecutableScenario(
                methodBody,
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
