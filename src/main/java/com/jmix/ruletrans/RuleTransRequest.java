package com.jmix.ruletrans;

import com.jmix.ruletrans.context.RuleContext;

/**
 * Single public request model for natural-language RuleTrans execution.
 */
public record RuleTransRequest(
        String naturalLanguage,
        RuleContext context,
        int maxRetries,
        RuleTransPipelineOptions options) {

    public RuleTransRequest {
        options = options == null ? RuleTransPipelineOptions.defaults() : options;
    }
}
