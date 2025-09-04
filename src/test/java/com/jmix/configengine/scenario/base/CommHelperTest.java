package com.jmix.configengine.scenario.base;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/**
 * CommHelper测试类
 */
public class CommHelperTest {

    @Test
    public void testGetResourcePath() {
        // 测试获取资源路径
        String path = CommHelper.getResourcePath(CommHelperTest.class);
        assertNotNull("Resource path should not be null", path);
        assertTrue("Resource path should contain src/test/java", path.contains("src/test/java"));
        assertTrue("Resource path should contain package path", path.contains("com/jmix/configengine/scenario/base"));
    }

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
        assertTrue("Directory should be created", testDir.exists());
        assertTrue("Should be a directory", testDir.isDirectory());
        
        // 清理测试目录
        testDir.delete();
    }

    @Test
    public void testCreateTempPath() {
        // 测试创建临时路径
        String tempPath = CommHelper.createTempPath(CommHelperTest.class);
        
        assertNotNull("Temp path should not be null", tempPath);
        assertTrue("Temp path should contain tempResource", tempPath.contains("tempResource"));
        assertTrue("Temp path should contain package path", tempPath.contains("com/jmix/configengine/scenario/base"));
        
        // 验证临时目录已创建
        File tempDir = new File(tempPath);
        assertTrue("Temp directory should be created", tempDir.exists());
        assertTrue("Should be a directory", tempDir.isDirectory());
        
        // 清理测试目录
        tempDir.delete();
    }

    @Test
    public void testCreateTempPathWithExistingDirectory() {
        // 测试在已存在目录的情况下创建临时路径
        String tempPath = CommHelper.createTempPath(CommHelperTest.class);
        
        // 再次调用，应该不会出错
        String tempPath2 = CommHelper.createTempPath(CommHelperTest.class);
        
        assertEquals("Should return same path for same class", tempPath, tempPath2);
        
        // 清理测试目录
        new File(tempPath).delete();
    }
} 