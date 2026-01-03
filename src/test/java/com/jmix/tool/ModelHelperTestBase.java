package com.jmix.tool;

import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.autoruletest.InjectCompatibleRuleTest.InjectCompatibleRuleConstraint;
import com.jmix.tool.impl.ModelGenneratorException;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ModelHelper测试类
 * 测试生成并运行模型文件的功能
 */
@Slf4j
public class ModelHelperTestBase {

    /**
     * 主方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // generatorModelFile();
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
    public static void generatorModelFile(Class<?> genneratorClazz) {
        String packageName = genneratorClazz.getPackage().getName();
        String modelName = genneratorClazz.getSimpleName().replace("Gen", "");
        String javaFilePath = CommHelper.getJavaFilePath(genneratorClazz);
        javaFilePath = javaFilePath + File.separator + modelName + "Spec.md";
        generatorModelFile(javaFilePath, packageName, modelName);
    }

    private static void generatorModelFile(String mdFile, String defalutPackageName, String defaultModelName) {
        try {
            log.info("=== ModelHelper Test ===\n");

            // 创建ModelHelper实例
            ModelHelper modelHelper = new ModelHelper();
            String mdContent = readMarkdownFile(mdFile);

            // 方式1：从Markdown文件读取（推荐）
            String packageName = parseMarkdownSection(mdContent, "packageName", defalutPackageName);
            String modelName = parseMarkdownSection(mdContent, "modelName", defaultModelName);
            String userVariableModel = parseMarkdownSection(mdContent, "userVariableModel", "");
            String userLogicByPseudocode = parseMarkdownSection(mdContent, "userLogicByPseudocode", "");
            String userTestCaseSpec = parseMarkdownSection(mdContent, "userTestCaseSpec", "");
            if (userVariableModel.isEmpty() || userLogicByPseudocode.isEmpty()) {
                log.error("userVariableModel or userLogicByPseudocode is empty!");
                return;
            }
            log.info("Package name: {}", packageName);
            log.info("Model name: {}", modelName);

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

    private static String readMarkdownFile(String mdFile) {
        try {
            // 读取Markdown文件内容
            String content = new String(Files.readAllBytes(Paths.get(mdFile)));
            return content;

        } catch (Exception e) {
            log.error("Failed to read Markdown file: {}", e);
            throw new ModelGenneratorException("Failed to read Markdown file: {}", e);
        }
    }

    private static String parseMarkdownSection(String content, String section, String defaultValue) {
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
            log.warn("Failed to parse Markdown section: " + e.getMessage());
            return defaultValue;
        }
    }
}