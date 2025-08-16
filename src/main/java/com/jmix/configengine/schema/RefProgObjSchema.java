package com.jmix.configengine.schema;

import lombok.Data;

/**
 * 编程对象引用Schema
 */
@Data
public class RefProgObjSchema {
    /**
     * 可编程对象类型
     */
    private String progObjType;
    
    /**
     * 可编程对象编码
     */
    private String progObjCode;
    
    /**
     * 可编程对象属性
     */
    private String progObjField;
} 