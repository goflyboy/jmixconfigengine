package com.jmix.configengine.scenario.base;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.impl.ModuleConstraintExecutorImpl;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.OtherVar;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;
import com.jmix.tool.model.CommHelper;
import com.jmix.tool.model.ModuleGenneratorByAnno;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块场景测试基类
 */
@Slf4j
@Data
public abstract class ModuleScenarioTestBase {

    /**
     * 约束算法类
     */
    private Class<? extends ConstraintAlgImpl> constraintAlgClazz;

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
    private ConstraintConfig cfg;

    /**
     * 解决方案列表
     */
    private List<ModuleInst> solutions;

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
     */
    public ModuleScenarioTestBase(Class<? extends ConstraintAlgImpl> constraintAlgClazz) {
        this.constraintAlgClazz = constraintAlgClazz;
    }

    /**
     * 每个用例执行前调用
     */
    @Before
    public void setUp() {
        init();
    }

    /**
     * 每个用例执行后调用
     */
    @After
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
    }

    /**
     * 构建Module数据
     */
    protected Module buildModule(Class<? extends ConstraintAlgImpl> moduleAlgClazz) {
        // 通过注解生成Module
        module = ModuleGenneratorByAnno.build(moduleAlgClazz, tempResourcePath);
        return module;
    }

    // createTempPath方法已移动到CommHelper类中
    /**
     * 初始化测试环境
     */
    protected void init() {
        // 生成临时资源路径
        setTempResourcePath(CommHelper.createTempPath(getConstraintAlgClazz()));
        // 使用单例访问
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true); // 测试环境直接使用当前classpath加载
        config.setRootFilePath(getTempResourcePath());
        config.setLogFilePath(getTempResourcePath());
        config.setLogModelProto(true);
        beforeInitConfig(config);
        ModuleConstraintExecutor.INST.init(config);
        setCfg(config);

        Module moduleInstance = buildModule(getConstraintAlgClazz());
        ModuleConstraintExecutor.INST.addModule(moduleInstance.getId(), moduleInstance);
        setModule(moduleInstance);
    }

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
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());

        // 设置主部件实例
        PartInst mainPartInst = new PartInst();
        mainPartInst.setCode(partCode);
        mainPartInst.setQuantity(qty);
        req.setMainPartInst(mainPartInst);

        // 处理预定义参数（复用公共方法）
        if (paraCodeValuePairs.length > 0) {
            req.setPreParaInsts(buildParaInstsFromPairs(paraCodeValuePairs));
        }

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        log.info("推理结果: {}", result);
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
        req.setEnumerateAllSolution(true);

        // 复用公共方法处理参数
        req.setPreParaInsts(buildParaInstsFromPairs(paraCodeValuePairs));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        log.info("推理结果: {}", result);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 根据单个参数值进行推理（向后兼容）
     * 
     * @param paraCode 参数代码
     * @param value    参数值
     * @return 推理结果
     */
    protected List<ModuleInst> inferParasByPara(String paraCode, String value) {
        return inferParasByPara(new String[] { paraCode, value });
    }

    /**
     * 获取指定索引的解决方案
     */
    protected ProgammableInstAssert solutions(int index) {
        if (getSolutions() == null || index >= getSolutions().size()) {
            throw new IndexOutOfBoundsException("解决方案索引超出范围: " + index);
        }
        ModuleInst solution = getSolutions().get(index);
        return new ProgammableInstAssert(solution, getModule());
    }

    /**
     * 打印所有解决方案
     */
    protected void printSolutionsDetail() {
        if (getSolutions() == null || getSolutions().isEmpty()) {
            log.info("没有找到解决方案");
            return;
        }

        log.info("找到 {} 个解决方案:\n", getSolutions().size());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < getSolutions().size(); i++) {
            sb.append(String.format("\n解决方案%d: %s", i, ModuleConstraintExecutorImpl.toJson(getSolutions().get(i))));
        }
        log.info(sb.toString());
    }

    /**
     * 获取结果断言对象，用于验证执行结果
     */
    protected ResultAssert resultAssert() {
        if (getResult() == null) {
            throw new IllegalStateException("尚未执行推理，无法获取结果断言");
        }
        return new ResultAssert(getResult());
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
            throw new AssertionError("没有解决方案可供验证");
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

        log.info("解决方案数量验证通过，条件: {}，数量: {}", conditionExpr, actualMatchSolutionNum);
    }

    /**
     * 解析条件表达式
     * 格式："Color:Red,TShirt1:2" -> {"Color":"Red", "TShirt1":"2"}
     */
    private Map<String, String> parseConditionExpr(String conditionExpr) {
        Map<String, String> kvMap = new HashMap<>();
        String[] pairs = conditionExpr.split(",");

        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                kvMap.put(kv[0].trim(), kv[1].trim());
            } else {
                throw new IllegalArgumentException("条件表达式格式错误: " + pair);
            }
        }

        return kvMap;
    }

    /**
     * 构建条件元素列表
     */
    private List<ConditionElement> buildConditionElements(Map<String, String> kvMap) {
        List<ConditionElement> elements = new ArrayList<>();

        for (Map.Entry<String, String> entry : kvMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 在module.paras中查找
            java.util.Optional<Para> paraOpt = getModule().getPara(key);
            if (paraOpt.isPresent()) {
                Para para = paraOpt.get();
                // 找到Para，进一步根据value在option中查找
                String codeIdValue = ParaTypeHandler.getCodeIdValue(para, value);
                elements.add(new ConditionElement("para", key, codeIdValue));
                continue;
            }

            // 在module.parts中查找
            java.util.Optional<Part> partOpt = getModule().getPart(key);
            if (partOpt.isPresent()) {
                Part part = partOpt.get();
                // 找到Part
                elements.add(new ConditionElement("part", key, value));
                continue;
            }

            // 都找不到，报错
            throw new AlgLoaderException(String.format(
                    "Parameter or part not found in Module: %s", key));
        }

        return elements;
    }

    /**
     * 计算匹配条件的解决方案数量
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
    private List<ParaInst> buildParaInstsFromPairs(String... paraCodeValuePairs) {
        if (paraCodeValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("参数必须是偶数个，格式：paraCode1, value1, paraCode2, value2, ...");
        }

        List<ParaInst> paraInsts = new ArrayList<>();

        for (int i = 0; i < paraCodeValuePairs.length; i += 2) {
            String paraCode = paraCodeValuePairs[i];
            String value = paraCodeValuePairs[i + 1];

            ParaInst paraInst = new ParaInst();
            paraInst.setCode(paraCode);

            // 根据module.paras中的para.options，找到value对应的option
            java.util.Optional<Para> paraOpt = getModule().getPara(paraCode);
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
     * @param module    模块
     * @param solutions 解列表
     */
    protected void printSolutions() {
        printSolutions(getModule(), getSolutions());
    }

    /**
     * 打印短格式解信息
     * 
     * @param module    模块
     * @param solutions 解列表
     */
    protected void printSolutions(Module module, List<ModuleInst> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            log.info("No solutions found");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append("******************************************")
                .append(System.lineSeparator());
        // 打印缩语解释
        sb.append("Abbreviation explanation:").append(System.lineSeparator());

        // 1. shortCodes(P1:Size, P2:Color,PT1:part1)
        sb.append("1.").append(module.getProgObjShortCodeMemo()).append(System.lineSeparator());
        // 2. Attrs(V:value, H:isHidden, Q:qty)
        sb.append("2. Attrs(V:value, H:isHidden, Q:qty)").append(System.lineSeparator());
        // 3. Other Variable shortName
        sb.append("3. Other Variable shortName:").append(System.lineSeparator());
        sb.append("Solutions:").append(System.lineSeparator());
        if (!solutions.isEmpty()) {
            ModuleInst firstSolution = solutions.get(0);
            Object otherVarsMemo = firstSolution.getExtAttrs().get(ModuleInst.OTHER_VARIABLES_MEMO_KEY);
            if (otherVarsMemo instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, OtherVar> otherVarMap = (Map<String, OtherVar>) otherVarsMemo;
                for (Map.Entry<String, OtherVar> entry : otherVarMap.entrySet()) {
                    OtherVar otherVar = entry.getValue();
                    sb.append(" ").append(otherVar.getShortCode()).append(":").append(otherVar.getCode())
                            .append(System.lineSeparator());
                }
            }
        }

        sb.append(System.lineSeparator());

        // 打印解
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("S_").append(i + 1).append(": ").append(solutions.get(i).toShortString())
                    .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator()).append("******************************************")
                .append(System.lineSeparator());
        log.info(sb.toString());
    }

    // getResourcePath和createDirectory方法已移动到CommHelper类中
}