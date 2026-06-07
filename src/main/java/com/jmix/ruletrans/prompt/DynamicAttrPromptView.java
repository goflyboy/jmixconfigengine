package com.jmix.ruletrans.prompt;

import java.util.List;

/**
 * Prompt-safe dynamic attribute projection.
 */
public record DynamicAttrPromptView(
        String code,
        String name,
        String dynAttrType,
        int instType,
        String value,
        String optionExtSchema,
        List<PromptOptionView> options) {
}
