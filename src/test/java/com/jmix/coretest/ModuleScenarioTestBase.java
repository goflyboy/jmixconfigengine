package com.jmix.coretest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.cmodel.DiagnosticConstraint;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.ModuleConstraintExecutorImpl;
import com.jmix.executor.impl.SolutionUtils;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.model.AggregateConditionReq;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.PartCategoryConstraintReqBase;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;
import com.jmix.executor.model.StrategyConfig;
import com.jmix.executor.model.StrategyType;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.impl.ModulePacker;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模块场景测试基类
 * 
 * @since 2025-09-22
 */
@Slf4j
@Data
public abstract class ModuleScenarioTestBase {

    /**
     * 约束算法类
     */
    private Class<?> constraintAlgClazz;

    /**
     * 临时资源路径
     */
    private String tempResourcePath = "";

    /**
     * 模块
     */
    private Module module;

    /**
     * 配置
     */
    protected ConstraintConfig cfg;

    /**
     * 解决方案列表
     */
    private List<ModuleInst> solutions = new ArrayList<>();

    /**
     * 推理结果
     */
    private Result<List<ModuleInst>> result;

    /**
     * 是否枚举所有解决方案
     */
    private boolean enumerateAllSolution = true;

    /**
     * 构造函数
     * 
     * @param constraintAlgClazz 约束算法类
     */
    public ModuleScenarioTestBase(Class<?> constraintAlgClazz) {
        this.constraintAlgClazz = constraintAlgClazz;
    }

    /**
     * 每个用例执行前调用
     */
    @BeforeEach
    public void setUp() {
        init();
    }

