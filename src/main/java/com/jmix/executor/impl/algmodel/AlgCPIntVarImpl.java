package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.util.Domain;
import com.jmix.executor.southinf.cp.AlgCPIntVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Implementation of AlgCPIntVar that wraps an OR-Tools IntVar.
 */
@SouthApiSince(SouthApiVersion.V1_0)
public class AlgCPIntVarImpl implements AlgCPIntVar {

    private final IntVar delegate;

    public AlgCPIntVarImpl(IntVar delegate) {
        this.delegate = delegate;
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public String name() {
        return delegate.getName();
    }

    private static java.util.List<Long> getDomainValues(Domain domain) {
        try {
            java.lang.reflect.Method m = Domain.class.getMethod("getValuesList");
            @SuppressWarnings("unchecked")
            java.util.List<Long> values = (java.util.List<Long>) m.invoke(domain);
            return values;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public long lowerBound() {
        var values = getDomainValues(delegate.getDomain());
        if (values.isEmpty()) {
            return Long.MIN_VALUE;
        }
        return values.get(0);
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public long upperBound() {
        var values = getDomainValues(delegate.getDomain());
        if (values.isEmpty()) {
            return Long.MAX_VALUE;
        }
        return values.get(values.size() - 1);
    }

    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public Object build() {
        return delegate;
    }

    /**
     * Get the underlying OR-Tools IntVar.
     */
    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    public IntVar getIntVar() {
        return delegate;
    }
}
