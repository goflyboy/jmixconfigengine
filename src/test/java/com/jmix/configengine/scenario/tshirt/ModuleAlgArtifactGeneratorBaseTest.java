package com.jmix.configengine.scenario.tshirt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.configengine.artifact.ModuleAlgArtifactGenerator;
import com.jmix.configengine.model.*;
import com.jmix.configengine.util.ModuleUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ModuleAlgArtifactGenerator.buildConstraintRule 方法测试类
 * 
 * 测试目标：
 * 1. 根据T恤衫模块样例数据生成Module数据
 * 2. 调用ModuleAlgArtifactGenerator.buildConstraintRule生成约束规则代码
 * 3. 验证生成的代码文件是否正确
 */
public class ModuleAlgArtifactGeneratorBaseTest {
    
    private ModuleAlgArtifactGenerator generator;
    private com.jmix.configengine.model.Module tshirtModule;
    private String outputPath;
    //是否删除生成的文件
    private boolean deleteGeneratedFile = true;
    
    @Before
    public void setUp() throws Exception {
        // 初始化生成器
        generator = new ModuleAlgArtifactGenerator();
        
        // 设置输出路径为当前包所在目录
        outputPath = getCurrentPackagePath() + "/TShirtConstraint.java";
        
        // 创建T恤衫模块数据（简化版本，避免JSON反序列化问题）
        // tshirtModule = createSimpleTShirtModule();
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json"; 
        // 使用ModuleUtils.fromJsonFile方法读取JSON文件
        tshirtModule= ModuleUtils.fromJsonFile(jsonFilePath);
        
        // 初始化模块
        tshirtModule.init();
    }
    
    @Test
    public void testBuildConstraintRule() throws Exception {
        deleteGeneratedFile = false;
        System.out.println("=== 开始测试 buildConstraintRule 方法 ===");
        System.out.println("输出路径: " + outputPath);
        
        // 验证模块数据是否正确创建
        assertNotNull("T恤衫模块应该创建成功", tshirtModule);
        assertEquals("模块代码应该是TShirt", "TShirt", tshirtModule.getCode());
        assertNotNull("参数列表不应该为空", tshirtModule.getParas());
        assertNotNull("部件列表不应该为空", tshirtModule.getParts());
        assertNotNull("规则列表不应该为空", tshirtModule.getRules());
        
        System.out.println("✓ 模块数据验证通过");
        System.out.println("  参数数量: " + tshirtModule.getParas().size());
        System.out.println("  部件数量: " + tshirtModule.getParts().size());
        System.out.println("  规则数量: " + tshirtModule.getRules().size());
        
        // 调用buildConstraintRule方法
        System.out.println("\n=== 调用 buildConstraintRule 方法 ===");
        
        // 由于模板需要rule.left和rule.right属性，我们需要先手动构建这些信息
        // 或者修改模板来处理这种情况
        try {
            generator.buildConstraintRule(tshirtModule, outputPath);
        } catch (Exception e) {
            System.out.println("buildConstraintRule 调用失败，错误信息: " + e.getMessage());
            // 这是一个已知问题，模板需要rule.left和rule.right属性
            // 但当前的实现没有正确设置这些属性
            throw e;
        }
        
        // 验证生成的代码文件是否存在
        File generatedFile = new File(outputPath);
        assertTrue("生成的约束代码文件应该存在", generatedFile.exists());
        assertTrue("生成的约束代码文件应该可读", generatedFile.canRead());
        
        System.out.println("✓ 约束代码文件生成成功");
        System.out.println("  文件路径: " + generatedFile.getAbsolutePath());
        System.out.println("  文件大小: " + generatedFile.length() + " 字节");
        
        // 验证生成的代码内容
        validateGeneratedCode();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 验证生成的代码内容
     */
    private void validateGeneratedCode() throws Exception {
        Path path = Paths.get(outputPath);
        String content = new String(Files.readAllBytes(path));
        
        // 验证基本结构
        assertTrue("生成的代码应该包含类定义", content.contains("public class TShirtConstraint"));
        assertTrue("生成的代码应该继承ConstraintAlgImpl", content.contains("extends ConstraintAlgImpl"));
        
        // 验证参数变量
        assertTrue("应该包含Color参数变量", content.contains("private ParaVar ColorVar"));
        assertTrue("应该包含Size参数变量", content.contains("private ParaVar SizeVar"));
        
        // 验证部件变量
        assertTrue("应该包含TShirt11部件变量", content.contains("private PartVar TShirt11Var"));
        assertTrue("应该包含TShirt12部件变量", content.contains("private PartVar TShirt12Var"));
        
        // 验证方法
        assertTrue("应该包含initVariables方法", content.contains("public void initVariables()"));
        assertTrue("应该包含initConstraint方法", content.contains("public void initConstraint()"));
        
        // 验证约束规则方法
        assertTrue("应该包含规则1的约束方法", content.contains("addConstraint_rule1"));
        assertTrue("应该包含规则2的约束方法", content.contains("addConstraint_rule2"));
        assertTrue("应该包含规则3的约束方法", content.contains("addConstraint_rule3"));
        
        System.out.println("✓ 生成的代码内容验证通过");
        System.out.println("  包含正确的类定义");
        System.out.println("  包含所有参数变量");
        System.out.println("  包含所有部件变量");
        System.out.println("  包含所有约束规则方法");
    }
    
    /**
     * 获取当前包所在目录路径
     */
    private String getCurrentPackagePath() {
        // 获取当前类的包路径
        String packagePath = this.getClass().getPackage().getName().replace('.', '/');
        
        // 构建完整的目录路径
        String baseDir = "src/test/java";
        String fullPath = baseDir + "/" + packagePath;
        
        // 确保目录存在
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return fullPath;
    }
    
    @After
    public void tearDown() {
        // 清理生成的测试文件
        if (!deleteGeneratedFile) {
            return;
        }

        try {
            File generatedFile = new File(outputPath);
            if (generatedFile.exists()) {
                generatedFile.delete();
                System.out.println("清理测试文件: " + outputPath);
            }
        } catch (Exception e) {
            System.err.println("清理测试文件失败: " + e.getMessage());
        }
    }
} 