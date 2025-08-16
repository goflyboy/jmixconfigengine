package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 部件变量
 */
@Data
public class PartVar {
    /**
     * 部件编码
     */
    public String code;
    
    /**
     * 部件的数量值
     */
    public Object var;
    
    /**
     * 显示隐藏属性
     */
    public Object isHiddenVar;
    
    /**
     * 子部件选中状态
     */
    public Map<Integer, Object> subPartSelectedVars = new HashMap<>();
} 