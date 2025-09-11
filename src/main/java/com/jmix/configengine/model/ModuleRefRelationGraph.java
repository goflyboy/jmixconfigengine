package com.jmix.configengine.model;

import com.jmix.configengine.model.schema.RefProgObjSchema;
import com.jmix.configengine.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 模块引用关系图
 * 用于管理模块中规则之间的依赖关系
 */
@Slf4j
public class ModuleRefRelationGraph {
    
    /**
     * 依赖关系图：从左侧编程对象到右侧编程对象的映射
     * Key: 左侧编程对象编码，Value: 右侧编程对象编码列表
     */
    private Map<String, Set<String>> fromToMap = new HashMap<>();
    
    /**
     * 规则编码到依赖关系的映射
     * Key: 规则编码，Value: 依赖关系信息
     */
    private Map<String, RelationInfo> ruleRelationMap = new HashMap<>();
    
    /**
     * 依赖关系信息
     */
    private static class RelationInfo {
        private String ruleCode;
        private List<RefProgObjSchema> fromLefts;
        private RefProgObjSchema toRight;
        
        public RelationInfo(String ruleCode, List<RefProgObjSchema> fromLefts, RefProgObjSchema toRight) {
            this.ruleCode = ruleCode;
            this.fromLefts = fromLefts;
            this.toRight = toRight;
        }
        
        public String getRuleCode() { return ruleCode; }
        public List<RefProgObjSchema> getFromLefts() { return fromLefts; }
        public RefProgObjSchema getToRight() { return toRight; }
    }
    
    /**
     * 按Rule维度添加关系
     * @param edgeRuleCode 规则编码
     * @param fromLefts 左侧编程对象列表
     * @param toRight 右侧编程对象
     */
    public void add(String edgeRuleCode, List<RefProgObjSchema> fromLefts, RefProgObjSchema toRight) {
        if (fromLefts == null || fromLefts.isEmpty() || toRight == null) {
            log.warn("Invalid relation data: ruleCode={}, fromLefts={}, toRight={}", 
                    edgeRuleCode, fromLefts, toRight);
            return;
        }
        
        // 遍历fromLefts，调用单个添加方法
        for (RefProgObjSchema fromLeft : fromLefts) {
            add(edgeRuleCode, fromLeft, toRight);
        }
        
        // 保存完整的依赖关系信息
        ruleRelationMap.put(edgeRuleCode, new RelationInfo(edgeRuleCode, fromLefts, toRight));
    }
    
    /**
     * 按Rule维度添加关系
     * @param edgeRuleCode 规则编码
     * @param fromLeft 左侧编程对象
     * @param toRight 右侧编程对象
     */
    public void add(String edgeRuleCode, RefProgObjSchema fromLeft, RefProgObjSchema toRight) {
        if (fromLeft == null || toRight == null) {
            log.warn("Invalid relation data: ruleCode={}, fromLeft={}, toRight={}", 
                    edgeRuleCode, fromLeft, toRight);
            return;
        }
        
        String fromCode = fromLeft.getProgObjCode();
        String toCode = toRight.getProgObjCode();
        
        // 构建图
        fromToMap.computeIfAbsent(fromCode, k -> new HashSet<>()).add(toCode);
        
        log.debug("Added relation: {} -> {} (rule: {})", fromCode, toCode, edgeRuleCode);
    }
    
