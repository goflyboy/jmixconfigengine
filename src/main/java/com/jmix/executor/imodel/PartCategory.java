package com.jmix.executor.imodel;

/**
 * 部件分类
 * 表示部件的分类定义，继承自Part
 *
 * @since 2025-12-27
 */
public class PartCategory extends Part {

    public PartCategory() {
        super();
        this.setPartType(PartType.CATEGORY);
    }
}
