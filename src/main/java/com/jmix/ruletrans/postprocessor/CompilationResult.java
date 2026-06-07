package com.jmix.ruletrans.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Structured javac result.
 */
public record CompilationResult(
        boolean success,
        int exitCode,
        List<String> errors,
        List<String> diagnostics,
        Path sourceFile,
        Path outputDir) {
}
