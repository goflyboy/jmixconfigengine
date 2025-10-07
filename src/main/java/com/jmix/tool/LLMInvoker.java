package com.jmix.tool;

/**
 * LLM调用接口
 * 提供统一的LLM调用抽象，不依赖具体的第三方实现
 * 
 * @since 2025-09-27
 */
public interface LLMInvoker {

    /**
     * 调用LLM生成内容
     * 
     * @param systemMessage 系统消息
     * @param userMessage   用户消息
     * @return LLM生成的响应内容
     * @throws Exception 当LLM调用失败时抛出异常
     */
    String generate(String systemMessage, String userMessage) throws Exception;

    /**
     * 获取当前配置信息
     * 
     * @return 配置信息字符串
     */
    String getConfigInfo();
}
