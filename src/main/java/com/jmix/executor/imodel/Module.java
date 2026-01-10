package com.jmix.executor.imodel;

import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.impl.ModuleRefRelationGraph;
import com.jmix.executor.impl.util.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 模块定义
 * 表示一个完整的约束模块，包含参数、部件和规则
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Module extends ProgrammableObject<Integer> implements IModule {

    /**
     * 默认版本号常量
     */
    public static final String DEFAULT_VERSION = "1.0.0";

    /**
     * 版本信息
     */
    private Long id = 0L;

    /**
     * 版本号
     */
    private String version = DEFAULT_VERSION;

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
        initSubParts();
    }

    private void initShortCode() {
        int index = 0;
        for (Para para : paras) {
            if (para.getCode().length() <= 3) { // 如果编码长度小于等于3，则直接使用编码
                para.setShortCode(para.getCode());
            } else {
                para.setShortCode(Para.SHORT_CODE_PREFIX + index);
                index++;
            }
        }
        index = 0;
        for (Part part : parts) {
            if (part.getCode().length() <= 3) { // 如果编码长度小于等于3，则直接使用编码
                part.setShortCode(part.getCode());
            } else {
                part.setShortCode(Part.SHORT_CODE_PREFIX + index);
                index++;
            }
        }
    }

    /**
     * 获取编程对象短代码备忘录
     * 
     * @return 编程对象短代码字符串
     */
    @JsonIgnore
    public String getProgObjShortCodeMemo() {
        // 2、shortCodes(P1:Size, P2:Color,PT1:part1,PT2:part2)
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

    /**
     * 获取属性短代码备忘录
     * 
     * @return 属性短代码字符串
     */
    @JsonIgnore
    public String getAttrShortCodeMemo() {
        return "Attrs(V:value, H:isHidden, Q:qty)";
    }

    /**
     * 根据编码获取参数对象
     * 
     * @param code 参数编码
     * @return 参数对象，如果不存在则返回Optional.empty()
     */
    @JsonIgnore
    public Optional<Para> getPara(String code) {
        if (code == null || paraMap.isEmpty()) {
            return Optional.empty();
        }
        Para para = paraMap.get(code);
        return para != null ? Optional.of(para) : Optional.empty();
    }

    /**
     * 根据编码获取部件对象
     * 
     * @param code 部件编码
     * @return 部件对象，如果不存在则返回Optional.empty()
     */
    @JsonIgnore
    public Optional<Part> getPart(String code) {
        if (code == null || partMap.isEmpty()) {
            return Optional.empty();
        }
        Part part = partMap.get(code);
        return part != null ? Optional.of(part) : Optional.empty();
    }

    /**
     * 获取规则列表
     * 
     * @return 规则列表
     */
    @JsonIgnore
    @Override
    public List<Rule> getRules() {
        return rules != null ? rules : new ArrayList<>();
    }

    /**
     * 根据父部件编码获取子部件列表
     * 
     * @param fatherCode 父部件编码
     * @return 子部件列表
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
     * 
     * @return 顶级部件列表
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
     * 
     * @param code 参数编码
     * @return 如果包含则返回true，否则返回false
     */
    @JsonIgnore
    public boolean hasPara(String code) {
        return code != null && paraMap.containsKey(code);
    }

    /**
     * 检查是否包含指定编码的部件
     * 
     * @param code 部件编码
     * @return 如果包含则返回true，否则返回false
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
            // 调用refRelationGraph.add(rule.getCode(),fromLeftProgObjs,toRightProgObjs.get(0))
            refRelationGraph.add(rule.getCode(), fromLeftProgObjs, toRightProgObjs);
        }
    }

    /**
     * 初始化单个PartCategory
     *
     * @param category 部件分类
     */
    private void initSubParts() {
        // 初始化partCategoryMap（子分类）
        for (Part part : parts) {
            if (Strings.isNullOrEmpty(part.getFatherCode())) {
                continue;
            }
            Part fatherPart = this.partMap.get(part.getFatherCode());
            if (fatherPart instanceof PartCategory) {
                ((PartCategory) fatherPart).addSubPart(part);
            } else {
                throw new IllegalStateException("Father part '" + part.getFatherCode() +
                        "' is not a PartCategory, cannot add subpart '" + part.getCode() + "'");
            }
        }
    }

    /**
     * 对RefProgObjs进行去重（progObjCode）作为主键
     * 
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
     * 
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