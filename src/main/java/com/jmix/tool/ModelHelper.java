package com.jmix.tool;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.impl.algmodel.ConstraintAlg;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.artbuilder.ModuleAlgArtifactGenerator;
import com.jmix.tool.artbuilder.impl.ModuleVarInfo;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.impl.ModelGenneratorException;
import com.jmix.tool.impl.ModuleCompiler;
import com.jmix.tool.impl.ModuleGenerator;
import com.jmix.tool.impl.ModuleRunner;
import com.jmix.tool.impl.StructCodeInjector;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

            ModuleCompiler compiler = new ModuleCompiler();
            compiler.compile(projectRoot, fullPath.toString());
            log.info("Successfully compiled test file: {}", fullPath.toAbsolutePath());

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
        String currentDir = ""; // System.getProperty("user.dir"); 多Module的情况下，路径取的是跟project的地址
        String classPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        currentDir = classPath.substring(1, classPath.indexOf("/target"));
        currentDir = currentDir.replace('/', File.separatorChar);
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

            // 使用 ModuleRunner 运行这个文件（运行这个测试类）
            ModuleRunner moduleRunner = new ModuleRunner();
            moduleRunner.runTestFile(packageName, modelScenarioName);
            log.info("Successfully run test file: {}", packageName + "." + modelScenarioName);
        } catch (Exception e) {
            throw new ModelGenneratorException(
                    "Failed to generate and run model file: " + e.getMessage(), e);
        }
    }

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