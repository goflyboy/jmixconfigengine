package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.cpu4CannotUseDrive5400MethodBody;
import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.RuleUnitCaseExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class RuleTransPipelineTest {

    @Test
    public void testNaturalLanguageToRuleUnitReportSucceeds() {
        RuleTransPipelineResult result = pipeline(
                cpu4CannotUseDrive5400MethodBody(),
                incompatibleCaseJson()).execute(request(1));

        assertTrue(result.success(), result.messages().toString());
        assertTrue(result.compilationResult().success(), result.compilationResult().errors().toString());
        assertNotNull(result.businessCaseSet());
        assertTrue(result.ruleUnitReport().passed(), result.ruleUnitReport().caseReports().toString());
        assertEquals(RuleTransFailureKind.NONE, result.failureKind());
    }

    @Test
    public void testCompilationFailureTriggersCodeRetry() {
        RuleTransPipelineResult result = pipeline(
                "missingSymbol();",
                cpu4CannotUseDrive5400MethodBody(),
                incompatibleCaseJson()).execute(request(1));

        assertTrue(result.success(), result.messages().toString());
        assertEquals(2, result.attempts());
        assertTrue(result.attemptStates().stream()
                .anyMatch(state -> state.failureKind() == RuleTransFailureKind.COMPILATION_FAILED));
    }

    @Test
    public void testBusinessCaseSchemaFailureRegeneratesCasesWithoutChangingMethodBody() {
        RuleTransPipelineResult result = pipeline(
                cpu4CannotUseDrive5400MethodBody(),
                unsupportedServiceMethodCaseJson(),
                incompatibleCaseJson()).execute(request(1));

        assertTrue(result.success(), result.messages().toString());
        assertEquals(2, result.attempts());
        assertTrue(result.attemptStates().stream()
                .anyMatch(state -> state.failureKind() == RuleTransFailureKind.BUSINESS_CASE_SCHEMA_INVALID));
        assertEquals(cpu4CannotUseDrive5400MethodBody().trim(), result.methodBody().trim());
    }

    @Test
    public void testRuleUnitLogicFailureTriggersCodeRetry() {
        RuleTransPipelineResult result = pipeline(
                "int ignored = 0;",
                incompatibleCaseJson(),
                cpu4CannotUseDrive5400MethodBody()).execute(request(1));

        assertTrue(result.success(), result.messages().toString());
        assertEquals(2, result.attempts());
        assertTrue(result.attemptStates().stream()
                .anyMatch(state -> state.failureKind() == RuleTransFailureKind.RULE_LOGIC_FAILED));
        assertEquals(1, result.businessCaseSet().cases().size());
    }

    @Test
    public void testInvalidRequestReturnsFailureResult() {
        RuleTransPipelineResult result = pipeline().execute(new RuleTransRequest(
                "",
                RuleContextFactory.module(sampleModule(), List.of("cpu", "drive")),
                0,
                RuleTransPipelineOptions.defaults()));

        assertNotEquals(RuleTransFailureKind.NONE, result.failureKind());
        assertSame(RuleTransFailureKind.INVALID_REQUEST, result.failureKind());
    }

    private RuleTransRequest request(int maxRetries) {
        return new RuleTransRequest(
                "cpu CoreNum=4 cannot be selected together with drive Speed=5400",
                RuleContextFactory.module(sampleModule(), List.of("cpu", "drive")),
                maxRetries,
                RuleTransPipelineOptions.defaults());
    }

    private RuleTransPipeline pipeline(String... responses) {
        PromptBuilder promptBuilder = new PromptBuilder();
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-pipeline"));
        QueueInvoker invoker = new QueueInvoker(responses);
        return new RuleTransPipeline(
                new CategoryIdentifier(invoker, promptBuilder),
                new RuleSnippetGenerator(invoker, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(invoker, promptBuilder),
                new RuleUnitCaseExecutionProcessor(tempFileManager),
                promptBuilder);
    }

    private String incompatibleCaseJson() {
        return """
                {
                  "cases": [
                    {
                      "id": "COMP-PART-001",
                      "businessFamily": "COMPATIBILITY",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testCompatibility",
                      "given": {
                        "parts": [
                          {"code": "cpu4", "isSelected": true},
                          {"code": "drive5400", "isSelected": true}
                        ]
                      },
                      "expect": {"compatible": false}
                    }
                  ]
                }
                """;
    }

    private String unsupportedServiceMethodCaseJson() {
        return """
                {
                  "cases": [
                    {
                      "id": "COMP-PART-001",
                      "businessFamily": "COMPATIBILITY",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testUnknown",
                      "given": {
                        "parts": [
                          {"code": "cpu4", "isSelected": true},
                          {"code": "drive5400", "isSelected": true}
                        ]
                      },
                      "expect": {"compatible": false}
                    }
                  ]
                }
                """;
    }

    private static final class QueueInvoker implements LLMInvoker {

        private final Deque<String> responses = new ArrayDeque<>();

        private QueueInvoker(String... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public String generate(String systemMessage, String userMessage) {
            if (responses.isEmpty()) {
                throw new IllegalStateException("No fake LLM response left for prompt: " + userMessage);
            }
            return responses.removeFirst();
        }

        @Override
        public String getConfigInfo() {
            return "queue";
        }
    }
}
