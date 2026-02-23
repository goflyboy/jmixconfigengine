package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.ModuleRefRelationGraph;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块定义
 * 表示一个完整的约束模块，包含参数、部件和规则
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Module extends ModuleBase implements IModule {

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
     * 算法制品描述信息
     */
    private ModuleAlgArtifact alg;

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
        initShortCode();
        initRefRelationGraph();
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
        for (Para para : getParas()) {
            sb.append(para.getShortCode()).append(":").append(para.getCode()).append(",");
        }
        for (IPart part : getAllParts()) {
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
     * 获取规则列表
     * 
     * @return 规则列表
     */
    @Override
    public List<Rule> getRules() {
        List<Rule> rules = super.getRules();
        return rules != null ? rules : new ArrayList<>();
    }

    /**
     * 初始化引用关系图
     */
    private void initRefRelationGraph() {
        refRelationGraph = new ModuleRefRelationGraph();

        List<Rule> rules = getRules();
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

    @Override
    @JsonIgnore
    public boolean hasPriorityRule() {
        List<Rule> priorityRules = queryPriorityRules();
        return !priorityRules.isEmpty();
    }

    @Override
    @JsonIgnore
    public List<Rule> queryPriorityRules() {
        List<Rule> priorityRules = new ArrayList<>();
        List<Rule> rules = getRules();
        if (rules == null || rules.isEmpty()) {
            return priorityRules;
        }
        for (Rule rule : rules) {
            if (RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName())) {
                priorityRules.add(rule);
            }
        }
        return priorityRules;
    }
}