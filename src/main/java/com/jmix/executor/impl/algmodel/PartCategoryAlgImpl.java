package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.southinf.IModuleAlg;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
public interface PartCategoryAlgImpl {
    String getCategoryCode();

    List<Part> getAllAtomicParts();

    List<PartVar> getAllPartVars(String filterConditionStr);

    Pair<List<PartVar>, List<PartVar>> filterAllPartVars(String filterConditionStr);

    List<Part> getAtomicParts();

    ParaVar getParaVar(String code);

    PartVar getPartVar(String code);

    void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage);
}
