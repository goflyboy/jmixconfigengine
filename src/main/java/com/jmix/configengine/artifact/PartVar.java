package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;
import com.jmix.configengine.model.Part;

/**
 * 部件变量
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartVar extends Var<Part> {
	/**
	 * 部件的数量值
	 */
	public IntVar var;
	
	/**
	 * 显示隐藏属性
	 */
	public BoolVar isHiddenVar;
	
	/**
	 * 子部件选中状态(Part.code -> BoolVar)
	 */
	public Map<String, BoolVar> subPartSelectedVars = new HashMap<>();
	
	@Override
	public String getVarString(CpSolverSolutionCallback solutionCallback) {
		StringBuilder sb = new StringBuilder();
		sb.append("PartVar{code=").append(getCode());
		if (var != null) {
			sb.append(", var=").append(solutionCallback.value(this.var));
		}
		if (isHiddenVar != null) {
			sb.append(", hidden=").append(solutionCallback.value(this.isHiddenVar));
		}
		if (!subPartSelectedVars.isEmpty()) {//TODO
			sb.append(", subParts=").append(subPartSelectedVars.size());
		}
		sb.append("}");
		return sb.toString();
	}
} 