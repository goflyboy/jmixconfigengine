package com.jmix.temp3;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型跟踪器（核心解决方案）
 * 包装CpModel，提供变量、约束、目标函数的跟踪功能
 */
public class CpModelTracker {
    private CpModel model;
    private Map<String, IntVar> variables;
    private List<String> variableLogs;
    private List<TrackerConstraint> constraints;
    private TrackedLinearExpr objectiveExpr;
    private boolean isMaximize;
    private String objectiveDescription;

    public CpModelTracker() {
        this.model = new CpModel();
        this.variables = new HashMap<>();
        this.variableLogs = new ArrayList<>();
        this.constraints = new ArrayList<>();
    }

    /**
     * 获取原始CpModel对象
     * 
     * @return CpModel对象
     */
    public CpModel getModel() {
        return model;
    }

    /**
     * 创建变量并记录
     *
     * @param name        变量名
     * @param lb          下界
     * @param ub          上界
     * @param description 描述信息
     * @return 创建的IntVar变量
     */
    public IntVar newIntVar(long lb, long ub, String name) {
        IntVar var = model.newIntVar(lb, ub, name);
        variables.put(name, var);
        variableLogs.add(String.format("%s ∈ [%d, %d]  ",
                name, lb, ub));
        return var;
    }

    /**
     * 创建布尔变量并记录
     *
     * @param name 变量名
     * @return 创建的IntVar变量
     */
    public BoolVar newBoolVar(String name) {
        BoolVar var = model.newBoolVar(name);
        variables.put(name, var);
        variableLogs.add(String.format("%s ∈ {0, 1}",
                name));
        return var;
    }

    /**
     * 添加大于等于约束并记录
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addGreaterOrEqual(TrackedLinearExpr expr, long value) {
        Constraint ct = model.addGreaterOrEqual(expr.build(), value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr.build(), value, "addGreaterOrEqual", ">=");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加大于等于约束（直接使用LinearExpr）
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addGreaterOrEqual(LinearArgument expr, long value) {
        Constraint ct = model.addGreaterOrEqual(expr, value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr, value, "addGreaterOrEqual", ">=");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加小于等于约束
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addLessOrEqual(LinearArgument expr, long value) {
        Constraint ct = model.addLessOrEqual(expr, value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr, value, "addLessOrEqual", "<=");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加小于等于约束
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addLessOrEqual(TrackedLinearExpr expr, long value) {
        Constraint ct = model.addLessOrEqual(expr.build(), value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr, value, "addLessOrEqual", "<=");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加相等约束
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addEquality(LinearArgument expr, long value) {
        Constraint ct = model.addEquality(expr, value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr, value, "addEquality", "==");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加相等约束
     * 
     * @param expr  左侧表达式
     * @param value 右侧值
     * @return 包装的约束对象
     */
    public TrackerConstraint addEquality(TrackedLinearExpr expr, long value) {
        Constraint ct = model.addEquality(expr.build(), value);
        TrackerConstraint tct = TrackerConstraint.build(ct, expr, value, "addEquality", "==");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加小于约束
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 包装的约束对象
     */
    public TrackerConstraint addLessThan(TrackedLinearExpr left, long right) {
        Constraint ct = model.addLessThan(left.build(), right);
        TrackerConstraint tct = TrackerConstraint.build(ct, left.build(), right, "addLessThan", "<");
        constraints.add(tct);
        return tct;
    }

    /**
     * 添加蕴含约束
     * 
     * @param condition   条件
     * @param implication 蕴含
     * @return 包装的约束对象
     */
    public TrackerConstraint addImplication(BoolVar condition, BoolVar implication) {
        Constraint ct = model.addImplication(condition, implication);
        TrackerConstraint tct = TrackerConstraint.build(ct, LinearExpr.constant(0), LinearExpr.constant(0),
                "addImplication", "->");
        constraints.add(tct);
        return tct;
    }

    /**
     * 获取变量
     * 
     * @param name 变量名
     * @return 变量对象
     */
    public IntVar getVariable(String name) {
        return variables.get(name);
    }

    /**
     * 获取所有变量
     * 
     * @return 变量集合
     */
    public Collection<IntVar> getAllVariables() {
        return variables.values();
    }

    /**
     * 创建跟踪的表达式
     * 
     * @param name 表达式名称
     * @return 跟踪表达式对象
     */
    public TrackedLinearExpr newTrackedExpr(String name) {
        return new TrackedLinearExpr(name);
    }

    /**
     * 设置目标函数并记录
     * 
     * @param expr        目标函数表达式
     * @param maximize    是否最大化
     * @param description 描述信息
     */
    public void setObjective(TrackedLinearExpr expr, boolean maximize, String description) {
        if (maximize) {
            model.maximize(expr.build());
        } else {
            model.minimize(expr.build());
        }

        this.objectiveExpr = expr;
        this.isMaximize = maximize;
        this.objectiveDescription = description;
    }

    /**
     * 直接设置目标函数（简单方式）
     * 
     * @param var         变量
     * @param coefficient 系数
     * @param maximize    是否最大化
     * @param description 描述信息
     */
    public void setObjectiveDirect(IntVar var, long coefficient, boolean maximize) {
        TrackedLinearExpr expr = newTrackedExpr("Objective");
        expr.addTerm(var, coefficient);
        setObjective(expr, maximize, "Objective");
    }

    /**
     * 获取目标函数表达式
     * 
     * @return 目标函数表达式
     */
    public TrackedLinearExpr getObjectiveExpr() {
        return objectiveExpr;
    }

    /**
     * 是否为最大化问题
     * 
     * @return true为最大化，false为最小化
     */
    public boolean isMaximize() {
        return isMaximize;
    }

    /**
     * 打印模型摘要
     */
    public void printModelSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CP-SAT MODEL SUMMARY");
        System.out.println("=".repeat(80));

        System.out.println("\nVARIABLES (" + variables.size() + "):");
        System.out.println("-".repeat(80));
        for (int i = 0; i < variableLogs.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, variableLogs.get(i));
        }

        System.out.println("\nCONSTRAINTS (" + constraints.size() + "):");
        System.out.println("-".repeat(80));
        for (int i = 0; i < constraints.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, constraints.get(i).toString());
        }

        System.out.println("\nOBJECTIVE:");
        System.out.println("-".repeat(80));
        if (objectiveExpr != null) {
            System.out.println("Type: " + (isMaximize ? "MAXIMIZE" : "MINIMIZE"));
            System.out.println("Description: " + objectiveDescription);
            System.out.println("\nExpression:");
            System.out.println(objectiveExpr);

            // 打印详细项
            System.out.println("\nDetailed terms:");
            List<String> terms = objectiveExpr.getTerms();
            for (int i = 0; i < terms.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, terms.get(i));
            }
        } else {
            System.out.println("No objective (satisfaction problem)");
        }

        System.out.println("\n" + "=".repeat(80));
    }
}
