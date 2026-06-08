package com.jmix.executor.impl.southbridge;

import com.jmix.executor.impl.algmodel.AlgCPConstraintImpl;
import com.jmix.executor.impl.algmodel.AlgCPFacadeAdapters;
import com.jmix.executor.impl.algmodel.AlgCPBoolVarImpl;
import com.jmix.executor.impl.algmodel.AlgCPIntVarImpl;
import com.jmix.executor.impl.algmodel.AlgCPLinearExprImpl;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExprImpl;
import com.jmix.executor.southinf.ModuleCPModel;
import com.jmix.executor.southinf.cp.AlgCPConstraint;
import com.jmix.executor.southinf.cp.AlgCPIntVar;
import com.jmix.executor.southinf.cp.AlgCPLinearArgument;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.AlgCPLiteral;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;

import java.util.List;
import java.util.stream.Collectors;

public class SouthboundLatestBridge {
    private final ModuleCPModel model;

    public SouthboundLatestBridge(ModuleAlgImpl algorithm) {
        this.model = new ModelBridge(algorithm);
    }

    public ModuleCPModel model() {
        return model;
    }

    private static class ModelBridge implements ModuleCPModel {
        private final ModuleAlgImpl algorithm;

        ModelBridge(ModuleAlgImpl algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public ParaVar para(String code) {
            return SouthboundVarHandles.para(algorithm.getCurrentOrModuleParaVar(code));
        }

        @Override
        public PartVar part(String code) {
            return SouthboundVarHandles.part(algorithm.getPartVar(code));
        }

        @Override
        public PartCategoryVar partCategory(String code) {
            return SouthboundVarHandles.partCategory(algorithm.getPartCategoryAlg(code));
        }

        @Override
        public List<PartVar> partVars() {
            return partVars("");
        }

        @Override
        public List<PartVar> partVars(String filterCondition) {
            return algorithm.getInternalPartVars(filterCondition).stream()
                    .map(SouthboundVarHandles::part)
                    .collect(Collectors.toList());
        }

        @Override
        public PartAlgCPLinearExpr sum4Quantity(String attrCode, String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Quantity(attrCode, filterCondition));
        }

        @Override
        public PartAlgCPLinearExpr sum4Quantity(String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Quantity(filterCondition));
        }

