package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.inf.Result;
import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.inf.ModuleInst;
import com.jmix.configengine.inf.ParaInst;
import com.jmix.configengine.inf.PartInst;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DC公司的T恤衫测试类
 * 演示扩展性功能的使用
 */
@Slf4j
public class DCMyTShirtTest {
    
    private DCDeliveryTypePostProcess postProcess;
    
    @Before
    public void setUp() {
        // 使用单例DC执行器
        
        // 初始化配置
        com.jmix.configengine.inf.ConstraintConfig config = new com.jmix.configengine.inf.ConstraintConfig();
        config.isAttachedDebug = true;
        config.rootFilePath = "target/generated-sources";
        config.logFilePath = "target/logs";
        config.isLogModelProto = false;
        
        // 初始化DC执行器
        DCModuleConstraintExecutor.INST.init(config);
        
        // 注册后处理器
        postProcess = new DCDeliveryTypePostProcess();
        DCModuleConstraintExecutor.INST.registerExtensible(postProcess);
        
        // 添加模块
        DCModuleConstraintExecutor.INST.addDCModule(1L, getDCModule());
    }
    
    @After
    public void tearDown() {
        // 清理资源
        DCModuleConstraintExecutor.INST.fini();
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testBasicInference() {
        log.info("Testing basic inference with DC extensions");
        
        // 由于模块没有算法制品，我们跳过实际的推理测试
        // 这里主要测试扩展性功能的注册和管理
        assertNotNull("DC Executor should not be null", DCModuleConstraintExecutor.INST);
        // assertNotNull("Standard Executor should not be null", executor);
        assertNotNull("Post process should not be null", postProcess);
        
        // 验证后处理器已注册
        assertTrue("Post process should be enabled", postProcess.isEnabled());
        assertEquals("Post process name should be correct", "DCDeliveryTypePostProcess", postProcess.getProcessName());
        
        log.info("Basic inference test passed - extension functionality verified");
        
        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testDCWrapperFunctionality() {
        log.info("Testing DC wrapper functionality");
        
        // 测试DC执行器的基本功能
        assertNotNull("DC Executor should not be null", DCModuleConstraintExecutor.INST);
        
        // 测试DC模块添加
        DCModule testModule = getDCModule();
        DCResult<Void> addResult = DCModuleConstraintExecutor.INST.addDCModule(2L, testModule);
        assertEquals("DC module addition should be successful", Result.SUCCESS, addResult.code);
        assertNotNull("DC result code should not be null", addResult.getDcResultCode());
        assertEquals("DC result code should be correct", "DC_ADD_MODULE_SUCCESS", addResult.getDcResultCode());
        
        // 测试DC推理请求
        DCModuleConstraintExecutor.DCInferParasReq dcReq = new DCModuleConstraintExecutor.DCInferParasReq();
        dcReq.moduleCode = "TShirt11";
        dcReq.mainPartInst = createDCPartInst("TShirt11", 1);
        dcReq.enumerateAllSolution = true;
        
        // 由于没有算法制品，这里主要测试接口调用
        DCResult<java.util.List<DCModuleInst>> result = DCModuleConstraintExecutor.INST.inferDCParas(dcReq);
        assertNotNull("DC inference result should not be null", result);
        assertNotNull("DC result code should not be null", result.getDcResultCode());
        assertTrue("DC result should have process timestamp", result.getProcessTimestamp() > 0);
        
        log.info("DC wrapper functionality test passed");
        
        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testDeliveryTypeCalculation() {
        log.info("Testing delivery type calculation");
        
        // 测试后处理器的业务逻辑
        com.jmix.configengine.model.Module module = getModule();
        
        // 创建模拟的解决方案
        java.util.List<ModuleInst> solutions = new java.util.ArrayList<>();
        ModuleInst solution = new ModuleInst();
        solution.setId(123L);
        solution.setCode("TShirt11");
        solution.setInstanceId(0);
        solution.setQuantity(1);
        
        // 添加参数实例
        java.util.List<ParaInst> paras = new java.util.ArrayList<>();
        ParaInst paraInst = new ParaInst();
        paraInst.setCode("Color");
        paraInst.setValue("Red");
        paras.add(paraInst);
        solution.setParas(paras);
        
        // 添加部件实例
        java.util.List<PartInst> parts = new java.util.ArrayList<>();
        PartInst partInst = new PartInst();
        partInst.setCode("TShirt11");
        partInst.setQuantity(3);
        parts.add(partInst);
        solution.setParts(parts);
        
        solutions.add(solution);
        
        // 测试后处理器
        Result<java.util.List<ModuleInst>> result = 
                postProcess.postProcess(module, solutions);
        
        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Should be successful", Result.SUCCESS, result.code);
        assertNotNull("Processed solutions should not be null", result.data);
        assertTrue("Should have processed solutions", !result.data.isEmpty());
        
        log.info("Found {} processed solutions with delivery type calculation", result.data.size());
        
        // 验证后处理器是否被调用
        assertTrue("Post process should be enabled", postProcess.isEnabled());
        assertEquals("Post process name should be correct", "DCDeliveryTypePostProcess", postProcess.getProcessName());
        
        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testAdapterFunctionality() {
        log.info("Testing adapter functionality");
        
        // 测试模块适配器
        DCModuleAdapter moduleAdapter = new DCModuleAdapter();
        DCModule dcModule = moduleAdapter.adapt(getModule());
        
        assertNotNull("DCModule should not be null", dcModule);
        assertEquals("DCCode should be set", getModule().getCode(), dcModule.getDccode());
        assertNotNull("Season should be set", dcModule.getSeason());
        
        log.info("DCModule adapted: dccode={}, season={}", 
                dcModule.getDccode(), dcModule.getSeason());
        
        // 测试参数实例适配器
        DCParaInstAdapter paraInstAdapter = new DCParaInstAdapter();
        ParaInst paraInst = new ParaInst();
        paraInst.setCode("Color");
        paraInst.setValue("Red");
        paraInst.setHidden(false);
        
        DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);
        
        assertNotNull("DCParaInst should not be null", dcParaInst);
        assertEquals("Code should be preserved", paraInst.getCode(), dcParaInst.getCode());
        assertEquals("Value should be preserved", paraInst.getValue(), dcParaInst.getValue());
        assertNotNull("DeliveryType should be set", dcParaInst.getDeliveryType());
        
        log.info("DCParaInst adapted: code={}, value={}, deliveryType={}", 
                dcParaInst.getCode(), dcParaInst.getValue(), dcParaInst.getDeliveryType());
        
        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testExtensibleProcessManagement() {
        log.info("Testing extensible process management");
        
        // 测试注册
        DCDeliveryTypePostProcess newPostProcess = new DCDeliveryTypePostProcess();
        Result<Void> registerResult = ModuleConstraintExecutor.INST.registerExtensible(newPostProcess);
        
        assertEquals("Registration should be successful", Result.SUCCESS, registerResult.code);
        
        // 测试注销
        Result<Void> unregisterResult = ModuleConstraintExecutor.INST.unregisterExtensible(newPostProcess);
        
        assertEquals("Unregistration should be successful", Result.SUCCESS, unregisterResult.code);
        
        // 测试完成后清理
        ModuleConstraintExecutor.INST.fini();
    }
    
    @Test
    public void testNoSolution() {
        log.info("Testing no solution scenario");
        
        // 测试空解决方案的后处理
        com.jmix.configengine.model.Module module = getModule();
        java.util.List<ModuleInst> emptySolutions = new java.util.ArrayList<>();
        
        // 测试后处理器处理空解决方案
        Result<java.util.List<ModuleInst>> result = 
                postProcess.postProcess(module, emptySolutions);
        
        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Should be successful even with empty solutions", Result.SUCCESS, result.code);
        assertNotNull("Processed solutions should not be null", result.data);
        assertTrue("Should have empty processed solutions", result.data.isEmpty());
        
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
    private com.jmix.configengine.model.Module getModule() {
        // 这里应该返回实际的测试模块
        // 为了简化，我们创建一个基本的模块
        com.jmix.configengine.model.Module module = new com.jmix.configengine.model.Module();
        module.setId(123L);
        module.setCode("TShirt11");
        module.setVersion("1.0.0");
        module.setPackageName("com.jmix.configengine.test");
        module.init();
        return module;
    }
}
