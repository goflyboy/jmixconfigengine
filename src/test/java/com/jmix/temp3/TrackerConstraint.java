package com.jmix.temp3;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;

/**
 * 约束的跟踪器，对Constraint进行包装
 * 用于记录约束的详细信息，便于日志输出和调试
 */
public class TrackerConstraint {
    private Constraint ct;
    private String name; // 如：addGreaterOrEqual
    private String left; // 左表达式的str
    private String operator = ""; // 操作符
    private String right = ""; // 右表达式的str
    private String ifMemo = ""; // 条件信息

    /**
     * 构建TrackerConstraint的静态方法
     * 
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintType 约束类型
     * @param operator       操作符
     * @return 包装的约束对象
     */
    public static TrackerConstraint build(Constraint constraint,
            LinearArgument leftExpr, long rightValue,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = toNameString(leftExpr);
        tct.operator = operator;
        tct.right = String.valueOf(rightValue);
        return tct;
    }

    /**
     * 构建TrackerConstraint的静态方法
     * 
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintType 约束类型
     * @param operator       操作符
     * @return 包装的约束对象
     */
    public static TrackerConstraint build(Constraint constraint, LinearExpr leftExpr, long rightValue,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = leftExpr.toString();
        tct.operator = operator;
        tct.right = String.valueOf(rightValue);
        return tct;
    }

    private static String toNameString(LinearArgument expr) {
        if (expr instanceof IntVar) {
            return ((IntVar) expr).getName();
        } else if (expr instanceof BoolVar) {
            return ((BoolVar) expr).getName();
        } else {
            throw new IllegalArgumentException("Unsupported linear argument type: " + expr.getClass().getName());
        }
    }

    /**
     * 构建TrackerConstraint的静态方法（表达式比较）
     * 
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightExpr      右侧表达式
     * @param constraintType 约束类型
     * @param operator       操作符
     * @return 包装的约束对象
     */
    public static TrackerConstraint build(Constraint constraint, LinearExpr leftExpr, LinearExpr rightExpr,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = leftExpr.toString();
        tct.operator = operator;
        tct.right = rightExpr.toString();
        return tct;
    }

    /**
     * 获取原始约束对象
     * 
     * @return 原始Constraint对象
     */
    public Constraint getConstraint() {
        return ct;
    }

    /**
     * 添加文字到约束（条件强制）
     * 
     * @param lit 文字对象
     */
    public void onlyEnforceIf(Literal lit) {
        ifMemo += " if " + toNameString(lit);
        ct.onlyEnforceIf(lit);
    }

    /**
     * 添加多个文字到约束
     * 
     * @param lits 文字数组
     */
    public void onlyEnforceIf(Literal[] lits) {
        for (Literal lit : lits) {
            ifMemo += " if " + toNameString(lit);
        }
        ct.onlyEnforceIf(lits);
    }

    /**
     * 获取约束在模型中的索引
     * 
     * @return 约束索引
     */
    public int getIndex() {
        return ct.getIndex();
    }

    /**
     * 获取约束构建器
     * 
     * @return 约束构建器
     */
    public ConstraintProto.Builder getBuilder() {
        return ct.getBuilder();
    }

    /**
     * 将文字转换为名称字符串
     * 
     * @param lit 文字对象
     * @return 名称字符串
     */
    private String toNameString(Literal lit) {
        if (lit instanceof BoolVar) {
            return ((BoolVar) lit).getName();
        } else if (lit instanceof IntVar) {
            return ((IntVar) lit).getName();
        } else {
            return lit.toString();
        }
    }

    /**
     * 返回约束的字符串表示
     * 
     * @return 格式化的约束字符串
     */
    @Override
    public String toString() {
        return left + " " + operator + " " + right + ifMemo + " (" + name + ")";
    }

    /**
     * 获取约束名称
     * 
     * @return 约束名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取左侧表达式字符串
     * 
     * @return 左侧表达式
     */
    public String getLeft() {
        return left;
    }

    /**
     * 获取操作符
     * 
     * @return 操作符
     */
    public String getOperator() {
        return operator;
    }

    /**
     * 获取右侧表达式字符串
     * 
     * @return 右侧表达式
     */
    public String getRight() {
        return right;
    }

    /**
     * 获取条件信息
     * 
     * @return 条件信息
     */
    public String getIfMemo() {
        return ifMemo;
    }
}
