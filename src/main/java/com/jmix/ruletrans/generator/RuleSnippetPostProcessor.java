package com.jmix.ruletrans.generator;

import com.jmix.ruletrans.RuleTransException;

import java.util.regex.Pattern;

/**
 * Extracts and validates generated rule method snippets.
 */
public final class RuleSnippetPostProcessor {

    private static final Pattern PACKAGE_OR_IMPORT = Pattern.compile("(?m)^\\s*(package|import)\\s+");
    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("(?m)^\\s*(public\\s+|final\\s+|abstract\\s+)*class\\s+");

    public String process(String rawResponse) {
        String snippet = extractCode(rawResponse);
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
