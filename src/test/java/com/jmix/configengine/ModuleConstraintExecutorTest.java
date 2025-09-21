package com.jmix.configengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jmix.executor.executor.ModuleConstraintExecutorImpl;
import com.jmix.executor.inf.ConstraintConfig;
import com.jmix.executor.inf.InferParasReq;
import com.jmix.executor.inf.ModuleInst;
import com.jmix.executor.inf.Result;
import com.jmix.executor.model.Module;
import com.jmix.executor.util.ModuleUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ModuleConstraintExecutorTest {
    @Test
    public void testInitAddModuleAndInfer2() {
    }

    // @Test
    public void testInitAddModuleAndInfer() {
        ModuleConstraintExecutorImpl exec = new ModuleConstraintExecutorImpl();
        ConstraintConfig cfg = new ConstraintConfig();
        cfg.setAttachedDebug(true); // 测试环境直接使用当前classpath加载
        cfg.setRootFilePath("");
        assertEquals(0, exec.init(cfg).code);

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
        assertEquals(0, exec.addModule(123123L, m).code);

        InferParasReq req = new InferParasReq();
        req.setModuleId(123123L);
        req.setEnumerateAllSolution(true);
        Result<List<ModuleInst>> r = exec.inferParas(req);
        int i = 0;
        r.data.forEach(inst -> {
            // 打印第一个解
            System.out.println("--------------------------------");
            System.out.println("解" + (i + 1) + ":");
            System.out.println(ModuleConstraintExecutorImpl.toJson(inst));
            // i++;
        });

        // 枚举所有解，应该至少返回1个
        assertEquals(0, r.code);
        assertNotNull(r.data);
        assertTrue(r.data.size() >= 1);
        // 解的结构应包含para和part
        ModuleInst first = r.data.get(0);
        assertNotNull(first.getParas());
        assertNotNull(first.getParts());
    }
}