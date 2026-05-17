package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartUtils;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.IModuleInput;
import com.jmix.executor.impl.ModuleInstAccessor;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.southinf.IModuleAlg;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 算法基类
 * 定义模块/部件分类级别算法的公共方法
 * 
 * @since 2025-12-27
 */
@Slf4j
public abstract class ModuleBaseAlgImpl implements IModuleAlg {

    /**
     * CP约束求解模型实例
     */
    protected AlgCPModel model;

    /**
     * module的基础信息
     */
    protected IModule module;

    /**
     * 部件变量映射表，存储code到PartVar的映射
     */
    protected Map<String, PartVar> partMap = new LinkedHashMap<>();

    /**
     * 参数变量映射表，存储code到ParaVar的映射
     */
    protected Map<String, ParaVar> paraMap = new LinkedHashMap<>();

    /**
     * 受显式约束控制可见性的编码集合，跳过默认绑定
     */
    protected Set<String> codesOfHiddenConstraint = new HashSet<>();

    /**
     * 所有规则方法的映射表，存储规则代码到对应方法的映射
     */
    protected Map<String, Method> ruleMethods = new HashMap<>();

    /**
     * 优先级约束映射表，存储属性代码到优先级约束的映射，仅能支持一条优先级约束
     */
    @Getter
    @Setter
    protected PriorityConstraint priorityConstraint;

    /**
     * 兼容性约束算法实例
     */
    protected CompatibleConstraintAlg compatibleConstraintAlg;

    /**
     * 当前模块,算法当前执行模块
     */
    protected IModule currentModule;

    /**
     * 当前模块算法实例
     */
    protected ModuleBaseAlgImpl currentModuleAlg;

    /**
     * 当前部件约束
     */
    protected IModuleInput moduleInput;

