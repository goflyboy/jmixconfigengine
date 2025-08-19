package com.jmix.configengine.executor;

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
			ModuleAlgClassLoader loader = new ModuleAlgClassLoader(config);
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
			alg.initModel(model);
			CpSolver solver = new CpSolver();
			// 可根据config设置参数
			// solver.getParameters().setLogSearchProgress(true);
			solver.solve(model);
			// Demo：返回空列表作为占位，后续可解析变量填充
			return Result.success(Collections.emptyList());
		} catch (Exception ex) {
			log.error("Failed to infer paras", ex);
			return Result.failed("exception: " + ex.getMessage());
		}
	}
	
	public static class ModuleAlgClassLoader extends ClassLoader {
		private final ConstraintConfig config;
		private boolean isAttachedDebug;
		private String constraintRuleClassName;
		private final Map<String, Class<?>> classMap = new HashMap<>();
		private ModuleAlgArtifact algArtifact;
		
		public ModuleAlgClassLoader(ConstraintConfig config) {
			this.config = config;
		}
		
		public void init(String moduleCode, ModuleAlgArtifact algArtifact) {
			this.algArtifact = algArtifact;
			this.isAttachedDebug = (config != null && config.isAttachedDebug);
			if (algArtifact != null) {
				this.constraintRuleClassName = algArtifact.getPackageName() + "." + moduleCode + "Constraint";
			}
		}
		
		public ConstraintAlgImpl newConstraintAlg(String moduleCode) throws Exception {
			String className = this.constraintRuleClassName;
			Class<?> clazz = classMap.get(className);
			if (clazz == null) {
				clazz = loadClass(className);
				classMap.put(className, clazz);
			}
			return (ConstraintAlgImpl) clazz.getDeclaredConstructor().newInstance();
		}
		
		@Override
		public Class<?> loadClass(String name) {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Class not found: " + name, e);
			}
		}
	}
} 