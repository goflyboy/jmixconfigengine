package com.jmix.ruletrans.testgen.business;

import com.jmix.ruletrans.RuleTransException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Business-readable test cases generated for one rule method.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessRuleTestCaseSet(
        String ruleMethod,
        List<BusinessRuleTestCase> cases) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public BusinessRuleTestCaseSet {
        cases = cases == null ? List.of() : List.copyOf(cases);
    }

    public static BusinessRuleTestCaseSet empty() {
        return new BusinessRuleTestCaseSet("", List.of());
    }

    public static BusinessRuleTestCaseSet fromJson(String rawJson) {
        String json = extractJson(rawJson);
        if (json.isBlank()) {
            return empty();
        }
        try {
            if (json.startsWith("[")) {
                List<BusinessRuleTestCase> parsedCases = OBJECT_MAPPER.readValue(
                        json,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(
                                List.class, BusinessRuleTestCase.class));
                return new BusinessRuleTestCaseSet("", parsedCases);
            }
            if (json.contains("\"cases\"")) {
                return OBJECT_MAPPER.readValue(json, BusinessRuleTestCaseSet.class);
            }
            BusinessRuleTestCase parsedCase = OBJECT_MAPPER.readValue(json, BusinessRuleTestCase.class);
            return new BusinessRuleTestCaseSet("", List.of(parsedCase));
        } catch (Exception e) {
            throw new RuleTransException("Failed to parse business rule test case JSON: " + e.getMessage(), e);
        }
    }

    public static BusinessRuleTestCaseSet fromFile(Path path) {
        try {
            return fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuleTransException("Failed to read business rule test case file: " + path, e);
        }
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuleTransException("Failed to write business rule test case JSON: " + e.getMessage(), e);
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return cases.isEmpty();
    }

    public void writeTo(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, toJson(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuleTransException("Failed to write business rule test case file: " + path, e);
        }
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
