package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.LinearExpr;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Implementation of AlgCPBoolVar that wraps an OR-Tools BoolVar.
 */
@SouthApiSince(SouthApiVersion.V1_0)
public class AlgCPBoolVarImpl implements AlgCPBoolVar {

    private final BoolVar delegate;
    private final boolean negated;

    public AlgCPBoolVarImpl(BoolVar delegate) {
        this.delegate = delegate;
        this.negated = false;
    }

    private AlgCPBoolVarImpl(BoolVar delegate, boolean negated) {
        this.delegate = delegate;
        this.negated = negated;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public String name() {
        return delegate.getName();
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public long lowerBound() {
        return 0;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public long upperBound() {
        return 1;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public AlgCPBoolVar not() {
        return new AlgCPBoolVarImpl((BoolVar) delegate.not(), !negated);
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public boolean isNegated() {
        return negated;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public Literal internal() {
        return negated ? delegate.not() : delegate;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public Object build() {
        Literal lit = negated ? delegate.not() : delegate;
        return LinearExpr.newBuilder().addTerm(lit, 1).build();
    }

    /**
     * Get the underlying OR-Tools BoolVar.
     */
    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public BoolVar getBoolVar() {
        return delegate;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public IntVar getIntVar() {
        return delegate;
    }
}
