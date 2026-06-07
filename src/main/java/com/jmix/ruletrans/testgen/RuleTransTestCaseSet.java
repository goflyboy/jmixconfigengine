package com.jmix.ruletrans.testgen;

import java.util.List;

/**
 * Minimal structured test case set generated for RuleTrans P1 flows.
 */
public record RuleTransTestCaseSet(List<String> cases) {

    public static RuleTransTestCaseSet empty() {
        return new RuleTransTestCaseSet(List.of());
    }
}
