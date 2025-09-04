package com.jmix.configengine;
import com.jmix.configengine.util.ModuleUtils;
import com.jmix.configengine.model.ModuleAlgArtifact;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

public class ModuleConstraintExecutorTest {
	@Test
	public void testInitAddModuleAndInfer2()  {
	}
	// @Test TODO
	public void testInitAddModuleAndInfer()  {
		ModuleConstraintExecutorImpl exec = new ModuleConstraintExecutorImpl();
		ModuleConstraintExecutor.ConstraintConfig cfg = new ModuleConstraintExecutor.ConstraintConfig();
		cfg.isAttachedDebug = true; // 测试环境直接使用当前classpath加载
		cfg.rootFilePath = "";
		assertEquals(0, exec.init(cfg).code);
		
		// 构造最小Module，代码为TShirt，对应生成类名 TShirtConstraint
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json"; 
        // 使用ModuleUtils.fromJsonFile方法读取JSON文件
        com.jmix.configengine.model.Module m = null;
		try {
			m = ModuleUtils.fromJsonFile(jsonFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("读取JSON文件失败:"+e.getMessage());
			// e.printStackTrace();
			return;
		}
        
        // 初始化模块
        m.init();
		assertEquals(0, exec.addModule(123123L, m).code);
		
		ModuleConstraintExecutor.InferParasReq req = new ModuleConstraintExecutor.InferParasReq();
		req.moduleId = 123123L;
		req.enumerateAllSolution = true;
		ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> r 
		= exec.inferParas(req);
		int i = 0;
		r.data.forEach(inst -> {
			//打印第一个解
			System.out.println("--------------------------------"); 
			System.out.println("解" + (i+1) + ":");
			System.out.println(ModuleConstraintExecutorImpl.toJson(inst));
			// i++;
		});
		
		// 枚举所有解，应该至少返回1个
		assertEquals(0, r.code);
		assertNotNull(r.data);
		assertTrue(r.data.size() >= 1);
		// 解的结构应包含para和part
		ModuleConstraintExecutor.ModuleInst first = r.data.get(0);
		assertNotNull(first.paras);
		assertNotNull(first.parts);
	}
} 