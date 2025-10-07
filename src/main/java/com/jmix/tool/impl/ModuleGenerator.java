package com.jmix.tool.impl;

import com.jmix.tool.LLMInvoker;

import lombok.extern.slf4j.Slf4j;

/**
 * 模块代码生成器
 * 基于LLM生成Java代码，支持多种模型
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleGenerator {

    private final LLMInvoker llmInvoker;

    /**
     * 构造函数，初始化LLM调用器
     */
    public ModuleGenerator() {
        this.llmInvoker = new LLMInvokerImpl();
    }

    /**
     * 构造函数，使用指定的LLM调用器
     * 
     * @param llmInvoker LLM调用器实例
     */
    public ModuleGenerator(LLMInvoker llmInvoker) {
        this.llmInvoker = llmInvoker;
    }

    /**
     * 生成模型代码（使用默认模型和默认包名）
     * 
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCode(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        return generatorModelCodeWithPackage(
                "com.jmix.configengine.scenario.ruletest",
                modelScenarioName,
                userVariableModel,
                userLogicByPseudocode,
                "");
    }

    /**
     * 生成模型代码（使用默认模型，指定包名）
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCode(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        return generatorModelCodeWithPackage(
                packageName,
                modelScenarioName,
                userVariableModel,
                userLogicByPseudocode,
                "");
    }

    /**
     * 生成模型代码（包含用户测试用例特殊规格）
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec      用户测试用例特殊规格
     * @return 生成的Java代码
     */
    public String generatorModelCode(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode, String userTestCaseSpec) {
        return generatorModelCodeWithPackage(
                packageName,
                modelScenarioName,
                userVariableModel,
                userLogicByPseudocode,
                userTestCaseSpec);
    }

    /**
     * 使用指定包名生成代码
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 生成的Java代码
     */
    public String generatorModelCodeWithPackage(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        return generatorModelCodeWithPackage(
                packageName,
                modelScenarioName,
                userVariableModel,
                userLogicByPseudocode,
                "");
    }

    /**
     * 使用指定包名生成代码（包含用户测试用例特殊规格）
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec      用户测试用例特殊规格
     * @return 生成的Java代码
     */
    public String generatorModelCodeWithPackage(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            String prompt = buildPromptWithPackage(packageName, modelScenarioName, userVariableModel,
                    userLogicByPseudocode, userTestCaseSpec);

            String systemMessage = "你是一个专业的Java开发工程师，擅长根据用户需求生成高质量的Java代码。";
            String rawCode = llmInvoker.generate(systemMessage, prompt);

            // 后处理生成的代码
            return postProcessCode(rawCode);

        } catch (Exception e) {
            log.error("Failed to generate model code", e);
            throw new ModelGenneratorException("LLM invocation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建带包名的prompt模板（包含用户测试用例特殊规格）
     * 
     * @param packageName           包名
     * @param modelName             模型名称
     * @param userVariableModel     用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec      用户测试用例特殊规格
     * @return 构建的prompt字符串
     */
    private String buildPromptWithPackage(String packageName, String modelName, String userVariableModel,
            String userLogicByPseudocode, String userTestCaseSpec) {
        return PromptTemplateLoader.renderJavaCodeTemplate(packageName, modelName, userVariableModel,
                userLogicByPseudocode, userTestCaseSpec);
    }

    /**
     * 获取当前配置信息
     */
    public String getConfigInfo() {
        return llmInvoker.getConfigInfo();
    }

    /**
     * 清理生成的代码
     * 移除不必要的注释、空行和格式化问题
     * 
     * @param code 原始代码
     * @return 清理后的代码
     */
    public String cleanGeneratedCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }

        log.debug("Cleaning generated code, original length: {}", code.length());

        // 移除多余的空行（保留单个空行）
        String cleaned = code.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");

        // 移除行尾空格
        cleaned = cleaned.replaceAll("\\s+$", "");

        // 移除多余的空格
        cleaned = cleaned.replaceAll("\\s+", " ");

        // 恢复适当的换行
        cleaned = cleaned.replaceAll("; ", ";\n");
        cleaned = cleaned.replaceAll("\\{ ", "{\n");
        cleaned = cleaned.replaceAll(" \\}", "\n}");

        // 移除空的方法体
        cleaned = cleaned.replaceAll("\\{\\s*\\}", "{}");

        // 确保包声明后有空行
        cleaned = cleaned.replaceAll("(package [^;]+;)([^\\n])", "$1\n$2");

        // 确保导入语句后有空行
        cleaned = cleaned.replaceAll("(import [^;]+;)([^\\n])", "$1\n$2");

        // 确保类声明前有空行
        cleaned = cleaned.replaceAll("(import [^;]+;)\\n([^\\n\\s])", "$1\n\n$2");

        log.debug("Code cleaned, new length: {}", cleaned.length());
        return cleaned;
    }

    /**
     * 确保代码编码正确
     * 处理可能的编码问题，确保输出为UTF-8格式
     * 
     * @param code 原始代码
     * @return 编码正确的代码
     */
    public String ensureProperEncoding(String code) {
        if (code == null) {
            return null;
        }

        log.debug("Ensuring proper encoding for code");

        try {
            // 检查是否包含非ASCII字符
            boolean hasNonAscii = !code.equals(code.replaceAll("[^\\x00-\\x7F]", ""));

            if (hasNonAscii) {
                log.debug("Code contains non-ASCII characters, ensuring UTF-8 encoding");

                // 确保字符串是UTF-8编码
                byte[] bytes = code.getBytes("UTF-8");
                String utf8Code = new String(bytes, "UTF-8");

                // 检查是否有编码问题
                if (!utf8Code.equals(code)) {
                    log.warn("Encoding conversion detected, using UTF-8 version");
                    return utf8Code;
                }
            }

            return code;

        } catch (Exception e) {
            log.error("Failed to ensure proper encoding: {}", e.getMessage());
            return code; // 返回原始代码
        }
    }

    /**
     * 后处理生成的代码
     * 应用清理和编码处理
     * 
     * @param code 原始生成的代码
     * @return 处理后的代码
     */
    public String postProcessCode(String code) {
        return code;
        // if (code == null || code.trim().isEmpty()) {
        // return code;
        // }

        // log.debug("Post-processing generated code");

        // // 1. 确保编码正确
        // String encodedCode = ensureProperEncoding(code);

        // // 2. 清理代码
        // String cleanedCode = cleanGeneratedCode(encodedCode);

        // log.debug("Code post-processing completed");
        // return cleanedCode;
    }
}