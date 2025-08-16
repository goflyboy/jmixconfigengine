package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

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
    public Object var;
    
    /**
     * 显示隐藏属性
     */
    public Object isHiddenVar;
    
    /**
     * 参数可选值的选中状态
     */
    public Map<Integer, Object> optionSelectVars = new HashMap<>();
} 