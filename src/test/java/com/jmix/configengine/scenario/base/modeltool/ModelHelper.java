package com.jmix.configengine.scenario.base.modeltool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.jmix.configengine.scenario.base.CommHelper;
import com.jmix.configengine.scenario.base.ModuleGenneratorByAnno;
import com.jmix.configengine.scenario.base.StructCodeInjector;
import com.jmix.configengine.artifact.ModuleAlgArtifactGenerator;
import com.jmix.configengine.model.Module;

/**
 * 模型文件生成器
 * 用于根据用户输入生成测试代码文件
 */
public class ModelHelper {
    
    /**
     * 生成ModelFile文件（使用默认包名）
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorModelFile(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        generatorModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成ModelFile文件
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成ModelFile文件（包含用户测试用例特殊规格）
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec 用户测试用例特殊规格
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 调用大模型（如:deepseek,Qwen）的深度思考模式
            LLMInvoker llmInvoker = new LLMInvoker();
            String code = llmInvoker.generatorModelCode(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, userTestCaseSpec);
            
            // 根据code生成类 {modelScenarioName}Test.java
            String className = modelScenarioName + "Test";
            String fileName = className + ".java";
            
            // 类文件保存在当前工程的测试代码 + packageName
            String targetPath = getTestSourcePath(packageName);
            Path fullPath = Paths.get(targetPath, fileName);
            
            // 确保目录存在
            Files.createDirectories(fullPath.getParent());

            //如果fullPath存在，则删除
            if(Files.exists(fullPath)){
                Files.delete(fullPath);
            }
            
            // 写入文件，使用UTF-8编码
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(fullPath.toFile()), "UTF-8")) {
                writer.write(code);
            }
            
            System.out.println("成功生成测试文件: " + fullPath.toAbsolutePath());
            
            // 尝试编译生成的Java文件
            compileJavaFile(fullPath);
            
            // 新增代码：自动注入约束代码
            boolean isNeedInject = autoInjectConstraintCode(className, packageName);
            if (isNeedInject) {
                compileJavaFile(fullPath); // 再次编译这个文件
            }
            
        } catch (Exception e) {
            throw new RuntimeException("生成模型文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取测试源码路径
     * @param packageName 包名
     * @return 测试源码路径
     */
    private String getTestSourcePath(String packageName) {
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");
        
        // 构建测试源码路径
        String packagePath = packageName.replace('.', '/');
        return currentDir + "/src/test/java/" + packagePath;
    }
    
