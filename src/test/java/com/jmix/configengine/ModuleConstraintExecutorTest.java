package com.jmix.configengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.impl.ModuleConstraintExecutorImpl;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.Result;
import com.jmix.tool.ModuleUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ModuleConstraintExecutorTest {
    /**
     * 测试初始化、添加模块和推理参数（空测试）
     */
    @Test
    public void testInitAddModuleAndInfer2() {
    }

    // @Test
    /**
     * 测试初始化、添加模块和推理参数
     */
    public void testInitAddModuleAndInfer() {
        ModuleConstraintExecutorImpl exec = new ModuleConstraintExecutorImpl();
        ConstraintConfig cfg = new ConstraintConfig();
        cfg.setAttachedDebug(true); // 测试环境直接使用当前classpath加载
        cfg.setRootFilePath("");
        assertEquals(0, exec.init(cfg).getCode());

        // 构造最小Module，代码为TShirt，对应生成类名 TShirtConstraint
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json";
        // 使用ModuleUtils.fromJsonFile方法读取JSON文件
        Module m = null;
        try {
            m = ModuleUtils.fromJsonFile(jsonFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取JSON文件失败:" + e.getMessage());
            // e.printStackTrace();
            return;
        }

        // 初始化模块
        m.init();
        assertEquals(0, exec.addModule(123123L, m).getCode());

        InferParasReq req = new InferParasReq();
        req.setModuleId(123123L);
        req.setEnumerateAllSolution(true);
        Result<List<ModuleInst>> r = exec.inferParas(req);
        int i = 0;
        r.getData().forEach(inst -> {
            // 打印第一个解
            System.out.println("--------------------------------");
            System.out.println("解" + (i + 1) + ":");
            System.out.println(ModuleConstraintExecutorImpl.toJson(inst));
            // i++;
        });

        // 枚举所有解，应该至少返回1个
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
        assertTrue(r.getData().size() >= 1);
        // 解的结构应包含para和part
        ModuleInst first = r.getData().get(0);
        assertNotNull(first.getParas());
        assertNotNull(first.getParts());
    }
}