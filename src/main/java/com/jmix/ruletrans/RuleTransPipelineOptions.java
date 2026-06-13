package com.jmix.ruletrans;

import com.jmix.tool.impl.llm.LlmCacheMode;
import com.jmix.tool.impl.llm.LlmRuntimeOptions;

import java.nio.file.Path;

/**
 * Runtime switches for the RuleTrans end-to-end pipeline.
 */
public record RuleTransPipelineOptions(
        boolean generateBusinessCases,
        boolean executeBusinessCases,
        boolean allowEmptyBusinessCases,
        LlmRuntimeOptions llmRuntime) {

    public RuleTransPipelineOptions {
        llmRuntime = llmRuntime == null ? LlmRuntimeOptions.defaults() : llmRuntime;
    }

    public RuleTransPipelineOptions(
            boolean generateBusinessCases,
            boolean executeBusinessCases,
            boolean allowEmptyBusinessCases) {
        this(generateBusinessCases, executeBusinessCases, allowEmptyBusinessCases, LlmRuntimeOptions.defaults());
    }

    public static RuleTransPipelineOptions defaults() {
        return new RuleTransPipelineOptions(true, true, false);
    }

    public static RuleTransPipelineOptions compileOnly() {
        return new RuleTransPipelineOptions(false, false, true);
    }

    public RuleTransPipelineOptions withLlmRuntime(LlmRuntimeOptions nextRuntime) {
        return new RuleTransPipelineOptions(
                generateBusinessCases,
                executeBusinessCases,
                allowEmptyBusinessCases,
                nextRuntime);
    }

    public RuleTransPipelineOptions enableModel(String modelTag) {
        return withLlmRuntime(llmRuntime.withModel(modelTag));
    }

    public RuleTransPipelineOptions forceRefreshLlmCache() {
        return withLlmRuntime(llmRuntime.withCacheMode(LlmCacheMode.REFRESH));
    }

    public RuleTransPipelineOptions disableLlmCache() {
        return withLlmRuntime(llmRuntime.withCacheMode(LlmCacheMode.DISABLED));
    }

    public RuleTransPipelineOptions withLlmCacheDir(Path cacheDir) {
        return withLlmRuntime(llmRuntime.withCacheDir(cacheDir));
    }
}
