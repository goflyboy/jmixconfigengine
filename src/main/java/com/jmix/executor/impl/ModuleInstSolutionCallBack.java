package com.jmix.executor.impl;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.PriorityAttrValue;
import com.jmix.executor.imodel.PriorityAttrValueImpl;
import com.jmix.executor.imodel.PriorityConstraint;
import com.jmix.executor.imodel.PriorityType;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.OtherVar;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.executor.impl.algmodel.Var;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;

import com.google.ortools.sat.CpSolverSolutionCallback;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块实例解决方案回调类
 * 继承CpSolverSolutionCallback，用于处理约束求解过程中的解决方案回调
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {

    private final Module module;

    private final List<Var<?>> vars;

    private final List<ModuleInst> allSolutions = new ArrayList<>();

    // 第几个解
    private int solutionIndex = 0;

    // 其他变量映射
    private Map<String, OtherVar> otherVarMap;

    // 约束算法实现，用于查询优先级约束
    private ConstraintAlgImpl alg;

    // 优先级属性最优值列表
    private List<PriorityAttrValueImpl> optimalValues = new ArrayList<>();

    // 常量定义
    private static final String OTHER_VARIABLES_VALUE_KEY = "OTHER_VARIABLES_VALUE";

    private static final String OTHER_VARIABLES_MEMO_KEY = "OTHER_VARIABLES_MEMO";

    /**
     * 构造函数
     * 
     * @param module 模块对象
     * @param vars   变量列表
     */
    public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars) {
        this.vars = vars != null ? vars : Collections.emptyList();
        this.module = module;
    }

    /**
     * 构造函数
     * 
     * @param module      模块对象
     * @param vars        变量列表
     * @param otherVarMap 其他变量映射
     */
    public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars, Map<String, OtherVar> otherVarMap) {
        this(module, vars);
        this.otherVarMap = otherVarMap;
    }

    /**
     * 构造函数
     * 
     * @param module      模块对象
     * @param vars        变量列表
     * @param otherVarMap 其他变量映射
     * @param alg         约束算法实现
     */
    public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars, Map<String, OtherVar> otherVarMap,
            ConstraintAlgImpl alg) {
        this(module, vars, otherVarMap);
        this.alg = alg;
    }

    /**
     * 构造函数
     * 
     * @param module        模块对象
     * @param vars          变量列表
     * @param otherVarMap   其他变量映射
     * @param alg           约束算法实现
     * @param optimalValues 优先级属性最优值列表
     */
    public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars, Map<String, OtherVar> otherVarMap,
            ConstraintAlgImpl alg, List<PriorityAttrValueImpl> optimalValues) {
        this(module, vars, otherVarMap, alg);
        this.optimalValues = optimalValues != null ? optimalValues : new ArrayList<>();
    }

    @Override
    public void onSolutionCallback() {
        // 后续考虑多线程，需要解决对相同解重复遍历（如：通过校验), this.stopSearch()
        solutionIndex++;
        log.info("-------------varInfos-solutionIndex:{}----------- \n {}", solutionIndex);
        // 创建ModuleInst实例，instanceId从0开始
        ModuleInst moduleInst = createModuleInst(module, solutionIndex);

        for (Var<?> v : vars) {
            if (v instanceof ParaVar) {
                ParaVar pv = (ParaVar) v;
                ParaInst pi = toParaInst(pv);
                moduleInst.addParaInst(pi);
            } else if (v instanceof PartVar) {
                PartVar partVar = (PartVar) v;
                PartInst inst = toPartInst(partVar);
                moduleInst.addPartInst(inst);
            } else {
                log.error("Unsupported variable type: {}", v.getClass().getSimpleName());
                throw new AlgLoaderException("Unsupported variable type: " + v.getClass().getSimpleName());
            }
        }

        // 处理其他变量
        processOtherVariables(moduleInst);

        // 处理优先级值
        processPriorityValues(moduleInst);

        allSolutions.add(moduleInst);
    }

    /**
     * 获取所有解决方案
     * 
     * @return 所有解决方案列表
     */
    public List<ModuleInst> getAllSolutions() {
        return allSolutions;
    }

    /**
     * 将ParaVar转换为ParaInst
     * 
     * @param pv ParaVar对象
     * @return ParaInst实例
     */
    private ParaInst toParaInst(ParaVar pv) {
        ParaInst pi = new ParaInst();
        pi.setCode(pv.getCode());
        // 从模块中获取对应的Para模型，设置shortCode
        pi.setShortCode(pv.getBase().getShortCode());

        // value: read IntVar domain value
        int value = (int) value(pv.getValue());
        pi.setValue(String.valueOf(value));

        // options: selected option codes
        List<String> options = new ArrayList<>();
        pv.getOptionSelectVars().forEach((codeId, optionVar) -> {
            long sel = value(optionVar.getIsSelectedVar());
            if (sel == 1L) {
                options.add(optionVar.getCode());
            }
        });
        pi.setOptions(options);
        pi.setHidden((int) value(pv.getIsHidden()) == 1);

        return pi;
    }

    /**
     * 将PartVar转换为PartInst
     * 
     * @param partVar PartVar对象
     * @return PartInst实例
     */
    private PartInst toPartInst(PartVar partVar) {
        PartInst inst = new PartInst();
        inst.setCode(partVar.getCode());
        // 从模块中获取对应的Part模型，设置shortCode
        inst.setShortCode(partVar.getBase().getShortCode());
        inst.setQuantity((int) value(partVar.getQty()));
        inst.setSelected(partVar.getIsSelected() != null && value(partVar.getIsSelected()) == 1);
        return inst;
    }

    /**
     * 处理其他变量，将otherVarMap中的变量值添加到ModuleInst的扩展属性中
     * 
     * @param moduleInst 模块实例对象
     */
    private void processOtherVariables(ModuleInst moduleInst) {
        // 如果isLogVariables为true，则增加根据otherVarMap值的获取,组织otherVarKeyMap
        if (otherVarMap != null && !otherVarMap.isEmpty()) {
            Map<String, Long> otherVarKeyMap = new HashMap<>();
            for (Map.Entry<String, OtherVar> entry : otherVarMap.entrySet()) {
                OtherVar otherVar = entry.getValue();
                long varValue = value(otherVar.getVar());
                otherVarKeyMap.put(otherVar.getShortCode(), varValue);
            }
            moduleInst.getExtAttrs().put(OTHER_VARIABLES_VALUE_KEY, otherVarKeyMap);
            moduleInst.getExtAttrs().put(OTHER_VARIABLES_MEMO_KEY, otherVarMap);
        }
    }

    /**
     * 处理优先级值
     * 根据 ModuleInst 中的部件实例计算每个优先级约束的值
     * 
     * @param moduleInst 模块实例
     */
    private void processPriorityValues(ModuleInst moduleInst) {
        if (optimalValues == null || optimalValues.isEmpty()) {
            log.info("optimalValues is null or empty, cannot process priority values");
            return;
        }

        List<PriorityAttrValue> priorityAttrValues = new ArrayList<>();

        for (PriorityAttrValueImpl optimalValue : optimalValues) {
            PriorityConstraint pConstraint = optimalValue.getPConstraint();
            List<Integer> exprVariablesValues = new ArrayList<>();
            PriorityType priorityType = pConstraint.getPriorityType();

            // 遍历表达式变量
            for (PriorityConstraint.PartTerm exprVariable : pConstraint.getExprVariables()) {
                PartInst pInst = moduleInst.queryPart(exprVariable.getPartCode());
                if (pInst == null) {
                    exprVariablesValues.add(0);
                } else {
                    if (priorityType == PriorityType.SELECT) {
                        exprVariablesValues.add(pInst.isSelected() ? 1 : 0);
                    } else {
                        exprVariablesValues.add(pInst.getQuantity() != null ? pInst.getQuantity() : 0);
                    }
                }
            }

            // 计算最优值
            double calculatedValue = PriorityConstraint.calcToString(pConstraint, exprVariablesValues);

            // 创建 PriorityAttrValueImpl
            PriorityAttrValue priorityAttrValue = new PriorityAttrValue();
            priorityAttrValue.setAttrCode(pConstraint.getAttrCode());
            priorityAttrValue.setOptimalValue(calculatedValue);
            priorityAttrValues.add(priorityAttrValue);
            log.info("Priority:PA_{}: {} = {}", optimalValue.getAttrCode(),
                    PriorityConstraint.instanceExprTemplateStr(pConstraint.getExprTemplateStr(), exprVariablesValues),
                    calculatedValue);
        }

        // 设置到 ModuleInst
        moduleInst.setPriorityAttrValues(priorityAttrValues);

        // 计算综合值（平均值）
        if (!priorityAttrValues.isEmpty()) {
            double sum = 0.0;
            for (PriorityAttrValue value : priorityAttrValues) {
                sum += value.getOptimalValue();
            }
            double overallValue = sum / priorityAttrValues.size();
            moduleInst.setPriorityOverallValue(overallValue);
        } else {
            moduleInst.setPriorityOverallValue(0.0);
        }
    }

    /**
     * 创建ModuleInst实例
     * 
     * @param module     模块对象
     * @param instanceId 实例ID，默认为0
     * @return ModuleInst实例
     */
    private static ModuleInst createModuleInst(Module module, int instanceId) {
        ModuleInst moduleInst = new ModuleInst();
        moduleInst.setId(module.getId());
        moduleInst.setCode(module.getCode());
        // 设置ModuleInst的shortCode
        moduleInst.setShortCode(module.getShortCode());
        moduleInst.setInstanceId(instanceId);
        return moduleInst;
    }
}
