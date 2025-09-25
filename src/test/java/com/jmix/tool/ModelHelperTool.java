package com.jmix.tool;

import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.autoruletest.InjectCompatibleRuleTest.InjectCompatibleRuleConstraint;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * ModelHelper测试类
 * 测试生成并运行模型文件的功能
 */
@Slf4j
public final class ModelHelperTool {

    private ModelHelperTool() {
        // 工具类私有构造器
    }

    /**
     * 主方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        generatorModelFile();
        // injectConstraintCode();

    }

    /**
     * 注入约束代码
     */
    public static void injectConstraintCode() {
        try {
            ModelHelper modelHelper = new ModelHelper();
            modelHelper.autoInjectConstraintCode(InjectCompatibleRuleConstraint.class); //
            log.info("Injecting constraint code...");
        } catch (Exception e) {
            log.error("Failed to inject constraint code: {}", e.getMessage());
        }
    }

    /**
     * 调用大模型生成模型文件和测试文件
     */
    public static void generatorModelFile() {
        try {
            log.info("=== ModelHelper Test ===\n");

            // 创建ModelHelper实例
            ModelHelper modelHelper = new ModelHelper();

            // 方式1：从Markdown文件读取（推荐）
            String packageName = readFromMarkdown("packageName");
            String modelName = readFromMarkdown("modelName");
            String userVariableModel = readFromMarkdown("userVariableModel");
            String userLogicByPseudocode = readFromMarkdown("userLogicByPseudocode");
            String userTestCaseSpec = readFromMarkdown("userTestCaseSpec");

            log.info("Package name: {}", packageName);
            log.info("Model name: {}", modelName);

            // 方式2：直接使用字符串（保留原有方式）
            /*
             * String packageName = "com.jmix.configengine.scenario.ruletest";
             * String modelName = "MyTShirt";
             * String userVariableModel = "@ParaAnno( \n" +
             * "\t\t\toptions = {\"op11\", \"op12\", \"op13\"} \n" +
             * "        )\n" +
             * "        private ParaVar p1Var;\n" +
             * "    \n" +
             * "        @ParaAnno( \n" +
             * "            options = {\"op21\", \"op22\", \"op23\"}\n" +
             * "        )\n" +
             * "        private ParaVar p2Var;\n" +
             * "        @PartAnno\n" +
             * "        private PartVar pt1Var;";
             * String userLogicByPseudocode =
             * "if(p1Var.value == op11 && p2Var.value == op21) {\n" +
             * "                pt1Var.qty = 1;\n" +
             * "            }\n" +
             * "            else {\n" +
             * "                pt1Var.qty = 3;\n" +
             * "            }";
             */

            try {
                log.info("Generating and running test files...");

                // 测试生成并运行功能
                modelHelper.generatorRunModelFile(packageName, modelName, userVariableModel, userLogicByPseudocode,
                        userTestCaseSpec);

                log.info("Test completed successfully!");

            } catch (Exception e) {
                log.error("Test failed: {}", e.getMessage());
                log.error("Possible causes:");
                log.error("1. LLM API configuration issue");
                log.error("2. Network connection problem");
                log.error("3. Code generation or compilation issue");
                e.printStackTrace();
            }

        } catch (Exception e) {
            log.error("Error occurred during test execution: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从Markdown文件读取指定部分的内容
     * 
     * @param section 要读取的部分名称（如"userVariableModel"或"userLogicByPseudocode"）
     * @return 该部分的内容
     */
    private static String readFromMarkdown(String section) {
        try {
            // 获取当前类文件所在目录
            String currentDir = ModelHelperTool.class.getResource(".").getPath();
            if (currentDir.startsWith(File.separator)) {
                currentDir = currentDir.substring(1);
            }

            // 构建Markdown文件路径
            String markdownPath = currentDir + "ModelHelperToolBlockInput.md";

            // 读取Markdown文件内容
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(markdownPath)));

            // 解析指定部分的内容
            return parseMarkdownSection(content, section);

        } catch (Exception e) {
            log.warn("Failed to read Markdown file: {}", e.getMessage());
            log.warn("Will use default content");

            // 返回默认内容
            if ("packageName".equals(section)) {
                return getDefaultPackageName();
            } else if ("modelName".equals(section)) {
                return getDefaultModelName();
            } else if ("userVariableModel".equals(section)) {
                return getDefaultVariableModel();
            } else if ("userLogicByPseudocode".equals(section)) {
                return getDefaultLogicPseudocode();
            } else if ("userTestCaseSpec".equals(section)) {
                return getDefaultTestCaseSpec();
            } else {
                return "";
            }
        }
    }

    /**
     * 解析Markdown文件中的指定部分
     * 
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
                throw new AlgLoaderException("Section not found: " + section);
            }

            // 查找代码块开始
            int codeStart = content.indexOf("```java", startIndex);
            if (codeStart == -1) {
                throw new AlgLoaderException("Code block start marker not found");
            }

            // 查找代码块结束
            int codeEnd = content.indexOf("```", codeStart + 7);
            if (codeEnd == -1) {
                throw new AlgLoaderException("Code block end marker not found");
            }

            // 提取代码内容
            String code = content.substring(codeStart + 7, codeEnd).trim();

            log.info("Successfully read {} section from Markdown file", section);
            return code;

        } catch (Exception e) {
            throw new AlgLoaderException("Failed to parse Markdown section: " + e.getMessage(), e);
        }
    }

    /**
     * 获取默认的包名
     * 
     * @return 默认包名
     */
    private static String getDefaultPackageName() {
        return "com.jmix.configengine.scenario.ruletest";
    }

    /**
     * 获取默认的模型名
     * 
     * @return 默认模型名
     */
    private static String getDefaultModelName() {
        return "MyTShirt";
    }

    /**
     * 获取默认的变量模型
     * 
     * @return 默认变量模型
     */
    private static String getDefaultVariableModel() {
        return "@ParaAnno( \n"
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
    }

    /**
     * 获取默认的逻辑伪代码
     * 
     * @return 默认逻辑伪代码
     */
    private static String getDefaultLogicPseudocode() {
        return "if(p1Var.value == op11 && p2Var.value == op21) {\n"
                + "                pt1Var.qty = 1;\n"
                + "            }\n"
                + "            else {\n"
                + "                pt1Var.qty = 3;\n"
                + "            }";
    }

    /**
     * 获取默认的测试用例特殊规格
     * 
     * @return 默认测试用例特殊规格
     */
    private static String getDefaultTestCaseSpec() {
        return ""; // 默认为空，表示不指定特殊规格
    }
}