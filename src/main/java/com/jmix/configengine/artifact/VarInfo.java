package com.jmix.configengine.artifact;

import lombok.Data;
import com.jmix.configengine.model.Extensible;

/**
 * 变量信息基类
 */
@Data
public class VarInfo<T extends Extensible> {
    /**
     * 变量名称
     */
    public String varName;
    
    /**
     * 扩展基础信息
     */
    public T base;
    
    /**
     * 构造函数
     */
    public VarInfo() {
        // 注意：泛型类型T不能直接实例化，所以这里不设置base
    }
    
    /**
     * 构造函数
     */
    public VarInfo(T base) {
        this.base = base;
    }
} 