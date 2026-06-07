package com.jmix.ruletrans.prompt;

import java.util.List;
import java.util.Map;

/**
 * Prompt-safe part category projection.
 */
public record PartCategoryPromptView(
        String code,
        String name,
        String fatherCode,
        String partType,
        boolean supportMultiInst,
        String selectionPolicy,
        List<ParaPromptView> paras,
        List<AttrParaPromptView> attrParas,
        List<DynamicAttrPromptView> dynAttrSchemas,
        Map<String, String> dynAttr,
        List<PartPromptView> atomicParts,
        List<PartCategoryPromptView> childCategories) {
}
