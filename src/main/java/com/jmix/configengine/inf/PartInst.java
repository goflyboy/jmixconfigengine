package com.jmix.configengine.inf;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * 部件实例类
 */
@Data
public class PartInst {
    public String code;
    public Integer quantity;
    public Map<String, String> selectAttrValue;
    public boolean isHidden = false;
    public Map<String, Object> extAttrs;

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

    /**
     * 生成短字符串表示
     * 
     * @return 短字符串，例如：PT1(Q:20,H:0)
     */
    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(shortCode != null ? shortCode : code).append("(");

        // 添加数量信息
        if (quantity != null) {
            sb.append("Q:").append(quantity);
        }

        // 添加隐藏状态
        if (sb.length() > (shortCode != null ? shortCode : code).length() + 1) {
            sb.append(",");
        }
        sb.append("H:").append(isHidden ? 1 : 0);

        sb.append(")");
        return sb.toString();
    }
}
