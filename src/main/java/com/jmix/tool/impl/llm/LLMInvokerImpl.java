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

/**
 * LLM调用器实现类
 * 专门处理DeepSeek和QWen等LLM模型的调用
 * 
 * @since 2025-09-27
 */
@Slf4j
public class LLMInvokerImpl extends LLMInvokerBase implements LLMInvoker {

    private final ChatModel chatModel;

    /**
     * 构造函数，使用默认模型
     */
    public LLMInvokerImpl() {
        super();
        this.chatModel = createLLMModel();
    }

    @Override
    public String generate(String systemMessage, String userMessage) throws Exception {
        try {
            log.info("Generating response with model: {}", modelName);
            log.debug("System message: {}", systemMessage);
            log.debug("User message: {}", userMessage);

            // 构建消息
            SystemMessage systemMsg = new SystemMessage(systemMessage);
            UserMessage userMsg = new UserMessage(userMessage);

            // 构建消息列表
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(systemMsg);
            messages.add(userMsg);

            // 创建提示（模型名称已在创建 ChatModel 时设置）
            Prompt prompt = new Prompt(messages);

            // 调用LLM模型
            log.debug("Calling chatModel.call() with {} messages...", messages.size());
            ChatResponse response = chatModel.call(prompt);
            log.debug("Received response from chatModel");

            String result = response.getResult().getOutput().getText();
            log.info("Generated response length: {}", result.length());

            return result;

        } catch (Exception e) {
            log.error("Failed to generate response with model {}: {}", modelName, e.getMessage(), e);
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
    private ChatModel createLLMModel() {
        return chooseModel();
    }

    /**
     * 创建DeepSeek模型
     * 
     * @param config 配置属性
     * @return DeepSeek模型实例
     */
    private ChatModel createDeepSeekModel(Properties config) {
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("deepseek.api.key");
        }

        String baseUrl = System.getenv("DEEPSEEK_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("deepseek.api.base.url", "https://api.deepseek.com");
        }
        // 确保 baseUrl 格式正确：https://api.deepseek.com/v1
        // 移除末尾的斜杠
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // 如果 baseUrl 不包含 /v1，则添加
        if (!baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl + "/v1";
        }
        String modelName = config.getProperty("deepseek.model.name",
                config.getProperty("llm.deepseek.modelName", "deepseek-chat"));
        double temperature = Double.parseDouble(config.getProperty("llm.deepseek.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.deepseek.maxTokens", "4000"));

        log.info("Creating DeepSeek model: {} with baseUrl: {}, apiKey: {}",
                modelName, baseUrl,
                apiKey != null && !apiKey.isEmpty() ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4))
                        : "NOT SET");

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel(modelName);
        chatOptions.setTemperature(temperature);
        chatOptions.setMaxTokens(maxTokens);

        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        return chatModel;
    }

    /**
     * 创建QWen模型
     * 
     * @param config 配置属性
     * @return QWen模型实例
     */
    private ChatModel createQWenModel(Properties config) {
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("QWEN_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("qwen.api.key");
        }

        String baseUrl = System.getenv("QWEN_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("qwen.api.base.url", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        }
        // 确保 baseUrl 格式正确
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // Qwen 的 baseUrl 已经包含 /v1，不需要额外添加
        String modelName = config.getProperty("qwen.model.name",
                config.getProperty("llm.qwen.modelName", "qwen-plus"));
        double temperature = Double.parseDouble(config.getProperty("llm.qwen.temperature", "0.7"));
        int maxTokens = Integer.parseInt(config.getProperty("llm.qwen.maxTokens", "4000"));

        log.info("Creating QWen model: {} with baseUrl: {}, apiKey: {}",
                modelName, baseUrl,
                apiKey != null && !apiKey.isEmpty() ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4))
                        : "NOT SET");

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatOptions chatOptions = new OpenAiChatOptions();
        chatOptions.setModel(modelName);
        chatOptions.setTemperature(temperature);
        chatOptions.setMaxTokens(maxTokens);

        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        return chatModel;
    }

    private ChatModel chooseModel() {
        if ("qwen".equalsIgnoreCase(modelName)) {
            return createQWenModel(config);
        } else {
            return createDeepSeekModel(config);
        }
    }
}