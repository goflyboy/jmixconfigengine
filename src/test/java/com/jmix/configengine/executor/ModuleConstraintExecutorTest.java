package com.jmix.configengine.executor;

import com.jmix.configengine.model.*;
import com.jmix.configengine.artifact.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ModuleConstraintExecutorTest {
	@Test
	public void testInitAddModuleAndInfer() {
		ModuleConstraintExecutorImpl exec = new ModuleConstraintExecutorImpl();
		ModuleConstraintExecutor.ConstraintConfig cfg = new ModuleConstraintExecutor.ConstraintConfig();
		cfg.isAttachedDebug = true; // 测试环境直接使用当前classpath加载
		cfg.rootFilePath = "";
		assertEquals(0, exec.init(cfg).code);
		
		// 构造最小Module，代码为TShirt，对应生成类名 TShirtConstraint
		com.jmix.configengine.model.Module m = new com.jmix.configengine.model.Module();
		m.setId(1L);
		m.setCode("TShirt");
		m.setPackageName("com.jmix.configengine.scenario.tshirt");
		ModuleAlgArtifact alg = new ModuleAlgArtifact();
		alg.setPackageName("com.jmix.configengine.scenario.tshirt");
		alg.setFileName("TShirtConstraint.jar");
		m.setAlg(alg);
		m.init();
		
		assertEquals(0, exec.addModule(1L, m).code);
		
		ModuleConstraintExecutor.InferParasReq req = new ModuleConstraintExecutor.InferParasReq();
		req.moduleId = 1L;
		req.enumerateAllSolution = false;
		ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> r = exec.inferParas(req);
		
		// 在当前环境下，只要能走到求解并返回，不抛异常即认为打通
		assertEquals(0, r.code);
		assertNotNull(r.data);
	}
} 