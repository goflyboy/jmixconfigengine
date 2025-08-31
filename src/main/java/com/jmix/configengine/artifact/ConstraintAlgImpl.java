package com.jmix.configengine.artifact;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.google.ortools.Loader;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Part;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 约束算法实现基类
 */
public abstract class ConstraintAlgImpl implements ConstraintAlg{
	static {
		Loader.loadNativeLibraries();
	}
	// CP模型
	protected CpModel model; 
	// module context
	protected Module module;
	// var map
	protected Map<String, Var<?>> varMap = new LinkedHashMap<>();

	public void initModel(CpModel model, Module module){
		this.model = model;
		this.module = module;
		initModelAfter(model);
		initVariables();
		initConstraint();
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
				String fieldName = field.getName();
				
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
	protected abstract void initConstraint();

	public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
		return model.newIntVarFromDomain(Domain.fromValues(values), name);
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

	protected ParaVar createParaVar(String code) {
		Para para = module != null ? module.getPara(code) : null;
		if (para == null) {
			throw new RuntimeException("Para not found for code: " + code);
		}
		ParaVar paraVar = new ParaVar();
		paraVar.setBase(para);
		paraVar.var = newIntVarFromDomain(model, para.getOptionIds(), code);
		if (para.getOptions() != null) {
			for (ParaOption option : para.getOptions()) {
				ParaOptionVar optionVar = createParaOptionVar(para.getCode(), option.getCode());
				paraVar.optionSelectVars.put(option.getCodeId(), optionVar);
			}
		}
		paraVar.optionSelectVars.forEach((optionId, optionVar) -> {
			model.addEquality((IntVar) paraVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
			model.addDifferent((IntVar) paraVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
		});
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
		// partVar.var = model.newIntVar(0, 1, code);
		partVar.var = model.newIntVar(0, part.getMaxQuantity(), code);
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
		optionVar.isSelectedVar = model.newBoolVar(paraCode + "_" + option.getCodeId());
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
		BoolVar leftCond = model.newBoolVar(ruleCode + "_" + "leftCond");
		Literal[] leftSelected = leftParaVar.optionSelectVars.values().stream()
			.filter(option -> leftParaFilterOptionCodes.contains(option.getCode()))
			.map(option -> (Literal) option.getIsSelectedVar())
			.toArray(Literal[]::new);
		// 当leftCond为true时，左侧集合中至少一个被选中；当为false时，左侧集合全部不被选中
		model.addBoolOr(leftSelected).onlyEnforceIf(leftCond);
		model.addBoolAnd(Arrays.stream(leftSelected).map(Literal::not).toArray(Literal[]::new)).onlyEnforceIf(leftCond.not());

		// 定义右侧条件：右侧集合中至少一个被选中
		BoolVar rightCond = model.newBoolVar(ruleCode + "_" + "rightCond");
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
		model.addEquality((IntVar) partVar.var, partQuantity);
	}
	public void addParaEquality(String paraCode, String paraValue) {
		ParaVar paraVar = (ParaVar) varMap.get(paraCode);
		if (paraVar == null) {
			throw new RuntimeException("ParaVar not found for code: " + paraCode);
		}
		model.addEquality((IntVar) paraVar.var, Integer.parseInt(paraValue));
	}
}