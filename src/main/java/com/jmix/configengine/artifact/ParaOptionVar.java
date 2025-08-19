package com.jmix.configengine.artifact;

import lombok.Data;

import com.google.ortools.sat.BoolVar;
import com.jmix.configengine.model.ParaOption;

/**
 * 参数选项变量
 * 用于表示参数选项的选中状态和相关信息
 */
@Data
public class ParaOptionVar extends Var<ParaOption> {
	/**
	 * 参数选项的选中状态（布尔变量）
	 */
	public BoolVar isSelectedVar;

	public ParaOptionVar() {
	}

	public ParaOptionVar(ParaOption base) {
		this.base = base;
	}

	public ParaOptionVar(ParaOption base, BoolVar isSelectedVar) {
		this.base = base;
		this.isSelectedVar = isSelectedVar;
	}

	public String getCode() {
		return base != null ? base.getCode() : null;
	}

	public int getCodeId() {
		return base != null ? base.getCodeId() : 0;
	}
} 