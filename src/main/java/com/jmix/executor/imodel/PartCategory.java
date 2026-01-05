package com.jmix.executor.imodel;

import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AttrFunConstant;
import com.jmix.executor.omodel.PartConstraintReq;
import com.jmix.tool.impl.FilterExpressionExecutor;
import com.jmix.tool.impl.FilterExpressionExecutor.FilterCondition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 部件分类
 * 表示部件的分类定义，继承自Part
 *
 * @since 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartCategory extends Part {

    @JsonIgnore
    private Map<String, PartCategory> partCategoryMap = new HashMap<>();

    @JsonIgnore
    private Map<String, Part> partMap = new HashMap<>();

    public PartCategory() {
        super();
        this.setPartType(PartType.CATEGORY);
    }

    /**
     * 克隆PartCategory对象
     *
     * @return 克隆的PartCategory对象
     */
    @JsonIgnore
    public PartCategory clone() {
        PartCategory pc = new PartCategory();
        // 复制ProgrammableObject属性
        pc.setCode(this.getCode());
        pc.setFatherCode(this.getFatherCode());
        pc.setDefaultValue(this.getDefaultValue());
        pc.setDescription(this.getDescription());
        pc.setSortNo(this.getSortNo());
        pc.setShortCode(this.getShortCode());

        // 复制Part属性
        pc.setPartType(this.getPartType());
        pc.setPrice(this.getPrice());
        pc.setMaxQuantity(this.getMaxQuantity());
        pc.setDynAttr(new HashMap<>(this.getDynAttr()));
        pc.setDynAttrSchemas(this.getDynAttrSchemas());
        pc.setDynAttrSchema(this.getDynAttrSchema());

        // 不复制partCategoryMap和partMap，这些将在init方法中重新初始化
        return pc;
    }

    /**
     * 初始化方法
     * 对partCategoryMap、partMap进行初始化
     */
    @JsonIgnore
    public void init() {
        // 初始化逻辑将在Module的init方法中实现
    }

    /**
     * 查询满足条件的Part
     *
     * @param constraintReq 约束请求
     * @return 满足条件的PartCategory列表
     */
    public PartCategory query(PartConstraintReq constraintReq) {
        return query(this, constraintReq);
    }

    /**
     * 查询满足条件的Part
     *
     * @param category      部件分类
     * @param constraintReq 约束请求
     * @return 满足条件的PartCategory
     */
    public PartCategory query(PartCategory category, PartConstraintReq constraintReq) {
        if (constraintReq.getAttrWhereCondition() == null || constraintReq.getAttrWhereCondition().trim().isEmpty()) {
            throw new IllegalArgumentException("Filter condition cannot be empty");
        }
        return query(category, constraintReq, constraintReq.getAttrWhereCondition());
    }

    /**
     * 查询满足条件的Part
     *
     * @param category           部件分类
     * @param constraintReq      约束请求
     * @param attrWhereCondition 过滤条件字符串
     * @return 满足条件的PartCategory列表
     */
    public PartCategory query(PartCategory category, PartConstraintReq constraintReq, String attrWhereCondition) {
        PartCategory resultPartCategory = category.clone();
        List<PartCategory> resultSubPartCategory = new ArrayList<>();

        // 处理子分类
        if (!category.partCategoryMap.isEmpty()) {
            for (PartCategory pc : category.partCategoryMap.values()) {
                resultSubPartCategory.add(query(pc, constraintReq));
            }
        }
        if (!resultSubPartCategory.isEmpty()) {
            resultPartCategory.addPartCategory(resultSubPartCategory);
        }

        // 解析属性代码, 有一个非空即可，whereCondition和attrcode不能有一个实例属性，有一个不是实例属性
        Pair<DynamicAttribute, String> attrResult = null;
        if (!Strings.isNullOrEmpty(constraintReq.getAttrCode())) {
            attrResult = parseAttribute(constraintReq.getAttrCode(),
                    category.getDynAttrSchemas());
        } else {
            attrResult = parseAttributeFromCondition(constraintReq.getAttrWhereCondition(),
                    category.getDynAttrSchemas());
        }

        List<Part> filterParts = new ArrayList<>();
        if (attrResult.getFirst().getInstType() == 0) { // 非实例属性
            // 过滤条件不涉及实例属性，直接在Part级别过滤
            filterParts = FilterExpressionExecutor.doSelect(new ArrayList<>(category.partMap.values()),
                    attrWhereCondition);

        } else { // 实例属性
            for (Part part : category.partMap.values()) {
                InstanceDynAttrValue instAttrs = part.getInstanceAttrs();
                if (instAttrs == null) {
                    throw new IllegalStateException("Instance attributes not found for part: " + part.getCode());
                }
                List<InstanceDynAttrValueItem> instValues = FilterExpressionExecutor
                        .doSelect(instAttrs.getInstsValues(), attrWhereCondition);
                if (instValues.isEmpty()) {
                    continue;
                }
                int sumAttrValue = 0;
                for (InstanceDynAttrValueItem instValue : instValues) {
                    String attrValue = instValue.getInstAttr(attrResult.getFirst().getCode());
                    if (attrValue == null) {
                        throw new IllegalArgumentException(
                                "Attribute '" + attrResult.getFirst().getCode() + "' value is null for part '"
                                        + part.getCode() + "'");
                    }
                    try {
                        sumAttrValue += Integer.parseInt(attrValue);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid number format for attribute '" + attrResult.getFirst().getCode()
                                        + "' with value '" + attrValue + "' in part '" + part.getCode() + "'",
                                e);
                    }
                }
                Part cPart = part.clone();
                cPart.setAttr(attrResult.getFirst().getCode(), String.valueOf(sumAttrValue));
                filterParts.add(cPart);
            }
        }
        resultPartCategory.addPart(filterParts);
        return resultPartCategory;
    }

    /**
     * 检查属性是否为实例属性
     *
     * @param attrCode       属性代码
     * @param dynAttrSchemas 动态属性schema列表
     * @return 如果是实例属性则返回true，否则返回false
     */
    @JsonIgnore
    private boolean isInstanceAttribute(String attrCode, List<DynamicAttribute> dynAttrSchemas) {
        for (DynamicAttribute attr : dynAttrSchemas) {
            if (attr.getCode().equals(attrCode) && attr.getInstType() == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从过滤条件中解析属性
     *
     * @param whereCondition 过滤条件字符串
     * @param dynAttrSchemas 动态属性schema列表
     * @return 属性和操作符的配对
     */
    @JsonIgnore
    public Pair<DynamicAttribute, String> parseAttributeFromCondition(String whereCondition,
            List<DynamicAttribute> dynAttrSchemas) {
        Optional<FilterCondition> filterConditionOpt = FilterExpressionExecutor.parseFilterExpression(whereCondition);
        if (!filterConditionOpt.isPresent()) {
            throw new IllegalArgumentException("Invalid filter condition format: " + whereCondition);
        }

        FilterCondition filterCondition = filterConditionOpt.get();
        String fieldName = filterCondition.getFieldName();

        for (DynamicAttribute attr : dynAttrSchemas) {
            if (attr.getCode().equals(fieldName)) {
                return Pair.of(attr, AttrFunConstant.FUN_PREFIX_EMPTY);
            }
        }

        throw new IllegalArgumentException("Attribute '" + fieldName + "' not found in schema");
    }

    /**
     * 解析属性代码
     *
     * @param orgAttrCode    原始属性代码
     * @param dynAttrSchemas 动态属性schema列表
     * @return 属性和函数的配对
     */
    @JsonIgnore
    public Pair<DynamicAttribute, String> parseAttribute(String orgAttrCode, List<DynamicAttribute> dynAttrSchemas) {
        // 解析逻辑，例如："sum.Capacity" -> DynamicAttribute{Capacity}, "sum"
        String funPrefix = "";
        String attrCode = orgAttrCode;

        if (orgAttrCode.contains(".")) {
            String[] parts = orgAttrCode.split("\\.", 2);
            funPrefix = parts[0];
            attrCode = parts[1];
        }

        for (DynamicAttribute attr : dynAttrSchemas) {
            if (attr.getCode().equals(attrCode)) {
                return Pair.of(attr, funPrefix);
            }
        }

        throw new IllegalArgumentException("Attribute not found: " + attrCode);
    }

    /**
     * 添加子部件分类
     *
     * @param partCategories 子部件分类列表
     */
    public void addPartCategory(List<PartCategory> partCategories) {
        for (PartCategory partCategory : partCategories) {
            this.partCategoryMap.put(partCategory.getCode(), partCategory);
        }
    }

    /**
     * 添加部件
     *
     * @param parts 部件列表
     */
    public void addPart(List<Part> parts) {
        for (Part part : parts) {
            this.partMap.put(part.getCode(), part);
        }
    }
	
	/**
     * 获取子部件列表
     *
     * @return 子部件列表
     */
    @JsonIgnore
    public List<Part> getSubParts() {
        return new ArrayList<>(this.partMap.values());
    }
}
