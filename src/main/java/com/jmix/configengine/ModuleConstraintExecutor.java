package com.jmix.configengine;

import com.jmix.configengine.model.Module;

import lombok.Data;

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
	@Data
	class ModuleInst {
		public Long id;//module id
		public String code;//module code
		public String instanceConfigId;//instance config id
		public int instanceId;//instance id 默认为0，多个实例，从0开始，如：0，1,2，....
		public Integer quantity;//quantity
		public List<ParaInst> paras;//paras
		public List<PartInst> parts;//parts
		public void addParaInst(ParaInst paraInst){
			paras.add(paraInst);
		}
		public void addPartInst(PartInst partInst){
			parts.add(partInst);
		}
	}
	@Data
	class PartInst {
		public String code;
		public Integer quantity;
		public Map<String,String> selectAttrValue;
		public boolean isHidden = false;
	}
	@Data
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