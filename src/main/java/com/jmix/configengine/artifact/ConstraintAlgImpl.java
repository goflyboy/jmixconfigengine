package com.jmix.configengine.artifact;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.google.ortools.Loader;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption; 
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.Rule;
import com.jmix.configengine.model.schema.RefProgObjSchema;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 约束算法实现基类
 */
@Slf4j
public abstract class ConstraintAlgImpl implements ConstraintAlg{
	static {
		Loader.loadNativeLibraries();
	}
	// CP模型
	protected AlgCPModel model; 
	// module context
	protected Module module;
	// var map
	protected Map<String, Var<?>> varMap = new LinkedHashMap<>();
	// codes whose visibility are controlled by explicit constraints; skip default binding
	protected Set<String> codesOfHiddenConstraint = new HashSet<>();
	// 所有rule的方法
	protected Map<String, Method> ruleMethods = new HashMap<>();

	/**
	 * 添加和隐藏相关的约束的Var
	 * @param hiddenVars
	 */
	protected void addVarAboutHiddenConstraints(Var<?>... hiddenVars){
		for (Var<?> v : hiddenVars) {
			codesOfHiddenConstraint.add(v.getCode());
		}
	}

	public void initModel(CpModel model, Module module){
		this.model = new AlgCPModel(model);
		this.module = module;
		initModelAfter(model);

		
		initVariables();
				
		// 构建ruleMethods映射
		initRules();
		
		initConstraint();
		// 设置默认可见性约束
		setDefaultVisibilityConstraints();
	}
	
