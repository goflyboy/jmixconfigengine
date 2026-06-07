package com.jmix.ruletrans.testgen;

import java.util.List;

/**
 * Structured generated test case for RuleTrans P1 validation.
 */
public record RuleTransTestCase(
        String id,
        String type,
        List<String> selectedParts,
        Boolean expectedValid,
        List<String> expectedViolatedRuleCodes,
        List<String> requests,
        String expectedResult) {

    public static final String TYPE_VALIDATE = "validate";
    public static final String TYPE_RECOMMEND = "recommend";

    public List<String> selectedPartsOrEmpty() {
        return selectedParts == null ? List.of() : selectedParts;
    }

    public List<String> expectedViolatedRuleCodesOrEmpty() {
        return expectedViolatedRuleCodes == null ? List.of() : expectedViolatedRuleCodes;
    }

    public List<String> requestsOrEmpty() {
        return requests == null ? List.of() : requests;
    }

    public String normalizedType() {
        return type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isValidateCase() {
        return TYPE_VALIDATE.equals(normalizedType());
    }

    public boolean isRecommendCase() {
        return TYPE_RECOMMEND.equals(normalizedType());
    }
}
