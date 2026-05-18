package com.jmix.coretest;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVarImpl;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.impl.algmodel.VarImpl;
import com.jmix.executor.model.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConstraintAlgImplTestBase extends ModuleAlgImpl {

    /**
     * 涓轰簡绠€鍖栨祴璇曠敤渚嬩腑绠楁硶鐨勮〃杈撅紝绫讳技 p1.value + p2.value 鐨勮〃杈撅紝鑰屼笉鏄?
     * p1.getValue() + p2.getValue()銆傚拰 ParaVarImpl/Part 绫讳技锛屼絾涓嶆槸缁ф壙锛岃€屾槸鍐呴儴绫汇€?
     */
    static public class ParaVar extends VarImpl<Para> {

        /**
         * 鍙傛暟鍊肩姸鎬?
         */
        public IntVar value;

        /**
         * 鍏煎鏃фā鍨嬬殑闅愯棌甯冨皵鍙橀噺
         * 0-鏄剧ず锛?-闅愯棌
         */
        public BoolVar isHidden;

        /**
         * 鍙傛暟鍙€夊€肩殑閫変腑鐘舵€?CodeId -> ParaOptionVar)
         */
        public Map<Integer, ParaOptionVarImpl> optionSelectVars = new HashMap<>();

        /**
         * 鏍规嵁浠ｇ爜ID鑾峰彇鍙傛暟閫夐」鍙橀噺
         *
         * @param codeId 閫夐」鐨勪唬鐮両D
         * @return 瀵瑰簲鐨勫弬鏁伴€夐」鍙橀噺锛屽鏋滀笉瀛樺湪鍒欒繑鍥瀗ull
         */
        public ParaOptionVarImpl getParaOptionByCodeId(Integer codeId) {
            return optionSelectVars.get(codeId);
        }

        /**
         * 鏍规嵁浠ｇ爜鑾峰彇鍙傛暟閫夐」鍙橀噺
         *
         * @param code 閫夐」鐨勪唬鐮?
         * @return 瀵瑰簲鐨勫弬鏁伴€夐」鍙橀噺
         * @throws AlgLoaderException 濡傛灉鎵句笉鍒板搴旂殑閫夐」
         */
        public ParaOptionVarImpl getParaOptionByCode(String code) {
            for (ParaOptionVarImpl option : optionSelectVars.values()) {
                if (code != null && code.equals(option.getCode())) {
                    return option;
                }
            }
            throw new AlgLoaderException("ParaOptionVar not found for code: " + code);
        }

        /**
         * Reference to internal implementation for delegating runtime values in tests.
         */
        public ParaVarImpl internal;

        public Integer getInputValue() {
            return internal != null ? internal.getInputValue() : null;
        }

        public Boolean getIsHasInputed() {
            return internal != null ? internal.getHasInputed() : Boolean.FALSE;
        }

        public void setIsHasInputed(Boolean hasInputed) {
            if (internal != null) {
                internal.setHasInputed(hasInputed);
            }
        }
    }

    @Data
    public class PartVar extends VarImpl<Part> {
        /**
         * 閮ㄤ欢鐨勬暟閲忓€?
         */
        public IntVar qty;

        /**
         * 鏄剧ず闅愯棌灞炴€?
         */
        public BoolVar isHidden;

        /**
         * 瀛愰儴浠堕€変腑鐘舵€?Part.code -> BoolVar)
         */
        // public Map<String, BoolVar> subPartSelectedVars = new HashMap<>();

        /**
         * 鏄惁閫変腑
         */
        public BoolVar isSelected;
    }

    /**
     * 鍒濆鍖朚odule鍚?
     *
     * @param module
     * @param moduleAlg
     */
    protected void afterInitData(IModule module, Object moduleAlg) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = this.getPartCategoryAlgs();

        // 鑾峰彇褰撳墠绫荤殑鎵€鏈夊瓧娈?
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            fieldMap.put(field.getName(), field);
        }

        // 閬嶅巻partCategoryAlgImpls锛屽垱寤篜artCategoryVar骞惰缃埌瀵瑰簲鐨勫瓧娈典笂
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            String categoryCode = partCategoryAlgImpl.getCategoryCode();
            Field field = fieldMap.get(categoryCode);

            if (field != null && PartCategoryVar.class.isAssignableFrom(field.getType())) {
                try {
                    // 鍒涘缓PartCategoryVar
                    PartCategoryVar partCategoryVar = new PartCategoryVar();
                    partCategoryVar.setAlgImpl((ModuleBaseAlgImpl) partCategoryAlgImpl);

                    // 璁剧疆鍒板瓧娈典笂
                    field.set(this, partCategoryVar);
                    log.info("Set PartCategoryVar for field: {}, categoryCode: {}", field.getName(), categoryCode);
                } catch (IllegalAccessException e) {
                    log.error("Failed to set PartCategoryVar for field: {}", field.getName(), e);
                }
            }
        }
    }

    public class PartCategoryVar extends PartVar {
        /**
         * 寮曠敤鍒板唴閮ㄧ畻娉曞疄鐜?
         */
        private ModuleBaseAlgImpl algImpl;

        public void setAlgImpl(ModuleBaseAlgImpl algImpl) {
            this.algImpl = algImpl;
        }

        public ParaVar getSumSumParaByAttr(String attrCode) {
            ParaVarImpl tmpVar = algImpl.getSumSumParaByAttr(attrCode);
            return toParaVar(tmpVar);
        }

        public ParaVar getSumParaByAttr(String attrCode) {
            ParaVarImpl tmpVar = algImpl.getSumParaByAttr(attrCode);
            return toParaVar(tmpVar);
        }
    }

    /**
     * 鍒涘缓閮ㄤ欢鍙橀噺锛岀户鎵跨被鍙互閲嶈浇
     *
     * @param internalPartVar 鍐呴儴閮ㄤ欢鍙橀噺
     * @return 鍒涘缓鐨勯儴浠跺彉閲?
     */
    protected Object newPartVar(PartVarImpl internalPartVar) {
        PartVar partVar = new PartVar();
        partVar.setBase((Part) internalPartVar.getBase());
        partVar.qty = internalPartVar.getQty();
        partVar.isHidden = internalPartVar.getIsHidden();
        partVar.isSelected = internalPartVar.getIsSelected();
        return partVar;
    }

    /**
     * 鍒涘缓鍙傛暟鍙橀噺锛岀户鎵跨被鍙互閲嶈浇
     *
     * @param internalParaVar 鍐呴儴鍙傛暟鍙橀噺
     * @return 鍒涘缓鐨勫弬鏁板彉閲?
     */
    protected Object newParaVar(ParaVarImpl internalParaVar) {
        return toParaVar(internalParaVar);
    }

    /**
     * 灏嗗唴閮≒araVar杞崲涓烘祴璇曠敤鐨凱araVar
     *
     * @param internalParaVar 鍐呴儴鍙傛暟鍙橀噺
     * @return 杞崲鍚庣殑鍙傛暟鍙橀噺
     */
    public static ParaVar toParaVar(ParaVarImpl internalParaVar) {
        if (internalParaVar == null) {
            return null;
        }
        ParaVar paraVar = new ParaVar();
        paraVar.setBase(internalParaVar.getBase());
        paraVar.internal = internalParaVar;
        paraVar.value = internalParaVar.getValue();
        paraVar.isHidden = internalParaVar.getIsHidden();
        paraVar.optionSelectVars = internalParaVar.getOptionSelectVars();
        return paraVar;
    }

    public void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintInCompatible(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    public void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintCoDependent(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    public void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintRequires(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    public List<PartVar> getPartVars(String filtedConditionStr) {
        List<PartVarImpl> partVars = super.getInternalPartVars(filtedConditionStr);
        List<PartVar> result = new ArrayList<>();
        for (PartVarImpl partVar : partVars) {
            result.add((PartVar) newPartVar(partVar));
        }
        return result;
    }
}
