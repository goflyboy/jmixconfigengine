package com.jmix.ruletrans.testgen;

import com.jmix.ruletrans.RuleTransException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Structured test case set generated for RuleTrans P1 flows.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleTransTestCaseSet(
        String ruleMethod,
        List<RuleTransTestCase> cases) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public RuleTransTestCaseSet {
        cases = cases == null ? List.of() : List.copyOf(cases);
    }

    public static RuleTransTestCaseSet empty() {
        return new RuleTransTestCaseSet("", List.of());
    }

    public static RuleTransTestCaseSet fromJson(String rawJson) {
        String json = extractJson(rawJson);
        if (json.isBlank()) {
            return empty();
        }
        try {
            if (json.startsWith("[")) {
                List<RuleTransTestCase> parsedCases = OBJECT_MAPPER.readValue(
                        json,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(
                                List.class, RuleTransTestCase.class));
                return new RuleTransTestCaseSet("", parsedCases);
            }
            return OBJECT_MAPPER.readValue(json, RuleTransTestCaseSet.class);
        } catch (Exception e) {
            throw new RuleTransException("Failed to parse RuleTrans test case JSON: " + e.getMessage(), e);
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return cases.isEmpty();
    }

    private static String extractJson(String rawJson) {
        if (rawJson == null) {
            return "";
        }
        String text = rawJson.trim();
        int firstFence = text.indexOf("```");
        if (firstFence >= 0) {
            int codeStart = text.indexOf('\n', firstFence);
            int codeEnd = codeStart < 0 ? -1 : text.indexOf("```", codeStart + 1);
            if (codeStart >= 0 && codeEnd > codeStart) {
                text = text.substring(codeStart + 1, codeEnd).trim();
            }
        }
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        int start = firstJsonStart(objectStart, arrayStart);
        if (start < 0) {
            return text;
        }
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int end = text.lastIndexOf(close);
        if (end >= start) {
            return text.substring(start, end + 1).trim();
        }
        return text.substring(start).trim();
    }

    private static int firstJsonStart(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }
}
