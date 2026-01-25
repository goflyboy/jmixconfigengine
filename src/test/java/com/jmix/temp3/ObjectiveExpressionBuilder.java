package com.jmix.temp3;

import com.jmix.temp3.core.CpModelTracker;
import com.jmix.temp3.core.Part;
import com.jmix.temp3.core.PartVar;
import com.jmix.temp3.core.TrackedLinearExpr;

import com.google.ortools.sat.IntVar;

import lombok.Data;

import java.util.*;

/**
 * 目标构建器 - 负责构建目标函数的线性表达式
 * 使用归一化后的数据，但不包含归一化逻辑
 */
public class ObjectiveExpressionBuilder {
    private CpModelTracker model;
    private List<PartVar> partVars;
    private Map<String, PartVar> partVarMap; // code -> PartVar

    public ObjectiveExpressionBuilder(CpModelTracker model, List<PartVar> partVars) {
        this.model = model;
        this.partVars = partVars;
        this.partVarMap = new HashMap<>();

        for (PartVar pv : partVars) {
            partVarMap.put(pv.code, pv);
        }
    }

    // 目标配置
    @Data
    public static class ObjectiveConfig {
        private String name;
        private ObjectiveType type;
        private double weight;            // 归一化后的权重
        private String attrName;          // 属性名称
        private boolean quantityRelated = true; // 是否与数量相关
        private AggregateType aggregateType = AggregateType.SUM; // 聚合类型

        public ObjectiveConfig(String name, ObjectiveType type, double weight, String attrName) {
            this.name = name;
            this.type = type;
            this.weight = weight;
            this.attrName = attrName;
        }
    }

    // 目标类型
    public enum ObjectiveType {
        MAXIMIZE,    // 最大化
        MINIMIZE     // 最小化
    }

    // 聚合类型
    public enum AggregateType {
        SUM,         // 求和（数量相关）
        MAX,         // 最大值（独立目标）
        MIN,         // 最小值（独立目标）
        AVG          // 平均值
    }

    // 构建单个目标表达式
    public TrackedLinearExpr buildExpression(ObjectiveConfig config) {
        switch (config.aggregateType) {
            case SUM:
                return buildSumExpression(config);
            case MAX:
                return buildMaxExpression(config);
            case MIN:
                return buildMinExpression(config);
            case AVG:
                return buildAvgExpression(config);
            default:
                throw new IllegalArgumentException("不支持的聚合类型: " + config.aggregateType);
        }
    }

    // 构建求和表达式（数量相关）
    private TrackedLinearExpr buildSumExpression(ObjectiveConfig config) {
        TrackedLinearExpr sumExpr = new TrackedLinearExpr("sum_" + config.getName());

        for (PartVar pv : partVars) {
            Part part = pv.part;

            // 获取归一化属性值
            Double normalizedValue = part.getNormalizedAttr(config.getAttrName());
            if (normalizedValue == null) {
                // 如果没有归一化值，尝试使用原始值（应该不会发生）
                normalizedValue = part.getAttr(config.getAttrName());
            }

            // 创建表达式：quantity * normalizedValue
            sumExpr.addTerm(pv.qty, normalizedValue.longValue());
        }

        return sumExpr;
    }

    // 构建最大值表达式（独立目标）- 简化实现
    private TrackedLinearExpr buildMaxExpression(ObjectiveConfig config) {
        // 简化实现：返回总和作为近似
        // 完整实现需要复杂的约束逻辑，这里暂时使用求和代替
        return buildSumExpression(config);
    }

    // 构建最小值表达式（独立目标）- 简化实现
    private TrackedLinearExpr buildMinExpression(ObjectiveConfig config) {
        // 简化实现：返回总和作为近似
        // 完整实现需要复杂的约束逻辑，这里暂时使用求和代替
        return buildSumExpression(config);
    }

    // 构建平均值表达式
    private TrackedLinearExpr buildAvgExpression(ObjectiveConfig config) {
        // 计算总和
        TrackedLinearExpr sumExpr = buildSumExpression(config);

        // 计算选中部件数量
        IntVar selectedCount = buildSelectedCount();

        // 简化：直接使用总和，在目标函数中调整权重
        return sumExpr;
    }

    // 构建选中部件数量表达式
    private IntVar buildSelectedCount() {
        IntVar countVar = model.newIntVar(0, partVars.size(), "selected_count");

        TrackedLinearExpr countExpr = new TrackedLinearExpr("count_expr");
        for (PartVar pv : partVars) {
            // 使用isSelected（0/1）求和
            countExpr.addTerm(pv.isSelected, 1);
        }

        model.addEquality(countVar, countExpr);
        return countVar;
    }

    // 构建多个目标表达式的综合目标
    public TrackedLinearExpr buildCompositeObjective(List<ObjectiveConfig> configs) {
        Map<String, TrackedLinearExpr> expressions = new HashMap<>();

        // 构建所有表达式
        for (ObjectiveConfig config : configs) {
            TrackedLinearExpr expr = buildExpression(config);
            expressions.put(config.getName(), expr);
        }

        // 构建综合目标
        return combineExpressions(configs, expressions);
    }

    // 合并多个表达式为目标函数
    private TrackedLinearExpr combineExpressions(List<ObjectiveConfig> configs,
                                         Map<String, TrackedLinearExpr> expressions) {
        TrackedLinearExpr composite = new TrackedLinearExpr("composite_objective");

        for (ObjectiveConfig config : configs) {
            TrackedLinearExpr expr = expressions.get(config.getName());

            if (config.getType() == ObjectiveType.MAXIMIZE) {
                // 最大化：负权重（因为CP-SAT是最小化）
                composite.addExpr(expr, -(long)(config.getWeight() * 1000));
            } else {
                // 最小化：正权重
                composite.addExpr(expr, (long)(config.getWeight() * 1000));
            }
        }

        return composite;
    }

    // 创建常用的目标配置
    public static List<ObjectiveConfig> createCommonObjectives() {
        List<ObjectiveConfig> configs = new ArrayList<>();

        // 数量相关目标（求和）
        configs.add(new ObjectiveConfig("totalCost", ObjectiveType.MINIMIZE, 1.0, Part.ATTR_LISTPRICE));
        configs.add(new ObjectiveConfig("totalWeight", ObjectiveType.MAXIMIZE, 0.8, Part.ATTR_WEIGHT));
        configs.add(new ObjectiveConfig("totalProfit", ObjectiveType.MAXIMIZE, 1.2, Part.ATTR_PROFIT));
        configs.add(new ObjectiveConfig("totalCapacity", ObjectiveType.MAXIMIZE, 1.5, Part.ATTR_CAPACITY));

        // 独立目标（最大值/最小值）
        ObjectiveConfig deliveryConfig = new ObjectiveConfig("maxDelivery",
            ObjectiveType.MINIMIZE, 0.7, Part.ATTR_DELIVERYTIME);
        deliveryConfig.setQuantityRelated(false);
        deliveryConfig.setAggregateType(AggregateType.MAX);
        configs.add(deliveryConfig);

        ObjectiveConfig priorityConfig = new ObjectiveConfig("minPriority",
            ObjectiveType.MAXIMIZE, 0.5, Part.ATTR_PRIORITY_SCORE);
        priorityConfig.setQuantityRelated(false);
        priorityConfig.setAggregateType(AggregateType.MIN);
        configs.add(priorityConfig);

        return configs;
    }
}
