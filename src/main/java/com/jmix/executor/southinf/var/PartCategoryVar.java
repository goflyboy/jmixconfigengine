package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stable part category variable facade.
 */
public class PartCategoryVar extends PartVar {

    private final PartCategoryAlgImpl categoryAlg;

    public PartCategoryVar(PartCategoryAlgImpl categoryAlg) {
        super(newCategoryPartVar(categoryAlg));
        this.categoryAlg = categoryAlg;
    }

    public PartCategoryVar(PartVarImpl internal) {
        super(internal);
        this.categoryAlg = null;
    }

    public List<PartVar> parts() {
        return parts("");
    }

    public List<PartVar> parts(String filterCondition) {
        ensureCategoryAlg();
        return categoryAlg.getAllPartVars(filterCondition).stream()
                .map(PartVar::new)
                .collect(Collectors.toList());
    }

    public ParaVar sumPara(String attrCode) {
        ensureCategoryAlg();
        return new ParaVar(((ModuleBaseAlgImpl) categoryAlg).getSumParaByAttr(attrCode));
    }

    public ParaVar sumSumPara(String attrCode) {
        ensureCategoryAlg();
        return new ParaVar(((ModuleBaseAlgImpl) categoryAlg).getSumSumParaByAttr(attrCode));
    }

    private void ensureCategoryAlg() {
        if (categoryAlg == null) {
            throw new IllegalStateException("PartCategoryVar is not bound to a category algorithm");
        }
    }

    private static PartVarImpl newCategoryPartVar(PartCategoryAlgImpl categoryAlg) {
        PartVarImpl partVar = new PartVarImpl();
        if (((ModuleBaseAlgImpl) categoryAlg).getModule() instanceof IPart) {
            partVar.setBase((IPart) ((ModuleBaseAlgImpl) categoryAlg).getModule());
        }
        return partVar;
    }
}
