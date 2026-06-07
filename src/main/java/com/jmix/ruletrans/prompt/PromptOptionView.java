package com.jmix.ruletrans.prompt;

/**
 * Prompt-safe option projection.
 */
public record PromptOptionView(
        String code,
        int codeId,
        String codeValue,
        String defaultValue,
        String description,
        Integer sortNo) {
}
