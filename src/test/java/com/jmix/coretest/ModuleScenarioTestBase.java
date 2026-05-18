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
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferParasReq;
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
 * 妯″潡鍦烘櫙娴嬭瘯鍩虹被
 * 
 * @since 2025-09-22
 */
@Slf4j
@Data
public abstract class ModuleScenarioTestBase {

    /**
     * 绾︽潫绠楁硶绫?
     */
    private Class<?> constraintAlgClazz;

    /**
     * 涓存椂璧勬簮璺緞
     */
    private String tempResourcePath = "";

    /**
     * 妯″潡
     */
    private Module module;

    /**
     * 閰嶇疆
     */
    protected ConstraintConfig cfg;

    /**
     * 瑙ｅ喅鏂规鍒楄〃
     */
    private List<ModuleInst> solutions = new ArrayList<>();

    /**
     * 鎺ㄧ悊缁撴灉
     */
    private Result<List<ModuleInst>> result;

    /**
     * 鏄惁鏋氫妇鎵€鏈夎В鍐虫柟妗?
     */
    private boolean enumerateAllSolution = true;

    /**
     * 鏋勯€犲嚱鏁?
     * 
     * @param constraintAlgClazz 绾︽潫绠楁硶绫?
     */
    public ModuleScenarioTestBase(Class<?> constraintAlgClazz) {
        this.constraintAlgClazz = constraintAlgClazz;
    }

    /**
     * 姣忎釜鐢ㄤ緥鎵ц鍓嶈皟鐢?
     */
    @BeforeEach
    public void setUp() {
        init();
    }

    /**
     * 姣忎釜鐢ㄤ緥鎵ц鍚庤皟鐢?
     */
    @AfterEach
    public void tearDown() {
        ModuleConstraintExecutor.INST.fini();
    }

    /**
     * 鏋勫缓Module鏁版嵁
     * 
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     * @return 鏋勫缓鐨勬ā鍧?
     */
    protected Module buildModule(Class<?> moduleAlgClazz) {
        // 閫氳繃娉ㄨВ鐢熸垚Module
        module = ModuleGenneratorByAnno.build(moduleAlgClazz, tempResourcePath);
        return module;
    }

    /**
     * 鍒濆鍖栨祴璇曠幆澧冿紙鎵撳寘妯″紡锛?
     * 
     */
    protected void init() {
        // 鐢熸垚涓存椂璧勬簮璺緞
        setTempResourcePath(CommHelper.createTempPath(getConstraintAlgClazz()));

        // 浣跨敤鍗曚緥璁块棶
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true); // 娴嬭瘯鐜鐩存帴浣跨敤褰撳墠classpath鍔犺浇
        // config.setRootFilePath(getTempResourcePath());
        config.setLogFilePath(getTempResourcePath());
        config.setLogModelProto(true);
        beforeInitConfig(config);
        String rootFilePath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "cproot";
        config.setRootFilePath(rootFilePath);
        ModuleConstraintExecutor.INST.init(config);
        setCfg(config);

        if (getCfg().isAttachedDebug()) {
            // 璋冭瘯妯″紡锛氱洿鎺ュ姞杞絚lass锛屽拰鐜版湁娴佺▼涓€鏍?
            Module tempModule = buildModule(getConstraintAlgClazz());
            Result<Void> addModuleResult = ModuleConstraintExecutor.INST.addModule(tempModule.getId(), tempModule);
            assertEquals(Result.SUCCESS, addModuleResult.getCode(),
                    "Add module failed: " + addModuleResult.getMessage());
            setModule(tempModule);
        } else {
            // 鎵撳寘妯″紡
            Module tempModule = ModuleGenneratorByAnno.buildModule(getConstraintAlgClazz());

            // 璋冪敤ModulePacker.pack杩涜鎵撳寘
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
     * 鍒濆鍖栭厤缃?
     * 
     * @param cfg 閰嶇疆
     */
    protected void beforeInitConfig(ConstraintConfig cfg) {

    }

    /**
     * 鎵ц鍙傛暟鎺ㄧ悊
     * 
     * @param partCode           閮ㄤ欢浠ｇ爜
     * @param qty                鏁伴噺
     * @param paraCodeValuePairs 鍙傛暟浠ｇ爜鍜屽€肩殑浜ゆ浛鏁扮粍锛屾牸寮忥細paraCode1, value1, paraCode2,
     *                           value2, ...
     * @return 鎺ㄧ悊缁撴灉
     */
    protected List<ModuleInst> inferParas(String partCode, Integer qty, String... paraCodeValuePairs) {
        return inferParas(partCode, qty, toPreParas(paraCodeValuePairs), new ArrayList<>());
    }

    /**
     * 鎵ц鍙傛暟鎺ㄧ悊
     * s
     * 
     * @param partCode 閮ㄤ欢浠ｇ爜
     * @param qty      鏁伴噺
     * @param preParas 鍓嶇疆鍙傛暟鍒楄〃
     * @param preParts 鍓嶇疆閮ㄤ欢鍒楄〃
     * @return 鎺ㄧ悊缁撴灉
     */
    protected List<ModuleInst> inferParas(String partCode, Integer qty, List<ParaInst> preParas,
            List<PartInst> preParts) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());

