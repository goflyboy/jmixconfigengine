package com.jmix.configengine.scenario.base.modeltool;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板加载器测试类
 */
public class TemplateTest {
    
    public static void main(String[] args) {
        System.out.println("=== Prompt模板加载器测试 ===\n");
        
        try {
            // 测试1：加载原始模板
            System.out.println("测试1：加载原始模板");
            String template = PromptTemplateLoader.loadTemplate("constraint_generate_prompt.jtl");
            System.out.println("模板内容：");
            System.out.println(template);
            System.out.println();
            
            // 测试2：渲染模板
            System.out.println("测试2：渲染模板");
            String userVariableModel = "用户类包含id、username、email字段";
            String userLogicByPseudocode = "1. 创建用户对象\n2. 验证字段\n3. 提供getter/setter";
            
            String renderedPrompt = PromptTemplateLoader.renderJavaCodeTemplate(userVariableModel, userLogicByPseudocode);
            System.out.println("渲染后的prompt：");
            System.out.println(renderedPrompt);
            System.out.println();
            
            // 测试3：使用自定义变量
            System.out.println("测试3：使用自定义变量");
            Map<String, String> customVars = new HashMap<>();
            customVars.put("userVariableModel", "计算器类，包含add和subtract方法");
            customVars.put("userLogicByPseudocode", "1. 实现加法\n2. 实现减法\n3. 处理异常");
            
            String customPrompt = PromptTemplateLoader.loadAndRenderTemplate("constraint_generate_prompt.jtl", customVars);
            System.out.println("自定义变量渲染结果：");
            System.out.println(customPrompt);
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 