    /**
     * 当前模块实例访问器，用于POST阶段读写ModuleInst
     */
    private ModuleInstAccessor currentModuleInstAccessor;

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        return ModuleInst.DEFAULT_INSTANCE_ID;
    }

    /**
     * 添加和隐藏相关的约束的Var
     * 
     * @param hiddenVars 需要添加隐藏约束的变量数组
     */
    protected void addVarAboutHiddenConstraints(Var<?>... hiddenVars) {
        for (Var<?> v : hiddenVars) {
            codesOfHiddenConstraint.add(v.getCode());
        }
    }

    /**
     * 根据外部传入的部件约束（求和约束）设置对应的参数输入值
     *
     * @param partConstraints 部件约束列表，可以为null
     */
    protected void setPartCategoryInput(PartCategoryInputBase partCategoryInput) {
        if (null == partCategoryInput) {
            return;
        }
        setPartCategoryInputVariales(partCategoryInput);
        if (Strings.isNotEmpty(partCategoryInput.getComparator())) {
            model.addRuleSeperator("input_constraint_" + partCategoryInput.getPartCategoryCode() + this.getInstId());
            sumFunConstraint(this, partCategoryInput);
        }
    }

    protected void setPartCategoryInputVariales(PartCategoryInputBase ipt) {
        if (!Strings.isNotEmpty(ipt.getSumAttrCode())) {
            return;
        }
        // 多实例后，不支持这种方式，ontoCode = pt.getFilteredCategory().getCode(); String paraCode =
        // ontoCode + "Sum" + pt.getSumAttrCode();
        String paraCode = ipt.getAttrType().name() + AttrPara.CODE_SEPARATOR + ipt.getSumAttrCode();

        ParaVar pVar = this.getParaVar(paraCode);
        if (pVar == null) {
            pVar = newAttrParaVar(paraCode);
            log.info("Dynamic created para: {}", paraCode);
        } else {
            log.info("Para already exists for paraCode: {}, skipping", paraCode);
        }

        pVar.setInputValue(ipt.getLeftValue());
        pVar.setIsHasInputed(Boolean.TRUE);
        log.info("Set input variable {} = {}", paraCode, ipt.getLeftValue());

    }

    /**
     * 设置默认可见性约束
     * 对于没有显式控制可见性的变量，设置 isHiddenVar == 0（即默认可见）
     * 遍历所有变量，为没有显式可见性控制的变量设置默认可见状态
     * 
     * @throws AlgLoaderException 异常
     */
    protected void setDefaultVisibilityConstraints() {
        boolean isFirst = true;
        for (ParaVar paraVar : this.getParaVars()) {
            if (codesOfHiddenConstraint.contains(paraVar.getCode())) {
                continue;
            }
            if (isFirst) {
                this.model.setRelax4SysRule("hiddensrule");
                isFirst = false;
            }
            if (paraVar.getBase().getAssignType() == AssignType.CALC) {
                // 暂时没有添加到松弛变量里
                model.addEquality(paraVar.getIsHidden(), 0);
            }
        }
        for (PartVar partVar : this.getPartVars()) {
            if (codesOfHiddenConstraint.contains(partVar.getCode())) {
                continue;
            }
            if (isFirst) {
                this.model.setRelax4SysRule("hiddensrule");
                isFirst = false;
            }
            model.addEquality(partVar.getIsHidden(), 0);
        }
    }

    protected PartAlgCPLinearExpr buildSumExprInternal(PartAlgCPLinearExpr algExpr,
            PartCategoryAlgImpl partCategoryAlgImpl, String attrCode,
            String varName, Function<PartVar, LinearArgument> varGetter, String filtedConditionStr) {
        boolean isWithoutAttr = attrCode == null || attrCode.isEmpty();
        List<PartVar> partVars = partCategoryAlgImpl.getAllPartVars(filtedConditionStr);
        for (PartVar partVar : partVars) {
            int attrValue;
            if (isWithoutAttr) {
                attrValue = 1;
            } else if (PartConstantAttr.Quantity.getCode().equals(attrCode)) {
                attrValue = 1;
            } else {
                attrValue = partVar.getAttr4Int(attrCode);
            }
            algExpr.addTerm(partCategoryAlgImpl.getCategoryCode(), partVar, (IntVar) varGetter.apply(partVar),
                    attrValue, varName);
        }

        return algExpr;
    }

    protected PartAlgCPLinearExpr buildSumExpr(List<PartCategoryAlgImpl> partCategoryAlgImpls, String attrCode,
            String varName, Function<PartVar, LinearArgument> varGetter, String filtedConditionStr) {
        PartAlgCPLinearExpr algExpr = new PartAlgCPLinearExpr(
                partCategoryAlgImpls.get(0).getCategoryCode() + "_" + partCategoryAlgImpls.get(0).getInstId()
                        + "_sumPars_"
                        + (attrCode == null ? "" : attrCode) + "_" + varName);
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            buildSumExprInternal(algExpr, partCategoryAlgImpl, attrCode, varName, varGetter, filtedConditionStr);
        }
        return algExpr;
    }

    protected PartAlgCPLinearExpr buildSumExpr(PartCategoryAlgImpl partCategoryAlgImpl, String attrCode,
            String varName, Function<PartVar, LinearArgument> varGetter, String filtedConditionStr) {
        PartAlgCPLinearExpr algExpr = new PartAlgCPLinearExpr(
                partCategoryAlgImpl.getCategoryCode() + "_" + partCategoryAlgImpl.getInstId() + "_sumPars_"
                        + (attrCode == null ? "" : attrCode) + "_" + varName);
        return buildSumExprInternal(algExpr, partCategoryAlgImpl, attrCode, varName, varGetter, filtedConditionStr);
    }

    protected void sumFunConstraint(ModuleBaseAlgImpl moduleBaseAlgImpl,
            PartCategoryInputBase partConstraint) {
        // 子类继承实现
    }

    /**
     * 添加松弛目标函数
     * 目标函数：最小化需要松弛的约束数量（带权重）
     */
    public void addRelaxObjectFunction() {
        if (!model.isIsAttachRelax()) {
            return;
        }

        // 目标函数：最小化需要松弛的约束数量（带权重）
        List<RelaxVar> relaxVars = new ArrayList<>(model.getRelaxVarMap().values());
        if (!relaxVars.isEmpty()) {
            // 构建加权目标函数：min(relaxVar[0].value * relaxVar[0].weight + relaxVar[1].value *
            // relaxVar[1].weight + ...)
            AlgCPLinearExpr[] weightedTerms = new AlgCPLinearExpr[relaxVars.size()];
            for (int i = 0; i < relaxVars.size(); i++) {
                RelaxVar relaxVar = relaxVars.get(i);
                weightedTerms[i] = AlgCPLinearExpr.term(relaxVar.getValue(), relaxVar.getWeight());
            }
            AlgCPLinearExpr objectiveExpr = AlgCPLinearExpr.sum(weightedTerms);
            model.minimize(objectiveExpr);
            log.info("relax: -----relaxation objective function with {} relaxation variables", relaxVars.size());
        }
    }

    /**
     * 执行单个规则方法
     *
     * @param ruleCode 规则代码
     * @param method   规则方法
     * @throws AlgLoaderException 异常
     */
    protected void executeRuleMethod(Rule rule, Method method) {
        setCurrentModule4Rule(this, this);
        executeRuleMethod(rule, this, method);
        setCurrentModule4Rule(this, null);
    }

    protected void executeRuleMethod(Rule rule, IModuleAlg moduleAlgFile, Method method) {
        if (method == null) {
            log.error("Rule method not found for execution: " + rule.getCode());
            throw new AlgLoaderException("Rule method not found for execution: " + rule.getCode());
        }
        try {
            // 设置当前松弛变量名称
            this.model.setRelax4CustomRule(rule.getCode());

            // 执行规则方法
            method.setAccessible(true);
            method.invoke(moduleAlgFile);
            log.info("Executed rule method: {}", method.getName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to execute rule method: " + method.getName(), e);
            throw new AlgLoaderException("Failed to execute rule method: " + method.getName(), e);
        }
    }

    /**
     * 模型初始化后的回调方法
     * 子类可以重写此方法来实现自定义的初始化逻辑
     * 
     * @param model CP模型实例
     */
    protected void initModelAfter(AlgCPModel model) {

    }

    /**
     * 初始化所有变量（paras和parts）
     * 遍历module的paras，创建ParaVar并放到paraMap
     * 遍历module的parts，创建PartVar并放到partMap
     */
    protected void initAll(IModuleAlg moduleAlgFile) {
        if (module == null) {
            log.warn("Module is null, skip initAll");
            return;
        }

        // 初始化所有paras
        for (Para para : module.getParas()) {
            ParaVar paraVar = initParaVar(para);
            paraMap.put(para.getCode(), paraVar);
        }
        log.info("Initialized {} para variables", paraMap.size());

        // 初始化所有parts（原子部件）
        for (Part part : module.getAtomicParts()) {
            PartVar partVar = initPartVar(part);
            partMap.put(part.getCode(), partVar);
        }
        log.info("Initialized {} part variables", partMap.size());
    }

    /**
     * 初始化AttrParas，创建对应的Para和ParaVar
     * 
     * @param attrParas 属性参数列表
     */
    protected void newAttrParaVar(List<AttrPara> attrParas) {
        if (attrParas == null || attrParas.isEmpty()) {
            return;
        }
        for (AttrPara attrPara : attrParas) {
            newAttrParaVar(attrPara);
        }
    }

    /**
     * 初始化单个AttrPara，创建对应的Para和ParaVar
     * 
     * @param attrPara 属性参数
     */
    protected void newAttrParaVar(AttrPara attrPara) {
        if (attrPara == null || attrPara.getAttrCode() == null) {
            return;
        }
        String paraCode = attrPara.getType().name() + AttrPara.CODE_SEPARATOR + attrPara.getAttrCode();
        newAttrParaVar(paraCode);
    }

    protected ParaVar newAttrParaVar(String paraCode) {
        if (paraMap.containsKey(paraCode)) {
            log.error("Para already exists for attrCode: {}, skipping", paraCode);
            throw new AlgLoaderException("Para already exists for attrCode: " + paraCode);
        }
        Para para = new Para();
        para.setCode(paraCode);
        para.setAssignType(AssignType.INPUT);
        // 从对应的属性里面获取数据issue（min，max等）
        para.setParaType(ParaType.INTEGER);
        // ModuleBase tempModule = (ModuleBase) module;
        // tempModule.addPara(para);
        ParaVar pVar = initParaVar(para);
        paraMap.put(paraCode, pVar);
        return pVar;
    }

    /**
     * 初始化参数变量
     * 根据参数类型创建对应的变量（INTEGER/ENUM）
     * 
     * @param para 参数对象
     * @return 创建的参数变量
     */
    protected ParaVar initParaVar(Para para) {
        String code = para.getCode();
        ParaVar paraVar = new ParaVar();
        paraVar.setBase(para);

        if (para.getAssignType() == AssignType.INPUT) {
            return paraVar;
        }
        String ipf = toInstPrefix();
        switch (para.getParaType()) {
            case INTEGER:
                paraVar.setValue(newIntVar(Integer.parseInt(para.getMinValue()), Integer.parseInt(para.getMaxValue()),
                        f(ParaVar.VALUE_PATTERN, ipf, code)));
                break;
            case ENUM:
                List<DynamicAttributerOption> options = para.getOptions();
                if (options == null || options.isEmpty()) {
                    log.error("Para options not found for code: {}", code);
                    throw new AlgLoaderException("Para options not found for code: " + code);
                }
                paraVar.setValue(newIntVarFromDomain(para.getOptionIds(), f(ParaVar.VALUE_PATTERN, ipf, code)));

                for (DynamicAttributerOption option : options) {
                    ParaOptionVar optionVar = createParaOptionVar(para.getCode(), option.getCode());
                    paraVar.getOptionSelectVars().put(option.getCodeId(), optionVar);
                }
                paraVar.getOptionSelectVars().forEach((optionId, optionVar) -> {
                    model.addEquality(paraVar.getValue(), optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
                    model.addDifferent(paraVar.getValue(), optionId)
                            .onlyEnforceIf(optionVar.getIsSelectedVar().not());
                });
                break;
            default:
                log.error("Para type not supported: {}", para.getParaType());
                throw new AlgLoaderException("Para type not supported: " + para.getParaType());
        }
        paraVar.setIsHidden(newBoolVar(f(ParaVar.HIDDEN_PATTERN, ipf, code)));
        paraVar.setInstId(getInstId());
        return paraVar;
    }

    private String toInstPrefix() {
        final String prefix = (getInstId() == ModuleInst.DEFAULT_INSTANCE_ID ? "" : "I" + getInstId() + "_");
        return prefix;
    }

    /**
     * 初始化部件变量
     * 创建部件的数量、选中状态和隐藏状态变量
     * 
     * @param part 部件对象
     * @return 创建的部件变量
     */
    protected PartVar initPartVar(Part part) {
        String ipf = toInstPrefix();
        PartVar partVar = new PartVar();
        partVar.setBase(part);
        partVar.setQty(newIntVar(0, part.getMaxQuantity(), f(PartVar.QTY_PATTERN, ipf, part.getCode())));
        partVar.setIsHidden(newBoolVar(f(PartVar.HIDDEN_PATTERN, ipf, part.getCode())));
        partVar.setIsSelected(newBoolVar(f(PartVar.ISSELECTED_PATTERN, ipf, part.getCode())));
        partVar.setInstId(getInstId());
        // 添加Qty和IsSelected的关系
        model.addGreaterOrEqual(partVar.getQty(), 1).onlyEnforceIf(partVar.getIsSelected());
        model.addEquality(partVar.getQty(), 0).onlyEnforceIf(partVar.getIsSelected().not());

        return partVar;
    }

    /**
     * 获取所有PartVar列表
     * 
     * @return PartVar列表
     */
    public List<PartVar> getPartVars() {
        return new ArrayList<>(partMap.values());
    }

    /**
     * 获取所有ParaVar列表
     * 
     * @return ParaVar列表
     */
    public List<ParaVar> getParaVars() {
        return new ArrayList<>(paraMap.values());
    }

    /**
     * 子类重写此方法来实现自定义变量初始化逻辑
     */
    protected void onInitCustomVariables() {
        // 默认空实现，子类可以重写
    }

    /**
     * 过滤出指定fatherCode的规则
     *
     * @param rules      所有规则列表
     * @param fatherCode 父级代码，如果为null则获取模块级别的规则
     * @return 过滤后的规则列表
     */
    protected List<Rule> filterRulesByFatherCode(List<Rule> rules, String fatherCode) {
        if (rules == null) {
            return new java.util.ArrayList<>();
        }
        return rules.stream()
                .filter(rule -> {
                    if (fatherCode == null) {
                        return rule.getFatherCode() == null || rule.getFatherCode().isEmpty();
                    }
                    return fatherCode.equals(rule.getFatherCode());
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据module.getAllRules()构建所有规则方法的映射
     *
     * @param module 模块对象
     * @return 规则代码到方法对象的映射
     */
    protected Map<String, Method> buildAllRuleMethods(IModule module) {
        return buildAllRuleMethods(module, this);
    }

    protected Map<String, Method> buildAllRuleMethods(IModule module, IModuleAlg moduleAlgFile) {
        if (module == null || module.getAllRules() == null) {
            return new HashMap<>();
        }

        // 获取当前类的所有方法
        Method[] methods = moduleAlgFile.getClass().getDeclaredMethods();
        Map<String, Method> allMethods = new HashMap<>();

        // 构建方法名到Method对象的映射
        for (Method method : methods) {
            allMethods.put(method.getName(), method);
        }

        // 根据module.getAllRules()来构建ruleMethods
        Map<String, Method> ruleMethods = new HashMap<>();
        for (Rule rule : module.getAllRules()) {
            String ruleCode = rule.getCode();
            Method method = allMethods.get(ruleCode);
            if (method != null) {
                ruleMethods.put(ruleCode, method);
                log.info("Built rule method mapping: {} -> {}", ruleCode, method.getName());
                // // 检查是否是PriorityRule，如果是则构建优先级约束
                // if (RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName())) {
                // buildPriorityConstraint(rule);
                // }
            } else {
                log.warn("Rule method not found for rule code: {} in class {}", ruleCode, this.getClass().getName());
            }
        }

        log.info("Built {} rule methods from module rules", ruleMethods.size());
        return ruleMethods;
    }

    protected void buildPriorityConstraint(IModule module) {
        module.getPriorityRules().forEach(rule -> buildPriorityConstraint(rule));
        for (Rule rule : module.getPriorityRules()) {
            buildPriorityConstraint(rule);
        }
    }

    /**
     * 构建优先级约束
     * 
     * @param rule 规则对象
     */
    protected void buildPriorityConstraint(Rule rule) {
        if (rule.getRawCode() == null || !(rule.getRawCode() instanceof PriorityRuleSchema)) {
            log.warn("Rule rawCode is not PriorityRuleSchema for rule: {}", rule.getCode());
            return;
        }
        PriorityConstraint pConstraint = new PriorityConstraint();
        pConstraint.setRule(rule);
        setPriorityConstraint(pConstraint);
    }

    /**
     * 将变量写回字段，保证规则使用变量是同一个
     */
    protected void writeBackToFields(IModuleAlg moduleAlgFile) {
        Map<String, Field> fieldMap = getAllFieldVariables(moduleAlgFile);
        ModuleBaseAlgImpl algFileImpl = (ModuleBaseAlgImpl) moduleAlgFile;
        // 处理PartVar
        for (Map.Entry<String, PartVar> entry : partMap.entrySet()) {
            String code = entry.getKey();
            PartVar partVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Var<?> tVar = algFileImpl.newPartVarForField(partVar, field);
                setVariableField(moduleAlgFile, tVar, field);
            }
        }

        // 处理ParaVar
        for (Map.Entry<String, ParaVar> entry : paraMap.entrySet()) {
            String code = entry.getKey();
            ParaVar paraVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Var<?> tVar = algFileImpl.newParaVarForField(paraVar, field);
                setVariableField(moduleAlgFile, tVar, field);
            }
        }
    }

    /**
     * 执行本层的规则
     *
     * @param allRuleMethods 所有规则方法映射
     */
    protected void executeModuleRules(IModuleAlg moduleAlgFile, Map<String, Method> allRuleMethods,
            CalcStage calcStage) {
        if (module == null || module.getAllRules() == null || allRuleMethods == null) {
            return;
        }

        List<Rule> moduleRules = module.getRules(calcStage);
        executeModuleRules(moduleRules, moduleAlgFile, allRuleMethods, calcStage);
    }

    /**
     * 执行本层的规则
     *
     * @param allRuleMethods 所有规则方法映射
     */
    protected void executeModuleRules(List<Rule> rules, IModuleAlg moduleAlgFile, Map<String, Method> allRuleMethods,
            CalcStage calcStage) {
        if (rules == null || allRuleMethods == null) {
            return;
        }
        for (Rule rule : rules) {
            String ruleCode = rule.getCode();
            Method method = allRuleMethods.get(ruleCode);
            if (method == null) {
                log.error("Rule method not found for rule code: {} in class {}", ruleCode, this.getClass().getName());
                throw new AlgLoaderException("Rule method not found for rule code: " + ruleCode + " in class "
                        + this.getClass().getName());
            }
            model.addRuleSeperator(ruleCode);
            setCurrentModule4Rule(moduleAlgFile, this);
            executeRuleMethod(rule, moduleAlgFile, method);
            setCurrentModule4Rule(moduleAlgFile, null);
        }
    }

    private void setCurrentModule4Rule(IModuleAlg moduleAlgFile, ModuleBaseAlgImpl tmpModuleAlg) {
        ModuleBaseAlgImpl algFileImpl = (ModuleBaseAlgImpl) moduleAlgFile;
        algFileImpl.currentModule = (tmpModuleAlg == null ? null : tmpModuleAlg.getModule());
        algFileImpl.currentModuleAlg = tmpModuleAlg;

    }

    protected void initData(AlgCPModel model, IModule module, IModuleInput moduleInput,
            IModuleAlg moduleAlgFile) {
        // Module级别
        this.model = model;
        this.module = module;
        this.moduleInput = moduleInput;
        // 初始化兼容性约束算法实例
        this.compatibleConstraintAlg = new CompatibleConstraintAlg(model);
        initModelAfter(model);

        // 初始化本层变量（paras和parts）
        initAll(moduleAlgFile);
    }

    protected void initInput(IModuleAlg moduleAlgFile) {
        // 将变量写回字段
        writeBackToFields(moduleAlgFile);

        log.info("ModuleBaseAlgImpl initInput {}", module.getClass().getSimpleName());
    }

    public void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        // 执行本层的规则
        executeModuleRules(moduleAlgFile, allRuleMethods, calcStage);

        if (calcStage == CalcStage.MID) {
            // 设置默认可见性约束
            setDefaultVisibilityConstraints();
        }

        log.info("ModuleBaseAlgImpl initRules {}", module.getClass().getSimpleName());
    }

    /**
     * 创建部件变量, 继承类可以重载
     * 
     * @param internalPartVar 内部部件变量
     * @return 创建的部件变量
     */
    protected abstract Var<?> newPartVar(PartVar internalPartVar);

    protected Var<?> newPartVarForField(PartVar internalPartVar, Field field) {
        return newPartVar(internalPartVar);
    }

    /**
     * 创建参数变量, 继承类可以重载
     * 
     * @param internalParaVar 内部参数变量
     * @return 创建的参数变量
     */
    protected abstract Var<?> newParaVar(ParaVar internalParaVar);

    protected Var<?> newParaVarForField(ParaVar internalParaVar, Field field) {
        return newParaVar(internalParaVar);
    }

    /**
     * 设置单个变量字段
     * 
     * @param v     变量值
     * @param field 字段对象
     * @throws AlgLoaderException 异常
     */
    protected void setVariableField(Var<?> v, Field field) throws AlgLoaderException {
        setVariableField(this, v, field);
    }

    protected void setVariableField(IModuleAlg moduleAlgFile, Var<?> v, Field field) throws AlgLoaderException {
        if (field == null) {
            log.error("Field not found for code: null {}", v.getCode());
            throw new AlgLoaderException("Field not found for code: null " + v.getCode());
        }
        try {
            field.setAccessible(true);
            field.set(moduleAlgFile, v);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("Failed to write back variable to field: " + v.getCode(), e);
            throw new AlgLoaderException("Failed to write back variable to field: " + v.getCode(), e);
        }
    }

    /**
     * 获取所有字段变量
     * 
     * @return 字段映射表
     */
    protected Map<String, Field> getAllFieldVariables() { // TODO delete
        Field[] fields = this.getClass().getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldMap.put(extractCodeFromFieldName(field.getName()), field);
        }
        return fieldMap;
    }

    /**
     * 算法文件类，获取所有字段变量
     * 
     * @return 字段映射表
     */
    protected Map<String, Field> getAllFieldVariables(IModuleAlg moduleAlgFile) {
        Field[] fields = moduleAlgFile.getClass().getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldMap.put(extractCodeFromFieldName(field.getName()), field);
        }
        return fieldMap;
    }

    /**
     * 从字段名提取代码
     * 例如: colorVar -> Color, tShirt11Var -> TShirt11
     * 
     * @param fieldName 字段名
     * @return 提取的代码，如果字段名不以"Var"结尾则返回原字段名
     */
    private String extractCodeFromFieldName(String fieldName) {
        // 移除末尾的"Var"
        if (fieldName.endsWith("Var")) {
            return fieldName.substring(0, fieldName.length() - 3);
        }
        return fieldName;
    }

    /**
     * 静态方法：从指定值数组创建整数变量
     * 
     * @param model  CP模型实例
     * @param values 允许的值数组
     * @param name   变量名称
     * @return 创建的整数变量
     */
    public static IntVar newIntVarFromDomain(AlgCPModel model, long[] values, String name) {
        return model.newIntVarFromDomain(Domain.fromValues(values), name);
    }

    /**
     * 封装CpModel的newIntVar方法
     * 
     * @param left  最小值
     * @param right 最大值
     * @param name  变量名称
     * @return 创建的整数变量
     */
    protected IntVar newIntVar(long left, long right, String name) {
        return this.model.newIntVar(left, right, name);
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 单个值
     * 
     * @param value 单个值
     * @param name  变量名称
     * @return 创建的整数变量
     */
    protected IntVar newIntVarFromDomain(long value, String name) {
        return this.model.newIntVarFromDomain(value, name);
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 多个值
     * 
     * @param values 允许的值数组
     * @param name   变量名称
     * @return 创建的整数变量
     */
    protected IntVar newIntVarFromDomain(long[] values, String name) {
        return this.model.newIntVarFromDomain(values, name);
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 区间
     * 
     * @param intervals 区间数组
     * @param name      变量名称
     * @return 创建的整数变量
     */
    protected IntVar newIntVarFromDomain(long[][] intervals, String name) {
        return this.model.newIntVarFromDomain(intervals, name);
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 完整域
     * 
     * @param name 变量名称
     * @return 创建的整数变量
     */
    protected IntVar newIntVarFromDomain(String name) {
        return this.model.newIntVarFromDomain(name);
    }

    /**
     * 封装CpModel的newBoolVar方法
     * 
     * @param name 变量名称
     * @return 创建的布尔变量
     */
    protected BoolVar newBoolVar(String name) {
        return this.model.newBoolVar(name);
    }

    /**
     * 获取CP约束求解模型实例
     * 
     * @return CP约束求解模型实例
     */
    public AlgCPModel getModel() {
        return model;
    }

    /**
     * 获取模块对象
     *
     * @return 模块对象
     */
    public IModule getModule() {
        return module;
    }

    public ParaVar getParaVar(String code) {
        return paraMap.get(code);
    }

    public ParaVar getSumSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumSumParaByAttr'");
    }

    public ParaVar getSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumParaByAttr'");
    }

    /**
     * 获取部件变量
     * 
     * @param code 变量代码
     * @return 部件变量实例
     * @throws AlgLoaderException 异常
     */
    public PartVar getPartVar(String code) {
        return partMap.get(code);
    }

    /**
     * Return list of atomic parts used by sum4Selected, optionally filtered by
     * condition.
     *
     * @param filtedConditionStr filter expression, may be null
     * @return filtered list of atomic parts
     */
    public List<PartVar> getInternalPartVars(String filtedConditionStr) {
        List<Part> atomicParts = currentModule.getAtomicParts();
        if (filtedConditionStr != null && !filtedConditionStr.trim().isEmpty()) {
            atomicParts = FilterExpressionExecutor.doSelect(atomicParts, filtedConditionStr);
            log.info("Priority-Filtered parts: {} in getPartVars", PartUtils.toShortString(atomicParts));
        }
        List<PartVar> partVars = new ArrayList<>();
        for (Part part : atomicParts) {
            partVars.add(currentModuleAlg.getPartVar(part.getCode()));
        }
        return partVars;
    }

    /**
     * 创建参数选项变量
     * 
     * @param paraCode   参数代码
     * @param optionCode 选项代码
     * @return 创建的参数选项变量
     */
    protected ParaOptionVar createParaOptionVar(String paraCode, String optionCode) {
        String ipf = toInstPrefix();
        Optional<Para> paraOpt = module != null ? module.getPara(paraCode) : Optional.empty();
        if (!paraOpt.isPresent()) {
            log.error("Para not found for code: {}", paraCode);
            throw new AlgLoaderException("Para not found for code: " + paraCode);
        }
        Para para = paraOpt.get();
        Optional<DynamicAttributerOption> optionOpt = para.getOption(optionCode);
        if (!optionOpt.isPresent()) {
            log.error("ParaOption not found for code: {}", optionCode);
            throw new AlgLoaderException("ParaOption not found for code: " + optionCode);
        }
        DynamicAttributerOption option = optionOpt.get();
        ParaOptionVar optionVar = new ParaOptionVar(option);
        optionVar.setIsSelectedVar(newBoolVar(f(ParaVar.OPTIONS_PATTERN, ipf, paraCode, option.getCode())));
        return optionVar;
    }

    /**
     * 兼容性规则：Requires关系约束
     * 规则内容：如果左侧参数选择指定选项，则右侧参数必须选择指定选项
     * 例如：(a1,a3) Requires (b1,b2,b3) 表示如果A选择a1或a3，则B必须选择b1、b2或b3
     * 
     * @param ruleCode                   规则代码
     * @param leftParaVar                左侧参数变量
     * @param leftParaFilterOptionCodes  左侧参数过滤选项代码列表
     * @param rightParaVar               右侧参数变量
     * @param rightParaFilterOptionCodes 右侧参数过滤选项代码列表
     */
    public void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:确保只有一个参数选项被选中
        addExactlyOneConstraint(leftParaVar);
        // right:确保只有一个参数选项被选中
        addExactlyOneConstraint(rightParaVar);

        // 定义左侧条件：左侧集合中至少一个被选中
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 定义右侧条件：右侧集合中至少一个被选中
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 实现Requires关系：如果左侧条件为true，则右侧条件必须为true
        model.addImplication(leftCond, rightCond);
    }

    /**
     * 添加部件数量相等约束
     * 根据partCode找到对应的partVar，并添加数量相等约束
     * 
     * @param partCode     部件代码
     * @param partQuantity 期望的部件数量
     * @throws AlgLoaderException 异常
     */
    public void addPartEquality(String partCode, int partQuantity) {
        // 设置当前松弛变量名称
        this.model.setRelax4SysRule("addPartEquality_" + partCode + "_" + partQuantity);
        // 1. 根据partCode找到对应的partVar
        PartVar partVar = partMap.get(partCode);
        if (partVar == null) {
            log.error("PartVar not found for code: {}", partCode);
            throw new AlgLoaderException("PartVar not found for code: " + partCode);
        }
        // 2. 使用model.addEquality添加数量约束
        model.addEquality(partVar.getQty(), partQuantity);
    }

    /**
     * 添加参数相等约束
     * 
     * @param paraCode  参数代码
     * @param paraValue 参数值
     * @throws AlgLoaderException 异常
     */
    public void addParaEquality(String paraCode, String paraValue) {
        // 设置当前松弛变量名称
        this.model.setRelax4SysRule("addParaEquality_" + paraCode + "_" + paraValue);
        ParaVar paraVar = paraMap.get(paraCode);
        if (paraVar == null) {
            log.error("ParaVar not found for code: {}", paraCode);
            throw new AlgLoaderException("ParaVar not found for code: " + paraCode);
        }
        model.addEquality(paraVar.getValue(), Integer.parseInt(paraValue));
    }

    /**
     * 兼容性规则：CoDependent关系约束
     * 规则内容：双向依赖关系，左侧参数和右侧参数必须在对应的组内或组外
     * 例如：(a1,a3) CoDependent (b1,b2,b3) 表示：
     * - 如果A选择a1或a3，则B必须选择b1、b2或b3
     * - 如果A选择a2、a4或a5，则B必须选择b4或b5
     * - 如果B选择b1、b2或b3，则A必须选择a1或a3
     * - 如果B选择b4或b5，则A必须选择a2、a4或a5
     * 
     * @param ruleCode                   规则代码
     * @param leftParaVar                左侧参数变量
     * @param leftParaFilterOptionCodes  左侧参数过滤选项代码列表
     * @param rightParaVar               右侧参数变量
     * @param rightParaFilterOptionCodes 右侧参数过滤选项代码列表
     */
    public void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:确保只有一个参数选项被选中
        addExactlyOneConstraint(leftParaVar);
        // right:确保只有一个参数选项被选中
        addExactlyOneConstraint(rightParaVar);

        // 定义左侧条件：左侧集合中至少一个被选中
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 定义右侧条件：右侧集合中至少一个被选中
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 定义左侧非条件：左侧集合外至少一个被选中
        BoolVar leftNotCond = createNotSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                "leftNotCond");

        // 定义右侧非条件：右侧集合外至少一个被选中
        BoolVar rightNotCond = createNotSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes,
                "rightNotCond");

        // 实现CoDependent双向关系：
        // 正向1.1：如果左侧条件为true，则右侧条件必须为true
        model.addImplication(leftCond, rightCond);

        // 正向1.2：如果左侧非条件为true，则右侧非条件必须为true
        model.addImplication(leftNotCond, rightNotCond);

        // 反向2.1：如果右侧条件为true，则左侧条件必须为true
        model.addImplication(rightCond, leftCond);

        // 反向2.2：如果右侧非条件为true，则左侧非条件必须为true
        model.addImplication(rightNotCond, leftNotCond);
    }

    /**
     * 兼容性规则：Incompatible关系约束
     * 规则内容：双向不兼容关系，左侧参数和右侧参数不能在对应的组内同时存在
     * 例如：(a1,a3) Incompatible (b1,b2,b3) 表示：
     * - 如果A选择a1或a3，则B不能选择b1、b2或b3（必须选择b4或b5）
     * - 如果A选择a2、a4或a5，则B可以选择任意值
     * - 如果B选择b1、b2或b3，则A不能选择a1或a3（必须选择a2、a4或a5）
     * - 如果B选择b4或b5，则A可以选择任意值
     * 
     * @param ruleCode                   规则代码
     * @param leftParaVar                左侧参数变量
     * @param leftParaFilterOptionCodes  左侧参数过滤选项代码列表
     * @param rightParaVar               右侧参数变量
     * @param rightParaFilterOptionCodes 右侧参数过滤选项代码列表
     */
    public void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:确保只有一个参数选项被选中
        addExactlyOneConstraint(leftParaVar);
        // right:确保只有一个参数选项被选中
        addExactlyOneConstraint(rightParaVar);

        // 定义左侧条件：左侧集合中至少一个被选中
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 定义右侧条件：右侧集合中至少一个被选中
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 定义左侧非条件：左侧集合外至少一个被选中
        BoolVar leftNotCond = createNotSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                "leftNotCond");

        // 定义右侧非条件：右侧集合外至少一个被选中
        BoolVar rightNotCond = createNotSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes,
                "rightNotCond");

        // 实现Incompatible双向关系：
        // 正向1.1：如果左侧条件为true，则右侧条件必须为false（右侧非条件必须为true）
        model.addImplication(leftCond, rightNotCond);

        // 正向1.2：如果左侧非条件为true，则右侧可以是任意值（无约束）
        // 这个约束不需要显式添加，因为默认允许

        // 反向2.1：如果右侧条件为true，则左侧条件必须为false（左侧非条件必须为true）
        model.addImplication(rightCond, leftNotCond);

        // 反向2.2：如果右侧非条件为true，则左侧可以是任意值（无约束）
        // 这个约束不需要显式添加，因为默认允许
    }

    /**
     * 字符串格式化工具方法
     * 
     * @param fstr   格式化字符串
     * @param values 参数值
     * @return 格式化后的字符串
     */
    private String f(String fstr, String... values) {
        return String.format(Locale.ROOT, fstr, (Object[]) values);
    }

    /**
     * 创建字符串列表的工具方法
     * 封装Arrays.asList，提供更简洁的API
     * 
     * @param codes 字符串数组
     * @return List<String>
     */
    protected List<String> listOf(String... codes) {
        return Arrays.asList(codes);
    }

    /**
     * 为参数变量添加"确保只有一个选项被选中"的约束
     * 
     * @param paraVar 参数变量
     */
    private void addExactlyOneConstraint(ParaVar paraVar) {
        model.addExactlyOne(paraVar.getOptionSelectVars().values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));
    }

    /**
     * 创建"集合中至少一个被选中"的条件变量和约束
     * 
     * @param ruleCode          规则代码
     * @param paraVar           参数变量
     * @param filterOptionCodes 过滤的选项代码列表
     * @param conditionSuffix   条件后缀（如"leftCond"、"rightCond"）
     * @return 条件变量
     */
    private BoolVar createSelectedCondition(String ruleCode, ParaVar paraVar,
            List<String> filterOptionCodes, String conditionSuffix) {
        // 定义条件变量
        BoolVar condition = newBoolVar(f("%s_%s", ruleCode, conditionSuffix));

        // 获取选中的选项
        Literal[] selected = paraVar.getOptionSelectVars().values().stream()
                .filter(option -> filterOptionCodes.contains(option.getCode()))
                .map(option -> option.getIsSelectedVar())
                .toArray(Literal[]::new);

        // 当条件为true时，集合中至少一个被选中；当为false时，集合全部不被选中
        model.addBoolOr(selected).onlyEnforceIf(condition);
        model.addBoolAnd(Arrays.stream(selected).map(Literal::not).toArray(Literal[]::new))
                .onlyEnforceIf(condition.not());

        return condition;
    }

    /**
     * 创建"集合外至少一个被选中"的条件变量和约束
     * 
     * @param ruleCode          规则代码
     * @param paraVar           参数变量
     * @param filterOptionCodes 过滤的选项代码列表（这些选项不在集合内）
     * @param conditionSuffix   条件后缀（如"leftNotCond"、"rightNotCond"）
     * @return 条件变量
     */
    private BoolVar createNotSelectedCondition(String ruleCode, ParaVar paraVar,
            List<String> filterOptionCodes, String conditionSuffix) {
        // 定义条件变量
        BoolVar condition = newBoolVar(f("%s_%s", ruleCode, conditionSuffix));

        // 获取集合外选中的选项
        Literal[] notSelected = paraVar.getOptionSelectVars().values().stream()
                .filter(option -> !filterOptionCodes.contains(option.getCode()))
                .map(option -> option.getIsSelectedVar())
                .toArray(Literal[]::new);

        // 当条件为true时，集合外至少一个被选中；当为false时，集合外全部不被选中
        model.addBoolOr(notSelected).onlyEnforceIf(condition);
        model.addBoolAnd(Arrays.stream(notSelected).map(Literal::not).toArray(Literal[]::new))
                .onlyEnforceIf(condition.not());

        return condition;
    }

    /**
     * 设置部件为未选中状态（批量）
     * 遍历调用 setPartUnSelected(Part part)
     *
     * @param parts 部件列表
     */
    public void setPartUnSelected(List<Part> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        for (Part part : parts) {
            setPartUnSelected(part);
        }
    }

    /**
     * 设置部件为未选中状态
     * 根据 part.code 找到对应 partVar，使用 ortools 设置：
     * - partVar.isSelected == 0
     * - partVar.qty == 0
     *
     * @param part 部件
     */
    public void setPartUnSelected(Part part) {
        if (part == null) {
            log.warn("Part is null, cannot set unselected");
            return;
        }

        // 根据 part.code 找到对应 partVar
        PartVar partVar = getPartVar(part.getCode());

        // 使用 ortools 设置约束
        // partVar.isSelected == 0
        model.addEquality(partVar.getIsSelected(), 0);

        // partVar.qty == 0
        model.addEquality(partVar.getQty(), 0);

        log.info("Set part unselected: code={}, isSelected=0, qty=0", part.getCode());
    }

    public List<PartVar> getAllPartVars(String filterConditionStr) {
        if (filterConditionStr == null || filterConditionStr.isEmpty()) {
            return new ArrayList<>(partMap.values());
        } else {
            return FilterExpressionExecutor.doSelect(new ArrayList<>(partMap.values()), filterConditionStr);
        }
    }

    /**
     * 过滤所有部件变量
     * 
     * @param filterConditionStr
     * @return 第一个是过滤后，第一个没有过滤的
     */
    public Pair<List<PartVar>, List<PartVar>> filterAllPartVars(String filterConditionStr) {
        List<PartVar> filterPartVars = getAllPartVars(filterConditionStr);
        Set<String> filterPartVarCodes = filterPartVars.stream().map(PartVar::getCode).collect(Collectors.toSet());
        List<PartVar> noFilterPartVars = new ArrayList<>();
        for (PartVar partVar : partMap.values()) {
            if (!filterPartVarCodes.contains(partVar.getCode())) {
                noFilterPartVars.add(partVar);
            }
        }
        return Pair.of(filterPartVars, noFilterPartVars);
    }

    public String toString() {
        return this.module.getCode();
    }

    // ==================== ModuleInstAccessor binding ====================

    /**
     * 绑定当前ModuleInst访问器，用于POST阶段读写实例数据
     */
    public void bindModuleInstAccessor(ModuleInstAccessor accessor) {
        this.currentModuleInstAccessor = accessor;
    }

    /**
     * 清理当前ModuleInst访问器
     */
    public void clearModuleInstAccessor() {
        this.currentModuleInstAccessor = null;
    }

    /**
     * 获取当前ModuleInst访问器，仅在POST上下文可用
     */
    protected ModuleInstAccessor currentModuleInstAccessor() {
        if (currentModuleInstAccessor == null) {
            throw new AlgLoaderException(
                    "ModuleInstAccessor is not bound. This method is only available in POST context.");
        }
        return currentModuleInstAccessor;
    }

    // ==================== forwarding methods to ModuleInstAccessor ====================

    public String getDynAttr(String partCategoryCode, String attrCode) {
        return currentModuleInstAccessor().getDynAttr(partCategoryCode, attrCode);
    }

    public String getDynAttr(String partCategoryCode, int instId, String attrCode) {
        return currentModuleInstAccessor().getDynAttr(partCategoryCode, instId, attrCode);
    }

    public List<String> getDynAttrValues(String partCategoryCode, String attrCode) {
        return currentModuleInstAccessor().getDynAttrValues(partCategoryCode, attrCode);
    }

    public List<String> getDynAttrValues(String partCategoryCode, int instId, String attrCode) {
        return currentModuleInstAccessor().getDynAttrValues(partCategoryCode, instId, attrCode);
    }

    public String getSumDynAttr(String partCategoryCode, String attrCode) {
        return currentModuleInstAccessor().getSumDynAttr(partCategoryCode, attrCode);
    }

    public String getSumDynAttr(String partCategoryCode, int instId, String attrCode) {
        return currentModuleInstAccessor().getSumDynAttr(partCategoryCode, instId, attrCode);
    }

    public int getQuantity(String partCategoryCode) {
        return currentModuleInstAccessor().getQuantity(partCategoryCode);
    }

    public int getQuantity(String partCategoryCode, int instId) {
        return currentModuleInstAccessor().getQuantity(partCategoryCode, instId);
    }

    public List<Integer> getInstanceIds(String partCategoryCode) {
        return currentModuleInstAccessor().getInstanceIds(partCategoryCode);
    }

    public void setParaValue(String paraCode, String value) {
        currentModuleInstAccessor().setParaValue(paraCode, value);
    }

    public void setParaValue(String partCategoryCode, String paraCode, String value) {
        currentModuleInstAccessor().setParaValue(partCategoryCode, paraCode, value);
    }

    public void setParaValue(String partCategoryCode, int instId, String paraCode, String value) {
        currentModuleInstAccessor().setParaValue(partCategoryCode, instId, paraCode, value);
    }

    public String getParaValue(String paraCode) {
        return currentModuleInstAccessor().getParaValue(paraCode);
    }

    public String getParaValue(String partCategoryCode, String paraCode) {
        return currentModuleInstAccessor().getParaValue(partCategoryCode, paraCode);
    }

    public String getParaValue(String partCategoryCode, int instId, String paraCode) {
        return currentModuleInstAccessor().getParaValue(partCategoryCode, instId, paraCode);
    }

    // ==================== type conversion helpers ====================

    protected int toInt(String value) {
        if (value == null || value.isEmpty()) {
            throw new AlgLoaderException("Cannot convert null or empty string to int");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AlgLoaderException("Cannot convert '" + value + "' to int", e);
        }
    }

    protected long toLong(String value) {
        if (value == null || value.isEmpty()) {
            throw new AlgLoaderException("Cannot convert null or empty string to long");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new AlgLoaderException("Cannot convert '" + value + "' to long", e);
        }
    }

    protected double toDouble(String value) {
        if (value == null || value.isEmpty()) {
            throw new AlgLoaderException("Cannot convert null or empty string to double");
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new AlgLoaderException("Cannot convert '" + value + "' to double", e);
        }
    }

    protected String toString(Object value) {
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> T toValue(String value, Class<T> targetType) {
        if (value == null || value.isEmpty()) {
            throw new AlgLoaderException("Cannot convert null or empty string to " + targetType.getSimpleName());
        }
        if (targetType == String.class) {
            return (T) value;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(value);
        }
        throw new AlgLoaderException("Unsupported target type: " + targetType.getSimpleName());
    }
}
