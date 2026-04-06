package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ParConstraint;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class PartCategoryAlgImpl extends ModuleBaseAlgImpl {

    private PartCategory partCategory;

    /**
     * 默认构造函数
     */
    public PartCategoryAlgImpl() {
        super();
    }

    /**
     * 构造函数
     *
     * @param partCategory 部件分类对象
     * @param model        CP模型
     * @param varMap       变量映射表
     */
    public PartCategoryAlgImpl(PartCategory partCategory, AlgCPModel model, Map<String, Var<?>> varMap) {
        this.partCategory = partCategory;
        this.model = model;
        this.varMap = varMap;
    }

    /**
     * 初始化部件分类算法实例
     * Module级别初始化
     *
     * @param model                    CP约束模型
     * @param module                   模块对象（PartCategory）
     * @param partConstraintFromReqs   来自请求的部件约束列表
     */
    public void init(AlgCPModel model, PartCategory module, List<ParConstraint> partConstraintFromReqs) {
        init(model, module, partConstraintFromReqs, null);
    }

    /**
     * 初始化部件分类算法实例
     *
     * @param model                    CP约束模型
     * @param module                   模块对象（PartCategory）
     * @param partConstraintFromReqs   来自请求的部件约束列表
     * @param allRuleMethods           所有规则方法的映射表
     */
    public void init(AlgCPModel model, PartCategory module, List<ParConstraint> partConstraintFromReqs,
                     Map<String, Method> allRuleMethods) {
        this.model = model;
        this.partCategory = module;
        this.module = module;

        // 初始化兼容性约束算法实例
        this.compatibleConstraintAlg = new CompatibleConstraintAlg(model);

        // 初始化本层变量
        initAll();

        // 设置输入变量
        setInputVariables(partConstraintFromReqs);

        // 设置默认可见性约束
        setDefaultVisibilityConstraints();

        // 将变量写回字段
        try {
            writeBackToFields();
        } catch (AlgLoaderException e) {
            log.error("Failed to write back variables to fields for PartCategory: {}", module.getCode(), e);
        }

        log.info("PartCategoryAlgImpl initialized for category: {}", partCategory.getCode());
    }

    /**
     * 初始化并执行规则
     * 使用外部传入的 allRuleMethods 映射来选择要执行的规则
     *
     * @param allRuleMethods 所有规则方法的映射表 (ruleCode -> Method)
     */
    public void initRule(Map<String, Method> allRuleMethods) {
        if (allRuleMethods == null || allRuleMethods.isEmpty()) {
            log.warn("allRuleMethods is null or empty, skip initRule");
            return;
        }

        if (partCategory == null || partCategory.getAllRules() == null) {
            log.warn("PartCategory or partCategory.getAllRules() is null, skip initRule");
            return;
        }

        // 获取本分类下的规则（fatherCode等于本分类的code）
        List<Rule> categoryRules = filterRulesByFatherCode(partCategory.getAllRules(), partCategory.getCode());

        for (Rule rule : categoryRules) {
            String ruleCode = rule.getCode();
            Method ruleMethod = allRuleMethods.get(ruleCode);
            if (ruleMethod != null) {
                ruleMethods.put(ruleCode, ruleMethod);
                log.info("Executing rule in PartCategory {}: {} -> {}", partCategory.getCode(), ruleCode, ruleMethod.getName());

                // 执行规则方法
                executeRuleMethod(ruleCode, ruleMethod);
            } else {
                log.warn("Rule method not found in allRuleMethods for rule code: {} in PartCategory {}", ruleCode, partCategory.getCode());
            }
        }
        log.info("Executed {} rules for PartCategory {}", categoryRules.size(), partCategory.getCode());
    }

    @Override
    protected List<Part> getParts4Sum() {
        return partCategory.getAtomicParts();
    }

    public PartCategory getPartCategory() {
        return partCategory;
    }

    @Override
    public PartCategory getModule() {
        return partCategory;
    }

    public String getCategoryCode() {
        return partCategory != null ? partCategory.getCode() : null;
    }
}
