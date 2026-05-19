package com.jmix.tool.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Packs module algorithm artifacts into runtime and source jars.
 *
 * @since 2025-09-27
 */
@Slf4j
public class ModulePacker {

    /**
     * Packs an algorithm class.
     *
     * @param cpClazz    algorithm class
     * @param outRootDir output root directory
     * @return output directory path
     */
    public String pack(Class<?> cpClazz, String outRootDir) {
        Module module = ModuleGenneratorByAnno.buildModule(cpClazz);
        return pack(module, cpClazz, outRootDir);
    }

    /**
     * Packs a module and its algorithm class.
     *
     * @param module     module metadata
     * @param cpClazz    algorithm class
     * @param outRootDir output root directory
     * @return output directory path
     */
    public String pack(Module module, Class<?> cpClazz, String outRootDir) {
        try {
            String inputDir = CommHelper.getJavaFilePath(cpClazz);

            String outDir = module.getAlg().getModuleDirPath(outRootDir);

            Path outDirPath = Paths.get(outDir);
            if (Files.exists(outDirPath)) {
                log.info("Removing existing directory: {}", outDir);
                deleteDirectory(outDirPath);
            }

            Files.createDirectories(outDirPath);
            log.info("Created output directory: {}", outDir);

            String moduleFilePath = module.getAlg().getBaseJsonPath(outRootDir);
            ModuleUtils.toJsonFile(module, moduleFilePath);
            log.info("Module saved to: {}", moduleFilePath);

            boolean isMultifile = isMultifile(cpClazz);
            log.info("Pack mode: {}", isMultifile ? "multifile" : "singlefile");

            boolean classJarSuccess = packClassJar(inputDir, module, isMultifile, outDir);
            if (!classJarSuccess) {
                log.error("Failed to pack class jar");
                throw new AlgLoaderException("Failed to pack class jar");
            }

            boolean sourceJarSuccess = packSourceJar(inputDir, module, isMultifile, outDir);
            if (!sourceJarSuccess) {
                log.error("Failed to pack source jar");
                throw new AlgLoaderException("Failed to pack source jar");
            }

            log.info("Module packed successfully to: {}", outDir);
            return outDir;

        } catch (Exception e) {
            log.error("Failed to pack module", e);
            throw new AlgLoaderException("Failed to pack module: " + e.getMessage(), e);
        }
    }

    /**
     * Returns whether the algorithm is represented by multiple source files.
     *
     * @param cpClassName algorithm class
     * @return true for multifile algorithms, false for single-file tests
     */
    public boolean isMultifile(Class<?> cpClassName) {
        return cpClassName.getEnclosingClass() == null
                && !cpClassName.getSimpleName().endsWith("Test");
    }

    /**
     * Packs class files into the runtime jar.
     *
     * @param inputDir    source input directory
     * @param module      module metadata
     * @param isMultifile whether this is a multifile algorithm
     * @param outDir      output directory
     * @return whether packing succeeded
     */
    public boolean packClassJar(String inputDir, Module module, boolean isMultifile, String outDir) {
        try {
            String jarFilePath = module.getAlg()
                    .getRuntimeJarPath(outDir.substring(0, outDir.lastIndexOf(File.separator)));

            String testClassesDir = inputDir.replace("src\\test\\java", "target\\test-classes");
            log.info("Looking for class files in: {}", testClassesDir);

            List<File> classFiles = new ArrayList<>();

            if (isMultifile) {
                String constraintClassName = module.getCode() + "Constraint";
                String testClassName = module.getCode() + "ConstraintTest";

                findClassFiles(testClassesDir, constraintClassName, classFiles);
                findClassFiles(testClassesDir, testClassName, classFiles);
            } else {
                String testClassName = module.getCode() + "Test";
                String constraintClassName = testClassName + "$" + module.getCode() + "Constraint";

                findClassFiles(testClassesDir, testClassName, classFiles);
                findClassFiles(testClassesDir, constraintClassName, classFiles);
            }

            if (classFiles.isEmpty()) {
                log.warn("No class files found for packing in {}", testClassesDir);
                return false;
            }

            createJarFile(jarFilePath, classFiles, testClassesDir);
            log.info("Class jar created: {}", jarFilePath);

            return true;

        } catch (Exception e) {
            log.error("Failed to pack class jar", e);
            return false;
        }
    }

    /**
     * Packs source files into the source jar.
     *
     * @param inputDir    source input directory
     * @param module      module metadata
     * @param isMultifile whether this is a multifile algorithm
     * @param outDir      output directory
     * @return whether packing succeeded
     */
    public boolean packSourceJar(String inputDir, Module module, boolean isMultifile, String outDir) {
        try {
            String jarFilePath = module.getAlg()
                    .getSourceJarPath(outDir.substring(0, outDir.lastIndexOf(File.separator)));

            List<File> sourceFiles = new ArrayList<>();

            if (isMultifile) {
                String constraintClassName = module.getCode() + "Constraint";
                String testClassName = module.getCode() + "ConstraintTest";

                findSourceFiles(inputDir, constraintClassName, sourceFiles);
                findSourceFiles(inputDir, testClassName, sourceFiles);
            } else {
                String testClassName = module.getCode() + "Test";

                findSourceFiles(inputDir, testClassName, sourceFiles);
            }

            if (sourceFiles.isEmpty()) {
                log.warn("No source files found for packing");
                return false;
            }

            createJarFile(jarFilePath, sourceFiles, inputDir);
            log.info("Source jar created: {}", jarFilePath);

            return true;

        } catch (Exception e) {
            log.error("Failed to pack source jar", e);
            return false;
        }
    }

    private void findClassFiles(String inputDir, String className, List<File> classFiles) {
        String classFileName = className + ".class";
        if (inputDir.startsWith("/")) {
            inputDir = inputDir.substring(1, inputDir.length());
        }
        String normalizedInputDir = inputDir.replace("\\", File.separator).replace("/", File.separator);
        Path classFilePath = Paths.get(normalizedInputDir, classFileName);

        if (Files.exists(classFilePath)) {
            classFiles.add(classFilePath.toFile());
            log.info("Found class file: {}", classFileName);
        } else {
            log.info("Class file not found: {}", classFileName);
        }
    }

    private void findSourceFiles(String inputDir, String className, List<File> sourceFiles) {
        String sourceFileName = className + ".java";
        if (inputDir.startsWith("/")) {
            inputDir = inputDir.substring(1, inputDir.length());
        }
        String normalizedInputDir = inputDir.replace("\\", File.separator).replace("/", File.separator);
        Path sourceFilePath = Paths.get(normalizedInputDir, sourceFileName);

        if (Files.exists(sourceFilePath)) {
            sourceFiles.add(sourceFilePath.toFile());
            log.info("Found source file: {}", sourceFileName);
        } else {
            log.info("Source file not found: {}", sourceFileName);
        }
    }

    private void createJarFile(String jarFilePath, List<File> files, String baseDir) throws IOException {
        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFilePath), new Manifest())) {
            for (File file : files) {
                JarEntry jarEntry = new JarEntry(file.getName());
                jarOut.putNextEntry(jarEntry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        jarOut.write(buffer, 0, bytesRead);
                    }
                }
                jarOut.closeEntry();
                log.info("Added to jar: {}", file.getName());
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
        }
    }
}
