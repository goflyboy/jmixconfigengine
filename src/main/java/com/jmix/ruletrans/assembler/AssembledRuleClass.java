package com.jmix.ruletrans.assembler;

import java.nio.file.Path;

/**
 * Complete temporary Java class assembled from a rule snippet.
 */
public record AssembledRuleClass(
        String packageName,
        String className,
        String qualifiedClassName,
        String sourceCode,
        Path sourceFile) {
}
