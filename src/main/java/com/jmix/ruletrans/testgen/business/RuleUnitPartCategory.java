package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Business-facing PartCategory aggregate request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleUnitPartCategory(
        String category,
        String aggregate,
        String operator,
        Object value,
        Map<String, String> where) {

    public RuleUnitPartCategory {
        where = where == null ? Map.of() : Map.copyOf(where);
    }
}
