package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.util.ParaTypeHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

import org.junit.Before;

/**
 * 模块场景测试基类
 */
@Slf4j
public abstract class ModuleSecnarioTestBase {
    
    // 核心属性
    protected Class<? extends ConstraintAlgImpl> constraintAlgClazz;
    protected String tempResourcePath = "";
    protected Module module;
    protected ModuleConstraintExecutor.ConstraintConfig cfg;
    protected ModuleConstraintExecutorImpl exec;
    protected List<ModuleConstraintExecutor.ModuleInst> solutions;
    protected ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> result;
    
    /**
     * 构造函数
     */
    public ModuleSecnarioTestBase(Class<? extends ConstraintAlgImpl> constraintAlgClazz) {
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
        tempResourcePath = CommHelper.createTempPath(constraintAlgClazz);
        exec = new ModuleConstraintExecutorImpl();
        cfg = new ModuleConstraintExecutor.ConstraintConfig();
        cfg.isAttachedDebug = true; // 测试环境直接使用当前classpath加载
        cfg.rootFilePath = tempResourcePath;
        cfg.logFilePath = tempResourcePath;
        cfg.isLogModelProto = true;
        exec.init(cfg);
        
        module = buildModule(this.constraintAlgClazz);
        exec.addModule(module.getId(), module);
    }
    
    /**
     * 执行参数推理
     * @param partCode 部件代码
     * @param qty 数量
     * @param paraCodeValuePairs 参数代码和值的交替数组，格式：paraCode1, value1, paraCode2, value2, ...
     * @return 推理结果
     */
    protected List<ModuleConstraintExecutor.ModuleInst> inferParas(String partCode, Integer qty, String... paraCodeValuePairs) {
        ModuleConstraintExecutor.InferParasReq req = new ModuleConstraintExecutor.InferParasReq();
        req.moduleId = module.getId();
        req.enumerateAllSolution = true;
        
        // 设置主部件实例
        req.mainPartInst = new ModuleConstraintExecutor.PartInst();
        req.mainPartInst.code = partCode;
        req.mainPartInst.quantity = qty;
        
        // 处理预定义参数（复用公共方法）
        if (paraCodeValuePairs.length > 0) {
            req.preParaInsts = buildParaInstsFromPairs(paraCodeValuePairs);
        }
        
        ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> result = exec.inferParas(req);
        log.info("推理结果: {}", result);
        this.result = result;
        this.solutions = result.data;
        return solutions;
    }
    
    /**
     * 根据多个参数值进行推理（可变参数版本）
     * @param paraCodeValuePairs 参数代码和值的交替数组，格式：paraCode1, value1, paraCode2, value2, ...
     * @return 推理结果
     */
    protected List<ModuleConstraintExecutor.ModuleInst> inferParasByPara(String... paraCodeValuePairs) {
        ModuleConstraintExecutor.InferParasReq req = new ModuleConstraintExecutor.InferParasReq();
        req.moduleId = module.getId();
        req.enumerateAllSolution = true;
        
        // 复用公共方法处理参数
        req.preParaInsts = buildParaInstsFromPairs(paraCodeValuePairs);

        ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> result = exec.inferParas(req);
        log.info("推理结果: {}", result);
        this.result = result;
        this.solutions = result.data;
        return solutions;
    }
    
    /**
     * 根据单个参数值进行推理（向后兼容）
     * @param paraCode 参数代码
     * @param value 参数值
     * @return 推理结果
     */
    protected List<ModuleConstraintExecutor.ModuleInst> inferParasByPara(String paraCode, String value) {
        return inferParasByPara(new String[]{paraCode, value});
    }
    
    /**
     * 获取指定索引的解决方案
     */
    protected ProgammableInstAssert solutions(int index) {
        if (solutions == null || index >= solutions.size()) {
            throw new IndexOutOfBoundsException("解决方案索引超出范围: " + index);
        }
        ModuleConstraintExecutor.ModuleInst solution = solutions.get(index);
        return new ProgammableInstAssert(solution, module);
    }
    
