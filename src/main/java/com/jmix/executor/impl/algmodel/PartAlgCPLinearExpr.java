package com.jmix.executor.impl.algmodel;

import com.jmix.executor.impl.PriorityConstraint;

import com.google.ortools.sat.IntVar;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
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

    @Getter(AccessLevel.NONE)
    private final List<PriorityConstraint.PartTerm> terms = new ArrayList<>();

    private int termIndex = 0;

    public PartAlgCPLinearExpr(String name) {
        super(name);
    }

    public String getExprStr() {
        return String.join(" + ", termStrs);
    }

    public String getExprTemplate() {
        return String.join(" + ", termTemplates);
    }

    public String getExprTemplateStr() {
        return String.join(" + ", termTemplateStrs);
    }

    public List<PriorityConstraint.PartTerm> getTerms() {
        return terms;
    }

    public PartAlgCPLinearExpr() {
        super();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("exprStrParts: ").append(getExprStr()).append(System.lineSeparator());
        sb.append("exprTemplateParts: ").append(getExprTemplate()).append(System.lineSeparator());
        sb.append("exprTemplateStrParts: ").append(getExprTemplateStr());
        return sb.toString();
    }

    /**
     * Add a part term with explicit varName (e.g. "S" or "Q").
     */
    public void addTerm(PartVar partVar, IntVar var, long coefficient, String varName) {
        int attrValue = (int) coefficient;

        // build expression string parts
        String shortCode = partVar.getBase().getShortCode();
        if (attrValue != 1) {
            termStrs.add(shortCode + "." + varName + "*" + attrValue);
            termTemplates.add("%d*" + attrValue);
            termTemplateStrs.add(shortCode + "." + varName + "_%d*" + attrValue);
        } else {
            termStrs.add(shortCode + "." + varName);
            termTemplates.add("%d");
            termTemplateStrs.add(shortCode + "." + varName + "_%d");
        }

        // create PartTerm
        PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();
        term.setIndex(termIndex);
        term.setPartCode(partVar.getCode());
        term.setTermValue(null);
        terms.add(term);
        termIndex++;

        // delegate to parent to build numeric expression
        super.addTerm(var, coefficient);
    }

    /**
     * Convenience overload that uses default varName "Q".
     */
    public void addTerm(PartVar partVar, IntVar var, long coefficient) {
        addTerm(partVar, var, coefficient, "Q");
    }

    /**
     * Add constant and keep template parts in sync.
     */
    @Override
    public void addConstant(long value) {
        super.addConstant(value);
        termStrs.add(String.valueOf(value));
        termTemplates.add("%d");
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

        if (coefficient != 1) {
            termStrs.add(coefficient + "*(" + groupedStr + ")");
            termTemplates.add(coefficient + "*(" + groupedTemplate + ")");
            termTemplateStrs.add(coefficient + "*(" + groupedTemplateStr + ")");
        } else {
            termStrs.add("(" + groupedStr + ")");
            termTemplates.add("(" + groupedTemplate + ")");
            termTemplateStrs.add("(" + groupedTemplateStr + ")");
        }

        // merge PartTerm list with adjusted indices
        for (PriorityConstraint.PartTerm t : expr.getTerms()) {
            PriorityConstraint.PartTerm newTerm = new PriorityConstraint.PartTerm();
            newTerm.setIndex(termIndex++); // TODO
            newTerm.setPartCode(t.getPartCode());
            newTerm.setTermValue(null);
            terms.add(newTerm);
        }
    }

}
