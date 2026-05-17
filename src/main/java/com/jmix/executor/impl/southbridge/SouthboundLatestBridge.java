package com.jmix.executor.impl.southbridge;

import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.southinf.AlgorithmDescriptor;
import com.jmix.executor.southinf.ConstraintCapabilities;
import com.jmix.executor.southinf.ConstraintContext;
import com.jmix.executor.southinf.ConstraintModel;
import com.jmix.executor.southinf.ConstraintVarRegistry;
import com.jmix.executor.southinf.var.BoolExpr;
import com.jmix.executor.southinf.var.ConstraintRef;
import com.jmix.executor.southinf.var.Exprs;
import com.jmix.executor.southinf.var.IntExpr;
import com.jmix.executor.southinf.var.LinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.view.ModuleInstView;

import com.google.ortools.sat.Literal;

import java.util.Collection;

/**
 * V1 bridge from the stable southinf API to the current solver implementation.
 */
public class SouthboundLatestBridge implements ConstraintContext {

    private final ModuleAlgImpl algorithm;

    private final ConstraintModel model;

    private final ConstraintVarRegistry vars;

    private final ConstraintCapabilities capabilities;

    public SouthboundLatestBridge(ModuleAlgImpl algorithm) {
        this.algorithm = algorithm;
        this.model = new ModelBridge(algorithm);
        this.vars = new VarRegistryBridge(algorithm);
        this.capabilities = new ConstraintCapabilities() {
        };
    }

    @Override
    public AlgorithmDescriptor descriptor() {
        AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
        descriptor.setAlgorithmId(algorithm.getClass().getName());
        descriptor.setSouthApiVersion("1.0");
        return descriptor;
    }

    @Override
    public ConstraintModel model() {
        return model;
    }

    @Override
    public ConstraintVarRegistry vars() {
        return vars;
    }

    @Override
    public ModuleInstView moduleInst() {
        if (algorithm instanceof ModuleInstView) {
            return (ModuleInstView) algorithm;
        }
        throw new UnsupportedOperationException("ModuleInstView is only available for ConstraintAlgBase");
    }

    @Override
    public ConstraintCapabilities capabilities() {
        return capabilities;
    }

    private static class VarRegistryBridge implements ConstraintVarRegistry {

        private final ModuleAlgImpl algorithm;

        VarRegistryBridge(ModuleAlgImpl algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public ParaVar para(String code) {
            return new ParaVar(algorithm.getParaVar(code));
        }

        @Override
        public PartVar part(String code) {
            return new PartVar(algorithm.getPartVar(code));
        }

        @Override
        public PartCategoryVar partCategory(String code) {
            return new PartCategoryVar(algorithm.getPartCategoryAlg(code));
        }
    }

    private static class ModelBridge implements ConstraintModel {

        private final ModuleAlgImpl algorithm;

        ModelBridge(ModuleAlgImpl algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public ConstraintRef equal(IntExpr left, long right) {
            return Exprs.constraintRef(algorithm.getModel().addEquality(left.unwrap(), right));
        }

        @Override
        public ConstraintRef equal(IntExpr left, IntExpr right) {
            return Exprs.constraintRef(algorithm.getModel().addEquality(left.unwrap(), right.unwrap()));
        }

        @Override
        public ConstraintRef greaterOrEqual(IntExpr left, long right) {
            return Exprs.constraintRef(algorithm.getModel().addGreaterOrEqual(left.unwrap(), right));
        }

        @Override
        public ConstraintRef greaterOrEqual(IntExpr left, IntExpr right) {
            return Exprs.constraintRef(algorithm.getModel().addGreaterOrEqual(left.unwrap(), right.unwrap()));
        }

        @Override
        public ConstraintRef lessOrEqual(IntExpr left, long right) {
            return Exprs.constraintRef(algorithm.getModel().addLessOrEqual(left.unwrap(), right));
        }

        @Override
        public ConstraintRef implication(BoolExpr left, BoolExpr right) {
            return Exprs.constraintRef(algorithm.getModel().addImplication(left.literal(), right.literal()));
        }

        @Override
        public ConstraintRef exactlyOne(Collection<BoolExpr> expressions) {
            Literal[] literals = expressions.stream()
                    .map(BoolExpr::literal)
                    .toArray(Literal[]::new);
            return Exprs.constraintRef(algorithm.getModel().addExactlyOne(literals));
        }

        @Override
        public ConstraintRef compatibilityRequire(String ruleCode, BoolExpr left, BoolExpr right) {
            return implication(left, right);
        }

        @Override
        public ConstraintRef compatibilityIncompatible(String ruleCode, BoolExpr left, BoolExpr right) {
            ConstraintRef ref = Exprs.constraintRef(
                    algorithm.getModel().addImplication(left.literal(), right.literal().not()));
            algorithm.getModel().addImplication(right.literal(), left.literal().not());
            return ref;
        }

        @Override
        public ConstraintRef compatibilityCoDependent(String ruleCode, BoolExpr left, BoolExpr right) {
            ConstraintRef ref = implication(left, right);
            implication(right, left);
            return ref;
        }

        @Override
        public LinearExpr linearExpr(String name) {
            return Exprs.linearExpr(algorithm.getModel().newLinearExpr(name));
        }

        @Override
        public void minimize(LinearExpr expr) {
            algorithm.getModel().minimize(expr.unwrap());
        }

        @Override
        public void maximize(LinearExpr expr) {
            algorithm.getModel().maximize(expr.unwrap());
        }
    }
}
