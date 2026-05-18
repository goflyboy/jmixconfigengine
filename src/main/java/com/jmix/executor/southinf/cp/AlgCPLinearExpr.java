package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPLinearExpr extends AlgCPLinearArgument {
    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addConstant(long value);

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean isEmpty();

    @SouthApiSince(SouthApiVersion.V1_0)
    static AlgCPLinearExpr weightedSum(AlgCPIntVar[] vars, long[] weights) {
        if (vars.length != weights.length) {
            throw new IllegalArgumentException("Variables and weights arrays must have the same length");
        }
        AlgCPLinearExpr expr = new DefaultAlgCPLinearExpr("weighted_sum");
        for (int i = 0; i < vars.length; i++) {
            expr.addTerm(vars[i], weights[i]);
        }
        return expr;
    }
}
