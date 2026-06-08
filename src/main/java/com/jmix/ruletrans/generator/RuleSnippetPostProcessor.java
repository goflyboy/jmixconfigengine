package com.jmix.ruletrans.generator;

import com.jmix.ruletrans.RuleTransException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extracts and validates generated rule method snippets.
 */
public final class RuleSnippetPostProcessor {

    private static final Pattern PACKAGE_OR_IMPORT = Pattern.compile("(?m)^\\s*(package|import)\\s+");
    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("(?m)^\\s*(public\\s+|final\\s+|abstract\\s+)*class\\s+");

    public String process(String rawResponse) {
        String snippet = normalizeSnippet(extractCode(rawResponse));
        validateSnippet(snippet);
        return snippet;
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

    private String normalizeSnippet(String snippet) {
        return removeCodeRuleAnnoCodeAttribute(snippet);
    }

    private String removeCodeRuleAnnoCodeAttribute(String snippet) {
        String marker = "@CodeRuleAnno";
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (cursor < snippet.length()) {
            int annotationStart = snippet.indexOf(marker, cursor);
            if (annotationStart < 0) {
                result.append(snippet.substring(cursor));
                break;
            }
            result.append(snippet, cursor, annotationStart);
            int markerEnd = annotationStart + marker.length();
            int argsStart = skipWhitespace(snippet, markerEnd);
            if (argsStart >= snippet.length() || snippet.charAt(argsStart) != '(') {
                result.append(marker);
                cursor = markerEnd;
                continue;
            }
            int argsEnd = findMatchingParen(snippet, argsStart);
            if (argsEnd < 0) {
                result.append(snippet.substring(annotationStart));
                break;
            }
            result.append(marker).append(normalizeCodeRuleAnnoArgs(snippet.substring(argsStart + 1, argsEnd)));
            cursor = argsEnd + 1;
        }
        return result.toString();
    }

    private String normalizeCodeRuleAnnoArgs(String args) {
        List<String> retained = new ArrayList<>();
        for (String arg : splitAnnotationArgs(args)) {
            String trimmed = arg.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("code\\s*=.*")) {
                retained.add(trimmed);
            }
        }
        if (retained.isEmpty()) {
            return "";
        }
        return "(" + String.join(", ", retained) + ")";
    }

    private List<String> splitAnnotationArgs(String args) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int depth = 0;
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
                continue;
            }
            if (ch == '(' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == '}') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                parts.add(args.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(args.substring(start));
        return parts;
    }

    private int skipWhitespace(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private int findMatchingParen(String value, int openIndex) {
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

    private void validateSnippet(String snippet) {
        if (snippet == null || snippet.trim().isEmpty()) {
            throw new RuleTransException("Generated rule snippet is blank");
        }
        if (PACKAGE_OR_IMPORT.matcher(snippet).find()) {
            throw new RuleTransException("Generated rule snippet must not contain package/import declarations");
        }
        if (CLASS_DECLARATION.matcher(snippet).find()) {
            throw new RuleTransException("Generated rule snippet must not contain a class declaration");
        }
        if (!snippet.contains("@CodeRuleAnno")) {
            throw new RuleTransException("Generated rule snippet must contain @CodeRuleAnno");
        }
    }
}
