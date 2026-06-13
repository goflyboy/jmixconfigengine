package com.jmix.ruletrans.identifier;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.CategoryNotFoundException;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.impl.llm.LLMInvoker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Module-level stage1 category identifier.
 */
@Slf4j
public final class CategoryIdentifier {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryIdentifier(LLMInvoker llmInvoker, PromptBuilder promptBuilder) {
        this.llmInvoker = llmInvoker;
        this.promptBuilder = promptBuilder;
    }

    public List<String> identify(String naturalLanguage, Module module) {
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            throw new IllegalArgumentException("naturalLanguage must not be blank");
        }
        if (module == null) {
            throw new IllegalArgumentException("module must not be null");
        }
        module.init();
        try {
            String prompt = promptBuilder.buildCategoryIdentificationPrompt(naturalLanguage, module);
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return validateCodes(parseCodes(response), module);
        } catch (CategoryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to identify rule categories for naturalLanguage=[{}], module=[{}]",
                    naturalLanguage, module.getCode(), e);
            throw new RuleTransException("Failed to identify rule categories: " + e.getMessage(), e);
        }
    }

    private List<String> parseCodes(String response) {
        String json = extractJsonArray(response);
        try {
            List<String> codes = objectMapper.readValue(json, new TypeReference<>() {
            });
            return codes.stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            throw new RuleTransException("Category identification response is not a JSON string array: " + response,
                    e);
        }
    }

    private String extractJsonArray(String response) {
        if (response == null) {
            return "[]";
        }
        String text = response.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                text = text.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end >= start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<String> validateCodes(List<String> codes, Module module) {
        if (codes.isEmpty()) {
            throw new CategoryNotFoundException("No PartCategory identified for module-level rule");
        }
        Set<String> missing = new LinkedHashSet<>();
        for (String code : codes) {
            PartCategory category = module.getPartCategory(code);
            if (category == null) {
                missing.add(code);
            }
        }
        if (!missing.isEmpty()) {
            throw new CategoryNotFoundException("PartCategory not found: " + String.join(",", missing));
        }
        return codes;
    }
}
