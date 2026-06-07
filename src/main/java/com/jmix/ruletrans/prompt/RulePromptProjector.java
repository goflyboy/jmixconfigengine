package com.jmix.ruletrans.prompt;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.ModuleBase;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.ruletrans.context.RuleContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects existing domain objects into prompt-only views.
 */
public final class RulePromptProjector {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    public PartCategoryPromptView projectPartCategory(RuleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (context.targetCategories().isEmpty()) {
            throw new IllegalArgumentException("context must contain at least one target category");
        }
        return projectCategory(context.targetCategories().get(0));
    }

    public ProductPromptView projectProduct(RuleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        Module module = context.module();
        List<PartCategory> categories = context.targetCategories().isEmpty()
                ? module.getPartCategorys()
                : context.targetCategories();
        return new ProductPromptView(
                module.getCode(),
                module.getDescription(),
                module.getVersion(),
                stableMap(module.getDynAttr()),
                projectParas(module),
                projectAttrParas(module),
                projectDynAttrs(module),
                projectParts(module.getAtomicParts()),
                categories.stream().map(this::projectCategory).toList(),
                module.getAllPartCategorys().stream().map(PartCategory::getCode).toList());
    }

    public String toJson(Object view) {
        try {
            return objectMapper.writeValueAsString(view);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize prompt view", e);
        }
    }

    private PartCategoryPromptView projectCategory(PartCategory category) {
        return new PartCategoryPromptView(
                category.getCode(),
                category.getDescription(),
                category.getFatherCode(),
                stringValue(category.getPartType()),
                category.isSupportMultiInst(),
                stringValue(category.getSelectionPolicy()),
                projectParas(category),
                projectAttrParas(category),
                projectDynAttrs(category),
                stableMap(category.getDynAttr()),
                projectParts(category.getAtomicParts()),
                category.getPartCategorys().stream().map(this::projectCategory).toList());
    }

    private List<ParaPromptView> projectParas(ModuleBase moduleBase) {
        return moduleBase.getParas().stream().map(this::projectPara).toList();
    }

    private ParaPromptView projectPara(Para para) {
        return new ParaPromptView(
                para.getCode(),
                para.getName(),
                para.getFatherCode(),
                stringValue(para.getParaType()),
                stringValue(para.getAssignType()),
                para.getDefaultValue(),
                para.getMinValue(),
                para.getMaxValue(),
                para.getRefSpecCode(),
                projectOptions(para.getOptions()));
    }

    private List<AttrParaPromptView> projectAttrParas(ModuleBase moduleBase) {
        return moduleBase.getAttrParas().stream().map(this::projectAttrPara).toList();
    }

    private AttrParaPromptView projectAttrPara(AttrPara attrPara) {
        return new AttrParaPromptView(attrPara.getAttrCode(), stringValue(attrPara.getType()));
    }

    private List<DynamicAttrPromptView> projectDynAttrs(ModuleBase moduleBase) {
        return moduleBase.getDynAttrSchemas().stream().map(this::projectDynAttr).toList();
    }

    private DynamicAttrPromptView projectDynAttr(DynamicAttribute attr) {
        return new DynamicAttrPromptView(
                attr.getCode(),
                attr.getName(),
                stringValue(attr.getDynAttrType()),
                attr.getInstType(),
                attr.getValue(),
                attr.getOptionExtSchema(),
                projectOptions(attr.getOptions()));
    }

    private List<PromptOptionView> projectOptions(List<DynamicAttributerOption> options) {
        return options.stream()
                .map(option -> new PromptOptionView(
                        option.getCode(),
                        option.getCodeId(),
                        option.getCodeValue(),
                        option.getDefaultValue(),
                        option.getDescription(),
                        option.getSortNo()))
                .toList();
    }

    private List<PartPromptView> projectParts(List<Part> parts) {
        return parts.stream().map(this::projectPart).toList();
    }

    private PartPromptView projectPart(Part part) {
        return new PartPromptView(
                part.getCode(),
                part.getDescription(),
                part.getFatherCode(),
                stringValue(part.getPartType()),
                part.getMaxQuantity(),
                part.getDefaultQuantity(),
                part.getPrice(),
                stableMap(part.getDynAttr()));
    }

    private Map<String, String> stableMap(Map<String, String> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
