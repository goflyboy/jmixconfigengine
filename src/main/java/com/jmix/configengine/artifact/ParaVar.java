package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar; 

/**
 * 参数变量
 */
@Data
public class ParaVar {
    /**
     * 参数编码
     */
    public String code;
    
    /**
     * 参数值状态
     */
    public IntVar var;
    
    /**
     * 显示隐藏属性
     */
    public BoolVar isHiddenVar;
    
    /**
     * 参数可选值的选中状态(CodeId -> ParaOptionVar)
     */
    public Map<Integer, ParaOptionVar> optionSelectVars = new HashMap<>();

    public ParaOptionVar getParaOptionByCodeId(Integer codeId) {
        return optionSelectVars.get(codeId);
    }

    public ParaOptionVar getParaOptionByCode(String code) {
        // 遍历optionSelectVars，找到code相同的ParaOptionVar
        for (ParaOptionVar option : optionSelectVars.values()) {
            if (option.getCode().equals(code)) {
                return option;
            }
        }
        throw new RuntimeException("ParaOptionVar not found for code: " + code);
    }
    public List<ParaOptionVar> getParaOptionByCodes(List<String> codes) {
        return codes.stream()
                .map(this::getParaOptionByCode)
                .collect(Collectors.toList());
    }
    public List<ParaOptionVar> getParaOptionByExcusiveCodes(List<String> exclusiveCodes) {
        return optionSelectVars.values().stream()
                .filter(option -> !exclusiveCodes.contains(option.getCode()))
                .collect(Collectors.toList());
    }
} 