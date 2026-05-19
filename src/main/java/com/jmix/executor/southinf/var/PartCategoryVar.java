package com.jmix.executor.southinf.var;

import java.util.List;

/**
 * Stable part category variable facade.
 */
public class PartCategoryVar extends PartVar {

    private final Delegate delegate;

    public PartCategoryVar(Delegate delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public interface Delegate extends PartVar.Delegate {
        List<PartVar> parts(String filterCondition);

        ParaVar sumPara(String attrCode);

        ParaVar sumSumPara(String attrCode);
    }

    public List<PartVar> parts() {
        return parts("");
    }

    public List<PartVar> parts(String filterCondition) {
        return delegate.parts(filterCondition);
    }

    public ParaVar sumPara(String attrCode) {
        return delegate.sumPara(attrCode);
    }

    public ParaVar sumSumPara(String attrCode) {
        return delegate.sumSumPara(attrCode);
    }
}
