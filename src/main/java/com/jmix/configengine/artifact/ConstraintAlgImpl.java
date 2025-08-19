package com.jmix.configengine.artifact;
 
import com.jmix.configengine.artifact.*; 
import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.google.ortools.Loader;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Part;
import java.util.*;
/**
 * 约束算法实现基类
 */
public abstract class ConstraintAlgImpl {
	static {
		Loader.loadNativeLibraries();
	}
	// CP模型
	protected CpModel model; 
	// module context
	protected Module module;
	// var map
	protected Map<String, Var<?>> varMap = new HashMap<>();

	public void initModel(CpModel model, Module module){
		this.model = model;
		this.module = module;
		initModelAfter(model);
		initVariables();
		initConstraint();
	}
	public void initModel(CpModel model){
		this.model = model;
		initModelAfter(model);
		initVariables();
		initConstraint();
	}
	protected void initModelAfter(CpModel model){
		
	}
	protected abstract void initVariables();
	protected abstract void initConstraint();

	public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
		return model.newIntVarFromDomain(Domain.fromValues(values), name);
	}

	public Map<String, Var<?>> getVarMap() {
		return varMap;
	}

	public void registerVar(String code, Var<?> var) {
		if (code != null && var != null) {
			varMap.put(code, var);
		}
	}

	protected ParaVar createParaVar(String code) {
		Para para = module != null ? module.getPara(code) : null;
		if (para == null) {
			throw new RuntimeException("Para not found for code: " + code);
		}
		ParaVar paraVar = new ParaVar();
		paraVar.setBase(para);
		paraVar.var = newIntVarFromDomain(model, para.getOptionIds(), code);
		if (para.getOptions() != null) {
			for (ParaOption option : para.getOptions()) {
				ParaOptionVar optionVar = createParaOptionVar(para.getCode(), option.getCode());
				paraVar.optionSelectVars.put(option.getCodeId(), optionVar);
			}
		}
		paraVar.optionSelectVars.forEach((optionId, optionVar) -> {
			model.addEquality((IntVar) paraVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
			model.addDifferent((IntVar) paraVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
		});
		registerVar(code, paraVar);
		return paraVar;
	}

	protected PartVar createPartVar(String code) {
		Part part = module != null ? module.getPart(code) : null;
		if (part == null) {
			throw new RuntimeException("Part not found for code: " + code);
		}
		PartVar partVar = new PartVar();
		partVar.setBase(part);
		partVar.var = model.newIntVar(0, 1000, code);
		registerVar(code, partVar);
		return partVar;
	}

	protected ParaOptionVar createParaOptionVar(String paraCode, String optionCode) {
		Para para = module != null ? module.getPara(paraCode) : null;
		if (para == null) {
			throw new RuntimeException("Para not found for code: " + paraCode);
		}
		ParaOption option = para.getOption(optionCode);
		if (option == null) {
			throw new RuntimeException("ParaOption not found for code: " + optionCode);
		}
		ParaOptionVar optionVar = new ParaOptionVar(option);
		optionVar.isSelectedVar = model.newBoolVar(paraCode + "_" + option.getCodeId());
		return optionVar;
	}
} 