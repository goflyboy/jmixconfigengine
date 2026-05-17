package com.jmix.executor.southinf;

/**
 * Stable lookup facade for algorithm variables.
 * 
 * @deprecated Use {@link ModuleCPModel} instead, which includes these lookup methods.
 */
@Deprecated
public interface ConstraintVarRegistry {

    com.jmix.executor.southinf.var.ParaVar para(String code);

    com.jmix.executor.southinf.var.PartVar part(String code);

    com.jmix.executor.southinf.var.PartCategoryVar partCategory(String code);
}
