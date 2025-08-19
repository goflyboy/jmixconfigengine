package com.jmix.configengine.executor;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaOptionVar;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.artifact.Var;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.ModuleAlgArtifact;
import lombok.extern.slf4j.Slf4j;
import com.google.ortools.sat.*;

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
			if (req.enumerateAllSolution) {
				solver.getParameters().setEnumerateAllSolutions(true);
			}
			// 可按需设置更多参数
			// if (config != null) { solver.getParameters().setLogSearchProgress(true); }
			ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(alg.getVars());
			solver.solve(model, cb);
			return Result.success(cb.getAllSolutions());
		} catch (Exception ex) {
			log.error("Failed to infer paras", ex);
			return Result.failed("exception: " + ex.getMessage());
		}
	}

	static class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {
		private final List<Var<?>> vars;
		private final List<ModuleInst> allSolutions = new ArrayList<>();

		public ModuleInstSolutionCallBack(List<Var<?>> vars) {
			this.vars = vars != null ? vars : Collections.emptyList();
		}

		@Override
		public void onSolutionCallback() {
			ModuleInst moduleInst = new ModuleInst();
			List<ParaInst> paraInsts = new ArrayList<>();
			List<PartInst> partInsts = new ArrayList<>();
			for (Var<?> v : vars) {
				if (v instanceof ParaVar) {
					ParaVar pv = (ParaVar) v;
					ParaInst pi = new ParaInst();
					pi.code = pv.getCode();
					// value: read IntVar domain value
					int value = (int) value((IntVar) pv.var);
					pi.value = String.valueOf(value);
					// options: selected option codes
					List<String> options = new ArrayList<>();
					pv.optionSelectVars.forEach((codeId, optionVar) -> {
						long sel = value(optionVar.getIsSelectedVar());
						if (sel == 1L) {
							options.add(optionVar.getCode());
						}
					});
					pi.options = options;
					paraInsts.add(pi);
				} else if (v instanceof PartVar) {
					PartVar partVar = (PartVar) v;
					PartInst inst = new PartInst();
					inst.code = partVar.getCode();
					inst.quantity = (int) value((IntVar) partVar.var);
					partInsts.add(inst);
				}
			}
			moduleInst.paras = paraInsts;
			moduleInst.parts = partInsts;
			allSolutions.add(moduleInst);
		}

		public List<ModuleInst> getAllSolutions() {
			return allSolutions;
		}
	}
} 