package com.jmix.tool.impl;

import lombok.extern.slf4j.Slf4j;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ModuleRunner {

    public void runTestFile(String packageName, String modelScenarioName) {
        String className = modelScenarioName + "Test";
        String fullClassName = packageName + "." + className;

        try {
            log.info("Running test class: {}", fullClassName);

            String projectRoot = System.getProperty("user.dir");
            List<URL> runtimeUrls = new ArrayList<>();

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

            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            URLClassLoader urlClassLoader = new URLClassLoader(runtimeUrls.toArray(new URL[0]), parent);
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            Class<?> testClass = Class.forName(fullClassName, true, urlClassLoader);
            runWithJUnit(testClass);
        } catch (Exception e) {
            if (e instanceof ModelGenneratorException) {
                throw (ModelGenneratorException) e;
            }
            throw new ModelGenneratorException("Failed to run test class: " + fullClassName, e);
        }
    }

    public void runWithJUnit(Class<?> testClass) {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();

        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(summaryListener);
        launcher.execute(request);

        logTestResults(summaryListener.getSummary());
    }

    private void logTestResults(TestExecutionSummary summary) {
        log.info("=== JUnit Test Execution Results ===");
        log.info("Tests found: {}", summary.getTestsFoundCount());
        log.info("Tests succeeded: {}", summary.getTestsSucceededCount());
        log.info("Tests failed: {}", summary.getTestsFailedCount());

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

        log.info("=== End of Test Results ===");
        if (summary.getTestsFoundCount() == 0) {
            throw new ModelGenneratorException("No tests were discovered");
        }
        if (summary.getTestsFailedCount() > 0 || summary.getTestsAbortedCount() > 0) {
            throw new ModelGenneratorException("Some tests failed or were aborted");
        }
        log.info("All tests passed successfully");
    }
}
