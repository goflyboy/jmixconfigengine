package com.jmix.executor.bmodel;

import lombok.Data;

/**
 * 属性Schema
 * 定义属性的结构和约束
 * 
 * @since 2025-09-22
 */
@Data
public class AttrSchema {
    /**
     * 属性编码
     */
    private String code;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性类型
     */
    private String type;

    /**
     * 属性描述
     */
    private String description;
}