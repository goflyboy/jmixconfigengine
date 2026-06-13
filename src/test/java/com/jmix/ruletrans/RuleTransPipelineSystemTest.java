package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.tool.impl.llm.FileLLMCacheStore;
import com.jmix.tool.impl.llm.LlmCacheEntry;
import com.jmix.tool.impl.llm.LlmCacheKeyBuilder;
import com.jmix.tool.impl.llm.LlmCacheStatus;
import com.jmix.tool.impl.llm.LlmModelProfile;
import com.jmix.tool.impl.llm.LlmModelRegistry;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * System-level RuleTrans tests backed by local LLM cache fixtures.
 */
public class RuleTransPipelineSystemTest extends RuleTransPipelineTestBase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CATEGORY_CPU = "cpu";
    private static final String CPU_AT_MOST_ONE = "CPU can be selected at most once";
    private static final String CPU_DRIVE_INCOMPATIBLE =
            "cpu CoreNum=4 cannot be selected together with drive Speed=5400";
    private static final String CACHE_RESOURCE_PATH = "target/ruletrans-pipeline-cache-resources";

    @Override
    protected boolean diagnosticsEnabled() {
        return true;
    }

    @Test
    public void testPartCategoryCompileOnlyPrintsPipelineDiagnostics() {
        RuleTransPipelineOptions options = cachedOptions(
                CPU_AT_MOST_ONE,
                CATEGORY_CPU,
                RuleTransPipelineOptions.compileOnly(),
                RuleTransTestFixtures.cpuAtMostOneMethodBody());
        RuleTransPipelineRunResult result = runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                CATEGORY_CPU,
                CPU_AT_MOST_ONE,
                options);

        expectJavaContains("addLessOrEqual").assertMatches(result.methodBody());
        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertFalse(result.diagnostics().llmCalls().isEmpty());
        assertTrue(result.pipelineResult().businessCaseSet().isEmpty());
        assertNull(result.pipelineResult().ruleUnitReport());
        assertTrue(result.diagnostics().llmCalls().stream()
                .anyMatch(call -> call.cacheStatus() == LlmCacheStatus.HIT));
    }

    @Test
    public void testPrinterOutputsCompileOnlySkippedBusinessCasesAndRuleUnit() {
        RuleTransPipelineOptions options = cachedOptions(
                CPU_AT_MOST_ONE,
                CATEGORY_CPU,
                RuleTransPipelineOptions.compileOnly(),
                RuleTransTestFixtures.cpuAtMostOneMethodBody());
        RuleTransPipelineRunResult result = runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                CATEGORY_CPU,
                CPU_AT_MOST_ONE,
                options);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RuleTransPipelineResultPrinter.print(
                result,
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String output = bytes.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("[LLM Calls]"), output);
        assertTrue(output.contains("cache=HIT"), output);
        assertTrue(output.contains("businessCases: count=0"), output);
        assertTrue(output.contains("ruleUnit: skipped"), output);
    }

    @Test
    public void testPrinterOutputsBusinessCaseGivenExpectAndActual() {
        RuleTransPipelineOptions options = cachedOptions(
                CPU_DRIVE_INCOMPATIBLE,
                null,
                RuleTransPipelineOptions.defaults(),
                """
                        ["cpu","drive"]
                        """,
                RuleTransTestFixtures.cpu4CannotUseDrive5400MethodBody(),
                incompatibleCaseJson());
        RuleTransPipelineRunResult result = runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                null,
                CPU_DRIVE_INCOMPATIBLE,
                options);
        print(result);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RuleTransPipelineResultPrinter.print(
                result,
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String output = bytes.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("[Business Test Cases]"), output);
        assertTrue(output.contains("id=COMP-PART-001"), output);
        assertTrue(output.contains("given:"), output);
        assertTrue(output.contains("expect:"), output);
        assertTrue(output.contains("actual:"), output);
        assertTrue(output.contains("\"compatible\" : false"), output);
        assertTrue(output.contains("model=qwen/"), output);
        assertTrue(output.contains("cacheKey="), output);
    }

    @Test
    public void testDiagnosticsDisabledByDefaultDoesNotCaptureLlmCalls() {
        RuleTransPipelineTestBase base = new RuleTransPipelineTestBase() {
        };
        RuleTransPipelineOptions options = cachedOptions(
                CPU_AT_MOST_ONE,
                CATEGORY_CPU,
                RuleTransPipelineOptions.compileOnly(),
                RuleTransTestFixtures.cpuAtMostOneMethodBody());

        RuleTransPipelineRunResult result = base.runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                CATEGORY_CPU,
                CPU_AT_MOST_ONE,
                options);
        base.print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertFalse(result.diagnostics().enabled());
        assertTrue(result.diagnostics().llmCalls().isEmpty());
    }

    @Test
    public void testModuleLevelRuleUsesCategoryIdentificationAndRuleUnit() {
        RuleTransPipelineOptions options = cachedOptions(
                CPU_DRIVE_INCOMPATIBLE,
                null,
                RuleTransPipelineOptions.defaults(),
                """
                        ["cpu","drive"]
                        """,
                RuleTransTestFixtures.cpu4CannotUseDrive5400MethodBody(),
                incompatibleCaseJson());
        RuleTransPipelineRunResult result = runRuleTrans(
                RuleTransTestFixtures.SampleRuleTransConstraint.class,
                null,
                CPU_DRIVE_INCOMPATIBLE,
                options);

        expectJavaContains("inCompatible").assertMatches(result.methodBody());
        print(result);

        assertTrue(result.success(), result.pipelineResult().messages().toString());
        assertTrue(result.methodBody().contains("cpu"), result.methodBody());
        assertTrue(result.methodBody().contains("drive"), result.methodBody());
        assertNotNull(result.pipelineResult().ruleUnitReport());
        assertTrue(result.pipelineResult().ruleUnitReport().passed());
        expectBusinessCase("COMP-PART-001", incompatibleGivenJson(), incompatibleExpectJson())
                .assertMatches(result.pipelineResult().businessCaseSet());
        assertTrue(result.diagnostics().llmCalls().stream()
                .anyMatch(call -> "CATEGORY_IDENTIFICATION".equals(call.stage())));
    }

    private RuleTransPipelineOptions cachedOptions(
            String naturalLanguage,
            String categoryCode,
            RuleTransPipelineOptions baseOptions,
            String... responses) {
        Path cacheDir = Path.of(
                "target",
                "ruletrans-pipeline-cache",
                sanitize(naturalLanguage + "-" + categoryCode + "-" + baseOptions.hashCode()));
        RuleTransPipelineOptions options = baseOptions
                .enableModel("qwen")
                .withLlmCacheDir(cacheDir);
        seedCache(cacheDir, naturalLanguage, categoryCode, options, responses);
        return options;
    }

    private void seedCache(
            Path cacheDir,
            String naturalLanguage,
            String categoryCode,
            RuleTransPipelineOptions options,
            String... responses) {
        try {
            Deque<String> remaining = new ArrayDeque<>(List.of(responses));
            PromptBuilder promptBuilder = new PromptBuilder(new RulePromptProjector());
            ModuleRuleContext moduleContext = RuleContextFactory.fromAnnotatedClass(
                    RuleTransTestFixtures.SampleRuleTransConstraint.class,
                    CACHE_RESOURCE_PATH);
            RuleContext context = context(moduleContext, categoryCode);
            if (context.isModuleLevel() && context.targetCategories().isEmpty()) {
                String prompt = promptBuilder.buildCategoryIdentificationPrompt(naturalLanguage, moduleContext.module());
                String response = remaining.removeFirst();
                writeCache(cacheDir, options, prompt, response);
                context = moduleContextWithCategories(moduleContext, response);
            }
            RuleScenario scenario = new RuleScenarioClassifier().classify(naturalLanguage, context);
            String methodPrompt = promptBuilder.buildGeneratePrompt(naturalLanguage, context, scenario);
            String methodResponse = remaining.removeFirst();
            writeCache(cacheDir, options, methodPrompt, methodResponse);
            String methodBody = new RuleSnippetPostProcessor()
                    .processMethodBody(methodResponse, scenario.sdkProfile(), context, naturalLanguage);
            if (options.generateBusinessCases()) {
                String casePrompt = promptBuilder.buildTestCasePrompt(naturalLanguage, context, scenario, methodBody);
                writeCache(cacheDir, options, casePrompt, remaining.removeFirst());
            }
            if (!remaining.isEmpty()) {
                throw new IllegalArgumentException("Unused cache fixture responses: " + remaining.size());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed RuleTrans LLM cache fixture", e);
        }
    }

    private void writeCache(
            Path cacheDir,
            RuleTransPipelineOptions options,
            String userMessage,
            String response) throws Exception {
        LlmModelProfile profile = LlmModelRegistry.load().resolve(options.llmRuntime().modelTag());
        LlmCacheKeyBuilder keyBuilder = new LlmCacheKeyBuilder();
        String key = keyBuilder.key(PromptBuilder.SYSTEM_MESSAGE, userMessage, profile);
        String now = Instant.now().toString();
        LlmCacheEntry entry = new LlmCacheEntry(
                LlmCacheKeyBuilder.SCHEMA_VERSION,
                key,
                "TEST_FIXTURE",
                profile.tag(),
                profile.identity(),
                now,
                now,
                0,
                new LlmCacheEntry.Request(PromptBuilder.SYSTEM_MESSAGE, userMessage),
                new LlmCacheEntry.Response(response),
                new LlmCacheEntry.Diagnostics(0, "test fixture"));
        new FileLLMCacheStore(cacheDir).put(entry);
    }

    private RuleContext context(ModuleRuleContext moduleContext, String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            return moduleContext;
        }
        return RuleContextFactory.partCategory(moduleContext.module(), categoryCode);
    }

    private RuleContext moduleContextWithCategories(ModuleRuleContext moduleContext, String categoryResponse)
            throws Exception {
        List<String> codes = OBJECT_MAPPER.readValue(extractJsonArray(categoryResponse), new TypeReference<>() {
        });
        List<PartCategory> categories = codes.stream()
                .map(code -> moduleContext.module().getPartCategory(code))
                .toList();
        return new ModuleRuleContext(moduleContext.module(), categories);
    }

    private String extractJsonArray(String response) {
        String text = response == null ? "[]" : response.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end >= start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String incompatibleGivenJson() {
        return """
                {
                  "parts": [
                    {"code": "cpu4", "isSelected": true},
                    {"code": "drive5400", "isSelected": true}
                  ]
                }
                """;
    }

    private String incompatibleExpectJson() {
        return """
                {"compatible": false}
                """;
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

    private String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
