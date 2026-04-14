package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValue;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValueItem;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.impl.util.FilterExpressionExecutor.FilterCondition;
import com.jmix.executor.model.AttrFunConstant;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.tool.bbuilder.MultiInstCategoryUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部件分类
 * 表示部件的分类定义，继承自Part
 *
 * @since 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class PartCategory extends ModuleBase implements IModule, IPart {
    /**
     * 部件类型
     */
    private PartType partType = PartType.ATOMIC;

    /**
     * 是否支持多实例
     * 用于约束算法的多实例处理，默认为false
     * 当设置为true时，该部件分类将支持多实例复制
     */
    private boolean supportMultiInst = false;

    public PartCategory() {
        super();
        this.setPartType(PartType.CATEGORY);
    }

    /**
     * 是否是请求多实例
     * 
     * @return 是否是请求多实例
     */
    public boolean isEnumMutiInst() {
        return MultiInstCategoryUtils.isMultiInstCategory(this);
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getEnumInstId() {
        return MultiInstCategoryUtils.getEnumInstId(this.getCode());
    }

    /**
     * 克隆PartCategory对象
     *
     * @return 克隆的PartCategory对象
     */
    @JsonIgnore
    public PartCategory clone() {
        PartCategory to = new PartCategory();
        // 调用父类的clone方法
        super.clone(to);
        // 复制PartCategory特有的属性
        to.setPartType(this.getPartType());
        to.setSupportMultiInst(this.isSupportMultiInst());
        // 调用init方法初始化映射表
        to.init();
        return to;
    }

    @Override
    public PartCategory getPartCategory(String categoryCode) {
        if (this.getCode().equals(categoryCode)) {
            return this;
        }
        return super.getPartCategory(categoryCode);
    }

    /**
     * 查询满足条件的Part
     *
     * @param constraintReq 约束请求
     * @return 满足条件的PartCategory列表
     */
    public PartCategory filterClone(PartConstraintReq constraintReq) {
        return filterClone(this, constraintReq, false);
    }

    private PartCategory filterClone(PartCategory category, PartConstraintReq constraintReq,
            boolean hasMatchedPartCategoryOfReq) {
        PartCategory resultPartCategory = category.clone();
        if (!hasMatchedPartCategoryOfReq) {
            if (constraintReq.getPartCategoryCode().equals(category.getCode())) {
                hasMatchedPartCategoryOfReq = true;
            }
        }
        if (hasMatchedPartCategoryOfReq && CollectionUtils.isEmpty(category.getPartCategorys())) {
            // 先匹配当前原子part
            List<Part> filterParts = querySubAtomicParts(category, constraintReq);
            resultPartCategory.addAtomicPartsWithoutStructure(filterParts);
        }
        // 在去自己的子分类中匹配
        for (PartCategory pc : category.getPartCategorys()) {
            resultPartCategory.addPart(filterClone(pc, constraintReq, hasMatchedPartCategoryOfReq));
        }
        return resultPartCategory;
    }

    private List<Part> querySubAtomicParts(PartCategory category, PartConstraintReq constraintReq) {
        if (Strings.isNullOrEmpty(constraintReq.getAttrWhereCondition())) {
            return new ArrayList<>(category.getAtomicParts());
        }
        // 解析属性代码
        Pair<DynamicAttribute, String> attrResult = parseAttributeFromCondition(constraintReq.getAttrWhereCondition(),
                category.getDynAttrSchemas());

        List<Part> filterParts = new ArrayList<>();
        if (attrResult.getFirst().getInstType() == 0) { // 非实例属性
            // 过滤条件不涉及实例属性，直接在Part级别过滤
            filterParts = FilterExpressionExecutor.doSelect(new ArrayList<>(category.getAtomicParts()),
                    constraintReq.getAttrWhereCondition());

        } else { // 实例属性
            for (Part part : category.getAtomicParts()) {
                InstanceDynAttrValue instAttrs = part.getInstanceAttrs();
                if (instAttrs == null) {
                    throw new IllegalStateException("Instance attributes not found for part: " + part.getCode());
                }
                List<InstanceDynAttrValueItem> instValues = FilterExpressionExecutor
                        .doSelect(instAttrs.getInstsValues(), constraintReq.getAttrWhereCondition());
                if (instValues.isEmpty()) {
                    continue;
                }
                Part cPart = part.clone(); // 带有实例属性，通过clone的方法，把属性的多实例值进行汇总，存储起来，方便后续调用。
                List<DynamicAttribute> instAttrsList = category.queryDynAttrSchemas4Inst();
                for (DynamicAttribute instAttr : instAttrsList) {
                    String sumAttrValue = null;
                    switch (instAttr.getDynAttrType().getBaseType()) {
                        case INT:
                            sumAttrValue = sumInstanceDynAttrValue4Int(part, instValues, instAttr.getCode());
                            cPart.setAttr(instAttr.getCode(), sumAttrValue);
                            break;
                        case FLOAT:
                            sumAttrValue = sumInstanceDynAttrValue4Float(part, instValues, instAttr.getCode());
                            cPart.setAttr(instAttr.getCode(), sumAttrValue);
                            break;
                        case DOUBLE:
                            sumAttrValue = sumInstanceDynAttrValue4Double(part, instValues, instAttr.getCode());
                            cPart.setAttr(instAttr.getCode(), sumAttrValue);
                            break;
                        default:
                            log.warn("Unsupported base type for aggregation: {} in part: {}",
                                    instAttr.getDynAttrType().getBaseType(), part.getCode());
                            break;
                    }
                }
                filterParts.add(cPart);
            }
        }
        return filterParts;
    }

    // /**
    // * 根据代码查找PartCategory（可能是this，也可能是子PartCategory）
    // *
    // * @param category 当前PartCategory
    // * @param code 要查找的代码
    // * @return 找到的PartCategory，如果未找到则返回null
    // */
    // @JsonIgnore
    // private PartCategory getPartCategoryByCode(PartCategory category, String
    // code) {
    // if (code == null || code.trim().isEmpty()) {
    // log.error("PartCategory code is null or empty");
    // return null;
    // }
    // // 如果是当前PartCategory的code，直接返回
    // if (category.getCode().equals(code)) {
    // return category;
    // }
    // // 在子PartCategory中查找
    // if (category.partCategoryMap != null && !category.partCategoryMap.isEmpty())
    // {
    // // 先在直接子分类中查找
    // PartCategory found = category.partCategoryMap.get(code);
    // if (found != null) {
    // return found;
    // }
    // // 递归在子分类中查找
    // for (PartCategory subCategory : category.partCategoryMap.values()) {
    // PartCategory result = getPartCategoryByCode(subCategory, code);
    // if (result != null) {
    // return result;
    // }
    // }
    // }
    // return null;
    // }

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
        // 解析逻辑，例如："Sum_Capacity" -> DynamicAttribute{Capacity}, "Sum"
        String funPrefix = "";
        String attrCode = orgAttrCode;

        if (orgAttrCode.contains(AttrPara.CODE_SEPARATOR)) {
            String[] parts = orgAttrCode.split(AttrPara.CODE_SEPARATOR, 2);
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
     * 添加子部件列表
     *
     * @param subParts 子部件列表（可以是Part或PartCategory）
     */
    public void addSubParts(List<? extends IPart> subParts) {
        super.addParts(subParts);
    }

    /**
     * 计算未过滤的叶子部件
     * 遍历 allPartCategory 本身的 parts 及其子 PartCategory 的 parts，
     * 如果不在 filterLeafParts 中，添加到结果列表
     *
     * @param allPartCategory 所有部件分类
     * @param filterLeafParts 已过滤的叶子部件列表
     * @return 未过滤的叶子部件列表
     */
    @JsonIgnore
    public static List<Part> calcUnFilterLeafParts(PartCategory allPartCategory, List<Part> filterLeafParts) {
        List<Part> unFilterLeafParts = new ArrayList<>();
        List<Part> allLeafParts = allPartCategory.getAtomicParts();

        // 使用 Set 来快速查找，提高性能
        Set<String> filterPartCodes = filterLeafParts.stream()
                .map(Part::getCode)
                .collect(Collectors.toSet());

        // 遍历所有叶子部件，如果不在 filterLeafParts 中，添加到 unFilterLeafParts
        for (Part part : allLeafParts) {
            if (!filterPartCodes.contains(part.getCode())) {
                unFilterLeafParts.add(part);
            }
        }

        return unFilterLeafParts;
    }

    /**
     * 对实例属性值进行整数类型求和
     *
     * @param part       部件对象
     * @param instValues 实例属性值列表
     * @param attrCode   属性代码
     * @return 求和后的字符串值
     */
    @JsonIgnore
    private String sumInstanceDynAttrValue4Int(Part part, List<InstanceDynAttrValueItem> instValues,
            String attrCode) {
        int sumAttrValue = 0;
        for (InstanceDynAttrValueItem instValue : instValues) {
            String attrValue = instValue.getInstAttr(attrCode);
            if (attrValue == null) {
                throw new IllegalArgumentException(
                        "Attribute '" + attrCode + "' value is null for part '" + part.getCode() + "'");
            }
            try {
                sumAttrValue += Integer.parseInt(attrValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid number format for attribute '" + attrCode + "' with value '" + attrValue
                                + "' in part '" + part.getCode() + "'",
                        e);
            }
        }
        return String.valueOf(sumAttrValue);
    }

    /**
     * 对实例属性值进行浮点数类型求和
     *
     * @param part       部件对象
     * @param instValues 实例属性值列表
     * @param attrCode   属性代码
     * @return 求和后的字符串值
     */
    @JsonIgnore
    private String sumInstanceDynAttrValue4Float(Part part, List<InstanceDynAttrValueItem> instValues,
            String attrCode) {
        float sumAttrValue = 0.0f;
        for (InstanceDynAttrValueItem instValue : instValues) {
            String attrValue = instValue.getInstAttr(attrCode);
            if (attrValue == null) {
                throw new IllegalArgumentException(
                        "Attribute '" + attrCode + "' value is null for part '" + part.getCode() + "'");
            }
            try {
                sumAttrValue += Float.parseFloat(attrValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid number format for attribute '" + attrCode + "' with value '" + attrValue
                                + "' in part '" + part.getCode() + "'",
                        e);
            }
        }
        return String.valueOf(sumAttrValue);
    }

    /**
     * 对实例属性值进行双精度浮点数类型求和
     *
     * @param part       部件对象
     * @param instValues 实例属性值列表
     * @param attrCode   属性代码
     * @return 求和后的字符串值
     */
    @JsonIgnore
    private String sumInstanceDynAttrValue4Double(Part part, List<InstanceDynAttrValueItem> instValues,
            String attrCode) {
        double sumAttrValue = 0.0;
        for (InstanceDynAttrValueItem instValue : instValues) {
            String attrValue = instValue.getInstAttr(attrCode);
            if (attrValue == null) {
                throw new IllegalArgumentException(
                        "Attribute '" + attrCode + "' value is null for part '" + part.getCode() + "'");
            }
            try {
                sumAttrValue += Double.parseDouble(attrValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid number format for attribute '" + attrCode + "' with value '" + attrValue
                                + "' in part '" + part.getCode() + "'",
                        e);
            }
        }
        return String.valueOf(sumAttrValue);
    }

    /**
     * 获取原子部件的短字符串表示
     * 格式：Size:xx Codes: code1,code2,....（最多MAX_DISPLAY_CODES个代码）
     * 
     * @return 原子部件的短字符串表示
     */
    @JsonIgnore
    public String getAtomicPartShortString() {
        List<Part> atomicParts = getAtomicParts();
        return PartUtils.toShortString(atomicParts);
    }

    /**
     * 获取原子部件的短字符串表示
     * 格式：Size:xx Codes: code1,code2,....（最多MAX_DISPLAY_CODES个代码）
     * 
     * @return 原子部件的短字符串表示
     */
    @JsonIgnore
    public String getAllAtomicPartShortString() {
        List<Part> atomicParts = getAllAtomicParts();
        return PartUtils.toShortString(atomicParts);
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
        for (Rule rule : super.getRules()) {
            if (RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName())) {
                priorityRules.add(rule);
            }
        }
        return priorityRules;
    }

    /**
     * 获取指定计算阶段的所有规则列表（包括子分类中的规则）
     * 递归收集所有子分类中的规则
     * 
     * @param calcStage 计算阶段
     * @return 指定计算阶段的所有规则列表
     */
    @Override
    @JsonIgnore
    public List<Rule> getAllRules(CalcStage calcStage) {
        if (calcStage == null) {
            return new ArrayList<>();
        }
        List<Rule> allRules = new ArrayList<>();
        // 添加当前分类的规则
        List<Rule> rules = getRules(calcStage);
        if (rules != null) {
            allRules.addAll(rules);
        }
        // 递归收集所有子分类中的规则
        for (PartCategory subCategory : partCategorys) {
            allRules.addAll(subCategory.getAllRules(calcStage));
        }
        return allRules;
    }
}
