package com.jmix.executor.imodel;

import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.PartConstraintReq;
import com.jmix.tool.impl.FilterExpressionExecutor;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        pc.setDynAttrSchemas(new ArrayList<>(this.getDynAttrSchemas()));
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
    public List<PartCategory> query(PartConstraintReq constraintReq) {
        return query(this, constraintReq);
    }

    /**
     * 查询满足条件的Part
     *
     * @param category      部件分类
     * @param constraintReq 约束请求
     * @return 满足条件的PartCategory列表
     */
    public List<PartCategory> query(PartCategory category, PartConstraintReq constraintReq) {
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
    public List<PartCategory> query(PartCategory category, PartConstraintReq constraintReq, String attrWhereCondition) {
        PartCategory resultPartCategory = category.clone();
        List<PartCategory> resultSubPartCategory = new ArrayList<>();

        // 处理子分类
        if (!category.partCategoryMap.isEmpty()) {
            for (PartCategory pc : category.partCategoryMap.values()) {
                resultSubPartCategory.addAll(query(pc, constraintReq));
            }
        }
        resultPartCategory.addPartCategory(resultSubPartCategory);

        // 解析属性代码
        Pair<DynamicAttribute, String> attrResult = parseAttribute(constraintReq.getAttrCode(),
                category.getDynAttrSchemas());

        List<Part> filterParts = new ArrayList<>();
        if (attrResult.getFirst().getInstType() == 0) { // 非实例属性
            filterParts = FilterExpressionExecutor.doSelect(new ArrayList<>(category.partMap.values()),
                    attrWhereCondition);
        } else { // 实例属性
            for (Part part : category.partMap.values()) {
                InstanceDynAttrValue instAttrs = part.getInstanceAttrs();
                if (instAttrs != null) {
                    List<InstanceDynAttrValueItem> instValues = FilterExpressionExecutor
                            .doSelect(instAttrs.getInstsValues(), attrWhereCondition);
                    int sumAttrValue = 0;
                    for (InstanceDynAttrValueItem instValue : instValues) {
                        String attrValue = instValue.getInstAttr(attrResult.getFirst().getCode());
                        if (attrValue != null) {
                            try {
                                sumAttrValue += Integer.parseInt(attrValue);
                            } catch (NumberFormatException e) {
                                // 处理非数字类型的情况
                            }
                        }
                    }
                    Part cPart = part.clone();
                    cPart.setAttr(attrResult.getFirst().getCode(), String.valueOf(sumAttrValue));
                    filterParts.add(cPart);
                }
            }
        }
        resultPartCategory.addPart(filterParts);

        List<PartCategory> resultList = new ArrayList<>();
        resultList.add(resultPartCategory);
        return resultList;
    }

    /**
     * 解析属性代码
     *
     * @param orgAttrCode    原始属性代码
     * @param dynAttrSchemas 动态属性schema列表
     * @return 属性和函数的配对
     */
    private Pair<DynamicAttribute, String> parseAttribute(String orgAttrCode, List<DynamicAttribute> dynAttrSchemas) {
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
        // 实现添加子部件分类的逻辑
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
}
