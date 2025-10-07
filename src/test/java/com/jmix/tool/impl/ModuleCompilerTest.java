package com.jmix.tool.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.impl.util.CommHelper;
import com.jmix.scenario.ruletest.ParaIntegerTest;

import org.junit.jupiter.api.Test;

import java.io.File;

public class ModuleCompilerTest {

    @Test
    public void compileParaIntegerTest() {
        String projectRoot = System.getProperty("user.dir");
        String fileSep = File.separator;

        // 使用 CommHelper 基于类获取源码目录
        String javaDir = CommHelper.getJavaFilePath(ParaIntegerTest.class);
        String javaPath = javaDir + fileSep + "ParaIntegerTest.java";

        // 编译前先删除目标 class 文件，保证幂等
        String classRelPath = ParaIntegerTest.class.getName().replace('.', File.separatorChar) + ".class";
        File out = new File(projectRoot + fileSep + "target" + fileSep + "test-classes" + fileSep
                + classRelPath);
        if (out.exists()) {
            // noinspection ResultOfMethodCallIgnored
            out.delete();
        }

        new ModuleCompiler().compile(projectRoot, javaPath);

        assertTrue(out.exists(), "Compiled class file should exist");
    }
}
