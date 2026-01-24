package com.jmix.temp3;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import java.util.*;

/**
 * 表达式构建器包装器
 * 用于跟踪构建线性表达式时的所有信息，以便于日志记录和调试
 */
public class TrackedLinearExpr {
    private LinearExprBuilder builder;
    private List<String> terms;
    private String name;

    public TrackedLinearExpr(String name) {
        this.builder = LinearExpr.newBuilder();
        this.terms = new ArrayList<>();
        this.name = name;
    }

    /**
     * 添加变量项到表达式
     * @param var 变量
     * @param coefficient 系数
     * @param description 描述信息
     */
    public void addTerm(IntVar var, long coefficient, String description) {
        builder.addTerm(var, coefficient);

        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), var.getName());
        if (description != null && !description.isEmpty()) {
            termStr += "  // " + description;
        }
        terms.add(termStr);
    }

    /**
     * 添加常数项到表达式
     * @param value 常数值
     * @param description 描述信息
     */
    public void addConstant(long value, String description) {
        // 在 OR-Tools 9.12.4544 中，添加常数可能需要特殊处理
        // 通常我们通过创建一个固定变量来实现
        String sign = value >= 0 ? "+" : "-";
        String termStr = String.format("%s %d", sign, Math.abs(value));
        if (description != null && !description.isEmpty()) {
            termStr += "  // " + description;
        }
        terms.add(termStr);
    }

    /**
     * 构建LinearExpr
     * @return 构建的线性表达式
     */
    public LinearExpr build() {
        return builder.build();
    }

    /**
     * 获取表达式名称
     * @return 表达式名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取所有项的列表
     * @return 不可修改的项列表
     */
    public List<String> getTerms() {
        return Collections.unmodifiableList(terms);
    }

    /**
     * 返回表达式的字符串表示
     * @return 格式化的表达式字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");

        boolean first = true;
        for (String term : terms) {
            if (first) {
                // 处理第一个项的符号
                if (term.startsWith("+ ")) {
                    sb.append(term.substring(2));
                } else if (term.startsWith("- ")) {
                    sb.append(term);
                } else {
                    sb.append(term);
                }
                first = false;
            } else {
                sb.append(" ").append(term);
            }
        }

        if (terms.isEmpty()) {
            sb.append("0");
        }

        return sb.toString();
    }
}
