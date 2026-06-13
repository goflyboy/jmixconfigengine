package com.jmix.tool.impl.llm;

/**
 * Resolved model profile for an OpenAI-compatible LLM provider.
 */
public record LlmModelProfile(
        String tag,
        String provider,
        String apiKeyEnvName,
        String baseUrl,
        String modelName,
        double temperature,
        int maxTokens,
        String completionsPath) {

    public LlmModelProfile {
        tag = requireText(tag, "tag");
        provider = requireText(provider, "provider");
        apiKeyEnvName = requireText(apiKeyEnvName, "apiKeyEnvName");
        baseUrl = requireText(baseUrl, "baseUrl");
        modelName = requireText(modelName, "modelName");
        completionsPath = completionsPath == null ? "" : completionsPath.trim();
    }

    public String identity() {
        return provider + ":" + modelName + "@" + baseUrl;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
