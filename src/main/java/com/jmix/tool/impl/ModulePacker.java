package com.jmix.tool.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.ModuleAlgArtifact;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.omodel.AlgLoaderException;
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
 * 模块打包器
 * 负责将约束算法类打包成jar文件，支持单文件和多文件模式
 * 
 * @since 2025-09-27
 */
@Slf4j
public class ModulePacker {

    /**
     * 对算法进行打包
     * 
     * @param cpClazz    约束算法类
     * @param outRootDir 输出根目录
     * @return 输出目录路径
     */
    public String pack(Class<? extends ConstraintAlgImpl> cpClazz, String outRootDir) {
        Module module = ModuleGenneratorByAnno.buildModule(cpClazz);
        return pack(module, cpClazz, outRootDir);
    }

    /**
     * 对模块进行打包
     * 
     * @param module     模块对象
     * @param cpClazz    约束算法类
     * @param outRootDir 输出根目录
     * @return 输出目录路径
     */
    public String pack(Module module, Class<? extends ConstraintAlgImpl> cpClazz, String outRootDir) {
        try {
            String inputDir = CommHelper.getJavaFilePath(cpClazz);

            // 根据module的code和id在outRootDir创建这个module的子文件夹
            String outDir = module.getAlg().getModuleDirPath(outRootDir);

            // 如果目录已存在则删除
            Path outDirPath = Paths.get(outDir);
            if (Files.exists(outDirPath)) {
                log.info("Removing existing directory: {}", outDir);
                deleteDirectory(outDirPath);
            }

            // 创建输出目录
            Files.createDirectories(outDirPath);
            log.info("Created output directory: {}", outDir);

            // 保存Module到文件
            String moduleFilePath = module.getAlg().getBaseJsonPath(outRootDir);
            ModuleUtils.toJsonFile(module, moduleFilePath);
            log.info("Module saved to: {}", moduleFilePath);

            // 判断是否为多文件模式
            boolean isMultifile = isMultifile(cpClazz);
            log.info("Pack mode: {}", isMultifile ? "multifile" : "singlefile");

            // 打包class文件
            boolean classJarSuccess = packClassJar(inputDir, module, isMultifile, outDir);
            if (!classJarSuccess) {
                log.error("Failed to pack class jar");
                throw new AlgLoaderException("Failed to pack class jar");
            }

            // 打包源码文件
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
     * 判断是否为多文件模式
     * 
     * @param cpClassName 约束算法类
     * @return true表示多文件模式，false表示单文件模式
     */
    public boolean isMultifile(Class<? extends ConstraintAlgImpl> cpClassName) {
        // 如果cpClass继承了ConstraintAlgImpl且不是内部类，则是多个文件
        // 例如：com.jmix.scenario.hello.HelloConstraint (HelloConstraint extends
        // ConstraintAlgImpl)
        // 否则就是单文件，com.jmix.scenario.ruletest.ParaIsHiddenTest (ParaIsHiddenTest extends
        // ModuleScenarioTestBase)
        return ConstraintAlgImpl.class.isAssignableFrom(cpClassName)
                && cpClassName.getEnclosingClass() == null
                && !cpClassName.getSimpleName().endsWith("Test");
    }

    /**
     * 打包class文件为jar
     * 
     * @param inputDir    输入目录
     * @param module      模块对象
     * @param isMultifile 是否为多文件模式
     * @param outDir      输出目录
     * @return 是否成功
     */
    public boolean packClassJar(String inputDir, Module module, boolean isMultifile, String outDir) {
        try {
            String jarFilePath = module.getAlg()
                    .getRuntimeJarPath(outDir.substring(0, outDir.lastIndexOf(File.separator)));

            // 在target/test-classes目录下查找class文件
            String testClassesDir = inputDir.replace("src\\test\\java", "target\\test-classes");
            log.info("Looking for class files in: {}", testClassesDir);

            List<File> classFiles = new ArrayList<>();

            String baseClassRootDir = testClassesDir.substring(0, testClassesDir.lastIndexOf("\\com"));
            baseClassRootDir = baseClassRootDir + File.separator + ModuleAlgArtifact.ALG_PACKAGE.replace(".", "\\");
            findClassFiles(baseClassRootDir, ModuleAlgArtifact.ALG_BASE_CLASS, classFiles);
            findClassFiles(baseClassRootDir, ModuleAlgArtifact.ALG_PART_CLASS, classFiles);
            findClassFiles(baseClassRootDir, ModuleAlgArtifact.ALG_PARA_CLASS, classFiles);

            if (isMultifile) {
                // 多文件模式：需要将ConstraintAlg类及测试类对应的class打成jar包
                // 例如：HelloConstraint.class、HelloConstraintTest.class
                String constraintClassName = module.getCode() + "Constraint";
                String testClassName = module.getCode() + "ConstraintTest";

                // 查找对应的class文件
                findClassFiles(testClassesDir, constraintClassName, classFiles);
                findClassFiles(testClassesDir, testClassName, classFiles);
            } else {
                // 单文件模式：需要将ConstraintAlg类和其父类对应的class文件打成jar包
                // 例如：ParaIsHiddenTest.class、ParaIsHiddenTest$ParaIsHiddenConstraint.class
                String testClassName = module.getCode() + "Test";
                String constraintClassName = testClassName + "$" + module.getCode() + "Constraint";

                // 查找对应的class文件
                findClassFiles(testClassesDir, testClassName, classFiles);
                findClassFiles(testClassesDir, constraintClassName, classFiles);
            }

            if (classFiles.isEmpty()) {
                log.warn("No class files found for packing in {}", testClassesDir);
                return false;
            }

            // 创建jar文件
            createJarFile(jarFilePath, classFiles, testClassesDir);
            log.info("Class jar created: {}", jarFilePath);

            return true;

        } catch (Exception e) {
            log.error("Failed to pack class jar", e);
            return false;
        }
    }

    /**
     * 打包源码文件为jar
     * 
     * @param inputDir    输入目录
     * @param module      模块对象
     * @param isMultifile 是否为多文件模式
     * @param outDir      输出目录
     * @return 是否成功
     */
    public boolean packSourceJar(String inputDir, Module module, boolean isMultifile, String outDir) {
        try {
            String jarFilePath = module.getAlg()
                    .getSourceJarPath(outDir.substring(0, outDir.lastIndexOf(File.separator)));

            List<File> sourceFiles = new ArrayList<>();

            if (isMultifile) {
                // 多文件模式：需要将ConstraintAlg类及测试类源码
                // 例如：HelloConstraint.java、HelloConstraintTest.java
                String constraintClassName = module.getCode() + "Constraint";
                String testClassName = module.getCode() + "ConstraintTest";

                // 查找对应的java文件
                findSourceFiles(inputDir, constraintClassName, sourceFiles);
                findSourceFiles(inputDir, testClassName, sourceFiles);
            } else {
                // 单文件模式：需要将测试类（含ConstraintAlg类）源码
                // 例如：ParaIsHiddenTest.java，内部包含了ParaIsHiddenConstraint
                String testClassName = module.getCode() + "Test";

                // 查找对应的java文件
                findSourceFiles(inputDir, testClassName, sourceFiles);
            }

            if (sourceFiles.isEmpty()) {
                log.warn("No source files found for packing");
                return false;
            }

            // 创建jar文件
            createJarFile(jarFilePath, sourceFiles, inputDir);
            log.info("Source jar created: {}", jarFilePath);

            return true;

        } catch (Exception e) {
            log.error("Failed to pack source jar", e);
            return false;
        }
    }

    /**
     * 查找class文件
     * 
     * @param inputDir   输入目录
     * @param className  类名
     * @param classFiles 结果列表
     */
    private void findClassFiles(String inputDir, String className, List<File> classFiles) {
        String classFileName = className + ".class";
        // 标准化路径，解决Windows路径问题
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

    /**
     * 查找源码文件
     * 
     * @param inputDir    输入目录
     * @param className   类名
     * @param sourceFiles 结果列表
     */
    private void findSourceFiles(String inputDir, String className, List<File> sourceFiles) {
        String sourceFileName = className + ".java";
        // 标准化路径，解决Windows路径问题
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

    /**
     * 创建jar文件
     * 
     * @param jarFilePath jar文件路径
     * @param files       要打包的文件列表
     * @param baseDir     基础目录
     */
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

    /**
     * 递归删除目录
     * 
     * @param dir 目录路径
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件再删除目录
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
