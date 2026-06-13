package com.jmix.ruletrans.prompt;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.RuleTransVerificationFailure;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.sdk.SdkContext;
import com.jmix.ruletrans.sdk.SdkContextBuilder;
import com.jmix.tool.impl.PromptTemplateLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds RuleTrans prompts from templates and projected context.
 */
public final class PromptBuilder {

    public static final String SYSTEM_MESSAGE = """
            You generate Java rule method bodies for JMix Config Engine.
            Return only Java statements that can be inserted inside an existing rule method.
            Do not return package declarations, imports, classes, annotations, method declarations,
            explanations, or markdown unless asked.
            """;

    private final RulePromptProjector projector;
    private final SdkContextBuilder sdkContextBuilder;

    public PromptBuilder() {
        this(new RulePromptProjector());
    }

    public PromptBuilder(RulePromptProjector projector) {
        this(projector, new SdkContextBuilder());
    }

    public PromptBuilder(RulePromptProjector projector, SdkContextBuilder sdkContextBuilder) {
        this.projector = projector;
        this.sdkContextBuilder = sdkContextBuilder == null ? new SdkContextBuilder() : sdkContextBuilder;
    }

    public String buildGeneratePrompt(String naturalLanguage, RuleContext context) {
        return buildGeneratePrompt(naturalLanguage, context, null);
    }

    public String buildGeneratePrompt(String naturalLanguage, RuleContext context, RuleScenario scenario) {
        validate(naturalLanguage, context);
        return buildGeneratePrompt(
                naturalLanguage,
                context,
                scenario,
                sdkContextBuilder.build(context, scenario));
    }

    public String buildGeneratePrompt(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            SdkContext sdkContext) {
        validate(naturalLanguage, context);
        return PromptTemplateLoader.loadAndRenderTemplate(
                generateTemplate(context, scenario),
                baseVariables(naturalLanguage, context, promptView(context), scenario, sdkContext));
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
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/module_stage1_prompt.jtl", variables);
    }

    public String buildCompilationCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            String previousSnippet,
            CompilationResult result) {
        return buildCompilationCorrectionPrompt(naturalLanguage, context, null, previousSnippet, result);
    }

    public String buildCompilationCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            String previousMethodBody,
            CompilationResult result) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(
                naturalLanguage, context, promptView(context), scenario, sdkContextBuilder.build(context, scenario));
        variables.put("previousMethodBody", value(previousMethodBody));
        variables.put("previousSnippet", value(previousMethodBody));
        variables.put("compilerErrors", result == null ? "" : String.join("\n", result.errors()));
        variables.put("diagnostics", result == null ? "" : String.join("\n", result.diagnostics()));
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/correction_compilation_prompt.jtl", variables);
    }

    public String buildTestCasePrompt(String naturalLanguage, RuleContext context, String snippet) {
        return buildTestCasePrompt(naturalLanguage, context, null, snippet);
    }

    public String buildTestCasePrompt(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            String methodBody) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(
                naturalLanguage, context, promptView(context), scenario, sdkContextBuilder.build(context, scenario));
        variables.put("methodBody", value(methodBody));
        variables.put("snippet", value(methodBody));
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/test_case_prompt.jtl", variables);
    }

    public String buildTestCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            String previousSnippet,
            List<RuleTransVerificationFailure> failedCases) {
        return buildTestCorrectionPrompt(naturalLanguage, context, null, previousSnippet, failedCases);
    }

    public String buildTestCorrectionPrompt(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            String previousMethodBody,
            List<RuleTransVerificationFailure> failedCases) {
        validate(naturalLanguage, context);
        Map<String, String> variables = baseVariables(
                naturalLanguage, context, promptView(context), scenario, sdkContextBuilder.build(context, scenario));
        variables.put("previousMethodBody", value(previousMethodBody));
        variables.put("previousSnippet", value(previousMethodBody));
        variables.put("failedCases", failedCases == null ? "" : failedCases.toString());
        return PromptTemplateLoader.loadAndRenderTemplate("ruletrans/correction_test_prompt.jtl", variables);
    }

    private Object promptView(RuleContext context) {
        return context.isModuleLevel()
                ? projector.projectModule(context)
                : projector.projectPartCategory(context);
    }

    private String generateTemplate(RuleContext context, RuleScenario scenario) {
        if (scenario != null && scenario.isPost()) {
            return context.isModuleLevel()
                    ? "ruletrans/post_module_prompt.jtl"
                    : "ruletrans/post_part_category_prompt.jtl";
        }
        return context.isModuleLevel()
                ? "ruletrans/module_stage2_prompt.jtl"
                : "ruletrans/part_category_prompt.jtl";
    }

    private Map<String, String> baseVariables(
            String naturalLanguage,
            RuleContext context,
            Object view,
            RuleScenario scenario,
            SdkContext sdkContext) {
        SdkContext safeSdkContext = sdkContext == null ? sdkContextBuilder.build(context, scenario) : sdkContext;
        Map<String, String> variables = new HashMap<>();
        variables.put("naturalLanguage", naturalLanguage);
        variables.put("contextSummary", context.summary());
        variables.put("contextJson", projector.toJson(view));
        variables.put("targetCategories", String.join(",", context.categoryCodes()));
        variables.put("sdkProfile", safeSdkContext.profile().name());
        variables.put("allowedSdkApis", safeSdkContext.allowedApisText());
        variables.put("forbiddenSdkApis", safeSdkContext.forbiddenApisText());
        variables.put("ruleScenario", scenario == null ? "" : scenario.toString());
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
