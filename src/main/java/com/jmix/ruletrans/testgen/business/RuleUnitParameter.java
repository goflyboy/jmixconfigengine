package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Business-facing parameter input or expected output.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleUnitParameter(
        String code,
        String value,
        Boolean hidden) {

    public RuleUnitParameter(String code, String value) {
        this(code, value, null);
    }
}
