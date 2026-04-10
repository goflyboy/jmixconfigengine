package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;

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
     * 获取所有参数列表
     *
     * @return 所有参数列表
     */
    List<Para> getAllParas();

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
     * 获取原子部件列表
     *
     * @return 原子部件列表（partType为ATOMIC的部件）
     */
    List<Part> getAtomicParts();

    /**
     * 根据部件分类编码获取原子部件列表
     *
     * @param partCategoryCode 部件分类编码
     * @return 指定分类下的原子部件列表
     */
    List<Part> getAllAtomicParts(String partCategoryCode);

    /**
     * 判断是否包含所有优先级规则
     *
     * @return 如果包含所有优先级规则返回true，否则返回false
     */
    boolean hasAllPriorityRule();

    /**
     * 根据分类编码查找部件分类
     *
     * @param categoryCode 部件分类编码
     * @return 部件分类对象，如果不存在则返回null
     */
    PartCategory findPartCategory(String categoryCode);

    /**
     * 查询所有优先级规则
     *
     * @return 优先级规则列表
     */
    List<Rule> queryPriorityRules();

    /**
     * 获取指定计算阶段的所有规则列表（包括子分类中的规则）
     * 递归收集所有子分类中的规则
     *
     * @param calcStage 计算阶段
     * @return 指定计算阶段的所有规则列表
     */
    List<Rule> getAllRules(CalcStage calcStage);

}
