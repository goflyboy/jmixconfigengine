package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ParConstraint;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块算法实现类
 * 实现ConstraintAlg接口，提供约束求解的具体实现
 * 继承自ModuleBaseAlgImpl，支持模块级别和部件分类级别的求和操作
 *
 * @since 2025-04-05
 */
@Slf4j
public class ModuleAlgImpl extends ModuleBaseAlgImpl {

    /**
     * 部件分类算法实例映射表
     */
    protected Map<String, PartCategoryAlgImpl> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 初始化模块算法实例
     * 按partCategoryCode对partConstraintFromReqs进行分组，然后初始化本层和子层的变量与规则
     *
     * @param model                    CP约束模型
     * @param module                   模块对象
     * @param partConstraintFromReqs   来自请求的部件约束列表
     */
    public void init(AlgCPModel model, IModule module, List<ParConstraint> partConstraintFromReqs) {
        // Module级别
        this.model = model;
        this.module = module;

        // 初始化兼容性约束算法实例
        this.compatibleConstraintAlg = new CompatibleConstraintAlg(model);
        initModelAfter(model);

        // 构建规则方法映射
        Map<String, Method> allRuleMethods = buildAllRuleMethods((Module) module);

        // 按partCategoryCode对partConstraintFromReqs进行分组
        Map<String, List<ParConstraint>> partConstraintFromReqMap = groupConstraintsByPartCategory(partConstraintFromReqs);

        // 初始化本层变量（paras和parts）
        initAll();

        // 设置输入变量
        setInputVariables(partConstraintFromReqs);

        // 设置默认可见性约束
        setDefaultVisibilityConstraints();

        // 将变量写回字段
        try {
            writeBackToFields();
        } catch (AlgLoaderException e) {
            log.error("Failed to write back variables to fields", e);
            throw new AlgLoaderException("Failed to write back variables to fields", e);
        }

        // 获取本层的规则
        List<Rule> moduleRules = filterRulesByFatherCode(module.getAllRules(), null);

        // 执行本层的规则
        for (Rule rule : moduleRules) {
            String ruleCode = rule.getCode();
            Method method = allRuleMethods.get(ruleCode);
            if (method != null) {
                executeRuleMethod(ruleCode, method);
            }
        }

        // 对本层的paras和parts执行writeBackToFields
        writeBackPartAndParaVars();

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        if (module instanceof Module) {
            Module bModule = (Module) module;
            if (bModule.getPartCategorys() != null && !bModule.getPartCategorys().isEmpty()) {
                for (PartCategory partCategory : bModule.getPartCategorys()) {
                    String categoryCode = partCategory.getCode();
                    List<ParConstraint> pc4PartConstraintFromReqs = partConstraintFromReqMap.get(categoryCode);

                    PartCategoryAlgImpl pcAlg = new PartCategoryAlgImpl();
                    pcAlg.init(model, partCategory, pc4PartConstraintFromReqs, allRuleMethods);

                    // 执行PartCategoryAlgImpl的规则
                    pcAlg.initRule(allRuleMethods);

                    partCategoryAlgs.put(categoryCode, pcAlg);
                }
            }
        }

        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    /**
     * 按partCategoryCode对partConstraintFromReqs进行分组
     *
     * @param partConstraintFromReqs 部件约束列表
     * @return 按partCategoryCode分组的映射
     */
    private Map<String, List<ParConstraint>> groupConstraintsByPartCategory(List<ParConstraint> partConstraintFromReqs) {
        Map<String, List<ParConstraint>> result = new LinkedHashMap<>();
        if (partConstraintFromReqs == null || partConstraintFromReqs.isEmpty()) {
            return result;
        }

        for (ParConstraint constraint : partConstraintFromReqs) {
            if (constraint == null || constraint.getFilteredCategory() == null) {
                continue;
            }
            String categoryCode = constraint.getFilteredCategory().getCode();
            result.computeIfAbsent(categoryCode, k -> new java.util.ArrayList<>()).add(constraint);
        }
        return result;
    }

    /**
     * 根据module.getAllRules()构建所有规则方法的映射
     *
     * @param module 模块对象
     * @return 规则代码到方法对象的映射
     */
    protected Map<String, Method> buildAllRuleMethods(Module module) {
        if (module == null || module.getAllRules() == null) {
            return new HashMap<>();
        }

        // 获取当前类的所有方法
        Method[] methods = this.getClass().getDeclaredMethods();
        Map<String, Method> allMethods = new HashMap<>();

        // 构建方法名到Method对象的映射
        for (Method method : methods) {
            allMethods.put(method.getName(), method);
        }

        // 根据module.getAllRules()来构建ruleMethods
        Map<String, Method> ruleMethods = new HashMap<>();
        for (Rule rule : module.getAllRules()) {
            String ruleCode = rule.getCode();
            Method method = allMethods.get(ruleCode);
            if (method != null) {
                ruleMethods.put(ruleCode, method);
                log.info("Built rule method mapping: {} -> {}", ruleCode, method.getName());

                // 检查是否是PriorityRule，如果是则构建优先级约束
                if (com.jmix.executor.bmodel.logic.RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName())) {
                    buildPriorityConstraint(rule);
                }
            } else {
                log.warn("Rule method not found for rule code: {} in class {}", ruleCode, this.getClass().getName());
            }
        }

        log.info("Built {} rule methods from module rules", ruleMethods.size());
        return ruleMethods;
    }

