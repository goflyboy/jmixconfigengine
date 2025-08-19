package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParaVarInfo extends VarInfo<com.jmix.configengine.model.Para> {
	private String code;
	private List<ParaOptionVarInfo> options;
	
	public ParaVarInfo() {
		super();
	}
	
	public String getOptionIdsStr(){
		return options.stream().map(ParaOptionVarInfo::getCodeId).map(String::valueOf).collect(Collectors.joining(","));
	}
	
	public ParaVarInfo(com.jmix.configengine.model.Para para) {
		super(para);
	}
} 