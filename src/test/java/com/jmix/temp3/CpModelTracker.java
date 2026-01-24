package com.jmix.temp3;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
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
     * 创建并记录约束的私有辅助方法（TrackedLinearExpr约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private TrackerConstraint createAndTrackConstraint(Constraint constraint, TrackedLinearExpr leftExpr,
            long rightValue,
            String constraintName, String operator) {
        TrackerConstraint tct = TrackerConstraint.build(constraint, leftExpr, rightValue, constraintName, operator);
        constraints.add(tct);
        return tct;
    }

    /**
     * 创建并记录约束的私有辅助方法（LinearArgument约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private TrackerConstraint createAndTrackConstraint(Constraint constraint, LinearArgument leftExpr, long rightValue,
            String constraintName, String operator) {
        TrackerConstraint tct = TrackerConstraint.build(constraint, leftExpr, rightValue, constraintName, operator);
        constraints.add(tct);
        return tct;
    }

    /**
     * 创建并记录约束的私有辅助方法（TrackedLinearExpr表达式约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightExpr      右侧表达式
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private TrackerConstraint createAndTrackConstraint(Constraint constraint, TrackedLinearExpr leftExpr,
            TrackedLinearExpr rightExpr,
            String constraintName, String operator) {
        TrackerConstraint tct = TrackerConstraint.build(constraint, leftExpr.build(), rightExpr.build(), constraintName,
                operator);
        constraints.add(tct);
        return tct;
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
        return createAndTrackConstraint(ct, expr, value, "addGreaterOrEqual", ">=");
    }

    /**
     * 添加大于等于约束（接受IntVar）
     *
     * @param var   变量
     * @param value 值
     * @return 包装的约束对象
     */
    public TrackerConstraint addGreaterOrEqual(IntVar var, long value) {
        Constraint ct = model.addGreaterOrEqual(var, value);
        return createAndTrackConstraint(ct, var, value, "addGreaterOrEqual", ">=");
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
        return createAndTrackConstraint(ct, (LinearExpr) expr, value, "addGreaterOrEqual", ">=");
    }

    /**
     * 添加小于等于约束（接受IntVar）
     *
     * @param var   变量
     * @param value 值
     * @return 包装的约束对象
     */
    public TrackerConstraint addLessOrEqual(IntVar var, long value) {
        Constraint ct = model.addLessOrEqual(var, value);
        return createAndTrackConstraint(ct, var, value, "addLessOrEqual", "<=");
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
        return createAndTrackConstraint(ct, (LinearExpr) expr, value, "addLessOrEqual", "<=");
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
        return createAndTrackConstraint(ct, expr, value, "addLessOrEqual", "<=");
    }

    /**
     * 添加相等约束（接受IntVar）
     *
     * @param var   变量
     * @param value 值
     * @return 包装的约束对象
     */
    public TrackerConstraint addEquality(IntVar var, long value) {
        Constraint ct = model.addEquality(var, value);
        return createAndTrackConstraint(ct, var, value, "addEquality", "==");
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
        return createAndTrackConstraint(ct, (LinearExpr) expr, value, "addEquality", "==");
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
        return createAndTrackConstraint(ct, expr, value, "addEquality", "==");
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
        return createAndTrackConstraint(ct, left, right, "addLessThan", "<");
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
        TrackerConstraint tct = TrackerConstraint.build(ct,
                condition,
                implication,
                "addImplication", "->");
        constraints.add(tct);
        return tct;
    }

    public void minimize(TrackedLinearExpr expr) {
        model.minimize(expr.build());
        TrackerConstraint tct = TrackerConstraint.build(null,
                expr,
                "minimize");
        constraints.add(tct);
    }

    public void maximize(TrackedLinearExpr expr) {
        model.maximize(expr.build());
        TrackerConstraint tct = TrackerConstraint.build(null,
                expr,
                "maximize");
        constraints.add(tct);
    }

    /**
     * 设置目标函数（最小化或最大化）
     *
     * @param expr        目标函数表达式
     * @param minimize    是否最小化（true为最小化，false为最大化）
     * @param description 目标函数描述
     */
    public void setObjective(TrackedLinearExpr expr, boolean minimize, String description) {
        if (minimize) {
            model.minimize(expr.build());
        } else {
            model.maximize(expr.build());
        }
        // 这里可以记录目标函数信息，但暂时不添加到constraints列表
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
        System.out.println("\n" + "=".repeat(80));
    }

    public void printRunValue(CpSolver cpSolver) {
        System.out.println("\nVARIABLE VALUES (" + variables.size() + "):");
        System.out.println("-".repeat(80));
        for (IntVar var : variables.values()) {
            long value = cpSolver.value(var);
            System.out.printf("  %-20s = %d%n", var.getName(), value);
        }
        System.out.println("-".repeat(80));
    }
}
