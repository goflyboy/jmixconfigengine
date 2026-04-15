package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ParaVar extends Var<Para> {
    /**
     * 参数变量命名模式常量
     */
    public static final String VAR_PATTERN_PREFIX = "##Para.";

    /**
     * 参数值Var的命名模式
     */
    public static final String VALUE_PATTERN = VAR_PATTERN_PREFIX + "value.%s%s";

    /**
     * 显示隐藏Var的命名模式
     */
    public static final String HIDDEN_PATTERN = VAR_PATTERN_PREFIX + "isHidden.%s%s";

    /**
     * 参数选项Var的命名模式
     */
    public static final String OPTIONS_PATTERN = VAR_PATTERN_PREFIX + "options.%s%s.%s";

    /**
     * 参数值Var的短名称
     */
    public static final String VALUE_SHORT_NAME = "V";

    /**
     * 显示隐藏Var的短名称
     */
    public static final String HIDDEN_SHORT_NAME = "H";

    /**
     * 参数选项Var的短名称
     */
    public static final String OPTIONS_SHORT_NAME = "O";
    /**
     * 实例ID
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

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
     * 手工输入值
     */
    private Integer inputValue = null;

    /**
     * 是否已输入
     */
    private Boolean hasInputed = false;

    /**
     * 设置是否已输入（兼容旧代码）
     * 
     * @param hasInputed 是否已输入
     */
    public void setIsHasInputed(Boolean hasInputed) {
        this.hasInputed = hasInputed;
    }

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
        log.error("ParaOptionVar not found for code: {}", code);
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
        sb.append(",value=").append(this.value == null ? "null" : solutionCallback.value(this.value));
        sb.append(", isHidden=").append(this.isHidden == null ? "null" : solutionCallback.value(this.isHidden));

        // 打印每个option的getVarString
        sb.append(", options=");
        for (ParaOptionVar option : optionSelectVars.values()) {
            sb.append(option.getVarString(solutionCallback)).append(",");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        // P1(V:1,H:0)
        return String.format("%s(%s:%s,%s:%s)", getCode(), VALUE_SHORT_NAME,
                this.value == null ? "null" : solutionCallback.value(this.value), HIDDEN_SHORT_NAME,
                this.isHidden == null ? "null" : solutionCallback.value(this.isHidden));
    }
}