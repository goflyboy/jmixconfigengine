package com.jmix.ruletrans.generator;

import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.sdk.SdkProfile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and validates generated Java rule method bodies.
 */
public final class RuleSnippetPostProcessor {

    private static final Pattern PACKAGE_OR_IMPORT = Pattern.compile("(?m)^\\s*(package|import)\\s+");
    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("(?m)^\\s*(public\\s+|final\\s+|abstract\\s+)*class\\s+");
    private static final Pattern METHOD_DECLARATION = Pattern.compile(
            "(?m)^\\s*(public|protected|private)?\\s*(static\\s+)?(final\\s+)?"
                    + "(void|[A-Za-z_$][A-Za-z0-9_$.<>\\[\\]]*)\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(");
    private static final Pattern CODE_RULE_ANNOTATION = Pattern.compile("@\\s*CodeRuleAnno\\b");
    private static final Pattern FORBIDDEN_PACKAGE =
            Pattern.compile("com\\.google\\.ortools|com\\.jmix\\.executor\\.impl\\.");
    private static final Pattern CONSTRAINT_FORBIDDEN_API = Pattern.compile(
            "\\b(currentInst|partCategorySum)\\s*\\(|\\.setValue\\s*\\(|\\bsetDynAttr\\s*\\("
                    + "|\\bpartCategory\\s*\\(|\\bpart\\s*\\(");
    private static final Pattern POST_FORBIDDEN_API = Pattern.compile(
            "\\bmodel\\s*\\(|\\bpartVar\\s*\\(|\\bpartCategoryVar\\s*\\(|\\bpara\\s*\\("
                    + "|\\binCompatible\\s*\\(|\\baddCompatibleConstraint|\\bonlyEnforceIf\\s*\\("
                    + "|\\bupdatePriorityObjectFuntion\\s*\\(|\\baddVarAboutHiddenConstraints\\s*\\(");

    public String process(String rawResponse) {
        return processMethodBody(rawResponse, SdkProfile.CONSTRAINT);
    }

    public String process(String rawResponse, SdkProfile sdkProfile) {
        return processMethodBody(rawResponse, sdkProfile);
    }

    public String processMethodBody(String rawResponse) {
        return processMethodBody(rawResponse, SdkProfile.CONSTRAINT);
    }

    public String processMethodBody(String rawResponse, SdkProfile sdkProfile) {
        String methodBody = normalizeMethodBody(extractCode(rawResponse));
        validateMethodBody(methodBody, sdkProfile == null ? SdkProfile.CONSTRAINT : sdkProfile);
        return methodBody;
    }

    /**
     * Compatibility helper for legacy snippet-shaped responses.
     */
    public String processLegacySnippetToMethodBody(String rawResponse, SdkProfile sdkProfile) {
        String code = extractCode(rawResponse);
        String methodBody = looksLikeMethod(code) ? extractMethodBody(code) : code;
        return processMethodBody(methodBody, sdkProfile);
    }

    private String extractCode(String rawResponse) {
        if (rawResponse == null) {
            throw new RuleTransException("Generated response is null");
        }
        String text = rawResponse.trim();
        int firstFence = text.indexOf("```");
        if (firstFence < 0) {
            return text;
        }
        int codeStart = text.indexOf('\n', firstFence);
        int codeEnd = text.indexOf("```", codeStart + 1);
        if (codeStart >= 0 && codeEnd > codeStart) {
            return text.substring(codeStart + 1, codeEnd).trim();
        }
        return text;
    }

    private String normalizeMethodBody(String methodBody) {
        if (methodBody == null) {
            return "";
        }
        return methodBody.trim();
    }

    private void validateMethodBody(String methodBody, SdkProfile sdkProfile) {
        if (methodBody == null || methodBody.trim().isEmpty()) {
            throw new RuleTransException("Generated rule method body is blank");
        }
        if (PACKAGE_OR_IMPORT.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not contain package/import declarations");
        }
        if (CLASS_DECLARATION.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not contain a class declaration");
        }
        if (CODE_RULE_ANNOTATION.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not contain @CodeRuleAnno");
        }
        if (METHOD_DECLARATION.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not contain a method declaration");
        }
        if (FORBIDDEN_PACKAGE.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not use internal or OR-Tools packages");
        }
        if (sdkProfile == SdkProfile.POST && POST_FORBIDDEN_API.matcher(methodBody).find()) {
            throw new RuleTransException("POST rule method body must not use constraint-model APIs");
        }
        if (sdkProfile == SdkProfile.CONSTRAINT && CONSTRAINT_FORBIDDEN_API.matcher(methodBody).find()) {
            throw new RuleTransException("Constraint rule method body must not use POST instance-view APIs");
        }
    }

    private boolean looksLikeMethod(String code) {
        return CODE_RULE_ANNOTATION.matcher(code).find() || METHOD_DECLARATION.matcher(code).find();
    }

    private String extractMethodBody(String code) {
        Matcher matcher = METHOD_DECLARATION.matcher(code);
        if (!matcher.find()) {
            throw new RuleTransException("Legacy generated snippet does not contain a method declaration");
        }
        int openBrace = code.indexOf('{', matcher.end());
        if (openBrace < 0) {
            throw new RuleTransException("Legacy generated snippet does not contain a method body");
        }
        int closeBrace = findMatchingBrace(code, openBrace);
        if (closeBrace < 0) {
            throw new RuleTransException("Legacy generated snippet has unbalanced braces");
        }
        return code.substring(openBrace + 1, closeBrace).trim();
    }

    private int findMatchingBrace(String value, int openIndex) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = openIndex; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
