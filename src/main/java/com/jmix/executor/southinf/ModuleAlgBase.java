package com.jmix.executor.southinf;

import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;

import java.util.List;

/**
 * Base class for product algorithms.
 * Provides access to ModuleCPModel.
 * Replaces ConstraintAlgBase.
 * 
 * <p>Product algorithms should extend this class and use model() to access
 * constraint modeling capabilities.
 */
public abstract class ModuleAlgBase {

    private ModuleCPModel constraintModel;

    protected final ModuleCPModel model() {
        return constraintModel;
    }

    protected void setConstraintModel(ModuleCPModel constraintModel) {
        this.constraintModel = constraintModel;
    }

    protected ParaVar para(String code) {
        return model().para(code);
    }

    protected PartVar part(String code) {
        return model().part(code);
    }

    protected PartCategoryVar partCategory(String code) {
        return model().partCategory(code);
    }

    protected List<PartVar> partVars() {
        return partVars("");
    }

    protected List<PartVar> partVars(String filterCondition) {
        return model().partVars(filterCondition);
    }
}