    /**
     * 根据progObjCodes查找依赖的 edgeRuleCodes和RefProgObjSchemas(节点）
     * 
     * 例如：
     * 关系如下：
     * Rule01: From(left):P0,To(Right):P11  
     * Rule02: From(left):P0,To(Right):P21
     * Rule11: From(left):P11,To(Right):PT1
     * Rule21: From(left):[P21,P22],To(Right):PT2
     *
     * 查询结果：
     * 1、输入：progObjCodes={PT1,P0} 输出:{ruleCode=[Rule11,Rule01],refProgObjCodes=[PT1,P11,P0](包含输入)
     * 2、输入：progObjCodes={PT2} 输出:{ruleCode=[Rule21,Rule02],refProgObjCodes=[PT2,P21,P22,P0](包含输入)
     * 
     * @param progObjCodes 编程对象编码数组
     * @return Pair<依赖的ruleCode列表, 依赖RefProgObjSchema列表>
     */
    public Pair<List<String>, List<RefProgObjSchema>> querySubGraph(String... progObjCodes) {
        Set<String> tmpEdgeRuleCode = new HashSet<>();
        Set<RefProgObjSchema> tmpRefProgObjCodes = new HashSet<>();
        
        // 遍历progObjCodes，对每个progObjCode
        for (String progObjCode : progObjCodes) {
            // 根据图progObjCode查找依赖的节点，到有progObjCodes的终止，将节点和边的信息添加到tmpEdgeRuleCode，tmpRefProgObjCodes
            findDependencies(progObjCode, new HashSet<>(), tmpEdgeRuleCode, tmpRefProgObjCodes, Arrays.asList(progObjCodes));
        }
        
        // 添加输入的编程对象
        for (String progObjCode : progObjCodes) {
            RefProgObjSchema refProgObj = new RefProgObjSchema();
            refProgObj.setProgObjCode(progObjCode);
            tmpRefProgObjCodes.add(refProgObj);
        }
        
        List<String> ruleCodes = new ArrayList<>(tmpEdgeRuleCode);
        List<RefProgObjSchema> refProgObjs = new ArrayList<>(tmpRefProgObjCodes);
        
        log.info("Query subgraph for {}: found {} rules and {} progObjs", 
                Arrays.toString(progObjCodes), ruleCodes.size(), refProgObjs.size());
        
        return Pair.of(ruleCodes, refProgObjs);
    }
    
    /**
     * 递归查找依赖关系
     * @param currentCode 当前编程对象编码
     * @param visited 已访问的编码集合，防止循环依赖
     * @param ruleCodes 收集到的规则编码
     * @param refProgObjs 收集到的编程对象
     * @param targetCodes 目标编码列表
     */
    private void findDependencies(String currentCode, Set<String> visited, 
                                 Set<String> ruleCodes, Set<RefProgObjSchema> refProgObjs, 
                                 List<String> targetCodes) {
        if (visited.contains(currentCode)) {
            return; // 防止循环依赖
        }
        visited.add(currentCode);
        
        // 查找当前编码作为左侧的所有依赖关系
        Set<String> dependentCodes = fromToMap.get(currentCode);
        if (dependentCodes != null) {
            for (String dependentCode : dependentCodes) {
                // 添加依赖的编程对象
                RefProgObjSchema refProgObj = new RefProgObjSchema();
                refProgObj.setProgObjCode(dependentCode);
                refProgObjs.add(refProgObj);
                
                // 查找相关的规则
                for (Map.Entry<String, RelationInfo> entry : ruleRelationMap.entrySet()) {
                    RelationInfo relationInfo = entry.getValue();
                    if (relationInfo.getFromLefts().stream().anyMatch(left -> left.getProgObjCode().equals(currentCode)) &&
                        relationInfo.getToRight().getProgObjCode().equals(dependentCode)) {
                        ruleCodes.add(entry.getKey());
                    }
                }
                
                // 如果依赖的编码不在目标列表中，继续递归查找
                if (!targetCodes.contains(dependentCode)) {
                    findDependencies(dependentCode, visited, ruleCodes, refProgObjs, targetCodes);
                }
            }
        }
    }
    
    /**
     * 获取图的统计信息
     * @return 图的统计信息字符串
     */
    public String getGraphInfo() {
        int totalRelations = fromToMap.values().stream().mapToInt(Set::size).sum();
        return String.format("ModuleRefRelationGraph: %d nodes, %d relations, %d rules", 
                fromToMap.size(), totalRelations, ruleRelationMap.size());
    }
}
