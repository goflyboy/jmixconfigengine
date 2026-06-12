package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * 兼容性约束算法实现类
 * 处理部件之间的不兼容、依赖等约束关系
 * 
 * @since 2025-09-22
 */
@Slf4j
public class CompatibleConstraintAlg {

    /**
     * CP约束求解模型实例
     */
    private AlgCPModel model;

    public CompatibleConstraintAlg(AlgCPModel model) {
        this.model = model;
    }

    /**
     * 添加不兼容性约束（部件级别）
     * 
     * @param ruleCode       规则代码
     * @param leftPartsExpr  左侧部件表达式
     * @param rightPartsExpr 右侧部件表达式
     */
    public void addCompatibleConstraintInCompatible(String ruleCode, PartsExpr leftPartsExpr,
            PartsExpr rightPartsExpr) {
        // left：确保只有一个部件被选中
        addExactlyOneConstraint(leftPartsExpr.getPartVars());
        // right：确保只有一个部件被选中
        addExactlyOneConstraint(rightPartsExpr.getPartVars());
        // 获取左侧和右侧的部件变量
        List<PartVarImpl> leftFilterParts = leftPartsExpr.getFilterPartVars();
        List<PartVarImpl> rightFilterParts = rightPartsExpr.getFilterPartVars();
        List<PartVarImpl> leftNoFilterParts = leftPartsExpr.getNoFilterPartVars();
        List<PartVarImpl> rightNoFilterParts = rightPartsExpr.getNoFilterPartVars();

        // 定义左侧条件：左侧过滤集合中至少一个被选中
        BoolVar leftCond = createSelectedCondition(ruleCode, leftFilterParts, "leftCond");

        // 定义右侧条件：右侧过滤集合中至少一个被选中
        BoolVar rightCond = createSelectedCondition(ruleCode, rightFilterParts, "rightCond");

        // 定义左侧非条件：左侧过滤集合外至少一个被选中
        BoolVar leftNotCond = createSelectedCondition(ruleCode, leftNoFilterParts, "leftNotCond");

        // 定义右侧非条件：右侧过滤集合外至少一个被选中
        BoolVar rightNotCond = createSelectedCondition(ruleCode, rightNoFilterParts, "rightNotCond");

        // 实现Incompatible双向关系：
        // 正向1.1：如果左侧条件为true，则右侧条件必须为false（右侧非条件必须为true）
        model.addImplication(leftCond, rightNotCond);

        // 反向2.1：如果右侧条件为true，则左侧条件必须为false（左侧非条件必须为true）
        model.addImplication(rightCond, leftNotCond);

        log.info("Added inCompatible constraint: {} - left filter: {}, right filter: {}",
                ruleCode, leftFilterParts.size(), rightFilterParts.size());
    }

    private void addExactlyOneConstraint(List<PartVarImpl> partVars) {
        model.addHardExactlyOne(partVars.stream()
                .map(partVar -> partVar.getIsSelected())
                .toArray(Literal[]::new));
    }

    /**
     * 创建"集合中至少一个被选中"的条件变量和约束
     * 
     * @param ruleCode        规则代码
     * @param partVars        部件变量列表
     * @param conditionSuffix 条件后缀
     * @return 条件变量
     */
    private BoolVar createSelectedCondition(String ruleCode, List<PartVarImpl> partVars, String conditionSuffix) {

        // 定义条件变量
        BoolVar condition = model.newBoolVar(ruleCode + "_" + conditionSuffix);
        if (partVars == null || partVars.isEmpty()) {
            model.addEquality(condition, 0);
            return condition;
        }

        // 获取选中变量数组
        Literal[] selected = partVars.stream()
                .map(partVar -> partVar.getIsSelected())
                .toArray(Literal[]::new);

        // 当条件为true时，集合中至少一个被选中；当为false时，集合全部不被选中
        model.addBoolOr(selected).onlyEnforceIf(condition);
        model.addBoolAnd(Arrays.stream(selected).map(Literal::not).toArray(Literal[]::new))
                .onlyEnforceIf(condition.not());

        return condition;
    }
}
