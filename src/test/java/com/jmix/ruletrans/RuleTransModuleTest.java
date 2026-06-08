package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * RFC-0011 RuleTrans engine tests.
 */
public class RuleTransModuleTest {

    @Test
    public void testPartCategoryRuleGenerationReturnsPureMethodBody() {
        RuleTransEngine engine = engine(invoker(cpuAtMostOneMethodBody()));

        String methodBody = engine.translate("CPU at most one", RuleContextFactory.partCategory(sampleModule(), "cpu"));

        assertTrue(methodBody.contains("cpu"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
        assertFalse(methodBody.contains("package "), methodBody);
        assertFalse(methodBody.contains("class "), methodBody);
    }

    @Test
    public void testProductTranslateUsesIdentifiedCategories() {
        ProductRuleContext context = RuleContextFactory.product(sampleModule());

        String methodBody = engine(invoker("[\"cpu\", \"drive\"]", cpuDriveIncompatibleBody()))
                .translate("4-core CPU cannot use 5400 drive", context);

        assertTrue(methodBody.contains("cpu"), methodBody);
        assertTrue(methodBody.contains("drive"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
        assertFalse(methodBody.contains("package "), methodBody);
        assertFalse(methodBody.contains("class "), methodBody);
    }

    @Test
    public void testPartCategoryTranslateWithRetryCompilesMethodBody() {
        RuleTransResult result = engine(invoker(cpuAtMostOneMethodBody())).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertTrue(result.compilationResult().success(), String.join("\n", result.compilationResult().errors()));
        assertTrue(result.methodBody().contains("model().addLessOrEqual"), result.methodBody());
        assertFalse(result.methodBody().contains("@CodeRuleAnno"), result.methodBody());
    }

    @Test
    public void testPartCategoryEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(invoker(
                cpuAtMostOneMethodBody(),
                cpuAtMostOneTestCases())).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testProductEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(invoker(
                "[\"cpu\", \"drive\"]",
                cpuDriveIncompatibleBody(),
                cpuDriveIncompatibleTestCases())).translateWithRetry(
                "4-core CPU cannot use 5400 drive",
                RuleContextFactory.product(sampleModule()),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testPartCategoryGeneratedTestCasesPassWithRetryBudget() {
        RuleTransResult result = engineWithGeneratedCases(invoker(
                cpuAtMostOneMethodBody(),
                cpuAtMostOneTestCases())).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().success());
    }

    @Test
    public void testBoundaryInputs() {
        RuleTransEngine engine = engine(invoker(cpuAtMostOneMethodBody()));

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("CPU at most one", null));
    }

    private RuleTransEngine engine(LLMInvoker llmInvoker) {
        return engine(llmInvoker, false);
    }

    private RuleTransEngine engineWithGeneratedCases(LLMInvoker llmInvoker) {
        return engine(llmInvoker, true);
    }

    private RuleTransEngine engine(LLMInvoker llmInvoker, boolean generateCases) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        return new RuleTransEngine(
                new CategoryIdentifier(llmInvoker, promptBuilder),
                new RuleSnippetGenerator(llmInvoker, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(generateCases ? llmInvoker : null, promptBuilder),
                new TestExecutionProcessor(tempFileManager),
                promptBuilder);
    }

    private String cpuAtMostOneMethodBody() {
        return RuleTransTestFixtures.cpuAtMostOneMethodBody();
    }

    private String cpuDriveIncompatibleBody() {
        return RuleTransTestFixtures.cpu4CannotUseDrive5400MethodBody();
    }

    private String cpuAtMostOneTestCases() {
        return """
                {
                  "cases": [
                    {
                      "id": "twoCpuInvalid",
                      "type": "validate",
                      "selectedParts": ["cpu4", "cpu8", "drive5400"],
                      "expectedValid": false
                    },
                    {
                      "id": "oneCpuValid",
                      "type": "validate",
                      "selectedParts": ["cpu4", "drive5400"],
                      "expectedValid": true
                    }
                  ]
                }
                """;
    }

    private String cpuDriveIncompatibleTestCases() {
        return """
                {
                  "cases": [
                    {
                      "id": "cpu4Drive5400Invalid",
                      "type": "validate",
                      "selectedParts": ["cpu4", "drive5400"],
                      "expectedValid": false
                    },
                    {
                      "id": "cpu8Drive7200Valid",
                      "type": "validate",
                      "selectedParts": ["cpu8", "drive7200"],
                      "expectedValid": true
                    }
                  ]
                }
                """;
    }

    private LLMInvoker invoker(String... responses) {
        return new ScriptedLlmInvoker(responses);
    }

    private static final class ScriptedLlmInvoker implements LLMInvoker {
        private final Queue<String> responses;

        private ScriptedLlmInvoker(String... responses) {
            this.responses = new ArrayDeque<>(Arrays.asList(responses));
        }

        @Override
        public String generate(String systemMessage, String userMessage) {
            if (responses.isEmpty()) {
                throw new IllegalStateException("No scripted LLM response left for prompt: " + userMessage);
            }
            return responses.remove();
        }

        @Override
        public String getConfigInfo() {
            return "scripted";
        }
    }
}
