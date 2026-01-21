package com.jmix.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.ModuleAlgArtifact;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
public class ExecutorSampleTest {
    @Test
    public void helloSample() throws IOException {
        // Hello的模型说明，详细见 cp-Hello-123-sources.jar
        String moduleCode = "Hello";
        long moduleId = 123L;
        // 参数1：color: "Red-1", "Black-2", "White-3"
        // 参数2："Small-10", "Medium-20", "Big-30"
        // 部件1：tShirt11
        // 算法（约束):
        // if(colorVar.value ==Red && sizeVar.value == Small ) {
        // tShirt11Var.qty = 1;
        // }
        // else {
        // tShirt11Var.qty = 3
        // }

        // 放算法的根路径：\src\test\resources\algroot
        String rootPath = CommHelper.getTestResourcePath() + File.separator + "algroot";
        String moduleDirPath = rootPath + File.separator
                + String.format(ModuleAlgArtifact.MODULE_DIR_PATTERN, moduleCode, moduleId);
        String moduleJsonFile = String.format(ModuleAlgArtifact.BASE_JSON_PATTERN, moduleCode, moduleId);

        // 初始化执行器
        ConstraintConfig config = new ConstraintConfig();
        config.setRootFilePath(rootPath);
        ModuleConstraintExecutor.INST.init(config);

        // 初始化Module数据
        Module module = ModuleUtils.fromJsonFile(moduleDirPath + File.separator + moduleJsonFile);
        ModuleConstraintExecutor.INST.addModule(moduleId, module);

        // 推理参数 inferParas("tShirt11", 1);
        InferParasReq req = new InferParasReq();
        req.setModuleId(moduleId);
        req.setMainPartInst(new PartInst("tShirt11", 1));
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);

        // 结果处理
        log.info("result: {}", result);
        ModuleInst solution = result.getData().get(0);
        ParaInst colorInst = solution.getParas().get(0);
        ParaInst sizeInst = solution.getParas().get(1);
        assertEquals(colorInst.getValue(), "1"); // "Red"
        assertEquals(sizeInst.getValue(), "10"); // "Small"
    }

}
