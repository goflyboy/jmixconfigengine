package com.jmix.executor.impl.southbridge;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.impl.algmodel.AlgCPBoolVarImpl;
import com.jmix.executor.impl.algmodel.AlgCPIntVarImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVarImpl;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;
import com.jmix.executor.southinf.var.ParaOptionVar;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;

import java.util.List;
import java.util.stream.Collectors;

public final class SouthboundVarHandles {

    private SouthboundVarHandles() {
    }

    public static ParaVar para(ParaVarImpl internal) {
        return new ParaVar(new ParaDelegate(internal));
    }

    public static PartVar part(PartVarImpl internal) {
        return new PartVar(new PartDelegate(internal));
    }

    public static PartCategoryVar partCategory(PartCategoryAlgImpl categoryAlg) {
        return new PartCategoryVar(new PartCategoryDelegate(categoryAlg));
    }

    private static class ParaDelegate implements ParaVar.Delegate {
        private final ParaVarImpl internal;

        ParaDelegate(ParaVarImpl internal) {
            this.internal = internal;
        }

        @Override
        public String code() {
            return internal.getCode();
        }

        @Override
        public AlgCPIntVar valueVar() {
            return new AlgCPIntVarImpl(internal.getValue());
        }

        @Override
        public AlgCPBoolVar hiddenVar() {
            return new AlgCPBoolVarImpl(internal.getIsHidden());
        }

        @Override
        public ParaOptionVar option(String optionCode) {
            return new ParaOptionVar(new ParaOptionDelegate(internal.getParaOptionByCode(optionCode)));
        }

        @Override
        public Integer inputValue() {
            return internal.getInputValue();
        }

        @Override
        public boolean hasInput() {
            return Boolean.TRUE.equals(internal.getHasInputed());
        }
    }

    private static class ParaOptionDelegate implements ParaOptionVar.Delegate {
        private final ParaOptionVarImpl internal;

        ParaOptionDelegate(ParaOptionVarImpl internal) {
            this.internal = internal;
        }

        @Override
        public String code() {
            return internal.getCode();
        }

        @Override
        public AlgCPBoolVar selectedVar() {
            return new AlgCPBoolVarImpl(internal.getIsSelectedVar());
        }
    }

    private static class PartDelegate implements PartVar.Delegate {
        private final PartVarImpl internal;

        PartDelegate(PartVarImpl internal) {
            this.internal = internal;
        }

        @Override
        public String code() {
            return internal.getCode();
        }

        @Override
        public String fatherCode() {
            return internal.getBase().getFatherCode();
        }

        @Override
        public String attr(String attrCode) {
            return internal.getAttr(attrCode);
        }

        @Override
        public int attrAsInt(String attrCode) {
            return internal.getAttr4Int(attrCode);
        }

        @Override
        public AlgCPIntVar quantityVar() {
            return new AlgCPIntVarImpl(internal.getQty());
        }

        @Override
        public AlgCPBoolVar selectedVar() {
            return new AlgCPBoolVarImpl(internal.getIsSelected());
        }

        @Override
        public AlgCPBoolVar hiddenVar() {
            return new AlgCPBoolVarImpl(internal.getIsHidden());
        }
    }

    private static class PartCategoryDelegate implements PartCategoryVar.Delegate {
        private final PartCategoryAlgImpl categoryAlg;

        PartCategoryDelegate(PartCategoryAlgImpl categoryAlg) {
            this.categoryAlg = categoryAlg;
        }

        @Override
        public String code() {
            return categoryAlg.getCategoryCode();
        }

        @Override
        public String fatherCode() {
            if (((ModuleBaseAlgImpl) categoryAlg).getModule() instanceof IPart) {
                return ((IPart) ((ModuleBaseAlgImpl) categoryAlg).getModule()).getFatherCode();
            }
            return "";
        }

        @Override
        public String attr(String attrCode) {
            if (((ModuleBaseAlgImpl) categoryAlg).getModule() instanceof IPart) {
                return ((IPart) ((ModuleBaseAlgImpl) categoryAlg).getModule()).getAttr(attrCode);
            }
            return null;
        }

        @Override
        public int attrAsInt(String attrCode) {
            String attr = attr(attrCode);
            if (attr == null) {
                throw new IllegalArgumentException("Attribute not found: " + attrCode);
            }
            return Integer.parseInt(attr);
        }

        @Override
        public AlgCPIntVar quantityVar() {
            throw new UnsupportedOperationException("PartCategoryVar does not expose quantityVar");
        }

        @Override
        public AlgCPBoolVar selectedVar() {
            throw new UnsupportedOperationException("PartCategoryVar does not expose selectedVar");
        }

        @Override
        public AlgCPBoolVar hiddenVar() {
            throw new UnsupportedOperationException("PartCategoryVar does not expose hiddenVar");
        }

        @Override
        public List<PartVar> parts(String filterCondition) {
            return categoryAlg.getAllPartVars(filterCondition).stream()
                    .map(SouthboundVarHandles::part)
                    .collect(Collectors.toList());
        }

        @Override
        public ParaVar sumPara(String attrCode) {
            return SouthboundVarHandles.para(categoryAlg.getSumParaByAttr(attrCode));
        }

        @Override
        public ParaVar sumSumPara(String attrCode) {
            return SouthboundVarHandles.para(categoryAlg.getSumSumParaByAttr(attrCode));
        }
    }
}
