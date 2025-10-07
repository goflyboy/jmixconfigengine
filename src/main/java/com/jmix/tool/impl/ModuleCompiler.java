package com.jmix.tool.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Java 源码编译器工具
 */
@Slf4j
public class ModuleCompiler {

    /**
     * 编译指定的 Java 源文件
     *
     * @param projDir      项目根目录
     * @param javaFilePath Java 源文件路径（可为绝对或相对路径）
     */
    public void compile(String projDir, String javaFilePath) {
        try {
            Path sourcePath = Paths.get(javaFilePath);
            if (!sourcePath.isAbsolute()) {
                sourcePath = Paths.get(projDir, javaFilePath).normalize();
            }

            log.info("Compiling Java file: {}", sourcePath.getFileName());

            String projectRoot = (projDir != null && !projDir.isEmpty()) ? projDir : System.getProperty("user.dir");
            String pathSep = File.pathSeparator;
            String fileSep = File.separator;

            String classesDir = projectRoot + fileSep + "target" + fileSep + "classes";
            String testClassesDir = projectRoot + fileSep + "target" + fileSep + "test-classes";
            String libWildcard = projectRoot + fileSep + "lib" + fileSep + "*";
            String classpath = classesDir + pathSep + testClassesDir + pathSep + libWildcard;

            // 根据源码路径选择输出目录
            String normalizedPath = sourcePath.toString().replace('\\', '/');
            String outDir = normalizedPath.contains("/src/test/java/") ? testClassesDir : classesDir;

            // 使用 JAVA_HOME 中的 javac，确保与项目 Java 版本一致
            String javaHome = System.getenv("JAVA_HOME");
            String javacPath = "javac";
            if (javaHome != null && !javaHome.isEmpty()) {
                javacPath = javaHome + fileSep + "bin" + fileSep + "javac";
                log.debug("Using javac from JAVA_HOME: {}", javacPath);
            } else {
                log.warn("JAVA_HOME not set, using system javac");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    javacPath,
                    "-encoding", "UTF-8",
                    "-cp", classpath,
                    "-d", outDir,
                    sourcePath.toString());

            // 在项目根目录执行，确保通配符与相对路径解析一致
            pb.directory(new File(projectRoot));

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Compilation output: {}", line);
                }
            }

            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.error("Compilation error: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Java file compiled successfully");
            } else {
                log.error("Java file compilation failed, exit code: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("Failed to compile Java file: {}", e.getMessage());
            log.error("Please ensure Java compiler (javac) is installed");
        }
    }
}
