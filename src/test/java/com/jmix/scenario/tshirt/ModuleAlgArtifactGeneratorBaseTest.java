package com.jmix.scenario.tshirt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.artbuilder.ModuleAlgArtifactGenerator;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
 * 
 * @since 2025-09-23
 */
@Slf4j
public class ModuleAlgArtifactGeneratorBaseTest {

    private ModuleAlgArtifactGenerator generator;

    private Module tshirtModule;

    private String outputPath;

    // 是否删除生成的文件
    private boolean deleteGeneratedFile = true;

    /**
     * 测试设置方法
     * 
     * @throws Exception 异常
     */
    @BeforeEach
    public void setUp() throws Exception {
        // 初始化生成器
        generator = new ModuleAlgArtifactGenerator();

        // 设置输出路径为当前包所在目录
        outputPath = getCurrentPackagePath() + "/TShirtConstraint.java";

        // 创建T恤衫模块数据（简化版本，避免JSON反序列化问题）
        // tshirtModule = createSimpleTShirtModule();
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json";
        // 使用ModuleUtils.fromJsonFile方法读取JSON文件
        tshirtModule = ModuleUtils.fromJsonFile(jsonFilePath);

        // 初始化模块
        tshirtModule.init();
    }

    /**
     * 测试buildConstraintRule方法
     * 
     * @throws Exception 异常
     */
    @Test
    @Disabled
    public void testBuildConstraintRule() throws Exception {
        deleteGeneratedFile = false;
        log.info("=== Starting test for buildConstraintRule method ===");
        log.info("Output path: {}", outputPath);

        // 验证模块数据是否正确创建
        assertNotNull(tshirtModule, "T恤衫模块应该创建成功");
        assertEquals("模块代码应该是TShirt", "TShirt", tshirtModule.getCode());
        assertNotNull(tshirtModule.getParas(), "参数列表不应该为空");
        assertNotNull(tshirtModule.getParts(), "部件列表不应该为空");
        assertNotNull(tshirtModule.getRules(), "规则列表不应该为空");

        log.info("✓ Module data validation passed");
        log.info("  Parameter count: {}", tshirtModule.getParas().size());
        log.info("  Part count: {}", tshirtModule.getParts().size());
        log.info("  Rule count: {}", tshirtModule.getRules().size());

        // 调用buildConstraintRule方法
        log.info("\n=== Calling buildConstraintRule method ===");

        // 由于模板需要rule.left和rule.right属性，我们需要先手动构建这些信息
        // 或者修改模板来处理这种情况
        try {
            generator.buildConstraintRule(tshirtModule, outputPath);
        } catch (Exception e) {
            log.error("buildConstraintRule call failed, error message: {}", e.getMessage());
            // 这是一个已知问题，模板需要rule.left和rule.right属性
            // 但当前的实现没有正确设置这些属性
            throw new AlgLoaderException("buildConstraintRule call failed, error message: " + e.getMessage());
        }

        // 验证生成的代码文件是否存在
        File generatedFile = new File(outputPath);
        assertTrue(generatedFile.exists(), "生成的约束代码文件应该存在");
        assertTrue(generatedFile.canRead(), "生成的约束代码文件应该可读");

        log.info("✓ Constraint code file generated successfully");
        log.info("  File path: {}", generatedFile.getAbsolutePath());
        log.info("  File size: {} bytes", generatedFile.length());

        // 验证生成的代码内容
        validateGeneratedCode();

        log.info("\n=== Test completed ===");
    }

    /**
     * 验证生成的代码内容
     */
    private void validateGeneratedCode() throws Exception {
        Path path = Paths.get(outputPath);
        String content = new String(Files.readAllBytes(path));

        // 验证基本结构
        assertTrue(content.contains("public class TShirtConstraint"), "生成的代码应该包含类定义");
        assertTrue(content.contains("extends ConstraintAlgImplTestBase"), "生成的代码应该继承ConstraintAlgImplTestBase");

        // 验证参数变量
        assertTrue(content.contains("private ParaVar colorVar"), "应该包含Color参数变量");
        assertTrue(content.contains("private ParaVar sizeVar"), "应该包含Size参数变量");

        // 验证部件变量
        assertTrue(content.contains("private PartVar tShirt11Var"), "应该包含TShirt11部件变量");
        assertTrue(content.contains("private PartVar tShirt12Var"), "应该包含TShirt12部件变量");

        // 验证方法
        assertTrue(content.contains("public void initVariables()"), "应该包含initVariables方法");
        assertTrue(content.contains("public void initConstraint()"), "应该包含initConstraint方法");

        // 验证约束规则方法
        assertTrue(content.contains("addConstraint_rule1"), "应该包含规则1的约束方法");
        assertTrue(content.contains("addConstraint_rule2"), "应该包含规则2的约束方法");
        assertTrue(content.contains("addConstraint_rule3"), "应该包含规则3的约束方法");

        log.info("✓ Generated code content validation passed");
        log.info("  Contains correct class definition");
        log.info("  Contains all parameter variables");
        log.info("  Contains all part variables");
        log.info("  Contains all constraint rule methods");
    }

    private String getCurrentPackagePath() {
        // 获取当前类的包路径
        String packagePath = this.getClass().getPackage().getName().replace('.', File.separatorChar);

        // 构建完整的目录路径
        String baseDir = "src/test/java";
        String fullPath = baseDir + File.separator + packagePath;

        // 确保目录存在
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return fullPath;
    }

    /**
     * 测试清理方法
     */
    @AfterEach
    public void tearDown() {
        // 清理生成的测试文件
        if (!deleteGeneratedFile) {
            return;
        }

        try {
            File generatedFile = new File(outputPath);
            if (generatedFile.exists()) {
                generatedFile.delete();
                log.info("Cleaning up test file: {}", outputPath);
            }
        } catch (Exception e) {
            log.error("Failed to clean up test file: {}", e.getMessage());
        }
    }
}