package com.jmix.configengine.scenario.base.modeltool;

/**
 * ModelHelper测试类
 * 测试生成并运行模型文件的功能
 */
public class ModelHelperTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== ModelHelper测试 ===\n");
            
            // 创建ModelHelper实例
            ModelHelper modelHelper = new ModelHelper();
            
            // 测试用例：MyTShirt约束模型
            System.out.println("测试用例：MyTShirt约束模型");
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
            String userLogicByPseudocode = "if(P1Var.var == op11 && P2Var.var == op21) {\n" +
                    "                PT1Var.var = 1;\n" +
                    "            }\n" +
                    "            else {\n" +
                    "                PT1Var.var = 3;\n" +
                    "            }";
            
            try {
                System.out.println("正在生成并运行测试文件...");
                
                // 测试生成并运行功能
                modelHelper.generatorRunModelFile(modelName, userVariableModel, userLogicByPseudocode);
                
                System.out.println("测试完成！");
                
            } catch (Exception e) {
                System.out.println("测试失败: " + e.getMessage());
                System.out.println("可能的原因：");
                System.out.println("1. LLM API配置问题");
                System.out.println("2. 网络连接问题");
                System.out.println("3. 代码生成或编译问题");
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 