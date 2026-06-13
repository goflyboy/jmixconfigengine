package com.jmix.ruleunit;

import java.util.List;

/**
 * Report returned after executing one business JSON case.
 */
public record RuleUnitTestReport(
        String caseId,
        boolean passed,
        RuleUnitActualResult actual,
        List<String> failures) {

    public RuleUnitTestReport {
        failures = failures == null ? List.of() : List.copyOf(failures);
    }

    public static RuleUnitTestReport passed(String caseId, RuleUnitActualResult actual) {
        return new RuleUnitTestReport(caseId, true, actual, List.of());
    }

    public static RuleUnitTestReport failed(String caseId, RuleUnitActualResult actual, List<String> failures) {
        return new RuleUnitTestReport(caseId, false, actual, failures);
    }
}
