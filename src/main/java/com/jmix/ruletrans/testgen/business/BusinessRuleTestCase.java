package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Business-readable JSON case generated for a single rule.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessRuleTestCase(
        String id,
        String title,
        BusinessRuleFamily businessFamily,
        String scenario,
        TestEnvironment environment,
        String serviceMethod,
        BusinessCaseGiven given,
        BusinessCaseExpect expect,
        String note) {

    public BusinessRuleTestCase {
        given = given == null ? BusinessCaseGiven.empty() : given;
        expect = expect == null ? BusinessCaseExpect.empty() : expect;
    }
}
