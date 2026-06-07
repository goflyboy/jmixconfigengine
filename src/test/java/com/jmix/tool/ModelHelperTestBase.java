package com.jmix.tool;

import com.jmix.executor.impl.util.CommHelper;
import com.jmix.tool.autoruletest.InjectCompatibleRuleTest.InjectCompatibleRuleConstraint;
import com.jmix.tool.impl.ModelGenneratorException;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ModelHelperTestBase {

    public static void main(String[] args) {
        // Utility base class for generator entry points.
    }

    public static void injectConstraintCode() {
        try {
            ModelHelper modelHelper = new ModelHelper();
            modelHelper.autoInjectConstraintCode(InjectCompatibleRuleConstraint.class);
            log.info("Injecting constraint code...");
        } catch (Exception e) {
            throw new ModelGenneratorException("Failed to inject constraint code: " + e.getMessage(), e);
        }
    }

    public static void generatorModelFile(Class<?> generatorClazz) {
        String packageName = generatorClazz.getPackage().getName();
        String modelName = generatorClazz.getSimpleName().replace("Gen", "");
        String javaFilePath = CommHelper.getJavaFilePath(generatorClazz);
        javaFilePath = javaFilePath + File.separator + modelName + "Spec.md";
        generatorModelFile(javaFilePath, packageName, modelName);
    }

    private static void generatorModelFile(String mdFile, String defaultPackageName, String defaultModelName) {
        try {
            log.info("=== ModelHelper Test ===");

            ModelHelper modelHelper = new ModelHelper();
            String mdContent = readMarkdownFile(mdFile);

            String packageName = parseMarkdownSection(mdContent, "packageName", defaultPackageName);
            String modelName = parseMarkdownSection(mdContent, "modelName", defaultModelName);
            String userVariableModel = parseMarkdownSection(mdContent, "userVariableModel", "");
            String userLogicByPseudocode = parseMarkdownSection(mdContent, "userLogicByPseudocode", "");
            String userTestCaseSpec = parseMarkdownSection(mdContent, "userTestCaseSpec", "");
            if (userVariableModel.isEmpty() || userLogicByPseudocode.isEmpty()) {
                throw new ModelGenneratorException("userVariableModel or userLogicByPseudocode is empty");
            }

            log.info("Package name: {}", packageName);
            log.info("Model name: {}", modelName);
            log.info("Generating and running test files...");

            modelHelper.generatorRunModelFile(packageName, modelName, userVariableModel, userLogicByPseudocode,
                    userTestCaseSpec);

            log.info("Test completed successfully!");
        } catch (Exception e) {
            if (e instanceof ModelGenneratorException) {
                throw (ModelGenneratorException) e;
            }
            throw new ModelGenneratorException("Error occurred during test execution: " + e.getMessage(), e);
        }
    }

    private static String readMarkdownFile(String mdFile) {
        try {
            return new String(Files.readAllBytes(Paths.get(mdFile)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ModelGenneratorException("Failed to read Markdown file: " + mdFile, e);
        }
    }

    private static String parseMarkdownSection(String content, String section, String defaultValue) {
        String startMarker = "##" + section;
        int startIndex = content.indexOf(startMarker);
        if (startIndex == -1) {
            log.debug("Markdown section {} not found, using default value", section);
            return defaultValue;
        }

        int codeStart = content.indexOf("```java", startIndex);
        if (codeStart == -1) {
            log.warn("Markdown section {} has no java code block, using default value", section);
            return defaultValue;
        }

        int codeEnd = content.indexOf("```", codeStart + 7);
        if (codeEnd == -1) {
            log.warn("Markdown section {} code block is not closed, using default value", section);
            return defaultValue;
        }

        String code = content.substring(codeStart + 7, codeEnd).trim();
        log.info("Successfully read {} section from Markdown file", section);
        return code;
    }
}
