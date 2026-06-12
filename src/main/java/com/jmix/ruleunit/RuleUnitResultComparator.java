package com.jmix.ruleunit;

import com.jmix.ruletrans.testgen.business.BusinessCaseExpect;
import com.jmix.ruletrans.testgen.business.RuleUnitDiagnostic;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares only fields explicitly present in business expectations.
 */
final class RuleUnitResultComparator {

    List<String> compare(BusinessCaseExpect expect, RuleUnitActualResult actual) {
        List<String> failures = new ArrayList<>();
        if (expect.compatible() != null && !Objects.equals(expect.compatible(), actual.compatible())) {
            failures.add("compatible expected " + expect.compatible() + " but was " + actual.compatible());
        }
        compareParameters(expect.parameters(), actual.parameters(), failures, "actual");
        compareParts(expect.parts(), actual.parts(), failures, "actual");
        compareDiagnostics(expect.diagnostics(), actual.diagnostics(), failures);
        compareSolutions(expect.solutions(), actual.solutions(), failures);
        return failures;
    }

    private void compareParameters(
            List<RuleUnitParameter> expected,
            List<RuleUnitParameter> actual,
            List<String> failures,
            String scope) {
        for (RuleUnitParameter expectedParameter : expected) {
            RuleUnitParameter actualParameter = actual.stream()
                    .filter(parameter -> Objects.equals(parameter.code(), expectedParameter.code()))
                    .findFirst()
                    .orElse(null);
            if (actualParameter == null) {
                failures.add(scope + " parameter not found: " + expectedParameter.code());
                continue;
            }
            if (expectedParameter.value() != null
                    && !Objects.equals(expectedParameter.value(), actualParameter.value())) {
                failures.add(scope + " parameter " + expectedParameter.code()
                        + " value expected " + expectedParameter.value()
                        + " but was " + actualParameter.value());
            }
            if (expectedParameter.hidden() != null
                    && !Objects.equals(expectedParameter.hidden(), actualParameter.hidden())) {
                failures.add(scope + " parameter " + expectedParameter.code()
                        + " hidden expected " + expectedParameter.hidden()
                        + " but was " + actualParameter.hidden());
            }
        }
    }

    private void compareParts(
            List<RuleUnitPart> expected,
            List<RuleUnitPart> actual,
            List<String> failures,
            String scope) {
        for (RuleUnitPart expectedPart : expected) {
            RuleUnitPart actualPart = actual.stream()
                    .filter(part -> Objects.equals(part.code(), expectedPart.code()))
                    .findFirst()
                    .orElse(null);
            if (actualPart == null) {
                failures.add(scope + " part not found: " + expectedPart.code());
                continue;
            }
            if (expectedPart.quantity() != null
                    && !Objects.equals(expectedPart.quantity(), actualPart.quantity())) {
                failures.add(scope + " part " + expectedPart.code()
                        + " quantity expected " + expectedPart.quantity()
                        + " but was " + actualPart.quantity());
            }
            if (expectedPart.isSelected() != null
                    && !Objects.equals(expectedPart.isSelected(), actualPart.isSelected())) {
                failures.add(scope + " part " + expectedPart.code()
                        + " selected expected " + expectedPart.isSelected()
                        + " but was " + actualPart.isSelected());
            }
            if (expectedPart.hidden() != null
                    && !Objects.equals(expectedPart.hidden(), actualPart.hidden())) {
                failures.add(scope + " part " + expectedPart.code()
                        + " hidden expected " + expectedPart.hidden()
                        + " but was " + actualPart.hidden());
            }
        }
    }

    private void compareDiagnostics(
            List<RuleUnitDiagnostic> expected,
            List<RuleUnitDiagnostic> actual,
            List<String> failures) {
        for (RuleUnitDiagnostic expectedDiagnostic : expected) {
            boolean exists = actual.stream().anyMatch(diagnostic ->
                    Objects.equals(diagnostic.ruleCode(), expectedDiagnostic.ruleCode()));
            if (!exists) {
                failures.add("diagnostic ruleCode not found: " + expectedDiagnostic.ruleCode());
            }
        }
    }

    private void compareSolutions(
            List<RuleUnitSolution> expected,
            List<RuleUnitSolution> actual,
            List<String> failures) {
        for (int i = 0; i < expected.size(); i++) {
            RuleUnitSolution expectedSolution = expected.get(i);
            RuleUnitSolution actualSolution = actualSolution(expectedSolution, actual, i);
            if (actualSolution == null) {
                failures.add("solution not found for rank " + expectedSolution.rank());
                continue;
            }
            String scope = "solution rank " + (expectedSolution.rank() == null ? i + 1 : expectedSolution.rank());
            compareParts(expectedSolution.parts(), actualSolution.parts(), failures, scope);
            compareParameters(expectedSolution.parameters(), actualSolution.parameters(), failures, scope);
        }
    }

    private RuleUnitSolution actualSolution(RuleUnitSolution expectedSolution, List<RuleUnitSolution> actual, int index) {
        if (expectedSolution.rank() != null) {
            return actual.stream()
                    .filter(solution -> Objects.equals(solution.rank(), expectedSolution.rank()))
                    .findFirst()
                    .orElse(null);
        }
        if (actual.size() <= index) {
            return null;
        }
        return actual.get(index);
    }
}
