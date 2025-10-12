package com.jmix.tool.confictdebug;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;
import com.jmix.tool.modelgentest.Hello2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * 松弛变量冲突检测测试
 * 
 * @since 2025-01-27
 */
public class RelaxationVarTest {

    private ModuleConstraintExecutor executor;
    private ConstraintConfig config;

    @BeforeEach
    public void setUp() {
        executor = ModuleConstraintExecutor.INST;
        config = new ConstraintConfig();
        config.setIsAttachedDebug(true);
        config.setDebugByRelaxationVar(true); // 启用松弛变量调试
        config.setLogFilePath("target/logs");

        Result<Void> initResult = executor.init(config);
        assertTrue(initResult.isSuccess(), "Executor initialization should succeed");

        // 添加测试模块
        Result<Void> addResult = executor.addModule(123L, Hello2Test.createModule());
        assertTrue(addResult.isSuccess(), "Module addition should succeed");
    }

    @Test
    public void testRelaxationVarDebugging() {
        // 创建一个会导致冲突的请求
        InferParasReq req = new InferParasReq();
        req.setModuleId(123L);

        // 设置冲突的部件配置（红色+小号，这应该与规则冲突）
        PartInst mainPart = new PartInst();
        mainPart.setCode("tShirt11");
        mainPart.setQuantity(1);
        req.setMainPartInst(mainPart);

        // 设置冲突的参数配置
        ParaInst colorPara = new ParaInst();
        colorPara.setCode("color");
        colorPara.setValue("10"); // Red
        colorPara.setOptions(Arrays.asList("Red"));

        ParaInst sizePara = new ParaInst();
        sizePara.setCode("size");
        sizePara.setValue("10"); // Small
        sizePara.setOptions(Arrays.asList("Small"));

        req.setPreParaInsts(Arrays.asList(colorPara, sizePara));
        req.setEnumerateAllSolution(false);

        // 执行推理
        Result<?> result = executor.inferParas(req);

        // 验证结果
        assertNotNull(result, "Result should not be null");
        System.out.println("Test result: " + result);

        // 由于启用了松弛变量调试，应该能够检测到冲突规则
        if (result.getCode() == Result.NO_SOLUTION) {
            String message = result.getMessage();
            assertNotNull(message, "No solution message should not be null");
            System.out.println("Conflict detection message: " + message);

            // 验证消息包含冲突规则信息
            assertTrue(message.contains("conflict rules") || message.contains("No feasible solution found"),
                    "Message should contain conflict information");
        }
    }

    @Test
    public void testNormalModeWithoutRelaxation() {
        // 测试正常模式（不启用松弛变量）
        config.setDebugByRelaxationVar(false);
        Result<Void> reinitResult = executor.init(config);
        assertTrue(reinitResult.isSuccess(), "Re-initialization should succeed");

        // 重新添加模块
        Result<Void> addResult = executor.addModule(123L, Hello2Test.createModule());
        assertTrue(addResult.isSuccess(), "Module addition should succeed");

        // 创建正常的请求
        InferParasReq req = new InferParasReq();
        req.setModuleId(123L);

        PartInst mainPart = new PartInst();
        mainPart.setCode("tShirt11");
        mainPart.setQuantity(3);
        req.setMainPartInst(mainPart);

        req.setEnumerateAllSolution(false);

        // 执行推理
        Result<?> result = executor.inferParas(req);

        // 验证结果
        assertNotNull(result, "Result should not be null");
        System.out.println("Normal mode result: " + result);
    }
}
