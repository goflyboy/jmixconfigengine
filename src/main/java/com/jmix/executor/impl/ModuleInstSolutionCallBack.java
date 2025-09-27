package com.jmix.executor.impl;

import com.jmix.executor.imodel.Module;
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

    @Override
    public void onSolutionCallback() {
        solutionIndex++;
        // 创建ModuleInst实例，instanceId从0开始
        ModuleInst moduleInst = createModuleInst(module, 0);

        for (Var<?> v : vars) {
            // 打印var.getVarString
            log.info("-------------varInfos-solutionIndex:{}----------- \n {}", solutionIndex,
                    v.getVarString(this));
            // 如果不是debug模式，则不打印Var的值
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
        moduleInst.setInstanceConfigId("0");
        moduleInst.setInstanceId(instanceId);
        moduleInst.setQuantity(1);
        moduleInst.setParas(new ArrayList<>());
        moduleInst.setParts(new ArrayList<>());
        return moduleInst;
    }
}
