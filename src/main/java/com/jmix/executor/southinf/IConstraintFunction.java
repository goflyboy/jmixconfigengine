package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;

public interface IConstraintFunction {
    void addLessOrEqual(final AlgCPLinearExpr expr, final long value);

    void addLessThan(final AlgCPLinearExpr expr, final long value);

    void addGreaterOrEqual(final AlgCPLinearExpr expr, final long value);

    void addGreaterThan(final AlgCPLinearExpr expr, final long value);

}
