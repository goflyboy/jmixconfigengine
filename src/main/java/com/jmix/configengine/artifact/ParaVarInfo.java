package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.stream.Collectors;

import com.jmix.configengine.model.Para;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParaVarInfo extends VarInfo<Para> {
	private String code;
	private List<ParaOptionVarInfo> options;
	
	public ParaVarInfo() {
		super();
	}
	
	public String getOptionIdsStr(){
		return options.stream().map(ParaOptionVarInfo::getCodeId).map(String::valueOf).collect(Collectors.joining(","));
	}
	
	public ParaVarInfo(Para para) {
		super(para);
	}
} 