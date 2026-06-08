package com.jmix.ruletrans.rulescenario;

import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.testgen.RuleTransTestCase;
import com.jmix.ruletrans.testgen.RuleTransTestCaseSet;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Shared scenario harness.
 *
 * <p>Business scenario assertions call the real LLM, translate the supplied
 * natural language into a rule method body, then compile and execute the
 * generated rule against SDK-level cases.</p>
 */
abstract class RuleScenarioHarnessSupport {

    static final String TEMP_RESOURCE_PATH = "target/ruletrans-rulescenario-resources";

    private final RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final RuleTransTempFileManager tempFileManager =
            new RuleTransTempFileManager(Path.of("target/ruletrans-rulescenario"));
    private final RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
    private final CompilationProcessor compilationProcessor = new CompilationProcessor(tempFileManager);
    private final TestExecutionProcessor testExecutionProcessor = new TestExecutionProcessor(tempFileManager);
    private final RuleScenarioClassifier scenarioClassifier = new RuleScenarioClassifier();

    protected ProductRuleContext productContext(Class<?> algClass) {
        return RuleContextFactory.fromAnnotatedClass(algClass, TEMP_RESOURCE_PATH);
    }

    protected ProductRuleContext productContext(Class<?> algClass, String... categoryCodes) {
        ProductRuleContext context = productContext(algClass);
        if (categoryCodes == null || categoryCodes.length == 0) {
            return context;
        }
        return RuleContextFactory.product(context.module(), Arrays.asList(categoryCodes));
    }

    protected PartCategoryRuleContext partCategoryContext(Class<?> algClass, String categoryCode) {
        return RuleContextFactory.partCategory(productContext(algClass).module(), categoryCode);
    }

    protected RuleMetadata metadata(
            String ruleCode,
            String naturalLanguage,
            String fatherCode,
            String attrParaCodes) {
        return new RuleMetadata(ruleCode, ruleCode, naturalLanguage, fatherCode, attrParaCodes, "", "");
    }

    protected RuleTransTestCaseSet caseSet(RuleTransTestCase... cases) {
        return new RuleTransTestCaseSet("", List.of(cases));
    }

    protected RuleTransTestCase validateCase(String id, boolean expectedValid, String... selectedParts) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_VALIDATE,
                Arrays.asList(selectedParts),
                expectedValid,
                null,
                null,
                null);
    }

    protected RuleTransTestCase recommendCase(String id, String expectedResult, String... requests) {
        return recommendCase(id, expectedResult, null, null, requests);
    }

    protected RuleTransTestCase recommendCase(
            String id,
            String expectedResult,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedFirstPartQuantities,
            String... requests) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_RECOMMEND,
                null,
                null,
                null,
                Arrays.asList(requests),
                expectedResult,
                null,
                null,
                null,
                expectedSolutionCount,
                null,
                null,
                null,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase inferPartCase(
            String id,
            String partCode,
            int quantity,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_INFER_PART,
                null,
                null,
                null,
                null,
                null,
                partCode,
                quantity,
                preParas,
                expectedSolutionCount,
                expectedConditionCounts,
                expectedFirstParaValues,
                expectedFirstParaHidden,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase inferParaCase(
            String id,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_INFER_PARA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                preParas,
                expectedSolutionCount,
                expectedConditionCounts,
                expectedFirstParaValues,
                expectedFirstParaHidden,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase postRecommendCase(
            String id,
            List<String> requests,
            Integer expectedSolutionCount,
            List<String> expectedAllParaNonBlank,
            Map<String, Integer> expectedAllParaMinValues,
            Map<String, String> expectedAllParaValues,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_POST_RECOMMEND,
                null,
                null,
                null,
                requests,
                null,
                null,
                null,
                null,
                expectedSolutionCount,
                null,
                null,
                null,
                expectedFirstPartQuantities,
                expectedAllParaNonBlank,
                expectedAllParaMinValues,
                expectedAllParaValues);
    }

    protected void assertNaturalLanguageTranslatesAndExecutes(
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleTransTestCaseSet testCaseSet,
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
            RuleTransTestCaseSet testCaseSet,
            String className) {
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        RuleScenario effectiveScenario = scenario == null
                ? scenarioClassifier.classify(naturalLanguage, preparedContext)
                : scenario;
        RuleMetadata effectiveMetadata = metadata == null
                ? RuleMetadata.from(naturalLanguage, preparedContext, effectiveScenario)
                : metadata;

        String methodBody = null;
        CompilationResult lastCompilation = null;
        TestExecutionResult lastTestResult = null;
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
                        lastTestResult);
            } catch (RuntimeException e) {
                if (methodBody == null || methodBody.isBlank()) {
                    lastCompilation = generationFailureResult(e);
                    lastTestResult = null;
                } else if (lastTestResult != null && !lastTestResult.success()) {
                    lastCompilation = null;
                } else if (lastCompilation == null || lastCompilation.success()) {
                    lastCompilation = generationFailureResult(e);
                    lastTestResult = null;
                }
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
                lastTestResult = null;
                continue;
            }

            AssembledRuleClass executableTest = assembler.assembleExecutableTest(
                    methodBody,
                    preparedContext,
                    effectiveScenario,
                    effectiveMetadata,
                    testCaseSet,
                    className + "Generated" + attempt);
            CompilationResult testCompileResult = compilationProcessor.compile(executableTest);
            if (!testCompileResult.success()) {
                lastCompilation = testCompileResult;
                lastTestResult = null;
                continue;
            }

            lastTestResult = testExecutionProcessor.execute(executableTest);
            if (lastTestResult.success() && lastTestResult.testsSucceeded() > 0) {
                return;
            }
        }

        fail(buildNaturalLanguageFailureMessage(
                naturalLanguage, methodBody, lastCompilation, lastTestResult));
    }

    private String generateAttemptMethodBody(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            int attempt,
            String previousMethodBody,
            CompilationResult lastCompilation,
            TestExecutionResult lastTestResult) {
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
        if (lastTestResult != null && !lastTestResult.success()) {
            return snippetGenerator().generateMethodBodyFromPrompt(
                    promptBuilder.buildTestCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastTestResult.failedCases()),
                    scenario.sdkProfile(),
                    context);
        }
        return snippetGenerator().generateMethodBody(naturalLanguage, context, scenario);
    }

    private RuleContext prepareContext(String naturalLanguage, RuleContext context) {
        if (!context.isProductLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        if (context.module().getAllPartCategorys().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = categoryIdentifier().identify(naturalLanguage, context.module());
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ProductRuleContext(context.module(), categories);
    }

    private RuleSnippetGenerator snippetGenerator() {
        return new RuleSnippetGenerator(realLlmInvoker(), promptBuilder, postProcessor);
    }

    private CategoryIdentifier categoryIdentifier() {
        return new CategoryIdentifier(realLlmInvoker(), promptBuilder);
    }

    private CompilationResult generationFailureResult(RuntimeException e) {
        String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
        return new CompilationResult(false, -1, List.of(message), List.of(message), null, null);
    }

    private String buildNaturalLanguageFailureMessage(
            String naturalLanguage,
            String methodBody,
            CompilationResult compilationResult,
            TestExecutionResult testExecutionResult) {
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
        if (testExecutionResult != null && !testExecutionResult.success()) {
            builder.append("Failed cases:\n")
                    .append(testExecutionResult.failedCases())
                    .append("\n");
        }
        return builder.toString();
    }
}
