package com.jmix.executor.impl.algmodel;

import com.jmix.executor.impl.PriorityConstraint;

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

    private final List<String> exprStrPartTerms = new ArrayList<>();

    private final List<String> exprTemplatePartTerms = new ArrayList<>();

    private final List<String> exprTemplateStrPartTerms = new ArrayList<>();

    private final List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

    private int termIndex = 0;

    public PartAlgCPLinearExpr(String name) {
        super(name);
    }

    public String getExprStrParts() {
        return String.join(" + ", exprStrPartTerms);

    }

    public String getExprTemplateParts() {
        return String.join(" + ", exprTemplatePartTerms);
    }

    public String getExprTemplateStrParts() {
        return String.join(" + ", exprTemplateStrPartTerms);
    }

    public List<PriorityConstraint.PartTerm> getExprVariables() {
        return exprVariables;
    }

    public PartAlgCPLinearExpr() {
        super();
    }

    /**
     * Add a part term with explicit varName (e.g. "S" or "Q").
     */
    public void addTerm(PartVar partVar, IntVar var, long coefficient, String varName) {
        int attrValue = (int) coefficient;

        // build expression string parts
        String shortCode = partVar.getBase().getShortCode();
        if (attrValue != 1) {
            exprStrPartTerms.add(shortCode + "." + varName + "*" + attrValue);
            exprTemplatePartTerms.add("%d*" + attrValue);
            exprTemplateStrPartTerms.add(shortCode + "." + varName + "_%d*" + attrValue);
        } else {
            exprStrPartTerms.add(shortCode + "." + varName);
            exprTemplatePartTerms.add("%d");
            exprTemplateStrPartTerms.add(shortCode + "." + varName + "_%d");
        }

        // create PartTerm
        PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();
        term.setIndex(termIndex);
        term.setPartCode(partVar.getCode());
        term.setTermValue(null);
        exprVariables.add(term);
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
        exprStrPartTerms.add(String.valueOf(value));
        exprTemplatePartTerms.add("%d");
        exprTemplateStrPartTerms.add(String.valueOf(value));
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
        String groupedStr = expr.getExprStrParts();
        String groupedTemplate = expr.getExprTemplateParts();
        String groupedTemplateStr = expr.getExprTemplateStrParts();
        if (groupedStr.isEmpty()) {
            // nothing to add
            return;
        }

        if (coefficient != 1) {
            exprStrPartTerms.add(coefficient + "*(" + groupedStr + ")");
            exprTemplatePartTerms.add(coefficient + "*(" + groupedTemplate + ")");
            exprTemplateStrPartTerms.add(coefficient + "*(" + groupedTemplateStr + ")");
        } else {
            exprStrPartTerms.add("(" + groupedStr + ")");
            exprTemplatePartTerms.add("(" + groupedTemplate + ")");
            exprTemplateStrPartTerms.add("(" + groupedTemplateStr + ")");
        }

        // merge PartTerm list with adjusted indices
        for (PriorityConstraint.PartTerm t : expr.getExprVariables()) {
            PriorityConstraint.PartTerm newTerm = new PriorityConstraint.PartTerm();
            newTerm.setIndex(termIndex++);// TODO
            newTerm.setPartCode(t.getPartCode());
            newTerm.setTermValue(null);
            exprVariables.add(newTerm);
        }
    }

}
