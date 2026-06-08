package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.PartUtils;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Cardinality;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.impl.ModuleInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.StrategyConfig;
import com.jmix.executor.model.StrategyType;
import com.jmix.tool.bbuilder.MultiInstCategoryUtils;

import com.google.ortools.sat.DecisionStrategyProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
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

    // 左表达式部件分类算法实例，仅针对oneMany规则
    private SingleInstPartCategoryAlgImpl currentLeftPartCategoryAlgImpl = null;
    // 右表达式部件分类算法实例，仅针对oneMany规则
    private SingleInstPartCategoryAlgImpl currentRightPartCategoryAlgImpl = null;
    // 当前规则后缀，仅针对oneMany规则
    private String currrentRulePostName = "";

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
        Object moduleAlgFile = moduleAlgFile();
        initData(model, module, moduleInput, moduleAlgFile);

        afterInitData(this.module, moduleAlgFile);

        initInput();

        initCrossCategoryTotalInputs();

        // preCalculate
        preCalculate();

        // apply decision strategies for ordering solution enumeration
        applyDecisionStrategies();

        // midCalculate
        initRules(moduleAlgFile, CalcStage.MID);
        // postCalculate
    }

    protected Object moduleAlgFile() {
        return this;
    }

    private void initCrossCategoryTotalInputs() {
        ModuleInput input = getModuleInput();
        if (input == null || input.getCrossCategoryConstraintReqs() == null
                || input.getCrossCategoryConstraintReqs().isEmpty()) {
            return;
        }
        new CrossCategoryTotalConstraintBuilder().build(this, input.getCrossCategoryConstraintReqs());
    }

    public Object ruleMethodOwner() {
        return moduleAlgFile();
    }

    // 重写父类的方法
    protected void executeRuleMethod(Rule rule, Object moduleAlgFile, Method method) {
        if (isOneManyRule(rule)) {
            executeRuleMethod4OneManyRule(rule, moduleAlgFile, method);
        } else {
            super.executeRuleMethod(rule, moduleAlgFile, method);
        }
    }

    private boolean isOneManyRule(Rule rule) {
        return rule.getLeftCardinality() == Cardinality.ONE && rule.getRightCardinality() == Cardinality.MANY;
    }

    // 执行类似logicAB1(1-*)的用例
    private void executeRuleMethod4OneManyRule(Rule rule, Object moduleAlgFile, Method method) {
        String leftPartCategoryCode = rule.getLeftCategoryCode();
        String rightPartCategoryCode = rule.getRightCategoryCode();
        if (Strings.isEmpty(leftPartCategoryCode) || Strings.isEmpty(rightPartCategoryCode)) {
            log.error("leftPartCategoryCode or rightPartCategoryCode is null, cannot execute one many rule");
            throw new AlgLoaderException(
                    "leftPartCategoryCode or rightPartCategoryCode is null, cannot execute one many rule");
        }
        SingleInstPartCategoryAlgImpl leftPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) getPartCategoryAlg(
                leftPartCategoryCode);
        PartCategoryAlgImpl rightPartCategoryAlgImpl = getPartCategoryAlg(
                rightPartCategoryCode);
        if (leftPartCategoryAlgImpl == null || rightPartCategoryAlgImpl == null) {
            log.error("leftPartCategoryAlgImpl or rightPartCategoryAlgImpl is null, cannot execute one many rule");
            throw new AlgLoaderException(
                    "leftPartCategoryAlgImpl or rightPartCategoryAlgImpl is null, cannot execute one many rule");
        }
        if (rightPartCategoryAlgImpl instanceof MultiInstPartCategoryAlgImpl) {
            MultiInstPartCategoryAlgImpl multiInstPartCategoryAlgImpl =
                    (MultiInstPartCategoryAlgImpl) rightPartCategoryAlgImpl;
            for (SingleInstPartCategoryAlgImpl rightPartCategoryAlgImplItem : multiInstPartCategoryAlgImpl
                    .getPartCategoryInsts()) {

                currentLeftPartCategoryAlgImpl = leftPartCategoryAlgImpl;
                currentRightPartCategoryAlgImpl = rightPartCategoryAlgImplItem;
                currrentRulePostName = currentLeftPartCategoryAlgImpl.getCategoryCode() + "_"
                        + currentRightPartCategoryAlgImpl.getCategoryCode() + ".I"
                        + currentRightPartCategoryAlgImpl.getInstId();
                super.executeRuleMethod(rule, moduleAlgFile, method);
                currentLeftPartCategoryAlgImpl = null;
                currentRightPartCategoryAlgImpl = null;
            }
        } else {
            currentLeftPartCategoryAlgImpl = leftPartCategoryAlgImpl;
            currentRightPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) rightPartCategoryAlgImpl;
            currrentRulePostName = currentLeftPartCategoryAlgImpl.getCategoryCode() + "_"
                    + currentRightPartCategoryAlgImpl.getCategoryCode() + ".I"
                    + currentRightPartCategoryAlgImpl.getInstId();
            super.executeRuleMethod(rule, moduleAlgFile, method);
            currentLeftPartCategoryAlgImpl = null;
            currentRightPartCategoryAlgImpl = null;
        }

    }

    private ModuleInput getModuleInput() {
        return (ModuleInput) this.moduleInput;
    }

    protected void initInput() {
        super.initInput(moduleAlgFile());

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
                this.addPartEqualityInModule(partInst.getCode(), partInst.getQuantity());
            }
        }

        // 分类的input初始化
        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initInput(moduleAlgFile());
        }

    }

    private void addPartEqualityInModule(String partCode, int partQuantity) {
        if (getPartVar(partCode) != null) {
            addPartEquality(partCode, partQuantity);
            return;
        }
        for (PartCategoryAlgImpl partCategoryAlg : getPartCategoryAlgs()) {
            if (partCategoryAlg instanceof MultiInstPartCategoryAlgImpl multiInstPartCategoryAlg) {
                for (SingleInstPartCategoryAlgImpl singleInstPartCategoryAlg : multiInstPartCategoryAlg
                        .getPartCategoryInsts()) {
                    if (singleInstPartCategoryAlg.getPartVar(partCode) != null) {
                        singleInstPartCategoryAlg.addPartEquality(partCode, partQuantity);
                        return;
                    }
                }
            } else if (partCategoryAlg instanceof ModuleBaseAlgImpl moduleBaseAlg
                    && moduleBaseAlg.getPartVar(partCode) != null) {
                moduleBaseAlg.addPartEquality(partCode, partQuantity);
                return;
            }
        }
        log.error("PartVarImpl not found for code: {}", partCode);
        throw new AlgLoaderException("PartVarImpl not found for code: " + partCode);
    }

    /**
     * 初始化Module后
     * 
     * @param module
     * @param moduleAlg
     */
    protected void afterInitData(IModule module, Object moduleAlg) {

    }

    private void preCalculate() {
        log.info("preCalculate: module={}", getModule().getCode());
        // 暂时支持模块级前置计算，主要是对控制变量进行初始化
        if (!(module instanceof Module)) {
            return;
        }
        initRules(moduleAlgFile(), CalcStage.PRE);
    }

    protected void initData(AlgCPModel model, IModule module,
            ModuleInput moduleInput,
            Object moduleAlgFile) {
        // 调用基类初始化
        super.initData(model, module, moduleInput, moduleAlgFile);

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
                if (pc4partCategoryInput == null) {
                    // No constraint for this PartCategory, create a default input
                    PartCategoryInput defaultInput = new PartCategoryInput();
                    defaultInput.setFilteredCategory(partCategory);
                    pc4partCategoryInput = defaultInput;
                }
                if (partCategory.isSupportMultiInst() && pc4partCategoryInput instanceof MultiInstPartCategoryInput) {
                    pcAlg = new MultiInstPartCategoryAlgImpl();
                    MultiInstPartCategoryInput multiInstPartCategoryInput =
                            (MultiInstPartCategoryInput) pc4partCategoryInput;
                    pcAlg.initData(model, (IModule) multiInstPartCategoryInput, pc4partCategoryInput, moduleAlgFile);
                } else {
                    pcAlg = new SingleInstPartCategoryAlgImpl();
                    pcAlg.initData(model, (IModule) partCategory, pc4partCategoryInput, moduleAlgFile);
                }
                partCategoryAlgs.put(categoryCode, (PartCategoryAlgImpl) pcAlg);
            }
        }
        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    protected void initRules(Object moduleAlgFile, CalcStage calcStage) {
        Map<String, Method> allRuleMethods = buildAllRuleMethods(module, moduleAlgFile);
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
    public Map<String, PartVarImpl> getPartMap() {
        return partMap;
    }

    /**
     * 获取所有参数变量
     *
     * @return 参数变量映射
     */
    public Map<String, ParaVarImpl> getParaMap() {
        return paraMap;
    }

    public ParaVarImpl getCurrentOrModuleParaVar(String code) {
        if (currentModuleAlg != null) {
            ParaVarImpl contextParaVar = currentModuleAlg.getParaVar(code);
            if (contextParaVar != null) {
                return contextParaVar;
            }
        }
        return getParaVar(code);
    }

    @Override
    protected Object newPartVar(PartVarImpl internalPartVar) {
        return internalPartVar;
    }

    @Override
    protected Object newParaVar(ParaVarImpl internalParaVar) {
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
        PartsExpr left = filterPartExpr(leftPartsExprStr, true);
        PartsExpr right = filterPartExpr(rightPartsExprStr, false);

        if (left.isEmpty4FilterPars() || right.isEmpty4FilterPars()) {
            log.info("Skip inCompatible constraint: {} - left or right filter is empty", ruleCode);
            return;
        }

        compatibleConstraintAlg.addCompatibleConstraintInCompatible(ruleCode + currrentRulePostName, left, right);
    }

    private PartsExpr filterPartExpr(String partExprStr, boolean isLeft) {
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

        PartCategoryAlgImpl partCategoryAlgImpl = isLeft ? currentLeftPartCategoryAlgImpl
                : currentRightPartCategoryAlgImpl;
        if (partCategoryAlgImpl == null) {
            // 兼容以前的
            partCategoryAlgImpl = this.getPartCategoryAlg(partCategoryCode);
        } else {
            if (!partCategoryAlgImpl.getCategoryCode().equals(partCategoryCode)) {
                // 打日志，抛异常
                log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
                throw new AlgLoaderException("partCategoryAlgImpl is invalid, cannot filter part expr");
            }
        }

        if (partCategoryAlgImpl == null) {
            // 打日志，抛异常
            log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
            return partsExpr;
        }
        // 转换为PartVar
        partsExpr.setPartVars(partCategoryAlgImpl.getAllPartVars(""));
        Pair<List<PartVarImpl>, List<PartVarImpl>> partVars = partCategoryAlgImpl.filterAllPartVars(filterConditionStr);
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
    public List<PartVarImpl> toFilterPartVar(PartCategoryAlgImpl partCategoryAlgImpl, String filtedConditionStr) {
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
    public List<PartVarImpl> toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, List<Part> parts) {
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
     * @return PartVarImpl，如果不存在则返回null
     */
    public PartVarImpl toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, Part part) {
        PartVarImpl partVar = partCategoryAlgImpl.getPartVar(part.getCode());
        if (partVar == null) {
            // 打日志，抛异常
            log.error("PartVarImpl not found for code: {}", part.getCode());
            throw new AlgLoaderException("PartVarImpl not found for code: " + part.getCode());
        }
        return partVar;
    }

    /**
     * 通用的部件求和方法，根据指定的变量获取函数计算总和
     * 
     * @param cofAttrCode          属性代码
     * @param varGetter          从PartVar获取LinearArgument的函数（如getIsSelected或getQty）
     * @param varName            变量名称（如"isSelected"或"qty"），用于构建字符串表达式
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    // @Override
    protected PartAlgCPLinearExpr sum4Parts(PartCategoryAlgImpl partCategoryAlgImpl, String cofAttrCode,
            Function<PartVarImpl, LinearArgument> varGetter, String varName, String filtedConditionStr) {
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
                partCategoryAlgImpls, cofAttrCode, PartVarImpl.ISSELECTED_SHORT_NAME, PartVarImpl::getIsSelected,
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
        return buildSumExpr(partCategoryAlgImpls, cofAttrCode, PartVarImpl.QTY_SHORT_NAME, PartVarImpl::getQty,
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
     * @param cofAttrCode          属性代码
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */

    public PartAlgCPLinearExpr sum4Selected(String cofAttrCode,
            String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg, cofAttrCode, PartVarImpl::getIsSelected,
                PartVarImpl.ISSELECTED_SHORT_NAME, filtedConditionStr);
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
     * @param cofAttrCode          属性代码
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg,
                cofAttrCode, PartVarImpl::getQty, PartVarImpl.QTY_SHORT_NAME, filtedConditionStr);
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
        ParaVarImpl sumParaVar = this.getParaVar(sumParaCode);
        if (sumParaVar == null) {
            // 打日志，抛异常
            log.error("ParaVarImpl not found for code: {}", sumParaCode);
            throw new AlgLoaderException("ParaVarImpl not found for code: " + sumParaCode);
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
                ParaVarImpl instParaVar = partCategoryAlgImpl.getParaVar(sumQuantityCode);
                if (instParaVar == null || instParaVar.getInputValue() == null) {
                    // 打日志，抛异常
                    log.error("ParaVarImpl not found for code: {}", sumQuantityCode);
                    // 暂时不抛异常 throw new AlgLoaderException("ParaVarImpl not found for code: " +
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

    /**
     * Apply decision strategies to part category variables.
     * Called after all variables are created and before rule initialization.
     */
    private void applyDecisionStrategies() {
        ModuleInput mi = getModuleInput();
        if (mi == null || mi.getPartCategoryInputs() == null) {
            return;
        }
        for (PartCategoryInputBase input : mi.getPartCategoryInputs()) {
            PartConstraintReq orgReq = input.getOrgReq();
            if (orgReq == null) {
                continue;
            }
            List<StrategyConfig> strategies = orgReq.getDecisionStrategies();
            if (strategies == null || strategies.isEmpty()) {
                continue;
            }

            String partCategoryCode = input.getPartCategoryCode();

            if (input instanceof MultiInstPartCategoryInput) {
                MultiInstPartCategoryInput multiInput = (MultiInstPartCategoryInput) input;
                for (PartCategoryInput pcInput : multiInput.getPartCategoryInputs()) {
                    PartCategoryAlgImpl pcAlg = getPartCategoryAlg(pcInput.getPartCategoryCode());
                    if (pcAlg != null) {
                        applyStrategies(pcAlg, strategies);
                    }
                }
            } else {
                PartCategoryAlgImpl pcAlg = getPartCategoryAlg(partCategoryCode);
                if (pcAlg != null) {
                    applyStrategies(pcAlg, strategies);
                }
            }
        }
    }

    private void applyStrategies(PartCategoryAlgImpl pcAlg, List<StrategyConfig> strategies) {
        for (StrategyConfig config : strategies) {
            if (config.getStrategyType() == null || config.getStrategyType() == StrategyType.UNSPECIFIED) {
                continue;
            }
            if (config.getSortAttributeCode() == null || config.getSortAttributeCode().isEmpty()) {
                log.warn("Strategy has no sortAttributeCode, skipping");
                continue;
            }
            applySingleStrategy(pcAlg, config);
        }
    }

    private void applySingleStrategy(PartCategoryAlgImpl pcAlg, StrategyConfig config) {
        List<PartVarImpl> partVars = pcAlg.getAllPartVars("");
        if (partVars.isEmpty()) {
            log.warn("No part vars found for category {}, skipping strategy", pcAlg.getCategoryCode());
            return;
        }

        String sortAttr = config.getSortAttributeCode();
        boolean ascending = config.getStrategyType() == StrategyType.ASCENDING;

        // Sort part vars by the sort attribute
        partVars.sort(Comparator.comparingInt(pv -> getSortValue(pv, sortAttr)));

        // Reverse for descending
        if (!ascending) {
            java.util.Collections.reverse(partVars);
        }

        // Get isSelected BoolVars (which are IntVars) in sorted order
        IntVar[] vars = partVars.stream()
                .map(PartVarImpl::getIsSelected)
                .toArray(IntVar[]::new);

        // SELECT_MAX_VALUE: try selecting parts first, which means preferred parts
        // (sorted first) get selected in early solutions
        model.addDecisionStrategy(vars,
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MAX_VALUE);

        log.info("Applied decision strategy: {} on {} vars for category {}, sortAttr={}",
                config.getStrategyType(), vars.length, pcAlg.getCategoryCode(), sortAttr);
    }

    /**
     * Get the integer sort value for a part variable by attribute code.
     * Supports basic attributes like "price" and dynamic attributes.
     */
    private int getSortValue(PartVarImpl partVar, String attrCode) {
        if ("price".equals(attrCode) && partVar.getBase() instanceof Part) {
            Long price = ((Part) partVar.getBase()).getPrice();
            return price != null ? price.intValue() : 0;
        }
        return partVar.getAttr4Int(attrCode);
    }
}
