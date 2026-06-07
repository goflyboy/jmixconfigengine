package com.jmix.tool.impl.llm;

import com.jmix.tool.impl.ModelGenneratorException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class LLMInvokerImpl extends LLMInvokerBase implements LLMInvoker {

    private static final String DEEPSEEK_COMPLETIONS_PATH = "/chat/completions";
    private static final String SOURCE_ENV = "environment variable";
    private static final String SOURCE_CONFIG = "configuration file";

    private final ChatModel chatModel;

    private String activeModelName;

    public LLMInvokerImpl() {
        super();
        this.chatModel = createLLMModel();
    }

    @Override
    public String generate(String systemMessage, String userMessage) {
        try {
            log.info("Generating response with model: {}", getActiveModelName());
            log.debug("System message: {}", systemMessage);
            log.debug("User message: {}", userMessage);

            SystemMessage systemMsg = new SystemMessage(systemMessage);
            UserMessage userMsg = new UserMessage(userMessage);

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(systemMsg);
            messages.add(userMsg);

            Prompt prompt = new Prompt(messages);

            log.debug("Calling chatModel.call() with {} messages...", messages.size());
            ChatResponse response = chatModel.call(prompt);
            log.debug("Received response from chatModel");

            String result = response.getResult().getOutput().getText();
            if (result == null || result.isBlank()) {
                throw new ModelGenneratorException("LLM returned empty response");
            }
            log.info("Generated response length: {}", result.length());

            return result;

        } catch (Exception e) {
            log.error("Failed to generate response with model {}: {}", getActiveModelName(), e.getMessage(), e);
            throw new ModelGenneratorException("LLM generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfigInfo() {
        return String.format("LLM Model: %s, Provider: %s",
                getActiveModelName(),
                "qwen".equalsIgnoreCase(modelName) ? "QWen" : "DeepSeek");
    }

    private ChatModel createLLMModel() {
        return chooseModel();
    }

    private ChatModel createDeepSeekModel(Properties config) {
        ConfigValue apiKey = firstConfigured("DEEPSEEK_API_KEY",
                "deepseek.api.key",
                "llm.deepseek.apiKey");
        if (apiKey.value() == null) {
            throw new ModelGenneratorException("DEEPSEEK_API_KEY is not configured");
        }

        String baseUrl = firstConfiguredOrDefault(
                "DEEPSEEK_API_BASE_URL",
                "https://api.deepseek.com",
                "deepseek.api.base.url",
                "llm.deepseek.baseUrl");
        baseUrl = normalizeBaseUrl(baseUrl);
        if (baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
        }

        String configuredModelName = firstConfiguredOrDefault(
                "DEEPSEEK_MODEL_NAME",
                "deepseek-v4-pro",
                "deepseek.model.name",
                "llm.deepseek.modelName");
        activeModelName = configuredModelName;

        double temperature = Double.parseDouble(config.getProperty("llm.deepseek.temperature", "0.3"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.deepseek.maxTokens", "4000"));

        log.info("Creating DeepSeek model: {} with baseUrl: {}, apiKey: {} ({})",
                configuredModelName, baseUrl, maskApiKey(apiKey.value()), apiKey.source());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey.value())
                .baseUrl(baseUrl)
                .completionsPath(DEEPSEEK_COMPLETIONS_PATH)
                .build();

        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel(configuredModelName);
        chatOptions.setTemperature(temperature);
        chatOptions.setMaxTokens(maxTokens);

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    private ChatModel createQWenModel(Properties config) {
        ConfigValue apiKey = firstConfigured("QWEN_API_KEY",
                "qwen.api.key",
                "llm.qwen.apiKey");
        if (apiKey.value() == null) {
            throw new ModelGenneratorException("QWEN_API_KEY is not configured");
        }

        String baseUrl = firstConfiguredOrDefault(
                "QWEN_API_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen.api.base.url",
                "llm.qwen.baseUrl");
        baseUrl = normalizeBaseUrl(baseUrl);

        String configuredModelName = firstConfiguredOrDefault(
                "QWEN_MODEL_NAME",
                "qwen-plus",
                "qwen.model.name",
                "llm.qwen.modelName");
        activeModelName = configuredModelName;

        double temperature = Double.parseDouble(config.getProperty("llm.qwen.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.qwen.maxTokens", "4000"));

        log.info("Creating QWen model: {} with baseUrl: {}, apiKey: {} ({})",
                configuredModelName, baseUrl, maskApiKey(apiKey.value()), apiKey.source());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey.value())
                .baseUrl(baseUrl)
                .build();

        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel(configuredModelName);
        chatOptions.setTemperature(temperature);
        chatOptions.setMaxTokens(maxTokens);

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    private ChatModel chooseModel() {
        if ("qwen".equalsIgnoreCase(modelName)) {
            return createQWenModel(config);
        }
        return createDeepSeekModel(config);
    }

    private String getActiveModelName() {
        return activeModelName == null || activeModelName.isBlank() ? modelName : activeModelName;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private ConfigValue firstConfigured(String envName, String... propertyNames) {
        ConfigValue envValue = configuredFromEnv(envName);
        if (envValue.value() != null) {
            return envValue;
        }
        for (String propertyName : propertyNames) {
            ConfigValue configValue = configuredFromProperty(propertyName);
            if (configValue.value() != null) {
                return configValue;
            }
        }
        return new ConfigValue(null, "");
    }

    private String firstConfiguredOrDefault(String envName, String defaultValue, String... propertyNames) {
        ConfigValue configured = firstConfigured(envName, propertyNames);
        if (configured.value() != null) {
            return configured.value();
        }
        return defaultValue;
    }

    private ConfigValue configuredFromEnv(String envName) {
        return new ConfigValue(normalizeConfiguredValue(System.getenv(envName)), SOURCE_ENV + " " + envName);
    }

    private ConfigValue configuredFromProperty(String propertyName) {
        return new ConfigValue(
                normalizeConfiguredValue(config.getProperty(propertyName)),
                SOURCE_CONFIG + " " + propertyName);
    }

    private String normalizeConfiguredValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isEmpty() || isPlaceholderValue(value)) {
            return null;
        }
        String envName = envPlaceholderName(value);
        if (envName != null) {
            return normalizeConfiguredValue(System.getenv(envName));
        }
        return value;
    }

    private boolean isPlaceholderValue(String value) {
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

    private String maskApiKey(String apiKey) {
        return "***" + apiKey.substring(Math.max(0, apiKey.length() - 4));
    }

    private record ConfigValue(String value, String source) {
    }
}
