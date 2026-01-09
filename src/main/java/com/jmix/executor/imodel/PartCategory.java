package com.jmix.executor.imodel;

import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AttrFunConstant;
import com.jmix.executor.omodel.PartConstantAttr;
import com.jmix.executor.omodel.PartConstraintReq;
import com.jmix.tool.impl.FilterExpressionExecutor;
import com.jmix.tool.impl.FilterExpressionExecutor.FilterCondition;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
        // 在this根据查找code=constraintReq.partCatagoryCode的reqPartCategory,可能是this，也可能是this下的子PartCategory
        PartCategory reqPartCategory = findPartCategoryByCode(this, constraintReq.getPartCatagoryCode());
        if (reqPartCategory == null) {
            throw new IllegalArgumentException("PartCategory not found: " + constraintReq.getPartCatagoryCode());
        }
        return query(reqPartCategory, constraintReq);
    }

    /**
     * 根据代码查找PartCategory（可能是this，也可能是子PartCategory）
     *
     * @param category 当前PartCategory
     * @param code     要查找的代码
     * @return 找到的PartCategory，如果未找到则返回null
     */
    @JsonIgnore
    private PartCategory findPartCategoryByCode(PartCategory category, String code) {
        if (code == null || code.trim().isEmpty()) {
            log.error("PartCategory code is null or empty");
            return null;
        }
        // 如果是当前PartCategory的code，直接返回
        if (category.getCode().equals(code)) {
            return category;
        }
        // 在子PartCategory中查找
        if (category.partCategoryMap != null && !category.partCategoryMap.isEmpty()) {
            // 先在直接子分类中查找
            PartCategory found = category.partCategoryMap.get(code);
            if (found != null) {
                return found;
            }
            // 递归在子分类中查找
            for (PartCategory subCategory : category.partCategoryMap.values()) {
                PartCategory result = findPartCategoryByCode(subCategory, code);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
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
        String attrWhereCondition = constraintReq.getAttrWhereCondition();
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

        // 解析属性代码
        Pair<DynamicAttribute, String> attrResult = parseAttributeFromCondition(constraintReq.getAttrWhereCondition(),
                category.getDynAttrSchemas());

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
        resultPartCategory.addSubParts(filterParts);
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

        DynamicAttribute attr = findAttribute(fieldName, dynAttrSchemas);
        return Pair.of(attr, AttrFunConstant.FUN_PREFIX_EMPTY);
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

        DynamicAttribute attr = findAttribute(attrCode, dynAttrSchemas);
        return Pair.of(attr, funPrefix);
    }

    /**
     * 根据属性代码查找动态属性
     *
     * @param attrCode       属性代码
     * @param dynAttrSchemas 动态属性schema列表
     * @return 找到的DynamicAttribute对象
     * @throws IllegalArgumentException 当属性未找到时抛出
     */
    @JsonIgnore
    private DynamicAttribute findAttribute(String attrCode, List<DynamicAttribute> dynAttrSchemas) {
        // 首先在动态属性schema列表中查找
        for (DynamicAttribute attr : dynAttrSchemas) {
            if (attr.getCode().equals(attrCode)) {
                return attr;
            }
        }

        // 如果在schema列表中找不到，则在常量属性中查找
        DynamicAttribute cAttr = PartConstantAttr.getAttr(attrCode);
        if (cAttr != null) {
            return cAttr;
        }

        // 如果都找不到，抛出异常
        throw new IllegalArgumentException("Attribute '" + attrCode + "' not found in schema");
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
     * 添加子部件
     *
     * @param subPart 子部件（可以是Part或PartCategory）
     */
    public void addSubPart(Part subPart) {
        if (subPart instanceof PartCategory) {
            this.partCategoryMap.put(subPart.getCode(), (PartCategory) subPart);
        } else {
            this.partMap.put(subPart.getCode(), subPart);
        }
    }

    /**
     * 添加子部件列表
     *
     * @param subParts 子部件列表（可以是Part或PartCategory）
     */
    public void addSubParts(List<? extends Part> subParts) {
        for (Part part : subParts) {
            addSubPart(part);
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
