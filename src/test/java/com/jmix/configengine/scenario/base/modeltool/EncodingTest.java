package com.jmix.configengine.scenario.base.modeltool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 编码测试类
 * 用于验证文件读写编码是否正确
 */
public class EncodingTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== 编码测试 ===\n");
            
            // 测试读取Markdown文件
            testReadMarkdownFile();
            
            // 测试写入文件
            testWriteFile();
            
            System.out.println("编码测试完成！");
            
        } catch (Exception e) {
            System.err.println("编码测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试读取Markdown文件
     */
    private static void testReadMarkdownFile() throws Exception {
        System.out.println("测试读取Markdown文件...");
        
        // 获取当前类文件所在目录
        String currentDir = EncodingTest.class.getResource(".").getPath();
        if (currentDir.startsWith("/")) {
            currentDir = currentDir.substring(1);
        }
        
        // 构建Markdown文件路径
        String markdownPath = currentDir + "ModelHelperTestBlockInput.md";
        Path path = Paths.get(markdownPath);
        
        if (Files.exists(path)) {
            // 使用UTF-8编码读取文件
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            System.out.println("成功读取Markdown文件，内容长度: " + content.length());
            
            // 检查是否包含中文乱码
            if (content.contains("�")) {
                System.out.println("警告: 检测到可能的乱码字符");
            } else {
                System.out.println("✓ 文件编码正确，无乱码");
            }
            
            // 检查是否包含英文注释
            if (content.contains("Logic 2:")) {
                System.out.println("✓ 包含正确的英文注释");
            } else {
                System.out.println("警告: 未找到预期的英文注释");
            }
            
        } else {
            System.out.println("Markdown文件不存在: " + path);
        }
    }
    
    /**
     * 测试写入文件
     */
    private static void testWriteFile() throws Exception {
        System.out.println("\n测试写入文件...");
        
        // 测试内容
        String testContent = "// Test comment: Logic 2: P1.visibilityModeVar and P2.visibilityModeVar are incompatible\n" +
                           "model.addDifferent((IntVar) P1.visibilityModeVar, (IntVar) P2.visibilityModeVar);";
        
        // 获取当前类文件所在目录
        String currentDir = EncodingTest.class.getResource(".").getPath();
        if (currentDir.startsWith("/")) {
            currentDir = currentDir.substring(1);
        }
        
        // 构建测试文件路径
        String testFilePath = currentDir + "encoding_test_output.java";
        Path path = Paths.get(testFilePath);
        
        // 使用UTF-8编码写入文件
        Files.write(path, testContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("✓ 成功写入测试文件: " + path);
        
        // 读取文件验证
        String readContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        if (readContent.equals(testContent)) {
            System.out.println("✓ 文件读写编码一致");
        } else {
            System.out.println("警告: 文件读写编码不一致");
        }
        
        // 清理测试文件
        Files.deleteIfExists(path);
        System.out.println("✓ 清理测试文件");
    }
} 