package com.jmix.executor.omodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 部件实例类
 * 
 * @since 2025-9-21
 */
@Data
@NoArgsConstructor
public class PartInst {

    /**
     * 部件编码
     */
    private String code;

    /**
     * 部件值，空表示没有赋值
     */
    private String value;

    /**
     * 部件选项列表，空表示没有赋值
     */
    private List<String> options;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 选择属性值映射
     */
    private Map<String, String> selectAttrValue;

    /**
     * 是否隐藏
     */
    private boolean isHidden = false;

    /**
     * 扩展属性
     */
    private Map<String, Object> extAttrs;

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

    public PartInst(String code, int quantity) {
        this.code = code;
        this.quantity = quantity;
    }

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
