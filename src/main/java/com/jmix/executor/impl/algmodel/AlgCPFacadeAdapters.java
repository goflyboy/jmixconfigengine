package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;
import com.jmix.executor.southinf.cp.AlgCPLinearArgument;
import com.jmix.executor.southinf.cp.AlgCPLiteral;
import com.jmix.executor.southinf.cp.DefaultAlgCPLinearExpr;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;

public final class AlgCPFacadeAdapters {
    private AlgCPFacadeAdapters() {
    }

    public static IntVar unwrapInt(AlgCPIntVar var) {
        if (var instanceof AlgCPIntVarImpl impl) {
            return impl.delegate();
        }
        throw new IllegalArgumentException("Unsupported AlgCPIntVar implementation: " + var.getClass().getName());
    }

    public static BoolVar unwrapBool(AlgCPBoolVar var) {
        if (var instanceof AlgCPBoolVarImpl impl) {
            return impl.delegate();
        }
        throw new IllegalArgumentException("Unsupported AlgCPBoolVar implementation: " + var.getClass().getName());
    }

    public static Literal unwrapLiteral(AlgCPLiteral literal) {
        if (literal instanceof AlgCPBoolVarImpl impl) {
            return impl.delegate();
        }
        if (literal instanceof AlgCPLiteralImpl impl) {
            return impl.delegate();
        }
        throw new IllegalArgumentException("Unsupported AlgCPLiteral implementation: " + literal.getClass().getName());
    }

    public static LinearArgument unwrapArgument(AlgCPLinearArgument argument) {
        if (argument instanceof AlgCPIntVarImpl impl) {
            return impl.delegate();
        }
        if (argument instanceof AlgCPLinearExprImpl impl) {
            return impl.delegate().build();
        }
        if (argument instanceof DefaultAlgCPLinearExpr defaultExpr) {
            return unwrapExpr(defaultExpr).build();
        }
        throw new IllegalArgumentException(
                "Unsupported AlgCPLinearArgument implementation: " + argument.getClass().getName());
    }

    public static com.jmix.executor.impl.algmodel.AlgCPLinearExpr unwrapExpr(
            com.jmix.executor.southinf.cp.AlgCPLinearExpr expr) {
        if (expr instanceof AlgCPLinearExprImpl impl) {
            return impl.delegate();
        }
        if (expr instanceof DefaultAlgCPLinearExpr defaultExpr) {
            AlgCPLinearExprImpl impl = new AlgCPLinearExprImpl(defaultExpr.name());
            for (DefaultAlgCPLinearExpr.Term term : defaultExpr.terms()) {
                impl.addTerm(term.var(), term.coefficient());
            }
            impl.addConstant(defaultExpr.constant());
            return impl.delegate();
        }
        throw new IllegalArgumentException("Unsupported AlgCPLinearExpr implementation: " + expr.getClass().getName());
    }

    public static com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr unwrapPartExpr(
            com.jmix.executor.southinf.cp.PartAlgCPLinearExpr expr) {
        if (expr instanceof PartAlgCPLinearExprImpl impl) {
            return impl.delegate();
        }
        throw new IllegalArgumentException(
                "Unsupported PartAlgCPLinearExpr implementation: " + expr.getClass().getName());
    }

    public static Literal[] unwrapLiterals(AlgCPLiteral[] literals) {
        Literal[] result = new Literal[literals.length];
        for (int i = 0; i < literals.length; i++) {
            result[i] = unwrapLiteral(literals[i]);
        }
        return result;
    }
}
