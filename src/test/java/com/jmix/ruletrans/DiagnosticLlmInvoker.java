package com.jmix.ruletrans;

import com.jmix.tool.impl.llm.LLMInvoker;
import com.jmix.tool.impl.llm.LlmInvocationTrace;
import com.jmix.tool.impl.llm.LlmInvocationTraceProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * LLM invoker decorator that records prompt, response, duration and failure diagnostics.
 */
final class DiagnosticLlmInvoker implements LLMInvoker {

    private static final int SUMMARY_LIMIT = 600;

    private final LLMInvoker delegate;
    private final RuleTransPipelineDiagnostics diagnostics;

    DiagnosticLlmInvoker(LLMInvoker delegate, RuleTransPipelineDiagnostics diagnostics) {
        this.delegate = delegate;
        this.diagnostics = diagnostics;
    }

    @Override
    public String generate(String systemMessage, String userMessage) throws Exception {
        if (delegate == null) {
            throw new IllegalStateException("delegate LLM invoker must not be null");
        }
        if (!diagnostics.enabled()) {
            return delegate.generate(systemMessage, userMessage);
        }
        int index = diagnostics.nextLlmCallIndex();
        String stage = identifyStage(userMessage);
        Path promptFile = writeDiagnosticFile(index, "prompt", userMessage);
        long start = System.nanoTime();
        try {
            String response = delegate.generate(systemMessage, userMessage);
            long durationMillis = elapsedMillis(start);
            Path responseFile = writeDiagnosticFile(index, "response", response);
            LlmInvocationTrace trace = traceOf(delegate);
            diagnostics.addLlmCall(new RuleTransLlmCallDiagnostic(
                    index,
                    stage,
                    summarize(userMessage),
                    promptFile,
                    summarize(response),
                    responseFile,
                    durationMillis,
                    true,
                    "",
                    trace.modelTag(),
                    trace.modelIdentity(),
                    trace.cacheStatus(),
                    trace.cacheKey(),
                    trace.cacheEntryFile()));
            return response;
        } catch (Exception e) {
            long durationMillis = elapsedMillis(start);
            Path responseFile = writeDiagnosticFile(index, "response", "");
            LlmInvocationTrace trace = traceOf(delegate);
            diagnostics.addLlmCall(new RuleTransLlmCallDiagnostic(
                    index,
                    stage,
                    summarize(userMessage),
                    promptFile,
                    "",
                    responseFile,
                    durationMillis,
                    false,
                    messageOf(e),
                    trace.modelTag(),
                    trace.modelIdentity(),
                    trace.cacheStatus(),
                    trace.cacheKey(),
                    trace.cacheEntryFile()));
            throw e;
        }
    }

    @Override
    public String getConfigInfo() {
        return delegate == null ? "diagnostic:null" : delegate.getConfigInfo();
    }

    private Path writeDiagnosticFile(int index, String suffix, String content) {
        try {
            Files.createDirectories(diagnostics.outputDir());
            Path file = diagnostics.outputDir().resolve("llm-%03d.%s.txt".formatted(index, suffix));
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
            return file;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write RuleTrans diagnostic file", e);
        }
    }

    private String identifyStage(String prompt) {
        String text = prompt == null ? "" : prompt;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("partcategory code") && lower.contains("json")) {
            return "CATEGORY_IDENTIFICATION";
        }
        if (lower.contains("compiler errors") || lower.contains("compilation")) {
            return "COMPILATION_CORRECTION";
        }
        if (lower.contains("failed cases") || lower.contains("test correction")) {
            return "TEST_CORRECTION";
        }
        if (text.contains("\"businessFamily\"") || lower.contains("business test")) {
            return "BUSINESS_CASE_GENERATION";
        }
        if (lower.contains("java rule method") || lower.contains("method body")) {
            return "RULE_GENERATION";
        }
        return "LLM_CALL";
    }

    private LlmInvocationTrace traceOf(LLMInvoker invoker) {
        if (invoker instanceof LlmInvocationTraceProvider provider) {
            LlmInvocationTrace trace = provider.lastInvocationTrace();
            return trace == null ? LlmInvocationTrace.empty() : trace;
        }
        return LlmInvocationTrace.empty();
    }

    private String summarize(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .trim();
        if (compact.length() <= SUMMARY_LIMIT) {
            return compact;
        }
        return compact.substring(0, SUMMARY_LIMIT) + "...";
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String messageOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }
}
