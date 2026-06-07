package com.jmix.ruletrans.prompt;

import java.util.Map;

/**
 * Prompt-safe atomic part projection.
 */
public record PartPromptView(
        String code,
        String name,
        String fatherCode,
        String partType,
        Integer maxQuantity,
        Integer defaultQuantity,
        Long price,
        Map<String, String> dynAttr) {
}
