package com.jmix.tool.impl;

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
        this.modelName = this.config.getProperty("default.model");
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
