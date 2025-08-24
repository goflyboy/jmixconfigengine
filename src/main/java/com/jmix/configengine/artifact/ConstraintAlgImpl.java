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
		partVar.var = model.newIntVar(0, 1, code);
		// partVar.var = model.newIntVar(0, 1, code);
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
     * 兼容性规则：颜色和尺寸兼容关系规则
     * 规则内容：如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号
     */
    public void addCompatibleConstraint(String ruleCode,ParaVar leftParaVar, List<String> leftParaFilterOptionCodes,
		ParaVar rightParaVar,List<String> rightParaFilterOptionCodes) {
		// left:确保只有一个颜色选项被选中,part要考虑TODO
		model.addExactlyOne(leftParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));
		// right:确保只有一个颜色选项被选中,part要考虑TODO
		model.addExactlyOne(rightParaVar.optionSelectVars.values().stream()
			.map(option -> option.getIsSelectedVar())
			.toArray(BoolVar[]::new));

		// 4. 定义筛选后的集合 Set1=(A1,A2,A3) =>筛出的结果A1，A2 (filterCodeIds)
		// 左表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)

		// BoolVar leftCond = model.newBoolVar(ruleCode + "_" + "leftCond");
		// model.addBoolOr(new Literal[]{
		// 	leftParaVar.getParaOptionByCode("Red").getIsSelectedVar()
		// }).onlyEnforceIf(leftCond);
		// model.addBoolAnd(new Literal[]{
		// 	leftParaVar.getParaOptionByCode("Red").getIsSelectedVar().not()
		// }).onlyEnforceIf(leftCond.not());
		BoolVar leftCond = model.newBoolVar(ruleCode + "_" + "leftCond");
		leftParaVar.optionSelectVars.values().stream().filter(option -> leftParaFilterOptionCodes.contains(option.getCode())).forEach(option -> {
			model.addBoolOr(new Literal[]{option.getIsSelectedVar()}).onlyEnforceIf(leftCond);
			model.addBoolAnd(new Literal[]{option.getIsSelectedVar().not()}).onlyEnforceIf(leftCond.not());
		});


		
		// 右表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
		// BoolVar rightCond = model.newBoolVar("rule1" + "_" + "rightCond");
		// model.addBoolOr(new Literal[]{
		// 	rightParaVar.getParaOptionByCode("Big").getIsSelectedVar(),
		// 	rightParaVar.getParaOptionByCode("Small").getIsSelectedVar()
		// }).onlyEnforceIf(rightCond);
		// model.addBoolAnd(new Literal[]{
		// 	rightParaVar.getParaOptionByCode("Big").getIsSelectedVar().not(),
		// 	rightParaVar.getParaOptionByCode("Small").getIsSelectedVar().not()
		// }).onlyEnforceIf(rightCond.not());
		BoolVar rightCond = model.newBoolVar("rule1" + "_" + "rightCond");
		rightParaVar.optionSelectVars.values().stream().filter(option -> rightParaFilterOptionCodes.contains(option.getCode())).forEach(option -> {
			model.addBoolOr(new Literal[]{option.getIsSelectedVar()}).onlyEnforceIf(rightCond);
			model.addBoolAnd(new Literal[]{option.getIsSelectedVar().not()}).onlyEnforceIf(rightCond.not());
		});
		// 5. 实现Codependent关系
		model.addEquality(leftCond, rightCond);
	}
}