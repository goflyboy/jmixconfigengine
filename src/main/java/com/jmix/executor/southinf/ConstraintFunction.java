package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;

//对南向的接口，不需要有相信的实现，仅哟㖋定义，TODO AlgCPLinearExpr也是接口
public class ConstraintFunction {
    IConstraintFunction impl;

    public void addLessOrEqual(final AlgCPLinearExpr expr, final long value) {
        impl.addLessOrEqual(expr, value);
    }

    public void addLessThan(final AlgCPLinearExpr expr, final long value) {
        impl.addLessThan(expr, value);
    }

    public void addGreaterOrEqual(final AlgCPLinearExpr expr, final long value) {
        impl.addGreaterOrEqual(expr, value);
    }

    public void addGreaterThan(final AlgCPLinearExpr expr, final long value) {
        impl.addGreaterThan(expr, value);
    }

}
