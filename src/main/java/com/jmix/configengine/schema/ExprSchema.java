package com.jmix.configengine.schema;

import lombok.Data;
import java.util.List;

/**
 * 表达式Schema
 */
@Data
public class ExprSchema {
    /**
     * 原始代码
     */
    private String rawCode;
    
    /**
     * 引用的编程对象
     */
    private List<RefProgObjSchema> refProgObjs;
} 