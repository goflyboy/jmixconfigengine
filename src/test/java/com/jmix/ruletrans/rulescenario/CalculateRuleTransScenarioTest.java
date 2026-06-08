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
        RuleMetadata metadata = metadata("ruleIfElseQuantity", "if p1 op11 and p2 op21 then pt1 is 1 else 3",
                "", "");

        String methodBody = """
                AlgCPBoolVar op11AndOp21 = model().newBoolVar("rule_op11_and_op21");
                model().addBoolAnd(new AlgCPLiteral[] {
                        p1.option("op11").selectedVar(),
                        p2.option("op21").selectedVar()
                }).onlyEnforceIf(op11AndOp21);
                model().addBoolOr(new AlgCPLiteral[] {
                        p1.option("op11").selectedVar().not(),
                        p2.option("op21").selectedVar().not()
                }).onlyEnforceIf(op11AndOp21.not());
                model().addEquality(pt1.quantityVar(), 1).onlyEnforceIf(op11AndOp21);
                model().addEquality(pt1.quantityVar(), 3).onlyEnforceIf(op11AndOp21.not());
                """;

        assertExecutableScenario(
                methodBody,
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
        RuleMetadata metadata = metadata("ruleIntegerParameterSum", "part quantity equals p1 plus p2", "", "");

        String methodBody = """
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
