package com.jmix.configengine.executor;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.artifact.Var;
import com.jmix.configengine.model.Module;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;
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
			m.init();
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
			if (req.mainPartInst != null) {
				alg.addPartEquality(req.mainPartInst.code, req.mainPartInst.quantity);
			}
			if (req.preParaInsts != null) {
				for (ParaInst paraInst : req.preParaInsts) {
					alg.addParaEquality(paraInst.code, paraInst.value);
				}
			}
			if (req.prePartInsts != null) {
				for (PartInst partInst : req.prePartInsts) {
					alg.addPartEquality(partInst.code, partInst.quantity);
				}
			}
			if(config.isLogModelProto){
				//将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
				model.exportToFile(config.logFilePath + "/" + module.getCode() + ".proto.txt");
			}
			CpSolver solver = new CpSolver();
			if (req.enumerateAllSolution) {
				solver.getParameters().setEnumerateAllSolutions(true);
			}
			// 可按需设置更多参数
			// if (config != null) { solver.getParameters().setLogSearchProgress(true); }
			ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg.getVars());
			CpSolverStatus status = solver.solve(model, cb);
			
			// 如果模型无效，调用ValidateCpModel获取详细错误信息
			if(status == CpSolverStatus.MODEL_INVALID) {
				String validationError = model.validate();
				log.error("Model validation failed: {}", validationError);
				return Result.failed("Model validation failed: " + validationError);
			}
			
			if(status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE && status != CpSolverStatus.INFEASIBLE){
				return Result.failed("solver status: " + status);
			}
			// if(cb.getAllSolutions().isEmpty()){
			// 	return Result.noSolution();
			// }
			return Result.success(cb.getAllSolutions());
		} catch (Exception ex) {
			log.error("Failed to infer paras", ex);
			return Result.failed("exception: " + ex.getMessage());
		}
	}

	static class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {
		private final Module module;
		private final List<Var<?>> vars;
		private final List<ModuleInst> allSolutions = new ArrayList<>();
		//第几个解
		private int solutionIndex = 0;
		public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars) {
			this.vars = vars != null ? vars : Collections.emptyList();
			this.module = module;
		}

		@Override
		public void onSolutionCallback() {
			solutionIndex++;
			// 创建ModuleInst实例，instanceId从0开始
			ModuleInst moduleInst = createModuleInst(module, 0);
			for (Var<?> v : vars) {
				//打印var.getVarString
				log.info("-------------varInfos-solutionIndex:{}----------- \n {}"
				, solutionIndex, v.getVarString(this));
				//如果不是debug模式，则不打印Var的值
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
					// visibilityMode: read visibilityModeVar value
					if (pv.visibilityModeVar != null) {
						pi.visibilityMode = (int) value(pv.visibilityModeVar);
					}
					moduleInst.addParaInst(pi);
				} else if (v instanceof PartVar) {
					PartVar partVar = (PartVar) v;
					PartInst inst = new PartInst();
					inst.code = partVar.getCode();
					inst.quantity = (int) value((IntVar) partVar.var);
					// visibilityMode: read visibilityModeVar value
					if (partVar.visibilityModeVar != null) {
						inst.visibilityMode = (int) value(partVar.visibilityModeVar);
					}
					moduleInst.addPartInst(inst);
				}
			}
			allSolutions.add(moduleInst);
		}

		public List<ModuleInst> getAllSolutions() {
			return allSolutions;
		}
	}
	/**
	 * 创建ModuleInst实例
	 * @param module 模块对象
	 * @param instanceId 实例ID，默认为0
	 * @return ModuleInst实例
	 */
	private static ModuleInst createModuleInst(Module module, int instanceId) {
		ModuleInst moduleInst = new ModuleInst();
		moduleInst.id = module.getId();
		moduleInst.code = module.getCode();
		moduleInst.instanceConfigId = "INST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		moduleInst.instanceId = instanceId;
		moduleInst.quantity = 1;
		moduleInst.paras = new ArrayList<>();
		moduleInst.parts = new ArrayList<>();
		return moduleInst;
	}
	
	/**
	 * 将ModuleInst对象转换为JSON字符串
	 * @param inst ModuleInst实例
	 * @return JSON字符串
	 */
	public static String toJson(ModuleInst inst) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(inst);
		} catch (Exception e) {
			return "{\"error\": \"序列化失败: " + e.getMessage() + "\"}";
		}
	}
} 