package com.jmix.executor.impl.algmodel;

import com.jmix.executor.imodel.DynamicAttributerOption;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.imodel.PartCategory;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.executor.omodel.PartConstantAttr;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;

import lombok.extern.slf4j.Slf4j;

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

/**
 * 约束算法实现基类
 * 实现ConstraintAlg接口，提供约束求解的具体实现
 * 
 * @since 2025-09-22
 */
@Slf4j
public abstract class ConstraintAlgImpl implements ConstraintAlg {
    static {
        Loader.loadNativeLibraries();
    }

    /**
     * CP约束求解模型实例
     */
    protected AlgCPModel model;

    /**
     * module的基础信息
     */
    protected Module module;

    /**
     * 变量映射表，存储所有创建的变量实例
     */
    protected Map<String, Var<?>> varMap = new LinkedHashMap<>();

    /**
     * 受显式约束控制可见性的编码集合，跳过默认绑定
     */
    protected Set<String> codesOfHiddenConstraint = new HashSet<>();

    /**
     * 所有规则方法的映射表，存储规则代码到对应方法的映射
     */
    protected Map<String, Method> ruleMethods = new HashMap<>();

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
     * 初始化约束模型（带松弛变量支持）
     * 
     * @param model           CP模型实例
     * @param module          模块对象
     * @param isAttachRelax   是否附加松弛变量
     * @param confictedRelaxs 冲突松弛变量列表
     */
    public void initModel(CpModel model, Module module, boolean isAttachRelax, List<RelaxVar> confictedRelaxs) {
        List<String> fullRules = toFullRules(module);
        List<RefProgObjSchema> fullProgObjs = toFullProgObjs(module);
        initModel(model, module, fullRules, fullProgObjs, isAttachRelax, confictedRelaxs);
    }

    private List<String> toFullRules(Module tempModule) {
        List<String> fullRules = new ArrayList<>();
        for (Rule rule : tempModule.getRules()) {
            fullRules.add(rule.getCode());
        }
        return fullRules;
    }

    private List<RefProgObjSchema> toFullProgObjs(Module tempModule) {
        List<RefProgObjSchema> fullProgObjs = new ArrayList<>();
        RefProgObjSchema refProgObjSchema = null;
        // 根据module.parts,module.paras,创建RefProgObjSchema列表
        for (Part part : tempModule.getParts()) {
            refProgObjSchema = new RefProgObjSchema(RefProgObjSchema.PROG_OBJ_TYPE_PART, part.getCode(), "");
            fullProgObjs.add(refProgObjSchema);
        }
        for (Para para : tempModule.getParas()) {
            refProgObjSchema = new RefProgObjSchema(RefProgObjSchema.PROG_OBJ_TYPE_PARA, para.getCode(), "");
            fullProgObjs.add(refProgObjSchema);
        }
        return fullProgObjs;
    }

    /**
     * 初始化模型（差量加载版本）
     * 
     * @param model           CP模型
     * @param module          模块
     * @param exeRules        本次要加载的rules
     * @param exeProgObjs     本次要初始化的变量
     * @param confictedRelaxs 冲突松弛变量列表
     */
    public void initModel(CpModel model,
            Module module,
            List<String> exeRules,
            List<RefProgObjSchema> exeProgObjs,
            List<RelaxVar> confictedRelaxs) {
        initModel(model, module, exeRules, exeProgObjs, false, confictedRelaxs);
    }

    /**
     * 初始化模型（差量加载版本，带松弛变量支持）
     * 
     * @param model           CP模型
     * @param module          模块
     * @param exeRules        本次要加载的rules
     * @param exeProgObjs     本次要初始化的变量
     * @param isAttachRelax   是否附加松弛变量
     * @param confictedRelaxs 冲突松弛变量列表
     */
    public void initModel(CpModel model,
            Module module,
            List<String> exeRules,
            List<RefProgObjSchema> exeProgObjs,
            boolean isAttachRelax,
            List<RelaxVar> confictedRelaxs) {
        this.model = new AlgCPModel(model);
        this.model.setIsAttachRelax(isAttachRelax);
        this.model.setConfictedRelaxVars(confictedRelaxs);
        this.module = module;
        initModelAfter(model);

        // 初始化AlgCPModel
        initAlgCPModel();

        // 根据exeProgObjs初始化Variables
        initVariables(exeProgObjs);

        // 根据exeRules初始化rule
        initRules(exeRules);

        // 设置默认可见性约束
        setDefaultVisibilityConstraints();
    }

