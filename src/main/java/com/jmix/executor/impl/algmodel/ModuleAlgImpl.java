package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.PartUtils;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.impl.ModuleInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.IModuleAlg;
import com.jmix.tool.bbuilder.MultiInstCategoryUtils;

import com.google.ortools.sat.LinearArgument;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
     * 按partCategoryCode对partCategoryInputs进行分组，然后初始化本层和子层的变量与规则
     * 重写基类方法，添加PartCategoryAlgImpl的初始化
     *
     * @param model              CP约束模型
     * @param module             模块对象
     * @param partCategoryInputs 来自请求的部件约束列表
     */
    public void init(AlgCPModel model, IModule module,
            ModuleInput moduleInput) {
        initData(model, module, moduleInput, this);

        afterInitData(this.module, this);

        initInput();

        // preCalculate
        preCalculate();

        // midCalculate
        initRules(this, CalcStage.MID);
        // postCalculate
    }

    private ModuleInput getModuleInput() {
        return (ModuleInput) this.moduleInput;
    }

    protected void initInput() {
        super.initInput(this);

        // 本模块的初始化
        ModuleInput req = getModuleInput();
        if (req.getMainPartInst() != null) {
            this.addPartEquality(req.getMainPartInst().getCode(), req.getMainPartInst().getQuantity());
        }
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                this.addParaEquality(paraInst.getCode(), paraInst.getValue());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                this.addPartEquality(partInst.getCode(), partInst.getQuantity());
            }
        }

        // 分类的input初始化
        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initInput(this);
        }

    }

    /**
     * 初始化Module后
     * 
     * @param module
     * @param moduleAlg
     */
    protected void afterInitData(IModule module, IModuleAlg moduleAlg) {

    }

    private void preCalculate() {
        log.info("preCalculate: module={}", getModule().getCode());
        // 暂时支持模块级前置计算，主要是对控制变量进行初始化
        if (!(module instanceof Module)) {
            return;
        }
        initRules(this, CalcStage.PRE);
    }

    protected void initData(AlgCPModel model, IModule module,
            ModuleInput moduleInput,
            IModuleAlg moduleAlgFile) {
        // 调用基类初始化
        super.initData(model, module, moduleInput, this);

        // 按partCategoryCode对partCategoryInputs进行分组
        Map<String, PartCategoryInputBase> partConstraintFromReqMap = new LinkedHashMap<>();
        for (PartCategoryInputBase partCategoryInput : moduleInput.getPartCategoryInputs()) {
            partConstraintFromReqMap.put(partCategoryInput.getPartCategoryCode(), partCategoryInput);
        }

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        if (module instanceof Module) {
            Module bModule = (Module) module;
            for (PartCategory partCategory : bModule.getPartCategorys()) {
                String categoryCode = partCategory.getCode();
                PartCategoryInputBase pc4partCategoryInput = partConstraintFromReqMap
                        .get(categoryCode);
                ModuleBaseAlgImpl pcAlg = null;
                if (partCategory.isSupportMultiInst() && pc4partCategoryInput instanceof MultiInstPartCategoryInput) {
                    pcAlg = new MultiInstPartCategoryAlgImpl();
                    MultiInstPartCategoryInput multiInstPartCategoryInput = (MultiInstPartCategoryInput) pc4partCategoryInput;
                    pcAlg.initData(model, (IModule) multiInstPartCategoryInput, pc4partCategoryInput, this);
                } else {
                    pcAlg = new SingleInstPartCategoryAlgImpl();
                    pcAlg.initData(model, (IModule) partCategory, pc4partCategoryInput, this);
                }
                partCategoryAlgs.put(categoryCode, (PartCategoryAlgImpl) pcAlg);
            }
        }
        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    protected void initRules(IModuleAlg moduleAlgFile, CalcStage calcStage) {
        Map<String, Method> allRuleMethods = buildAllRuleMethods(module, this);
        // 先本身这个模块的前置算法

        // 先构建本模块的优先类规则
        buildPriorityConstraint(module);

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initRules(allRuleMethods, moduleAlgFile, calcStage);
        }

        // 执行本身这个模块的后置算法
        this.initRules(allRuleMethods, moduleAlgFile, calcStage);
    }

    /**
     * 是否有优先类规则
     * 
     * @return exr
     */
    public boolean hasPriorityRule() {
        return !getAllPriorityConstraintMap().isEmpty();
    }

    private Map<String, PriorityConstraint> getAllPriorityConstraintMap() {
        // ruleCode -> PriorityConstraint
        Map<String, PriorityConstraint> priorityConstraints = new HashMap<>();
        if (getPriorityConstraint() != null) {
            priorityConstraints.put(getPriorityConstraint().getRule().getCode(), getPriorityConstraint());
        }
        for (PartCategoryAlgImpl partCategoryAlgImpl : getPartCategoryAlgs()) {
            PriorityConstraint priorityConstraint = ((ModuleBaseAlgImpl) partCategoryAlgImpl).getPriorityConstraint();
            if (priorityConstraint != null) {
                priorityConstraints.put(
                        priorityConstraint.getRule().getCode(), priorityConstraint);
            }
        }
        return priorityConstraints;
    }

    /**
     * 获取所有的Priority合并后的表达式
     *
     * @return exr
     */
    public PartAlgCPLinearExpr queryMergerPriorityConstraintExpr() {
        PartAlgCPLinearExpr mergedExpr = model.newPartLinearExpr("merged_priority_expr");
        for (PriorityConstraint pc : getAllPriorityConstraintMap().values()) {
            Rule rule = pc.getRule();
            PriorityRuleSchema schema = (PriorityRuleSchema) rule.getRawCode();
            PriorityStrategy strategy = schema.getPriorityStrategy();
            // int weight = schema.getWeight();
            int coff = strategy == PriorityStrategy.MAX ? -1 : 1;
            mergedExpr.addExpr(pc.getExpr(), coff);
        }
        return mergedExpr;
    }

    /**
     * Update or create priority objective function for given attribute code.
     *
     * @param attrCode attribute code
     * @param expr     expression containing part-term metadata and numeric terms
     */
    public void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr) {
        if (ruleCode == null || ruleCode.isEmpty()) {
            log.warn("ruleCode is null or empty, skip updating priority objective");
            return;
        }
        PriorityConstraint pConstraint = getAllPriorityConstraintMap().get(ruleCode);
        if (pConstraint == null) {
            log.error("PriorityConstraint not found for ruleCode: {}", ruleCode);
            throw new AlgLoaderException("PriorityConstraint not found for ruleCode: " + ruleCode);
        }
        pConstraint.setExpr(expr);
        log.info("Updated priority objective for ruleCode {}: expr={}", ruleCode,
                expr != null ? expr.toString() : "null");
    }

    /**
     * 获取部件分类算法实例
     *
     * @param categoryCode 部件分类代码
     * @return PartCategoryAlgImpl实例
     */
    public PartCategoryAlgImpl getPartCategoryAlg(String categoryCode) {
        return (PartCategoryAlgImpl) partCategoryAlgs.get(categoryCode);
    }

    /**
     * 获取部件分类算法实例
     *
     * @param categoryCodeInstPrefix 部件分类代码前缀
     * @return PartCategoryAlgImpl实例
     */
    public List<PartCategoryAlgImpl> getPartCategoryAlgByInstPrefix(String categoryCodeInstPrefix) {
        String instPrefix = categoryCodeInstPrefix + String.valueOf(MultiInstCategoryUtils.INST_PREFIX_CHAR);
        List<PartCategoryAlgImpl> partCategoryAlgImpls = new ArrayList<>();
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgs.values()) {
            if (partCategoryAlgImpl.getCategoryCode().startsWith(instPrefix)) {
                partCategoryAlgImpls.add(partCategoryAlgImpl);
            }
        }
        return partCategoryAlgImpls;
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
            log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
            return partsExpr;
        }
        // 转换为PartVar
        partsExpr.setPartVars(partCategoryAlgImpl.getAllPartVars(""));
        Pair<List<PartVar>, List<PartVar>> partVars = partCategoryAlgImpl.filterAllPartVars(filterConditionStr);
        partsExpr.setFilterPartVars(partVars.getFirst());
        partsExpr.setNoFilterPartVars(partVars.getSecond());
        return partsExpr;
    }

    // /**
    // * 解析部件表达式字符串
    // * 格式：fatherCode=xxx:filterCondition
    // *
    // * @param partExprStr 部件表达式字符串
    // * @return PartsExpr
    // */
    // private PartsExpr filterPartExpr(String partExprStr) {
    // if (partExprStr == null || partExprStr.isEmpty()) {
    // // 打日志，抛异常
    // log.error("partExprStr is null or empty, cannot filter part expr");
    // throw new AlgLoaderException("partExprStr is null or empty, cannot filter
    // part expr");
    // }
    // PartsExpr partsExpr = new PartsExpr();
    // // 解析partCategoryCode和filterConditionStr
    // String partCategoryCode = "";
    // String filterConditionStr = "";

    // int colonIndex = partExprStr.indexOf(':');
    // if (colonIndex > 0) {
    // partCategoryCode = partExprStr.substring(0, colonIndex);
    // filterConditionStr = partExprStr.substring(colonIndex + 1);
    // } else {
    // // 打日志，抛异常
    // log.error("partExprStr is invalid, cannot filter part expr");
    // throw new AlgLoaderException("partExprStr is invalid, cannot filter part
    // expr");
    // }

    // partsExpr.setFilterConditionStr(filterConditionStr);

    // PartCategoryAlgImpl partCategoryAlgImpl =
    // this.getPartCategoryAlg(partCategoryCode);
    // if (partCategoryAlgImpl == null) {
    // // 打日志，抛异常
    // log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
    // return partsExpr;
    // }
    // // 获取该分类下的所有原子部件
    // List<Part> atomicParts = partCategoryAlgImpl.getAllAtomicParts();

    // // 根据过滤条件筛选部件
    // List<Part> filterParts;
    // List<Part> noFilterParts;

    // if (filterConditionStr == null || filterConditionStr.isEmpty()) {
    // filterParts = new ArrayList<>();
    // noFilterParts = new ArrayList<>(atomicParts);
    // } else {
    // filterParts = FilterExpressionExecutor.doSelect(atomicParts,
    // filterConditionStr);
    // noFilterParts = subPart(atomicParts, filterParts);
    // }

    // // 转换为PartVar
    // partsExpr.setPartVars(toPartVar(partCategoryAlgImpl, atomicParts));
    // partsExpr.setFilterPartVars(toPartVar(partCategoryAlgImpl, filterParts));
    // partsExpr.setNoFilterPartVars(toPartVar(partCategoryAlgImpl, noFilterParts));
    // return partsExpr;
    // }

    /**
     * 将Part列表转换为PartVar列表
     * 
     * @param parts Part列表
     * @return PartVar列表
     */
    public List<PartVar> toFilterPartVar(PartCategoryAlgImpl partCategoryAlgImpl, String filtedConditionStr) {
        List<Part> atomicParts = partCategoryAlgImpl.getAtomicParts();
        if (filtedConditionStr != null && !filtedConditionStr.trim().isEmpty()) {
            atomicParts = FilterExpressionExecutor.doSelect(atomicParts, filtedConditionStr);
            log.info("Priority-Filtered parts: {} in sum4Parts", PartUtils.toShortString(atomicParts));
        }
        return toPartVar(partCategoryAlgImpl, atomicParts);
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
     * 通用的部件求和方法，根据指定的变量获取函数计算总和
     * 
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param varGetter          从PartVar获取LinearArgument的函数（如getIsSelected或getQty）
     * @param varName            变量名称（如"isSelected"或"qty"），用于构建字符串表达式
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    // @Override
    protected PartAlgCPLinearExpr sum4Parts(PartCategoryAlgImpl partCategoryAlgImpl, String cofAttrCode,
            Function<PartVar, LinearArgument> varGetter, String varName, String filtedConditionStr) {
        PartAlgCPLinearExpr expr = buildSumExpr(
                partCategoryAlgImpl,
                cofAttrCode, varName, varGetter,
                filtedConditionStr);
        return expr;
    }

    /**
     * 对指定部件分类的部件求和（选中状态，支持多分类逗号分隔）
     * 
     * @param partCategoryCodesStr 部件分类代码，支持逗号分隔，如 "driveI0,driveI1"
     * @param cofAttrCode          属性代码
     * @param filtedConditionStr   过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Selected(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        if (partCategoryCodesStr == null || partCategoryCodesStr.trim().isEmpty()) {
            return sum4Selected(cofAttrCode, filtedConditionStr);
        }
        List<PartCategoryAlgImpl> partCategoryAlgImpls = toPartCategoryAlgImpls(partCategoryCodesStr);

        return buildSumExpr(
                partCategoryAlgImpls, cofAttrCode, PartVar.ISSELECTED_SHORT_NAME, PartVar::getIsSelected,
                filtedConditionStr);
    }

    /**
     * 对指定部件分类的部件求和（数量，支持多分类逗号分隔）
     * 
     * @param partCategoryCodesStr 部件分类代码，支持逗号分隔，如 "driveI0,driveI1"
     * @param cofAttrCode          属性代码
     * @param filtedConditionStr   过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        if (Strings.isEmpty(partCategoryCodesStr)) {
            return sum4Quantity(cofAttrCode, filtedConditionStr);
        }
        List<PartCategoryAlgImpl> partCategoryAlgImpls = toPartCategoryAlgImpls(partCategoryCodesStr);
        return buildSumExpr(partCategoryAlgImpls, cofAttrCode, PartVar.QTY_SHORT_NAME, PartVar::getQty,
                filtedConditionStr);
    }

    private List<PartCategoryAlgImpl> toPartCategoryAlgImpls(String partCategoryCodesStr) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = new ArrayList<>();
        if (partCategoryCodesStr.isEmpty()) {
            return partCategoryAlgImpls;
        }
        if (partCategoryCodesStr.contains("*")) {
            String partCategoryCodePrefix = partCategoryCodesStr.replace("*", "");
            partCategoryAlgImpls.addAll(getPartCategoryAlgByInstPrefix(partCategoryCodePrefix));
            return partCategoryAlgImpls;
        }
        String[] categoryCodes = partCategoryCodesStr.split(",");
        for (String categoryCode : categoryCodes) {
            partCategoryAlgImpls.add(getPartCategoryAlg(categoryCode));
        }
        return partCategoryAlgImpls;
    }

    /**
     * 对选中的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */

    public PartAlgCPLinearExpr sum4Selected(String cofAttrCode,
            String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg, cofAttrCode, PartVar::getIsSelected,
                PartVar.ISSELECTED_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对选中的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Selected(String filtedConditionStr) {
        return sum4Selected(null, filtedConditionStr);
    }

    /**
     * 对数量的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg,
                cofAttrCode, PartVar::getQty, PartVar.QTY_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对数量的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr) {
        return sum4Quantity(null, filtedConditionStr);
    }

    public void addControlParaEqual(String sumParaCode, String instSumParaCode) {
        // 动态表达式 addParaEqual("driveSumQuantity", "drive:SumQuantity");
        ParaVar sumParaVar = this.getParaVar(sumParaCode);
        if (sumParaVar == null) {
            // 打日志，抛异常
            log.error("ParaVar not found for code: {}", sumParaCode);
            throw new AlgLoaderException("ParaVar not found for code: " + sumParaCode);
        }
        // instSumParaCode,如："drive:SumQuantity", 解析出drive，SumQuantity
        String[] parts = instSumParaCode.split(":");
        if (parts.length != 2) {
            // 打日志，抛异常
            log.error("instSumParaCode is invalid: {}", instSumParaCode);
            throw new AlgLoaderException("instSumParaCode is invalid: " + instSumParaCode);
        }

        String partCategoryCodePrefix = parts[0].trim();
        String sumQuantityCode = parts[1].trim();
        // expr=sumParaVar.value + sumParaVar.value + ...
        List<PartCategoryAlgImpl> partCategoryAlgImpls = this.getPartCategoryAlgByInstPrefix(partCategoryCodePrefix);
        if (partCategoryAlgImpls.isEmpty()) {
            // 打日志，抛异常
            log.warn("PartCategoryAlgImpl not found for code: {}", partCategoryCodePrefix);
            sumParaVar.setHasInputed(false);
        } else {
            Integer sumInputValue = 0;
            boolean hasInput = false;
            for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
                ParaVar instParaVar = partCategoryAlgImpl.getParaVar(sumQuantityCode);
                if (instParaVar == null || instParaVar.getInputValue() == null) {
                    // 打日志，抛异常
                    log.error("ParaVar not found for code: {}", sumQuantityCode);
                    // 暂时不抛异常 throw new AlgLoaderException("ParaVar not found for code: " +
                    // sumQuantityCode);
                    // issue:是不是总有一个控制参数是有输入的，方便执行（对结果进行校验）
                    continue;
                }
                // paraSumExpr.addTerm(instParaVar.getInputValue(), 1);
                sumInputValue += instParaVar.getInputValue();
                hasInput = true;
            }
            if (hasInput) {
                // 构建等式
                sumParaVar.setHasInputed(true);
                sumParaVar.setInputValue(sumInputValue);

            } else {
                sumParaVar.setHasInputed(false);
            }

        }

    }
}
