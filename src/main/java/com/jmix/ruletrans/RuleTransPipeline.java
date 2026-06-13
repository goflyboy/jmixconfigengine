package com.jmix.ruletrans;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.RuleUnitCaseExecutionProcessor;
import com.jmix.ruletrans.postprocessor.RuleUnitExecutionResult;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates natural-language rules into compiled RuleUnit execution reports.
 */
@Slf4j
public final class RuleTransPipeline {

    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleSnippetAssembler assembler;
    private final CompilationProcessor compilationProcessor;
    private final RuleTestCaseGenerator testCaseGenerator;
    private final RuleUnitCaseExecutionProcessor executionProcessor;
    private final PromptBuilder promptBuilder;
    private final RuleScenarioClassifier scenarioClassifier;

    public RuleTransPipeline(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            RuleSnippetAssembler assembler,
            CompilationProcessor compilationProcessor,
            RuleTestCaseGenerator testCaseGenerator,
            RuleUnitCaseExecutionProcessor executionProcessor,
            PromptBuilder promptBuilder) {
        this(identifier, generator, assembler, compilationProcessor, testCaseGenerator,
                executionProcessor, promptBuilder, new RuleScenarioClassifier());
    }

    public RuleTransPipeline(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            RuleSnippetAssembler assembler,
            CompilationProcessor compilationProcessor,
            RuleTestCaseGenerator testCaseGenerator,
            RuleUnitCaseExecutionProcessor executionProcessor,
            PromptBuilder promptBuilder,
            RuleScenarioClassifier scenarioClassifier) {
        this.identifier = identifier;
        this.generator = generator;
        this.assembler = assembler;
        this.compilationProcessor = compilationProcessor;
        this.testCaseGenerator = testCaseGenerator;
        this.executionProcessor = executionProcessor;
        this.promptBuilder = promptBuilder;
        this.scenarioClassifier = scenarioClassifier == null ? new RuleScenarioClassifier() : scenarioClassifier;
    }

    public RuleTransPipelineResult execute(RuleTransRequest request) {
        RequestState requestState = prepareRequest(request);
        if (requestState.failureKind() != RuleTransFailureKind.NONE) {
            return failureResult(requestState.failureKind(), requestState.messages());
        }

        RuleTransPipelineOptions options = request.options();
        int attemptsLimit = Math.max(1, Math.max(0, request.maxRetries()) + 1);
        List<String> messages = new ArrayList<>();
        List<RuleTransAttemptState> attemptStates = new ArrayList<>();
        String methodBody = null;
        AssembledRuleClass assembled = null;
        CompilationResult compilationResult = null;
        BusinessRuleTestCaseSet businessCaseSet = null;
        RuleUnitExecutionResult executionResult = null;
        RuleTransFailureKind lastFailureKind = RuleTransFailureKind.NONE;

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            if (shouldGenerateMethod(attempt, lastFailureKind, methodBody)) {
                try {
                    methodBody = generateAttemptMethodBody(
                            request,
                            requestState.context(),
                            requestState.scenario(),
                            attempt,
                            methodBody,
                            compilationResult,
                            executionResult,
                            lastFailureKind);
                } catch (RuntimeException e) {
                    lastFailureKind = RuleTransFailureKind.CODE_GENERATION_FAILED;
                    addFailureState(attemptStates, attempt, methodBody, compilationResult,
                            businessCaseSet, null, lastFailureKind);
                    messages.add(attemptMessage(attempt, lastFailureKind, messageOf(e)));
                    continue;
                }

                assembled = assembler.assembleCompileUnit(
                        methodBody,
                        requestState.context(),
                        requestState.scenario(),
                        requestState.metadata(),
                        "RuleTransCandidate" + String.format("%03d", attempt) + "Constraint");
                compilationResult = compilationProcessor.compile(assembled);
                if (!compilationResult.success()) {
                    lastFailureKind = RuleTransFailureKind.COMPILATION_FAILED;
                    addFailureState(attemptStates, attempt, methodBody, compilationResult,
                            businessCaseSet, null, lastFailureKind);
                    messages.add(attemptMessage(attempt, lastFailureKind,
                            String.join("\n", compilationResult.errors())));
                    continue;
                }
            }

            if (!options.generateBusinessCases()) {
                return successResult(attempt, requestState, methodBody, compilationResult,
                        BusinessRuleTestCaseSet.empty(), null, messages, attemptStates);
            }

            if (businessCaseSet == null || shouldRegenerateBusinessCases(lastFailureKind)) {
                try {
                    businessCaseSet = generateBusinessCases(
                            request, requestState.context(), requestState.scenario(), methodBody);
                } catch (RuntimeException e) {
                    lastFailureKind = classifyBusinessCaseFailure(e);
                    addFailureState(attemptStates, attempt, methodBody, compilationResult,
                            businessCaseSet, null, lastFailureKind);
                    messages.add(attemptMessage(attempt, lastFailureKind, messageOf(e)));
                    continue;
                }
            }

            if (businessCaseSet.isEmpty()) {
                if (options.allowEmptyBusinessCases()) {
                    return successResult(attempt, requestState, methodBody, compilationResult,
                            businessCaseSet, null, messages, attemptStates);
                }
                lastFailureKind = RuleTransFailureKind.BUSINESS_CASE_GENERATION_FAILED;
                addFailureState(attemptStates, attempt, methodBody, compilationResult,
                        businessCaseSet, null, lastFailureKind);
                messages.add(attemptMessage(attempt, lastFailureKind, "business case set is empty"));
                continue;
            }

            if (!options.executeBusinessCases()) {
                return successResult(attempt, requestState, methodBody, compilationResult,
                        businessCaseSet, null, messages, attemptStates);
            }

            executionResult = executionProcessor.execute(assembled, businessCaseSet);
            if (executionResult.success()) {
                return successResult(attempt, requestState, methodBody, compilationResult,
                        businessCaseSet, executionResult.report(), messages, attemptStates);
            }

            lastFailureKind = executionResult.failureKind();
            addFailureState(attemptStates, attempt, methodBody, compilationResult,
                    businessCaseSet, executionResult.report(), lastFailureKind);
            messages.add(attemptMessage(attempt, lastFailureKind, String.join("\n", executionResult.messages())));
            if (!isRetryable(lastFailureKind)) {
                return failureResult(attempt, requestState, methodBody, compilationResult,
                        businessCaseSet, executionResult.report(), lastFailureKind, messages, attemptStates);
            }
        }

