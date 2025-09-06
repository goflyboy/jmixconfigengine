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
	
	/**
	 * 注册扩展处理器
	 * @param eProcess 扩展处理器
	 * @return 注册结果
	 */
	Result<Void> registerExtensible(ExtensibleProcess eProcess);
	
	/**
	 * 注销扩展处理器
	 * @param eProcess 扩展处理器
	 * @return 注销结果
	 */
	Result<Void> unregisterExtensible(ExtensibleProcess eProcess);
	
	class Result<T> {
		public static final int SUCCESS = 0;
		public static final int FAILED = 1;
		public static final int NO_SOLUTION = 2;
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
		public static <X> Result<X> noSolution(){
			Result<X> r = new Result<>();
			r.code = NO_SOLUTION;
			return r;
		}
	}
	
	class ConstraintConfig {
		public boolean isAttachedDebug;
		public String rootFilePath;
		public String logFilePath;
		public boolean isLogModelProto=false;//是否输出ModelProto信息，方便定位
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
		// @JsonProperty("isHidden")
		public boolean isHidden = false;
	}
	@Data
	class ParaInst {
		public String code;
		public String value;//空表示没有赋值
		public List<String> options;//空表示没有赋值
		// @JsonProperty("isHidden")
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