package com.jmix.tool.modeltool;

import com.jmix.tool.ModelHelper;
import com.jmix.tool.impl.LLMInvoker;

import lombok.extern.slf4j.Slf4j;

/**
 * LLM调用框架测试类
 */
@Slf4j
public final class LLMInvokerTest {

    private LLMInvokerTest() {
        // 工具类应隐藏 public 构造器
    }

    /**
     * 主方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            log.info("=== LLM Invocation Framework Test ===\n");

            // 创建LLM调用器
            LLMInvoker invoker = new LLMInvoker();
            ModelHelper modelHelper = new ModelHelper();
            // 显示配置信息
            log.info("{}", invoker.getConfigInfo());

            // 测试用例1：MyTShirt约束模型
            log.info("\nTest Case 1: MyTShirt Constraint Model");
            String modelName = "MyTShirt";
            String userVariableModel = "@ParaAnno( \n"
                    + "\t\t\toptions = {\"op11\", \"op12\", \"op13\"} \n"
                    + "        )\n"
                    + "        private ParaVar p1Var;\n"
                    + "    \n"
                    + "        @ParaAnno( \n"
                    + "            options = {\"op21\", \"op22\", \"op23\"}\n"
                    + "        )\n"
                    + "        private ParaVar p2Var;\n"
                    + "        @PartAnno\n"
                    + "        private PartVar pt1Var;";
            String userLogicByPseudocode = "if(p1Var.value == op11 && p2Var.value == op21) {\n"
                    + "                pt1Var.qty = 1;\n"
                    + "            }\n"
                    + "            else {\n"
                    + "                pt1Var.qty = 3;\n"
                    + "            }";

            try {
                log.info("Calling LLM to generate code...");

                // 测试默认包名生成
                log.info("Generating code with default package name...");
                modelHelper.generatorModelFile(modelName, userVariableModel, userLogicByPseudocode);
                // 使用默认包名生成并运行
                // modelHelper.generatorRunModelFile(modelName, userVariableModel,
                // userLogicByPseudocode);

                // 测试自定义包名生成
                log.info("\nGenerating code with custom package name...");
                // String customPackage = "com.jmix.configengine.scenario.custom";
                // modelHelper.generatorModelFile(customPackage, modelName, userVariableModel,
                // userLogicByPseudocode);
                log.info("Generated code:");
                log.info("==================================================");
                // log.info(generatedCode);
                log.info("==================================================");
            } catch (Exception e) {
                log.error("Code generation failed: {}", e.getMessage());
                log.error("Possible reasons:");
                log.error("1. API key not configured");
                log.error("2. Network connection issues");
                log.error("3. API service unavailable");
                e.printStackTrace();
            }

            log.info("\nTest completed!");

        } catch (Exception e) {
            log.error("Error occurred during testing: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}