package com.jmix.configengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.jmix.configengine.model.schema.RefProgObjSchema;
import com.jmix.configengine.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 模块定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Module extends ProgrammableObject<Integer> {
	/**
	 * 版本信息
	 */
	private Long id = 0L;
	
	/**
	 * 版本号
	 */
	private String version = "1.0.0";
	
	/**
	 * 包名
	 */
	private String packageName;
	
	/**
	 * 模块类型
	 */
	private ModuleType type = ModuleType.GENERAL;
	
	/**
	 * 参数列表
	 */
	private List<Para> paras = new ArrayList<>();
	
	/**
	 * 部件列表
	 */
	private List<Part> parts = new ArrayList<>();
	
	/**
	 * 规则列表
	 */
	private List<Rule> rules = new ArrayList<>();
	
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
	 * 模块引用关系图
	 */
	@JsonIgnore
	private ModuleRefRelationGraph refRelationGraph;
	
	/**
	 * 初始化方法，建立映射关系提升效率
	 */
	@JsonIgnore
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
		initShortCode();
		initRefRelationGraph();
	}
	private void initShortCode() {
		int index = 0;
		for (Para para : paras) {
			para.setShortCode(Para.SHORT_CODE_PREFIX + index);
			index++;
		}
		index = 0;
		for (Part part : parts) {
			part.setShortCode(Part.SHORT_CODE_PREFIX + index);
			index++;
		}
	}
	@JsonIgnore
	public String getProgObjShortCodeMemo() {
		//2、shortCodes(P1:Size, P2:Color,PT1:part1,PT2:part2)
		StringBuilder sb = new StringBuilder();
		sb.append("ProgObjs(");
		for (Para para : paras) {
			sb.append(para.getShortCode()).append(":").append(para.getCode()).append(",");
		}
		for (Part part : parts) {
			sb.append(part.getShortCode()).append(":").append(part.getCode()).append(",");
		}
		sb.append(")");
		return sb.toString();
	}

	@JsonIgnore
	public String getAttrShortCodeMemo() {
		return "Attrs(V:value, H:isHidden, Q:qty)";
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
	
	/**
	 * 初始化引用关系图
	 */
	private void initRefRelationGraph() {
		refRelationGraph = new ModuleRefRelationGraph();
		
		if (rules == null || rules.isEmpty()) {
			return;
		}
		
		// 遍历module.getRules的每个rule
		for (Rule rule : rules) {
			List<RefProgObjSchema> fromLeftProgObjs = trimDuplicateRefProgObjs(rule.getFromLeftProgObjs());
			List<RefProgObjSchema> toRightProgObjs = trimDuplicateRefProgObjs(rule.getToRightProgObjs());
			
			// 如果toRightProgObjs.size != 1，则抛异常
			// if (toRightProgObjs.size() != 1) {
			// 	throw new RuntimeException("Rule " + rule.getCode() + " must have exactly one right progObj, but found " + toRightProgObjs.size());
			// }
			
			// 调用refRelationGraph.add(rule.getCode(),fromLeftProgObjs,toRightProgObjs.get(0))
			refRelationGraph.add(rule.getCode(), fromLeftProgObjs, toRightProgObjs);
		}
	}
	
	/**
	 * 对RefProgObjs进行去重（progObjCode）作为主键
	 * @param refProgObjs 引用编程对象列表
	 * @return 去重后的引用编程对象列表
	 */
	@JsonIgnore
	private List<RefProgObjSchema> trimDuplicateRefProgObjs(List<RefProgObjSchema> refProgObjs) {
		if (refProgObjs == null || refProgObjs.isEmpty()) {
			return new ArrayList<>();
		}
		
		Map<String, RefProgObjSchema> uniqueMap = new LinkedHashMap<>();
		for (RefProgObjSchema refProgObj : refProgObjs) {
			if (refProgObj != null && refProgObj.getProgObjCode() != null) {
				uniqueMap.put(refProgObj.getProgObjCode(), refProgObj);
			}
		}
		
		return new ArrayList<>(uniqueMap.values());
	}
	
	/**
	 * 查询子图
	 * @param progObjCodes 编程对象编码数组
	 * @return Pair<依赖的ruleCode列表, 依赖RefProgObjSchema列表>
	 */
	@JsonIgnore
	public Pair<List<String>, List<RefProgObjSchema>> querySubGraph(String... progObjCodes) {
		if (refRelationGraph == null) {
			initRefRelationGraph();
		}
		return refRelationGraph.querySubGraph(progObjCodes);
	}
} 