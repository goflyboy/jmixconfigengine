package com.jmix.ruletrans.metadata;

import com.jmix.executor.bmodel.logic.EffectScope;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.scenario.RuleScenario;

/**
 * Template-owned metadata for assembling a complete rule method.
 */
public record RuleMetadata(
        String ruleCode,
        String methodName,
        String normalNaturalCode,
        String fatherCode,
        String attrParaCodes,
        String leftProObjsStr,
        String rightProObjsStr,
        EffectScope effectScope) {

    public RuleMetadata {
        ruleCode = blankToDefault(ruleCode, methodName);
        methodName = blankToDefault(methodName, ruleCode);
        normalNaturalCode = value(normalNaturalCode);
        fatherCode = value(fatherCode);
        attrParaCodes = value(attrParaCodes);
        leftProObjsStr = value(leftProObjsStr);
        rightProObjsStr = value(rightProObjsStr);
        effectScope = effectScope == null ? EffectScope.SingleInst : effectScope;
    }

    public RuleMetadata(
            String ruleCode,
            String methodName,
            String normalNaturalCode,
            String fatherCode,
            String attrParaCodes,
            String leftProObjsStr,
            String rightProObjsStr) {
        this(ruleCode, methodName, normalNaturalCode, fatherCode, attrParaCodes,
                leftProObjsStr, rightProObjsStr, EffectScope.SingleInst);
    }

    public static RuleMetadata from(String naturalLanguage, RuleContext context, RuleScenario scenario) {
        String base = semanticBase(naturalLanguage, context, scenario);
        String methodName = "rule" + upperFirst(base);
        String fatherCode = context != null && !context.isModuleLevel() && !context.categoryCodes().isEmpty()
                ? context.categoryCodes().get(0)
                : "";
        return new RuleMetadata(methodName, methodName, naturalLanguage, fatherCode, "", "", "");
    }

    public RuleMetadata withRuleCode(String newRuleCode) {
        return new RuleMetadata(newRuleCode, newRuleCode, normalNaturalCode, fatherCode,
                attrParaCodes, leftProObjsStr, rightProObjsStr, effectScope);
    }

    public RuleMetadata withEffectScope(EffectScope newEffectScope) {
        return new RuleMetadata(ruleCode, methodName, normalNaturalCode, fatherCode,
                attrParaCodes, leftProObjsStr, rightProObjsStr, newEffectScope);
    }

    private static String semanticBase(String naturalLanguage, RuleContext context, RuleScenario scenario) {
        String text = naturalLanguage == null ? "" : naturalLanguage;
        StringBuilder raw = new StringBuilder();
        if (context != null && !context.categoryCodes().isEmpty()) {
            raw.append(context.categoryCodes().get(0)).append(' ');
        }
        raw.append(text);
        String ascii = raw.toString().replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (ascii.isEmpty()) {
            ascii = (scenario == null ? "generated" : scenario.family().name().toLowerCase()) + " rule";
        }
        StringBuilder base = new StringBuilder();
        for (String part : ascii.split("\\s+")) {
            if (!part.isEmpty()) {
                base.append(upperFirst(part));
            }
        }
        return base.isEmpty() ? "GeneratedRule" : base.toString();
    }

    private static String upperFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        String normalized = value(value);
        return normalized.isEmpty() ? value(defaultValue) : normalized;
    }
}
