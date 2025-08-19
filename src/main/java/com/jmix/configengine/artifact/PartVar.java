package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.jmix.configengine.model.Part;

/**
 * 部件变量
 */
@Data
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
} 