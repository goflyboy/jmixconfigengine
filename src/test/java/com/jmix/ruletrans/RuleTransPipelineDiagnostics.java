package com.jmix.ruletrans;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable collector for RuleTrans system-test diagnostics.
 */
public final class RuleTransPipelineDiagnostics {

    private final Path outputDir;
    private final List<RuleTransLlmCallDiagnostic> llmCalls = new ArrayList<>();

    public RuleTransPipelineDiagnostics(Path outputDir) {
        this.outputDir = outputDir;
    }

    public Path outputDir() {
        return outputDir;
    }

    public void addLlmCall(RuleTransLlmCallDiagnostic call) {
        llmCalls.add(call);
    }

    public List<RuleTransLlmCallDiagnostic> llmCalls() {
        return List.copyOf(llmCalls);
    }

    public int nextLlmCallIndex() {
        return llmCalls.size() + 1;
    }
}
