package com.jmix.ruletrans.testgen;

import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.impl.llm.LLMInvoker;

import java.util.HashSet;
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

    public RuleTransTestCaseSet generate(String naturalLanguage, RuleContext context, String snippet) {
        if (llmInvoker == null) {
            return RuleTransTestCaseSet.empty();
        }
        String prompt = promptBuilder.buildTestCasePrompt(naturalLanguage, context, snippet);
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
}
