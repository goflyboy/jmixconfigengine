package com.jmix.ruletrans.testgen;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.tool.impl.llm.LLMInvoker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates and parses structured P1 test cases using the existing LLM invoker.
 */
public final class RuleTestCaseGenerator {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;

    public RuleTestCaseGenerator(LLMInvoker llmInvoker, PromptBuilder promptBuilder) {
        this.llmInvoker = llmInvoker;
        this.promptBuilder = promptBuilder;
    }

    public RuleTransTestCaseSet generate(String naturalLanguage, RuleContext context, String methodBody) {
        return generate(naturalLanguage, context, null, methodBody);
    }

    public RuleTransTestCaseSet generate(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            String methodBody) {
        if (llmInvoker == null) {
            return RuleTransTestCaseSet.empty();
        }
        String prompt = promptBuilder.buildTestCasePrompt(naturalLanguage, context, scenario, methodBody);
        try {
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return pruneGeneratedCases(naturalLanguage, context, RuleTransTestCaseSet.fromJson(response));
        } catch (RuleTransException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleTransException("LLM test case generation failed: " + e.getMessage(), e);
        }
    }

    private RuleTransTestCaseSet pruneGeneratedCases(
            String naturalLanguage,
            RuleContext context,
            RuleTransTestCaseSet testCaseSet) {
        if (testCaseSet.isEmpty()) {
            return testCaseSet;
        }
        boolean allowRecommendCases = allowsRecommendCases(naturalLanguage, context);
        List<RuleTransTestCase> cases = testCaseSet.cases().stream()
                .filter(testCase -> allowRecommendCases || !testCase.isRecommendCase())
                .filter(this::hasDistinctSelectedParts)
                .map(testCase -> normalizeValidateCase(context, testCase))
                .filter(testCase -> testCase != null)
                .toList();
        return new RuleTransTestCaseSet(testCaseSet.ruleMethod(), cases);
    }

    private boolean allowsRecommendCases(String naturalLanguage, RuleContext context) {
        if (context != null && context.isProductLevel()) {
            return true;
        }
        String text = naturalLanguage == null ? "" : naturalLanguage.toLowerCase(Locale.ROOT);
        return text.contains("recommend")
                || text.contains("infer")
                || text.contains("solution")
                || text.contains("no solution");
    }

    private boolean hasDistinctSelectedParts(RuleTransTestCase testCase) {
        if (!testCase.isValidateCase()) {
            return true;
        }
        Set<String> parts = new HashSet<>();
        for (String part : testCase.selectedPartsOrEmpty()) {
            if (!parts.add(part)) {
                return false;
            }
        }
        return true;
    }

    private RuleTransTestCase normalizeValidateCase(RuleContext context, RuleTransTestCase testCase) {
        if (!testCase.isValidateCase() || context == null || context.module() == null) {
            return testCase;
        }
        Set<String> selectedPartCodes = new LinkedHashSet<>(testCase.selectedPartsOrEmpty());
        Set<String> targetCategoryCodes = new HashSet<>(context.categoryCodes());
        if (omitsRequiredTargetCategory(context, selectedPartCodes, targetCategoryCodes)) {
            return null;
        }
        List<String> normalizedSelectedParts = new ArrayList<>(selectedPartCodes);
        for (PartCategory category : sortedCategories(context)) {
            if (!category.isRequiredSelection() || targetCategoryCodes.contains(category.getCode())) {
                continue;
            }
            if (containsSelectedPart(category, selectedPartCodes)) {
                continue;
            }
            firstAtomicPartCode(category).ifPresent(partCode -> {
                selectedPartCodes.add(partCode);
                normalizedSelectedParts.add(partCode);
            });
        }
        if (normalizedSelectedParts.equals(testCase.selectedPartsOrEmpty())) {
            return testCase;
        }
        return new RuleTransTestCase(
                testCase.id(),
                testCase.type(),
                normalizedSelectedParts,
                testCase.expectedValid(),
                testCase.expectedViolatedRuleCodes(),
                testCase.requests(),
                testCase.expectedResult());
    }

    private boolean omitsRequiredTargetCategory(
            RuleContext context,
            Set<String> selectedPartCodes,
            Set<String> targetCategoryCodes) {
        for (PartCategory category : sortedCategories(context)) {
            if (!category.isRequiredSelection() || !targetCategoryCodes.contains(category.getCode())) {
                continue;
            }
            if (!containsSelectedPart(category, selectedPartCodes)) {
                return true;
            }
        }
        return false;
    }

    private List<PartCategory> sortedCategories(RuleContext context) {
        return context.module().getAllPartCategorys().stream()
                .sorted(Comparator.comparing(PartCategory::getCode))
                .toList();
    }

    private boolean containsSelectedPart(PartCategory category, Set<String> selectedPartCodes) {
        return category.getAllAtomicParts().stream()
                .map(Part::getCode)
                .anyMatch(selectedPartCodes::contains);
    }

    private java.util.Optional<String> firstAtomicPartCode(PartCategory category) {
        return category.getAllAtomicParts().stream()
                .map(Part::getCode)
                .sorted()
                .findFirst();
    }
}
