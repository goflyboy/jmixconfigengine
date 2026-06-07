package com.jmix.ruletrans.assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Owns RuleTrans temporary source and class output paths.
 */
public final class RuleTransTempFileManager {

    private final Path baseDir;

    public RuleTransTempFileManager() {
        this(Paths.get(System.getProperty("user.dir"), "target", "ruletrans"));
    }

    public RuleTransTempFileManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path sourceRoot() {
        return baseDir.resolve("generated-src");
    }

    public Path classesRoot() {
        return baseDir.resolve("classes");
    }

    public Path writeSource(String packageName, String className, String sourceCode) {
        try {
            Path packageDir = sourceRoot().resolve(packageName.replace('.', '/'));
            Files.createDirectories(packageDir);
            Path sourceFile = packageDir.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
            Files.createDirectories(classesRoot());
            return sourceFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write RuleTrans source file", e);
        }
    }
}