    /**
     * 过滤出指定fatherCode的规则
     *
     * @param rules     所有规则列表
     * @param fatherCode 父级代码，如果为null则获取模块级别的规则
     * @return 过滤后的规则列表
     */
    private List<Rule> filterRulesByFatherCode(List<Rule> rules, String fatherCode) {
        if (rules == null) {
            return new java.util.ArrayList<>();
        }
        return rules.stream()
                .filter(rule -> {
                    if (fatherCode == null) {
                        return rule.getFatherCode() == null || rule.getFatherCode().isEmpty();
                    }
                    return fatherCode.equals(rule.getFatherCode());
                })
                .collect(Collectors.toList());
    }

    /**
     * 将变量写回字段，保证规则使用变量是同一个
     *
     * @throws AlgLoaderException 异常
     */
    protected void writeBackToFields() throws AlgLoaderException {
        Map<String, Field> fieldMap = getAllFieldVariables();

        // 处理PartVar
        for (Map.Entry<String, PartVar> entry : partMap.entrySet()) {
            String code = entry.getKey();
            PartVar partVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Var<?> tVar = newPartVar(partVar);
                setVariableField(tVar, field);
            }
        }

        // 处理ParaVar
        for (Map.Entry<String, ParaVar> entry : paraMap.entrySet()) {
            String code = entry.getKey();
            ParaVar paraVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Var<?> tVar = newParaVar(paraVar);
                setVariableField(tVar, field);
            }
        }
    }

    /**
     * 将partMap和paraMap中的变量写回到对应的字段
     */
    private void writeBackPartAndParaVars() {
        Map<String, Field> fieldMap = getAllFieldVariables();

        // 处理PartVar
        for (Map.Entry<String, PartVar> entry : partMap.entrySet()) {
            String code = entry.getKey();
            PartVar partVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                try {
                    field.set(this, partVar);
                    log.debug("Wrote back PartVar to field: {}", code);
                } catch (IllegalAccessException e) {
                    log.error("Failed to write back PartVar to field: {}", code, e);
                }
            }
        }

        // 处理ParaVar
        for (Map.Entry<String, ParaVar> entry : paraMap.entrySet()) {
            String code = entry.getKey();
            ParaVar paraVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                try {
                    field.set(this, paraVar);
                    log.debug("Wrote back ParaVar to field: {}", code);
                } catch (IllegalAccessException e) {
                    log.error("Failed to write back ParaVar to field: {}", code, e);
                }
            }
        }
    }

    /**
     * 获取部件分类算法实例
     *
     * @param categoryCode 部件分类代码
     * @return PartCategoryAlgImpl实例
     */
    public PartCategoryAlgImpl getPartCategoryAlg(String categoryCode) {
        return partCategoryAlgs.get(categoryCode);
    }

    /**
     * 获取所有部件分类算法实例
     *
     * @return 部件分类算法映射表
     */
    public Map<String, PartCategoryAlgImpl> getPartCategoryAlgs() {
        return partCategoryAlgs;
    }

    /**
     * 获取Module对象
     *
     * @return Module对象
     */
    public Module getModule() {
        return (Module) super.getModule();
    }

    /**
     * 获取所有部件变量
     *
     * @return 部件变量映射
     */
    public Map<String, PartVar> getPartMap() {
        return partMap;
    }

    /**
     * 获取所有参数变量
     *
     * @return 参数变量映射
     */
    public Map<String, ParaVar> getParaMap() {
        return paraMap;
    }
}
