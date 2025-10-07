package com.jmix.tool.impl;

import lombok.extern.slf4j.Slf4j;

// JUnit Platform imports
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 模块测试运行器
 * 负责执行生成的测试类
 * 
 * @since 2025-10-07
 */
@Slf4j
public class ModuleRunner {

    /**
     * 运行测试文件
     * 
     * @param packageName       包名
     * @param modelScenarioName 模型场景名称
     */
    public void runTestFile(String packageName, String modelScenarioName) {
        try {
            String className = modelScenarioName + "Test";
            String fullClassName = packageName + "." + className;

            log.info("Running test class: {}", fullClassName);

            // Prepare runtime classpath using URLClassLoader (target dirs + lib jars)
            String projectRoot = System.getProperty("user.dir");
            List<URL> runtimeUrls = new ArrayList<>();
            try {
                File mainClasses = new File(projectRoot + File.separator + "target" + File.separator + "classes");
                if (mainClasses.exists()) {
                    runtimeUrls.add(mainClasses.toURI().toURL());
                }
                File testClasses = new File(projectRoot + File.separator + "target" + File.separator + "test-classes");
                if (testClasses.exists()) {
                    runtimeUrls.add(testClasses.toURI().toURL());
                }
                File libDir = new File(projectRoot + File.separator + "lib");
                if (libDir.exists() && libDir.isDirectory()) {
                    File[] jarFiles = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                    if (jarFiles != null) {
                        for (File jar : jarFiles) {
                            runtimeUrls.add(jar.toURI().toURL());
                        }
                    }
                }
            } catch (Exception urlEx) {
                log.warn("Failed to prepare runtime URLs: {}", urlEx.getMessage());
            }

            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            URLClassLoader urlClassLoader = new URLClassLoader(runtimeUrls.toArray(new URL[0]), parent);
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            // 使用反射加载测试类
            Class<?> testClass = Class.forName(fullClassName, true, urlClassLoader);
            runWithJUnit(testClass);
        } catch (ClassNotFoundException e) {
            log.error("Test class not found: {}", e.getMessage());
            log.error("Please ensure the class has been generated and compiled correctly");
        } catch (Exception e) {
            log.error("Failed to run test file: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 使用 JUnit Platform 运行测试类
     * 
     * @param testClass 测试类
     */
    public void runWithJUnit(Class<?> testClass) {
        try {
            log.info("Starting JUnit test execution for class: {}", testClass.getName());

            // 创建 JUnit Platform Launcher
            Launcher launcher = LauncherFactory.create();

            // 创建测试发现请求
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(testClass))
                    .build();

            // 创建结果监听器
            SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();

            // 创建自定义测试执行监听器
            TestExecutionListener customListener = new TestExecutionListener() {
                @Override
                public void testPlanExecutionStarted(TestPlan testPlan) {
                    log.info("=== Individual Test Cases ===");
                }

                @Override
                public void executionStarted(TestIdentifier testIdentifier) {
                    if (testIdentifier.isTest()) {
                        log.info("Starting test: {}", testIdentifier.getDisplayName());
                    }
                }
            };

            // 注册监听器
            launcher.registerTestExecutionListeners(summaryListener, customListener);

            // 执行测试
            launcher.execute(request);

            // 获取测试结果摘要
            TestExecutionSummary summary = summaryListener.getSummary();

            // 记录测试结果
            logTestResults(summary);

        } catch (Exception e) {
            log.error("Failed to run JUnit tests for class: {}", testClass.getName(), e);
        }
    }

    /**
     * 记录测试执行结果
     * 
     * @param summary 测试执行摘要
     */
    private void logTestResults(TestExecutionSummary summary) {
        log.info("=== JUnit Test Execution Results ===");
        log.info("Tests found: {}", summary.getTestsFoundCount());
        log.info("Tests succeeded: {}", summary.getTestsSucceededCount());
        log.info("Tests failed: {}", summary.getTestsFailedCount());

        // 记录失败的测试
        if (summary.getTestsFailedCount() > 0) {
            log.error("=== Failed Tests ===");
            summary.getFailures().forEach(failure -> {
                log.error("Test: {} - Reason: {}",
                        failure.getTestIdentifier().getDisplayName(),
                        failure.getException().getMessage());
                if (failure.getException().getCause() != null) {
                    log.error("Cause: {}", failure.getException().getCause().getMessage());
                }
            });
        }
        // 记录总体结果
        if (summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0) {
            log.info("✓ All tests passed successfully!");
        } else {
            log.error("✗ Some tests failed or were aborted");
        }

        log.info("=== End of Test Results ===");
    }
}
