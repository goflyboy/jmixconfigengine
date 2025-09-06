package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ExtensibleProcess;
import com.jmix.configengine.InferParasPostProcess;
import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.ModuleConstraintExecutor.ModuleInst;
import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.PartType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * DC交付类型后处理器
 * 根据Part类型和PartInst信息计算DCParaInst的deliveryType
 */
@Slf4j
public class DCDeliveryTypePostProcess extends ExtensibleProcess implements InferParasPostProcess {
    
    private final DCParaInstAdapter paraInstAdapter;
    
    public DCDeliveryTypePostProcess() {
        super("DCDeliveryTypePostProcess");
        this.paraInstAdapter = new DCParaInstAdapter();
    }
    
    @Override
    public ModuleConstraintExecutor.Result<List<ModuleInst>> postProcess(Module module, List<ModuleInst> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            log.debug("No solutions to process");
            return ModuleConstraintExecutor.Result.success(solutions);
        }
        
        List<ModuleInst> processedSolutions = new ArrayList<>();
        
        for (ModuleInst solution : solutions) {
            ModuleInst processedSolution = processSolution(module, solution);
            processedSolutions.add(processedSolution);
        }
        
        log.info("Processed {} solutions with delivery type calculation", solutions.size());
        return ModuleConstraintExecutor.Result.success(processedSolutions);
    }
    
    /**
     * 处理单个解决方案
     */
    private ModuleInst processSolution(Module module, ModuleInst solution) {
        ModuleInst processedSolution = new ModuleInst();
        processedSolution.setId(solution.getId());
        processedSolution.setCode(solution.getCode());
        processedSolution.setInstanceConfigId(solution.getInstanceConfigId());
        processedSolution.setInstanceId(solution.getInstanceId());
        processedSolution.setQuantity(solution.getQuantity());
        
        // 处理参数实例
        List<ParaInst> processedParas = new ArrayList<>();
        if (solution.getParas() != null) {
            for (ParaInst paraInst : solution.getParas()) {
                DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);
                
                // 根据业务规则计算deliveryType
                String deliveryType = calculateDeliveryType(module, solution, paraInst);
                dcParaInst.setDeliveryType(deliveryType);
                
                // 转换回ParaInst格式（这里简化处理，实际可能需要更复杂的转换）
                ParaInst processedParaInst = convertFromDCParaInst(dcParaInst);
                processedParas.add(processedParaInst);
            }
        }
        processedSolution.setParas(processedParas);
        
        // 处理部件实例
        processedSolution.setParts(solution.getParts());
        
        return processedSolution;
    }
    
    /**
     * 计算交付类型
     * 根据Part类型和PartInst信息计算deliveryType
     */
    private String calculateDeliveryType(Module module, ModuleInst solution, ParaInst paraInst) {
        // 业务规则示例：
        // 1. 如果是ATOMIC类型的Part，使用"Express"交付
        // 2. 如果是COMPOSITE类型的Part，使用"Standard"交付
        // 3. 如果Part数量大于5，使用"Bulk"交付
        
        if (module == null || solution == null || paraInst == null) {
            return "Standard";
        }
        
        // 查找对应的Part定义
        Part part = module.getPart(paraInst.getCode());
        if (part == null) {
            log.warn("Part not found for code: {}", paraInst.getCode());
            return "Standard";
        }
        
        // 查找对应的PartInst
        PartInst partInst = findPartInstByCode(solution, paraInst.getCode());
        
        // 根据Part类型计算交付类型
        if (part.getType() == PartType.ATOMIC) {
            if (partInst != null && partInst.getQuantity() != null && partInst.getQuantity() > 5) {
                return "Bulk";
            }
            return "Express";
        } else if (part.getType() == PartType.CATEGORY || part.getType() == PartType.BUNDLE) {
            return "Standard";
        }
        
        return "Standard";
    }
    
    /**
     * 根据编码查找PartInst
     */
    private PartInst findPartInstByCode(ModuleInst solution, String code) {
        if (solution.getParts() == null) {
            return null;
        }
        
        for (PartInst partInst : solution.getParts()) {
            if (code.equals(partInst.getCode())) {
                return partInst;
            }
        }
        
        return null;
    }
    
    /**
     * 将DCParaInst转换回ParaInst
     * 这里简化处理，实际可能需要更复杂的转换逻辑
     */
    private ParaInst convertFromDCParaInst(DCParaInst dcParaInst) {
        ParaInst paraInst = new ParaInst();
        paraInst.setCode(dcParaInst.getCode());
        paraInst.setValue(dcParaInst.getValue());
        paraInst.setOptions(dcParaInst.getOptions());
        paraInst.setHidden(dcParaInst.isHidden());
        
        // 注意：这里无法直接设置deliveryType到ParaInst中
        // 因为ParaInst没有这个字段，这需要在业务层面处理
        log.debug("Converted DCParaInst to ParaInst: {} with deliveryType: {}", 
                dcParaInst.getCode(), dcParaInst.getDeliveryType());
        
        return paraInst;
    }
    
    @Override
    public boolean supports(String operation) {
        return InferParasPostProcess.OPERATION_INFER_PARAS_POST.equals(operation);
    }
    
    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }
}
