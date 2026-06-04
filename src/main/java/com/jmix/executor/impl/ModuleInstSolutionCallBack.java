package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.ModuleBase;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.cmodel.ModuleBaseInst;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.MultiInstPartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.OtherVar;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.impl.algmodel.SingleInstPartCategoryAlgImpl;

import com.google.ortools.sat.CpSolverSolutionCallback;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
@Data
public class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {

    private final Module module;

    // private final List<ModuleInst> allSolutions = new ArrayList<>();

    // 第几个解
    private int solutionIndex = 0;

    // 其他变量映射
    private Map<String, OtherVar> otherVarMap;

    // 约束算法实现，用于查询优先级约束
    private ModuleAlgImpl moduleAlg;

    // 最大解数量限制，0表示无限制
    private int maxSolutionNum = 0;

    private PartAlgCPLinearExpr priorityExpr = null;

    private SolverResult solverResult = new SolverResult();

    /**
     * 构造函数
     * 
     * @param module      模块对象
     * @param vars        变量列表
     * @param otherVarMap 其他变量映射
     * @param alg         约束算法实现
     */
    public ModuleInstSolutionCallBack(Module module,
            ModuleAlgImpl moduleAlg) {
        this.module = module;
        this.moduleAlg = moduleAlg;
        this.priorityExpr = moduleAlg.queryMergerPriorityConstraintExpr();
    }

    private ModuleInst buildModuleInst(ModuleAlgImpl moduleAlg, int instanceId) {
        ModuleInst bInst = new ModuleInst();
        Module module = (Module) moduleAlg.getModule();
        bInst.setId(module.getId());
        buildModuleBaseInst(moduleAlg, instanceId, bInst);
        for (PartCategoryAlgImpl partCategoryAlg : moduleAlg.getPartCategoryAlgs()) {
            if (partCategoryAlg instanceof MultiInstPartCategoryAlgImpl) {
                MultiInstPartCategoryAlgImpl multiInstPartCategoryAlg = (MultiInstPartCategoryAlgImpl) partCategoryAlg;
                for (SingleInstPartCategoryAlgImpl singleInstPartCategoryAlg : multiInstPartCategoryAlg
                        .getPartCategoryInsts()) {
                    PartCategoryInst pInst = buildPartCategoryInst(singleInstPartCategoryAlg,
                            singleInstPartCategoryAlg.getInstId());
                    bInst.addPartCategoryInst(pInst);
                }
            } else {
                SingleInstPartCategoryAlgImpl singleInstPartCategoryAlg = (SingleInstPartCategoryAlgImpl) partCategoryAlg;
                PartCategoryInst pInst = buildPartCategoryInst(singleInstPartCategoryAlg,
                        singleInstPartCategoryAlg.getInstId());
                bInst.addPartCategoryInst(pInst);
            }
        }
        return bInst;
    }

    private PartCategoryInst buildPartCategoryInst(PartCategoryAlgImpl moduleAlg, int instanceId) {
        PartCategoryInst bInst = new PartCategoryInst();
        buildModuleBaseInst((ModuleBaseAlgImpl) moduleAlg, instanceId, bInst);
        return bInst;
    }

    private ModuleBaseInst buildModuleBaseInst(ModuleBaseAlgImpl moduleAlg, int instanceId, ModuleBaseInst bInst) {
        ModuleBase base = (ModuleBase) moduleAlg.getModule();
        bInst.setCode(base.getCode());
        bInst.setShortCode(base.getShortCode());
        bInst.setInstanceId(instanceId);
        for (ParaVarImpl pv : moduleAlg.getParaVars()) {
            ParaInst pi = toParaInst(pv);
            bInst.addParaInst(pi);
        }
        for (PartVarImpl partVar : moduleAlg.getPartVars()) {
            PartInst inst = toPartInst(partVar);
            bInst.addPartInst(inst);
        }
        return bInst;
    }