    /**
     * 初始化AlgCPModel
     * 用于增量加载的AlgCPModel初始化逻辑
     */
    private void initAlgCPModel() {
        // 初始化AlgCPModel的相关逻辑
        log.info("Initializing AlgCPModel for incremental loading");
    }

    /**
     * 设置默认可见性约束
     * 对于没有显式控制可见性的变量，设置 isHiddenVar == 0（即默认可见）
     * 遍历所有变量，为没有显式可见性控制的变量设置默认可见状态
     * 
     * @throws AlgLoaderException 异常
     */
    private void setDefaultVisibilityConstraints() {
        boolean isFirst = true;
        for (Map.Entry<String, Var<?>> entry : varMap.entrySet()) {
            String code = entry.getKey();
            if (codesOfHiddenConstraint.contains(code)) {
                continue;
            }
            Var<?> v = entry.getValue();
            if (v instanceof ParaVar) {
                if (isFirst) {
                    this.model.setRelax4SysRule("hiddensrule");
                    isFirst = false;
                }
                ParaVar pv = (ParaVar) v;
                // 暂时没有添加到松弛变量里
                model.addEquality(pv.getIsHidden(), 0);

            } else if (v instanceof PartVar) {
                if (isFirst) {
                    this.model.setRelax4SysRule("hiddensrule");
                    isFirst = false;
                }
                PartVar pt = (PartVar) v;
                model.addEquality(pt.getIsHidden(), 0);
            } else {
                log.error("Unsupported variable type: {}", v.getClass().getSimpleName());
                throw new AlgLoaderException("Unsupported variable type: " + v.getClass().getSimpleName());
            }
        }
    }

    /**
     * 根据module.getRules构建ruleMethods映射
     * 通过反射获取所有带有@CodeRuleAnno注解的方法，并建立规则代码到方法的映射关系
     * 
     * @throws AlgLoaderException 异常
     */
    private void buildRuleMethods() {
        if (module == null || module.getRules() == null) {
            return;
        }

        // 获取当前类的所有方法
        Method[] methods = this.getClass().getDeclaredMethods();
        Map<String, Method> allMethods = new HashMap<>();

        // 构建方法名到Method对象的映射
        for (Method method : methods) {
            allMethods.put(method.getName(), method);
        }

        // 根据module.getRules来构建ruleMethods
        for (Rule rule : module.getRules()) {
            String ruleCode = rule.getCode();
            Method method = allMethods.get(ruleCode);
            if (method != null) {
                ruleMethods.put(ruleCode, method);
                log.info("Built rule method mapping: {} -> {}", ruleCode, method.getName());
            } else {
                log.error("Rule method not found for rule code: {}", ruleCode);
                throw new AlgLoaderException("Rule method not found for rule code: " + ruleCode);
            }
        }

        log.info("Built {} rule methods from module rules", ruleMethods.size());
    }

