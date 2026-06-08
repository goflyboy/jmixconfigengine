package com.jmix.ruletrans.testgen;

import java.util.List;
import java.util.Map;

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
        String expectedResult,
        String partCode,
        Integer quantity,
        List<String> preParas,
        Integer expectedSolutionCount,
        Map<String, Integer> expectedConditionCounts,
        Map<String, String> expectedFirstParaValues,
        Map<String, Boolean> expectedFirstParaHidden,
        Map<String, Integer> expectedFirstPartQuantities,
        List<String> expectedAllParaNonBlank,
        Map<String, Integer> expectedAllParaMinValues,
        Map<String, String> expectedAllParaValues) {

    public static final String TYPE_VALIDATE = "validate";
    public static final String TYPE_RECOMMEND = "recommend";
    public static final String TYPE_INFER_PART = "inferPart";
    public static final String TYPE_INFER_PARA = "inferPara";
    public static final String TYPE_POST_RECOMMEND = "postRecommend";

    public RuleTransTestCase(
            String id,
            String type,
            List<String> selectedParts,
            Boolean expectedValid,
            List<String> expectedViolatedRuleCodes,
            List<String> requests,
            String expectedResult) {
        this(id, type, selectedParts, expectedValid, expectedViolatedRuleCodes, requests, expectedResult,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    public List<String> selectedPartsOrEmpty() {
        return selectedParts == null ? List.of() : selectedParts;
    }

    public List<String> expectedViolatedRuleCodesOrEmpty() {
        return expectedViolatedRuleCodes == null ? List.of() : expectedViolatedRuleCodes;
    }

    public List<String> requestsOrEmpty() {
        return requests == null ? List.of() : requests;
    }

    public List<String> preParasOrEmpty() {
        return preParas == null ? List.of() : preParas;
    }

    public Map<String, Integer> expectedConditionCountsOrEmpty() {
        return expectedConditionCounts == null ? Map.of() : expectedConditionCounts;
    }

    public Map<String, String> expectedFirstParaValuesOrEmpty() {
        return expectedFirstParaValues == null ? Map.of() : expectedFirstParaValues;
    }

    public Map<String, Boolean> expectedFirstParaHiddenOrEmpty() {
        return expectedFirstParaHidden == null ? Map.of() : expectedFirstParaHidden;
    }

    public Map<String, Integer> expectedFirstPartQuantitiesOrEmpty() {
        return expectedFirstPartQuantities == null ? Map.of() : expectedFirstPartQuantities;
    }

    public List<String> expectedAllParaNonBlankOrEmpty() {
        return expectedAllParaNonBlank == null ? List.of() : expectedAllParaNonBlank;
    }

    public Map<String, Integer> expectedAllParaMinValuesOrEmpty() {
        return expectedAllParaMinValues == null ? Map.of() : expectedAllParaMinValues;
    }

    public Map<String, String> expectedAllParaValuesOrEmpty() {
        return expectedAllParaValues == null ? Map.of() : expectedAllParaValues;
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

    public boolean isInferPartCase() {
        return TYPE_INFER_PART.toLowerCase(java.util.Locale.ROOT).equals(normalizedType());
    }

    public boolean isInferParaCase() {
        return TYPE_INFER_PARA.toLowerCase(java.util.Locale.ROOT).equals(normalizedType());
    }

    public boolean isPostRecommendCase() {
        return TYPE_POST_RECOMMEND.toLowerCase(java.util.Locale.ROOT).equals(normalizedType());
    }
}