	/**
	 * 初始化模型（差量加载版本）
	 * @param model CP模型
	 * @param module 模块
	 * @param exeRules 本次要加载的rules
	 * @param exeProgObjs 本次要初始化的变量
	 */
	public void initModel(CpModel model, Module module, List<String> exeRules, List<RefProgObjSchema> exeProgObjs) {
		this.model = new AlgCPModel(model);
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
	 */
	private void initAlgCPModel() {
		// 初始化AlgCPModel的相关逻辑
		log.debug("Initializing AlgCPModel for incremental loading");
	}
	
	/**
	 * 设置默认可见性约束
	 * 对于没有显式控制可见性的变量，设置 isHiddenVar == 0（即默认可见）
	 */
	private void setDefaultVisibilityConstraints() {
		for (Map.Entry<String, Var<?>> entry : varMap.entrySet()) {
			String code = entry.getKey();
			if (codesOfHiddenConstraint.contains(code)) {
				continue;
			}
			Var<?> v = entry.getValue();
			if (v instanceof ParaVar) {
				ParaVar pv = (ParaVar) v;
				if (pv.isHidden != null) {
					model.addEquality(pv.isHidden, 0);
				}
			} else if (v instanceof PartVar) {
				PartVar pt = (PartVar) v;
				if (pt.isHidden != null) {
					model.addEquality(pt.isHidden, 0);
				}
			}
		}
	}
	
	/**
	 * 根据module.getRules构建ruleMethods映射
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
				log.debug("Built rule method mapping: {} -> {}", ruleCode, method.getName());
			} else {
				throw new RuntimeException("Rule method not found for rule code: " + ruleCode);
			}
		}
		
		log.info("Built {} rule methods from module rules", ruleMethods.size());
	}
		
	/**
	 * 根据exeRules执行规则
	 * @param exeRules 要执行的规则列表
	 */
	private void initRules() {
		if (ruleMethods.isEmpty()) {
			buildRuleMethods();
		}
		for (Method method : ruleMethods.values()) {
			executeRuleMethod(method.getName(), method);
		}
		log.info("Executed all rule methods");
	}
	/**
	 * 根据exeRules执行规则
	 * @param exeRules 要执行的规则列表
	 */
	private void initRules(List<String> exeRules) {
		// 判断ruleMethods是否已经构建，如果没有构建则先构建
		if (ruleMethods.isEmpty()) {
			buildRuleMethods();
		}
		
		if (exeRules == null || exeRules.isEmpty()) {
			log.info("No specific rules to execute, all {} rule methods are available", ruleMethods.size());
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
	 * 执行单个规则方法
	 * @param ruleCode 规则代码
	 * @param method 规则方法
	 */
	private void executeRuleMethod(String ruleCode, Method method) {
		if (method != null) {
			try {
				// 执行规则方法
				method.setAccessible(true);
				method.invoke(this); 
				log.debug("Executed rule method: {}", method.getName());
			} catch (Exception e) {
				log.info(ruleCode, e);
				throw new RuntimeException("Failed to execute rule method: " + method.getName(), e);
			}
		} else {
			throw new RuntimeException("Rule method not found for execution: " + ruleCode);
		}
	}
	
	/**
	 * 根据exeProgObjs初始化Variables
	 * @param exeProgObjs 要初始化的编程对象列表
	 */
	private void initVariables(List<RefProgObjSchema> exeProgObjs) {
		if (exeProgObjs == null || exeProgObjs.isEmpty()) {
			throw new RuntimeException("exeProgObjs is null or empty");
		}
		for (RefProgObjSchema exeProgObj : exeProgObjs) { 
			String field = exeProgObj.getProgObjType();
			if (field.equals("Para")) {
				createParaVar(exeProgObj.getProgObjCode());
			} else if (field.equals("Part")) {
				createPartVar(exeProgObj.getProgObjCode());
			}
			else{
				throw new RuntimeException("Unsupported progObjType: " + field);
			}
		}
		log.info("Initialized {} variables for incremental loading", varMap.size());
	}
	
	/**
	 * 检查字段是否在目标列表中
	 * @param field 字段
	 * @param targetCodes 目标编码集合
	 * @return 是否在目标列表中
	 */
	private boolean isFieldInTargetList(Field field, Set<String> targetCodes) {
		// 检查字段名是否在目标编码中
		String fieldName = field.getName();
		return targetCodes.contains(fieldName);
	}
	
	/**
	 * 创建PartVar变量
	 * @param field 字段
	 * @return PartVar实例
	 */
	private PartVar createPartVar(Field field) {
		try {
			// 从模块中获取对应的Part模型
			com.jmix.configengine.model.Part part = module.getPart(field.getName());
			if (part == null) {
				log.warn("Part not found in module: {}", field.getName());
				return null;
			}
			
			PartVar partVar = new PartVar();
			partVar.setBase(part);
			
			// 设置数量变量
			partVar.qty = model.newIntVar(0, part.getMaxQuantity(), field.getName() + "_qty");
			
			// 设置隐藏属性
			partVar.isHidden = model.newBoolVar(field.getName() + "_isHidden");
			
			return partVar;
		} catch (Exception e) {
			log.error("Failed to create PartVar for field: {}", field.getName(), e);
			return null;
		}
	}
	
	protected void initModelAfter(CpModel model){
		
	}
	protected void initVariables() {
		// 默认实现：通过反射自动创建和赋值变量
		autoInitVariables();
		
		// 调用子类的自定义变量初始化逻辑
		onInitCustomVariables();
	}
	
	/**
	 * 子类重写此方法来实现自定义变量初始化逻辑
	 */
	protected void onInitCustomVariables() {
		// 默认空实现，子类可以重写
	}
	
	/**
	 * 通过反射自动创建和赋值变量
	 */
	private void autoInitVariables() {
		try {
			// 获取当前类的所有字段
			Field[] fields = this.getClass().getDeclaredFields();
			
			for (Field field : fields) {
				field.setAccessible(true);
				
				// 检查字段类型并创建对应的变量
				if (ParaVar.class.isAssignableFrom(field.getType())) {
					autoCreateAndAssignParaVar(field);
				} else if (PartVar.class.isAssignableFrom(field.getType())) {
					autoCreateAndAssignPartVar(field);
				}
			}
			
		} catch (Exception e) {
			throw new RuntimeException("自动初始化变量失败", e);
		}
	}
	
	/**
	 * 自动创建并赋值参数变量
	 */
	private void autoCreateAndAssignParaVar(Field field) throws Exception {
		String fieldName = field.getName();
		String paraCode = extractCodeFromFieldName(fieldName);
		
		// 检查Module中是否存在对应的Para
		Para para = module.getPara(paraCode);
		if (para == null) {
			return; // 跳过不存在的参数
		}
		
		// 创建ParaVar
		ParaVar paraVar = createParaVar(paraCode);
		
		// 通过反射赋值给字段
		field.set(this, paraVar);
	}
	
	/**
	 * 自动创建并赋值部件变量
	 */
	private void autoCreateAndAssignPartVar(Field field) throws Exception {
		String fieldName = field.getName();
		String partCode = extractCodeFromFieldName(fieldName);
		
		// 检查Module中是否存在对应的Part
		Part part = module.getPart(partCode);
		if (part == null) {
			return; // 跳过不存在的部件
		}
		
		// 创建PartVar
		PartVar partVar = createPartVar(partCode);
		
		// 通过反射赋值给字段
		field.set(this, partVar);
	}
	
	/**
	 * 从字段名提取代码
	 * 例如: ColorVar -> Color, TShirt11Var -> TShirt11
	 */
	private String extractCodeFromFieldName(String fieldName) {
		// 移除末尾的"Var"
		if (fieldName.endsWith("Var")) {
			return fieldName.substring(0, fieldName.length() - 3);
		}
		return fieldName;
	}

	/**
	 * 初始约束
	 */
	protected void initConstraint(){
		// 自动添加约束
	}

	public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
		return model.newIntVarFromDomain(Domain.fromValues(values), name);
	}

	/**
	 * 封装CpModel的newIntVar方法
	 */
	protected IntVar newIntVar(long left, long right, String name) {
		return this.model.newIntVar(left, right, name);
	}

	/**
	 * 封装CpModel的newIntVarFromDomain方法 - 单个值
	 */
	protected IntVar newIntVarFromDomain(long value, String name) {
		return this.model.newIntVarFromDomain(value, name);
	}

	/**
	 * 封装CpModel的newIntVarFromDomain方法 - 多个值
	 */
	protected IntVar newIntVarFromDomain(long[] values, String name) {
		return this.model.newIntVarFromDomain(values, name);
	}

	/**
	 * 封装CpModel的newIntVarFromDomain方法 - 区间
	 */
	protected IntVar newIntVarFromDomain(long[][] intervals, String name) {
		return this.model.newIntVarFromDomain(intervals, name);
	}

	/**
	 * 封装CpModel的newIntVarFromDomain方法 - 完整域
	 */
	protected IntVar newIntVarFromDomain(String name) {
		return this.model.newIntVarFromDomain(name);
	}

	/**
	 * 封装CpModel的newBoolVar方法
	 */
	protected BoolVar newBoolVar(String name) {
		return this.model.newBoolVar(name);
	}

	public Map<String, Var<?>> getVarMap() {
		return varMap;
	}

	public List<Var<?>> getVars() {
		return new ArrayList<>(varMap.values());
	}

	public void registerVar(String code, Var<?> var) {
		if (code != null && var != null) {
			varMap.put(code, var);
		}
	}

	/**
	 * 获取其他变量映射
	 */
	public Map<String, OtherVar> getOtherVarMap() {
		return model.otherVarMap;
	}

	protected ParaVar createParaVar(String code) {
		Para para = module != null ? module.getPara(code) : null;
		if (para == null) {
			throw new RuntimeException("Para not found for code: " + code);
		}
		ParaVar paraVar = new ParaVar();
		paraVar.setBase(para); 
		switch (para.getType()) {
			case INTEGER:
				paraVar.value = newIntVar(Integer.parseInt(para.getMinValue()), Integer.parseInt(para.getMaxValue()), f(ParaVar.VALUE_PATTEN, code));
				break;
			case ENUM:
				if (para.getOptions() == null) {
					throw new RuntimeException("Para options not found for code: " + code);
				}
				paraVar.value = newIntVarFromDomain(para.getOptionIds(), f(ParaVar.VALUE_PATTEN, code));

				for (ParaOption option : para.getOptions()) {
					ParaOptionVar optionVar = createParaOptionVar(para.getCode(), option.getCode());
					paraVar.optionSelectVars.put(option.getCodeId(), optionVar);
				}
				paraVar.optionSelectVars.forEach((optionId, optionVar) -> {
					model.addEquality((IntVar) paraVar.value, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
					model.addDifferent((IntVar) paraVar.value, optionId)
							.onlyEnforceIf(optionVar.getIsSelectedVar().not());
				});
				break;
			default:
				throw new RuntimeException("Para type not supported: " + para.getType());
		}
		paraVar.isHidden = newBoolVar(f(ParaVar.HIDDEN_PATTEN, code));
		registerVar(code, paraVar);
		return paraVar;
	}

	protected PartVar createPartVar(String code) {
		Part part = module != null ? module.getPart(code) : null;
		if (part == null) {
			throw new RuntimeException("Part not found for code: " + code);
		}
		PartVar partVar = new PartVar();
		partVar.setBase(part);
		// partVar.qty = model.newIntVar(0, 1, code);
		partVar.qty = newIntVar(0, part.getMaxQuantity(), f(PartVar.QTY_PATTEN, code));
		partVar.isHidden = newBoolVar(f(PartVar.HIDDEN_PATTEN, code));
		registerVar(code, partVar);
		return partVar;
	}

	protected ParaOptionVar createParaOptionVar(String paraCode, String optionCode) {
		Para para = module != null ? module.getPara(paraCode) : null;
		if (para == null) {
			throw new RuntimeException("Para not found for code: " + paraCode);
		}
		ParaOption option = para.getOption(optionCode);
		if (option == null) {
			throw new RuntimeException("ParaOption not found for code: " + optionCode);
		}
		ParaOptionVar optionVar = new ParaOptionVar(option);
		optionVar.isSelectedVar = newBoolVar(f(ParaVar.OPTIONS_PATTEN, paraCode, option.getCode()));
		return optionVar;
	}
	
	/**
     * 兼容性规则：Requires关系约束
     * 规则内容：如果左侧参数选择指定选项，则右侧参数必须选择指定选项
     * 例如：(a1,a3) Requires (b1,b2,b3) 表示如果A选择a1或a3，则B必须选择b1、b2或b3
     */
    public void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar, List<String> leftParaFilterOptionCodes,
		ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
		// left:确保只有一个参数选项被选中
		model.addExactlyOne(leftParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));
		// right:确保只有一个参数选项被选中
		model.addExactlyOne(rightParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));

		// 定义左侧条件：左侧集合中至少一个被选中
		BoolVar leftCond = newBoolVar(f("%s_leftCond", ruleCode));
		Literal[] leftSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftCond为true时，左侧集合中至少一个被选中；当为false时，左侧集合全部不被选中
		model.addBoolOr(leftSelected).onlyEnforceIf(leftCond);
		model.addBoolAnd(Arrays.stream(leftSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftCond.not());

		// 定义右侧条件：右侧集合中至少一个被选中
		BoolVar rightCond = newBoolVar(f("%s_rightCond", ruleCode));
		Literal[] rightSelected = rightParaVar.optionSelectVars.values().stream()
			.filter(option -> rightParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当rightCond为true时，右侧集合中至少一个被选中；当为false时，右侧集合全部不被选中
		model.addBoolOr(rightSelected).onlyEnforceIf(rightCond);
		model.addBoolAnd(Arrays.stream(rightSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(rightCond.not());
		
		// 实现Requires关系：如果左侧条件为true，则右侧条件必须为true
		model.addImplication(leftCond, rightCond);
	}
	
	/**
	 * 添加部件数量相等约束
	 * 根据partCode找到对应的partVar，并添加数量相等约束
	 * 
	 * @param partCode 部件代码
	 * @param partQuantity 期望的部件数量
	 */
	public void addPartEquality(String partCode, int partQuantity) {
		// 1. 根据partCode找到对应的partVar
		PartVar partVar = (PartVar) varMap.get(partCode);
		if (partVar == null) {
			throw new RuntimeException("PartVar not found for code: " + partCode);
		}
		
		// 2. 使用model.addEquality添加数量约束
		model.addEquality((IntVar) partVar.qty, partQuantity);
	}
	public void addParaEquality(String paraCode, String paraValue) {
		ParaVar paraVar = (ParaVar) varMap.get(paraCode);
		if (paraVar == null) {
			throw new RuntimeException("ParaVar not found for code: " + paraCode);
		}
		model.addEquality((IntVar) paraVar.value, Integer.parseInt(paraValue));
	}

	/**
     * 兼容性规则：CoDependent关系约束
     * 规则内容：双向依赖关系，左侧参数和右侧参数必须在对应的组内或组外
     * 例如：(a1,a3) CoDependent (b1,b2,b3) 表示：
     * - 如果A选择a1或a3，则B必须选择b1、b2或b3
     * - 如果A选择a2、a4或a5，则B必须选择b4或b5
     * - 如果B选择b1、b2或b3，则A必须选择a1或a3
     * - 如果B选择b4或b5，则A必须选择a2、a4或a5
     */
    public void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar, List<String> leftParaFilterOptionCodes,
		ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
		// left:确保只有一个参数选项被选中
		model.addExactlyOne(leftParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));
		// right:确保只有一个参数选项被选中
		model.addExactlyOne(rightParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));

		// 定义左侧条件：左侧集合中至少一个被选中
		BoolVar leftCond = newBoolVar(f("%s_leftCond", ruleCode));
		Literal[] leftSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftCond为true时，左侧集合中至少一个被选中；当为false时，左侧集合全部不被选中
		model.addBoolOr(leftSelected).onlyEnforceIf(leftCond);
		model.addBoolAnd(Arrays.stream(leftSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftCond.not());

		// 定义右侧条件：右侧集合中至少一个被选中
		BoolVar rightCond = newBoolVar(f("%s_rightCond", ruleCode));
		Literal[] rightSelected = rightParaVar.optionSelectVars.values().stream()
			.filter(option -> rightParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当rightCond为true时，右侧集合中至少一个被选中；当为false时，右侧集合全部不被选中
		model.addBoolOr(rightSelected).onlyEnforceIf(rightCond);
		model.addBoolAnd(Arrays.stream(rightSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(rightCond.not());

		// 定义左侧非条件：左侧集合外至少一个被选中
		BoolVar leftNotCond = newBoolVar(f("%s_leftNotCond", ruleCode));
		Literal[] leftNotSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> !leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftNotCond为true时，左侧集合外至少一个被选中；当为false时，左侧集合外全部不被选中
		model.addBoolOr(leftNotSelected).onlyEnforceIf(leftNotCond);
		model.addBoolAnd(Arrays.stream(leftNotSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftNotCond.not());

		// 定义右侧非条件：右侧集合外至少一个被选中
		BoolVar rightNotCond = newBoolVar(f("%s_rightNotCond", ruleCode));
		Literal[] rightNotSelected = rightParaVar.optionSelectVars.values().stream()
			.filter(option -> !rightParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当rightNotCond为true时，右侧集合外至少一个被选中；当为false时，右侧集合外全部不被选中
		model.addBoolOr(rightNotSelected).onlyEnforceIf(rightNotCond);
		model.addBoolAnd(Arrays.stream(rightNotSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(rightNotCond.not());
		
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
     */
    public void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar, List<String> leftParaFilterOptionCodes,
		ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
		// left:确保只有一个参数选项被选中
		model.addExactlyOne(leftParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));
		// right:确保只有一个参数选项被选中
		model.addExactlyOne(rightParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));

		// 定义左侧条件：左侧集合中至少一个被选中
		BoolVar leftCond = newBoolVar(f("%s_leftCond", ruleCode));
		Literal[] leftSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftCond为true时，左侧集合中至少一个被选中；当为false时，左侧集合全部不被选中
		model.addBoolOr(leftSelected).onlyEnforceIf(leftCond);
		model.addBoolAnd(Arrays.stream(leftSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftCond.not());

		// 定义右侧条件：右侧集合中至少一个被选中
		BoolVar rightCond = newBoolVar(f("%s_rightCond", ruleCode));
		Literal[] rightSelected = rightParaVar.optionSelectVars.values().stream()
			.filter(option -> rightParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当rightCond为true时，右侧集合中至少一个被选中；当为false时，右侧集合全部不被选中
		model.addBoolOr(rightSelected).onlyEnforceIf(rightCond);
		model.addBoolAnd(Arrays.stream(rightSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(rightCond.not());

		// 定义左侧非条件：左侧集合外至少一个被选中
		BoolVar leftNotCond = newBoolVar(f("%s_leftNotCond", ruleCode));
		Literal[] leftNotSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> !leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftNotCond为true时，左侧集合外至少一个被选中；当为false时，左侧集合外全部不被选中
		model.addBoolOr(leftNotSelected).onlyEnforceIf(leftNotCond);
		model.addBoolAnd(Arrays.stream(leftNotSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftNotCond.not());

		// 定义右侧非条件：右侧集合外至少一个被选中
		BoolVar rightNotCond = newBoolVar(f("%s_rightNotCond", ruleCode));
		Literal[] rightNotSelected = rightParaVar.optionSelectVars.values().stream()
			.filter(option -> !rightParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当rightNotCond为true时，右侧集合外至少一个被选中；当为false时，右侧集合外全部不被选中
		model.addBoolOr(rightNotSelected).onlyEnforceIf(rightNotCond);
		model.addBoolAnd(Arrays.stream(rightNotSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(rightNotCond.not());
		
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
	 * @param fstr 格式化字符串
	 * @param values 参数值
	 * @return 格式化后的字符串
	 */
	private String f(String fstr, String... values) {
		return String.format(fstr, (Object[]) values);
	}

	/**
	 * 创建字符串列表的工具方法
	 * 封装Arrays.asList，提供更简洁的API
	 * @param codes 字符串数组
	 * @return List<String>
	 */
	protected List<String> listOf(String... codes) {
		return Arrays.asList(codes);
	}
}