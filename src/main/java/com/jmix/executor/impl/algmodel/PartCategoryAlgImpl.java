package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;

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

    int getInstId();

    String getCategoryCode();

    List<Part> getAllAtomicParts();

    List<PartVarImpl> getAllPartVars(String filterConditionStr);

    Pair<List<PartVarImpl>, List<PartVarImpl>> filterAllPartVars(String filterConditionStr);

    List<Part> getAtomicParts();

    ParaVarImpl getParaVar(String code);

    PartVarImpl getPartVar(String code);

    /**
     * 根据属性代码获取汇总参数（SumSum类型）
     * 
     * @param attrCode 属性代码
     * @return 对应的参数变量
     */
    ParaVarImpl getSumSumParaByAttr(String attrCode);

    /**
     * 根据属性代码获取汇总参数（Sum类型）
     * 
     * @param attrCode 属性代码
     * @return 对应的参数变量
     */
    ParaVarImpl getSumParaByAttr(String attrCode);

    void initRules(Map<String, Method> allRuleMethods, Object moduleAlgFile, CalcStage calcStage);

    void initInput(Object moduleAlgFile);
}
