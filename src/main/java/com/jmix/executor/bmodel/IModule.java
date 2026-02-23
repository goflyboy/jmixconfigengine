package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.logic.Rule;

import java.util.List;

/**
 * 模块接口
 * 定义了模块的基本操作，包括获取参数、部件和规则
 *
 * @since 2025-01-XX
 */
public interface IModule extends IOnto {

    /**
     * 根据编码获取部件对象
     *
     * @param code 部件编码
     * @return 部件对象，如果不存在则返回Optional.empty()
     */
    IPart getPart(String code);

    /**
     * 获取所有规则列表（包括子分类中的规则）
     * 递归收集所有子分类中的规则
     *
     * @return 所有规则列表
     */
    List<Rule> getAllRules();

    /**
     * 获取部件列表
     *
     * @return 部件列表
     */
    List<IPart> getAllParts();

    /**
     * 获取原子部件列表
     *
     * @return 原子部件列表（partType为ATOMIC的部件）
     */
    List<Part> getAllAtomicParts();

    /**
     * 查询所有优先级规则
     *
     * @return 优先级规则列表
     */
    List<Rule> queryPriorityRules();

}
