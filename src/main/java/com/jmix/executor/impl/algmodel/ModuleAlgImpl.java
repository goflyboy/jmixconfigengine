package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ParConstraint;
import com.jmix.executor.southinf.IModuleAlg;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
public class ModuleAlgImpl extends ModuleBaseAlgImpl implements IModuleAlg {

    /**
     * 部件分类算法实例映射表
     */
    protected Map<String, PartCategoryAlgImpl> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 初始化模块算法实例
     * 按partCategoryCode对partConstraintFromReqs进行分组，然后初始化本层和子层的变量与规则
     * 重写基类方法，添加PartCategoryAlgImpl的初始化
     *
     * @param model                  CP约束模型
     * @param module                 模块对象
     * @param partConstraintFromReqs 来自请求的部件约束列表
     */
    public void init(AlgCPModel model, IModule module,
            List<ParConstraint> partConstraintFromReqs) {
        initData(model, module, partConstraintFromReqs, this);
        initRules(this);
    }

    protected void initData(AlgCPModel model, IModule module, List<ParConstraint> partConstraintFromReqs,
            IModuleAlg moduleAlgFile) {
        // 调用基类初始化
        super.initData(model, module, new ArrayList<>(), this);

        // 按partCategoryCode对partConstraintFromReqs进行分组
        Map<String, List<ParConstraint>> partConstraintFromReqMap = groupConstraintsByPartCategory(
                partConstraintFromReqs);

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        if (module instanceof Module) {
            Module bModule = (Module) module;
            for (PartCategory partCategory : bModule.getPartCategorys()) {
                String categoryCode = partCategory.getCode();
                List<ParConstraint> pc4PartConstraintFromReqs = partConstraintFromReqMap
                        .get(categoryCode);

                PartCategoryAlgImpl pcAlg = new PartCategoryAlgImpl();
                pcAlg.initData(model, (IModule) partCategory, pc4PartConstraintFromReqs, this);

                // 执行PartCategoryAlgImpl的规则
                // pcAlg.initRule(allRuleMethods);

                partCategoryAlgs.put(categoryCode, pcAlg);
            }
        }
        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    protected void initRules(IModuleAlg moduleAlgFile) {
        Map<String, Method> allRuleMethods = buildAllRuleMethods(module, this);
        // 先本身这个模块的前置算法

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initRules(allRuleMethods, moduleAlgFile);
        }

        // 执行本身这个模块的后置算法
        this.initRules(allRuleMethods, moduleAlgFile);
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
     * 获取所有部件分类算法实例列表
     * 
     * @return 部件分类算法实例列表
     */
    public List<PartCategoryAlgImpl> getPartCategoryAlgs() {
        return new ArrayList<>(partCategoryAlgs.values());
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

    @Override
    protected Var<?> newPartVar(PartVar internalPartVar) {
        return internalPartVar;
    }

    @Override
    protected Var<?> newParaVar(ParaVar internalParaVar) {
        return internalParaVar;
    }

    /**
     * 添加不兼容性约束（部件级别）
     * 
     * @param ruleCode          规则代码
     * @param leftPartsExprStr  左侧部件表达式字符串，格式如 "fatherCode=cpu:CoreNum=4"
     * @param rightPartsExprStr 右侧部件表达式字符串
     */
    public void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr) {
        PartsExpr left = filterPartExpr(leftPartsExprStr);
        PartsExpr right = filterPartExpr(rightPartsExprStr);

        if (left.isEmpty4FilterPars() || right.isEmpty4FilterPars()) {
            log.info("Skip inCompatible constraint: {} - left or right filter is empty", ruleCode);
            return;
        }

        compatibleConstraintAlg.addCompatibleConstraintInCompatible(ruleCode, left, right);
    }

    /**
     * 解析部件表达式字符串
     * 格式：fatherCode=xxx:filterCondition
     * 
     * @param partExprStr 部件表达式字符串
     * @return PartsExpr
     */
    private PartsExpr filterPartExpr(String partExprStr) {
        if (partExprStr == null || partExprStr.isEmpty()) {
            // 打日志，抛异常
            log.error("partExprStr is null or empty, cannot filter part expr");
            throw new AlgLoaderException("partExprStr is null or empty, cannot filter part expr");
        }
        PartsExpr partsExpr = new PartsExpr();
        // 解析partCategoryCode和filterConditionStr
        String partCategoryCode = "";
        String filterConditionStr = "";

        int colonIndex = partExprStr.indexOf(':');
        if (colonIndex > 0) {
            partCategoryCode = partExprStr.substring(0, colonIndex);
            filterConditionStr = partExprStr.substring(colonIndex + 1);
        } else {
            // 打日志，抛异常
            log.error("partExprStr is invalid, cannot filter part expr");
            throw new AlgLoaderException("partExprStr is invalid, cannot filter part expr");
        }

        partsExpr.setFilterConditionStr(filterConditionStr);

        PartCategoryAlgImpl partCategoryAlgImpl = this.getPartCategoryAlg(partCategoryCode);
        if (partCategoryAlgImpl == null) {
            // 打日志，抛异常
            log.error("partCategoryAlgImpl is invalid, cannot filter part expr");
            throw new AlgLoaderException("partCategoryAlgImpl is invalid, cannot filter part expr");
        }
        // 获取该分类下的所有原子部件
        List<Part> atomicParts = partCategoryAlgImpl.getModule().getAllAtomicParts();

        // 根据过滤条件筛选部件
        List<Part> filterParts;
        List<Part> noFilterParts;

        if (filterConditionStr == null || filterConditionStr.isEmpty()) {
            filterParts = new ArrayList<>();
            noFilterParts = new ArrayList<>(atomicParts);
        } else {
            filterParts = FilterExpressionExecutor.doSelect(atomicParts, filterConditionStr);
            noFilterParts = subPart(atomicParts, filterParts);
        }

        // 转换为PartVar
        partsExpr.setPartVars(toPartVar(partCategoryAlgImpl, atomicParts));
        partsExpr.setFilterPartVars(toPartVar(partCategoryAlgImpl, filterParts));
        partsExpr.setNoFilterPartVars(toPartVar(partCategoryAlgImpl, noFilterParts));
        return partsExpr;
    }

    /**
     * 将Part列表转换为PartVar列表
     * 
     * @param parts Part列表
     * @return PartVar列表
     */
    public List<PartVar> toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, List<Part> parts) {
        return parts.stream()
                .map(t -> toPartVar(
                        partCategoryAlgImpl, t))
                .filter(pv -> pv != null)
                .collect(Collectors.toList());
    }

    /**
     * 将Part对象转换为PartVar
     * 
     * @param part Part对象
     * @return PartVar，如果不存在则返回null
     */
    public PartVar toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, Part part) {
        PartVar partVar = partCategoryAlgImpl.getPartVar(part.getCode());
        if (partVar == null) {
            // 打日志，抛异常
            log.error("PartVar not found for code: {}", part.getCode());
            throw new AlgLoaderException("PartVar not found for code: " + part.getCode());
        }
        return partVar;
    }

    /**
     * 计算子部件列表
     * 从allParts中移除subParts
     * 
     * @param allParts 全部部件列表
     * @param subParts 要移除的部件列表
     * @return 剩余部件列表
     */
    private List<Part> subPart(List<Part> allParts, List<Part> subParts) {
        if (subParts == null || subParts.isEmpty()) {
            return new ArrayList<>(allParts);
        }

        List<Part> result = new ArrayList<>(allParts);
        result.removeAll(subParts);
        return result;
    }
}
