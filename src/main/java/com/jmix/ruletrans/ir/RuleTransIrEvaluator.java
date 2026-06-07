package com.jmix.ruletrans.ir;

import com.jmix.ruletrans.context.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates whether a generated snippet can move toward a schema/IR path.
 */
public final class RuleTransIrEvaluator {

    public RuleTransIrAssessment assess(RuleContext context, String snippet) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (snippet == null || snippet.trim().isEmpty()) {
            throw new IllegalArgumentException("snippet must not be blank");
        }

        List<String> blockers = new ArrayList<>();
        if (usesModuleAlgBaseFacade(snippet)) {
            blockers.add("Generated snippet uses ModuleAlgBase southbound facade calls.");
        }
        if (snippet.contains("@CodeRuleAnno")) {
            blockers.add("Generated output is still a Java @CodeRuleAnno method snippet.");
        }

        if (blockers.isEmpty()) {
            return new RuleTransIrAssessment(
                    RuleTransIrAssessment.Status.SCHEMA_EMISSION_CANDIDATE,
                    "SCHEMA_OR_IR",
                    true,
                    context.categoryCodes(),
                    List.of());
        }
        return new RuleTransIrAssessment(
                RuleTransIrAssessment.Status.MODULE_ALG_BASE_BOUND,
                "MODULE_ALG_BASE_SNIPPET",
                false,
                context.categoryCodes(),
                blockers);
    }

    private boolean usesModuleAlgBaseFacade(String snippet) {
        return snippet.contains("model()")
                || snippet.contains("inCompatible(")
                || snippet.contains("compatible(")
                || snippet.contains("require(");
    }
}
