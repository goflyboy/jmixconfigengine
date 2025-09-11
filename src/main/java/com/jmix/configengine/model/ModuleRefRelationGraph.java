package com.jmix.configengine.model;

import com.jmix.configengine.model.schema.RefProgObjSchema;
import com.jmix.configengine.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 模块引用关系图
 * 用于管理模块中规则之间的依赖关系
 * 
 * 设计说明：
 * - 每个编程对象(progObjCode)是一个节点(Node)
 * - 每个规则(edgeRuleCode)是边上的信息
 * - 支持一个规则对应多个左侧节点的情况
 */
@Slf4j
public class ModuleRefRelationGraph {
    
    /**
     * 图的边信息：从左侧节点到右侧节点的映射
     * Key: 左侧节点编码，Value: 右侧节点编码到规则编码的映射
     */
    private Map<String, Map<String, String>> fromToEdges = new HashMap<>();
    
    /**
     * 反向图的边信息：从右侧节点到左侧节点的映射
     * Key: 右侧节点编码，Value: 左侧节点编码到规则编码的映射
     */
    private Map<String, Map<String, String>> toFromEdges = new HashMap<>();
    
    /**
     * 所有节点的集合
     */
    private Set<String> allNodes = new HashSet<>();

    private Map<String, RefProgObjSchema> allRefProgObjMap = new LinkedHashMap<>();
    
    /**
     * 按Rule维度添加关系（多个左侧节点和多个右侧节点）
     * @param edgeRuleCode 规则编码
     * @param fromLefts 左侧编程对象列表
     * @param toRights 右侧编程对象列表
     */
    public void add(String edgeRuleCode, List<RefProgObjSchema> fromLefts, List<RefProgObjSchema> toRights) {
        if (fromLefts == null || fromLefts.isEmpty() || toRights == null || toRights.isEmpty()) {
            log.warn("Invalid relation data: ruleCode={}, fromLefts={}, toRights={}", 
                    edgeRuleCode, fromLefts, toRights);
            return;
        }
        
        // 双循环调用单个添加方法
        for (RefProgObjSchema fromLeft : fromLefts) {
            for (RefProgObjSchema toRight : toRights) {
                add(edgeRuleCode, fromLeft, toRight);
            }
        }
    }
    
    /**
     * 按Rule维度添加关系（单个左侧节点）
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
        allRefProgObjMap.put(fromLeft.getProgObjCode(), fromLeft);
        allRefProgObjMap.put(toRight.getProgObjCode(), toRight);
        String fromCode = fromLeft.getProgObjCode();
        String toCode = toRight.getProgObjCode();
        
        // 添加节点
        allNodes.add(fromCode);
        allNodes.add(toCode);
        
        // 构建正向边：fromLeft -> toRight
        fromToEdges.computeIfAbsent(fromCode, k -> new HashMap<>()).put(toCode, edgeRuleCode);
        
        // 构建反向边：toRight -> fromLeft
        toFromEdges.computeIfAbsent(toCode, k -> new HashMap<>()).put(fromCode, edgeRuleCode);
        
        log.debug("Added edge: {} -> {} (rule: {})", fromCode, toCode, edgeRuleCode);
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
        Map<String,RefProgObjSchema> tmpRefProgObjCodes = new HashMap<>();
        
        // 添加输入的编程对象
        for (String progObjCode : progObjCodes) {
            //如果是null，则跳过
            if (allRefProgObjMap.get(progObjCode) == null) {
                continue;
            }
            tmpRefProgObjCodes.put(progObjCode, allRefProgObjMap.get(progObjCode));
        }
        
        // 遍历progObjCodes，对每个progObjCode查找其依赖关系
        for (String progObjCode : progObjCodes) {
            findDependencies(progObjCode, new HashSet<>(), tmpEdgeRuleCode, tmpRefProgObjCodes, Arrays.asList(progObjCodes));
        }
        
        List<String> ruleCodes = new ArrayList<>(tmpEdgeRuleCode);
        List<RefProgObjSchema> refProgObjs = new ArrayList<>(tmpRefProgObjCodes.values());
        
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
                                 Set<String> ruleCodes, Map<String,RefProgObjSchema> refProgObjs, 
                                 List<String> targetCodes) {
        if (visited.contains(currentCode)) {
            return; // 防止循环依赖
        }
        visited.add(currentCode);
        
        // 查找当前编码作为右侧节点的所有依赖关系（向上查找）
        Map<String, String> dependentEdges = toFromEdges.get(currentCode);
        if (dependentEdges != null) {
            for (Map.Entry<String, String> edge : dependentEdges.entrySet()) {
                String dependentCode = edge.getKey();
                String ruleCode = edge.getValue();
                
                // 添加依赖的编程对象
                refProgObjs.put(dependentCode, allRefProgObjMap.get(dependentCode));
                
                // 添加规则编码
                ruleCodes.add(ruleCode);
                
                // 继续递归查找依赖
                findDependencies(dependentCode, visited, ruleCodes, refProgObjs, targetCodes);
            }
        }
    }
    
    /**
     * 获取图的统计信息
     * @return 图的统计信息字符串
     */
    public String getGraphInfo() {
        int totalEdges = fromToEdges.values().stream().mapToInt(Map::size).sum();
        return String.format("ModuleRefRelationGraph: %d nodes, %d edges", 
                allNodes.size(), totalEdges);
    }
    
    /**
     * 获取所有节点
     * @return 所有节点编码的集合
     */
    public Set<String> getAllNodes() {
        return new HashSet<>(allNodes);
    }
    
    /**
     * 检查图中是否存在指定节点
     * @param nodeCode 节点编码
     * @return 是否存在
     */
    public boolean containsNode(String nodeCode) {
        return allNodes.contains(nodeCode);
    }
    
    /**
     * 获取指定节点的出边（从该节点出发的边）
     * @param nodeCode 节点编码
     * @return 出边映射（目标节点 -> 规则编码）
     */
    public Map<String, String> getOutEdges(String nodeCode) {
        return fromToEdges.getOrDefault(nodeCode, new HashMap<>());
    }
    
    /**
     * 获取指定节点的入边（指向该节点的边）
     * @param nodeCode 节点编码
     * @return 入边映射（源节点 -> 规则编码）
     */
    public Map<String, String> getInEdges(String nodeCode) {
        return toFromEdges.getOrDefault(nodeCode, new HashMap<>());
    }
}