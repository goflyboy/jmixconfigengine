package com.jmix.configengine.scenario.base.modeltool;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        generatorModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel, userLogicByPseudocode);
    }
    
    /**
     * 生成ModelFile文件
     * @param packageName 包名
     * @param modelScenarioName 模型场景名称
     * @param userVariableModel 用户变量模型
     * @param userLogicByPseudocode 用户逻辑伪代码
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        try {
            // 调用大模型（如:deepseek,Qwen）的深度思考模式
            LLMInvoker llmInvoker = new LLMInvoker();
            String code = llmInvoker.generatorModelCode(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode);
            
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
            
            // 写入文件
            try (FileWriter writer = new FileWriter(fullPath.toFile())) {
                writer.write(code);
            }
            
            System.out.println("成功生成测试文件: " + fullPath.toAbsolutePath());
            
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
} 