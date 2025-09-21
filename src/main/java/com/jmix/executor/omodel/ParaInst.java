package com.jmix.executor.omodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 参数实例类
 */
@Data
public class ParaInst {

    private String code;

    // 空表示没有赋值
    private String value;

    // 空表示没有赋值
    private List<String> options;

    private boolean isHidden = false;

    private Map<String, Object> extAttrs;

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

    /**
     * 生成短字符串表示
     * 
     * @return 短字符串，例如：P1(V:1,H:0)
     */
    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(shortCode != null ? shortCode : code).append("(");

        // 添加值信息
        if (value != null) {
            sb.append("V:").append(value);
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
