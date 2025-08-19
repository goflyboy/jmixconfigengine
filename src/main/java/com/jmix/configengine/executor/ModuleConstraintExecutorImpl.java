package com.jmix.configengine.executor;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.ModuleAlgArtifact;
import lombok.extern.slf4j.Slf4j;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;

import java.util.*;

@Slf4j
public class ModuleConstraintExecutorImpl implements ModuleConstraintExecutor {
	private final Map<Long, Module> moduleMap = new HashMap<>();
	private final Map<Long, ModuleAlgClassLoader> moduleAlgClassLoaderMap = new HashMap<>();
	private ConstraintConfig config;
	
	@Override
	public Result<Void> init(ConstraintConfig config) {
		this.config = config;
		log.info("Module constraint executor initialized");
		return Result.success(null);
	}
	
	@Override
	public Result<Void> fini() {
		moduleAlgClassLoaderMap.clear();
		moduleMap.clear();
		log.info("Module constraint executor finalized");
		return Result.success(null);
	}
	
	@Override
	public Result<Void> addModule(Long rootModuleId, Module... modules) {
		if (modules == null) {
			return Result.failed("modules is null");
		}
		for (Module m : modules) {
			if (m == null) continue;
			moduleMap.put(m.getId(), m);
			ModuleAlgClassLoader loader = new ModuleAlgClassLoader(config != null && config.isAttachedDebug, config != null ? config.rootFilePath : null);
			loader.init(m.getCode(), m.getAlg());
			moduleAlgClassLoaderMap.put(m.getId(), loader);
		}
		log.info("Added modules: {}", modules.length);
		return Result.success(null);
	}
	
	@Override
	public Result<Void> removeModule(Long moduleId) {
		moduleAlgClassLoaderMap.remove(moduleId);
		moduleMap.remove(moduleId);
		log.info("Removed module: {}", moduleId);
		return Result.success(null);
	}
	
	@Override
	public Result<List<ModuleInst>> inferParas(InferParasReq req) {
		if (req == null) return Result.failed("req is null");
		Module module = null;
		if (req.moduleId != null) {
			module = moduleMap.get(req.moduleId);
		} else if (req.moduleCode != null) {
			for (Module m : moduleMap.values()) {
				if (req.moduleCode.equals(m.getCode())) { module = m; break; }
			}
		}
		if (module == null) return Result.failed("module not found");
		ModuleAlgClassLoader loader = moduleAlgClassLoaderMap.get(module.getId());
		if (loader == null) return Result.failed("loader not found");
		
		try {
			ConstraintAlgImpl alg = loader.newConstraintAlg(module.getCode());
			CpModel model = new CpModel();
			module.init();
			alg.initModel(model, module);
			CpSolver solver = new CpSolver();
			solver.solve(model);
			return Result.success(Collections.emptyList());
		} catch (Exception ex) {
			log.error("Failed to infer paras", ex);
			return Result.failed("exception: " + ex.getMessage());
		}
	}
} 