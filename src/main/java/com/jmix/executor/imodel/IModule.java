package com.jmix.executor.imodel;

import java.util.List;
import java.util.Optional;

/**
 * 模块接口
 * 定义了模块的基本操作，包括获取参数、部件和规则
 * 
 * @since 2025-01-XX
 */
public interface IModule {
    /**
     * 根据编码获取参数对象
     * 
     * @param code 参数编码
     * @return 参数对象，如果不存在则返回Optional.empty()
     */
    Optional<Para> getPara(String code);

    /**
     * 根据编码获取部件对象
     * 
     * @param code 部件编码
     * @return 部件对象，如果不存在则返回Optional.empty()
     */
    Optional<Part> getPart(String code);

    /**
     * 获取规则列表
     * 
     * @return 规则列表
     */
    List<Rule> getRules();

    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    List<Para> getParas();

    /**
     * 获取部件列表
     * 
     * @return 部件列表
     */
    List<Part> getParts();

    /**
     * 获取原子部件列表
     * 
     * @return 原子部件列表（partType为ATOMIC的部件）
     */
    List<Part> getAtomicParts();

    /**
     * 检查是否有优先级规则
     * 
     * @return 如果有优先级规则则返回true，否则返回false
     */
    boolean hasPriorityRule();

    /**
     * 查询所有优先级规则
     * 
     * @return 优先级规则列表
     */
    List<Rule> queryPriorityRules();

}
