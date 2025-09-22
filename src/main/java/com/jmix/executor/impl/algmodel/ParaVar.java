package com.jmix.executor.impl.algmodel;

import com.jmix.executor.imodel.Para;
import com.jmix.executor.omodel.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 参数变量
 * 表示约束求解中的参数变量，包含值、选项和隐藏状态
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaVar extends Var<Para> {
    /**
     * 参数变量命名模式常量
     */
    public static final String VAR_PATTEN_PREFIX = "##Para.";

    public static final String VALUE_PATTEN = VAR_PATTEN_PREFIX + "value.%s"; // ##Para.value.{code}

    public static final String HIDDEN_PATTEN = VAR_PATTEN_PREFIX + "isHidden.%s"; // ##Para.isHidden.{code}

    public static final String OPTIONS_PATTEN = VAR_PATTEN_PREFIX + "options.%s.%s"; // ##Para.options.{code}.{optionCode}

    public static final String VALUE_SHORT_NAME = "V";

    public static final String HIDDEN_SHORT_NAME = "H";

    /**
     * 参数值状态
     */
    private IntVar value;

    /**
     * 兼容旧模型的隐藏布尔变量
     * 0-显示，1-隐藏
     */
    private BoolVar isHidden;

    /**
     * 参数可选值的选中状态(CodeId -> ParaOptionVar)
     */
    private Map<Integer, ParaOptionVar> optionSelectVars = new HashMap<>();

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

    /**
     * 根据代码列表获取参数选项变量列表
     * 
     * @param codes 选项代码列表
     * @return 对应的参数选项变量列表
     */
    public List<ParaOptionVar> getParaOptionByCodes(List<String> codes) {
        return codes.stream()
                .map(this::getParaOptionByCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取排除指定代码外的所有参数选项变量
     * 
     * @param exclusiveCodes 要排除的选项代码列表
     * @return 排除指定代码后的参数选项变量列表
     */
    public List<ParaOptionVar> getParaOptionByExclusiveCodes(List<String> exclusiveCodes) {
        return optionSelectVars.values().stream()
                .filter(option -> !exclusiveCodes.contains(option.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("Para{code=").append(getCode());
        if (value != null) {
            sb.append(",value=").append(solutionCallback.value((IntVar) this.value));

        }
        if (isHidden != null) {
            sb.append(", isHidden=").append(solutionCallback.value(this.isHidden));
        }
        if (!optionSelectVars.isEmpty()) {
            // 打印每个option的getVarString
            sb.append(", options=");
            for (ParaOptionVar option : optionSelectVars.values()) {
                sb.append(option.getVarString(solutionCallback)).append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        // P1(V:1,H:0)
        return String.format("%s(%s:%s,%s:%s)", getCode(), VALUE_SHORT_NAME,
                solutionCallback.value((IntVar) this.value), HIDDEN_SHORT_NAME, solutionCallback.value(this.isHidden));
    }
}