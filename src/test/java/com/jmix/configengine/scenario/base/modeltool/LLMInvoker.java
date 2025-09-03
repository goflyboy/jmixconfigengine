package com.jmix.configengine.scenario.base.modeltool;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * LLM调用框架
 * 支持DeepSeek和Qwen两种模型
 * 兼容langchain4j 1.3.0
 */
public class LLMInvoker {
    
    private final Properties config;
    private final ChatLanguageModel llmModel;
    private final String defaultModel;
    
    public LLMInvoker() {
        this.config = loadConfig();
        this.defaultModel = config.getProperty("default.model", "deepseek");
        if(this.defaultModel.equals("deepseek")){
            this.llmModel = createDeepSeekModel();
        }else{
            this.llmModel = createQwenModel();
        }
    }
    

    /**
     * 生成模型代码（使用默认模型和默认包名）
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCode(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        return generatorModelCodeWithPackage("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成模型代码（使用默认模型，指定包名）
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCode(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        return generatorModelCodeWithPackage(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成模型代码（包含用户测试用例特殊规格）
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec 用户测试用例特殊规格
     * @return 生成的Java代码
     */
    public String generatorModelCode(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        return generatorModelCodeWithPackage(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, userTestCaseSpec);
    }
    
    /**
     * 使用指定包名生成代码
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCodeWithPackage(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        return generatorModelCodeWithPackage(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 使用指定包名生成代码（包含用户测试用例特殊规格）
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec 用户测试用例特殊规格
     * @return 生成的Java代码
     */
    public String generatorModelCodeWithPackage(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            String prompt = buildPromptWithPackage(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, userTestCaseSpec);
            
            Response<AiMessage> response = llmModel.generate(
                Arrays.asList(
                    new SystemMessage("你是一个专业的Java开发工程师，擅长根据用户需求生成高质量的Java代码。"),
                    new UserMessage(prompt)
                )
            );
            // 清理生成的代码，移除Markdown格式标记
            String rawCode = response.content().text();
            return cleanGeneratedCode(rawCode);
            
        } catch (Exception e) {
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }
    
    
    /**
     * 构建带包名的prompt模板
     */
    private String buildPromptWithPackage(String packageName, String modelName, String userVariableModel, String userLogicByPseudocode) {
        return PromptTemplateLoader.renderJavaCodeTemplate(packageName, modelName, userVariableModel, userLogicByPseudocode);
    }
    
    /**
     * 构建带包名的prompt模板（包含用户测试用例特殊规格）
     */
    private String buildPromptWithPackage(String packageName, String modelName, String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        return PromptTemplateLoader.renderJavaCodeTemplate(packageName, modelName, userVariableModel, userLogicByPseudocode, userTestCaseSpec);
    }
    
    /**
     * 清理生成的代码，移除Markdown格式标记
     * @param rawCode 原始生成的代码
     * @return 清理后的纯Java代码
     */
    private String cleanGeneratedCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return rawCode;
        }
        
        String cleanedCode = rawCode;
        
        // 移除Markdown代码块标记
        cleanedCode = cleanedCode.replaceAll("```java\\s*", "");
        cleanedCode = cleanedCode.replaceAll("```\\s*", "");
        
        // 移除可能的语言标识
        cleanedCode = cleanedCode.replaceAll("```\\w+\\s*", "");
        
        // 移除开头和结尾的空白字符
        cleanedCode = cleanedCode.trim();
        
        return cleanedCode;
    }
    
    /**
     * 创建DeepSeek模型
     */
    private ChatLanguageModel createDeepSeekModel() {
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("deepseek.api.key");
        }
        
        String baseUrl = System.getenv("DEEPSEEK_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("deepseek.api.base.url");
        }
        
        String modelName = System.getenv("DEEPSEEK_MODEL_NAME");
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = config.getProperty("deepseek.model.name");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty() || "your_deepseek_api_key_here".equals(apiKey)) {
            throw new IllegalStateException("请配置有效的DeepSeek API密钥，可通过环境变量DEEPSEEK_API_KEY或配置文件设置");
        }
        
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .maxTokens(4000)
            .temperature(0.1)
            .timeout(Duration.ofMinutes(5))
            .build();
    }
    
    /**
     * 创建Qwen模型
     */
    private ChatLanguageModel createQwenModel() {
        // 优先从环境变量读取，然后从配置文件读取
        String apiKey = System.getenv("QWEN_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = config.getProperty("qwen.api.key");
        }
        
        String baseUrl = System.getenv("QWEN_API_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = config.getProperty("qwen.api.base.url");
        }
        
        String modelName = System.getenv("QWEN_MODEL_NAME");
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = config.getProperty("qwen.model.name");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty() || "your_qwen_api_key_here".equals(apiKey)) {
            throw new IllegalStateException("请配置有效的Qwen API密钥，可通过环境变量QWEN_API_KEY或配置文件设置");
        }
        
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .maxTokens(4000)
            .temperature(0.1)
            .timeout(Duration.ofMinutes(5))
            .build();
    }
    
    /**
     * 加载配置文件
     */
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("llmmodel.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("无法找到配置文件 llmmodel.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败: " + e.getMessage(), e);
        }
        return props;
    }
    
    /**
     * 获取当前配置信息
     */
    public String getConfigInfo() {
        String deepseekKey = System.getenv("DEEPSEEK_API_KEY");
        if (deepseekKey == null || deepseekKey.trim().isEmpty()) {
            deepseekKey = config.getProperty("deepseek.api.key", "未配置");
        }
        
        String qwenKey = System.getenv("QWEN_API_KEY");
        if (qwenKey == null || qwenKey.trim().isEmpty()) {
            qwenKey = config.getProperty("qwen.api.key", "未配置");
        }
        
        return String.format("当前配置:\n" +
            "- 默认模型: %s\n" +
            "- DeepSeek API: %s (来源: %s)\n" +
            "- Qwen API: %s (来源: %s)", 
            defaultModel,
            deepseekKey.substring(0, Math.min(10, deepseekKey.length())) + "...",
            System.getenv("DEEPSEEK_API_KEY") != null ? "环境变量" : "配置文件",
            qwenKey.substring(0, Math.min(10, qwenKey.length())) + "...",
            System.getenv("QWEN_API_KEY") != null ? "环境变量" : "配置文件"
        );
    }
}