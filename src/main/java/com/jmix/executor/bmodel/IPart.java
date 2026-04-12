package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.IDynamicAttributable;
import com.jmix.executor.bmodel.base.IExtensible;
import com.jmix.executor.bmodel.base.Programmable;

/**
 * Part接口
 * 定义了模块的基本操作，包括获取参数、部件和规则
 * 
 * @since 2025-01-XX
 */
public interface IPart extends IExtensible, IDynamicAttributable, Programmable {
    /**
     * 获取部件类型
     * 
     * @return
     */
    PartType getPartType();

    /**
     * 设置部件类型
     * 
     * @param partType
     */
    void setPartType(PartType partType);

    /**
     * 获取部件代码
     * 
     * @return
     */
    String getCode();

    /**
     * 设置部件代码
     * 
     * @param code
     */
    void setCode(String code);

    /**
     * 获取父部件代码
     * 
     * @return
     */
    String getFatherCode();

    /**
     * 设置父部件代码
     * 
     * @param fatherCode
     */
    void setFatherCode(String fatherCode);

    /**
     * 获取部件短代码
     * 
     * @return
     */
    String getShortCode();

    /**
     * 获取是否支持多实例
     * 用于约束算法的多实例处理，默认为false
     * 
     * @return 是否支持多实例
     */
    boolean isSupportMultiInst();

    /**
     * 设置是否支持多实例
     * 
     * @param supportMultiInst 是否支持多实例
     */
    void setSupportMultiInst(boolean supportMultiInst);
}
