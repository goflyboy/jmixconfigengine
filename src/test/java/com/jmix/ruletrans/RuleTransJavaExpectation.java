package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertion helper for generated Java method bodies.
 */
@FunctionalInterface
public interface RuleTransJavaExpectation {

    void assertMatches(String methodBody);

    static RuleTransJavaExpectation contains(String expectedSnippet) {
        return methodBody -> {
            assertNotNull(methodBody, "methodBody");
            assertTrue(methodBody.contains(expectedSnippet),
                    () -> "Expected generated Java to contain [" + expectedSnippet + "] but was:\n" + methodBody);
        };
    }

    static RuleTransJavaExpectation equalsIgnoringWhitespace(String expectedMethodBody) {
        return methodBody -> {
            assertNotNull(methodBody, "methodBody");
            assertEquals(normalize(expectedMethodBody), normalize(methodBody),
                    () -> "Generated Java differed from expected body:\n" + methodBody);
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }
}
