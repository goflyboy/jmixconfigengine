package com.jmix.temp3.core;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.NotBoolVar;

import lombok.Data;

/**
 * 由于TrackerConstraint在同一个包中，TrackedLinearExpr不需要显式导入
 */

/**
 * 约束的跟踪器，对Constraint进行包装
 * 用于记录约束的详细信息，便于日志输出和调试
 */
@Data
public class TrackerConstraint {
    private Constraint ct;
    private String name; // 如：addGreaterOrEqual
    private String left; // 左表达式的str
    private String operator = ""; // 操作符
    private String right = ""; // 右表达式的str
    private String ifMemo = ""; // 条件信息
    private String leftName = "";
    private String rightName = "";

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
    public static TrackerConstraint build(Constraint constraint, TrackedLinearExpr leftExpr, long rightValue,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = leftExpr.toString();
        tct.leftName = leftExpr.getName();
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
    public static TrackerConstraint build(Constraint constraint,
            LinearArgument leftExpr,
            TrackedLinearExpr rightValue,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = toNameString(leftExpr);
        tct.leftName = toNameString(leftExpr);
        tct.operator = operator;
        tct.right = rightValue.toString();
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
    public static TrackerConstraint build(Constraint constraint, TrackedLinearExpr leftExpr, String constraintType) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = leftExpr.toString();
        tct.leftName = leftExpr.getName();
        return tct;
    }

    // /**
    // * 构建TrackerConstraint的静态方法
    // *
    // * @param constraint 原始约束对象
    // * @param leftExpr 左侧表达式
    // * @param rightValue 右侧值
    // * @param constraintType 约束类型
    // * @param operator 操作符
    // * @return 包装的约束对象
    // */
    // public static TrackerConstraint build(Constraint constraint,
    // TrackedLinearExpr leftExpr, long rightValue,
    // String constraintType, String operator) {
    // TrackerConstraint tct = new TrackerConstraint();
    // tct.ct = constraint;
    // tct.name = constraintType;
    // tct.left = leftExpr.toString();
    // tct.leftName = leftExpr.g
    // tct.operator = operator;
    // tct.right = String.valueOf(rightValue);
    // return tct;
    // }

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
    public static TrackerConstraint build(Constraint constraint,
            LinearArgument leftExpr,
            LinearArgument rightExpr,
            String constraintType, String operator) {
        TrackerConstraint tct = new TrackerConstraint();
        tct.ct = constraint;
        tct.name = constraintType;
        tct.left = toNameString(leftExpr);
        tct.operator = operator;
        tct.right = toNameString(rightExpr);
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
        }
        if (lit instanceof IntVar) {
            return ((IntVar) lit).getName();
        }
        if (lit instanceof NotBoolVar) {
            String str = lit.toString();
            // str = not(sd1.S(0..1)) --> extract "sd1.S"
            // Parse the string: "not(varName(range))" -> "varName"
            if (str.startsWith("not(") && str.endsWith(")")) {
                String inner = str.substring(4, str.length() - 1); // Remove "not(" and ")"
                int parenIndex = inner.indexOf('(');
                if (parenIndex > 0) {
                    return "!" + inner.substring(0, parenIndex); // Extract variable name before "("
                }
            }
        }
        // Handle other unsupported Literal types
        throw new UnsupportedOperationException(
                "Unsupported Literal type: " + lit.getClass().getSimpleName() +
                        ". Only BoolVar, IntVar, and NotBoolVar are supported. Literal: " + lit);

    }

    /**
     * 返回约束的字符串表示
     * 
     * @return 格式化的约束字符串
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(left).append(" ").append(operator).append(" ").append(right).append(ifMemo).append(" (").append(name)
                .append(")");
        if (!leftName.isEmpty()) {
            sb.append(" L:").append(leftName);
        }
        if (!rightName.isEmpty()) {
            sb.append(" R:").append(rightName);
        }
        return sb.toString();
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
