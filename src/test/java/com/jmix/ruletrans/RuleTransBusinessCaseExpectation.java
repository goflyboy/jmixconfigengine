package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Assertion helper for generated business-readable test cases.
 */
@FunctionalInterface
public interface RuleTransBusinessCaseExpectation {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    void assertMatches(BusinessRuleTestCaseSet caseSet);

    static RuleTransBusinessCaseExpectation any() {
        return caseSet -> {
        };
    }

    static RuleTransBusinessCaseExpectation caseCount(int expectedCount) {
        return caseSet -> {
            assertNotNull(caseSet, "businessCaseSet");
            assertEquals(expectedCount, caseSet.cases().size(),
                    () -> "Generated business case count differed:\n" + caseSet.toJson());
        };
    }

    static RuleTransBusinessCaseExpectation expectCase(
            String caseId,
            String expectedGivenJson,
            String expectedExpectJson) {
        return caseSet -> {
            assertNotNull(caseSet, "businessCaseSet");
            BusinessRuleTestCase testCase = caseSet.cases().stream()
                    .filter(candidate -> Objects.equals(candidate.id(), caseId))
                    .findFirst()
                    .orElse(null);
            assertNotNull(testCase, () -> "Generated business case not found: " + caseId
                    + "\nActual cases:\n" + caseSet.toJson());
            assertJsonContains(
                    parse(expectedGivenJson),
                    OBJECT_MAPPER.valueToTree(testCase.given()),
                    "given",
                    caseSet);
            assertJsonContains(
                    parse(expectedExpectJson),
                    OBJECT_MAPPER.valueToTree(testCase.expect()),
                    "expect",
                    caseSet);
        };
    }

    static RuleTransBusinessCaseExpectation allOf(RuleTransBusinessCaseExpectation... expectations) {
        return caseSet -> {
            if (expectations == null) {
                return;
            }
            for (RuleTransBusinessCaseExpectation expectation : expectations) {
                if (expectation != null) {
                    expectation.assertMatches(caseSet);
                }
            }
        };
    }

    private static JsonNode parse(String json) {
        try {
            return OBJECT_MAPPER.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            throw new AssertionError("Invalid expected business case JSON:\n" + json, e);
        }
    }

    private static void assertJsonContains(
            JsonNode expected,
            JsonNode actual,
            String path,
            BusinessRuleTestCaseSet caseSet) {
        List<String> failures = new ArrayList<>();
        collectJsonDiffs(expected, actual, path, failures);
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures)
                + "\nActual generated cases:\n" + caseSet.toJson());
    }

    private static void collectJsonDiffs(
            JsonNode expected,
            JsonNode actual,
            String path,
            List<String> failures) {
        if (expected == null || expected.isMissingNode()) {
            return;
        }
        if (actual == null || actual.isMissingNode()) {
            failures.add(path + " missing, expected " + expected);
            return;
        }
        if (expected.isObject()) {
            expected.fields().forEachRemaining(entry -> collectJsonDiffs(
                    entry.getValue(),
                    actual.get(entry.getKey()),
                    path + "." + entry.getKey(),
                    failures));
            return;
        }
        if (expected.isArray()) {
            if (!actual.isArray()) {
                failures.add(path + " expected array but was " + actual);
                return;
            }
            if (expected.size() != actual.size()) {
                failures.add(path + " size expected " + expected.size() + " but was " + actual.size());
                return;
            }
            for (int i = 0; i < expected.size(); i++) {
                collectJsonDiffs(expected.get(i), actual.get(i), path + "[" + i + "]", failures);
            }
            return;
        }
        if (!expected.equals(actual)) {
            failures.add(path + " expected " + expected + " but was " + actual);
        }
    }
}
