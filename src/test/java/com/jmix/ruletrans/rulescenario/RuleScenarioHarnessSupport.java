package com.jmix.ruletrans.rulescenario;

import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.RuleTransFailureKind;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.RuleUnitCaseExecutionProcessor;
import com.jmix.ruletrans.postprocessor.RuleUnitExecutionResult;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.testgen.business.BusinessCaseExpect;
import com.jmix.ruletrans.testgen.business.BusinessCaseGiven;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.ruleunit.RuleUnitActualResult;
import com.jmix.ruleunit.RuleUnitTestReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Shared scenario harness.
 *
 * <p>Business scenario assertions call the real LLM, translate the supplied
 * natural language into a rule method body, then compile and execute the
 * generated rule against business-readable RuleUnit cases.</p>
 */
abstract class RuleScenarioHarnessSupport {

    static final String TEMP_RESOURCE_PATH = "target/ruletrans-rulescenario-resources";

    private static final List<String> OPERATORS = List.of(">=", "<=", "==", "!=", ">", "<");

    private final RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final RuleTransTempFileManager tempFileManager =
            new RuleTransTempFileManager(Path.of("target/ruletrans-rulescenario"));
    private final RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
    private final CompilationProcessor compilationProcessor = new CompilationProcessor(tempFileManager);
    private final RuleUnitCaseExecutionProcessor executionProcessor =
            new RuleUnitCaseExecutionProcessor(tempFileManager);
    private final RuleScenarioClassifier scenarioClassifier = new RuleScenarioClassifier();

    protected ModuleRuleContext moduleContext(Class<?> algClass) {
        return RuleContextFactory.fromAnnotatedClass(algClass, TEMP_RESOURCE_PATH);
    }

    protected ModuleRuleContext moduleContext(Class<?> algClass, String... categoryCodes) {
        ModuleRuleContext context = moduleContext(algClass);
        if (categoryCodes == null || categoryCodes.length == 0) {
            return context;
        }
        return RuleContextFactory.module(context.module(), Arrays.asList(categoryCodes));
    }

    protected PartCategoryRuleContext partCategoryContext(Class<?> algClass, String categoryCode) {
        return RuleContextFactory.partCategory(moduleContext(algClass).module(), categoryCode);
    }

    protected RuleMetadata metadata(
            String ruleCode,
            String naturalLanguage,
            String fatherCode,
            String attrParaCodes) {
        return new RuleMetadata(ruleCode, ruleCode, naturalLanguage, fatherCode, attrParaCodes, "", "");
    }

    protected RuleScenarioCaseSet caseSet(RuleScenarioCase... cases) {
        return new RuleScenarioCaseSet(List.of(cases));
    }

    protected RuleScenarioCase validateCase(String id, boolean expectedValid, String... selectedParts) {
        List<RuleUnitPart> parts = Arrays.stream(selectedParts == null ? new String[0] : selectedParts)
                .map(partCode -> new RuleUnitPart(partCode, null, true, null, Map.of()))
                .toList();
        BusinessRuleTestCase businessCase = businessCase(
                id,
                BusinessRuleFamily.COMPATIBILITY,
                TestEnvironment.CONSTRAINT,
                "testCompatibility",
                new BusinessCaseGiven(List.of(), parts, List.of()),
                new BusinessCaseExpect(expectedValid, List.of(), List.of(), List.of(), List.of()));
        return new RuleScenarioCase(businessCase, RuleScenarioExpectation.empty());
    }

    protected RuleScenarioCase recommendCase(String id, String expectedResult, String... requests) {
        return recommendCase(id, expectedResult, null, null, requests);
    }

    protected RuleScenarioCase recommendCase(
            String id,
            String expectedResult,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedFirstPartQuantities,
            String... requests) {
        BusinessRuleTestCase businessCase = businessCase(
                id,
                BusinessRuleFamily.ASSIGNMENT,
                TestEnvironment.CONSTRAINT,
                null,
                new BusinessCaseGiven(List.of(), List.of(), parsePartCategoryRequests(requests)),
                BusinessCaseExpect.empty());
        return new RuleScenarioCase(businessCase, new RuleScenarioExpectation(
                expectedResult,
                expectedSolutionCount,
                Map.of(),
                Map.of(),
                Map.of(),
                safeMap(expectedFirstPartQuantities),
                List.of(),
                Map.of(),
                Map.of()));
    }

