package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * System-level RuleTrans tests that keep each test method close to user input and expected Java output.
 */
public class RuleTransPipelineSystemTest extends RuleTransPipelineTestBase {

    @Test
    public void testPartCategoryCompileOnlyPrintsPipelineDiagnostics() {
        RuleTransPipelineRunResult result = runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                "cpu",
                "CPU最多只能配一个",
                RuleTransPipelineOptions.compileOnly());

        expectJavaContains("addLessOrEqual").assertMatches(result.methodBody());
        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertFalse(result.diagnostics().llmCalls().isEmpty());
        assertTrue(result.pipelineResult().businessCaseSet().isEmpty());
        assertNull(result.pipelineResult().ruleUnitReport());
    }

    @Test
    public void testCompileOnlyPrintsSkippedBusinessCasesAndRuleUnit() {
        RuleTransPipelineRunResult result = runWithFakeLlm(
                "CPU最多只能配一个",
                "cpu",
                RuleTransPipelineOptions.compileOnly(),
                RuleTransTestFixtures.cpuAtMostOneMethodBody());

        expectJavaContains("addLessOrEqual").assertMatches(result.methodBody());
        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertTrue(result.pipelineResult().businessCaseSet().isEmpty());
        assertNull(result.pipelineResult().ruleUnitReport());
        assertTrue(result.diagnostics().llmCalls().stream()
                .anyMatch(call -> "RULE_GENERATION".equals(call.stage())));
    }

    @Test
    public void testModuleLevelRuleUsesCategoryIdentificationAndRuleUnit() {
        RuleTransPipelineRunResult result = runWithFakeLlm(
                "四核 CPU 不能兼容转速为 5400 转的硬盘",
                null,
                RuleTransPipelineOptions.defaults(),
                """
                        ["cpu","drive"]
                        """,
                RuleTransTestFixtures.cpu4CannotUseDrive5400MethodBody(),
                incompatibleCaseJson());

        expectJavaContains("inCompatible").assertMatches(result.methodBody());
        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertTrue(result.methodBody().contains("cpu"), result.methodBody());
        assertTrue(result.methodBody().contains("drive"), result.methodBody());
        assertNotNull(result.pipelineResult().ruleUnitReport());
        assertTrue(result.pipelineResult().ruleUnitReport().passed());
        assertTrue(result.diagnostics().llmCalls().stream()
                .anyMatch(call -> "CATEGORY_IDENTIFICATION".equals(call.stage())));
    }

    @Test
    public void testCompilationFailureRetryIsPrinted() {
        RuleTransPipelineRunResult result = runWithFakeLlm(
                "CPU最多只能配一个",
                "cpu",
                RuleTransPipelineOptions.compileOnly(),
                "missingSymbol();",
                RuleTransTestFixtures.cpuAtMostOneMethodBody());

        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertTrue(result.pipelineResult().attemptStates().stream()
                .anyMatch(state -> state.failureKind() == RuleTransFailureKind.COMPILATION_FAILED));
        assertTrue(result.diagnostics().llmCalls().stream()
                .anyMatch(call -> "COMPILATION_CORRECTION".equals(call.stage())));
    }

    private RuleTransPipelineRunResult runWithFakeLlm(
            String naturalLanguage,
            String categoryCode,
            RuleTransPipelineOptions options,
            String... responses) {
        return new RuleTransPipelineTestBase() {

            @Override
            protected LLMInvoker llmInvoker() {
                return new QueueInvoker(responses);
            }
        }.runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                categoryCode,
                naturalLanguage,
                options);
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
