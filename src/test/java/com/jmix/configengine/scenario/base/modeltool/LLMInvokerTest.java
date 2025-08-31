package com.jmix.configengine.scenario.base.modeltool;

/**
 * LLM调用框架测试类
 */
public class LLMInvokerTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== LLM调用框架测试 ===\n");
            
            // 创建LLM调用器
            LLMInvoker invoker = new LLMInvoker();
            
            // 显示配置信息
            System.out.println(invoker.getConfigInfo());
            
            // 测试用例1：简单的用户管理类
            System.out.println("\n测试用例1：用户管理类");
            String userVariableModel = "用户类包含以下字段：\n" +
                "- id: Long类型，用户唯一标识\n" +
                "- username: String类型，用户名\n" +
                "- email: String类型，邮箱\n" +
                "- age: Integer类型，年龄\n" +
                "- isActive: Boolean类型，是否激活";
            
            String userLogicByPseudocode = "1. 创建用户对象\n" +
                "2. 验证用户名长度（3-20字符）\n" +
                "3. 验证邮箱格式\n" +
                "4. 验证年龄范围（18-100）\n" +
                "5. 提供toString方法\n" +
                "6. 提供equals和hashCode方法";
            
            try {
                System.out.println("正在调用LLM生成代码...");
                String generatedCode = invoker.generatorModelCode(userVariableModel, userLogicByPseudocode);
                System.out.println("生成的代码：");
                System.out.println("==================================================");
                System.out.println(generatedCode);
                System.out.println("==================================================");
            } catch (Exception e) {
                System.out.println("代码生成失败: " + e.getMessage());
                System.out.println("可能的原因：");
                System.out.println("1. API密钥未配置");
                System.out.println("2. 网络连接问题");
                System.out.println("3. API服务不可用");
                e.printStackTrace();
            }
            
            System.out.println("\n" + "==================================================\n");
            
            // 测试用例2：计算器类
            System.out.println("测试用例2：计算器类");
            String calcVariableModel = "计算器类需要支持：\n" +
                "- 基本运算：加、减、乘、除\n" +
                "- 历史记录：存储最近10次计算\n" +
                "- 错误处理：除零等异常情况";
            
            String calcLogicByPseudocode = "1. 实现四则运算方法\n" +
                "2. 维护计算历史记录\n" +
                "3. 处理除零异常\n" +
                "4. 提供历史记录查询方法\n" +
                "5. 支持清除历史记录";
            
            try {
                System.out.println("正在调用LLM生成代码...");
                String generatedCode = invoker.generatorModelCode("deepseek", calcVariableModel, calcLogicByPseudocode);
                System.out.println("生成的代码：");
                System.out.println("==================================================");
                System.out.println(generatedCode);
                System.out.println("==================================================");
            } catch (Exception e) {
                System.out.println("代码生成失败: " + e.getMessage());
                System.out.println("可能的原因：");
                System.out.println("1. API密钥未配置");
                System.out.println("2. 网络连接问题");
                System.out.println("3. API服务不可用");
                e.printStackTrace();
            }
            
            System.out.println("\n测试完成！");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 