    protected RuleScenarioCase inferPartCase(
            String id,
            String partCode,
            int quantity,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        BusinessRuleTestCase businessCase = businessCase(
                id,
                BusinessRuleFamily.ASSIGNMENT,
                TestEnvironment.CONSTRAINT,
                null,
                new BusinessCaseGiven(
                        parseParameters(preParas),
                        List.of(new RuleUnitPart(partCode, quantity)),
                        List.of()),
                BusinessCaseExpect.empty());
        return new RuleScenarioCase(businessCase, new RuleScenarioExpectation(
                "HAS_SOLUTION",
                expectedSolutionCount,
                safeMap(expectedConditionCounts),
                safeMap(expectedFirstParaValues),
                safeMap(expectedFirstParaHidden),
                safeMap(expectedFirstPartQuantities),
                List.of(),
                Map.of(),
                Map.of()));
    }

    protected RuleScenarioCase inferParaCase(
            String id,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        BusinessRuleTestCase businessCase = businessCase(
                id,
                BusinessRuleFamily.ASSIGNMENT,
                TestEnvironment.CONSTRAINT,
                null,
                new BusinessCaseGiven(parseParameters(preParas), List.of(), List.of()),
                BusinessCaseExpect.empty());
        return new RuleScenarioCase(businessCase, new RuleScenarioExpectation(
                "HAS_SOLUTION",
                expectedSolutionCount,
                safeMap(expectedConditionCounts),
                safeMap(expectedFirstParaValues),
                safeMap(expectedFirstParaHidden),
                safeMap(expectedFirstPartQuantities),
                List.of(),
                Map.of(),
                Map.of()));
    }

    protected RuleScenarioCase postRecommendCase(
            String id,
            List<String> requests,
            Integer expectedSolutionCount,
            List<String> expectedAllParaNonBlank,
            Map<String, Integer> expectedAllParaMinValues,
            Map<String, String> expectedAllParaValues,
            Map<String, Integer> expectedFirstPartQuantities) {
        BusinessRuleTestCase businessCase = businessCase(
                id,
                BusinessRuleFamily.ASSIGNMENT,
                TestEnvironment.NON_CONSTRAINT,
                "testPostAssignment",
                new BusinessCaseGiven(List.of(), List.of(), parsePartCategoryRequests(requests)),
                BusinessCaseExpect.empty());
        return new RuleScenarioCase(businessCase, new RuleScenarioExpectation(
                "HAS_SOLUTION",
                expectedSolutionCount,
                Map.of(),
                Map.of(),
                Map.of(),
                safeMap(expectedFirstPartQuantities),
                expectedAllParaNonBlank == null ? List.of() : List.copyOf(expectedAllParaNonBlank),
                safeMap(expectedAllParaMinValues),
                safeMap(expectedAllParaValues)));
    }

    protected void assertNaturalLanguageTranslatesAndExecutes(
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleScenarioCaseSet testCaseSet,
            String className) {
        assertNaturalLanguageTranslatesAndExecutes(
                metadata.normalNaturalCode(),
                context,
                scenario,
                metadata,
                testCaseSet,
                className);
    }

    protected void assertNaturalLanguageTranslatesAndExecutes(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleScenarioCaseSet testCaseSet,
            String className) {
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        RuleScenario effectiveScenario = scenario == null
                ? scenarioClassifier.classify(naturalLanguage, preparedContext)
                : scenario;
        RuleMetadata effectiveMetadata = metadata == null
                ? RuleMetadata.from(naturalLanguage, preparedContext, effectiveScenario)
                : metadata;
        BusinessRuleTestCaseSet businessCaseSet = toBusinessCaseSet(testCaseSet, effectiveScenario);

        String methodBody = null;
        CompilationResult lastCompilation = null;
        RuleUnitExecutionResult lastExecution = null;
        int attemptsLimit = 3;
        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            try {
                methodBody = generateAttemptMethodBody(
                        naturalLanguage,
                        preparedContext,
                        effectiveScenario,
                        attempt,
                        methodBody,
                        lastCompilation,
                        lastExecution);
            } catch (RuntimeException e) {
                lastCompilation = generationFailureResult(e);
                lastExecution = null;
                continue;
            }

            AssembledRuleClass compileUnit = assembler.assembleCompileUnit(
                    methodBody,
                    preparedContext,
                    effectiveScenario,
                    effectiveMetadata,
                    className + "GeneratedCompileUnit" + attempt);
            lastCompilation = compilationProcessor.compile(compileUnit);
            if (!lastCompilation.success()) {
                lastExecution = null;
                continue;
            }

            lastExecution = executionProcessor.execute(compileUnit, businessCaseSet);
            if (lastExecution.success()) {
                assertScenarioExpectations(testCaseSet, lastExecution);
                return;
            }
            if (lastExecution.failureKind() != RuleTransFailureKind.RULE_LOGIC_FAILED) {
                break;
            }
        }