    /**
     * 每个用例执行后调用
     */
    @AfterEach
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
    }

    /**
     * 构建Module数据
     * 
     * @param moduleAlgClazz 模块算法类
     * @return 构建的模块
     */
    protected Module buildModule(Class<?> moduleAlgClazz) {
        // 通过注解生成Module
        module = ModuleGenneratorByAnno.build(moduleAlgClazz, tempResourcePath);
        return module;
    }

    /**
     * 初始化测试环境（打包模式）
     * 
     */
    protected void init() {
        // 生成临时资源路径
        setTempResourcePath(CommHelper.createTempPath(getConstraintAlgClazz()));

        // 使用单例访问
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true); // 测试环境直接使用当前classpath加载
        // config.setRootFilePath(getTempResourcePath());
        config.setLogFilePath(getTempResourcePath());
        config.setLogModelProto(true);
        beforeInitConfig(config);
        String rootFilePath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "cproot";
        config.setRootFilePath(rootFilePath);
        ModuleConstraintExecutor.INST.init(config);
        setCfg(config);

        if (getCfg().isAttachedDebug()) {
            // 调试模式：直接加载class，和现有流程一样
            Module tempModule = buildModule(getConstraintAlgClazz());
            Result<Void> addModuleResult = ModuleConstraintExecutor.INST.addModule(tempModule.getId(), tempModule);
            assertEquals(Result.SUCCESS, addModuleResult.getCode(),
                    "Add module failed: " + addModuleResult.getMessage());
            setModule(tempModule);
        } else {
            // 打包模式
            Module tempModule = ModuleGenneratorByAnno.buildModule(getConstraintAlgClazz());

            // 调用ModulePacker.pack进行打包
            ModulePacker packer = new ModulePacker();
            String packOutputDir = packer.pack(tempModule, getConstraintAlgClazz(), rootFilePath);
            log.info("Module packed to: {}", packOutputDir);

            Result<Void> addModuleResult = ModuleConstraintExecutor.INST.addModule(tempModule.getId(), tempModule);
            assertEquals(Result.SUCCESS, addModuleResult.getCode(),
                    "Add module failed: " + addModuleResult.getMessage());
            setModule(tempModule);
        }
    }

    /**
     * 初始化配置
     * 
     * @param cfg 配置
     */
    protected void beforeInitConfig(ConstraintConfig cfg) {

    }

    /**
     * 执行参数推理
     * 
     * @param partCode           部件代码
     * @param qty                数量
     * @param paraCodeValuePairs 参数代码和值的交替数组，格式：paraCode1, value1, paraCode2,
     *                           value2, ...
     * @return 推理结果
     */
    protected List<ModuleInst> inferParas(String partCode, Integer qty, String... paraCodeValuePairs) {
        return inferParas(partCode, qty, toPreParas(paraCodeValuePairs), new ArrayList<>());
    }

    /**
     * 执行参数推理
     * s
     * 
     * @param partCode 部件代码
     * @param qty      数量
     * @param preParas 前置参数列表
     * @param preParts 前置部件列表
     * @return 推理结果
     */
    protected List<ModuleInst> inferParas(String partCode, Integer qty, List<ParaInst> preParas,
            List<PartInst> preParts) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());

        // 设置主部件实例
        PartInst mainPartInst = new PartInst();
        mainPartInst.setCode(partCode);
        mainPartInst.setQuantity(qty);
        req.setMainPartInst(mainPartInst);

        // 处理预定义参数（复用公共方法）
        req.setPreParaInsts(preParas);
        req.setPrePartInsts(preParts);

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        log.info("Inference result: {}", result);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 根据多个参数值进行推理（可变参数版本）
     *
     * @param paraCodeValuePairs 参数代码和值的交替数组，格式：paraCode1, value1, paraCode2,
     *                           value2, ...
     * @return 推理结果
     */
    protected List<ModuleInst> inferParasByPara(String... paraCodeValuePairs) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());

        // 复用公共方法处理参数
        req.setPreParaInsts(toPreParas(paraCodeValuePairs));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 使用 relaxSolve=true 进行参数推理（冲突诊断模式）。
     *
     * @param paraCodeValuePairs 参数代码和值的交替数组
     * @return 推理结果（部分解）
     */
    protected List<ModuleInst> inferParasByParaRelax(String... paraCodeValuePairs) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());
        req.setRelaxSolve(true);

        req.setPreParaInsts(toPreParas(paraCodeValuePairs));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 校验给定部件组合是否满足当前模块规则。
     */
    protected boolean validData(String... selectedPartCodes) {
        return validData(toPartInsts(selectedPartCodes).toArray(PartInst[]::new));
    }

    protected boolean validData(PartInst... partInsts) {
        return validateData(partInsts).isValid();
    }

    protected ModuleValidateResp validateData(String... selectedPartCodes) {
        return validateData(toPartInsts(selectedPartCodes).toArray(PartInst[]::new));
    }

    protected ModuleValidateResp validateData(PartInst... partInsts) {
        Result<ModuleValidateResp> result = ModuleConstraintExecutor.INST.validate(
                validateReq(moduleInst(partInsts)));
        assertEquals(Result.SUCCESS, result.getCode(), "Validate failed: " + result.getMessage());
        return result.getData();
    }

    protected ModuleInst moduleInst(PartInst... partInsts) {
        ModuleInst inst = new ModuleInst();
        inst.setId(getModule().getId());
        inst.setCode(getModule().getCode());
        for (PartInst partInst : partInsts) {
            inst.addPartInst(partInst);
        }
        return inst;
    }

    protected PartInst partInst(String code, int quantity) {
        PartInst partInst = new PartInst(code, quantity);
        partInst.setSelected(quantity > 0);
        return partInst;
    }

    private ModuleValidateReq validateReq(ModuleInst moduleInst) {
        ModuleValidateReq req = new ModuleValidateReq();
        req.setModuleId(getModule().getId());
        req.setModuleCode(getModule().getCode());
        req.setModuleInst(moduleInst);
        return req;
    }

    private List<PartInst> toPartInsts(String... selectedPartCodes) {
        List<PartInst> partInsts = new ArrayList<>();
        for (String selectedPartCode : selectedPartCodes) {
            partInsts.add(partInst(selectedPartCode, 1));
        }
        return partInsts;
    }

    /**
     * 校验部分解中包含期望的字符串（从 SolverResult.solutions 中查找）。
     */
    protected void assertPartialSoluContain(String... expectedSubstrings) {
        SolverResult sr = getResult().getSolverResult();
        if (sr == null || sr.getSolutions() == null || sr.getSolutions().isEmpty()) {
            throw new AssertionError("No partial solutions available");
        }
        for (String expected : expectedSubstrings) {
            boolean found = false;
            for (ModuleInst solution : sr.getSolutions()) {
                String solutionStr = solution.toShortString(true);
                if (solutionStr != null && solutionStr.contains(expected)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("No partial solution found containing: ").append(expected);
                errorMsg.append("\nAvailable partial solutions:");
                for (int i = 0; i < sr.getSolutions().size(); i++) {
                    errorMsg.append("\n  PS_").append(i + 1).append(": ")
                            .append(sr.getSolutions().get(i).toShortString(true));
                }
                throw new AssertionError(errorMsg.toString());
            }
        }
    }

    /**
     * 获取指定索引的解决方案
     */
    protected ProgammableInstAssert solutions(int index) {
        if (getSolutions() == null || index >= getSolutions().size()) {
            throw new IndexOutOfBoundsException("Solution index out of bounds: " + index);
        }
        ModuleInst solution = getSolutions().get(index);
        return new ProgammableInstAssert(solution, getModule());
    }

    /**
     * 打印所有解决方案
     */
    protected void printSolutionsDetail() {
        if (getSolutions() == null || getSolutions().isEmpty()) {
            log.info("No solutions found");
            return;
        }

        log.info("Found {} solutions:\n", getSolutions().size());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < getSolutions().size(); i++) {
            sb.append(
                    String.format("\nSolution %d: %s", i, ModuleConstraintExecutorImpl.toJson(getSolutions().get(i))));
        }
        log.info(sb.toString());
    }

    /**
     * 获取结果断言对象，用于验证执行结果
     */
    protected ResultAssert resultAssert() {
        if (getResult() == null) {
            throw new IllegalStateException("Inference not executed yet, cannot get result assertion");
        }
        return new ResultAssert(getResult());
    }

    /**
     * 检查解决方案列表中是否包含指定的字符串
     * 遍历所有解决方案的 toShortString 输出，检查是否包含指定的子字符串
     * 如果找不到匹配的解决方案，则抛出 AssertionError
     * 
     * @param expectedStr 期望包含的字符串
     * @throws AssertionError 如果找不到包含指定字符串的解决方案
     */
    protected void soluContain(String expectedStr) {
        if (getSolutions() == null || getSolutions().isEmpty()) {
            throw new AssertionError("No solutions found, cannot check if contains: " + expectedStr);
        }
        if (expectedStr == null || expectedStr.isEmpty()) {
            return;
        }
        for (ModuleInst solution : getSolutions()) {
            String solutionStr = solution.toShortString(true);
            if (solutionStr != null && solutionStr.contains(expectedStr)) {
                log.info("Found solution containing '{}': {}", expectedStr, solutionStr);
                return;
            }
        }
        // 构建错误消息，包含所有解决方案的字符串表示
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("No solution found containing: ").append(expectedStr);
        errorMsg.append("\nAvailable solutions:");
        for (int i = 0; i < getSolutions().size(); i++) {
            String solutionStr = getSolutions().get(i).toShortString(true);
            errorMsg.append("\n  S_").append(i + 1).append(": ").append(solutionStr);
        }
        throw new AssertionError(errorMsg.toString());
    }

    /**
     * 校验满足条件conditionExpr解的个数是否等于expectSolutionNum
     * 
     * @param conditionExpr     条件表达式，格式如："Color:Red,TShirt1:2"
     * @param expectSolutionNum 期望的解决方案数量
     */
    protected void assertSolutionNum(String conditionExpr, int expectSolutionNum) {
        // 如果expectSolutionNum为0，则不进行验证
        if (expectSolutionNum == 0 && (getSolutions() == null || getSolutions().isEmpty())) {
            return;
        }
        if (getSolutions() == null || getSolutions().isEmpty()) {
            throw new AssertionError("No solutions available for verification");
        }
        // 解析条件表达式
        Map<String, String> kvMap = parseConditionExpr(conditionExpr);

        // 构建条件对象列表
        List<ConditionElement> conditionElements = buildConditionElements(kvMap);

        // 计算实际匹配的解决方案数量
        int actualMatchSolutionNum = countMatchingSolutions(conditionElements);

        // 比较实际数量和期望数量
        if (actualMatchSolutionNum != expectSolutionNum) {
            throw new AssertionError(String.format(
                    "解决方案数量不匹配，期望: %d，实际: %d，条件: %s",
                    expectSolutionNum, actualMatchSolutionNum, conditionExpr));
        }

        log.info("Solution count verification passed, condition: {}, count: {}", conditionExpr, actualMatchSolutionNum);
    }

    /**
     * 解析条件表达式
     * 格式："Color:Red,TShirt1:2" -> {"Color":"Red", "TShirt1":"2"}
     * 
     * @param conditionExpr 条件表达式字符串
     * @return 解析后的键值对映射
     */
    private Map<String, String> parseConditionExpr(String conditionExpr) {
        Map<String, String> kvMap = new HashMap<>();
        String[] pairs = conditionExpr.split(",");

        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                kvMap.put(kv[0].trim(), kv[1].trim());
            } else {
                throw new IllegalArgumentException("Invalid condition expression format: " + pair);
            }
        }

        return kvMap;
    }

    /**
     * 构建条件元素列表
     * 
     * @param kvMap 键值对映射
     * @return 条件元素列表
     */
    private List<ConditionElement> buildConditionElements(Map<String, String> kvMap) {
        List<ConditionElement> elements = new ArrayList<>();

        for (Map.Entry<String, String> entry : kvMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ConditionElement element = createConditionElement(key, value);
            elements.add(element);
        }

        return elements;
    }

    /**
     * 创建条件元素
     * 
     * @param key   键
     * @param value 值
     * @return 条件元素
     * @throws AlgLoaderException 当找不到参数或部件时
     */
    private ConditionElement createConditionElement(String key, String value) {
        // 在module.paras中查找
        java.util.Optional<Para> paraOpt = getModule().getPara(key);
        if (paraOpt.isPresent()) {
            return createParaConditionElement(key, value, paraOpt.get());
        }

        // 在module.parts中查找
        IPart partOpt = getModule().getPart(key);
        if (partOpt != null) {
            return createPartConditionElement(key, value);
        }

        // 都找不到，报错
        throw new AlgLoaderException(String.format(
                "Parameter or part not found in Module: %s", key));
    }

    /**
     * 创建参数条件元素
     * 
     * @param key   键
     * @param value 值
     * @param para  参数对象
     * @return 条件元素
     */
    private ConditionElement createParaConditionElement(String key, String value, Para para) {
        String codeIdValue = ParaTypeHandler.getCodeIdValue(para, value);
        return new ConditionElement("para", key, codeIdValue);
    }

    /**
     * 创建部件条件元素
     * 
     * @param key   键
     * @param value 值
     * @return 条件元素
     */
    private ConditionElement createPartConditionElement(String key, String value) {
        return new ConditionElement("part", key, value);
    }

    /**
     * 计算匹配条件的解决方案数量
     * 
     * @param conditionElements 条件元素列表
     * @return 匹配的解决方案数量
     */
    private int countMatchingSolutions(List<ConditionElement> conditionElements) {
        int matchCount = 0;

        for (ModuleInst solution : getSolutions()) {
            boolean isMatch = true;

            for (ConditionElement element : conditionElements) {
                if ("para".equals(element.type)) {
                    // 对paraInst，比较value和paraInst.value
                    ParaInst paraInst = findParaInstByCode(solution, element.code);
                    if (paraInst == null || !element.value.equals(paraInst.getValue())) {
                        isMatch = false;
                        break;
                    }
                } else if ("part".equals(element.type)) {
                    // 对partInst，比较value和partInst.quantity
                    PartInst partInst = findPartInstByCode(solution, element.code);
                    if (partInst == null || !element.value.equals(String.valueOf(partInst.getQuantity()))) {
                        isMatch = false;
                        break;
                    }
                } else {
                    throw new IllegalArgumentException("Invalid condition element type: " + element.type);
                }
            }

            if (isMatch) {
                matchCount++;
            }
        }

        return matchCount;
    }

    /**
     * 根据代码查找ParaInst
     * 
     * @param solution 模块实例
     * @param code     参数代码
     * @return 匹配的ParaInst，如果未找到则返回null
     */
    private ParaInst findParaInstByCode(ModuleInst solution, String code) {
        if (solution.getParas() == null) {
            return null;
        }
        return solution.getParas().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据代码查找PartInst
     * 
     * @param solution 模块实例
     * @param code     部件代码
     * @return 匹配的PartInst，如果未找到则返回null
     */
    private PartInst findPartInstByCode(ModuleInst solution, String code) {
        if (solution.getParts() == null) {
            return null;
        }
        return solution.getParts().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据参数代码值对构建ParaInst列表（公共方法）
     * 
     * @param paraCodeValuePairs 参数代码和值的交替数组
     * @return ParaInst列表
     */
    protected List<ParaInst> toPreParas(String... paraCodeValuePairs) {
        if (paraCodeValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Parameters must be in even numbers, format: paraCode1, value1, paraCode2, value2, ...");
        }

        List<ParaInst> paraInsts = new ArrayList<>();

        for (int i = 0; i < paraCodeValuePairs.length; i += 2) {
            String paraCode = paraCodeValuePairs[i];
            String value = paraCodeValuePairs[i + 1];

            ParaInst paraInst = new ParaInst();
            paraInst.setCode(paraCode);

            // 根据module.paras中的para.options，找到value对应的option
            Optional<Para> paraOpt = getModule().getPara(paraCode);
            if (!paraOpt.isPresent()) {
                throw new AlgLoaderException(String.format("Parameter %s does not exist", paraCode));
            }
            Para para = paraOpt.get();
            paraInst.setValue(ParaTypeHandler.getCodeIdValue(para, value));
            paraInsts.add(paraInst);
        }

        return paraInsts;
    }

    /**
     * 根据Part代码值对构建PartInst列表（公共方法）
     * 
     * @param partCodeValuePairs Part代码和值的交替数组
     * @return ParaInst列表
     */
    protected List<PartInst> toPreParts(String... partCodeValuePairs) {
        if (partCodeValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Parts must be in even numbers, format: partCode1, value1, partCode2, value2, ...");
        }

        List<PartInst> partInsts = new ArrayList<>();

        for (int i = 0; i < partCodeValuePairs.length; i += 2) {
            String partCode = partCodeValuePairs[i];
            String value = partCodeValuePairs[i + 1];

            PartInst partInst = new PartInst();
            partInst.setCode(partCode);

            // 根据module.parts中
            IPart partOpt = getModule().getPart(partCode);
            if (partOpt == null) {
                throw new AlgLoaderException(String.format("Part %s does not exist", partCode));
            }
            partInst.setQuantity(Integer.parseInt(value));
            partInsts.add(partInst);
        }

        return partInsts;
    }

    /**
     * 条件元素内部类
     */
    private static class ConditionElement {

        private final String type; // "para" 或 "part"

        private final String code; // 代码

        private final String value; // 值

        ConditionElement(String type, String code, String value) {
            this.type = type;
            this.code = code;
            this.value = value;
        }
    }

    /**
     * 打印短格式解信息
     * 
     */
    protected void printSolutions() {
        printSolutions(getModule(), getSolutions(), false);
    }

    protected void printSimpleSolutions() {
        printSolutions(getModule(), getSolutions(), true);
    }

    /**
     * 打印短格式解信息
     * 
     * @param module    模块
     * @param solutions 解列表
     */
    protected void printSolutions(Module module, List<ModuleInst> solutions, boolean isSimple) {
        SolutionUtils.printSolutions(module, solutions, isSimple, result);
    }

    /**
     * 将字符串约束请求转换为PartConstraintReq列表
     *
     * @param partCategory 部件类别
     * @param strReqs      字符串约束请求数组，格式如："sd:Sum_Quantity ==2 where Speed=5400"
     * @return PartConstraintReq列表
     */
    protected List<PartConstraintReq> toPartConstraintReqs(String partCategory, String... strReqs) {
        List<PartConstraintReq> reqs = new ArrayList<>();

        for (String strReq : strReqs) {
            parseConstraintReq(partCategory, strReq, reqs, null);
        }

        return reqs;
    }

    private void parseConstraintReq(
            String partCategory,
            String strReq,
            List<PartConstraintReq> partReqs,
            List<CrossCategoryPartCategoryConstraintReq> crossReqs) {
        PartConstraintReq partReq = new PartConstraintReq();

        // Parse strategy syntax: [strategy=ASCENDING:price]
        String remainingStr = parseStrategyConfig(strReq, partReq);

        // 解析格式：reqPartCategory:attrCode comparator value where condition
        // 示例："sd:Sum_Quantity ==2 where Speed=5400"
        String[] categoryParts = remainingStr.split(":", 2);
        String reqPartCategory;
        String attrExprStr;
        if (categoryParts.length == 2) {
            reqPartCategory = categoryParts[0].trim();
            attrExprStr = categoryParts[1].trim();
        } else {
            // 如果没有:，则使用默认的partCategory
            reqPartCategory = partCategory;
            attrExprStr = remainingStr;
        }

        if (isCrossCategoryReq(reqPartCategory)) {
            if (crossReqs == null) {
                throw new IllegalArgumentException(
                        "Cross category total request should be handled by inferRecommendModule");
            }
            CrossCategoryPartCategoryConstraintReq crossReq = new CrossCategoryPartCategoryConstraintReq();
            crossReq.setPartCategoryCodes(toPartCategoryCodes(reqPartCategory));
            parseAttrExpr(attrExprStr, crossReq);
            crossReqs.add(crossReq);
            return;
        }

        partReq.setPartCategoryCode(reqPartCategory);
        parseAttrExpr(attrExprStr, partReq);
        partReqs.add(partReq);
    }

    private boolean isCrossCategoryReq(String reqPartCategory) {
        return reqPartCategory != null && reqPartCategory.contains(",");
    }

    private List<String> toPartCategoryCodes(String partCategoryList) {
        List<String> codes = new ArrayList<>();
        for (String code : partCategoryList.split(",")) {
            String trimmedCode = code.trim();
            if (!trimmedCode.isEmpty()) {
                codes.add(trimmedCode);
            }
        }
        return codes;
    }

    /**
     * Parse strategy configuration from the constraint request string.
     * Syntax: [strategy=ASCENDING:price] or [strategy=DESCENDING:attrCode]
     *
     * @param strReq the full constraint request string
     * @param req    the PartConstraintReq to populate
     * @return the remaining string after removing strategy configuration
     */
    private String parseStrategyConfig(String strReq, PartConstraintReq req) {
        Pattern strategyPattern = Pattern.compile("\\[strategy=(ASCENDING|DESCENDING|UNSPECIFIED):(\\w+)\\]");
        Matcher matcher = strategyPattern.matcher(strReq);
        if (matcher.find()) {
            StrategyConfig config = new StrategyConfig();
            config.setStrategyType(StrategyType.valueOf(matcher.group(1)));
            config.setSortAttributeCode(matcher.group(2));
            List<StrategyConfig> strategies = new ArrayList<>();
            strategies.add(config);
            req.setDecisionStrategies(strategies);
            // Remove the strategy part from the string
            return matcher.replaceFirst("").trim();
        }
        return strReq;
    }

    /**
     * 解析属性表达式为PartConstraintReq
     * 支持三种场景：
     * 场景1：仅有过滤条件，如 "where Speed=5400" -> attrWhereCondition="Speed=5400", attrComparator=null
     * 场景2：仅有汇总条件，如 "Sum_Capacity >=5" -> attrWhereCondition=null, attrType=Sum, attrCode=Capacity, attrComparator=">=", attrValue="5"
     * 场景3：同时有汇总条件和过滤条件，如 "Sum_Capacity >=5 where Speed=5400" -> 同时设置两个条件
     *
     * @param attrExpr 属性表达式（可能包含 where 子句）
     * @param req 部件约束请求对象，用于设置解析结果
     */
    protected void parseAttrExpr(String attrExpr, PartCategoryConstraintReqBase req) {
        parseAggregateAttrExpr(attrExpr, req);
    }

    private void parseAggregateAttrExpr(String attrExpr, PartCategoryConstraintReqBase req) {
        ParsedAttrExpr parsed = splitWhere(attrExpr);
        if (parsed.whereCondition != null && !parsed.whereCondition.isEmpty()) {
            req.setAttrWhereCondition(parsed.whereCondition);
        }

        req.getAggregateConditions().clear();
        if (parsed.aggregateClause.isEmpty()) {
            if (parsed.whereCondition != null && !parsed.whereCondition.isEmpty()) {
                req.addAggregateCondition(defaultQuantityCondition());
            }
            return;
        }

        if (containsUnsupportedAggregateOperator(parsed.aggregateClause)) {
            throw new IllegalArgumentException("Invalid aggregate expression format: " + parsed.aggregateClause);
        }
        if (hasEmptyAggregatePart(parsed.aggregateClause)) {
            throw new IllegalArgumentException("Invalid aggregate expression format: " + parsed.aggregateClause);
        }

        String[] aggregateParts = parsed.aggregateClause.split("\\s*&&\\s*");
        if (req instanceof CrossCategoryPartCategoryConstraintReq && aggregateParts.length > 1) {
            throw new IllegalArgumentException("Cross category total request does not support multi aggregate");
        }
        for (String aggregatePart : aggregateParts) {
            String trimmedPart = aggregatePart.trim();
            if (trimmedPart.isEmpty()) {
                throw new IllegalArgumentException("Invalid aggregate expression format: " + parsed.aggregateClause);
            }
            req.addAggregateCondition(parseAggregateCondition(trimmedPart));
        }
    }

    private ParsedAttrExpr splitWhere(String attrExpr) {
        String trimmedExpr = attrExpr == null ? "" : attrExpr.trim();
        Pattern wherePattern = Pattern.compile("\\bwhere\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = wherePattern.matcher(trimmedExpr);
        if (!matcher.find()) {
            return new ParsedAttrExpr(trimmedExpr, null);
        }
        int whereStart = matcher.start();
        int whereEnd = matcher.end();
        if (matcher.find()) {
            throw new IllegalArgumentException("Only one where clause is supported: " + attrExpr);
        }
        String aggregateClause = trimmedExpr.substring(0, whereStart).trim();
        String whereCondition = trimmedExpr.substring(whereEnd).trim();
        return new ParsedAttrExpr(aggregateClause, whereCondition);
    }

    private boolean containsUnsupportedAggregateOperator(String aggregateClause) {
        return aggregateClause.contains("||")
                || Pattern.compile("\\bOR\\b", Pattern.CASE_INSENSITIVE).matcher(aggregateClause).find();
    }

    private boolean hasEmptyAggregatePart(String aggregateClause) {
        return Pattern.compile("(^\\s*&&|&&\\s*$|&&\\s*&&)").matcher(aggregateClause).find();
    }

    private AggregateConditionReq parseAggregateCondition(String aggregateExpr) {
        Pattern pattern = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|<=|>=|<|>)\\s*(\\d+)");
        Matcher matcher = pattern.matcher(aggregateExpr);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid attribute expression format: " + aggregateExpr);
        }
        String mergedAttrCode = matcher.group(1).trim();
        String[] mergedParts = mergedAttrCode.split(AttrPara.CODE_SEPARATOR);
        if (mergedParts.length != 2) {
            throw new IllegalArgumentException("Invalid attribute expression format: " + aggregateExpr);
        }
        AggregateConditionReq condition = new AggregateConditionReq();
        condition.setAttrType(AttrParaType.valueOf(mergedParts[0]));
        condition.setAttrCode(mergedParts[1]);
        condition.setComparator(matcher.group(2).trim());
        condition.setAttrValue(matcher.group(3).trim());
        return condition;
    }

    private AggregateConditionReq defaultQuantityCondition() {
        AggregateConditionReq condition = new AggregateConditionReq();
        condition.setAttrType(AttrParaType.Sum);
        condition.setAttrCode(PartConstantAttr.Quantity.getCode());
        condition.setComparator(">=");
        condition.setAttrValue("1");
        condition.setDefaulted(true);
        return condition;
    }

    private static class ParsedAttrExpr {

        private final String aggregateClause;

        private final String whereCondition;

        ParsedAttrExpr(String aggregateClause, String whereCondition) {
            this.aggregateClause = aggregateClause == null ? "" : aggregateClause;
            this.whereCondition = whereCondition;
        }
    }

    /**
     * 基于部件约束进行推理推荐
     * 
     * @param constraintReqs 约束请求字符串数组
     * @return 推荐的模块实例列表
     */
    protected List<ModuleInst> inferRecommendModule(String... constraintReqs) {
        return inferRecommend("", constraintReqs);
    }

    /**
     * 基于部件约束进行推理推荐
     * 
     * @param partCategoryCode 部件类别
     * @param constraintReqs   约束请求字符串数组
     * @return 推荐的模块实例列表
     */
    protected List<ModuleInst> inferRecommend(String partCategoryCode, String... constraintReqs) {
        List<PartConstraintReq> partConstraintReqs = new ArrayList<>();
        List<CrossCategoryPartCategoryConstraintReq> crossReqs = new ArrayList<>();
        for (String constraintReq : constraintReqs) {
            parseConstraintReq(partCategoryCode, constraintReq, partConstraintReqs, crossReqs);
        }

        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());
        req.setPartCategoryCode(partCategoryCode);
        req.setPartConstraintReqs(partConstraintReqs);
        req.setCrossCategoryConstraintReqs(crossReqs);

        long startTime = System.currentTimeMillis();
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        log.info("Inference recommend execution time: {} ms", executionTime);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    private void assertSoluSizeEqual(int size) {
        assertEquals(size, solutions.size(), "Solutions size should be " + size);
    }

    /**
     * 比较Solution的值
     *
     * @param index
     * @param expect
     */
    public void assertSoluContain(int index, String expect) {
        assertSoluContain(index, expect, true);
    }

    private void assertSoluContain(int index, String expect, boolean isSimple) {
        String msg = "Solution at index " + index + " should match expected string";
        assertTrue(solutions.size() >= index, msg);
        assertStringContains(solutions.get(index - 1).toShortString(isSimple),
                expect,
                msg);
    }

    private void assertStringContains(String real, String expect, String msg) {
        // boolean result = real.contains(expect);
        boolean result = contains(real, expect);
        if (!result) {
            assertEquals(expect, real, "contains is false," + msg);
        }
    }

    protected void assertSoluContain(String expect) {
        assertSoluContain(expect, true);
    }

    private void assertSoluContain(String expect, boolean isSimple) {
        boolean isContain = false;
        for (ModuleInst solu : result.getData()) {
            if (contains(solu.toShortString(isSimple), expect)) {
                isContain = true;
                break;
            }
        }
        assertTrue(isContain,
                "Solution contain " + expect);
    }

    private boolean contains(String realWholeStr, String expectStr) {
        List<String> expectParts = parseExpect(expectStr);
        for (String expectPart : expectParts) {
            if (!realWholeStr.contains(expectPart)) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseExpect(String expectStr) {
        // expectStr = "cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)"
        // 预期结果：["cpu2(Q:20,H:0,S:1)","md1(Q:5,H:0,S:1)","sd1(0*)"
        // 处理逻辑： 根据“),”来拆分
        List<String> result = new ArrayList<>();
        String[] items = expectStr.split("\\),");
        for (String item : items) {
            result.add(item.trim());
        }
        return result;
    }

}
