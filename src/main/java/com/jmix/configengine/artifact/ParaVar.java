package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;
import com.jmix.configengine.model.Para;

/**
 * 参数变量
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaVar extends Var<Para> {
	/**
	 * 参数值状态
	 */
	public IntVar value;
	
	/**
	 * 兼容旧模型的隐藏布尔变量
	 * 0-显示，1-隐藏
	 */
	public BoolVar isHidden;
	 
	/**
	 * 参数可选值的选中状态(CodeId -> ParaOptionVar)
	 */
	public Map<Integer, ParaOptionVar> optionSelectVars = new HashMap<>();

	public ParaOptionVar getParaOptionByCodeId(Integer codeId) {
		return optionSelectVars.get(codeId);
	}

	public ParaOptionVar getParaOptionByCode(String code) {
		for (ParaOptionVar option : optionSelectVars.values()) {
			if (code != null && code.equals(option.getCode())) {
				return option;
			}
		}
		throw new RuntimeException("ParaOptionVar not found for code: " + code);
	}
	public List<ParaOptionVar> getParaOptionByCodes(List<String> codes) {
		return codes.stream()
				.map(this::getParaOptionByCode)
				.collect(Collectors.toList());
	}
	public List<ParaOptionVar> getParaOptionByExcusiveCodes(List<String> exclusiveCodes) {
		return optionSelectVars.values().stream()
				.filter(option -> !exclusiveCodes.contains(option.getCode()))
				.collect(Collectors.toList());
	}
	
	@Override
	public String getVarString(CpSolverSolutionCallback solutionCallback) {
		StringBuilder sb = new StringBuilder();
		sb.append("Para{code=").append(getCode());
		if (value != null) {
			sb.append(",value=").append(solutionCallback.value((IntVar) this.value));
		 
		}
		if (isHidden != null) {
			sb.append(", isHidden=").append(solutionCallback.value(this.isHidden));
		}
		if (!optionSelectVars.isEmpty()) {
			//打印每个option的getVarString
			sb.append(", options=");
			for (ParaOptionVar option : optionSelectVars.values()) {
				sb.append(option.getVarString(solutionCallback)).append(",");
			}
		}
		sb.append("}");
		return sb.toString();
	}
} 