        fail(buildNaturalLanguageFailureMessage(
                naturalLanguage, methodBody, lastCompilation, lastExecution));
    }

    private String generateAttemptMethodBody(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            int attempt,
            String previousMethodBody,
            CompilationResult lastCompilation,
            RuleUnitExecutionResult lastExecution) {
        if (attempt == 1 || previousMethodBody == null || previousMethodBody.isBlank()) {
            return snippetGenerator().generateMethodBody(naturalLanguage, context, scenario);
        }
        if (lastCompilation != null && !lastCompilation.success()) {
            return snippetGenerator().generateMethodBodyFromPrompt(
                    promptBuilder.buildCompilationCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastCompilation),
                    scenario.sdkProfile(),
                    context);
        }
        if (lastExecution != null && !lastExecution.success()) {
            return snippetGenerator().generateMethodBodyFromPrompt(
                    promptBuilder.buildTestCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastExecution.failures()),
                    scenario.sdkProfile(),
                    context);
        }
        return snippetGenerator().generateMethodBody(naturalLanguage, context, scenario);
    }

    private RuleContext prepareContext(String naturalLanguage, RuleContext context) {
        if (!context.isModuleLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        if (context.module().getAllPartCategorys().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = categoryIdentifier().identify(naturalLanguage, context.module());
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ModuleRuleContext(context.module(), categories);
    }

    private RuleSnippetGenerator snippetGenerator() {
        return new RuleSnippetGenerator(realLlmInvoker(), promptBuilder, postProcessor);
    }

    private CategoryIdentifier categoryIdentifier() {
        return new CategoryIdentifier(realLlmInvoker(), promptBuilder);
    }

    private BusinessRuleTestCaseSet toBusinessCaseSet(
            RuleScenarioCaseSet caseSet,
            RuleScenario scenario) {
        List<BusinessRuleTestCase> cases = caseSet.cases().stream()
                .map(ruleCase -> withScenarioDefaults(ruleCase.businessCase(), scenario))
                .toList();
        return new BusinessRuleTestCaseSet("", cases);
    }

    private BusinessRuleTestCase withScenarioDefaults(BusinessRuleTestCase testCase, RuleScenario scenario) {
        BusinessRuleFamily family = testCase.businessFamily();
        if (family == BusinessRuleFamily.ASSIGNMENT && scenario != null && scenario.family() == RuleFamily.PRIORITY) {
            family = BusinessRuleFamily.PRIORITY;
        }
        TestEnvironment environment = testCase.environment() == null
                ? TestEnvironment.CONSTRAINT
                : testCase.environment();
        String serviceMethod = testCase.serviceMethod();
        if (serviceMethod == null || serviceMethod.isBlank()) {
            serviceMethod = serviceMethod(family, environment);
        }
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

    private String serviceMethod(BusinessRuleFamily family, TestEnvironment environment) {
        if (environment == TestEnvironment.NON_CONSTRAINT) {
            return "testPostAssignment";
        }
        return switch (family) {
            case COMPATIBILITY -> "testCompatibility";
            case PRIORITY -> "testPriority";
            case ASSIGNMENT -> "testAssignment";
        };
    }

    private BusinessRuleTestCase businessCase(
            String id,
            BusinessRuleFamily family,
            TestEnvironment environment,
            String serviceMethod,
            BusinessCaseGiven given,
            BusinessCaseExpect expect) {
        return new BusinessRuleTestCase(id, id, family, id, environment, serviceMethod, given, expect, null);
    }

    private List<RuleUnitParameter> parseParameters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<RuleUnitParameter> parameters = new ArrayList<>();
        for (int i = 0; i + 1 < values.size(); i += 2) {
            parameters.add(new RuleUnitParameter(values.get(i), values.get(i + 1)));
        }
        return parameters;
    }

    private List<RuleUnitPartCategory> parsePartCategoryRequests(String... requests) {
        return parsePartCategoryRequests(requests == null ? List.of() : Arrays.asList(requests));
    }

    private List<RuleUnitPartCategory> parsePartCategoryRequests(List<String> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(this::parsePartCategoryRequest)
                .toList();
    }

    private RuleUnitPartCategory parsePartCategoryRequest(String request) {
        String text = request == null ? "" : request.trim();
        int colon = text.indexOf(':');
        if (colon < 0) {
            return new RuleUnitPartCategory(text, "Sum_Quantity", ">=", 1, Map.of());
        }
        String category = text.substring(0, colon).trim();
        String remainder = text.substring(colon + 1).trim();
        String condition = remainder;
        String whereText = "";
        int whereIndex = remainder.toLowerCase(Locale.ROOT).indexOf(" where ");
        if (whereIndex >= 0) {
            condition = remainder.substring(0, whereIndex).trim();
            whereText = remainder.substring(whereIndex + " where ".length()).trim();
        }
        ParsedCondition parsed = parseCondition(condition);
        return new RuleUnitPartCategory(category, parsed.aggregate(), parsed.operator(),
                parsed.value(), parseWhere(whereText));
    }

    private ParsedCondition parseCondition(String condition) {
        String text = condition == null ? "" : condition.trim();
        for (String operator : OPERATORS) {
            int index = text.indexOf(operator);
            if (index >= 0) {
                String aggregate = text.substring(0, index).trim();
                String value = text.substring(index + operator.length()).trim();
                return new ParsedCondition(defaultAggregate(aggregate), operator, parseScalar(value));
            }
        }
        if (text.isBlank()) {
            return new ParsedCondition("Sum_Quantity", ">=", 1);
        }
        return new ParsedCondition(defaultAggregate(text), ">=", 1);
    }

    private String defaultAggregate(String aggregate) {
        return aggregate == null || aggregate.isBlank() ? "Sum_Quantity" : aggregate;
    }

    private Object parseScalar(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private Map<String, String> parseWhere(String whereText) {
        if (whereText == null || whereText.isBlank()) {
            return Map.of();
        }
        Map<String, String> where = new LinkedHashMap<>();
        String normalized = whereText.replaceAll("(?i)\\s+and\\s+", ",");
        for (String clause : normalized.split("\\s*,\\s*")) {
            String[] parts = clause.split("=", 2);
            if (parts.length == 2 && !parts[0].isBlank()) {
                where.put(parts[0].trim(), parts[1].trim());
            }
        }
        return where;
    }

    private void assertScenarioExpectations(
            RuleScenarioCaseSet caseSet,
            RuleUnitExecutionResult executionResult) {
        assertNotNull(executionResult.report(), "RuleUnit report must not be null");
        Map<String, RuleScenarioExpectation> expectations = caseSet.cases().stream()
                .collect(Collectors.toMap(
                        ruleCase -> ruleCase.businessCase().id(),
                        RuleScenarioCase::expectation));
        for (RuleUnitTestReport report : executionResult.report().caseReports()) {
            RuleScenarioExpectation expectation = expectations.get(report.caseId());
            if (expectation != null) {
                expectation.assertMatches(report.caseId(), report.actual());
            }
        }
    }

    private CompilationResult generationFailureResult(RuntimeException e) {
        String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
        return new CompilationResult(false, -1, List.of(message), List.of(message), null, null);
    }

    private String buildNaturalLanguageFailureMessage(
            String naturalLanguage,
            String methodBody,
            CompilationResult compilationResult,
            RuleUnitExecutionResult executionResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("Natural-language RuleTrans scenario failed: ")
                .append(naturalLanguage)
                .append("\nGenerated method body:\n")
                .append(methodBody == null ? "<none>" : methodBody)
                .append("\n");
        if (compilationResult != null && !compilationResult.success()) {
            builder.append("Compilation errors:\n")
                    .append(String.join("\n", compilationResult.errors()))
                    .append("\nDiagnostics:\n")
                    .append(String.join("\n", compilationResult.diagnostics()))
                    .append("\n");
        }
        if (executionResult != null && !executionResult.success()) {
            builder.append("RuleUnit failures:\n")
                    .append(executionResult.failures())
                    .append("\nMessages:\n")
                    .append(String.join("\n", executionResult.messages()))
                    .append("\n");
        }
        return builder.toString();
    }

    private <T> Map<String, T> safeMap(Map<String, T> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    protected record RuleScenarioCaseSet(List<RuleScenarioCase> cases) {

        protected RuleScenarioCaseSet {
            cases = cases == null ? List.of() : List.copyOf(cases);
        }
    }

    protected record RuleScenarioCase(
            BusinessRuleTestCase businessCase,
            RuleScenarioExpectation expectation) {
    }

    private record ParsedCondition(String aggregate, String operator, Object value) {
    }

    protected record RuleScenarioExpectation(
            String expectedResult,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities,
            List<String> expectedAllParaNonBlank,
            Map<String, Integer> expectedAllParaMinValues,
            Map<String, String> expectedAllParaValues) {

        static RuleScenarioExpectation empty() {
            return new RuleScenarioExpectation(null, null, Map.of(), Map.of(), Map.of(), Map.of(),
                    List.of(), Map.of(), Map.of());
        }

        protected RuleScenarioExpectation {
            expectedConditionCounts = expectedConditionCounts == null ? Map.of() : Map.copyOf(expectedConditionCounts);
            expectedFirstParaValues = expectedFirstParaValues == null ? Map.of() : Map.copyOf(expectedFirstParaValues);
            expectedFirstParaHidden = expectedFirstParaHidden == null ? Map.of() : Map.copyOf(expectedFirstParaHidden);
            expectedFirstPartQuantities = expectedFirstPartQuantities == null
                    ? Map.of()
                    : Map.copyOf(expectedFirstPartQuantities);
            expectedAllParaNonBlank = expectedAllParaNonBlank == null
                    ? List.of()
                    : List.copyOf(expectedAllParaNonBlank);
            expectedAllParaMinValues = expectedAllParaMinValues == null ? Map.of() : Map.copyOf(expectedAllParaMinValues);
            expectedAllParaValues = expectedAllParaValues == null ? Map.of() : Map.copyOf(expectedAllParaValues);
        }

        void assertMatches(String caseId, RuleUnitActualResult actual) {
            if (expectedResult != null) {
                assertExpectedResult(caseId, actual);
            }
            if (expectedSolutionCount != null) {
                assertEquals(expectedSolutionCount, actual.solutions().size(),
                        "solution count for " + caseId);
            }
            for (Map.Entry<String, Integer> entry : expectedConditionCounts.entrySet()) {
                assertEquals(entry.getValue().longValue(), countSolutions(actual, entry.getKey()),
                        "condition count " + entry.getKey() + " for " + caseId);
            }
            if (!expectedFirstParaValues.isEmpty()
                    || !expectedFirstParaHidden.isEmpty()
                    || !expectedFirstPartQuantities.isEmpty()) {
                RuleUnitSolution first = firstSolution(caseId, actual);
                assertFirstParaValues(caseId, first);
                assertFirstParaHidden(caseId, first);
                assertFirstPartQuantities(caseId, first);
            }
            assertAllParaExpectations(caseId, actual);
        }

        private void assertExpectedResult(String caseId, RuleUnitActualResult actual) {
            String expected = expectedResult.trim().toUpperCase(Locale.ROOT);
            if ("NO_SOLUTION".equals(expected)) {
                assertTrue(actual.solutions().isEmpty(), "expected no solution for " + caseId + ": " + actual);
                return;
            }
            if ("HAS_SOLUTION".equals(expected) || "SUCCESS".equals(expected) || expected.contains("SOLUTION")) {
                assertFalse(actual.solutions().isEmpty(), "expected solution for " + caseId + ": " + actual);
            }
        }

        private long countSolutions(RuleUnitActualResult actual, String conditionText) {
            Predicate<RuleUnitSolution> predicate = solution -> matchesCondition(solution, conditionText);
            return actual.solutions().stream().filter(predicate).count();
        }

        private boolean matchesCondition(RuleUnitSolution solution, String conditionText) {
            if (conditionText == null || conditionText.isBlank()) {
                return true;
            }
            for (String token : conditionText.split("\\s*,\\s*")) {
                String[] parts = token.split(":", 2);
                if (parts.length != 2) {
                    return false;
                }
                RuleUnitParameter parameter = findParameter(solution, parts[0]);
                if (parameter == null || !parts[1].equals(parameter.value())) {
                    return false;
                }
            }
            return true;
        }

        private RuleUnitSolution firstSolution(String caseId, RuleUnitActualResult actual) {
            assertFalse(actual.solutions().isEmpty(), "expected first solution for " + caseId);
            return actual.solutions().get(0);
        }

        private void assertFirstParaValues(String caseId, RuleUnitSolution first) {
            for (Map.Entry<String, String> entry : expectedFirstParaValues.entrySet()) {
                RuleUnitParameter parameter = findParameter(first, entry.getKey());
                assertNotNull(parameter, "parameter " + entry.getKey() + " for " + caseId);
                assertEquals(entry.getValue(), parameter.value(),
                        "parameter value " + entry.getKey() + " for " + caseId);
            }
        }

        private void assertFirstParaHidden(String caseId, RuleUnitSolution first) {
            for (Map.Entry<String, Boolean> entry : expectedFirstParaHidden.entrySet()) {
                RuleUnitParameter parameter = findParameter(first, entry.getKey());
                assertNotNull(parameter, "parameter " + entry.getKey() + " for " + caseId);
                assertEquals(entry.getValue(), parameter.hidden(),
                        "parameter hidden " + entry.getKey() + " for " + caseId);
            }
        }

        private void assertFirstPartQuantities(String caseId, RuleUnitSolution first) {
            for (Map.Entry<String, Integer> entry : expectedFirstPartQuantities.entrySet()) {
                RuleUnitPart part = findPart(first.parts(), entry.getKey());
                assertNotNull(part, "part " + entry.getKey() + " for " + caseId);
                assertEquals(entry.getValue(), part.quantity(),
                        "part quantity " + entry.getKey() + " for " + caseId);
            }
        }

        private void assertAllParaExpectations(String caseId, RuleUnitActualResult actual) {
            for (RuleUnitSolution solution : actual.solutions()) {
                for (String paraCode : expectedAllParaNonBlank) {
                    RuleUnitParameter parameter = findParameter(solution, paraCode);
                    assertNotNull(parameter, "parameter " + paraCode + " for " + caseId);
                    assertTrue(parameter.value() != null && !parameter.value().isBlank(),
                            "expected non-blank parameter " + paraCode + " for " + caseId);
                }
                for (Map.Entry<String, Integer> entry : expectedAllParaMinValues.entrySet()) {
                    RuleUnitParameter parameter = findParameter(solution, entry.getKey());
                    assertNotNull(parameter, "parameter " + entry.getKey() + " for " + caseId);
                    assertTrue(Integer.parseInt(parameter.value()) >= entry.getValue(),
                            "expected minimum parameter " + entry.getKey() + " for " + caseId);
                }
                for (Map.Entry<String, String> entry : expectedAllParaValues.entrySet()) {
                    RuleUnitParameter parameter = findParameter(solution, entry.getKey());
                    assertNotNull(parameter, "parameter " + entry.getKey() + " for " + caseId);
                    assertEquals(entry.getValue(), parameter.value(),
                            "parameter value " + entry.getKey() + " for " + caseId);
                }
            }
        }

        private RuleUnitParameter findParameter(RuleUnitSolution solution, String code) {
            return solution.parameters().stream()
                    .filter(parameter -> code.equals(parameter.code()))
                    .findFirst()
                    .orElse(null);
        }

        private RuleUnitPart findPart(List<RuleUnitPart> parts, String code) {
            return parts.stream()
                    .filter(part -> code.equals(part.code()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
