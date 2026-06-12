package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Business-facing part input or expected output.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleUnitPart(
        String code,
        Integer quantity,
        Boolean isSelected,
        Boolean hidden,
        Map<String, String> attrs) {

    public RuleUnitPart {
        attrs = attrs == null ? Map.of() : Map.copyOf(attrs);
    }

    public RuleUnitPart(String code, Integer quantity) {
        this(code, quantity, null, null, Map.of());
    }
}
