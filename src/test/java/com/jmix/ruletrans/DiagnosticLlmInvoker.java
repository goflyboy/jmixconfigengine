package com.jmix.ruletrans;

import com.jmix.tool.impl.llm.LLMInvoker;

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
        int index = diagnostics.nextLlmCallIndex();
        String stage = identifyStage(userMessage);
        Path promptFile = writeDiagnosticFile(index, "prompt", userMessage);
        long start = System.nanoTime();
        try {
            String response = delegate.generate(systemMessage, userMessage);
            long durationMillis = elapsedMillis(start);
            Path responseFile = writeDiagnosticFile(index, "response", response);
            diagnostics.addLlmCall(new RuleTransLlmCallDiagnostic(
                    index,
                    stage,
                    summarize(userMessage),
                    promptFile,
                    summarize(response),
                    responseFile,
                    durationMillis,
                    true,
                    ""));
            return response;
        } catch (Exception e) {
            long durationMillis = elapsedMillis(start);
            Path responseFile = writeDiagnosticFile(index, "response", "");
            diagnostics.addLlmCall(new RuleTransLlmCallDiagnostic(
                    index,
                    stage,
                    summarize(userMessage),
                    promptFile,
                    "",
                    responseFile,
                    durationMillis,
                    false,
                    messageOf(e)));
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
        if (text.contains("可用的 PartCategory 列表") || lower.contains("identify")) {
            return "CATEGORY_IDENTIFICATION";
        }
        if (text.contains("编译错误") || lower.contains("compiler errors")) {
            return "COMPILATION_CORRECTION";
        }
        if (text.contains("失败测试用例") || lower.contains("failed cases")) {
            return "TEST_CORRECTION";
        }
        if (text.contains("业务可读 JSON 测试用例") || text.contains("\"businessFamily\"")) {
            return "BUSINESS_CASE_GENERATION";
        }
        if (text.contains("Java 规则方法体") || lower.contains("java rule method")) {
            return "RULE_GENERATION";
        }
        return "LLM_CALL";
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
