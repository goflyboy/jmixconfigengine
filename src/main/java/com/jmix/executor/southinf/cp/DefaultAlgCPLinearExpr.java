package com.jmix.executor.southinf.cp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultAlgCPLinearExpr implements AlgCPLinearExpr {
    private String name;
    private long constant;
    private final List<Term> terms = new ArrayList<>();

    public DefaultAlgCPLinearExpr(String name) {
        this.name = name;
    }

    public record Term(AlgCPIntVar var, long coefficient) {
    }

    public List<Term> terms() {
        return Collections.unmodifiableList(terms);
    }

    public long constant() {
        return constant;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public AlgCPLinearExpr name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient) {
        terms.add(new Term(var, coefficient));
        return this;
    }

    @Override
    public AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient) {
        terms.add(new Term(var, coefficient));
        return this;
    }

    @Override
    public AlgCPLinearExpr addConstant(long value) {
        constant += value;
        return this;
    }

    @Override
    public boolean isEmpty() {
        return terms.isEmpty() && constant == 0;
    }
}
