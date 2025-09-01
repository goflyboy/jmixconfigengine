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
	 * 可见性模式属性
	 * 0-可见，可改(默认值）
	 * 1-可见，不可改
	 * 2-不可见，可改（不存在）
	 * 3-不可见，不可改
	 */
	public IntVar visibilityModeVar;
	
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
		if (visibilityModeVar != null) {
			sb.append(", visibilityMode=").append(solutionCallback.value(this.visibilityModeVar));
		}
		if (!subPartSelectedVars.isEmpty()) {//TODO
			sb.append(", subParts=").append(subPartSelectedVars.size());
		}
		sb.append("}");
		return sb.toString();
	}
} 