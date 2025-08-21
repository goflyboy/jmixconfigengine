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
	public IntVar var;
	
	/**
	 * 显示隐藏属性
	 */
	public BoolVar isHiddenVar;
	
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
		if (var != null) {
			sb.append(",var=").append(solutionCallback.value((IntVar) this.var));
		 
		}
		if (isHiddenVar != null) {
			sb.append(", hidden=").append(solutionCallback.booleanValue(this.isHiddenVar));
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