        messages.add("Retry exhausted after " + attemptsLimit + " attempts, lastFailure=" + lastFailureKind);
        RuleUnitTestCaseSetReport report = executionResult == null ? null : executionResult.report();
        return failureResult(attemptsLimit, requestState, methodBody, compilationResult, businessCaseSet, report,
                RuleTransFailureKind.RETRY_EXHAUSTED, messages, attemptStates);
    }

    private RequestState prepareRequest(RuleTransRequest request) {
        if (request == null) {
            return RequestState.failed(RuleTransFailureKind.INVALID_REQUEST, "request must not be null");
        }
        if (request.naturalLanguage() == null || request.naturalLanguage().trim().isEmpty()) {
            return RequestState.failed(RuleTransFailureKind.INVALID_REQUEST, "naturalLanguage must not be blank");
        }
        if (request.context() == null) {
            return RequestState.failed(RuleTransFailureKind.INVALID_REQUEST, "context must not be null");
        }
        try {
            RuleContext preparedContext = prepareContext(request.naturalLanguage(), request.context());
            RuleScenario scenario = scenarioClassifier.classify(request.naturalLanguage(), preparedContext);
            RuleMetadata metadata = RuleMetadata.from(request.naturalLanguage(), preparedContext, scenario);
            return new RequestState(preparedContext, scenario, metadata, RuleTransFailureKind.NONE, List.of());
        } catch (CategoryNotFoundException e) {
            return RequestState.failed(RuleTransFailureKind.CATEGORY_IDENTIFICATION_FAILED, messageOf(e));
        } catch (RuntimeException e) {
            log.error("Failed to prepare request context for naturalLanguage=[{}], module=[{}]",
                    request.naturalLanguage(), request.context() != null ? request.context().module().getCode() : "null", e);
            return RequestState.failed(RuleTransFailureKind.CATEGORY_IDENTIFICATION_FAILED, messageOf(e));
        }
    }

    private RuleContext prepareContext(String naturalLanguage, RuleContext context) {
        if (!context.isModuleLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = identifier.identify(naturalLanguage, context.module());
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ModuleRuleContext(context.module(), categories);
    }

    private String generateAttemptMethodBody(
            RuleTransRequest request,
            RuleContext context,
            RuleScenario scenario,
            int attempt,
            String previousMethodBody,
            CompilationResult lastCompilation,
            RuleUnitExecutionResult lastExecution,
            RuleTransFailureKind lastFailureKind) {
        if (attempt == 1 || previousMethodBody == null || previousMethodBody.isBlank()) {
            return generator.generateMethodBody(request.naturalLanguage(), context, scenario);
        }
        if (lastFailureKind == RuleTransFailureKind.COMPILATION_FAILED) {
            return generator.generateMethodBodyFromPrompt(
                    promptBuilder.buildCompilationCorrectionPrompt(
                            request.naturalLanguage(), context, scenario, previousMethodBody, lastCompilation),
                    scenario.sdkProfile(),
                    context,
                    request.naturalLanguage());
        }
        if (lastFailureKind == RuleTransFailureKind.RULE_LOGIC_FAILED && lastExecution != null) {
            return generator.generateMethodBodyFromPrompt(
                    promptBuilder.buildTestCorrectionPrompt(
                            request.naturalLanguage(), context, scenario, previousMethodBody,
                            lastExecution.failures()),
                    scenario.sdkProfile(),
                    context,
                    request.naturalLanguage());
        }
        return generator.generateMethodBody(request.naturalLanguage(), context, scenario);
    }

    private BusinessRuleTestCaseSet generateBusinessCases(
            RuleTransRequest request,
            RuleContext context,
            RuleScenario scenario,
            String methodBody) {
        if (testCaseGenerator == null) {
            return BusinessRuleTestCaseSet.empty();
        }
        return testCaseGenerator.generateBusinessCases(
                request.naturalLanguage(), context, scenario, methodBody);
    }

    private boolean shouldGenerateMethod(int attempt, RuleTransFailureKind lastFailureKind, String methodBody) {
        if (attempt == 1 || methodBody == null || methodBody.isBlank()) {
            return true;
        }
        return lastFailureKind == RuleTransFailureKind.COMPILATION_FAILED
                || lastFailureKind == RuleTransFailureKind.RULE_LOGIC_FAILED
                || lastFailureKind == RuleTransFailureKind.CODE_GENERATION_FAILED;
    }

    private boolean shouldRegenerateBusinessCases(RuleTransFailureKind lastFailureKind) {
        return lastFailureKind == RuleTransFailureKind.BUSINESS_CASE_GENERATION_FAILED
                || lastFailureKind == RuleTransFailureKind.BUSINESS_CASE_SCHEMA_INVALID;
    }

    private boolean isRetryable(RuleTransFailureKind kind) {
        return kind == RuleTransFailureKind.COMPILATION_FAILED
                || kind == RuleTransFailureKind.BUSINESS_CASE_GENERATION_FAILED
                || kind == RuleTransFailureKind.BUSINESS_CASE_SCHEMA_INVALID
                || kind == RuleTransFailureKind.RULE_LOGIC_FAILED;
    }

    private RuleTransFailureKind classifyBusinessCaseFailure(RuntimeException e) {
        String message = messageOf(e);
        if (message.contains("Unsupported serviceMethod")
                || message.contains("serviceMethod must not be blank")
                || message.contains("Unsupported aggregate type")) {
            return RuleTransFailureKind.BUSINESS_CASE_SCHEMA_INVALID;
        }
        return RuleTransFailureKind.BUSINESS_CASE_GENERATION_FAILED;
    }

    private RuleTransPipelineResult successResult(
            int attempts,
            RequestState requestState,
            String methodBody,
            CompilationResult compilationResult,
            BusinessRuleTestCaseSet businessCaseSet,
            RuleUnitTestCaseSetReport report,
            List<String> messages,
            List<RuleTransAttemptState> attemptStates) {
        return new RuleTransPipelineResult(true, methodBody, attempts, requestState.scenario(),
                requestState.metadata(), compilationResult, businessCaseSet, report, RuleTransFailureKind.NONE,
                messages, attemptStates);
    }

    private RuleTransPipelineResult failureResult(
            RuleTransFailureKind failureKind,
            List<String> messages) {
        return new RuleTransPipelineResult(false, null, 0, null, null, null,
                BusinessRuleTestCaseSet.empty(), null, failureKind, messages, List.of());
    }

    private RuleTransPipelineResult failureResult(
            int attempts,
            RequestState requestState,
            String methodBody,
            CompilationResult compilationResult,
            BusinessRuleTestCaseSet businessCaseSet,
            RuleUnitTestCaseSetReport report,
            RuleTransFailureKind failureKind,
            List<String> messages,
            List<RuleTransAttemptState> attemptStates) {
        return new RuleTransPipelineResult(false, methodBody, attempts, requestState.scenario(),
                requestState.metadata(), compilationResult, businessCaseSet, report, failureKind,
                messages, attemptStates);
    }

    private void addFailureState(
            List<RuleTransAttemptState> attemptStates,
            int attempt,
            String methodBody,
            CompilationResult compilationResult,
            BusinessRuleTestCaseSet businessCaseSet,
            RuleUnitTestCaseSetReport report,
            RuleTransFailureKind failureKind) {
        attemptStates.add(new RuleTransAttemptState(
                attempt, methodBody, compilationResult, businessCaseSet, report, failureKind));
    }

    private String attemptMessage(int attempt, RuleTransFailureKind failureKind, String message) {
        return "Attempt " + attempt + " " + failureKind + ": " + message;
    }

    private String messageOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }

    private record RequestState(
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleTransFailureKind failureKind,
            List<String> messages) {

        private static RequestState failed(RuleTransFailureKind failureKind, String message) {
            return new RequestState(null, null, null, failureKind, List.of(message));
        }
    }
}
