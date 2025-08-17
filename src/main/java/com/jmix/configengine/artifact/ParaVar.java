package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

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
    public Literal var;
    
    /**
     * 显示隐藏属性
     */
    public BoolVar isHiddenVar;
    
    /**
     * 参数可选值的选中状态(CodeId -> ParaOptionInfo)
     */
    public Map<Integer, ParaOptionInfo> optionSelectVars = new HashMap<>();

    public ParaOptionInfo getParaOptionByCodeId(Integer codeId) {
        return optionSelectVars.get(codeId);
    }

    public ParaOptionInfo getParaOptionByCode(String code) {
        // 遍历optionSelectVars，找到code相同的ParaOptionInfo
        for (ParaOptionInfo option : optionSelectVars.values()) {
            if (option.getCode().equals(code)) {
                return option;
            }
        }
        throw new RuntimeException("ParaOptionInfo not found for code: " + code);
    }
    public List<ParaOptionInfo> getParaOptionByCodes(List<String> codes) {
        return codes.stream()
                .map(this::getParaOptionByCode)
                .collect(Collectors.toList());
    }
    public List<ParaOptionInfo> getParaOptionByExcusiveCodes(List<String> exclusiveCodes) {
        return optionSelectVars.values().stream()
                .filter(option -> !exclusiveCodes.contains(option.getCode()))
                .collect(Collectors.toList());
    }
} 