package com.jmix.scenario.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * Hello约束算法简单测试类
 * 不依赖ModuleScenarioTestBase框架，直接使用ModuleConstraintExecutor.INST进行测试
 * 
 * @since 2025-11-15
 */
@Slf4j
public class HelloConstraintSimpleTest {

    private String tempResourcePath;
    private Module module;

    /**
     * 测试前初始化
     */
    @BeforeEach
    public void setUp() {
        // 生成临时资源路径
        tempResourcePath = CommHelper.createTempPath(HelloConstraint.class);

        // 初始化配置
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setLogFilePath(tempResourcePath);
        config.setLogModelProto(true);
        config.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);

        String rootFilePath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "cproot";
        config.setRootFilePath(rootFilePath);

        // 初始化执行器
        Result<Void> initResult = ModuleConstraintExecutor.INST.init(config);
        assertEquals(Result.SUCCESS, initResult.getCode(),
                "Executor initialization failed: " + initResult.getMessage());

        // 构建并添加模块
        module = ModuleGenneratorByAnno.build(HelloConstraint.class, tempResourcePath);
        Result<Void> addModuleResult = ModuleConstraintExecutor.INST.addModule(module.getId(), module);
        assertEquals(Result.SUCCESS, addModuleResult.getCode(),
                "Add module failed: " + addModuleResult.getMessage());

        log.info("Test setup completed, module ID: {}", module.getId());
    }

    /**
     * 测试后清理
     */
    @AfterEach
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
        log.info("Test teardown completed");
    }

    /**
     * 测试只有一个解的情况
     * 场景：tShirt11数量为1时，应该只有一个解：color=Red, size=Small
     */
    @Test
    public void testOnlyOneSolution() {
        // 构建基础数据
        // ......
        // 构建推理请求
        InferParasReq req = new InferParasReq();
        req.setModuleId(module.getId());
        req.setEnumerateAllSolution(false); // 只返回一个解

        // 设置主部件实例：tShirt11数量为1
        PartInst mainPartInst = new PartInst();
        mainPartInst.setCode("tShirt11");
        mainPartInst.setQuantity(1);
        req.setMainPartInst(mainPartInst);

        // 执行参数推理
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);

        // 验证结果
        assertEquals(Result.SUCCESS, result.getCode(),
                "Inference failed: " + result.getMessage());
        assertNotNull(result.getData(), "Result data should not be null");
        assertEquals(1, result.getData().size(), "Should have exactly one solution");

        // 验证第一个解的参数值
        ModuleInst solution = result.getData().get(0);
        assertNotNull(solution, "Solution should not be null");

        // 验证color参数为Red
        ParaInst colorPara = solution.getParas().stream()
                .filter(p -> "color".equals(p.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(colorPara, "Color parameter should exist");
        assertEquals("1", colorPara.getValue(), "Color should be Red");

        // 验证size参数为Small
        ParaInst sizePara = solution.getParas().stream()
                .filter(p -> "size".equals(p.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(sizePara, "Size parameter should exist");
        assertEquals("10", sizePara.getValue(), "Size should be Small");

        log.info("Test passed: color={}, size={}", colorPara.getValue(), sizePara.getValue());
    }
}
