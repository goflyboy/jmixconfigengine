package com.jmix.ruletrans.testgen.business;

import com.jmix.ruletrans.RuleTransException;

import java.util.Locale;

/**
 * Public service methods accepted by business JSON test cases.
 */
public enum RuleUnitServiceMethod {
    testAssignment,
    testCompatibility,
    testPriority,
    testPostAssignment;

    public static RuleUnitServiceMethod from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuleTransException("serviceMethod must not be blank");
        }
        String normalized = value.trim();
        for (RuleUnitServiceMethod method : values()) {
            if (method.name().equals(normalized)) {
                return method;
            }
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (RuleUnitServiceMethod method : values()) {
            if (method.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return method;
            }
        }
        throw new RuleTransException("Unsupported serviceMethod: " + value);
    }
}
