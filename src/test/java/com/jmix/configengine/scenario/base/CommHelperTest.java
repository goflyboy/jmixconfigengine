package com.jmix.configengine.scenario.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.impl.util.CommHelper;

import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * CommHelper测试类
 */
public class CommHelperTest {

    /**
     * 测试getResourcePath方法
     */
    @Test
    public void testGetResourcePath() {
        // 测试获取资源路径
        String path = CommHelper.getResourcePath(CommHelperTest.class);
        assertNotNull(path, "Resource path should not be null");
        assertTrue(path.contains("src/test/java"), "Resource path should contain src/test/java");
        assertTrue(path.contains("com/jmix/configengine/scenario/base"), "Resource path should contain package path");
    }

    /**
     * 测试createDirectory方法
     */
    @Test
    public void testCreateDirectory() {
        // 测试创建目录
        String testDirPath = "src/test/java/com/jmix/configengine/scenario/base/testDir";

        // 确保目录不存在
        File testDir = new File(testDirPath);
        if (testDir.exists()) {
            testDir.delete();
        }

        // 创建目录
        CommHelper.createDirectory(testDirPath);

        // 验证目录已创建
        assertTrue(testDir.exists(), "Directory should be created");
        assertTrue(testDir.isDirectory(), "Should be a directory");

        // 清理测试目录
        testDir.delete();
    }

    /**
     * 测试createTempPath方法
     */
    @Test
    public void testCreateTempPath() {
        // 测试创建临时路径
        String tempPath = CommHelper.createTempPath(CommHelperTest.class);

        assertNotNull(tempPath, "Temp path should not be null");
        assertTrue(tempPath.contains("tempResource"), "Temp path should contain tempResource");
        assertTrue(tempPath.contains("com/jmix/configengine/scenario/base"), "Temp path should contain package path");

        // 验证临时目录已创建
        File tempDir = new File(tempPath);
        assertTrue(tempDir.exists(), "Temp directory should be created");
        assertTrue(tempDir.isDirectory(), "Should be a directory");

        // 清理测试目录
        tempDir.delete();
    }

    /**
     * 测试createTempPath方法（已存在目录的情况）
     */
    @Test
    public void testCreateTempPathWithExistingDirectory() {
        // 测试在已存在目录的情况下创建临时路径
        String tempPath = CommHelper.createTempPath(CommHelperTest.class);

        // 再次调用，应该不会出错
        String tempPath2 = CommHelper.createTempPath(CommHelperTest.class);

        assertEquals(tempPath, tempPath2, "Should return same path for same class");

        // 清理测试目录
        new File(tempPath).delete();
    }
}