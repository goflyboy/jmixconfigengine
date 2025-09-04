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
            ModelHelper modelHelper = new ModelHelper();
            // 显示配置信息
            System.out.println(invoker.getConfigInfo());
            
            // 测试用例1：MyTShirt约束模型
            System.out.println("\n测试用例1：MyTShirt约束模型");
            String modelName = "MyTShirt";
            String userVariableModel = "@ParaAnno( \n" +
                    "\t\t\toptions = {\"op11\", \"op12\", \"op13\"} \n" +
                    "        )\n" +
                    "        private ParaVar P1Var;\n" +
                    "    \n" +
                    "        @ParaAnno( \n" +
                    "            options = {\"op21\", \"op22\", \"op23\"}\n" +
                    "        )\n" +
                    "        private ParaVar P2Var;\n" +
                    "        @PartAnno\n" +
                    "        private PartVar PT1Var;";
                    String userLogicByPseudocode = "if(P1Var.value == op11 && P2Var.value == op21) {\n" +
                "                PT1Var.qty = 1;\n" +
                "            }\n" +
                "            else {\n" +
                "                PT1Var.qty = 3;\n" +
                "            }";
            
            try {
                System.out.println("正在调用LLM生成代码...");

                // 测试默认包名生成
                System.out.println("正在使用默认包名生成代码...");
                modelHelper.generatorModelFile(modelName, userVariableModel, userLogicByPseudocode);
                // 使用默认包名生成并运行
                //modelHelper.generatorRunModelFile(modelName, userVariableModel, userLogicByPseudocode);

                // 测试自定义包名生成
                System.out.println("\n正在使用自定义包名生成代码...");
                // String customPackage = "com.jmix.configengine.scenario.custom";
                // modelHelper.generatorModelFile(customPackage, modelName, userVariableModel, userLogicByPseudocode);
                System.out.println("生成的代码：");
                System.out.println("==================================================");
                // System.out.println(generatedCode);
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