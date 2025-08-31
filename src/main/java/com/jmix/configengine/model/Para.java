package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 参数定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Para extends ProgrammableObject<String> {
    public static final String DEFAULT_MIN_VALUE = "-1000";//TODO
    public static final String DEFAULT_MAX_VALUE = "1000";
	/**
	 * 参数类型
	 */
	private ParaType type= ParaType.ENUM;
	
	/**
	 * 枚举类型下的具体枚举值
	 */
	private List<ParaOption> options= new ArrayList<>();
	
	/**
	 * Range类型的最小值
	 */
	private String minValue= DEFAULT_MIN_VALUE;
	
	/**
	 * Range类型的最大值
	 */
	private String maxValue= DEFAULT_MAX_VALUE;

	@JsonIgnore
	public ParaOption getOption(String optionCode) {
		if (options == null || optionCode == null) return null;
		for (ParaOption opt : options) {
			if (optionCode.equals(opt.getCode())) return opt;
		}
		return null;
	}

	@JsonIgnore
	public long[] getOptionIds() {
		if (options == null || options.isEmpty()) return new long[0];
		long[] ids = new long[options.size()];
		for (int i = 0; i < options.size(); i++) {
			ids[i] = options.get(i).getCodeId();
		}
		return ids;
	}
	@JsonIgnore
	public String[] getOptionCodes() {
		if (options == null || options.isEmpty()) return new String[0];
		String[] codes = new String[options.size()];
		for (int i = 0; i < options.size(); i++) {
			codes[i] = options.get(i).getCode();
		}
		return codes;
	}
}