        // 璁剧疆涓婚儴浠跺疄渚?
        PartInst mainPartInst = new PartInst();
        mainPartInst.setCode(partCode);
        mainPartInst.setQuantity(qty);
        req.setMainPartInst(mainPartInst);

        // 澶勭悊棰勫畾涔夊弬鏁帮紙澶嶇敤鍏叡鏂规硶锛?
        req.setPreParaInsts(preParas);
        req.setPrePartInsts(preParts);

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        log.info("Inference result: {}", result);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 鏍规嵁澶氫釜鍙傛暟鍊艰繘琛屾帹鐞嗭紙鍙彉鍙傛暟鐗堟湰锛?
     *
     * @param paraCodeValuePairs 鍙傛暟浠ｇ爜鍜屽€肩殑浜ゆ浛鏁扮粍锛屾牸寮忥細paraCode1, value1, paraCode2,
     *                           value2, ...
     * @return 鎺ㄧ悊缁撴灉
     */
    protected List<ModuleInst> inferParasByPara(String... paraCodeValuePairs) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());

        // 澶嶇敤鍏叡鏂规硶澶勭悊鍙傛暟
        req.setPreParaInsts(toPreParas(paraCodeValuePairs));

        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);
        setResult(result);
        setSolutions(result.getData());
        return getSolutions();
    }

    /**
     * 浣跨敤 relaxSolve=true 杩涜鍙傛暟鎺ㄧ悊锛堝啿绐佽瘖鏂ā寮忥級銆?
     *
     * @param paraCodeValuePairs 鍙傛暟浠ｇ爜鍜屽€肩殑浜ゆ浛鏁扮粍
     * @return 鎺ㄧ悊缁撴灉锛堥儴鍒嗚В锛?
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
     * 鏍￠獙閮ㄥ垎瑙ｄ腑鍖呭惈鏈熸湜鐨勫瓧绗︿覆锛堜粠 SolverResult.solutions 涓煡鎵撅級銆?
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
     * 鑾峰彇鎸囧畾绱㈠紩鐨勮В鍐虫柟妗?
     */
    protected ProgammableInstAssert solutions(int index) {
        if (getSolutions() == null || index >= getSolutions().size()) {
            throw new IndexOutOfBoundsException("Solution index out of bounds: " + index);
        }
        ModuleInst solution = getSolutions().get(index);
        return new ProgammableInstAssert(solution, getModule());
    }

    /**
     * 鎵撳嵃鎵€鏈夎В鍐虫柟妗?
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
     * 鑾峰彇缁撴灉鏂█瀵硅薄锛岀敤浜庨獙璇佹墽琛岀粨鏋?
     */
    protected ResultAssert resultAssert() {
        if (getResult() == null) {
            throw new IllegalStateException("Inference not executed yet, cannot get result assertion");
        }
        return new ResultAssert(getResult());
    }

    /**
     * 妫€鏌ヨВ鍐虫柟妗堝垪琛ㄤ腑鏄惁鍖呭惈鎸囧畾鐨勫瓧绗︿覆
     * 閬嶅巻鎵€鏈夎В鍐虫柟妗堢殑 toShortString 杈撳嚭锛屾鏌ユ槸鍚﹀寘鍚寚瀹氱殑瀛愬瓧绗︿覆
     * 濡傛灉鎵句笉鍒板尮閰嶇殑瑙ｅ喅鏂规锛屽垯鎶涘嚭 AssertionError
     * 
     * @param expectedStr 鏈熸湜鍖呭惈鐨勫瓧绗︿覆
     * @throws AssertionError 濡傛灉鎵句笉鍒板寘鍚寚瀹氬瓧绗︿覆鐨勮В鍐虫柟妗?
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
        // 鏋勫缓閿欒娑堟伅锛屽寘鍚墍鏈夎В鍐虫柟妗堢殑瀛楃涓茶〃绀?
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
     * 鏍￠獙婊¤冻鏉′欢conditionExpr瑙ｇ殑涓暟鏄惁绛変簬expectSolutionNum
     * 
     * @param conditionExpr     鏉′欢琛ㄨ揪寮忥紝鏍煎紡濡傦細"Color:Red,TShirt1:2"
     * @param expectSolutionNum 鏈熸湜鐨勮В鍐虫柟妗堟暟閲?
     */
    protected void assertSolutionNum(String conditionExpr, int expectSolutionNum) {
        // 濡傛灉expectSolutionNum涓?锛屽垯涓嶈繘琛岄獙璇?
        if (expectSolutionNum == 0 && (getSolutions() == null || getSolutions().isEmpty())) {
            return;
        }
        if (getSolutions() == null || getSolutions().isEmpty()) {
            throw new AssertionError("No solutions available for verification");
        }
        // 瑙ｆ瀽鏉′欢琛ㄨ揪寮?
        Map<String, String> kvMap = parseConditionExpr(conditionExpr);

        // 鏋勫缓鏉′欢瀵硅薄鍒楄〃
        List<ConditionElement> conditionElements = buildConditionElements(kvMap);

        // 璁＄畻瀹為檯鍖归厤鐨勮В鍐虫柟妗堟暟閲?
        int actualMatchSolutionNum = countMatchingSolutions(conditionElements);

        // 姣旇緝瀹為檯鏁伴噺鍜屾湡鏈涙暟閲?
        if (actualMatchSolutionNum != expectSolutionNum) {
            throw new AssertionError(String.format(
                    "瑙ｅ喅鏂规鏁伴噺涓嶅尮閰嶏紝鏈熸湜: %d锛屽疄闄? %d锛屾潯浠? %s",
                    expectSolutionNum, actualMatchSolutionNum, conditionExpr));
        }

        log.info("Solution count verification passed, condition: {}, count: {}", conditionExpr, actualMatchSolutionNum);
    }

    /**
     * 瑙ｆ瀽鏉′欢琛ㄨ揪寮?
     * 鏍煎紡锛?Color:Red,TShirt1:2" -> {"Color":"Red", "TShirt1":"2"}
     * 
     * @param conditionExpr 鏉′欢琛ㄨ揪寮忓瓧绗︿覆
     * @return 瑙ｆ瀽鍚庣殑閿€煎鏄犲皠
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
     * 鏋勫缓鏉′欢鍏冪礌鍒楄〃
     * 
     * @param kvMap 閿€煎鏄犲皠
     * @return 鏉′欢鍏冪礌鍒楄〃
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
     * 鍒涘缓鏉′欢鍏冪礌
     * 
     * @param key   閿?
     * @param value 鍊?
     * @return 鏉′欢鍏冪礌
     * @throws AlgLoaderException 褰撴壘涓嶅埌鍙傛暟鎴栭儴浠舵椂
     */
    private ConditionElement createConditionElement(String key, String value) {
        // 鍦╩odule.paras涓煡鎵?
        java.util.Optional<Para> paraOpt = getModule().getPara(key);
        if (paraOpt.isPresent()) {
            return createParaConditionElement(key, value, paraOpt.get());
        }

        // 鍦╩odule.parts涓煡鎵?
        IPart partOpt = getModule().getPart(key);
        if (partOpt != null) {
            return createPartConditionElement(key, value);
        }

        // 閮芥壘涓嶅埌锛屾姤閿?
        throw new AlgLoaderException(String.format(
                "Parameter or part not found in Module: %s", key));
    }

    /**
     * 鍒涘缓鍙傛暟鏉′欢鍏冪礌
     * 
     * @param key   閿?
     * @param value 鍊?
     * @param para  鍙傛暟瀵硅薄
     * @return 鏉′欢鍏冪礌
     */
    private ConditionElement createParaConditionElement(String key, String value, Para para) {
        String codeIdValue = ParaTypeHandler.getCodeIdValue(para, value);
        return new ConditionElement("para", key, codeIdValue);
    }

    /**
     * 鍒涘缓閮ㄤ欢鏉′欢鍏冪礌
     * 
     * @param key   閿?
     * @param value 鍊?
     * @return 鏉′欢鍏冪礌
     */
    private ConditionElement createPartConditionElement(String key, String value) {
        return new ConditionElement("part", key, value);
    }

    /**
     * 璁＄畻鍖归厤鏉′欢鐨勮В鍐虫柟妗堟暟閲?
     * 
     * @param conditionElements 鏉′欢鍏冪礌鍒楄〃
     * @return 鍖归厤鐨勮В鍐虫柟妗堟暟閲?
     */
    private int countMatchingSolutions(List<ConditionElement> conditionElements) {
        int matchCount = 0;

        for (ModuleInst solution : getSolutions()) {
            boolean isMatch = true;

            for (ConditionElement element : conditionElements) {
                if ("para".equals(element.type)) {
                    // 瀵筽araInst锛屾瘮杈僾alue鍜宲araInst.value
                    ParaInst paraInst = findParaInstByCode(solution, element.code);
                    if (paraInst == null || !element.value.equals(paraInst.getValue())) {
                        isMatch = false;
                        break;
                    }
                } else if ("part".equals(element.type)) {
                    // 瀵筽artInst锛屾瘮杈僾alue鍜宲artInst.quantity
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
     * 鏍规嵁浠ｇ爜鏌ユ壘ParaInst
     * 
     * @param solution 妯″潡瀹炰緥
     * @param code     鍙傛暟浠ｇ爜
     * @return 鍖归厤鐨凱araInst锛屽鏋滄湭鎵惧埌鍒欒繑鍥瀗ull
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
     * 鏍规嵁浠ｇ爜鏌ユ壘PartInst
     * 
     * @param solution 妯″潡瀹炰緥
     * @param code     閮ㄤ欢浠ｇ爜
     * @return 鍖归厤鐨凱artInst锛屽鏋滄湭鎵惧埌鍒欒繑鍥瀗ull
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
     * 鏍规嵁鍙傛暟浠ｇ爜鍊煎鏋勫缓ParaInst鍒楄〃锛堝叕鍏辨柟娉曪級
     * 
     * @param paraCodeValuePairs 鍙傛暟浠ｇ爜鍜屽€肩殑浜ゆ浛鏁扮粍
     * @return ParaInst鍒楄〃
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

            // 鏍规嵁module.paras涓殑para.options锛屾壘鍒皏alue瀵瑰簲鐨刼ption
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
     * 鏍规嵁Part浠ｇ爜鍊煎鏋勫缓PartInst鍒楄〃锛堝叕鍏辨柟娉曪級
     * 
     * @param partCodeValuePairs Part浠ｇ爜鍜屽€肩殑浜ゆ浛鏁扮粍
     * @return ParaInst鍒楄〃
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

            // 鏍规嵁module.parts涓?
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
     * 鏉′欢鍏冪礌鍐呴儴绫?
     */
    private static class ConditionElement {

        private final String type; // "para" 鎴?"part"

        private final String code; // 浠ｇ爜

        private final String value; // 鍊?

        ConditionElement(String type, String code, String value) {
            this.type = type;
            this.code = code;
            this.value = value;
        }
    }

    /**
     * 鎵撳嵃鐭牸寮忚В淇℃伅
     * 
     */
    protected void printSolutions() {
        printSolutions(getModule(), getSolutions(), false);
    }

    protected void printSimpleSolutions() {
        printSolutions(getModule(), getSolutions(), true);
    }

    /**
     * 鎵撳嵃鐭牸寮忚В淇℃伅
     * 
     * @param module    妯″潡
     * @param solutions 瑙ｅ垪琛?
     */
    protected void printSolutions(Module module, List<ModuleInst> solutions, boolean isSimple) {
        SolutionUtils.printSolutions(module, solutions, isSimple, result);
    }

    /**
     * 灏嗗瓧绗︿覆绾︽潫璇锋眰杞崲涓篜artConstraintReq鍒楄〃
     *
     * @param partCategory 閮ㄤ欢绫诲埆
     * @param strReqs      瀛楃涓茬害鏉熻姹傛暟缁勶紝鏍煎紡濡傦細"sd:Sum_Quantity ==2 where Speed=5400"
     * @return PartConstraintReq鍒楄〃
     */
    protected List<PartConstraintReq> toPartConstraintReqs(String partCategory, String... strReqs) {
        List<PartConstraintReq> reqs = new ArrayList<>();

        for (String strReq : strReqs) {
            PartConstraintReq req = new PartConstraintReq();

            // Parse strategy syntax: [strategy=ASCENDING:price]
            String remainingStr = parseStrategyConfig(strReq, req);

            // 瑙ｆ瀽鏍煎紡锛歳eqPartCategory:attrCode comparator value where condition
            // 绀轰緥锛?sd:Sum_Quantity ==2 where Speed=5400"
            String[] categoryParts = remainingStr.split(":", 2);
            String reqPartCategory;
            String attrExprStr;
            if (categoryParts.length == 2) {
                reqPartCategory = categoryParts[0].trim();
                attrExprStr = categoryParts[1].trim();
            } else {
                // 濡傛灉娌℃湁:锛屽垯浣跨敤榛樿鐨刾artCategory
                reqPartCategory = partCategory;
                attrExprStr = remainingStr;
            }
            req.setPartCategoryCode(reqPartCategory);

            // 瑙ｆ瀽灞炴€ц〃杈惧紡锛氭敮鎸佷笁绉嶅満鏅?
            // 鍦烘櫙1锛氫粎鏈夎繃婊ゆ潯浠讹紝濡?"where Speed=5400"
            // 鍦烘櫙2锛氫粎鏈夋眹鎬绘潯浠讹紝濡?"Sum_Capacity >=5"
            // 鍦烘櫙3锛氬悓鏃舵湁姹囨€绘潯浠跺拰杩囨护鏉′欢锛屽 "Sum_Capacity >=5 where Speed=5400"
            parseAttrExpr(attrExprStr, req);

            reqs.add(req);
        }

        return reqs;
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
     * 瑙ｆ瀽灞炴€ц〃杈惧紡涓篜artConstraintReq
     * 鏀寔涓夌鍦烘櫙锛?
     * 鍦烘櫙1锛氫粎鏈夎繃婊ゆ潯浠讹紝濡?"where Speed=5400" -> attrWhereCondition="Speed=5400", attrComparator=null
     * 鍦烘櫙2锛氫粎鏈夋眹鎬绘潯浠讹紝濡?"Sum_Capacity >=5" -> attrWhereCondition=null, attrType=Sum, attrCode=Capacity, attrComparator=">=", attrValue="5"
     * 鍦烘櫙3锛氬悓鏃舵湁姹囨€绘潯浠跺拰杩囨护鏉′欢锛屽 "Sum_Capacity >=5 where Speed=5400" -> 鍚屾椂璁剧疆涓や釜鏉′欢
     *
     * @param attrExpr 灞炴€ц〃杈惧紡锛堝彲鑳藉寘鍚?where 瀛愬彞锛?
     * @param req 閮ㄤ欢绾︽潫璇锋眰瀵硅薄锛岀敤浜庤缃В鏋愮粨鏋?
     */
    protected void parseAttrExpr(String attrExpr, PartConstraintReq req) {
        String trimmedExpr = attrExpr.trim();

        // 澶勭悊 where 瀛愬彞锛堣繃婊ゆ潯浠讹級
        // 鏀寔澶氱鏍煎紡锛?,where XXX", "where XXX", ", where XXX"
        int whereIndex = -1;
        String whereCondition = null;

        // 鍏堝皾璇?" where " (鍓嶅悗閮芥湁绌烘牸)
        whereIndex = trimmedExpr.indexOf(" where ");
        if (whereIndex >= 0) {
            whereCondition = trimmedExpr.substring(whereIndex + 7).trim();
            trimmedExpr = trimmedExpr.substring(0, whereIndex).trim();
        } else {
            // 鍐嶅皾璇?"where " (鍓嶉潰娌℃湁绌烘牸)
            whereIndex = trimmedExpr.indexOf("where ");
            if (whereIndex >= 0) {
                whereCondition = trimmedExpr.substring(whereIndex + 6).trim();
                trimmedExpr = trimmedExpr.substring(0, whereIndex).trim();
            }
        }

        if (whereCondition != null && !whereCondition.isEmpty()) {
            req.setAttrWhereCondition(whereCondition);
        }

        // 瑙ｆ瀽姹囨€绘潯浠堕儴鍒?
        if (trimmedExpr.isEmpty()) {
            // 鍦烘櫙1锛氫粎鏈夎繃婊ゆ潯浠讹紝娌℃湁姹囨€绘潯浠?
            return;
        }

        // 浣跨敤姝ｅ垯琛ㄨ揪寮忚В鏋愶細attrCode comparator value
        Pattern pattern = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|<=|>=|<|>)\\s*(\\d+)");
        Matcher matcher = pattern.matcher(trimmedExpr);

        if (matcher.matches()) {
            String mergedAttrCode = matcher.group(1).trim();
            String[] mergedParts = mergedAttrCode.split(AttrPara.CODE_SEPARATOR);
            if (mergedParts.length == 2) {
                req.setAttrType(AttrParaType.valueOf(mergedParts[0]));
                req.setAttrCode(mergedParts[1]);
            } else {
                throw new IllegalArgumentException("Invalid attribute expression format: " + trimmedExpr);
            }
            req.setAttrComparator(matcher.group(2).trim());
            req.setAttrValue(matcher.group(3).trim());
        } else {
            throw new IllegalArgumentException("Invalid attribute expression format: " + trimmedExpr);
        }
    }

    /**
     * 鍩轰簬閮ㄤ欢绾︽潫杩涜鎺ㄧ悊鎺ㄨ崘
     * 
     * @param constraintReqs 绾︽潫璇锋眰瀛楃涓叉暟缁?
     * @return 鎺ㄨ崘鐨勬ā鍧楀疄渚嬪垪琛?
     */
    protected List<ModuleInst> inferRecommendModule(String... constraintReqs) {
        return inferRecommend("", constraintReqs);
    }

    /**
     * 鍩轰簬閮ㄤ欢绾︽潫杩涜鎺ㄧ悊鎺ㄨ崘
     * 
     * @param partCategoryCode 閮ㄤ欢绫诲埆
     * @param constraintReqs   绾︽潫璇锋眰瀛楃涓叉暟缁?
     * @return 鎺ㄨ崘鐨勬ā鍧楀疄渚嬪垪琛?
     */
    protected List<ModuleInst> inferRecommend(String partCategoryCode, String... constraintReqs) {
        List<PartConstraintReq> partConstraintReqs = toPartConstraintReqs(partCategoryCode, constraintReqs);

        InferParasReq req = new InferParasReq();
        req.setModuleId(getModule().getId());
        req.setEnumerateAllSolution(isEnumerateAllSolution());
        req.setPartCategoryCode(partCategoryCode);
        req.setPartConstraintReqs(partConstraintReqs);

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
     * 姣旇緝Solution鐨勫€?
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
        // 棰勬湡缁撴灉锛歔"cpu2(Q:20,H:0,S:1)","md1(Q:5,H:0,S:1)","sd1(0*)"
        // 澶勭悊閫昏緫锛?鏍规嵁鈥?,鈥濇潵鎷嗗垎
        List<String> result = new ArrayList<>();
        String[] items = expectStr.split("\\),");
        for (String item : items) {
            result.add(item.trim());
        }
        return result;
    }

}
