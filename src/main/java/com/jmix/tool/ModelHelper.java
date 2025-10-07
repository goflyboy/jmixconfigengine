package com.jmix.tool;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.impl.algmodel.ConstraintAlg;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.artifact.ModuleAlgArtifactGenerator;
import com.jmix.tool.artifact.ModuleVarInfo;
import com.jmix.tool.impl.ModelGenneratorException;
import com.jmix.tool.impl.ModuleGenerator;
import com.jmix.tool.impl.ModuleGenneratorByAnno;
import com.jmix.tool.impl.StructCodeInjector;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 模型文件生成器
 * 用于根据用户输入生成测试代码文件
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModelHelper {

    /**
     * 生成ModelFile文件（使用默认包名）
     * 
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorModelFile(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        generatorModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel,
                userLogicByPseudocode, "");
    }

    /**
     * 生成ModelFile文件
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }

    /**
     * 生成ModelFile文件（包含用户测试用例特殊规格）
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec      用户测试用例特殊规格
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 调用大模型（如:deepseek,Qwen）的深度思考模式
            ModuleGenerator moduleGenerator = new ModuleGenerator();
            String code = moduleGenerator.generatorModelCode(packageName, modelScenarioName, userVariableModel,
                    userLogicByPseudocode, userTestCaseSpec);

            // 根据code生成类 {modelScenarioName}Test.java
            String className = modelScenarioName + "Test";
            String fileName = className + ".java";

            // 类文件保存在当前工程的测试代码 + packageName
            String targetPath = getTestSourcePath(packageName);
            Path fullPath = Paths.get(targetPath, fileName);

            // 确保目录存在
            Files.createDirectories(fullPath.getParent());

            // 如果fullPath存在，则删除
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
            }

            // 写入文件，使用UTF-8编码
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(fullPath.toFile()), "UTF-8")) {
                writer.write(code);
            }

            log.info("Successfully generated test file: {}", fullPath.toAbsolutePath());

            // 尝试编译生成的Java文件
            String projectRoot = System.getProperty("user.dir");
            new com.jmix.tool.impl.ModuleCompiler().compile(projectRoot, fullPath.toString());

            // // 新增代码：自动注入约束代码
            // boolean isNeedInject = autoInjectConstraintCode(className, packageName);
            // if (isNeedInject) {
            // compileJavaFile(fullPath); // 再次编译这个文件
            // }

        } catch (Exception e) {
            throw new ModelGenneratorException("Failed to generate model file: " + e.getMessage(),
                    e);
        }
    }

    /**
     * 获取测试源码路径
     * 
     * @param packageName 包名
     * @return 测试源码路径
     */
    private String getTestSourcePath(String packageName) {
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");

        // 构建测试源码路径
        String packagePath = packageName.replace('.', File.separatorChar);
        return currentDir + File.separator + "src" + File.separator + "test" + File.separator + "java" + File.separator
                + packagePath;
    }

    /**
     * 生成ModelFile文件，并运行（使用默认包名）
     * 
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorRunModelFile(String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        generatorRunModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel,
                userLogicByPseudocode, "");
    }

    /**
     * 生成ModelFile文件，并运行
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName,
            String userVariableModel, String userLogicByPseudocode) {
        generatorRunModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }

    /**
     * 生成ModelFile文件，并运行（包含用户测试用例特殊规格）
     * 
     * @param packageName           包名
     * @param modelScenarioName     模型场景名称
     * @param userVariableModel     用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec      用户测试用例特殊规格
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName,
            String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 调用generatorModelFile生成文件（是一个测试用例），如：CalculateRuleSimpleTest.java
            generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode,
                    userTestCaseSpec);

            // 运行这个文件（运行这个测试类）
            runTestFile(packageName, modelScenarioName);

        } catch (Exception e) {
            throw new ModelGenneratorException(
                    "Failed to generate and run model file: " + e.getMessage(), e);
        }
    }

    /**
     * 运行测试文件
     * 
     * @param packageName       包名
     * @param modelScenarioName 模型场景名称
     */
    private void runTestFile(String packageName, String modelScenarioName) {
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

            // 检查是否有main方法
            try {
                Method mainMethod = testClass.getMethod("main", String[].class);
                log.info("Found main method, executing...");
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (NoSuchMethodException e) {
                log.info("No main method found, using JUnit to run test cases...");
                // runWithJUnit(testClass);
            }

        } catch (ClassNotFoundException e) {
            log.error("Test class not found: {}", e.getMessage());
            log.error("Please ensure the class has been generated and compiled correctly");
        } catch (Exception e) {
            log.error("Failed to run test file: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 编译Java文件
     * 
     * @param javaFilePath Java文件路径
     */
    // compile(javaDir, classDir, javaFilePath) 已迁移至 ModuleCompiler

    /**
     * 自动注入约束代码
     * 
     * @param className   类名
     * @param packageName 包名
     * @return 是否需要注入
     */
    public boolean autoInjectConstraintCode(String className, String packageName) {
        // 获取完整的类名
        String fullClassName = packageName + "." + className;
        Class<?> injectedClazz;
        try {
            injectedClazz = Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", fullClassName, e);
            throw new AlgLoaderException("Class not found: " + fullClassName, e);
        }
        return autoInjectConstraintCode(injectedClazz);
    }

    /**
     * 自动注入约束代码
     * 
     * @param injectedClazz 类
     * @return 是否需要注入
     */
    public boolean autoInjectConstraintCode(Class<?> injectedClazz) {
        try {
            // 创建临时路径
            String tempPath = CommHelper.createTempPath(injectedClazz);

            // 生成Module
            @SuppressWarnings("unchecked")
            Class<? extends ConstraintAlg> constraintAlgClass = (Class<? extends ConstraintAlg>) injectedClazz;
            Module module = ModuleGenneratorByAnno.build(constraintAlgClass, tempPath);

            // 检查是否需要注入
            boolean isNeedInject = checkNeedInject(module);

            if (isNeedInject) {
                // 执行注入操作
                performInjection(injectedClazz, module);
                log.info("✓ Automatic constraint code injection completed");
            }

            return isNeedInject;

        } catch (Exception e) {
            log.error("Failed to inject constraint code automatically: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查是否需要注入约束代码
     * 
     * @param module 模块对象
     * @return 是否需要注入
     */
    private boolean checkNeedInject(Module module) {
        if (module.getRules() == null) {
            return false;
        }

        for (Rule rule : module.getRules()) {
            if ("CDSL.V5.Struct.CompatibleRule".equals(rule.getRuleSchemaTypeFullName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 执行约束代码注入
     * 
     * @param injectedClazz 注入的类
     * @param module        模块对象
     * @throws Exception 注入过程中的异常
     */
    private void performInjection(Class<?> injectedClazz, Module module) throws Exception {
        // 生成ModuleInfo
        ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
        ModuleVarInfo moduleInfo = generator.buildModuleInfo(module);

        // 注入规则
        StructCodeInjector injector = new StructCodeInjector();
        injector.injectRule(injectedClazz, moduleInfo.getRules());
    }
}