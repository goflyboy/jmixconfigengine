package com.jmix.configengine.scenario.base.modeltool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt模板加载器
 * 支持JTL格式的模板文件，使用${variable}语法进行变量替换
 */
public class PromptTemplateLoader {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * 加载模板文件
     * @param templatePath 模板文件路径（相对于resources目录）
     * @return 模板内容
     */
    public static String loadTemplate(String templatePath) {
        try (InputStream input = PromptTemplateLoader.class.getClassLoader().getResourceAsStream(templatePath)) {
            if (input == null) {
                throw new RuntimeException("无法找到模板文件: " + templatePath);
            }
            byte[] bytes = new byte[input.available()];
            input.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载模板文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 渲染模板
     * @param template 模板内容
     * @param variables 变量映射
     * @return 渲染后的内容
     */
    public static String renderTemplate(String template, Map<String, String> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, "");
            matcher.appendReplacement(result, value.replace("\\", "\\\\").replace("$", "\\$"));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 便捷方法：直接加载并渲染模板
     * @param templatePath 模板文件路径
     * @param variables 变量映射
     * @return 渲染后的内容
     */
    public static String loadAndRenderTemplate(String templatePath, Map<String, String> variables) {
        String template = loadTemplate(templatePath);
        return renderTemplate(template, variables);
    }
    
    /**
     * 便捷方法：使用两个主要变量渲染Java代码生成模板
     * @param userVariableModel 用户变量模型描述
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @return 渲染后的prompt
     */
    public static String renderJavaCodeTemplate(String userVariableModel, String userLogicByPseudocode) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userVariableModel", userVariableModel);
        variables.put("userLogicByPseudocode", userLogicByPseudocode);
        
        return loadAndRenderTemplate("constraint_generate_prompt.jtl", variables);
    }
} 