        @Override
        public PartAlgCPLinearExpr sum4Quantity(String partCategoryCodes, String attrCode, String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Quantity(partCategoryCodes, attrCode, filterCondition));
        }

        @Override
        public PartAlgCPLinearExpr sum4Selected(String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Selected(filterCondition));
        }

        @Override
        public PartAlgCPLinearExpr sum4Selected(String attrCode, String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Selected(attrCode, filterCondition));
        }

        @Override
        public PartAlgCPLinearExpr sum4Selected(String partCategoryCodes, String attrCode, String filterCondition) {
            return new PartAlgCPLinearExprImpl(algorithm.sum4Selected(partCategoryCodes, attrCode, filterCondition));
        }

        @Override
        public AlgCPIntVar newIntVar(long left, long right, String name) {
            return new AlgCPIntVarImpl(algorithm.getModel().newIntVar(left, right, name));
        }

        @Override
        public AlgCPIntVar newIntVarFromDomain(long[] values, String name) {
            return new AlgCPIntVarImpl(algorithm.getModel().newIntVarFromDomain(values, name));
        }

        @Override
        public com.jmix.executor.southinf.cp.AlgCPBoolVar newBoolVar(String name) {
            return new AlgCPBoolVarImpl(algorithm.getModel().newBoolVar(name));
        }

        @Override
        public AlgCPLinearExpr newLinearExpr(String name) {
            return new AlgCPLinearExprImpl(algorithm.getModel().newLinearExpr(name));
        }

        @Override
        public PartAlgCPLinearExpr newPartLinearExpr(String name) {
            return new PartAlgCPLinearExprImpl(algorithm.getModel().newPartLinearExpr(name));
        }

        @Override
        public AlgCPConstraint addBoolAnd(AlgCPLiteral[] literals) {
            return new AlgCPConstraintImpl(algorithm.getModel().addBoolAnd(AlgCPFacadeAdapters.unwrapLiterals(literals)));
        }

        @Override
        public AlgCPConstraint addBoolOr(AlgCPLiteral[] literals) {
            return new AlgCPConstraintImpl(algorithm.getModel().addBoolOr(AlgCPFacadeAdapters.unwrapLiterals(literals)));
        }

        @Override
        public AlgCPConstraint addExactlyOne(AlgCPLiteral[] literals) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addExactlyOne(AlgCPFacadeAdapters.unwrapLiterals(literals)));
        }

        @Override
        public AlgCPConstraint addAtMostOne(AlgCPLiteral[] literals) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addAtMostOne(AlgCPFacadeAdapters.unwrapLiterals(literals)));
        }

        @Override
        public AlgCPConstraint addImplication(AlgCPLiteral left, AlgCPLiteral right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addImplication(
                    AlgCPFacadeAdapters.unwrapLiteral(left), AlgCPFacadeAdapters.unwrapLiteral(right)));
        }

        @Override
        public AlgCPConstraint addEquality(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addEquality(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addEquality(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearExpr right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addEquality(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapExpr(right)));
        }

        @Override
        public AlgCPConstraint addEquality(AlgCPLinearExpr left, long right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addEquality(AlgCPFacadeAdapters.unwrapExpr(left), right));
        }

        @Override
        public AlgCPConstraint addLessOrEqual(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addLessOrEqual(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addLessOrEqual(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addLessOrEqual(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public AlgCPConstraint addLessOrEqual(AlgCPLinearExpr left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addLessOrEqual(AlgCPFacadeAdapters.unwrapExpr(left), right));
        }

        @Override
        public AlgCPConstraint addLessThan(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addLessThan(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addLessThan(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addLessThan(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public AlgCPConstraint addLessThan(AlgCPLinearExpr left, long right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addLessThan(AlgCPFacadeAdapters.unwrapExpr(left), right));
        }

        @Override
        public AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addGreaterOrEqual(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addGreaterOrEqual(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public AlgCPConstraint addGreaterOrEqual(AlgCPLinearExpr left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addGreaterOrEqual(AlgCPFacadeAdapters.unwrapExpr(left), right));
        }

        @Override
        public AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, AlgCPLinearExpr right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addGreaterOrEqual(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapExpr(right)));
        }

        @Override
        public AlgCPConstraint addGreaterThan(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addGreaterThan(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addGreaterThan(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addGreaterThan(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public AlgCPConstraint addGreaterThan(AlgCPLinearExpr left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addGreaterThan(AlgCPFacadeAdapters.unwrapExpr(left), right));
        }

        @Override
        public AlgCPConstraint addDifferent(AlgCPLinearArgument left, long right) {
            return new AlgCPConstraintImpl(
                    algorithm.getModel().addDifferent(AlgCPFacadeAdapters.unwrapArgument(left), right));
        }

        @Override
        public AlgCPConstraint addDifferent(AlgCPLinearArgument left, AlgCPLinearArgument right) {
            return new AlgCPConstraintImpl(algorithm.getModel().addDifferent(
                    AlgCPFacadeAdapters.unwrapArgument(left), AlgCPFacadeAdapters.unwrapArgument(right)));
        }

        @Override
        public void minimize(AlgCPLinearExpr expr) {
            algorithm.getModel().minimize(AlgCPFacadeAdapters.unwrapExpr(expr));
        }

        @Override
        public void maximize(AlgCPLinearExpr expr) {
            algorithm.getModel().maximize(AlgCPFacadeAdapters.unwrapExpr(expr));
        }

        @Override
        public void setObjectExpr(PartAlgCPLinearExpr expr) {
            algorithm.getModel().setObjectExpr(AlgCPFacadeAdapters.unwrapPartExpr(expr));
        }
    }
}
