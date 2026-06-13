package com.jmix.ruleunit;

import java.util.List;

/**
 * Report returned after executing a business JSON case set.
 */
public record RuleUnitTestCaseSetReport(
        boolean passed,
        List<RuleUnitTestReport> caseReports) {

    public RuleUnitTestCaseSetReport {
        caseReports = caseReports == null ? List.of() : List.copyOf(caseReports);
    }
}
