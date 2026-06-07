package com.jmix.ruletrans.prompt;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.FailedTestCase;
import com.jmix.tool.impl.PromptTemplateLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds RuleTrans prompts from templates and projected context.
 */
public final class PromptBuilder {

    public static final String SYSTEM_MESSAGE = """
            You generate Java rule method snippets for JMix Config Engine.
            Return only Java method snippets annotated with @CodeRuleAnno.
            Do not return package declarations, imports, classes, explanations, or markdown unless asked.
            """;

    private final RulePromptProjector projector;

    public PromptBuilder() {
        this(new RulePromptProjector());
    }

    public PromptBuilder(RulePromptProjector projector) {
        this.projector = projector;
    }

    public String buildGeneratePrompt(String naturalLanguage, RuleContext context) {
        validate(naturalLanguage, context);
        String template = context.isProductLevel()
                ? "ruletrans/product_stage2_prompt.jtl"
                : "ruletrans/part_category_prompt.jtl";
        Object view = context.isProductLevel()
                ? projector.projectProduct(context)
                : projector.projectPartCategory(context);
        return PromptTemplateLoader.loadAndRenderTemplate(template, baseVariables(naturalLanguage, context, view));
    }

    public String buildCategoryIdentificationPrompt(String naturalLanguage, Module module) {
        if (module == null) {
            throw new IllegalArgumentException("module must not be null");
        }
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            throw new IllegalArgumentException("naturalLanguage must not be blank");
        }
        Map<String, String> variables = new HashMap<>();
        variables.put("naturalLanguage", naturalLanguage);
        variables.put("moduleCode", value(module.getCode()));
        variables.put("availableCategories", categoryMemo(module.getAllPartCategorys()));
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/product_stage1_prompt.jtl", variables);
    }

    public String buildCompilationCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            String previousSnippet,
            CompilationResult result) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(naturalLanguage, context, projector.projectProduct(context));
        variables.put("previousSnippet", value(previousSnippet));
        variables.put("compilerErrors", result == null ? "" : String.join("\n", result.errors()));
        variables.put("diagnostics", result == null ? "" : String.join("\n", result.diagnostics()));
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/correction_compilation_prompt.jtl", variables);
    }

    public String buildTestCasePrompt(String naturalLanguage, RuleContext context, String snippet) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(naturalLanguage, context, projector.projectProduct(context));
        variables.put("snippet", value(snippet));
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/test_case_prompt.jtl", variables);
    }

    public String buildTestCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            String previousSnippet,
            List<FailedTestCase> failedCases) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(naturalLanguage, context, projector.projectProduct(context));
        variables.put("previousSnippet", value(previousSnippet));
        variables.put("failedCases", failedCases == null ? "" : failedCases.toString());
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/correction_test_prompt.jtl", variables);
    }

    private Map<String, String> baseVariables(String naturalLanguage, RuleContext context, Object view) {
        Map<String, String> variables = new HashMap<>();
        variables.put("naturalLanguage", naturalLanguage);
        variables.put("contextSummary", context.summary());
        variables.put("contextJson", projector.toJson(view));
        variables.put("targetCategories", String.join(",", context.categoryCodes()));
        return variables;
    }

    private void validate(String naturalLanguage, RuleContext context) {
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            throw new IllegalArgumentException("naturalLanguage must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    private String categoryMemo(List<PartCategory> categories) {
        StringBuilder builder = new StringBuilder();
        for (PartCategory category : categories) {
            builder.append("- code=").append(value(category.getCode()))
                    .append(", name=").append(value(category.getDescription()))
                    .append(", dynAttrs=")
                    .append(category.getDynAttrSchemas().stream().map(attr -> attr.getCode()).toList())
                    .append('\n');
        }
        return builder.toString();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
