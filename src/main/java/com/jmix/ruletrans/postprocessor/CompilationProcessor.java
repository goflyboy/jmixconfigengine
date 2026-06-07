package com.jmix.ruletrans.postprocessor;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Structured Java compilation processor for RuleTrans temporary classes.
 */
public final class CompilationProcessor {

    private final RuleTransTempFileManager tempFileManager;

    public CompilationProcessor() {
        this(new RuleTransTempFileManager());
    }

    public CompilationProcessor(RuleTransTempFileManager tempFileManager) {
        this.tempFileManager = tempFileManager;
    }

    public CompilationResult compile(AssembledRuleClass assembledRuleClass) {
        if (assembledRuleClass == null) {
            throw new IllegalArgumentException("assembledRuleClass must not be null");
        }
        return compile(assembledRuleClass.sourceFile());
    }

    public CompilationResult compile(Path sourceFile) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile must not be null");
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            List<String> errors = List.of("JDK compiler is not available. Run with a JDK instead of a JRE.");
            return new CompilationResult(false, -1, errors, errors, sourceFile, tempFileManager.classesRoot());
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(tempFileManager.classesRoot().toFile()));
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of("-encoding", "UTF-8", "-classpath", classpath());
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, compilationUnits);
            boolean success = Boolean.TRUE.equals(task.call());
            List<String> diagnosticLines = diagnostics.getDiagnostics().stream()
                    .map(this::formatDiagnostic)
                    .toList();
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    .map(this::formatDiagnostic)
                    .collect(Collectors.toList());
            return new CompilationResult(success, success ? 0 : 1, errors, diagnosticLines,
                    sourceFile, tempFileManager.classesRoot());
        } catch (IOException e) {
            List<String> errors = List.of("Compilation failed before javac execution: " + e.getMessage());
            return new CompilationResult(false, -1, errors, errors, sourceFile, tempFileManager.classesRoot());
        }
    }

    private String classpath() {
        String projectRoot = System.getProperty("user.dir");
        String pathSep = File.pathSeparator;
        List<String> entries = new ArrayList<>();
        entries.add(System.getProperty("java.class.path", ""));
        entries.add(Path.of(projectRoot, "target", "classes").toString());
        entries.add(Path.of(projectRoot, "target", "test-classes").toString());
        entries.add(tempFileManager.classesRoot().toString());
        File libDir = Path.of(projectRoot, "lib").toFile();
        File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                entries.add(jar.getAbsolutePath());
            }
        }
        return String.join(pathSep, entries);
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        String sourceName = diagnostic.getSource() == null ? "<unknown>" : diagnostic.getSource().getName();
        return diagnostic.getKind()
                + " " + sourceName
                + ":" + diagnostic.getLineNumber()
                + ":" + diagnostic.getColumnNumber()
                + " " + diagnostic.getMessage(Locale.ROOT);
    }
}
