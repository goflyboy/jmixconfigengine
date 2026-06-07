package com.jmix.ruletrans.prompt;

import java.util.List;

/**
 * Prompt-safe parameter projection.
 */
public record ParaPromptView(
        String code,
        String name,
        String fatherCode,
        String paraType,
        String assignType,
        String defaultValue,
        String minValue,
        String maxValue,
        String refSpecCode,
        List<PromptOptionView> options) {
}