    /**
     * 生成ModelFile文件，并运行（使用默认包名）
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorRunModelFile(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        generatorRunModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成ModelFile文件，并运行
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName, 
            String userVariableModel, String userLogicByPseudocode) {
        generatorRunModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }
    
    /**
     * 生成ModelFile文件，并运行（包含用户测试用例特殊规格）
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     * @param userTestCaseSpec 用户测试用例特殊规格
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName, 
            String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 调用generatorModelFile生成文件（是一个测试用例），如：CalculateRuleSimpleTest.java
            generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, userTestCaseSpec);
            
            // 运行这个文件（运行这个测试类）
            runTestFile(packageName, modelScenarioName);
            
        } catch (Exception e) {
            throw new RuntimeException("生成并运行模型文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 运行测试文件
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     */
    private void runTestFile(String packageName, String modelScenarioName) {
        try {
            String className = modelScenarioName + "Test";
            String fullClassName = packageName + "." + className;
            
            System.out.println("正在运行测试类: " + fullClassName);
            
            // 使用反射加载测试类
            Class<?> testClass = Class.forName(fullClassName);
            
            // 检查是否有main方法
            try {
                java.lang.reflect.Method mainMethod = testClass.getMethod("main", String[].class);
                System.out.println("找到main方法，正在执行...");
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (NoSuchMethodException e) {
                System.out.println("未找到main方法，使用JUnit运行测试用例...");
                runWithJUnit(testClass);
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("测试类未找到: " + e.getMessage());
            System.err.println("请确保类已正确生成并编译");
        } catch (Exception e) {
            System.err.println("运行测试文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用JUnit运行测试类
     * @param testClass 测试类
     */
    private void runWithJUnit(Class<?> testClass) {
        try {
            // 检查是否有@Test注解的方法
            java.lang.reflect.Method[] methods = testClass.getMethods();
            java.util.List<java.lang.reflect.Method> testMethods = new java.util.ArrayList<>();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.isAnnotationPresent(org.junit.Test.class)) {
                    testMethods.add(method);
                }
            }
            
            if (!testMethods.isEmpty()) {
                System.out.println("发现 " + testMethods.size() + " 个@Test方法，正在运行...");
                
                // 创建测试类实例
                Object testInstance = testClass.getDeclaredConstructor().newInstance();
                
                // 运行每个测试方法
                for (java.lang.reflect.Method testMethod : testMethods) {
                    try {
                        System.out.println("正在运行测试方法: " + testMethod.getName());
                        testMethod.invoke(testInstance);
                        System.out.println("✓ 测试方法 " + testMethod.getName() + " 执行成功");
                    } catch (Exception e) {
                        System.err.println("✗ 测试方法 " + testMethod.getName() + " 执行失败: " + e.getMessage());
                        if (e.getCause() != null) {
                            System.err.println("  原因: " + e.getCause().getMessage());
                        }
                    }
                }
                
                            System.out.println("所有测试方法执行完成");
            
        } else {
            System.out.println("未发现@Test方法，无法运行");
        }
        
    } catch (Exception e) {
        System.err.println("JUnit运行失败: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    /**
     * 编译Java文件
     * @param javaFilePath Java文件路径
     */
    private void compileJavaFile(Path javaFilePath) {
        try {
            System.out.println("正在编译Java文件: " + javaFilePath.getFileName());
            
            // 获取项目根目录
            String projectRoot = System.getProperty("user.dir");
            String classpath = projectRoot + "/target/classes;" + 
                             projectRoot + "/target/test-classes;" +
                             projectRoot + "/lib/*";
            
            // 构建编译命令
            ProcessBuilder pb = new ProcessBuilder(
                "javac", 
                "-cp", classpath,
                "-d", projectRoot + "/target/test-classes",
                javaFilePath.toString()
            );
            
            // 设置工作目录
            pb.directory(new java.io.File(projectRoot));
            
            // 执行编译
            Process process = pb.start();
            
            // 读取编译输出
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("编译输出: " + line);
                }
            }
            
            // 读取编译错误
            try (java.io.BufferedReader errorReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println("编译错误: " + line);
                }
            }
            
            // 等待编译完成
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✓ Java文件编译成功");
            } else {
                System.err.println("✗ Java文件编译失败，退出码: " + exitCode);
            }
            
        } catch (Exception e) {
            System.err.println("编译Java文件失败: " + e.getMessage());
            System.err.println("请确保已安装Java编译器(javac)");
        }
    }
        /**
     * 自动注入约束代码
     * @param className 类名
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
            throw new RuntimeException("类未找到: " + fullClassName, e);
        }
        return autoInjectConstraintCode(injectedClazz);
    }
        

    /**
     * 自动注入约束代码
     * @param className 类名
     * @param packageName 包名
     * @return 是否需要注入
     */
    public boolean autoInjectConstraintCode(Class<?> injectedClazz) {
        try {
            // 创建临时路径
            String tempPath = CommHelper.createTempPath(injectedClazz);
            
            // 生成Module
            Module module = ModuleGenneratorByAnno.build((Class<? extends com.jmix.configengine.artifact.ConstraintAlg>) injectedClazz, tempPath);
            
            // 检查是否需要注入：如果module.getRules()都不是CompatiableRule，则为false，否则为true
            boolean isNeedInject = false;
            if (module.getRules() != null) {
                for (com.jmix.configengine.model.Rule rule : module.getRules()) {
                    if ("CDSL.V5.Struct.CompatiableRule".equals(rule.getRuleSchemaTypeFullName())) {
                        isNeedInject = true;
                        break;
                    }
                }
            }
            
            if (isNeedInject) {
                // 生成ModuleInfo
                ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
                com.jmix.configengine.artifact.ModuleVarInfo moduleInfo = generator.buildModuleInfo(module);
                
                // 注入规则
                StructCodeInjector injector = new StructCodeInjector();
                injector.injectRule(injectedClazz, moduleInfo.getRules());
                
                System.out.println("✓ 自动注入约束代码完成");
            }
            
            return isNeedInject;
            
        } catch (Exception e) {
            System.err.println("自动注入约束代码失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 