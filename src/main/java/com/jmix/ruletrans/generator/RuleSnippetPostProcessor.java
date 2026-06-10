package com.jmix.ruletrans.generator;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.sdk.SdkProfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final Pattern MARKDOWN_CODE_FENCE = Pattern.compile("```");
    private static final Pattern FORBIDDEN_PACKAGE =
            Pattern.compile("com\\.google\\.ortools|com\\.jmix\\.executor\\.impl\\.");
    private static final Pattern CONSTRAINT_FORBIDDEN_API = Pattern.compile(
            "\\b(currentInst|partCategorySum)\\s*\\(|\\.setValue\\s*\\(|\\bsetDynAttr\\s*\\("
                    + "|\\bpartCategory\\s*\\(|\\bpart\\s*\\(");
    private static final Pattern POST_FORBIDDEN_API = Pattern.compile(
            "\\bmodel\\s*\\(|\\bpartVar\\s*\\(|\\bpartCategoryVar\\s*\\(|\\bpara\\s*\\("
                    + "|\\binCompatible\\s*\\(|\\baddCompatibleConstraint|\\bonlyEnforceIf\\s*\\("
                    + "|\\bupdatePriorityObjectFuntion\\s*\\(|\\baddVarAboutHiddenConstraints\\s*\\(");
    private static final Pattern POST_INVALID_AGGREGATE_API = Pattern.compile(
            "\\bpartCategorySum\\s*\\([^)]*\\)\\s*\\.\\s*(sumQuantity|quantity)\\s*\\(");
    private static final Pattern PART_LINEAR_EXPR_DECLARATION = Pattern.compile(
            "\\bPartAlgCPLinearExpr\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=");
    private static final Pattern AGGREGATE_EXPRESSION_ARGUMENT = Pattern.compile(
            "^\\s*model\\s*\\(\\s*\\)\\s*\\.\\s*(sum4Quantity|sum4Selected)\\s*\\(");

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
        return processMethodBody(rawResponse, sdkProfile, null);
    }

    public String processMethodBody(String rawResponse, SdkProfile sdkProfile, RuleContext context) {
        String methodBody = extractCode(rawResponse);
        methodBody = normalizeAggregateAddTermUsage(methodBody);
        methodBody = normalizePartCategoryAggregateOverloads(methodBody, context);
        methodBody = normalizeInputParameterValueVars(methodBody, context);
        methodBody = normalizeFilterLiteralSyntax(methodBody);
        methodBody = normalizeMethodBody(methodBody);
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
        if (codeStart < 0) {
            throw new RuleTransException("Generated rule method body contains an unterminated markdown code fence");
        }
        int codeEnd = text.indexOf("```", codeStart + 1);
        if (codeEnd <= codeStart) {
            throw new RuleTransException("Generated rule method body contains an unterminated markdown code fence");
        }
        return text.substring(codeStart + 1, codeEnd).trim();
    }

    private String normalizeMethodBody(String methodBody) {
        if (methodBody == null) {
            return "";
        }
        return methodBody.trim();
    }

    private String normalizeFilterLiteralSyntax(String methodBody) {
        if (methodBody == null || methodBody.isEmpty()) {
            return methodBody;
        }
        StringBuilder normalized = new StringBuilder(methodBody.length());
        int index = 0;
        while (index < methodBody.length()) {
            int callStart = nextAggregateCall(methodBody, index);
            if (callStart < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }
            int openParen = methodBody.indexOf('(', callStart);
            int closeParen = findMatchingParen(methodBody, openParen);
            if (closeParen < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }

            normalized.append(methodBody, index, callStart);
            normalized.append(normalizeAggregateCallFilter(methodBody.substring(callStart, closeParen + 1)));
            index = closeParen + 1;
        }
        return normalized.toString();
    }

    private String normalizeAggregateAddTermUsage(String methodBody) {
        if (methodBody == null || methodBody.isEmpty()) {
            return methodBody;
        }
        Set<String> partLinearExprVariables = partLinearExprVariables(methodBody);
        StringBuilder normalized = new StringBuilder(methodBody.length());
        int index = 0;
        while (index < methodBody.length()) {
            int callNameStart = nextAddTermCallName(methodBody, index);
            if (callNameStart < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }
            int openParen = findCallOpenParen(methodBody, callNameStart, "addTerm");
            int closeParen = findMatchingParen(methodBody, openParen);
            if (closeParen < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }
            List<String> args = splitTopLevelArguments(methodBody.substring(openParen + 1, closeParen));
            boolean aggregateArgument = args.size() == 2
                    && isPartAggregateExpressionArgument(args.get(0), partLinearExprVariables);

            normalized.append(methodBody, index, callNameStart);
            normalized.append(aggregateArgument ? "addExpr" : "addTerm");
            normalized.append(methodBody, openParen, closeParen + 1);
            index = closeParen + 1;
        }
        return normalized.toString();
    }

    private String normalizePartCategoryAggregateOverloads(String methodBody, RuleContext context) {
        if (methodBody == null || methodBody.isEmpty() || context == null || context.isModuleLevel()) {
            return methodBody;
        }
        List<String> categoryCodes = context.categoryCodes();
        if (categoryCodes.size() != 1) {
            return methodBody;
        }
        String categoryCode = categoryCodes.get(0);
        StringBuilder normalized = new StringBuilder(methodBody.length());
        int index = 0;
        while (index < methodBody.length()) {
            int callStart = nextAggregateCall(methodBody, index);
            if (callStart < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }
            int openParen = methodBody.indexOf('(', callStart);
            int closeParen = findMatchingParen(methodBody, openParen);
            if (closeParen < 0) {
                normalized.append(methodBody, index, methodBody.length());
                break;
            }
            List<String> args = splitTopLevelArguments(methodBody.substring(openParen + 1, closeParen));
            String firstArg = args.size() >= 2 ? stringLiteralValue(args.get(0)) : null;
            boolean currentCategoryAggregate = firstArg != null
                    && (firstArg.isEmpty()
                    || firstArg.equals(categoryCode)
                    || firstArg.equals(categoryCode + "*"));

            normalized.append(methodBody, index, callStart);
            if (args.size() == 3 && currentCategoryAggregate) {
                normalized.append(methodBody, callStart, openParen + 1)
                        .append(args.get(1))
                        .append(", ")
                        .append(args.get(2))
                        .append(")");
            } else if (args.size() == 2 && isCurrentCategoryCodeArgument(firstArg, categoryCode)) {
                normalized.append(methodBody, callStart, openParen + 1)
                        .append(normalizeSecondAggregateArgumentForCurrentCategory(args.get(1), context))
                        .append(")");
            } else {
                normalized.append(methodBody, callStart, closeParen + 1);
            }
            index = closeParen + 1;
        }
        return normalized.toString();
    }

    private String normalizeInputParameterValueVars(String methodBody, RuleContext context) {
        if (methodBody == null || methodBody.isEmpty() || context == null) {
            return methodBody;
        }
        String normalized = methodBody;
        for (String paraCode : inputParaCodes(context)) {
            Pattern pattern = Pattern.compile("\\bpara\\s*\\(\\s*\""
                    + Pattern.quote(paraCode)
                    + "\"\\s*\\)\\s*\\.\\s*valueVar\\s*\\(\\s*\\)");
            normalized = pattern.matcher(normalized)
                    .replaceAll(Matcher.quoteReplacement("para(\"" + paraCode + "\").inputValue()"));
        }
        return normalized;
    }

    private Set<String> inputParaCodes(RuleContext context) {
        Set<String> codes = new HashSet<>();
        if (context == null) {
            return codes;
        }
        if (context.module() != null && context.module().getParas() != null) {
            collectInputParaCodes(context.module().getParas(), codes);
        }
        for (PartCategory category : context.targetCategories()) {
            if (category.getParas() != null) {
                collectInputParaCodes(category.getParas(), codes);
            }
        }
        return codes;
    }

    private void collectInputParaCodes(List<Para> paras, Set<String> codes) {
        for (Para para : paras) {
            if (para != null && para.getAssignType() == AssignType.INPUT && para.getCode() != null) {
                codes.add(para.getCode());
            }
        }
    }

    private boolean isCurrentCategoryCodeArgument(String value, String categoryCode) {
        return value != null && (value.equals(categoryCode) || value.equals(categoryCode + "*"));
    }

    private String normalizeSecondAggregateArgumentForCurrentCategory(String argument, RuleContext context) {
        String value = stringLiteralValue(argument);
        if (value == null) {
            return "\"\", " + argument;
        }
        if (value.isEmpty()) {
            return "\"\", \"\"";
        }
        if (isKnownTargetAttribute(value, context) && !looksLikeFilterCondition(value)) {
            return argument + ", \"\"";
        }
        return "\"\", " + argument;
    }

    private boolean isKnownTargetAttribute(String attrCode, RuleContext context) {
        if (attrCode == null || context == null) {
            return false;
        }
        return context.targetCategories().stream()
                .filter(category -> category.getDynAttrSchemas() != null)
                .flatMap(category -> category.getDynAttrSchemas().stream())
                .anyMatch(attr -> attrCode.equals(attr.getCode()));
    }

    private int nextAggregateCall(String text, int fromIndex) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = Math.max(0, fromIndex); i < text.length(); i++) {
            char ch = text.charAt(i);
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
                escaped = false;
                continue;
            }
            if (aggregateCallStartsAt(text, i, "sum4Quantity")
                    || aggregateCallStartsAt(text, i, "sum4Selected")) {
                return i;
            }
        }
        return -1;
    }

    private int nextAddTermCallName(String text, int fromIndex) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = Math.max(0, fromIndex); i < text.length(); i++) {
            char ch = text.charAt(i);
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
                escaped = false;
                continue;
            }
            if (text.startsWith(".addTerm", i)) {
                int nameStart = i + 1;
                int afterName = nameStart + "addTerm".length();
                if (afterName < text.length() && Character.isJavaIdentifierPart(text.charAt(afterName))) {
                    continue;
                }
                int openParen = skipWhitespace(text, afterName);
                if (openParen < text.length() && text.charAt(openParen) == '(') {
                    return nameStart;
                }
            }
        }
        return -1;
    }

    private int findCallOpenParen(String text, int methodNameStart, String methodName) {
        int index = methodNameStart + methodName.length();
        index = skipWhitespace(text, index);
        return index < text.length() && text.charAt(index) == '(' ? index : -1;
    }

    private int skipWhitespace(String text, int index) {
        int current = Math.max(0, index);
        while (current < text.length() && Character.isWhitespace(text.charAt(current))) {
            current++;
        }
        return current;
    }

    private boolean aggregateCallStartsAt(String text, int index, String methodName) {
        if (!text.startsWith(methodName, index)) {
            return false;
        }
        int before = index - 1;
        if (before >= 0 && Character.isJavaIdentifierPart(text.charAt(before))) {
            return false;
        }
        int after = index + methodName.length();
        return after < text.length() && text.charAt(after) == '(';
    }

    private int findMatchingParen(String text, int openIndex) {
        if (openIndex < 0) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = openIndex; i < text.length(); i++) {
            char ch = text.charAt(i);
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
                escaped = false;
                continue;
            }
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String normalizeAggregateCallFilter(String call) {
        List<int[]> stringLiterals = stringLiteralRanges(call);
        if (stringLiterals.isEmpty()) {
            return call;
        }
        int[] filterLiteral = stringLiterals.get(stringLiterals.size() - 1);
        String filter = call.substring(filterLiteral[0] + 1, filterLiteral[1]);
        String normalized = normalizeFilterContent(filter);
        if (filter.equals(normalized)) {
            return call;
        }
        return call.substring(0, filterLiteral[0] + 1)
                + normalized
                + call.substring(filterLiteral[1]);
    }

    private List<int[]> stringLiteralRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!inString) {
                if (ch == '"') {
                    inString = true;
                    escaped = false;
                    start = i;
                }
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                ranges.add(new int[] {start, i});
                inString = false;
            }
        }
        return ranges;
    }

    private List<String> splitTopLevelArguments(String args) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < args.length(); i++) {
            char ch = args.charAt(i);
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
                escaped = false;
                continue;
            }
            if (ch == '(') {
                parenDepth++;
            } else if (ch == ')') {
                parenDepth--;
            } else if (ch == '{') {
                braceDepth++;
            } else if (ch == '}') {
                braceDepth--;
            } else if (ch == '[') {
                bracketDepth++;
            } else if (ch == ']') {
                bracketDepth--;
            } else if (ch == ',' && parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                result.add(args.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(args.substring(start).trim());
        return result;
    }

    private String stringLiteralValue(String argument) {
        if (argument == null) {
            return null;
        }
        String trimmed = argument.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"' || trimmed.charAt(trimmed.length() - 1) != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder(trimmed.length() - 2);
        boolean escaped = false;
        for (int i = 1; i < trimmed.length() - 1; i++) {
            char ch = trimmed.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else {
                value.append(ch);
            }
        }
        return value.toString();
    }

    private Set<String> partLinearExprVariables(String methodBody) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = PART_LINEAR_EXPR_DECLARATION.matcher(methodBody);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    private boolean isPartAggregateExpressionArgument(String argument, Set<String> partLinearExprVariables) {
        if (argument == null) {
            return false;
        }
        String trimmed = argument.trim();
        return AGGREGATE_EXPRESSION_ARGUMENT.matcher(trimmed).find()
                || partLinearExprVariables.contains(trimmed);
    }

    private String normalizeFilterContent(String content) {
        if (content == null || !looksLikeFilterCondition(content)) {
            return content;
        }
        String normalized = content.replaceAll("\\s*==\\s*", "=")
                .replaceAll("(?<![!<>])\\s*=\\s*(?![=])", "=")
                .replaceAll("\\s*(>=|<=|>|<)\\s*", "$1");
        normalized = normalized.replaceAll("(=|>=|<=|>|<)'([^']*)'", "$1$2");
        return normalized;
    }

    private boolean looksLikeFilterCondition(String content) {
        return content != null
                && content.matches(".*[A-Za-z_$][A-Za-z0-9_$]*\\s*(==|=|>=|<=|>|<)\\s*.+");
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
        if (MARKDOWN_CODE_FENCE.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not contain markdown code fences");
        }
        if (FORBIDDEN_PACKAGE.matcher(methodBody).find()) {
            throw new RuleTransException("Generated rule method body must not use internal or OR-Tools packages");
        }
        if (sdkProfile == SdkProfile.POST && POST_FORBIDDEN_API.matcher(methodBody).find()) {
            throw new RuleTransException("POST rule method body must not use constraint-model APIs");
        }
        if (sdkProfile == SdkProfile.POST && POST_INVALID_AGGREGATE_API.matcher(methodBody).find()) {
            throw new RuleTransException(
                    "POST rule method body must use partCategory(code).sumQuantity() for quantity totals");
        }
        if (sdkProfile == SdkProfile.CONSTRAINT && CONSTRAINT_FORBIDDEN_API.matcher(methodBody).find()) {
            throw new RuleTransException("Constraint rule method body must not use POST instance-view APIs");
        }
        if (sdkProfile == SdkProfile.CONSTRAINT) {
            validateAggregateSdkUsage(methodBody);
        }
    }

    private void validateAggregateSdkUsage(String methodBody) {
        int index = 0;
        while (index < methodBody.length()) {
            int callStart = nextAggregateCall(methodBody, index);
            if (callStart < 0) {
                return;
            }
            int openParen = methodBody.indexOf('(', callStart);
            int closeParen = findMatchingParen(methodBody, openParen);
            if (closeParen < 0) {
                return;
            }
            String call = methodBody.substring(callStart, closeParen + 1);
            List<int[]> literals = stringLiteralRanges(call);
            if (literals.size() >= 3) {
                int[] categoryLiteral = literals.get(0);
                String categoryCodes = call.substring(categoryLiteral[0] + 1, categoryLiteral[1]);
                if (categoryCodes.contains("*") && categoryCodes.contains(",")) {
                    throw new RuleTransException(
                            "Aggregate SDK does not support mixing '*' and comma-separated categories in one call: "
                                    + categoryCodes);
                }
            }
            index = closeParen + 1;
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
