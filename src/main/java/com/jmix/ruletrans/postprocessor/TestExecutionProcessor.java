package com.jmix.ruletrans.postprocessor;

import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;

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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Structured JUnit execution processor for generated tests.
 */
public final class TestExecutionProcessor {

    private final RuleTransTempFileManager tempFileManager;

    public TestExecutionProcessor() {
        this(new RuleTransTempFileManager());
    }

    public TestExecutionProcessor(RuleTransTempFileManager tempFileManager) {
        this.tempFileManager = tempFileManager;
    }

    public TestExecutionResult execute(AssembledRuleClass assembledTestClass) {
        if (assembledTestClass == null) {
            throw new IllegalArgumentException("assembledTestClass must not be null");
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            URLClassLoader loader = classLoader();
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> testClass = Class.forName(assembledTestClass.qualifiedClassName(), true, loader);
            return execute(testClass);
        } catch (Exception e) {
            FailedTestCase failed = new FailedTestCase(
                    assembledTestClass.qualifiedClassName(),
                    assembledTestClass.className(),
                    "",
                    "",
                    "",
                    e.getMessage(),
                    false);
            return new TestExecutionResult(false, 0, 0, 1, List.of(failed));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public TestExecutionResult execute(Class<?> testClass) {
        if (testClass == null) {
            throw new IllegalArgumentException("testClass must not be null");
        }
        try {
            Launcher launcher = LauncherFactory.create();
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(testClass))
                    .build();
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            return toResult(listener.getSummary());
        } catch (Exception e) {
            throw new RuleTransException("Failed to run JUnit test class: " + testClass.getName(), e);
        }
    }

    private TestExecutionResult toResult(TestExecutionSummary summary) {
        List<FailedTestCase> failedCases = summary.getFailures().stream()
                .map(this::toFailedCase)
                .toList();
        boolean success = summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0;
        return new TestExecutionResult(
                success,
                summary.getTestsFoundCount(),
                summary.getTestsSucceededCount(),
                summary.getTestsFailedCount(),
                failedCases);
    }

    private FailedTestCase toFailedCase(TestExecutionSummary.Failure failure) {
        Throwable exception = failure.getException();
        String reason = exception == null ? "" : exception.getMessage();
        return new FailedTestCase(
                failure.getTestIdentifier().getUniqueId(),
                failure.getTestIdentifier().getDisplayName(),
                "",
                expectedFrom(exception),
                actualFrom(exception),
                reason,
                isRuleLogicError(exception));
    }

    private boolean isRuleLogicError(Throwable exception) {
        if (exception == null) {
            return false;
        }
        String className = exception.getClass().getName().toLowerCase(Locale.ROOT);
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        if (className.contains("assertion") || className.contains("opentest4j")) {
            return true;
        }
        return message.contains("expected") || message.contains("violated") || message.contains("no solution");
    }

    private String expectedFrom(Throwable exception) {
        return exception instanceof org.opentest4j.AssertionFailedError assertion
                ? String.valueOf(assertion.getExpected())
                : "";
    }

    private String actualFrom(Throwable exception) {
        return exception instanceof org.opentest4j.AssertionFailedError assertion
                ? String.valueOf(assertion.getActual())
                : "";
    }

    private URLClassLoader classLoader() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        List<URL> urls = new ArrayList<>();
        addIfExists(urls, tempFileManager.classesRoot());
        addIfExists(urls, Path.of(projectRoot, "target", "classes"));
        addIfExists(urls, Path.of(projectRoot, "target", "test-classes"));
        File libDir = Path.of(projectRoot, "lib").toFile();
        File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                urls.add(jar.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    private void addIfExists(List<URL> urls, Path path) throws Exception {
        File file = path.toFile();
        if (file.exists()) {
            urls.add(file.toURI().toURL());
        }
    }
}
