package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 閮ㄤ欢鍒嗙被绾х畻娉曞疄鐜?
 * 涓撴敞浜庡崟涓儴浠跺垎绫荤殑绾︽潫澶勭悊
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
     * 鏍规嵁灞炴€т唬鐮佽幏鍙栨眹鎬诲弬鏁帮紙SumSum绫诲瀷锛?
     * 
     * @param attrCode 灞炴€т唬鐮?
     * @return 瀵瑰簲鐨勫弬鏁板彉閲?
     */
    ParaVarImpl getSumSumParaByAttr(String attrCode);

    /**
     * 鏍规嵁灞炴€т唬鐮佽幏鍙栨眹鎬诲弬鏁帮紙Sum绫诲瀷锛?
     * 
     * @param attrCode 灞炴€т唬鐮?
     * @return 瀵瑰簲鐨勫弬鏁板彉閲?
     */
    ParaVarImpl getSumParaByAttr(String attrCode);

    void initRules(Map<String, Method> allRuleMethods, Object moduleAlgFile, CalcStage calcStage);

    void initInput(Object moduleAlgFile);
}
