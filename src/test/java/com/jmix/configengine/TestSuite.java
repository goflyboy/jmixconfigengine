package com.jmix.configengine;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jmix.configengine.artifact.FilterExpressionExecutorTest;
import com.jmix.configengine.artifact.ModuleAlgArtifactGeneratorTest;
import com.jmix.configengine.scenario.hello.HelloConstraintTest;
import com.jmix.configengine.scenario.ruletest.CalculateRuleIfThenTest.CalculateRuleConstraint;
import com.jmix.configengine.util.ModuleUtilsTest;

/**
 * JMix Config Engine 测试套件
 * 
 * 运行所有单元测试用例，包括：
 * - 核心功能测试
 * - 工具类测试
 * - 场景测试
 * - 集成测试
 */
@RunWith(Suite.class)
@SuiteClasses({
    
    // 核心功能测试
    FilterExpressionExecutorTest.class,
    ModuleAlgArtifactGeneratorTest.class,
    ModuleConstraintExecutorTest.class,
    // 工具类测试
    ModuleUtilsTest.class,
    HelloConstraintTest.class,
    
    // 场景测试
    // com.jmix.configengine.scenario.tshirt.ModuleAlgArtifactGeneratorBaseTest.class, TODO
    com.jmix.configengine.scenario.ruletest.ParaIntegerTest.class,
    com.jmix.configengine.scenario.ruletest.MyTShirtTest.class,
    com.jmix.configengine.scenario.ruletest.CalculateRuleIfThenTest.class,
    com.jmix.configengine.scenario.ruletest.CalculateRuleSimpleTest.class,
    com.jmix.configengine.scenario.ruletest.CompatiableRuleRequireTest.class,
    com.jmix.configengine.scenario.ruletest.ParaIsHiddenTest.class

})
public class TestSuite {
    
    /**
     * 测试套件说明
     * 
     * 本测试套件包含以下测试类别：
     * 
     * 1. 核心功能测试 (Core Functionality Tests)
     *    - FilterExpressionExecutorTest: 过滤表达式执行器测试
     *    - ModuleAlgArtifactGeneratorTest: 模块算法制品生成器测试
     * 
     * 2. 工具类测试 (Utility Tests)
     *    - ModuleUtilsTest: Module工具类测试，包括JSON序列化/反序列化
     * 
     * 3. 场景测试 (Scenario Tests)
     *    - TShirtModuleAlgArtifactGeneratorTest: T恤衫模块场景测试
     * 
     * 运行方式：
     * - 在IDE中右键运行TestSuite类
     * - 使用Maven命令: mvn test -Dtest=TestSuite
     * - 使用JUnit运行器
     */
    
    /**
     * 测试套件配置
     * 
     * 注意事项：
     * 1. 所有测试类必须使用JUnit 4注解
     * 2. 测试方法应该相互独立，不依赖执行顺序
     * 3. 测试数据应该使用独立的测试数据集
     * 4. 测试完成后应该清理测试资源
     * 
     * 测试覆盖率目标：
     * - 行覆盖率: > 80%
     * - 分支覆盖率: > 70%
     * - 方法覆盖率: > 90%
     */
} 