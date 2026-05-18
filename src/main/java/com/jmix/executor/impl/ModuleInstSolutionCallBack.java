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
 * 妯″潡瀹炰緥瑙ｅ喅鏂规鍥炶皟绫?
 * 缁ф壙CpSolverSolutionCallback锛岀敤浜庡鐞嗙害鏉熸眰瑙ｈ繃绋嬩腑鐨勮В鍐虫柟妗堝洖璋?
 * 
 * @since 2025-09-22
 */
@Slf4j
@Data
public class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {

    private final Module module;

    // private final List<ModuleInst> allSolutions = new ArrayList<>();

    // 绗嚑涓В
    private int solutionIndex = 0;

    // 鍏朵粬鍙橀噺鏄犲皠
    private Map<String, OtherVar> otherVarMap;

    // 绾︽潫绠楁硶瀹炵幇锛岀敤浜庢煡璇紭鍏堢骇绾︽潫
    private ModuleAlgImpl moduleAlg;

    // 鏈€澶цВ鏁伴噺闄愬埗锛?琛ㄧず鏃犻檺鍒?
    private int maxSolutionNum = 0;

    private PartAlgCPLinearExpr priorityExpr = null;

    private SolverResult solverResult = new SolverResult();

    /**
     * 鏋勯€犲嚱鏁?
     * 
     * @param module      妯″潡瀵硅薄
     * @param vars        鍙橀噺鍒楄〃
     * @param otherVarMap 鍏朵粬鍙橀噺鏄犲皠
     * @param alg         绾︽潫绠楁硶瀹炵幇
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
        // 鍚庣画鑰冭檻澶氱嚎绋嬶紝闇€瑕佽В鍐冲鐩稿悓瑙ｉ噸澶嶉亶鍘嗭紙濡傦細閫氳繃鏍￠獙), this.stopSearch()
        solutionIndex++;
        log.info("-------------varInfos-solutionIndex:{}----------- \n {}", solutionIndex);
        // 鍒涘缓ModuleInst瀹炰緥锛宨nstanceId浠?寮€濮?
        ModuleInst moduleInst = buildModuleInst(moduleAlg, solutionIndex);

        // 澶勭悊鍏朵粬鍙橀噺
        processOtherVariables(moduleInst);

        // 澶勭悊浼樺厛绾у€?
        processPriorityValues(moduleInst);

        solverResult.addSolution(moduleInst);

        // 濡傛灉杈惧埌鏈€澶цВ鏁伴噺闄愬埗锛屽仠姝㈡悳绱?
        if (maxSolutionNum > 0 && solverResult.getSolutions().size() >= maxSolutionNum) {
            log.info("Reached maximum solution limit: {}, stopping search", maxSolutionNum);
            solverResult.setHasSearchMax(true);
            solverResult.setSearchMax(maxSolutionNum);
            stopSearch();
        }
    }

    /**
     * 灏哖araVar杞崲涓篜araInst
     * 
     * @param pv ParaVar瀵硅薄
     * @return ParaInst瀹炰緥
     */
    private ParaInst toParaInst(ParaVarImpl pv) {
        ParaInst pi = new ParaInst();
        pi.setCode(pv.getCode());
        // 浠庢ā鍧椾腑鑾峰彇瀵瑰簲鐨凱ara妯″瀷锛岃缃畇hortCode
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
        }
        return pi;
    }

    /**
     * 灏哖artVar杞崲涓篜artInst
     * 
     * @param partVar PartVar瀵硅薄
     * @return PartInst瀹炰緥
     */
    private PartInst toPartInst(PartVarImpl partVar) {
        PartInst inst = new PartInst();
        inst.setCode(partVar.getCode());
        // 浠庢ā鍧椾腑鑾峰彇瀵瑰簲鐨凱art妯″瀷锛岃缃畇hortCode
        inst.setShortCode(partVar.getBase().getShortCode());
        inst.setQuantity((int) value(partVar.getQty()));
        inst.setSelected(partVar.getIsSelected() != null && value(partVar.getIsSelected()) == 1);
        return inst;
    }

    /**
     * 澶勭悊鍏朵粬鍙橀噺锛屽皢otherVarMap涓殑鍙橀噺鍊兼坊鍔犲埌ModuleInst鐨勬墿灞曞睘鎬т腑
     * 
     * @param moduleInst 妯″潡瀹炰緥瀵硅薄
     */
    private void processOtherVariables(ModuleInst moduleInst) {
        // 濡傛灉isLogVariables涓簍rue锛屽垯澧炲姞鏍规嵁otherVarMap鍊肩殑鑾峰彇,缁勭粐otherVarKeyMap
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
     * 澶勭悊浼樺厛绾у€?
     * 鏍规嵁 ModuleInst 涓殑閮ㄤ欢瀹炰緥璁＄畻姣忎釜浼樺厛绾х害鏉熺殑鍊?
     * 
     * @param moduleInst 妯″潡瀹炰緥
     */
    private void processPriorityValues(ModuleInst moduleInst) {
        if (priorityExpr == null) {
            return;
        }
        List<Integer> exprVariablesValues = new ArrayList<>();
        // 閬嶅巻琛ㄨ揪寮忓彉閲?
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
        // 璁＄畻鏈€浼樺€?
        double calculatedValue = PriorityConstraint.calcToString(priorityExpr, exprVariablesValues);
        moduleInst.setPriorityOverallValue(calculatedValue);
    }
}