    @Override
    public void onSolutionCallback() {
        // 后续考虑多线程，需要解决对相同解重复遍历（如：通过校验), this.stopSearch()
        solutionIndex++;
        log.info("-------------varInfos-solutionIndex:{}----------- \n {}", solutionIndex);
        // 创建ModuleInst实例，instanceId从0开始
        ModuleInst moduleInst = buildModuleInst(moduleAlg, solutionIndex);

        // 处理其他变量
        processOtherVariables(moduleInst);

        // 处理优先级值
        processPriorityValues(moduleInst);

        solverResult.addSolution(moduleInst);

        // 如果达到最大解数量限制，停止搜索
        if (maxSolutionNum > 0 && solverResult.getSolutions().size() >= maxSolutionNum) {
            log.info("Reached maximum solution limit: {}, stopping search", maxSolutionNum);
            solverResult.setHasSearchMax(true);
            solverResult.setSearchMax(maxSolutionNum);
            stopSearch();
        }
    }

    /**
     * 将ParaVar转换为ParaInst
     * 
     * @param pv ParaVar对象
     * @return ParaInst实例
     */
    private ParaInst toParaInst(ParaVarImpl pv) {
        ParaInst pi = new ParaInst();
        pi.setCode(pv.getCode());
        // 从模块中获取对应的Para模型，设置shortCode
        pi.setShortCode(pv.getBase().getShortCode());

        if (pv.getBase().getAssignType() == AssignType.CALC) {
            // value: read IntVar domain value
            int value = (int) value(pv.getValue());
            pi.setValue(String.valueOf(value));
            pi.setHidden((int) value(pv.getIsHidden()) == 1);
            // options: selected option codes
            List<String> options = new ArrayList<>();
            pv.getOptionSelectVars().forEach((codeId, optionVar) -> {
                long sel = value(optionVar.getIsSelectedVar());
                if (sel == 1L) {
                    options.add(optionVar.getCode());
                }
            });
            pi.setOptions(options);
        } else {
            pi.setValue(String.valueOf(pv.getInputValue()));
            // Mark as INPUT type so it can be excluded from solution output
            pi.setExtAttrs(Map.of(ModuleBaseInst.PARA_INST_TYPE_KEY, "INPUT"));
        }
        return pi;
    }

    /**
     * 将PartVar转换为PartInst
     * 
     * @param partVar PartVar对象
     * @return PartInst实例
     */
    private PartInst toPartInst(PartVarImpl partVar) {
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
            moduleInst.getExtAttrs().put(ModuleBaseInst.OTHER_VARIABLES_VALUE_KEY, otherVarKeyMap);
            moduleInst.getExtAttrs().put(ModuleBaseInst.OTHER_VARIABLES_MEMO_KEY, otherVarMap);
        }
    }

    /**
     * 处理优先级值
     * 根据 ModuleInst 中的部件实例计算每个优先级约束的值
     * 
     * @param moduleInst 模块实例
     */
    private void processPriorityValues(ModuleInst moduleInst) {
        if (priorityExpr == null) {
            return;
        }
        List<Integer> exprVariablesValues = new ArrayList<>();
        // 遍历表达式变量
        for (PriorityConstraint.PartTerm exprVariable : priorityExpr.getPartTerms()) {
            PartInst pInst = moduleInst.queryPart(exprVariable.getPartCategoryCode(), exprVariable.getInstId(),
                    exprVariable.getPartCode());
            if (pInst == null) {
                exprVariablesValues.add(0);
                log.error("PartInst not found: partCategoryCode={}, instId={}, partCode={}",
                        exprVariable.getPartCategoryCode(), exprVariable.getInstId(), exprVariable.getPartCode());

            } else {
                if (exprVariable.getTermValue().equals(PartVarImpl.ISSELECTED_SHORT_NAME)) {
                    exprVariablesValues.add(pInst.isSelected() ? 1 : 0);
                } else {
                    exprVariablesValues.add(pInst.getQuantity() != null ? pInst.getQuantity() : 0);
                }
            }
        }
        // 计算最优值
        double calculatedValue = PriorityConstraint.calcToString(priorityExpr, exprVariablesValues);
        moduleInst.setPriorityOverallValue(calculatedValue);
    }
}
