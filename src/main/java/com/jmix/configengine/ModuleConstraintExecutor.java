package com.jmix.configengine;

import com.jmix.configengine.model.Module;
import java.util.List;
import java.util.Map;

public interface ModuleConstraintExecutor {
	Result<Void> init(ConstraintConfig config);
	Result<Void> fini();
	Result<Void> addModule(Long rootModuleId, Module... modules);
	Result<Void> removeModule(Long moduleId);
	Result<List<ModuleInst>> inferParas(InferParasReq req);
	
	class Result<T> {
		public static final int SUCCESS = 0;
		public static final int FAILED = 1;
		public int code = SUCCESS;
		public T data;
		public String message;
		public static <X> Result<X> success(X data){
			Result<X> r = new Result<>();
			r.code = SUCCESS;
			r.data = data;
			return r;
		}
		public static <X> Result<X> failed(String msg){
			Result<X> r = new Result<>();
			r.code = FAILED;
			r.message = msg;
			return r;
		}
	}
	
	class ConstraintConfig {
		public boolean isAttachedDebug;
		public String rootFilePath;
	}
	
	class ModuleInst {
		public Long id;
		public String code;
		public String instanceConfigId;
		public int instanceId;
		public Integer quantity;
		public List<ParaInst> paras;
		public List<PartInst> parts;
	}
	class PartInst {
		public String code;
		public Integer quantity;
		public Map<String,String> selectAttrValue;
		public boolean isHidden = false;
	}
	class ParaInst {
		public String code;
		public String value;
		public List<String> options;
		public boolean isHidden = false;
	}
	class InferParasReq {
		public Long moduleId;
		public String moduleCode;
		public PartInst mainPartInst;
		public List<ParaInst> preParaInsts;
		public List<PartInst> prePartInsts;
		public boolean enumerateAllSolution;
	}
} 