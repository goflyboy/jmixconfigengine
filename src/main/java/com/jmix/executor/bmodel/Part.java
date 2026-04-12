package com.jmix.executor.bmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部件定义
 * 表示模块中的部件，支持原子部件和复合部件
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Part extends Onto implements IPart {
    /**
     * 部件最大数量限制
     */
    public static final int MAX_QUANTITY = 20;

    /**
     * 部件最小数量限制
     */
    public static final int MIN_QUANTITY = 0;

    /**
     * 部件短编码前缀
     */
    public static final String SHORT_CODE_PREFIX = "PT";

    /**
     * 部件类型
     */
    private PartType partType = PartType.ATOMIC;

    /**
     * 原子Part的目录价
     */
    private Long price = 0L;

    /**
     * 原子Part的最大数量
     */
    private Integer maxQuantity = MAX_QUANTITY;

    /**
     * 是否支持多实例
     * 用于约束算法的多实例处理，默认为false
     * 当设置为true时，该部件将支持多实例复制
     */
    private boolean supportMultiInst = false;

    /**
     * 获取默认数量
     * 
     * @return 默认数量
     */
    @JsonIgnore
    public Integer getDefaultQuantity() {
        return super.getDefaultValue();
    }

    /**
     * 克隆Part对象
     *
     * @return 克隆的Part对象
     */
    @JsonIgnore
    public Part clone() {
        Part to = new Part();
        super.clone(to);
        // 复制Part特有属性
        to.setPartType(this.getPartType());
        to.setPrice(this.getPrice());
        to.setMaxQuantity(this.getMaxQuantity());
        to.setSupportMultiInst(this.isSupportMultiInst());
        return to;
    }

}