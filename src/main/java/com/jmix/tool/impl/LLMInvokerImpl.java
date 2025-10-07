package com.jmix.tool.impl;

import com.jmix.tool.LLMInvoker;

import lombok.extern.slf4j.Slf4j;

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
public class LLMInvokerImpl extends LLMInvokerBase implements LLMInvoker {

    private final ChatLanguageModel llmModel;

    /**
     * 构造函数，使用默认模型
     */
    public LLMInvokerImpl() {
        super();
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
        return chooseModel();
    }

    /**
     * 创建DeepSeek模型
     * 
     * @param config 配置属性
     * @return DeepSeek模型实例
     */
    private ChatLanguageModel createDeepSeekModel(Properties config) {
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("deepseek.api.key");
        }

        String baseUrl = System.getenv("DEEPSEEK_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("deepseek.api.base.url");
        }
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
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("QWEN_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("qwen.api.key");
        }

        String baseUrl = System.getenv("QWEN_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("qwen.api.base.url");
        }
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

    private ChatLanguageModel chooseModel() {
        if ("qwen".equalsIgnoreCase(modelName)) {
            return createQWenModel(config);
        } else {
            return createDeepSeekModel(config);
        }
    }
}