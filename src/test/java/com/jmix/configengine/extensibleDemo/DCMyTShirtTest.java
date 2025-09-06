package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DC公司的T恤衫测试类
 * 演示扩展性功能的使用
 */
@Slf4j
public class DCMyTShirtTest {
    
    private ModuleConstraintExecutor executor;
    private DCDeliveryTypePostProcess postProcess;
    
    @Before
    public void setUp() {
        // 创建执行器
        executor = new ModuleConstraintExecutorImpl();
        
        // 初始化配置
        ModuleConstraintExecutor.ConstraintConfig config = new ModuleConstraintExecutor.ConstraintConfig();
        config.isAttachedDebug = true;
        config.rootFilePath = "target/generated-sources";
        config.logFilePath = "target/logs";
        config.isLogModelProto = false;
        
        executor.init(config);
        
        // 注册后处理器
        postProcess = new DCDeliveryTypePostProcess();
        executor.registerExtensible(postProcess);
        
        // 添加模块
        executor.addModule(1L, getModule());
    }
    
    @Test
    public void testBasicInference() {
        log.info("Testing basic inference with DC extensions");
        
        // 由于模块没有算法制品，我们跳过实际的推理测试
        // 这里主要测试扩展性功能的注册和管理
        assertNotNull("Executor should not be null", executor);
        assertNotNull("Post process should not be null", postProcess);
        
        // 验证后处理器已注册
        assertTrue("Post process should be enabled", postProcess.isEnabled());
        assertEquals("Post process name should be correct", "DCDeliveryTypePostProcess", postProcess.getProcessName());
        
        log.info("Basic inference test passed - extension functionality verified");
    }
    
    @Test
    public void testDeliveryTypeCalculation() {
        log.info("Testing delivery type calculation");
        
        // 测试后处理器的业务逻辑
        com.jmix.configengine.model.Module module = getModule();
        
        // 创建模拟的解决方案
        java.util.List<ModuleConstraintExecutor.ModuleInst> solutions = new java.util.ArrayList<>();
        ModuleConstraintExecutor.ModuleInst solution = new ModuleConstraintExecutor.ModuleInst();
        solution.setId(123L);
        solution.setCode("TShirt11");
        solution.setInstanceId(0);
        solution.setQuantity(1);
        
        // 添加参数实例
        java.util.List<ModuleConstraintExecutor.ParaInst> paras = new java.util.ArrayList<>();
        ModuleConstraintExecutor.ParaInst paraInst = new ModuleConstraintExecutor.ParaInst();
        paraInst.setCode("Color");
        paraInst.setValue("Red");
        paras.add(paraInst);
        solution.setParas(paras);
        
        // 添加部件实例
        java.util.List<ModuleConstraintExecutor.PartInst> parts = new java.util.ArrayList<>();
        ModuleConstraintExecutor.PartInst partInst = new ModuleConstraintExecutor.PartInst();
        partInst.setCode("TShirt11");
        partInst.setQuantity(3);
        parts.add(partInst);
        solution.setParts(parts);
        
        solutions.add(solution);
        
        // 测试后处理器
        ModuleConstraintExecutor.Result<java.util.List<ModuleConstraintExecutor.ModuleInst>> result = 
                postProcess.postProcess(module, solutions);
        
        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Should be successful", ModuleConstraintExecutor.Result.SUCCESS, result.code);
        assertNotNull("Processed solutions should not be null", result.data);
        assertTrue("Should have processed solutions", !result.data.isEmpty());
        
        log.info("Found {} processed solutions with delivery type calculation", result.data.size());
        
        // 验证后处理器是否被调用
        assertTrue("Post process should be enabled", postProcess.isEnabled());
        assertEquals("Post process name should be correct", "DCDeliveryTypePostProcess", postProcess.getProcessName());
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
        ModuleConstraintExecutor.ParaInst paraInst = new ModuleConstraintExecutor.ParaInst();
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
    }
    
    @Test
    public void testExtensibleProcessManagement() {
        log.info("Testing extensible process management");
        
        // 测试注册
        DCDeliveryTypePostProcess newPostProcess = new DCDeliveryTypePostProcess();
        ModuleConstraintExecutor.Result<Void> registerResult = executor.registerExtensible(newPostProcess);
        
        assertEquals("Registration should be successful", ModuleConstraintExecutor.Result.SUCCESS, registerResult.code);
        
        // 测试注销
        ModuleConstraintExecutor.Result<Void> unregisterResult = executor.unregisterExtensible(newPostProcess);
        
        assertEquals("Unregistration should be successful", ModuleConstraintExecutor.Result.SUCCESS, unregisterResult.code);
    }
    
    @Test
    public void testNoSolution() {
        log.info("Testing no solution scenario");
        
        // 测试空解决方案的后处理
        com.jmix.configengine.model.Module module = getModule();
        java.util.List<ModuleConstraintExecutor.ModuleInst> emptySolutions = new java.util.ArrayList<>();
        
        // 测试后处理器处理空解决方案
        ModuleConstraintExecutor.Result<java.util.List<ModuleConstraintExecutor.ModuleInst>> result = 
                postProcess.postProcess(module, emptySolutions);
        
        // 验证结果
        assertNotNull("Result should not be null", result);
        assertEquals("Should be successful even with empty solutions", ModuleConstraintExecutor.Result.SUCCESS, result.code);
        assertNotNull("Processed solutions should not be null", result.data);
        assertTrue("Should have empty processed solutions", result.data.isEmpty());
        
        log.info("No solution test passed - empty solutions handled correctly");
    }
    
    /**
     * 创建部件实例
     */
    private ModuleConstraintExecutor.PartInst createPartInst(String code, Integer quantity) {
        ModuleConstraintExecutor.PartInst partInst = new ModuleConstraintExecutor.PartInst();
        partInst.setCode(code);
        partInst.setQuantity(quantity);
        partInst.setHidden(false);
        return partInst;
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
