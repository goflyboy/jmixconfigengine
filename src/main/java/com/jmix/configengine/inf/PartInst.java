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
    public Map<String,Object> extAttrs;
    
    /**
     * 生成短字符串表示
     * @return 短字符串，例如：PT1(Q:20,H:0)
     */
    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(code).append("(");
        
        // 添加数量信息
        if (quantity != null) {
            sb.append("Q:").append(quantity);
        }
        
        // 添加隐藏状态
        if (sb.length() > code.length() + 1) sb.append(",");
        sb.append("H:").append(isHidden ? 1 : 0);
        
        sb.append(")");
        return sb.toString();
    }
}
