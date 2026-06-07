package com.jmix.ruletrans.ir;

import java.util.List;

/**
 * P2 assessment for future RuleTrans IR or schema generation paths.
 */
public record RuleTransIrAssessment(
        Status status,
        String generationPath,
        boolean schemaEmissionReady,
        List<String> targetCategories,
        List<String> blockers) {

    public enum Status {
        MODULE_ALG_BASE_BOUND,
        SCHEMA_EMISSION_CANDIDATE
    }

    public RuleTransIrAssessment {
        targetCategories = targetCategories == null ? List.of() : List.copyOf(targetCategories);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