    /**
     * 根据exeRules执行规则
     * 
     * @param exeRules 要执行的规则列表
     */
    private void initRules(List<String> exeRules) {
        // 判断ruleMethods是否已经构建，如果没有构建则先构建
        if (ruleMethods.isEmpty()) {
            buildRuleMethods();
        }

        if (exeRules == null || exeRules.isEmpty()) {
            log.warn("No specific rules to execute, all {} rule methods are available", ruleMethods.size());
            return;
        }

        // 按exeRules列表来执行规则
        for (String ruleCode : exeRules) {
            Method method = ruleMethods.get(ruleCode);
            executeRuleMethod(ruleCode, method);
        }
        log.info("Executed  {} requested rules", exeRules.size());
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
            LinearArgument[] weightedTerms = new LinearArgument[relaxVars.size()];
            for (int i = 0; i < relaxVars.size(); i++) {
                RelaxVar relaxVar = relaxVars.get(i);
                weightedTerms[i] = LinearExpr.term(relaxVar.getValue(), relaxVar.getWeight());
            }
            model.minimize(LinearExpr.sum(weightedTerms));
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
    private void executeRuleMethod(String ruleCode, Method method) {
        if (method == null) {
            log.error("Rule method not found for execution: " + ruleCode);
            throw new AlgLoaderException("Rule method not found for execution: " + ruleCode);
        }
        try {
            // 设置当前松弛变量名称
            this.model.setRelax4CustomRule(ruleCode);

            // 执行规则方法
            method.setAccessible(true);
            method.invoke(this);
            log.info("Executed rule method: {}", method.getName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to execute rule method: " + method.getName(), e);
            throw new AlgLoaderException("Failed to execute rule method: " + method.getName(), e);
        }
    }

    /**
     * 根据exeProgObjs初始化Variables
     * 
     * @param exeProgObjs 要初始化的编程对象列表
     */
    private void initVariables(List<RefProgObjSchema> exeProgObjs) {
        // 创建变量
        createVariables(exeProgObjs);

        // 将变量写回字段, 保证规则使用变量是同一个
        writeBackVariablesToFields(varMap);
    }

    private void createVariables(List<RefProgObjSchema> exeProgObjs) {
        if (exeProgObjs == null || exeProgObjs.isEmpty()) {
            log.error("exeProgObjs is null or empty");
            throw new AlgLoaderException("exeProgObjs is null or empty");
        }
        for (RefProgObjSchema exeProgObj : exeProgObjs) {
            String field = exeProgObj.getProgObjType();
            if (field.equals(RefProgObjSchema.PROG_OBJ_TYPE_PARA)) {
                createParaVar(exeProgObj.getProgObjCode());
            } else if (field.equals(RefProgObjSchema.PROG_OBJ_TYPE_PART)) {
                createPartVar(exeProgObj.getProgObjCode());
            } else {
                log.error("Unsupported progObjType: {}", field);
                throw new AlgLoaderException("Unsupported progObjType: " + field);
            }
        }
        log.info("create {} variables for incremental loading", varMap.size());
    }

    /**
     * 模型初始化后的回调方法
     * 子类可以重写此方法来实现自定义的初始化逻辑
     * 
     * @param model CP模型实例
     */
    protected void initModelAfter(CpModel model) {

    }

    /**
     * 子类重写此方法来实现自定义变量初始化逻辑
     */
    protected void onInitCustomVariables() {
        // 默认空实现，子类可以重写
    }

    /**
     * 将变量写回字段, 保证在初始化rule时生效
     * 
     * @param varMap 变量映射
     * @throws AlgLoaderException 异常
     */
    private void writeBackVariablesToFields(Map<String, Var<?>> varMap) throws AlgLoaderException {
        Map<String, Field> fieldMap = getAllFieldVariables();
        Var<?> tVar = null;
        for (Map.Entry<String, Var<?>> entry : varMap.entrySet()) {
            String code = entry.getKey();
            Var<?> v = entry.getValue();
            if (v instanceof ParaVar) {
                tVar = newParaVar((ParaVar) v);
                setVariableField(tVar, fieldMap.get(code));
            } else if (v instanceof PartVar) {
                tVar = newPartVar((PartVar) v);
                setVariableField(tVar, fieldMap.get(code));
            } else {
                log.error("Unsupported variable to field: please check in constrain file! {}", v.getCode());
                throw new AlgLoaderException(
                        "Unsupported variable to field: please check in constrain file!" + v.getCode());
            }
        }
    }

    /**
     * 创建部件变量, 继承类可以重载
     * 
     * @param internalPartVar 内部部件变量
     * @return 创建的部件变量
     */
    protected Var<?> newPartVar(PartVar internalPartVar) {
        return internalPartVar;
    }

    /**
     * 创建参数变量, 继承类可以重载
     * 
     * @param internalParaVar 内部参数变量
     * @return 创建的参数变量
     */
    protected Var<?> newParaVar(ParaVar internalParaVar) {
        return internalParaVar;
    }

    /**
     * 设置单个变量字段
     * 
     * @param v     变量值
     * @param field 字段对象
     * @throws AlgLoaderException 异常
     */
    private void setVariableField(Var<?> v, Field field) throws AlgLoaderException {
        if (field == null) {
            log.error("Field not found for code: null {}", v.getCode());
            throw new AlgLoaderException("Field not found for code: null " + v.getCode());
        }
        try {
            field.set(this, v);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("Failed to write back variable to field: " + v.getCode(), e);
            throw new AlgLoaderException("Failed to write back variable to field: " + v.getCode(), e);
        }
    }

    private Map<String, Field> getAllFieldVariables() {
        Field[] fields = this.getClass().getDeclaredFields();
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
    public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
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
     * 获取变量映射
     * 
     * @return 变量映射Map
     */
    public Map<String, Var<?>> getVarMap() {
        return varMap;
    }

    /**
     * 获取变量
     *
     * @param code 变量代码
     * @return 变量实例
     */
    public Var<?> getVar(String code) {
        return varMap.get(code);
    }

    /**
     * 获取模块对象
     *
     * @return 模块对象
     */
    public Module getModule() {
        return module;
    }

    /**
     * 获取参数变量
     * 
     * @param code 变量代码
     * @return 参数变量实例
     * @throws AlgLoaderException 异常
     */
    public ParaVar getParaVar(String code) {
        Var<?> var = getVar(code);
        if (var == null || !(var instanceof ParaVar)) {
            log.error("ParaVar not found for code: {}", code);
            throw new AlgLoaderException("ParaVar not found for code: " + code);
        }
        return (ParaVar) var;

    }

    /**
     * 获取部件变量
     * 
     * @param code 变量代码
     * @return 部件变量实例
     * @throws AlgLoaderException 异常
     */
    public PartVar getPartVar(String code) {
        Var<?> var = getVar(code);
        if (var == null || !(var instanceof PartVar)) {
            log.error("PartVar not found for code: {}", code);
            throw new AlgLoaderException("PartVar not found for code: " + code);
        }
        return (PartVar) var;
    }

    /**
     * 获取所有变量列表
     * 
     * @return 变量列表
     */
    public List<Var<?>> getVars() {
        return new ArrayList<>(varMap.values());
    }

    /**
     * 注册变量
     * 
     * @param code 变量代码
     * @param var  变量实例
     */
    public void registerVar(String code, Var<?> var) {
        if (code != null && var != null) {
            varMap.put(code, var);
        }
    }

    private Var<?> getRegisterVar(String code) {
        return varMap.get(code);
    }

    /**
     * 获取其他变量映射
     * 
     * @return 其他变量映射Map
     */
    public Map<String, OtherVar> getOtherVarMap() {
        return model.getOtherVarMap();
    }

    /**
     * 创建参数变量
     * 
     * @param code 参数代码
     * @return 创建的参数变量
     * @throws AlgLoaderException 异常
     */
    protected ParaVar createParaVar(String code) {
        Optional<Para> paraOpt = module != null ? module.getPara(code) : Optional.empty();
        if (!paraOpt.isPresent()) {
            log.error("Para not found for code: {}", code);
            throw new AlgLoaderException("Para not found for code: " + code);
        }
        Para para = paraOpt.get();
        ParaVar paraVar = new ParaVar();
        paraVar.setBase(para);
        switch (para.getParaType()) {
            case INTEGER:
                paraVar.setValue(newIntVar(Integer.parseInt(para.getMinValue()), Integer.parseInt(para.getMaxValue()),
                        f(ParaVar.VALUE_PATTERN, code)));
                break;
            case ENUM:
                List<DynamicAttributerOption> options = para.getOptions();
                if (options == null || options.isEmpty()) {
                    log.error("Para options not found for code: {}", code);
                    throw new AlgLoaderException("Para options not found for code: " + code);
                }
                paraVar.setValue(newIntVarFromDomain(para.getOptionIds(), f(ParaVar.VALUE_PATTERN, code)));

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
        paraVar.setIsHidden(newBoolVar(f(ParaVar.HIDDEN_PATTERN, code)));
        registerVar(code, paraVar);
        return paraVar;
    }

    /**
     * 创建部件变量
     * 
     * @param code 部件代码
     * @return 创建的部件变量
     * @throws AlgLoaderException 异常
     */
    protected PartVar createPartVar(String code) {
        Optional<Part> partOpt = module != null ? module.getPart(code) : Optional.empty();
        if (!partOpt.isPresent()) {
            log.error("Part not found for code: {}", code);
            throw new AlgLoaderException("Part not found for code: " + code);
        }
        Part part = partOpt.get();
        if (part instanceof PartCategory) {
            return createPartCategoryVar((PartCategory) part);
        }
        return createPartVar(part);
    }

    protected PartVar createPartVar(Part part) {
        Var<?> tempVar = getRegisterVar(part.getCode());
        if (null != tempVar) {
            return (PartVar) tempVar;
        }
        // 现有代码
        PartVar partVar = new PartVar();
        partVar.setBase(part);
        partVar.setQty(newIntVar(0, part.getMaxQuantity(), f(PartVar.QTY_PATTERN, part.getCode())));
        partVar.setIsHidden(newBoolVar(f(PartVar.HIDDEN_PATTERN, part.getCode())));
        partVar.setIsSelected(newBoolVar(f(PartVar.ISSELECTED_PATTERN, part.getCode())));
        registerVar(part.getCode(), partVar);
        return partVar;
    }

    protected PartVar createPartCategoryVar(PartCategory categoryPart) {
        PartCategoryVar partCategoryVar = new PartCategoryVar();
        partCategoryVar.setBase(categoryPart);
        for (Part part : categoryPart.getPartMap().values()) {
            createPartVar(part);
        }
        for (PartCategory subCategory : categoryPart.getPartCategoryMap().values()) {
            createPartCategoryVar(subCategory);
        }
        return partCategoryVar;
    }

    /**
     * 添加求和函数约束
     *
     * @param sumParts    求和的部件列表
     * @param sumAttrCode 求和属性代码
     * @param comparator  比较符
     * @param leftValue   左值
     */
    public void sumFunConstraint(List<Part> sumParts, String sumAttrCode, String comparator, int leftValue) {
        List<LinearExpr> sumTerms = new ArrayList<>();
        List<String> sumTermStrings = new ArrayList<>();

        for (Part part : sumParts) {
            PartVar partVar = getPartVar(part.getCode());
            if (partVar.getQty() != null) {
                int attrValue = 0;
                if (PartConstantAttr.Quantity.getCode().equals(sumAttrCode)) {
                    attrValue = 1;
                } else {
                    attrValue = Integer.parseInt(part.getAttr(sumAttrCode));
                }
                sumTerms.add(LinearExpr.term(partVar.getQty(), attrValue));
                sumTermStrings.add(partVar.getBase().getShortCode() + "*" + attrValue);
            } else {
                log.error("PartVar quantity is null for part code: {}, cannot create sum constraint", part.getCode());
                throw new AlgLoaderException("PartVar quantity is null for part code: " + part.getCode()
                        + ", cannot create sum constraint");
            }
        }

        if (sumTerms.isEmpty()) {
            log.warn("No valid parts found for sum constraint with attrCode: {}, comparator: {}, leftValue: {}",
                    sumAttrCode, comparator, leftValue);
            return;
        }

        LinearExpr sumFunExpr = LinearExpr.sum(sumTerms.toArray(new LinearExpr[0]));
        String sumFormulaBase = String.join(" + ", sumTermStrings);

        ComparisonOperator operator = ComparisonOperator.fromSymbol(comparator);
        operator.applyConstraint(model.getCpModel(), sumFunExpr, leftValue);
        String sumFormula = operator.getFormulaString(sumFormulaBase, leftValue);

        log.info("Added sum constraint: {}", sumFormula);

    }

    /**
     * 创建参数选项变量
     * 
     * @param paraCode   参数代码
     * @param optionCode 选项代码
     * @return 创建的参数选项变量
     */
    protected ParaOptionVar createParaOptionVar(String paraCode, String optionCode) {
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
        optionVar.setIsSelectedVar(newBoolVar(f(ParaVar.OPTIONS_PATTERN, paraCode, option.getCode())));
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
        Var<?> var = varMap.get(partCode);
        if (!(var instanceof PartVar)) {
            log.error("PartVar not found for code: {}", partCode);
            throw new AlgLoaderException("PartVar not found for code: " + partCode);
        }
        PartVar partVar = (PartVar) var;

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
        Var<?> var = varMap.get(paraCode);
        if (!(var instanceof ParaVar)) {
            log.error("ParaVar not found for code: {}", paraCode);
            throw new AlgLoaderException("ParaVar not found for code: " + paraCode);
        }
        ParaVar paraVar = (ParaVar) var;
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
}