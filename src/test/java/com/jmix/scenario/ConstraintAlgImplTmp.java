package com.jmix.scenario.hello;

import com.jmix.executor.imodel.Para;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVar;
import com.jmix.executor.impl.algmodel.Var;
import com.jmix.executor.omodel.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import java.util.HashMap;
import java.util.Map;

public class ConstraintAlgImplTmp extends ConstraintAlgImpl {

    // @Data
    // @EqualsAndHashCode(callSuper = true)
    static public class ParaVar extends Var<Para> {

        /**
         * 参数值状态
         */
        public IntVar value;

        /**
         * 兼容旧模型的隐藏布尔变量
         * 0-显示，1-隐藏
         */
        public BoolVar isHidden;

        /**
         * 参数可选值的选中状态(CodeId -> ParaOptionVar)
         */
        public Map<Integer, ParaOptionVar> optionSelectVars = new HashMap<>();

        /**
         * 根据代码ID获取参数选项变量
         * 
         * @param codeId 选项的代码ID
         * @return 对应的参数选项变量，如果不存在则返回null
         */
        public ParaOptionVar getParaOptionByCodeId(Integer codeId) {
            return optionSelectVars.get(codeId);
        }

        /**
         * 根据代码获取参数选项变量
         * 
         * @param code 选项的代码
         * @return 对应的参数选项变量
         * @throws AlgLoaderException 如果找不到对应的选项
         */
        public ParaOptionVar getParaOptionByCode(String code) {
            for (ParaOptionVar option : optionSelectVars.values()) {
                if (code != null && code.equals(option.getCode())) {
                    return option;
                }
            }
            throw new AlgLoaderException("ParaOptionVar not found for code: " + code);
        }
    }
}
