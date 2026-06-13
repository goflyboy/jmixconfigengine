package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.RuleUnitCaseExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.impl.llm.CachedLLMInvoker;
import com.jmix.tool.impl.llm.FileLLMCacheStore;
import com.jmix.tool.impl.llm.LLMInvoker;
import com.jmix.tool.impl.llm.LLMInvokerFactory;
import com.jmix.tool.impl.llm.LLMInvokerImpl;
import com.jmix.tool.impl.llm.LlmModelProfile;
import com.jmix.tool.impl.llm.LlmRuntimeOptions;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/**
 * System-test base for executing the complete RuleTrans pipeline with diagnostics.
 */
public abstract class RuleTransPipelineTestBase {

    private static final Path BASE_OUTPUT_DIR = Path.of("target/ruletrans-pipeline-system");
    private static final String TEMP_RESOURCE_PATH = "target/ruletrans-pipeline-system-resources";

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String naturalLanguage,
            RuleTransJavaExpectation expectation) {
        return assertRuleTrans(annotatedModelClass, null, naturalLanguage, expectation);
    }

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String naturalLanguage,
            RuleTransJavaExpectation javaExpectation,
            RuleTransBusinessCaseExpectation caseExpectation) {
        return assertRuleTrans(annotatedModelClass, null, naturalLanguage, javaExpectation, caseExpectation);
    }

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String categoryCode,
            String naturalLanguage,
            RuleTransJavaExpectation expectation) {
        return assertRuleTrans(
                annotatedModelClass,
                categoryCode,
                naturalLanguage,
                expectation,
                RuleTransBusinessCaseExpectation.any());
    }

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String categoryCode,
            String naturalLanguage,
            RuleTransJavaExpectation javaExpectation,
            RuleTransBusinessCaseExpectation caseExpectation) {
        RuleTransPipelineRunResult result = runRuleTrans(
                annotatedModelClass,
                categoryCode,
                naturalLanguage,
                RuleTransPipelineOptions.defaults());
        assertTrue(result.success(), result.pipelineResult().messages().toString());
        javaExpectation.assertMatches(result.methodBody());
        if (caseExpectation != null) {
            caseExpectation.assertMatches(result.pipelineResult().businessCaseSet());
        }
        return result;
    }

    protected RuleTransPipelineRunResult runRuleTrans(
            Class<?> annotatedModelClass,
            String categoryCode,
            String naturalLanguage,
            RuleTransPipelineOptions options) {
        ModuleRuleContext moduleContext = RuleContextFactory.fromAnnotatedClass(
                annotatedModelClass,
                TEMP_RESOURCE_PATH);
        RuleContext context = context(moduleContext, categoryCode);
        RuleTransPipelineDiagnostics diagnostics = new RuleTransPipelineDiagnostics(
                outputDir(annotatedModelClass, categoryCode, naturalLanguage),
                diagnosticsEnabled());
        LLMInvoker invoker = llmInvoker(options);
        if (diagnostics.enabled()) {
            invoker = new DiagnosticLlmInvoker(invoker, diagnostics);
        }
        RuleTransPipelineResult pipelineResult = pipeline(invoker, diagnostics.outputDir()).execute(
                new RuleTransRequest(naturalLanguage, context, maxRetries(), options));
        return new RuleTransPipelineRunResult(naturalLanguage, context.summary(), pipelineResult, diagnostics);
    }

    protected RuleTransJavaExpectation expectJavaContains(String expectedSnippet) {
        return RuleTransJavaExpectation.contains(expectedSnippet);
    }

    protected RuleTransJavaExpectation expectJavaEqualsIgnoringWhitespace(String expectedMethodBody) {
        return RuleTransJavaExpectation.equalsIgnoringWhitespace(expectedMethodBody);
    }

    protected RuleTransBusinessCaseExpectation expectBusinessCase(
            String caseId,
            String expectedGivenJson,
            String expectedExpectJson) {
        return RuleTransBusinessCaseExpectation.expectCase(caseId, expectedGivenJson, expectedExpectJson);
    }

    protected void print(RuleTransPipelineRunResult result) {
        if (!result.diagnostics().enabled()) {
            return;
        }
        PrintStream utf8Out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        RuleTransPipelineResultPrinter.print(result, utf8Out);
    }

    protected LLMInvoker llmInvoker() {
        return llmInvoker(RuleTransPipelineOptions.defaults());
    }

    protected LLMInvoker llmInvoker(RuleTransPipelineOptions options) {
        LlmRuntimeOptions runtime = options == null
                ? LlmRuntimeOptions.defaults()
                : options.llmRuntime();
        LlmModelProfile profile = LLMInvokerFactory.profile(runtime.modelTag());
        LLMInvoker realInvoker = new LLMInvokerImpl(profile);
        return new CachedLLMInvoker(
                realInvoker,
                new FileLLMCacheStore(runtime.cacheDir()),
                runtime,
                profile);
    }

    protected int maxRetries() {
        return 1;
    }

    protected boolean diagnosticsEnabled() {
        return false;
    }

    private RuleContext context(ModuleRuleContext moduleContext, String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            return moduleContext;
        }
        return RuleContextFactory.partCategory(moduleContext.module(), categoryCode);
    }

    private RuleTransPipeline pipeline(LLMInvoker invoker, Path outputDir) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(outputDir.resolve("pipeline"));
        return new RuleTransPipeline(
                new CategoryIdentifier(invoker, promptBuilder),
                new RuleSnippetGenerator(invoker, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(invoker, promptBuilder),
                new RuleUnitCaseExecutionProcessor(tempFileManager),
                promptBuilder);
    }

    private Path outputDir(Class<?> annotatedModelClass, String categoryCode, String naturalLanguage) {
        String scope = categoryCode == null || categoryCode.isBlank() ? "module" : categoryCode;
        String id = currentTestId() + "-" + annotatedModelClass.getSimpleName() + "-" + scope + "-"
                + Integer.toHexString(
                naturalLanguage == null ? 0 : naturalLanguage.hashCode());
        return BASE_OUTPUT_DIR.resolve(sanitize(id));
    }

    private String currentTestId() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (className.startsWith("com.jmix.") && className.contains("Test")
                    && methodName.startsWith("test")) {
                int packageEnd = className.lastIndexOf('.');
                String simpleName = packageEnd < 0 ? className : className.substring(packageEnd + 1);
                return simpleName.replace('$', '-') + "-" + methodName;
            }
        }
        return "run";
    }

    private String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
