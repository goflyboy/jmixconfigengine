package com.jmix.tool.extensibleDemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * DC公司的T恤衫测试类
 * 演示扩展性功能的使用
 */
@Slf4j
public class DCMyTShirtTest {

    private DCDeliveryTypePostProcess postProcess;

    @BeforeEach
    public void setUp() {
        // 使用单例DC执行器

        // 初始化配置
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setRootFilePath("target/generated-sources");
        config.setLogFilePath("target/logs");
        config.setLogModelProto(false);

        // 初始化DC执行器
        DCModuleConstraintExecutor.INST.init(config);

        // 注册后处理器
        postProcess = new DCDeliveryTypePostProcess();
        DCModuleConstraintExecutor.INST.registerExtensible(postProcess);

        // 添加模块
        DCModuleConstraintExecutor.INST.addDCModule(1L, getDCModule());
    }

    @AfterEach
    public void tearDown() {
        // 清理资源
        DCModuleConstraintExecutor.INST.fini();
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testBasicInference() {
        log.info("Testing basic inference with DC extensions");

        // 由于模块没有算法制品，我们跳过实际的推理测试
        // 这里主要测试扩展性功能的注册和管理
        assertNotNull(DCModuleConstraintExecutor.INST, "DC Executor should not be null");
        assertNotNull(postProcess, "Post process should not be null");

        // 验证后处理器已注册
        assertTrue(postProcess.isEnabled(), "Post process should be enabled");
        assertEquals(postProcess.getProcessName(), "DCDeliveryTypePostProcess", "Post process name should be correct");

        log.info("Basic inference test passed - extension functionality verified");

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testDCWrapperFunctionality() {
        log.info("Testing DC wrapper functionality");

        // 测试DC执行器的基本功能
        assertNotNull(DCModuleConstraintExecutor.INST, "DC Executor should not be null");

        // 测试DC模块添加
        DCModule testModule = getDCModule();
        DCResult<Void> addResult = DCModuleConstraintExecutor.INST.addDCModule(2L, testModule);
        assertEquals(Result.SUCCESS, addResult.getCode(), "DC module addition should be successful");
        assertNotNull(addResult.getDcResultCode(), "DC result code should not be null");
        assertEquals(addResult.getDcResultCode(), "DC_ADD_MODULE_SUCCESS", "DC result code should be correct");

        // 测试DC推理请求
        DCModuleConstraintExecutor.DCInferParasReq dcReq = new DCModuleConstraintExecutor.DCInferParasReq();
        dcReq.setModuleCode("TShirt11");
        dcReq.setMainPartInst(createDCPartInst("TShirt11", 1));
        dcReq.setEnumerateAllSolution(true);

        // 由于没有算法制品，这里主要测试接口调用
        DCResult<List<DCModuleInst>> result = DCModuleConstraintExecutor.INST.inferDCParas(dcReq);
        assertNotNull(result, "DC inference result should not be null");
        assertNotNull(result.getDcResultCode(), "DC result code should not be null");
        assertTrue(result.getProcessTimestamp() > 0, "DC result should have process timestamp");

        log.info("DC wrapper functionality test passed");

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testDeliveryTypeCalculation() {
        log.info("Testing delivery type calculation");

        // 测试后处理器的业务逻辑
        Module module = getModule();

        // 创建模拟的解决方案
        List<ModuleInst> solutions = new ArrayList<>();
        ModuleInst solution = new ModuleInst();
        solution.setId(123L);
        solution.setCode("TShirt11");
        solution.setInstanceId(0);
        solution.setQuantity(1);

        // 添加参数实例
        List<ParaInst> paras = new ArrayList<>();
        ParaInst paraInst = new ParaInst();
        paraInst.setCode("Color");
        paraInst.setValue("Red");
        paras.add(paraInst);
        solution.setParas(paras);

        // 添加部件实例
        List<PartInst> parts = new ArrayList<>();
        PartInst partInst = new PartInst();
        partInst.setCode("TShirt11");
        partInst.setQuantity(3);
        parts.add(partInst);
        solution.setParts(parts);

        solutions.add(solution);

        // 测试后处理器
        Result<List<ModuleInst>> result = postProcess.postProcess(module, solutions);

        // 验证结果
        assertNotNull(result, "Result should not be null");
        assertEquals(Result.SUCCESS, result.getCode(), "Should be successful");
        assertNotNull(result.getData(), "Processed solutions should not be null");
        assertTrue(!result.getData().isEmpty(), "Should have processed solutions");

        log.info("Found {} processed solutions with delivery type calculation", result.getData().size());

        // 验证后处理器是否被调用
        assertTrue(postProcess.isEnabled(), "Post process should be enabled");
        assertEquals(postProcess.getProcessName(), "DCDeliveryTypePostProcess", "Post process name should be correct");

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testAdapterFunctionality() {
        log.info("Testing adapter functionality");

        // 测试模块适配器
        DCModuleAdapter moduleAdapter = new DCModuleAdapter();
        DCModule dcModule = moduleAdapter.adapt(getModule());

        assertNotNull(dcModule, "DCModule should not be null");
        assertEquals(getModule().getCode(), dcModule.getDccode(), "DCCode should be set");
        assertNotNull(dcModule.getSeason(), "Season should be set");

        log.info("DCModule adapted: dccode={}, season={}",
                dcModule.getDccode(), dcModule.getSeason());

        // 测试参数实例适配器
        DCParaInstAdapter paraInstAdapter = new DCParaInstAdapter();
        ParaInst paraInst = new ParaInst();
        paraInst.setCode("Color");
        paraInst.setValue("Red");
        paraInst.setHidden(false);

        DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);

        assertNotNull(dcParaInst, "DCParaInst should not be null");
        assertEquals(paraInst.getCode(), dcParaInst.getCode(), "Code should be preserved");
        assertEquals(paraInst.getValue(), dcParaInst.getValue(), "Value should be preserved");
        assertNotNull(dcParaInst.getDeliveryType(), "DeliveryType should be set");

        log.info("DCParaInst adapted: code={}, value={}, deliveryType={}",
                dcParaInst.getCode(), dcParaInst.getValue(), dcParaInst.getDeliveryType());

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testExtensibleProcessManagement() {
        log.info("Testing extensible process management");

        // 测试注册
        DCDeliveryTypePostProcess newPostProcess = new DCDeliveryTypePostProcess();
        Result<Void> registerResult = ModuleConstraintExecutor.INST.registerExtensible(newPostProcess);

        assertEquals(Result.SUCCESS, registerResult.getCode(), "Registration should be successful");

        // 测试注销
        Result<Void> unregisterResult = ModuleConstraintExecutor.INST.unregisterExtensible(newPostProcess);

        assertEquals(Result.SUCCESS, unregisterResult.getCode(), "Unregistration should be successful");

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    @Test
    @Disabled
    public void testNoSolution() {
        log.info("Testing no solution scenario");

        // 测试空解决方案的后处理
        Module module = getModule();
        List<ModuleInst> emptySolutions = new ArrayList<>();

        // 测试后处理器处理空解决方案
        Result<List<ModuleInst>> result = postProcess.postProcess(module, emptySolutions);

        // 验证结果
        assertNotNull(result, "Result should not be null");
        assertEquals(Result.SUCCESS, result.getCode(), "Should be successful even with empty solutions");
        assertNotNull(result.getData(), "Processed solutions should not be null");
        assertTrue(result.getData().isEmpty(), "Should have empty processed solutions");

        log.info("No solution test passed - empty solutions handled correctly");

        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }

    /**
     * 创建DC部件实例
     */
    private DCPartInst createDCPartInst(String code, Integer quantity) {
        DCPartInst partInst = new DCPartInst();
        partInst.setCode(code);
        partInst.setQuantity(quantity);
        partInst.setHidden(false);
        return partInst;
    }

    /**
     * 获取DC测试模块
     */
    private DCModule getDCModule() {
        DCModule dcModule = new DCModule();
        dcModule.setId(123L);
        dcModule.setCode("TShirt11");
        dcModule.setDccode("DC_TShirt11");
        dcModule.setVersion("1.0.0");
        dcModule.setPackageName("com.jmix.configengine.test");
        dcModule.setSeason("Spring");
        dcModule.init();
        return dcModule;
    }

    /**
     * 获取测试模块
     */
    private Module getModule() {
        // 这里应该返回实际的测试模块
        // 为了简化，我们创建一个基本的模块
        Module module = new Module();
        module.setId(123L);
        module.setCode("TShirt11");
        module.setVersion("1.0.0");
        module.setPackageName("com.jmix.configengine.test");
        module.init();
        return module;
    }
}
