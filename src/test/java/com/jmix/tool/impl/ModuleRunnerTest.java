package com.jmix.tool.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ModuleRunner 测试类
 * 测试 ModuleRunner 的各种功能，包括 runTestFile 方法
 */
@Slf4j
public class ModuleRunnerTest {

    private ModuleRunner moduleRunner;

    @BeforeEach
    void setUp() {
        moduleRunner = new ModuleRunner();
    }

    /**
     * 测试 runTestFile 方法
     * 使用 ParaIntegerTest 来测试
     */
    @Test
    void testRunTestFile() {
        // 测试参数
        String packageName = "com.jmix.scenario.ruletest";
        String modelScenarioName = "ParaInteger";

        log.info("Testing runTestFile with package: {} and modelScenarioName: {}",
                packageName, modelScenarioName);

        // 执行测试 - 不应该抛出异常
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile(packageName, modelScenarioName);
        }, "runTestFile should not throw any exception");

        log.info("runTestFile test completed successfully");
    }

    /**
     * 测试 runTestFile 方法 - 使用不存在的类
     */
    @Test
    void testRunTestFileWithNonExistentClass() {
        // 测试参数 - 使用不存在的类
        String packageName = "com.jmix.scenario.ruletest";
        String modelScenarioName = "NonExistentTest";

        log.info("Testing runTestFile with non-existent class: {}.{}Test",
                packageName, modelScenarioName);

        // 执行测试 - 不应该抛出异常，但会记录错误日志
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile(packageName, modelScenarioName);
        }, "runTestFile should handle non-existent class gracefully");

        log.info("runTestFile test with non-existent class completed");
    }

    /**
     * 测试 runTestFile 方法 - 使用错误的包名
     */
    @Test
    void testRunTestFileWithWrongPackage() {
        // 测试参数 - 使用错误的包名
        String packageName = "com.jmix.wrongpackage";
        String modelScenarioName = "ParaInteger";

        log.info("Testing runTestFile with wrong package: {} and modelScenarioName: {}",
                packageName, modelScenarioName);

        // 执行测试 - 不应该抛出异常，但会记录错误日志
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile(packageName, modelScenarioName);
        }, "runTestFile should handle wrong package gracefully");

        log.info("runTestFile test with wrong package completed");
    }

    /**
     * 测试 runTestFile 方法 - 参数验证
     */
    @Test
    void testRunTestFileWithNullParameters() {
        log.info("Testing runTestFile with null parameters");

        // 测试 null 包名
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile(null, "ParaInteger");
        }, "runTestFile should handle null package name");

        // 测试 null 模型场景名称
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile("com.jmix.scenario.ruletest", null);
        }, "runTestFile should handle null model scenario name");

        // 测试两个参数都为 null
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile(null, null);
        }, "runTestFile should handle both null parameters");

        log.info("runTestFile test with null parameters completed");
    }

    /**
     * 测试 runTestFile 方法 - 空字符串参数
     */
    @Test
    void testRunTestFileWithEmptyParameters() {
        log.info("Testing runTestFile with empty parameters");

        // 测试空包名
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile("", "ParaInteger");
        }, "runTestFile should handle empty package name");

        // 测试空模型场景名称
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile("com.jmix.scenario.ruletest", "");
        }, "runTestFile should handle empty model scenario name");

        // 测试两个参数都为空
        assertDoesNotThrow(() -> {
            moduleRunner.runTestFile("", "");
        }, "runTestFile should handle both empty parameters");

        log.info("runTestFile test with empty parameters completed");
    }
}
