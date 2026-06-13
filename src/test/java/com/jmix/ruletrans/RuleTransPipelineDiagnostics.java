package com.jmix.ruletrans;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable collector for RuleTrans system-test diagnostics.
 */
public final class RuleTransPipelineDiagnostics {

    private final Path outputDir;
    private final boolean enabled;
    private final List<RuleTransLlmCallDiagnostic> llmCalls = new ArrayList<>();

    public RuleTransPipelineDiagnostics(Path outputDir) {
        this(outputDir, true);
    }

    public RuleTransPipelineDiagnostics(Path outputDir, boolean enabled) {
        this.outputDir = outputDir;
        this.enabled = enabled;
    }

    public Path outputDir() {
        return outputDir;
    }

    public boolean enabled() {
        return enabled;
    }

    public void addLlmCall(RuleTransLlmCallDiagnostic call) {
        if (!enabled) {
            return;
        }
        llmCalls.add(call);
    }

    public List<RuleTransLlmCallDiagnostic> llmCalls() {
        return List.copyOf(llmCalls);
    }

    public int nextLlmCallIndex() {
        return llmCalls.size() + 1;
    }
}
