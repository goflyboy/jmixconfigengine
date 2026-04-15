package com.jmix.executor.impl.algmodel;

import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.PriorityConstraint;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended linear expression that carries part-related term metadata for
 * building
 * readable expression strings and PartTerm lists.
 */
@Slf4j
@Data
public class PartAlgCPLinearExpr extends AlgCPLinearExpr {

    private final List<String> termStrs = new ArrayList<>();

    private final List<String> termTemplates = new ArrayList<>();

    private final List<String> termTemplateStrs = new ArrayList<>();

    private final List<PriorityConstraint.PartTerm> partTerms = new ArrayList<>();

    private int termIndex = 0;

    public PartAlgCPLinearExpr(String name) {
        super(name);
    }

    public String getExprStr() {
        return joinWithSigns(termStrs);
    }

    public String getExprTemplate() {
        return joinWithSigns(termTemplates);
    }

    public String getExprTemplateStr() {
        return joinWithSigns(termTemplateStrs);
    }

    /**
     * Return comma separated part codes of stored PartTerm entries, e.g. "P1,P2"
     */
    public String getPartTermsStr() {
        StringBuilder sb = new StringBuilder();
        for (PriorityConstraint.PartTerm pt : partTerms) {
            String code = pt.getPartCode();
            if (code == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(code);
        }
        return sb.toString();
    }

    private String joinWithSigns(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            String raw = items.get(i).trim();
            if (raw.isEmpty()) {
                continue;
            }
            if (raw.startsWith("-")) {
                String unsigned = raw.substring(1);
                if (sb.length() == 0) {
                    sb.append("-").append(unsigned);
                } else {
                    sb.append(" - ").append(unsigned);
                }
            } else if (raw.startsWith("+")) {
                String unsigned = raw.substring(1);
                if (sb.length() == 0) {
                    sb.append(unsigned);
                } else {
                    sb.append(" + ").append(unsigned);
                }
            } else {
                if (sb.length() == 0) {
                    sb.append(raw);
                } else {
                    sb.append(" + ").append(raw);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getExprStr();
    }

    /**
     * 设置名称
     * 
     * @param name
     * @return
     */
    public PartAlgCPLinearExpr name(String name) {
        this.setName(name);
        return this;
    }

    private String getPrefix(int instId) {
        return instId == ModuleInst.DEFAULT_INSTANCE_ID ? "" : "I" + instId + "_";
    }

    /**
     * Add a part term with explicit varName (e.g. "S" or "Q").
     */
    public void addTerm(String partCategoryCode, PartVar partVar, IntVar var, long coefficient, String varName) {
        int attrValue = (int) coefficient;

        // build expression string parts
        String prefix = getPrefix(partVar.getInstId());
        String shortCode = prefix + partVar.getBase().getShortCode();
        termStrs.add(attrValue + "*" + shortCode + "." + varName);
        termTemplates.add(attrValue + "*" + "%d");
        termTemplateStrs.add(attrValue + "*" + shortCode + "." + varName + "_%d");

        // create PartTerm
        PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();
        term.setIndex(termIndex);
        term.setPartCategoryCode(partCategoryCode);
        term.setInstId(partVar.getInstId());
        term.setPartCode(partVar.getCode());
        term.setTermValue(varName);
        partTerms.add(term);
        termIndex++;

        // delegate to parent to build numeric expression
        super.addTerm(var, coefficient);
    }

    private boolean isSinglePositiveExpr(String expr) {
        // 如果含有"(“，则否
        // 统计含"+"、"-"的个数，如果个数大于1，则否
        // 否者是
        if (expr == null || expr.trim().isEmpty()) {
            return true;
        }
        if (expr.charAt(0) == '-') { // 如：-30*P3.Q
            return false;
        }
        // If contains parentheses, treat as not a single simple expression
        if (expr.indexOf('(') >= 0 || expr.indexOf(')') >= 0) {
            return false;
        }
        int operatorNum = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            // if (c == '+' || c == '-') {
            if (c == '+') {
                operatorNum++;
            }
        }
        return operatorNum <= 0;
    }

    /**
     * Convenience overload that uses default varName "Q".
     */
    public void addTerm(String partCategoryCode, PartVar partVar, IntVar var, long coefficient) {
        addTerm(partCategoryCode, partVar, var, coefficient, PartVar.QTY_SHORT_NAME);
    }

    /**
     * Convenience overload that uses default varName "Q".
     */
    public void addTerm(String partCategoryCode, PartVar partVar, BoolVar var, long coefficient) {
        addTerm(partCategoryCode, partVar, var, coefficient, PartVar.ISSELECTED_SHORT_NAME);
    }

    /**
     * Add constant and keep template parts in sync.
     */
    @Override
    public void addConstant(long value) {
        super.addConstant(value);
        termStrs.add(String.valueOf(value));
        termTemplates.add(String.valueOf(value));
        termTemplateStrs.add(String.valueOf(value));
    }

    /**
     * Add another PartAlgCPLinearExpr into this one with a coefficient.
     * This will merge numeric terms (via super.addExpr) and also merge
     * the part-related metadata (string parts, templates and PartTerms).
     *
     * @param expr        the other PartAlgCPLinearExpr
     * @param coefficient coefficient to apply to the other expression
     */
    public void addExpr(PartAlgCPLinearExpr expr, long coefficient) {
        // numeric combination
        super.addExpr(expr, coefficient);
        // compose grouped string/template parts
        String groupedStr = expr.getExprStr();
        String groupedTemplate = expr.getExprTemplate();
        String groupedTemplateStr = expr.getExprTemplateStr();
        if (groupedStr.isEmpty()) {
            // nothing to add
            return;
        }
        if (isSinglePositiveExpr(expr.getExprStr())) {
            termStrs.add(coefficient + "*" + groupedStr + "");
            termTemplates.add(coefficient + "*" + groupedTemplate + "");
            termTemplateStrs.add(coefficient + "*" + groupedTemplateStr + "");
        } else {
            termStrs.add(coefficient + "*(" + groupedStr + ")");
            termTemplates.add(coefficient + "*(" + groupedTemplate + ")");
            termTemplateStrs.add(coefficient + "*(" + groupedTemplateStr + ")");
        }

        // merge PartTerm list with adjusted indices
        for (PriorityConstraint.PartTerm t : expr.getPartTerms()) {
            PriorityConstraint.PartTerm newTerm = new PriorityConstraint.PartTerm();
            newTerm.setIndex(termIndex++);
            newTerm.setPartCategoryCode(t.getPartCategoryCode());
            newTerm.setInstId(t.getInstId());
            newTerm.setPartCode(t.getPartCode());
            newTerm.setTermValue(t.getTermValue());
            partTerms.add(newTerm);
        }
    }

    private void addAbsExpr(PartAlgCPLinearExpr expr) {
        // 合并所有 termStrs 为一个整体，然后添加绝对值符号
        String combinedStr = joinWithSigns(expr.getTermStrs());
        String combinedTemplate = joinWithSigns(expr.getTermTemplates());
        String combinedTemplateStr = joinWithSigns(expr.getTermTemplateStrs());

        // 清空并添加带绝对值符号的单一项
        expr.getTermStrs().clear();
        expr.getTermStrs().add("|" + combinedStr + "|");

        expr.getTermTemplates().clear();
        expr.getTermTemplates().add("|" + combinedTemplate + "|");

        expr.getTermTemplateStrs().clear();
        expr.getTermTemplateStrs().add("|" + combinedTemplateStr + "|");
    }

    /**
     * 在约束模型中添加绝对值约束。
     * 创建一个新的变量表示绝对值结果，并添加约束确保该变量等于传入表达式的绝对值。
     *
     * @param expr  要计算绝对值的表达式
     * @param model 约束模型
     * @return 表示绝对值结果的 IntVar 变量
     */
    public void addAbsExpr(PartAlgCPLinearExpr expr, AlgCPModel model) {
        // 首先修改字符串表示为绝对值形式
        addAbsExpr(expr);

        // 创建变量表示 |expr|
        // 假设表达式值范围在 [-100, 100]，所以绝对值范围是 [0, 100]
        IntVar absVar = model.newIntVar(0, 100000, "abs_" + expr.getName());

        // 获取表达式的 LinearExpr
        // LinearExpr exprLinear = expr.build();

        // 添加绝对值约束: absVar >= expr 且 absVar >= -expr
        // 这确保 absVar = |expr|
        model.addGreaterOrEqual(absVar, expr);
        model.addGreaterOrEqual(absVar, AlgCPLinearExpr.term(expr, -1));
        super.add(absVar);
    }

}
