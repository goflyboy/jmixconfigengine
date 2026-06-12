package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Business-facing diagnostic expectation or actual diagnostic output.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleUnitDiagnostic(
        String ruleCode,
        String reason) {
}
