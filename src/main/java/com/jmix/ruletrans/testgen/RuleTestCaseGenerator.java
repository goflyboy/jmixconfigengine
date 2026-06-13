package com.jmix.ruletrans.testgen;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.RuleUnitServiceMethod;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.tool.impl.llm.LLMInvoker;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public final class RuleTestCaseGenerator {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;

    public RuleTestCaseGenerator(LLMInvoker llmInvoker, PromptBuilder promptBuilder) {
        this.llmInvoker = llmInvoker;
        this.promptBuilder = promptBuilder;
    }

    @Deprecated(since = "RFC-0014", forRemoval = false)
    public RuleTransTestCaseSet generate(String naturalLanguage, RuleContext context, String methodBody) {
        return generate(naturalLanguage, context, null, methodBody);
    }

    @Deprecated(since = "RFC-0014", forRemoval = false)
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
            log.error("LLM test case generation failed for naturalLanguage=[{}], module=[{}]",
                    naturalLanguage, context != null ? context.module().getCode() : "null", e);
            throw new RuleTransException("LLM test case generation failed: " + e.getMessage(), e);
        }
    }

    public BusinessRuleTestCaseSet generateBusinessCases(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            String methodBody) {
        if (llmInvoker == null) {
            return BusinessRuleTestCaseSet.empty();
        }
        String prompt = promptBuilder.buildTestCasePrompt(naturalLanguage, context, scenario, methodBody);
        try {
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return normalizeBusinessCases(scenario, BusinessRuleTestCaseSet.fromJson(response));
        } catch (RuleTransException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM business test case generation failed for naturalLanguage=[{}], module=[{}]",
                    naturalLanguage, context != null ? context.module().getCode() : "null", e);
            throw new RuleTransException("LLM business test case generation failed: " + e.getMessage(), e);
        }
    }

    private BusinessRuleTestCaseSet normalizeBusinessCases(
            RuleScenario scenario,
            BusinessRuleTestCaseSet caseSet) {
        if (caseSet.isEmpty()) {
            return caseSet;
        }
        List<BusinessRuleTestCase> cases = caseSet.cases().stream()
                .map(testCase -> normalizeBusinessCase(scenario, testCase))
                .toList();
        return new BusinessRuleTestCaseSet(caseSet.ruleMethod(), cases);
    }

    private BusinessRuleTestCase normalizeBusinessCase(
            RuleScenario scenario,
            BusinessRuleTestCase testCase) {
        BusinessRuleFamily family = testCase.businessFamily() == null
                ? businessFamily(scenario)
                : testCase.businessFamily();
        TestEnvironment environment = testCase.environment() == null
                ? defaultEnvironment(scenario)
                : testCase.environment();
        String serviceMethod = notBlank(testCase.serviceMethod())
                ? RuleUnitServiceMethod.from(testCase.serviceMethod()).name()
                : serviceMethod(family, environment);
        return new BusinessRuleTestCase(
                testCase.id(),
                testCase.title(),
                family,
                testCase.scenario(),
                environment,
                serviceMethod,
                testCase.given(),
                testCase.expect(),
                testCase.note());
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
        if (context != null && context.isModuleLevel()) {
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

    private BusinessRuleFamily businessFamily(RuleScenario scenario) {
        if (scenario == null) {
            return BusinessRuleFamily.ASSIGNMENT;
        }
        return switch (scenario.family()) {
            case COMPATIBLE, STRUCTURED -> BusinessRuleFamily.COMPATIBILITY;
            case PRIORITY -> BusinessRuleFamily.PRIORITY;
            default -> BusinessRuleFamily.ASSIGNMENT;
        };
    }

    private TestEnvironment defaultEnvironment(RuleScenario scenario) {
        return scenario != null && scenario.isPost()
                ? TestEnvironment.NON_CONSTRAINT
                : TestEnvironment.CONSTRAINT;
    }

    private String serviceMethod(BusinessRuleFamily family, TestEnvironment environment) {
        if (environment == TestEnvironment.NON_CONSTRAINT) {
            return RuleUnitServiceMethod.testPostAssignment.name();
        }
        return switch (family) {
            case COMPATIBILITY -> RuleUnitServiceMethod.testCompatibility.name();
            case PRIORITY -> RuleUnitServiceMethod.testPriority.name();
            case ASSIGNMENT -> RuleUnitServiceMethod.testAssignment.name();
        };
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
