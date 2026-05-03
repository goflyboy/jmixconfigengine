package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.Result;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块后置计算器
 * 在CP-SAT求解完成后对每个ModuleInst执行CalcStage.POST规则
 *
 * @since 2026-05-03
 */
@Slf4j
public class ModulePostCalculator {

    private final Module module;
    private final ModuleAlgImpl moduleAlg;
    private final List<PostRuleMethod> postRuleMethods;

    public ModulePostCalculator(Module module, ModuleAlgImpl moduleAlg) {
        this.module = module;
        this.moduleAlg = moduleAlg;
        this.postRuleMethods = scanPostRuleMethods();
    }

    /**
     * 对解列表批量执行POST规则
     *
     * @param solutions CP求解产生的解列表
     * @return 写入派生参数后的结果
     */
    public Result<List<ModuleInst>> doCalc(List<ModuleInst> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            return Result.success(solutions);
        }
        if (postRuleMethods.isEmpty()) {
            return Result.success(solutions);
        }

        for (int i = 0; i < solutions.size(); i++) {
            ModuleInst solution = solutions.get(i);
            ModuleInstAccessor accessor = new ModuleInstAccessorImpl(module, solution);
            moduleAlg.bindModuleInstAccessor(accessor);
            try {
                for (PostRuleMethod prm : postRuleMethods) {
                    invokePostRule(prm, i + 1);
                }
            } catch (RuntimeException ex) {
                return Result.failed("POST rule failed, solutionIndex=" + (i + 1)
                        + ", message=" + ex.getMessage());
            } finally {
                moduleAlg.clearModuleInstAccessor();
            }
        }
        return Result.success(solutions);
    }

    private void invokePostRule(PostRuleMethod prm, int solutionIndex) {
        try {
            prm.method.setAccessible(true);
            prm.method.invoke(moduleAlg);
            log.info("Executed POST rule: {} for solution {}", prm.rule.getCode(), solutionIndex);
        } catch (IllegalAccessException e) {
            throw new AlgLoaderException(
                    "Failed to access POST rule method: " + prm.method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new AlgLoaderException(
                    "POST rule failed: ruleCode=" + prm.rule.getCode()
                            + ", method=" + prm.method.getName()
                            + ", solutionIndex=" + solutionIndex
                            + ", message=" + cause.getMessage(), cause);
        }
    }

    /**
     * 扫描所有POST规则并建立Rule→Method映射
     */
    private List<PostRuleMethod> scanPostRuleMethods() {
        List<Rule> postRules = module.getAllRules(CalcStage.POST);
        if (postRules == null || postRules.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Method> allRuleMethods = buildAllRuleMethods();
        List<PostRuleMethod> result = new ArrayList<>();

        for (Rule rule : postRules) {
            Method method = allRuleMethods.get(rule.getCode());
            if (method == null) {
                throw new AlgLoaderException(
                        "POST rule method not found for rule code: " + rule.getCode()
                                + " in class " + moduleAlg.getClass().getName());
            }
            result.add(new PostRuleMethod(rule, method));
            log.info("Scanned POST rule: {} -> {}", rule.getCode(), method.getName());
        }

        log.info("Scanned {} POST rule methods", result.size());
        return result;
    }

    private Map<String, Method> buildAllRuleMethods() {
        Method[] methods = moduleAlg.getClass().getDeclaredMethods();
        Map<String, Method> allMethods = new HashMap<>();
        for (Method method : methods) {
            allMethods.put(method.getName(), method);
        }
        Map<String, Method> ruleMethods = new LinkedHashMap<>();
        for (Rule rule : module.getAllRules()) {
            Method method = allMethods.get(rule.getCode());
            if (method != null) {
                ruleMethods.put(rule.getCode(), method);
            }
        }
        return ruleMethods;
    }

    /**
     * 内部类：POST规则方法对
     */
    private static class PostRuleMethod {
        final Rule rule;
        final Method method;

        PostRuleMethod(Rule rule, Method method) {
            this.rule = rule;
            this.method = method;
        }
    }
}
