package com.jmix.tool.impl.llm;

import com.jmix.tool.impl.ModelGenneratorException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads model profiles from cengine/llmmodel.properties with old-key compatibility.
 */
@Slf4j
public final class LlmModelRegistry {

    private static final String DEFAULT_PROVIDER = "openai-compatible";
    private static final String DEFAULT_COMPLETIONS_PATH = "/chat/completions";

    private final Properties properties;

    private LlmModelRegistry(Properties properties) {
        this.properties = copyOf(properties);
        warnIgnoredSecretProperties();
    }

    public static LlmModelRegistry load() {
        return new LlmModelRegistry(loadProperties());
    }

    public static LlmModelRegistry from(Properties properties) {
        return new LlmModelRegistry(properties);
    }

    public LlmModelProfile resolve(String requestedTag) {
        String tag = normalizeTag(requestedTag);
        if ("default".equals(tag)) {
            tag = defaultModelTag();
        }
        if (!availableTags().contains(tag)) {
            throw new ModelGenneratorException(
                    "Unknown LLM model tag: " + tag + ". Available tags: " + availableTags());
        }
        return profileFor(tag);
    }

    public Set<String> availableTags() {
        Set<String> tags = new TreeSet<>();
        tags.add("deepseek");
        tags.add("qwen");
        for (String name : properties.stringPropertyNames()) {
            String prefix = "llm.model.";
            if (!name.startsWith(prefix)) {
                continue;
            }
            String rest = name.substring(prefix.length());
            int dot = rest.indexOf('.');
            if (dot > 0) {
                tags.add(rest.substring(0, dot));
            }
        }
        if (hasOldModelConfig("glm")) {
            tags.add("glm");
        }
        return Set.copyOf(tags);
    }

    Properties properties() {
        return copyOf(properties);
    }

    private LlmModelProfile profileFor(String tag) {
        String provider = propertyOrDefault("llm.model." + tag + ".provider", DEFAULT_PROVIDER);
        String apiKeyEnv = propertyOrDefault("llm.model." + tag + ".apiKeyEnv", defaultApiKeyEnv(tag));
        String baseUrl = propertyOrDefault(
                "llm.model." + tag + ".baseUrl",
                defaultBaseUrl(tag),
                tag + ".api.base.url",
                "llm." + tag + ".baseUrl");
        String modelName = propertyOrDefault(
                "llm.model." + tag + ".modelName",
                defaultModelName(tag),
                tag + ".model.name",
                "llm." + tag + ".modelName");
        String temperature = propertyOrDefault(
                "llm.model." + tag + ".temperature",
                defaultTemperature(tag),
                "llm." + tag + ".temperature");
        String maxTokens = propertyOrDefault(
                "llm.model." + tag + ".maxTokens",
                "4000",
                "llm." + tag + ".maxTokens");
        String completionsPath = propertyOrDefault(
                "llm.model." + tag + ".completionsPath",
                DEFAULT_COMPLETIONS_PATH,
                "llm." + tag + ".completionsPath");
        return new LlmModelProfile(
                tag,
                provider,
                apiKeyEnv,
                baseUrl,
                modelName,
                parseDouble(temperature, tag, "temperature"),
                parseInt(maxTokens, tag, "maxTokens"),
                completionsPath);
    }

    private String defaultModelTag() {
        String tag = configured("llm.default.model");
        if (tag == null) {
            tag = configured("default.model");
        }
        return tag == null ? "deepseek" : tag;
    }

    private String normalizeTag(String requestedTag) {
        if (requestedTag == null || requestedTag.trim().isEmpty()) {
            return "default";
        }
        return requestedTag.trim();
    }

