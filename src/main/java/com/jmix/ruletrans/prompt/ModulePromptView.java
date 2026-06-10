package com.jmix.ruletrans.prompt;

import java.util.List;
import java.util.Map;

/**
 * Prompt-safe module projection.
 */
public record ModulePromptView(
        String code,
        String description,
        String version,
        Map<String, String> dynAttr,
        List<ParaPromptView> paras,
        List<AttrParaPromptView> attrParas,
        List<DynamicAttrPromptView> dynAttrSchemas,
        List<PartPromptView> atomicParts,
        List<PartCategoryPromptView> categories,
        List<String> availableCategoryCodes) {
}
