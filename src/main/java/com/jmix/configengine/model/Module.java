package com.jmix.configengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 模块定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Module extends ProgramableObject<Integer> {
	/**
	 * 版本信息
	 */
	private Long id = 0L;
	
	/**
	 * 版本号
	 */
	private String version;
	
	/**
	 * 包名
	 */
	private String packageName;
	
	/**
	 * 模块类型
	 */
	private ModuleType type;
	
	/**
	 * 参数列表
	 */
	private List<Para> paras;
	
	/**
	 * 部件列表
	 */
	private List<Part> parts;
	
	/**
	 * 规则列表
	 */
	private List<Rule> rules;
	
	/**
	 * 算法制品描述信息
	 */
	private ModuleAlgArtifact alg;
	
	@JsonIgnore
	private Map<String, Para> paraMap = new HashMap<>();
	
	@JsonIgnore
	private Map<String, Part> partMap = new HashMap<>();
	
	@JsonIgnore
	private Map<String, Object> errorMap = new HashMap<>();
	
	/**
	 * 初始化方法，建立映射关系提升效率
	 */
	public void init() {
		if (paras != null) {
			for (Para para : paras) {
				paraMap.put(para.getCode(), para);
			}
		}
		
		if (parts != null) {
			for (Part part : parts) {
				partMap.put(part.getCode(), part);
			}
		}
	}
	
	/**
	 * 根据编码获取参数对象
	 */
	@JsonIgnore
	public Para getPara(String code) {
		if (code == null || paraMap.isEmpty()) {
			return null;
		}
		return paraMap.get(code);
	}
	
	/**
	 * 根据编码获取部件对象
	 */
	@JsonIgnore
	public Part getPart(String code) {
		if (code == null || partMap.isEmpty()) {
			return null;
		}
		return partMap.get(code);
	}
	
	/**
	 * 根据父部件编码获取子部件列表
	 */
	@JsonIgnore
	public List<Part> getChildrenPart(String fatherCode) {
		if (fatherCode == null || parts == null) {
			return new ArrayList<>();
		}
		
		return parts.stream()
				.filter(part -> fatherCode.equals(part.getFatherCode()))
				.collect(java.util.stream.Collectors.toList());
	}
	
	/**
	 * 获取所有顶级部件（没有父部件的部件）
	 */
	@JsonIgnore
	public List<Part> getTopLevelParts() {
		if (parts == null) {
			return new ArrayList<>();
		}
		
		return parts.stream()
				.filter(part -> part.getFatherCode() == null || part.getFatherCode().isEmpty())
				.collect(java.util.stream.Collectors.toList());
	}
	
	/**
	 * 检查是否包含指定编码的参数
	 */
	@JsonIgnore
	public boolean hasPara(String code) {
		return code != null && paraMap.containsKey(code);
	}
	
	/**
	 * 检查是否包含指定编码的部件
	 */
	@JsonIgnore
	public boolean hasPart(String code) {
		return code != null && partMap.containsKey(code);
	}
} 