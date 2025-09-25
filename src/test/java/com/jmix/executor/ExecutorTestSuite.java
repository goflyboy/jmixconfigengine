package com.jmix.executor;

import com.jmix.executor.impl.ModuleConstraintExecutorTest;
import com.jmix.executor.model.ModuleRefRelationGraphTest;
import com.jmix.executor.util.ModuleUtilsTest;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * JMix Config Executor 测试套件
 * 
 * 运行所有单元测试用例，包括：
 * 
 * @since 2025-09-23
 */
@Suite
@SelectClasses({
        ModuleConstraintExecutorTest.class,
        ModuleUtilsTest.class,
        ModuleRefRelationGraphTest.class
})
public class ExecutorTestSuite {
    // 测试套件

}