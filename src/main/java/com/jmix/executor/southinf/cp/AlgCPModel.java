package com.jmix.executor.southinf.cp;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Core CP-SAT model facade interface.
 * All product algorithms interact with the constraint solver through this interface.
 */
public interface AlgCPModel {

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPIntVar newIntVar(long left, long right, String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPIntVar newIntVarFromDomain(long[] values, String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPBoolVar newBoolVar(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr newLinearExpr(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr newPartLinearExpr(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addBoolAnd(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addBoolOr(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addExactlyOne(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addExactlyOne(BoolVar[] vars);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addAtMostOne(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addAtMostOne(BoolVar[] vars);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPIntVar left, AlgCPLinearExpr right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(IntVar left, AlgCPLinearExpr right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addImplication(AlgCPLiteral left, AlgCPLiteral right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearArgument right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(IntVar left, AlgCPLinearExpr right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessOrEqual(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessOrEqual(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, AlgCPLinearArgument right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessThan(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessThan(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterThan(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterThan(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addDifferent(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addDifferent(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    void minimize(AlgCPLinearExpr expr);

    @SouthApiSince(SouthApiVersion.V1_0)
    void maximize(AlgCPLinearExpr expr);
}
