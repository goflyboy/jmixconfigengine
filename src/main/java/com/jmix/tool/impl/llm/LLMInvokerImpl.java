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
        String apiKey = firstNonBlank(
                System.getenv("DEEPSEEK_API_KEY"),
                config.getProperty("deepseek.api.key"),
                config.getProperty("llm.deepseek.apiKey"));
        if (apiKey == null) {
            throw new ModelGenneratorException("DEEPSEEK_API_KEY is not configured");
        }

        String baseUrl = firstNonBlank(
                System.getenv("DEEPSEEK_API_BASE_URL"),
                config.getProperty("deepseek.api.base.url"),
                config.getProperty("llm.deepseek.baseUrl"),
                "https://api.deepseek.com");
        baseUrl = normalizeBaseUrl(baseUrl);
        if (baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
        }

        String configuredModelName = firstNonBlank(
                System.getenv("DEEPSEEK_MODEL_NAME"),
                config.getProperty("deepseek.model.name"),
                config.getProperty("llm.deepseek.modelName"),
                "deepseek-v4-pro");
        activeModelName = configuredModelName;

        double temperature = Double.parseDouble(config.getProperty("llm.deepseek.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.deepseek.maxTokens", "4000"));

        log.info("Creating DeepSeek model: {} with baseUrl: {}, apiKey: {}",
                configuredModelName, baseUrl, maskApiKey(apiKey));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
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
        String apiKey = firstNonBlank(
                System.getenv("QWEN_API_KEY"),
                config.getProperty("qwen.api.key"),
                config.getProperty("llm.qwen.apiKey"));
        if (apiKey == null) {
            throw new ModelGenneratorException("QWEN_API_KEY is not configured");
        }

        String baseUrl = firstNonBlank(
                System.getenv("QWEN_API_BASE_URL"),
                config.getProperty("qwen.api.base.url"),
                config.getProperty("llm.qwen.baseUrl"),
                "https://dashscope.aliyuncs.com/compatible-mode/v1");
        baseUrl = normalizeBaseUrl(baseUrl);

        String configuredModelName = firstNonBlank(
                System.getenv("QWEN_MODEL_NAME"),
                config.getProperty("qwen.model.name"),
                config.getProperty("llm.qwen.modelName"),
                "qwen-plus");
        activeModelName = configuredModelName;

        double temperature = Double.parseDouble(config.getProperty("llm.qwen.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.qwen.maxTokens", "4000"));

        log.info("Creating QWen model: {} with baseUrl: {}, apiKey: {}",
                configuredModelName, baseUrl, maskApiKey(apiKey));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String maskApiKey(String apiKey) {
        return "***" + apiKey.substring(Math.max(0, apiKey.length() - 4));
    }
}
