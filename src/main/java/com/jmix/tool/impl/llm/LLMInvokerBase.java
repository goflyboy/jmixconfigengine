package com.jmix.tool.impl.llm;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * LLM调用器基类
 * 承载通用的配置加载逻辑
 */
@Slf4j
public abstract class LLMInvokerBase {

    protected final Properties config;

    protected final String modelName;

    protected LLMInvokerBase() {
        this.config = loadLLMConfig();
        this.modelName = normalizedOrDefault(this.config.getProperty("default.model"), "deepseek");
    }

    /**
     * 加载 LLM 相关配置
     */
    protected Properties loadLLMConfig() {
        Properties cfg = new Properties();

        // 1) 优先从 cengine 目录加载
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("cengine/llmmodel.properties")) {
            if (input != null) {
                cfg.load(input);
                log.info("LLM configuration loaded from cengine/llmmodel.properties");
                return cfg;
            }
        } catch (IOException e) {
            log.warn("Failed to load from cengine/llmmodel.properties: {}", e.getMessage());
        }

        // 3) 最终使用默认配置
        log.warn("LLM configuration file not found, using default values");
        return getDefaultLLMConfig();
    }

    /**
     * 默认配置
     */
    protected Properties getDefaultLLMConfig() {
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("default.model", "deepseek");
        defaultConfig.setProperty("llm.deepseek.apiKey", "${DEEPSEEK_API_KEY}");
        defaultConfig.setProperty("llm.deepseek.baseUrl", "https://api.deepseek.com");
        defaultConfig.setProperty("llm.deepseek.modelName", "deepseek-v4-pro");
        defaultConfig.setProperty("llm.deepseek.temperature", "0.7");
        defaultConfig.setProperty("llm.deepseek.maxTokens", "4000");
        defaultConfig.setProperty("llm.qwen.apiKey", "${QWEN_API_KEY}");
        defaultConfig.setProperty("llm.qwen.baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        defaultConfig.setProperty("llm.qwen.modelName", "qwen-plus");
        defaultConfig.setProperty("llm.qwen.temperature", "0.7");
        defaultConfig.setProperty("llm.qwen.maxTokens", "4000");
        return defaultConfig;
    }

    private String normalizedOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