    /**
     * 打印所有解决方案
     */
    protected void printSolutions() {
        if (solutions == null || solutions.isEmpty()) {
            log.info("没有找到解决方案");
            return;
        }
        
        log.info("找到 {} 个解决方案:\n", solutions.size());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < solutions.size(); i++) {
            sb.append(String.format("\n解决方案%d: %s", i, ModuleConstraintExecutorImpl.toJson(solutions.get(i))));
        }
        log.info(sb.toString());
    }
    
    /**
     * 获取结果断言对象，用于验证执行结果
     */
    protected ResultAssert resultAssert() {
        if (result == null) {
            throw new IllegalStateException("尚未执行推理，无法获取结果断言");
        }
        return new ResultAssert(result);
    }
    
    /**
     * 校验满足条件conditionExpr解的个数是否等于expectSolutionNum
     * 
     * @param conditionExpr 条件表达式，格式如："Color:Red,TShirt1:2"
     * @param expectSolutionNum 期望的解决方案数量
     */
    protected void assertSolutionNum(String conditionExpr, int expectSolutionNum) {
        //如果expectSolutionNum为0，则不进行验证
        if (expectSolutionNum == 0 && (solutions == null || solutions.isEmpty())) {
            return;
        }
        if (solutions == null || solutions.isEmpty()) {
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
            Para para = module.getPara(key);
            if (para != null) {
                // 找到Para，进一步根据value在option中查找
                String codeIdValue = ParaTypeHandler.getCodeIdValue(para, value);
                elements.add(new ConditionElement("para", key, codeIdValue));
                continue;
            }
            
            // 在module.parts中查找
            Part part = module.getPart(key);
            if (part != null) {
                // 找到Part
                elements.add(new ConditionElement("part", key, value));
                continue;
            }
            
            // 都找不到，报错
            throw new RuntimeException(String.format(
                "在Module中未找到参数或部件: %s", key));
        }
        
        return elements;
    }
    
    /**
     * 计算匹配条件的解决方案数量
     */
    private int countMatchingSolutions(List<ConditionElement> conditionElements) {
        int matchCount = 0;
        
        for (ModuleConstraintExecutor.ModuleInst solution : solutions) {
            boolean isMatch = true;
            
            for (ConditionElement element : conditionElements) {
                if ("para".equals(element.type)) {
                    // 对paraInst，比较value和paraInst.value
                    ParaInst paraInst = findParaInstByCode(solution, element.code);
                    if (paraInst == null || !element.value.equals(paraInst.value)) {
                        isMatch = false;
                        break;
                    }
                } else if ("part".equals(element.type)) {
                    // 对partInst，比较value和partInst.quantity
                    PartInst partInst = findPartInstByCode(solution, element.code);
                    if (partInst == null || !element.value.equals(String.valueOf(partInst.quantity))) {
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
    private ParaInst findParaInstByCode(ModuleConstraintExecutor.ModuleInst solution, String code) {
        if (solution.paras == null) return null;
        return solution.paras.stream()
            .filter(p -> code.equals(p.code))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据代码查找PartInst
     */
    private PartInst findPartInstByCode(ModuleConstraintExecutor.ModuleInst solution, String code) {
        if (solution.parts == null) return null;
        return solution.parts.stream()
            .filter(p -> code.equals(p.code))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 根据参数代码值对构建ParaInst列表（公共方法）
     * @param paraCodeValuePairs 参数代码和值的交替数组
     * @return ParaInst列表
     */
    private List<ModuleConstraintExecutor.ParaInst> buildParaInstsFromPairs(String... paraCodeValuePairs) {
        if (paraCodeValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("参数必须是偶数个，格式：paraCode1, value1, paraCode2, value2, ...");
        }
        
        List<ModuleConstraintExecutor.ParaInst> paraInsts = new ArrayList<>();
        
        for (int i = 0; i < paraCodeValuePairs.length; i += 2) {
            String paraCode = paraCodeValuePairs[i];
            String value = paraCodeValuePairs[i + 1];
            
            ModuleConstraintExecutor.ParaInst paraInst = new ModuleConstraintExecutor.ParaInst();
            paraInst.code = paraCode;
            
            //根据module.paras中的para.options，找到value对应的option
            Para para = module.getPara(paraCode);
            if (para == null) {
                throw new RuntimeException(String.format("参数 %s 不存在", paraCode));
            }
            paraInst.value = ParaTypeHandler.getCodeIdValue(para, value);
            paraInsts.add(paraInst);
        }
        
        return paraInsts;
    }
    
    /**
     * 条件元素内部类
     */
    private static class ConditionElement {
        final String type;    // "para" 或 "part"
        final String code;    // 代码
        final String value;   // 值
        
        ConditionElement(String type, String code, String value) {
            this.type = type;
            this.code = code;
            this.value = value;
        }
    }
    
    // getResourcePath和createDirectory方法已移动到CommHelper类中
} 