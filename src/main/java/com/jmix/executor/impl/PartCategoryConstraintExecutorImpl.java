package com.jmix.executor.impl;

import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.DynamicAttribute;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.imodel.PartCategory;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.RelaxVar;
import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AttrFunConstant;
import com.jmix.executor.omodel.InferPartCategoryReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartConstraintReq;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 部件分类约束执行器实现类
 * 专门处理部件分类相关的约束求解
 * 
 * @since 2025-01-XX
 */
@Slf4j
public class PartCategoryConstraintExecutorImpl {
    private final ConstraintConfig config;
    private final ModuleAlgClassLoader moduleAlgClassLoader;

    public PartCategoryConstraintExecutorImpl(ConstraintConfig config, ModuleAlgClassLoader moduleAlgClassLoader) {
        this.config = config;
        this.moduleAlgClassLoader = moduleAlgClassLoader;
    }

    /**
     * 处理部件分类约束
     * 
     * @param partCatagoryReq 部件分类请求
     * @param module          模块对象
     * @param isAttachRelax   是否附加松弛变量
     * @param confictedRelaxs 冲突松弛变量列表
     * @return 推理结果
     */
    public Result<List<ModuleInst>> processPartCategory(InferPartCategoryReq partCatagoryReq, Module module,
            boolean isAttachRelax, List<RelaxVar> confictedRelaxs) {
        try {
            // 创建约束算法实例
            ConstraintAlgImpl alg = createConstraintAlg(module.getId(), module.getCode());

            // 查找原始部件分类
            PartCategory originalCategory = module.findPartCategory(partCatagoryReq.getPartCatagoryCode());
            if (originalCategory == null) {
                log.error("PartCategory not found: {}", partCatagoryReq.getPartCatagoryCode());
                return Result.failed("PartCategory not found: " + partCatagoryReq.getPartCatagoryCode());
            }

            // 遍历所有部件约束请求，逐步过滤
            PartCategory filteredCategory = originalCategory;
            if (partCatagoryReq.getPartConstraintReqs() != null) {
                for (PartConstraintReq partConstraintReq : partCatagoryReq.getPartConstraintReqs()) {
                    // 查询满足条件的PartCategory
                    filteredCategory = filteredCategory.query(partConstraintReq);
                }
            }

            // 创建CP模型
            CpModel model = new CpModel();
            alg.initModel(model, filteredCategory, isAttachRelax, confictedRelaxs);

            // 根据请求初始化约束模型
            initModelByReq(filteredCategory, partCatagoryReq, alg);

            // 添加松弛目标函数, 方便调试
            alg.addRelaxObjectFunction();

            if (config.isLogModelProto()) {
                // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
                model.exportToFile(config.getLogFilePath() + File.separator + module.getCode() + ".proto.txt");
            }

            CpSolver solver = new CpSolver();
            solver.getParameters().setEnumerateAllSolutions(partCatagoryReq.isEnumerateAllSolution());
            solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解
            log.info("solver parameters:\n" + solver.getParameters().toString());
            // 可按需设置更多参数
            ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg.getVars(),
                    alg.getOtherVarMap());
            CpSolverStatus status = solver.solve(model, cb);

            // 如果模型无效，调用ValidateCpModel获取详细错误信息
            if (status == CpSolverStatus.MODEL_INVALID) {
                // 重新获取模型进行验证
                ConstraintAlgImpl validationAlg = createConstraintAlg(module.getId(), module.getCode());
                CpModel validationModel = new CpModel();
                validationAlg.initModel(validationModel, module, false, new ArrayList<>());
                String validationError = validationModel.validate();
                log.error("Model validation failed: {}", validationError);
                return Result.failed("Model validation failed: " + validationError);
            }

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                    && status != CpSolverStatus.INFEASIBLE) {
                return Result.failed("solver status: " + status);
            }

            if (status == CpSolverStatus.INFEASIBLE) {
                return Result.success(null);
            }

            // 返回成功结果
            return Result.success(cb.getAllSolutions());

        } catch (Exception e) {
            log.error("Failed to process part category constraint", e);
            return Result.failed("Failed to process part category constraint: " + e.getMessage());
        }
    }

    /**
     * 根据请求初始化约束模型
     * 
     * @param filteredCategory 过滤后的部件分类
     * @param req              部件分类请求
     * @param alg              约束算法实现
     */
    private void initModelByReq(PartCategory filteredCategory, InferPartCategoryReq req, ConstraintAlgImpl alg) {
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                alg.addParaEquality(paraInst.getCode(), paraInst.getValue());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                alg.addPartEquality(partInst.getCode(), partInst.getQuantity());
            }
        }

        if (req.getPartConstraintReqs() != null && !req.getPartConstraintReqs().isEmpty()) {
            // 获取所有叶子部件
            List<Part> filterParts = filteredCategory.getAllLeafParts();

            // 获取原始部件分类用于计算未过滤的部件
            PartCategory originalCategory = alg.getModule() instanceof Module
                    ? ((Module) alg.getModule()).findPartCategory(req.getPartCatagoryCode())
                    : null;
            if (originalCategory != null) {
                List<Part> unFilterParts = PartCategory.calcUnFilterLeafParts(originalCategory, filterParts);

                // 处理最后一个约束请求
                PartConstraintReq lastPartConstraintReq = req.getPartConstraintReqs()
                        .get(req.getPartConstraintReqs().size() - 1);

                log.info("filterParts size: {} after filter req.whereCondition: {}", filterParts.size(),
                        lastPartConstraintReq.getAttrWhereCondition());

                // 解析属性
                Pair<DynamicAttribute, String> result = originalCategory.parseAttribute(
                        lastPartConstraintReq.getAttrCode(),
                        originalCategory.getDynAttrSchemas());

                // 根据result.second构建约束表达式
                if (AttrFunConstant.FUN_PREFIX_SUM.equals(result.getSecond())) {
                    alg.sumFunConstraint(filterParts, result.getFirst().getCode(),
                            lastPartConstraintReq.getAttrComparator(),
                            Integer.parseInt(lastPartConstraintReq.getAttrValue()));
                    alg.setPartUnSelected(unFilterParts);
                }
            }
        }
    }

    /**
     * 创建约束算法实例
     * 
     * @param moduleId   模块ID
     * @param moduleCode 模块代码
     * @return 约束算法实例
     */
    private ConstraintAlgImpl createConstraintAlg(long moduleId, String moduleCode) {
        return moduleAlgClassLoader.newConstraintAlg(moduleCode);
    }
}
