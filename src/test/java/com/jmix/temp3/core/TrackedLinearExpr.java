package com.jmix.temp3.core;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import lombok.Data;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 表达式构建器包装器
 * 用于跟踪构建线性表达式时的所有信息，以便于日志记录和调试
 */
@Data
public class TrackedLinearExpr {
    private LinearExprBuilder builder;
    private List<Map.Entry<String, String>> terms;
    private String name;

    public TrackedLinearExpr(String name) {
        this.builder = LinearExpr.newBuilder();
        this.terms = new ArrayList<>();
        this.name = name;
    }

    /**
     * 添加变量项到表达式
     * 
     * @param var         变量
     * @param coefficient 系数
     * @param description 描述信息
     */
    public void addTerm(IntVar var, long coefficient) {
        builder.addTerm(var, coefficient);

        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), var.getName());
        // if (description != null && !description.isEmpty()) {
        // termStr += " // " + description;
        // }
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
    }

    /**
     * 添加常数项到表达式
     * 
     * @param value       常数值
     * @param description 描述信息
     */
    public void addConstant(long value) {
        // 在 OR-Tools 9.12.4544 中，添加常数可能需要特殊处理
        builder.add(value);
        // 通常我们通过创建一个固定变量来实现
        String sign = value >= 0 ? "+" : "-";
        String termStr = String.format("%s %d", sign, Math.abs(value));
        // if (description != null && !description.isEmpty()) {
        // termStr += " // " + description;
        // }
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
    }

    /**
     * 添加线性表达式项到表达式（带系数）
     * 
     * @param expr        线性表达式
     * @param coefficient 系数
     * @param description 描述信息
     */
    public void addExpr(TrackedLinearExpr expr, long coefficient) {
        builder.addTerm(expr.build(), coefficient);

        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * (%s)", sign, Math.abs(coefficient), expr.toString());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, expr.getName()));
    }

    /**
     * 构建LinearExpr
     * 
     * @return 构建的线性表达式
     */
    public LinearExpr build() {
        return builder.build();
    }

    /**
     * 获取表达式名称
     * 
     * @return 表达式名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取所有项的列表
     *
     * @return 不可修改的项列表
     */
    public List<String> getTerms() {
        List<String> termStrings = new ArrayList<>();
        for (Map.Entry<String, String> term : terms) {
            termStrings.add(term.getKey());
        }
        return Collections.unmodifiableList(termStrings);
    }

    /**
     * 返回表达式的字符串表示
     * 
     * @return 格式化的表达式字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> term : terms) {
            String termStr = term.getKey();
            if (first) {
                // 处理第一个项的符号
                if (termStr.startsWith("+ ")) {
                    sb.append(termStr.substring(2));
                } else if (termStr.startsWith("- ")) {
                    sb.append(termStr);
                } else {
                    sb.append(termStr);
                }
                first = false;
            } else {
                sb.append(" ").append(termStr);
            }
        }

        if (terms.isEmpty()) {
            sb.append("0");
        }
        // sb.append(" : ").append(name);
        return sb.toString();
    }

    /**
     * 返回表达式的字符串表示
     * 
     * @return 格式化的表达式字符串
     */
    public String toDetailString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> term : terms) {
            String termStr = term.getKey();
            String exprName = term.getValue();
            // 打印 term.second（表达式名称）
            if (!exprName.isEmpty()) {
                sb.append("\n");
                sb.append("  ").append(exprName).append(": ");
            }
            if (first) {
                // 处理第一个项的符号
                if (termStr.startsWith("+ ")) {
                    sb.append(termStr.substring(2));
                } else if (termStr.startsWith("- ")) {
                    sb.append(termStr);
                } else {
                    sb.append(termStr);
                }
                first = false;
            } else {
                sb.append(" ").append(termStr);
            }

        }

        if (terms.isEmpty()) {
            sb.append("0");
        }
        // sb.append(" : ").append(name);
        return sb.toString();
    }
}
