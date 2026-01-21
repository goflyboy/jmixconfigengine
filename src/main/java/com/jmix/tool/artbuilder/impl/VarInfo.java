package com.jmix.tool.artbuilder.impl;

import com.jmix.executor.bmodel.base.Extensible;

import lombok.Data;

/**
 * 变量信息基类
 * 所有变量信息类的基类，提供通用的变量信息结构
 * 
 * @param <T> 扩展对象类型
 * @since 2025-09-22
 */
@Data
public class VarInfo<T extends Extensible> {
    /**
     * 变量名称
     */
    private String varName;

    /**
     * 扩展基础信息
     */
    private T base;

    public VarInfo() {
        // 注意：泛型类型T不能直接实例化，所以这里不设置base
    }

    public VarInfo(T base) {
        this.base = base;
    }
}