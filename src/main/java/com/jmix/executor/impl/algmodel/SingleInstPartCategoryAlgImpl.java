package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.IModuleInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.southinf.cp.AlgCPLinearArgument;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;

import com.google.ortools.sat.LinearArgument;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class SingleInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {
    /**
     * 实例ID
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

    /**
     * 默认构造函数
     */
    public SingleInstPartCategoryAlgImpl() {
        super();
    }

    @Override
    protected void initData(AlgCPModelImpl model, IModule module, IModuleInput moduleInput,
            IModuleAlg moduleAlgFile) {
        PartCategoryInput input = (PartCategoryInput) moduleInput;
        this.instId = input.getInstId();
        super.initData(model, module, moduleInput, moduleAlgFile);
    }

    @Override
    public void initInput(IModuleAlg moduleAlgFile) {
        PartCategoryInput input = (PartCategoryInput) (this.moduleInput);
        newAttrParaVar(input.getSumAttrParas());
        super.initInput(moduleAlgFile);
        setPartCategoryInput(input);
    }

    public void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        super.buildPriorityConstraint(getModule()); // 先构建本部件分类的优先类规则
        super.initRules(allRuleMethods, moduleAlgFile, calcStage);
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        return getPartCategory().isEnumMutiInst() ? getPartCategory().getEnumInstId() : instId;
    }

    public PartCategory getPartCategory() {
        return (PartCategory) getModule();
    }

    public String getCategoryCode() {
        return getPartCategory().getCode();
    }

    @Override
    protected VarImpl<?> newPartVar(PartVarImpl internalPartVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newPartVar'");
    }

    @Override
    protected VarImpl<?> newParaVar(ParaVarImpl internalParaVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newParaVar'");
    }

    @Override
    public String toString() {
        return getPartCategory().getCode() + "[" + getPartCategory().getEnumInstId() + "]";
    }

    @Override
    public List<Part> getAllAtomicParts() {
        return getPartCategory().getAllAtomicParts();
    }

    @Override
    public List<Part> getAtomicParts() {
        return getPartCategory().getAtomicParts();
    }

    public ParaVarImpl getSumSumParaByAttr(String attrCode) {
        // 如果一个支持多实例的分类，仅初始化了一个分类，则需要把访问sumsum转变为sum的访问
        return getSumParaByAttr(attrCode);
    }

    public ParaVarImpl getSumParaByAttr(String attrCode) {
        ParaVarImpl paraVar = super.getParaVar(AttrParaType.Sum.name() + AttrPara.CODE_SEPARATOR + attrCode);
        if (paraVar == null) {
            log.error("ParaVarImpl not found for attrCode: {}", attrCode);
            throw new AlgLoaderException("ParaVarImpl not found for attrCode: " + attrCode);
        }
        return paraVar;
    }

    public ParaVarImpl getSumParaByAttrInternal(String attrCode) {
        ParaVarImpl paraVar = paraMap.get(AttrParaType.Sum.name() + AttrPara.CODE_SEPARATOR + attrCode);
        return paraVar;
    }

    protected void sumFunConstraint(ModuleBaseAlgImpl moduleBaseAlgImplTmp,
            PartCategoryInputBase partConstraintTmp) {
        SingleInstPartCategoryAlgImpl singleInstPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) moduleBaseAlgImplTmp;
        PartCategoryInput partConstraint = (PartCategoryInput) partConstraintTmp;
        PartAlgCPLinearExprImpl sumFunExpr = buildSumExpr(
                singleInstPartCategoryAlgImpl,
                partConstraint.getSumAttrCode(), "Q",
                PartVarImpl::getQty, "");
        // 应用约束
        ComparisonOperator operator = ComparisonOperator.fromSymbol(partConstraint.getComparator());
        operator.applyConstraint(model, sumFunExpr, partConstraint.getLeftValue());
        log.info("Priority-Added sum constraint: {} for {}",
                sumFunExpr.getExprStr(),
                partConstraint.getOrgReq() != null ? partConstraint.getOrgReq().toString() : "null");
    }

    /**
     * 对选中的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExprImpl sum4Selected(String filtedConditionStr) {
        return sum4Parts(this, null,
                p -> (LinearArgument) p.getIsSelected().build(),
                PartVarImpl.ISSELECTED_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对选中的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExprImpl sum4Selected(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts(this, cofAttrCode,
                p -> (LinearArgument) p.getIsSelected().build(),
                PartVarImpl.ISSELECTED_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 对指定部件分类的部件求和
     *
     * @param partCategoryAlgImpl 部件分类算法实现
     * @param cofAttrCode         属性代码，如果为null或空则使用默认值1
     * @param varGetter           变量获取函数
     * @param varName             变量名称
     * @param filtedConditionStr  过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    protected PartAlgCPLinearExprImpl sum4Parts(PartCategoryAlgImpl partCategoryAlgImpl, String cofAttrCode,
            Function<PartVarImpl, ? extends LinearArgument> varGetter, String varName, String filtedConditionStr) {
        PartAlgCPLinearExprImpl expr = buildSumExpr(
                partCategoryAlgImpl,
                cofAttrCode, varName, varGetter,
                filtedConditionStr);
        return expr;
    }

    /**
     * 对数量的部件求和（不带属性系数）
     *
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExprImpl sum4Quantity(String filtedConditionStr) {
        return sum4Quantity(null, filtedConditionStr);
    }

    /**
     * 对数量的部件求和（带属性系数）
     *
     * @param cofAttrCode        属性代码，如果为null或空则使用默认值1
     * @param filtedConditionStr 过滤条件字符串
     * @return 求和后的AlgCPLinearExpr表达式
     */
    public PartAlgCPLinearExprImpl sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts(this, cofAttrCode, PartVarImpl::getQty,
                PartVarImpl.QTY_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 创建部件线性表达式
     *
     * @param name 表达式名称
     * @return 新的PartAlgCPLinearExprImpl
     */
    public PartAlgCPLinearExprImpl newPartLinearExpr(String name) {
        return new PartAlgCPLinearExprImpl(name);
    }

    /**
     * 添加小于等于约束
     */
    public void addLessOrEqual(AlgCPLinearExprImpl expr, int value) {
        model.addLessOrEqual(expr, value);
    }

    /**
     * 添加大于等于约束
     */
    public void addGreaterOrEqual(AlgCPLinearExprImpl expr, int value) {
        model.addGreaterOrEqual(expr, value);
    }
}