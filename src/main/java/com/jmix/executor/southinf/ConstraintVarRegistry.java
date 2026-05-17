package com.jmix.executor.southinf;

/**
 * Stable lookup facade for algorithm variables.
 * This is a simplified registry that delegates to the new southbound interface.
 */
public interface ConstraintVarRegistry {

    com.jmix.executor.southinf.var.ParaVar para(String code);

    com.jmix.executor.southinf.var.PartVar part(String code);

    com.jmix.executor.southinf.var.PartCategoryVar partCategory(String code);
}
