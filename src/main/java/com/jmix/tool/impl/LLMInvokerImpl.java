package com.jmix.tool.impl;

import com.jmix.tool.LLMInvoker;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

/**
 * LLM调用器实现类
 * 专门处理DeepSeek和QWen等LLM模型的调用
 * 
 * @since 2025-09-27
 */
@Slf4j
public class LLMInvokerImpl implements LLMInvoker {

    private final ChatLanguageModel llmModel;
    private final String modelName;

    /**
     * 构造函数，使用默认模型
     */
    public LLMInvokerImpl() {
        this("deepseek");
    }

    /**
     * 构造函数，使用指定的LLM模型
     * 
     * @param modelName 模型名称 (deepseek 或 qwen)
     */
    public LLMInvokerImpl(String modelName) {
        this.modelName = modelName;
        this.llmModel = createLLMModel();
    }

    @Override
    public String generate(String systemMessage, String userMessage) throws Exception {
        try {
            log.debug("Generating response with model: {}", modelName);
            log.debug("System message: {}", systemMessage);
            log.debug("User message: {}", userMessage);

            // 构建消息
            SystemMessage systemMsg = new SystemMessage(systemMessage);
            UserMessage userMsg = new UserMessage(userMessage);

            // 调用LLM模型
            Response<AiMessage> response = llmModel.generate(Arrays.asList(systemMsg, userMsg));

            String result = response.content().text();
            log.debug("Generated response length: {}", result.length());

            return result;

        } catch (Exception e) {
            log.error("Failed to generate response with model {}: {}", modelName, e.getMessage());
            throw new ModelGenneratorException("LLM generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfigInfo() {
        return String.format("LLM Model: %s, Provider: %s",
                modelName,
                "deepseek".equalsIgnoreCase(modelName) ? "DeepSeek" : "QWen");
    }

    /**
     * 创建LLM模型实例
     * 
     * @return LLM模型实例
     */
    private ChatLanguageModel createLLMModel() {
        Properties config = loadLLMConfig();

        if ("qwen".equalsIgnoreCase(modelName)) {
            return createQWenModel(config);
        } else {
            return createDeepSeekModel(config);
        }
    }

    /**
     * 创建DeepSeek模型
     * 
     * @param config 配置属性
     * @return DeepSeek模型实例
     */
    private ChatLanguageModel createDeepSeekModel(Properties config) {
        String apiKey = config.getProperty("llm.deepseek.apiKey", "your-deepseek-api-key");
        String baseUrl = config.getProperty("llm.deepseek.baseUrl", "https://api.deepseek.com/v1");
        String modelName = config.getProperty("llm.deepseek.modelName", "deepseek-chat");
        double temperature = Double.parseDouble(config.getProperty("llm.deepseek.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.deepseek.maxTokens", "4000"));

        log.info("Creating DeepSeek model: {} with baseUrl: {}", modelName, baseUrl);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMinutes(5))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建QWen模型
     * 
     * @param config 配置属性
     * @return QWen模型实例
     */
    private ChatLanguageModel createQWenModel(Properties config) {
        String apiKey = config.getProperty("llm.qwen.apiKey", "your-qwen-api-key");
        String baseUrl = config.getProperty("llm.qwen.baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        String modelName = config.getProperty("llm.qwen.modelName", "qwen-plus");
        double temperature = Double.parseDouble(config.getProperty("llm.qwen.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.qwen.maxTokens", "4000"));

        log.info("Creating QWen model: {} with baseUrl: {}", modelName, baseUrl);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMinutes(5))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 加载LLM相关配置
     * 
     * @return LLM配置属性
     */
    private Properties loadLLMConfig() {
        Properties config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("llmmodel.properties")) {
            if (input == null) {
                log.warn("LLM configuration file 'llmmodel.properties' not found, using default values");
                return getDefaultLLMConfig();
            }
            config.load(input);
            log.info("LLM configuration loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load LLM configuration file: {}", e.getMessage());
            return getDefaultLLMConfig();
        }
        return config;
    }

    /**
     * 获取默认LLM配置
     * 
     * @return 默认LLM配置属性
     */
    private Properties getDefaultLLMConfig() {
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("llm.deepseek.apiKey", "your-deepseek-api-key");
        defaultConfig.setProperty("llm.deepseek.baseUrl", "https://api.deepseek.com/v1");
        defaultConfig.setProperty("llm.deepseek.modelName", "deepseek-chat");
        defaultConfig.setProperty("llm.deepseek.temperature", "0.7");
        defaultConfig.setProperty("llm.deepseek.maxTokens", "4000");
        defaultConfig.setProperty("llm.qwen.apiKey", "your-qwen-api-key");
        defaultConfig.setProperty("llm.qwen.baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        defaultConfig.setProperty("llm.qwen.modelName", "qwen-plus");
        defaultConfig.setProperty("llm.qwen.temperature", "0.7");
        defaultConfig.setProperty("llm.qwen.maxTokens", "4000");
        return defaultConfig;
    }
}