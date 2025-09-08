package com.jmix.configengine.inf;

import lombok.Data;

import java.util.Map;

/**
 * 部件实例类
 */
@Data
public class PartInst {
    public String code;
    public Integer quantity;
    public Map<String,String> selectAttrValue;
    public boolean isHidden = false;
}
