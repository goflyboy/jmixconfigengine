package com.jmix.scenario.partcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;
import com.jmix.tool.impl.ModuleGenneratorByAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * 计算机配件约束测试
 *
 * @since 2025-12-27
 */
@Slf4j
public class ComputerConstraintTest {

    private String tempResourcePath;
    private Module module;

    /**
     * 测试前初始化
     */
    @BeforeEach
    public void setUp() {
        // 生成临时资源路径
        tempResourcePath = CommHelper.createTempPath(ComputerConstraint.class);

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
        module = ModuleGenneratorByAnno.build(ComputerConstraint.class, tempResourcePath);
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
     * 测试固态硬盘实例1的配置
     */
    @Test
    public void testSolidDrive1() {
        // 构建推理请求
        InferParasReq req = new InferParasReq();
        req.setModuleId(module.getId());
        req.setEnumerateAllSolution(true);

        // 设置主部件实例：solidDrive1数量为1
        PartInst mainPartInst = new PartInst();
        mainPartInst.setCode("solidDrive1");
        mainPartInst.setQuantity(1);
        req.setMainPartInst(mainPartInst);

        // 执行参数推理
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);

        // 验证结果
        assertEquals(Result.SUCCESS, result.getCode(),
                "Inference failed: " + result.getMessage());
        assertNotNull(result.getData(), "Result data should not be null");
        assertTrue(result.getData().size() > 0, "Should have at least one solution");

        log.info("Solid drive 1 configuration test passed, found {} solutions", result.getData().size());
    }

    // /**
    // * 测试固态硬盘实例2的配置
    // */
    // @Test
    // public void testSolidDrive2() {
    // // 构建推理请求
    // InferParasReq req = new InferParasReq();
    // req.setModuleId(module.getId());
    // req.setEnumerateAllSolution(true);

    // // 设置主部件实例：solidDrive2数量为1
    // PartInst mainPartInst = new PartInst();
    // mainPartInst.setCode("solidDrive2");
    // mainPartInst.setQuantity(1);
    // req.setMainPartInst(mainPartInst);

    // // 执行参数推理
    // Result<List<ModuleInst>> result =
    // ModuleConstraintExecutor.INST.inferParas(req);

    // // 验证结果
    // assertEquals(Result.SUCCESS, result.getCode(),
    // "Inference failed: " + result.getMessage());
    // assertNotNull(result.getData(), "Result data should not be null");
    // assertTrue(result.getData().size() > 0, "Should have at least one solution");

    // log.info("Solid drive 2 configuration test passed, found {} solutions",
    // result.getData().size());
    // }

    // /**
    // * 测试机械硬盘实例1的配置
    // */
    // @Test
    // public void testMechDrive1() {
    // // 构建推理请求
    // InferParasReq req = new InferParasReq();
    // req.setModuleId(module.getId());
    // req.setEnumerateAllSolution(true);

    // // 设置主部件实例：mechDrive1数量为1
    // PartInst mainPartInst = new PartInst();
    // mainPartInst.setCode("mechDrive1");
    // mainPartInst.setQuantity(1);
    // req.setMainPartInst(mainPartInst);

    // // 执行参数推理
    // Result<List<ModuleInst>> result =
    // ModuleConstraintExecutor.INST.inferParas(req);

    // // 验证结果
    // assertEquals(Result.SUCCESS, result.getCode(),
    // "Inference failed: " + result.getMessage());
    // assertNotNull(result.getData(), "Result data should not be null");
    // assertTrue(result.getData().size() > 0, "Should have at least one solution");

    // log.info("Mechanical drive 1 configuration test passed, found {} solutions",
    // result.getData().size());
    // }

    // /**
    // * 测试机械硬盘实例2的配置
    // */
    // @Test
    // public void testMechDrive2() {
    // // 构建推理请求
    // InferParasReq req = new InferParasReq();
    // req.setModuleId(module.getId());
    // req.setEnumerateAllSolution(true);

    // // 设置主部件实例：mechDrive2数量为1
    // PartInst mainPartInst = new PartInst();
    // mainPartInst.setCode("mechDrive2");
    // mainPartInst.setQuantity(1);
    // req.setMainPartInst(mainPartInst);

    // // 执行参数推理
    // Result<List<ModuleInst>> result =
    // ModuleConstraintExecutor.INST.inferParas(req);

    // // 验证结果
    // assertEquals(Result.SUCCESS, result.getCode(),
    // "Inference failed: " + result.getMessage());
    // assertNotNull(result.getData(), "Result data should not be null");
    // assertTrue(result.getData().size() > 0, "Should have at least one solution");

    // log.info("Mechanical drive 2 configuration test passed, found {} solutions",
    // result.getData().size());
    // }

    // /**
    // * 测试模块初始化
    // */
    // @Test
    // public void testModuleInitialization() {
    // // 验证模块是否正确初始化
    // assertNotNull(module, "Module should not be null");
    // assertNotNull(module.getParts(), "Module parts should not be null");
    // assertTrue(module.getParts().size() > 0, "Module should have parts");

    // // 打印所有部件信息，用于调试
    // log.info("Module parts: {}", module.getParts().size());
    // for (Part part : module.getParts()) {
    // log.info("Part: {}, dynAttrSchemas: {}", part.getCode(),
    // part.getDynAttrSchemas().size());
    // }

    // // 验证部件是否存在
    // boolean hasDrive = module.getParts().stream().anyMatch(p ->
    // "drive".equals(p.getCode()));
    // boolean hasSolidDrive = module.getParts().stream().anyMatch(p ->
    // "solidDrive".equals(p.getCode()));
    // boolean hasMechDrive = module.getParts().stream().anyMatch(p ->
    // "mechDrive".equals(p.getCode()));

    // assertTrue(hasDrive, "Drive part should exist");
    // assertTrue(hasSolidDrive, "Solid drive part should exist");
    // assertTrue(hasMechDrive, "Mechanical drive part should exist");

    // log.info("Module initialization test passed");
    // }
}