    private String propertyOrDefault(String propertyName, String defaultValue, String... fallbackNames) {
        String value = configured(propertyName);
        if (value != null) {
            return value;
        }
        for (String fallbackName : fallbackNames) {
            value = configured(fallbackName);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    private String configured(String propertyName) {
        String rawValue = properties.getProperty(propertyName);
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isEmpty() || isSecretPlaceholder(value)) {
            return null;
        }
        String envName = envPlaceholderName(value);
        if (envName != null) {
            String envValue = System.getenv(envName);
            return envValue == null || envValue.trim().isEmpty() ? null : envValue.trim();
        }
        return value;
    }

    private boolean hasOldModelConfig(String tag) {
        return configured(tag + ".api.base.url") != null
                || configured(tag + ".model.name") != null
                || configured("llm." + tag + ".baseUrl") != null
                || configured("llm." + tag + ".modelName") != null;
    }

    private String defaultApiKeyEnv(String tag) {
        return tag.toUpperCase() + "_API_KEY";
    }

    private String defaultBaseUrl(String tag) {
        return switch (tag) {
            case "deepseek" -> "https://api.deepseek.com";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    private String defaultModelName(String tag) {
        return switch (tag) {
            case "deepseek" -> "deepseek-v4-pro";
            case "qwen" -> "qwen-plus";
            default -> tag;
        };
    }

    private String defaultTemperature(String tag) {
        return "deepseek".equals(tag) ? "0.3" : "0.7";
    }

    private double parseDouble(String value, String tag, String fieldName) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ModelGenneratorException(
                    "Invalid LLM profile " + tag + " " + fieldName + ": " + value, e);
        }
    }

    private int parseInt(String value, String tag, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ModelGenneratorException(
                    "Invalid LLM profile " + tag + " " + fieldName + ": " + value, e);
        }
    }

    private void warnIgnoredSecretProperties() {
        for (String name : properties.stringPropertyNames()) {
            String lower = name.toLowerCase();
            if (lower.endsWith("apikey") || lower.endsWith(".api.key")
                    || lower.endsWith(".sk") || lower.endsWith(".secretkey")) {
                log.warn("Ignoring LLM secret-like configuration property: {}", name);
            }
        }
    }

    private boolean isSecretPlaceholder(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("your-")
                || lower.startsWith("your_")
                || lower.endsWith("-api-key")
                || lower.endsWith("_api_key");
    }

    private String envPlaceholderName(String value) {
        if (!value.startsWith("${") || !value.endsWith("}")) {
            return null;
        }
        String envName = value.substring(2, value.length() - 1).trim();
        return envName.isEmpty() ? null : envName;
    }

    private static Properties loadProperties() {
        Properties cfg = new Properties();
        try (InputStream input = LlmModelRegistry.class.getClassLoader()
                .getResourceAsStream("cengine/llmmodel.properties")) {
            if (input != null) {
                cfg.load(input);
                log.info("LLM configuration loaded from cengine/llmmodel.properties");
                return cfg;
            }
        } catch (IOException e) {
            log.warn("Failed to load cengine/llmmodel.properties: {}", e.getMessage());
        }
        log.warn("LLM configuration file not found, using default model profiles");
        return defaultProperties();
    }

    private static Properties defaultProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("llm.default.model", "deepseek");
        defaults.setProperty("llm.model.deepseek.provider", DEFAULT_PROVIDER);
        defaults.setProperty("llm.model.deepseek.apiKeyEnv", "DEEPSEEK_API_KEY");
        defaults.setProperty("llm.model.deepseek.baseUrl", "https://api.deepseek.com");
        defaults.setProperty("llm.model.deepseek.modelName", "deepseek-v4-pro");
        defaults.setProperty("llm.model.deepseek.temperature", "0.3");
        defaults.setProperty("llm.model.deepseek.maxTokens", "4000");
        defaults.setProperty("llm.model.deepseek.completionsPath", DEFAULT_COMPLETIONS_PATH);
        defaults.setProperty("llm.model.qwen.provider", DEFAULT_PROVIDER);
        defaults.setProperty("llm.model.qwen.apiKeyEnv", "QWEN_API_KEY");
        defaults.setProperty("llm.model.qwen.baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        defaults.setProperty("llm.model.qwen.modelName", "qwen-plus");
        defaults.setProperty("llm.model.qwen.temperature", "0.7");
        defaults.setProperty("llm.model.qwen.maxTokens", "4000");
        defaults.setProperty("llm.model.qwen.completionsPath", DEFAULT_COMPLETIONS_PATH);
        return defaults;
    }

    private static Properties copyOf(Properties source) {
        Properties copy = new Properties();
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }
}
