package com.jmix.executor.southinf.var;

import com.jmix.executor.impl.algmodel.AlgCPConstraint;
import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;

/**
 * Small adapters for solver expression objects exposed through southinf.
 */
public final class Exprs {

    private Exprs() {
    }

    public static IntExpr intExpr(LinearArgument expr) {
        return new IntExprImpl(expr);
    }

    public static BoolExpr boolExpr(BoolVar expr) {
        return new BoolExprImpl(expr);
    }

    public static LinearExpr linearExpr(AlgCPLinearExpr expr) {
        return new LinearExprImpl(expr);
    }

    public static ConstraintRef constraintRef(AlgCPConstraint constraint) {
        return new ConstraintRefImpl(constraint);
    }

    static class IntExprImpl implements IntExpr {

        private final LinearArgument expr;

        IntExprImpl(LinearArgument expr) {
            this.expr = expr;
        }

        @Override
        public LinearArgument unwrap() {
            return expr;
        }
    }

    static final class BoolExprImpl extends IntExprImpl implements BoolExpr {

        private final BoolVar expr;

        BoolExprImpl(BoolVar expr) {
            super(expr);
            this.expr = expr;
        }

        @Override
        public Literal literal() {
            return expr;
        }
    }

    static final class LinearExprImpl implements LinearExpr {

        private final AlgCPLinearExpr expr;

        LinearExprImpl(AlgCPLinearExpr expr) {
            this.expr = expr;
        }

        @Override
        public AlgCPLinearExpr unwrap() {
            return expr;
        }
    }

    static final class ConstraintRefImpl implements ConstraintRef {

        private final AlgCPConstraint constraint;

        ConstraintRefImpl(AlgCPConstraint constraint) {
            this.constraint = constraint;
        }

        @Override
        public ConstraintRef onlyIf(BoolExpr condition) {
            constraint.onlyEnforceIf(condition.literal());
            return this;
        }

        @Override
        public ConstraintRef withRuleCode(String ruleCode) {
            return this;
        }
    }
}
