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

class CalculateRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testIfElseCalculationAndNoSolutionBoundary() {
        RuleContext context = productContext(IfElseFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.CALCULATE);
        RuleMetadata metadata = metadata("ruleIfElseQuantity",
                "如果参数 p1 选择 op11 且参数 p2 选择 op21，则部件 pt1 的数量必须等于 1；否则 pt1 的数量必须等于 3",
                "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        inferPartCase("ifBranch", "pt1", 1, null, 1, null,
                                Map.of("p1", "op11", "p2", "op21"), null, null),
                        inferPartCase("elseBranch", "pt1", 3, null, 8,
                                Map.of("p1:op11,p2:op21", 0), null, null, null),
                        inferPartCase("noMatchingQuantity", "pt1", 4, null, 0,
                                Map.of("p1:op11,p2:op21", 0), null, null, null)),
                "CalculateIfElseScenario");
    }

    @Test
    void testIntegerParameterCalculationAndReverseInference() {
        RuleContext context = productContext(IntegerFacts.class);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.CALCULATE);
        RuleMetadata metadata = metadata("ruleIntegerParameterSum", "部件 part1 的数量必须等于整数参数 p1 加 p2", "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        inferPartCase("quantityThreeHasFourSolutions", "part1", 3, null, 4,
                                Map.of("p1:0,p2:3", 1, "p1:1,p2:2", 1, "p1:2,p2:1", 1,
                                        "p1:3,p2:0", 1),
                                null, null, null),
                        inferPartCase("quantityFourOutOfPartRange", "part1", 4, null, 0,
                                null, null, null, null),
                        inferParaCase("parametersInferPartQuantity", List.of("p1", "2", "p2", "1"), 1,
                                null, null, null, Map.of("part1", 3))),
                "CalculateIntegerScenario");
    }

    @ModuleAnno(id = 811106L)
    public static class IfElseFacts extends ModuleAlgBase {

        @ParaAnno(defaultValue = "op11", options = {"op11", "op12", "op13"})
        private ParaVar p1;

        @ParaAnno(defaultValue = "op21", options = {"op21", "op22", "op23"})
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar pt1;
    }

    @ModuleAnno(id = 811107L)
    public static class IntegerFacts extends ModuleAlgBase {

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p1;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar part1;
    }
}
