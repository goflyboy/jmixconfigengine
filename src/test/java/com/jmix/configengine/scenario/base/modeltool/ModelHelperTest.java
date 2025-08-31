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
            
            // 方式1：从Markdown文件读取（推荐）
            String packageName = readFromMarkdown("packageName");
            String modelName = readFromMarkdown("modelName");
            String userVariableModel = readFromMarkdown("userVariableModel");
            String userLogicByPseudocode = readFromMarkdown("userLogicByPseudocode");
            
            System.out.println("包名: " + packageName);
            System.out.println("模型名: " + modelName);
            
            // 方式2：直接使用字符串（保留原有方式）
            /*
            String packageName = "com.jmix.configengine.scenario.ruletest";
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
                    "        private ParaVar PT1Var;";            
            String userLogicByPseudocode = "if(P1Var.var == op11 && P2Var.var == op21) {\n" +
                    "                PT1Var.var = 1;\n" +
                    "            }\n" +
                    "            else {\n" +
                    "                PT1Var.var = 3;\n" +
                    "            }";
            */
            
            try {
                System.out.println("正在生成并运行测试文件...");
                
                // 测试生成并运行功能
                modelHelper.generatorRunModelFile(packageName, modelName, userVariableModel, userLogicByPseudocode);
                
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
    
    /**
     * 从Markdown文件读取指定部分的内容
     * @param section 要读取的部分名称（如"userVariableModel"或"userLogicByPseudocode"）
     * @return 该部分的内容
     */
    private static String readFromMarkdown(String section) {
        try {
            // 获取当前类文件所在目录
            String currentDir = ModelHelperTest.class.getResource(".").getPath();
            if (currentDir.startsWith("/")) {
                currentDir = currentDir.substring(1);
            }
            
            // 构建Markdown文件路径
            String markdownPath = currentDir + "ModelHelperTestBlockInput.md";
            
            // 读取Markdown文件内容
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(markdownPath)));
            
            // 解析指定部分的内容
            return parseMarkdownSection(content, section);
            
        } catch (Exception e) {
            System.err.println("读取Markdown文件失败: " + e.getMessage());
            System.err.println("将使用默认内容");
            
            // 返回默认内容
            if ("packageName".equals(section)) {
                return getDefaultPackageName();
            } else if ("modelName".equals(section)) {
                return getDefaultModelName();
            } else if ("userVariableModel".equals(section)) {
                return getDefaultVariableModel();
            } else if ("userLogicByPseudocode".equals(section)) {
                return getDefaultLogicPseudocode();
            } else {
                return "";
            }
        }
    }
    
    /**
     * 解析Markdown文件中的指定部分
     * @param content Markdown文件内容
     * @param section 部分名称
     * @return 解析后的内容
     */
    private static String parseMarkdownSection(String content, String section) {
        try {
            // 查找部分开始标记
            String startMarker = "##" + section;
            int startIndex = content.indexOf(startMarker);
            if (startIndex == -1) {
                throw new RuntimeException("未找到部分: " + section);
            }
            
            // 查找代码块开始
            int codeStart = content.indexOf("```java", startIndex);
            if (codeStart == -1) {
                throw new RuntimeException("未找到代码块开始标记");
            }
            
            // 查找代码块结束
            int codeEnd = content.indexOf("```", codeStart + 7);
            if (codeEnd == -1) {
                throw new RuntimeException("未找到代码块结束标记");
            }
            
            // 提取代码内容
            String code = content.substring(codeStart + 7, codeEnd).trim();
            
            System.out.println("成功从Markdown文件读取 " + section + " 部分");
            return code;
            
        } catch (Exception e) {
            throw new RuntimeException("解析Markdown部分失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取默认的包名
     * @return 默认包名
     */
    private static String getDefaultPackageName() {
        return "com.jmix.configengine.scenario.ruletest";
    }
    
    /**
     * 获取默认的模型名
     * @return 默认模型名
     */
    private static String getDefaultModelName() {
        return "MyTShirt";
    }
    
    /**
     * 获取默认的变量模型
     * @return 默认变量模型
     */
    private static String getDefaultVariableModel() {
        return "@ParaAnno( \n" +
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
    }
    
    /**
     * 获取默认的逻辑伪代码
     * @return 默认逻辑伪代码
     */
    private static String getDefaultLogicPseudocode() {
        return "if(P1Var.var == op11 && P2Var.var == op21) {\n" +
               "                PT1Var.var = 1;\n" +
               "            }\n" +
               "            else {\n" +
               "                PT1Var.var = 3;\n" +
               "            }";
    }
} 