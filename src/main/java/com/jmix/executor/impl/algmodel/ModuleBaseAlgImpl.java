package com.jmix.executor.impl.algmodel;

import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.bmodel.Part;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 算法基类
 * 定义模块/部件分类级别算法的公共方法
 * 
 * @since 2025-12-27
 */
public abstract class ModuleBaseAlgImpl {

    protected AlgCPModel model;
    protected Map<String, Var<?>> varMap = new LinkedHashMap<>();

    public ModuleBaseAlgImpl() {
    }

    /**
     * 获取当前层的部件列表（抽象方法，由子类实现）
     */
    protected abstract List<Part> getParts4Sum();

    /**
     * 通用的sum4Parts实现
     */
    protected PartAlgCPLinearExpr sum4Parts(String cofAttrCode,
            Function<PartVar, LinearArgument> varGetter, String varName, String filtedConditionStr) {
        
        PriorityConstraint tempConstraint = new PriorityConstraint();
        List<Part> atomicParts = getParts4Sum();

        if (filtedConditionStr != null && !filtedConditionStr.trim().isEmpty()) {
            atomicParts = FilterExpressionExecutor.doSelect(atomicParts, filtedConditionStr);
        }

        return buildSumExpr(tempConstraint, cofAttrCode, varName, varGetter, atomicParts);
    }

    /**
     * 构建求和表达式
     */
    protected PartAlgCPLinearExpr buildSumExpr(PriorityConstraint tempConstraint, String cofAttrCode,
            String varName, Function<PartVar, LinearArgument> varGetter, List<Part> atomicParts) {
        
        PartAlgCPLinearExpr algExpr = new PartAlgCPLinearExpr(
                "sum_" + (cofAttrCode == null ? "" : cofAttrCode) + "_" + varName);
        
        boolean isWithoutAttr = cofAttrCode == null || cofAttrCode.isEmpty();
        
        for (Part part : atomicParts) {
            PartVar partVar = getPartVar(part.getCode());
            int attrValue;
            if (isWithoutAttr) {
                attrValue = 1;
            } else if (PartConstantAttr.Quantity.getCode().equals(cofAttrCode)) {
                attrValue = 1;
            } else {
                attrValue = Integer.parseInt(part.getAttr(cofAttrCode));
            }
            algExpr.addTerm(partVar, (IntVar) varGetter.apply(partVar), attrValue, varName);
        }

        tempConstraint.setExpr(algExpr);
        return algExpr;
    }

    /**
     * 对选中的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Selected(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts(cofAttrCode, PartVar::getIsSelected, PartVar.ISSELECTED_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对选中的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Selected(String filtedConditionStr) {
        return sum4Selected(null, filtedConditionStr);
    }

    /**
     * 对数量的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts(cofAttrCode, PartVar::getQty, PartVar.QTY_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对数量的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr) {
        return sum4Quantity(null, filtedConditionStr);
    }

    /**
     * 获取部件变量
     * 
     * @param code 部件代码
     * @return 部件变量实例
     * @throws AlgLoaderException 异常
     */
    public PartVar getPartVar(String code) {
        Var<?> var = varMap.get(code);
        if (var == null || !(var instanceof PartVar)) {
            throw new AlgLoaderException("PartVar not found: " + code);
        }
        return (PartVar) var;
    }
}
