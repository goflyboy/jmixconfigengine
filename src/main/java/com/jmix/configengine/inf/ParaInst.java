package com.jmix.configengine.inf;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 参数实例类
 */
@Data
public class ParaInst {
    public String code;
    public String value; // 空表示没有赋值
    public List<String> options; // 空表示没有赋值
    public boolean isHidden = false;
    public Map<String,Object> extAttrs;

}
