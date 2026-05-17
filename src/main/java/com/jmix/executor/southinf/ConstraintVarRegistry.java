package com.jmix.executor.southinf;

import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;

/**
 * Stable lookup facade for algorithm variables.
 */
public interface ConstraintVarRegistry {

    ParaVar para(String code);

    PartVar part(String code);

    PartCategoryVar partCategory(String code);
}
