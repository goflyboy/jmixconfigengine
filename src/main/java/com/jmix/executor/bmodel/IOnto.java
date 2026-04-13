package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.IDynamicAttributable;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;

import java.util.List;
import java.util.Optional;

/**
 * 本体接口
 * Part和Module的公共接口，包含公共属性和方法
 *
 * @since 2025-01-XX
 */
public interface IOnto extends IDynamicAttributable {
    /**
     * 获取编码
     *
     * @return 编码
     */
    String getCode();

    /**
     * 获取参数列表
     *
     * @return 参数列表
     */
    List<Para> getParas();

    /**
     * 根据编码获取参数对象
     *
     * @param code 参数编码
     * @return 参数对象，如果不存在则返回Optional.empty()
     */
    Optional<Para> getPara(String code);

    /**
     * 获取规则列表
     *
     * @return 规则列表
     */
    List<Rule> getRules();

    /**
     * 根据编码获取规则对象
     *
     * @param code 规则编码
     * @return 规则对象，如果不存在则返回Optional.empty()
     */
    Optional<Rule> getRule(String code);

    /**
     * 检查是否有优先级规则
     *
     * @return 如果有优先级规则则返回true，否则返回false
     */
    boolean hasPriorityRule();

    /**
     * 获取所有优先级规则
     *
     * @return 优先级规则列表
     */
    List<Rule> getPriorityRules();

    /**
     * 获取指定计算阶段的规则列表
     *
     * @param calcStage 计算阶段
     * @return 指定计算阶段的规则列表
     */
    List<Rule> getRules(CalcStage calcStage);
}
