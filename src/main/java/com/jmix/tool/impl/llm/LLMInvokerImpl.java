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

@Slf4j
public class LLMInvokerImpl extends LLMInvokerBase implements LLMInvoker {

    private final LlmModelProfile profile;

    private ChatModel chatModel;

    private String activeModelName;

    public LLMInvokerImpl() {
        super();
        this.profile = LlmModelRegistry.from(config).resolve(modelName);
        this.activeModelName = profile.modelName();
    }

    public LLMInvokerImpl(LlmModelProfile profile) {
        super(profile == null ? null : profile.tag());
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        this.profile = profile;
        this.activeModelName = profile.modelName();
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
            ChatResponse response = chatModel().call(prompt);
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
        return String.format("LLM Model: %s, Provider: %s, Tag: %s, Base URL: %s",
                getActiveModelName(),
                profile.provider(),
                profile.tag(),
                profile.baseUrl());
    }

    private ChatModel createLLMModel(LlmModelProfile resolvedProfile) {
        if (!"openai-compatible".equals(resolvedProfile.provider())) {
            throw new ModelGenneratorException("Unsupported LLM provider: " + resolvedProfile.provider());
        }
        String apiKey = normalizeApiKey(System.getenv(resolvedProfile.apiKeyEnvName()));
        if (apiKey == null) {
            throw new ModelGenneratorException(
                    "Environment variable " + resolvedProfile.apiKeyEnvName() + " is not configured");
        }

        String baseUrl = normalizeBaseUrl(resolvedProfile.baseUrl());
        activeModelName = resolvedProfile.modelName();

        log.info("Creating LLM model tag={} model={} provider={} baseUrl={} apiKeyEnv={}",
                resolvedProfile.tag(),
                resolvedProfile.modelName(),
                resolvedProfile.provider(),
                baseUrl,
                resolvedProfile.apiKeyEnvName());

        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl);
        if (!resolvedProfile.completionsPath().isBlank()) {
            apiBuilder.completionsPath(resolvedProfile.completionsPath());
        }
        OpenAiApi openAiApi = apiBuilder.build();

        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel(resolvedProfile.modelName());
        chatOptions.setTemperature(resolvedProfile.temperature());
        chatOptions.setMaxTokens(resolvedProfile.maxTokens());

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    private synchronized ChatModel chatModel() {
        if (chatModel == null) {
            chatModel = createLLMModel(profile);
        }
        return chatModel;
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

    private String normalizeApiKey(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        return rawValue.trim();
    }
}
