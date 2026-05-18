package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.PartUtils;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Cardinality;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.impl.ModuleInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.StrategyConfig;
import com.jmix.executor.model.StrategyType;
import com.jmix.tool.bbuilder.MultiInstCategoryUtils;

import com.google.ortools.sat.DecisionStrategyProto;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 妯″潡绠楁硶瀹炵幇绫?
 * 瀹炵幇ConstraintAlg鎺ュ彛锛屾彁渚涚害鏉熸眰瑙ｇ殑鍏蜂綋瀹炵幇
 * 缁ф壙鑷狹oduleBaseAlgImpl锛屾敮鎸佹ā鍧楃骇鍒拰閮ㄤ欢鍒嗙被绾у埆鐨勬眰鍜屾搷浣?
 *
 * @since 2025-04-05
 */
@Slf4j
public class ModuleAlgImpl extends ModuleBaseAlgImpl implements IModuleAlg {

    /**
     * 閮ㄤ欢鍒嗙被绠楁硶瀹炰緥鏄犲皠琛?
     */
    protected Map<String, PartCategoryAlgImpl> partCategoryAlgs = new LinkedHashMap<>();

    // 宸﹁〃杈惧紡閮ㄤ欢鍒嗙被绠楁硶瀹炰緥锛屼粎閽堝oneMany瑙勫垯
    private SingleInstPartCategoryAlgImpl currentLeftPartCategoryAlgImpl = null;
    // 鍙宠〃杈惧紡閮ㄤ欢鍒嗙被绠楁硶瀹炰緥锛屼粎閽堝oneMany瑙勫垯
    private SingleInstPartCategoryAlgImpl currentRightPartCategoryAlgImpl = null;
    // 褰撳墠瑙勫垯鍚庣紑锛屼粎閽堝oneMany瑙勫垯
    private String currrentRulePostName = "";

    /**
     * 鍒濆鍖栨ā鍧楃畻娉曞疄渚?
     * 鎸塸artCategoryCode瀵筽artCategoryInputs杩涜鍒嗙粍锛岀劧鍚庡垵濮嬪寲鏈眰鍜屽瓙灞傜殑鍙橀噺涓庤鍒?
     * 閲嶅啓鍩虹被鏂规硶锛屾坊鍔燩artCategoryAlgImpl鐨勫垵濮嬪寲
     *
     * @param model              CP绾︽潫妯″瀷
     * @param module             妯″潡瀵硅薄
     * @param partCategoryInputs 鏉ヨ嚜璇锋眰鐨勯儴浠剁害鏉熷垪琛?
     */
    public void init(AlgCPModel model, IModule module,
            ModuleInput moduleInput) {
        Object moduleAlgFile = moduleAlgFile();
        initData(model, module, moduleInput, moduleAlgFile);

        afterInitData(this.module, moduleAlgFile);

        initInput();

        // preCalculate
        preCalculate();

        // apply decision strategies for ordering solution enumeration
        applyDecisionStrategies();

        // midCalculate
        initRules(moduleAlgFile, CalcStage.MID);
        // postCalculate
    }

    protected Object moduleAlgFile() {
        return this;
    }

    public Object ruleMethodOwner() {
        return moduleAlgFile();
    }

    // 閲嶅啓鐖剁被鐨勬柟娉?
    protected void executeRuleMethod(Rule rule, Object moduleAlgFile, Method method) {
        if (isOneManyRule(rule)) {
            executeRuleMethod4OneManyRule(rule, moduleAlgFile, method);
        } else {
            super.executeRuleMethod(rule, moduleAlgFile, method);
        }
    }

    private boolean isOneManyRule(Rule rule) {
        return rule.getLeftCardinality() == Cardinality.ONE && rule.getRightCardinality() == Cardinality.MANY;
    }

    // 鎵ц绫讳技logicAB1(1-*)鐨勭敤渚?
    private void executeRuleMethod4OneManyRule(Rule rule, Object moduleAlgFile, Method method) {
        String leftPartCategoryCode = rule.getLeftCategoryCode();
        String rightPartCategoryCode = rule.getRightCategoryCode();
        if (Strings.isEmpty(leftPartCategoryCode) || Strings.isEmpty(rightPartCategoryCode)) {
            log.error("leftPartCategoryCode or rightPartCategoryCode is null, cannot execute one many rule");
            throw new AlgLoaderException(
                    "leftPartCategoryCode or rightPartCategoryCode is null, cannot execute one many rule");
        }
        SingleInstPartCategoryAlgImpl leftPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) getPartCategoryAlg(
                leftPartCategoryCode);
        PartCategoryAlgImpl rightPartCategoryAlgImpl = getPartCategoryAlg(
                rightPartCategoryCode);
        if (leftPartCategoryAlgImpl == null || rightPartCategoryAlgImpl == null) {
            log.error("leftPartCategoryAlgImpl or rightPartCategoryAlgImpl is null, cannot execute one many rule");
            throw new AlgLoaderException(
                    "leftPartCategoryAlgImpl or rightPartCategoryAlgImpl is null, cannot execute one many rule");
        }
        if (rightPartCategoryAlgImpl instanceof MultiInstPartCategoryAlgImpl) {
            MultiInstPartCategoryAlgImpl multiInstPartCategoryAlgImpl = (MultiInstPartCategoryAlgImpl) rightPartCategoryAlgImpl;
            for (SingleInstPartCategoryAlgImpl rightPartCategoryAlgImplItem : multiInstPartCategoryAlgImpl
                    .getPartCategoryInsts()) {

                currentLeftPartCategoryAlgImpl = leftPartCategoryAlgImpl;
                currentRightPartCategoryAlgImpl = rightPartCategoryAlgImplItem;
                currrentRulePostName = currentLeftPartCategoryAlgImpl.getCategoryCode() + "_"
                        + currentRightPartCategoryAlgImpl.getCategoryCode() + ".I"
                        + currentRightPartCategoryAlgImpl.getInstId();
                super.executeRuleMethod(rule, moduleAlgFile, method);
                currentLeftPartCategoryAlgImpl = null;
                currentRightPartCategoryAlgImpl = null;
            }
        } else {
            currentLeftPartCategoryAlgImpl = leftPartCategoryAlgImpl;
            currentRightPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) rightPartCategoryAlgImpl;
            currrentRulePostName = currentLeftPartCategoryAlgImpl.getCategoryCode() + "_"
                    + currentRightPartCategoryAlgImpl.getCategoryCode() + ".I"
                    + currentRightPartCategoryAlgImpl.getInstId();
            super.executeRuleMethod(rule, moduleAlgFile, method);
            currentLeftPartCategoryAlgImpl = null;
            currentRightPartCategoryAlgImpl = null;
        }

    }

    private ModuleInput getModuleInput() {
        return (ModuleInput) this.moduleInput;
    }

    protected void initInput() {
        super.initInput(moduleAlgFile());

        // 鏈ā鍧楃殑鍒濆鍖?
        ModuleInput req = getModuleInput();
        if (req.getMainPartInst() != null) {
            this.addPartEquality(req.getMainPartInst().getCode(), req.getMainPartInst().getQuantity());
        }
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                this.addParaEquality(paraInst.getCode(), paraInst.getValue());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                this.addPartEquality(partInst.getCode(), partInst.getQuantity());
            }
        }

        // 鍒嗙被鐨刬nput鍒濆鍖?
        // 濡傛灉module鏈塒artCategorys锛屽垯瀵规瘡涓狿artCategory鍒涘缓骞跺垵濮嬪寲PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initInput(moduleAlgFile());
        }

    }

    /**
     * 鍒濆鍖朚odule鍚?
     * 
     * @param module
     * @param moduleAlg
     */
    protected void afterInitData(IModule module, Object moduleAlg) {

    }

    private void preCalculate() {
        log.info("preCalculate: module={}", getModule().getCode());
        // 鏆傛椂鏀寔妯″潡绾у墠缃绠楋紝涓昏鏄鎺у埗鍙橀噺杩涜鍒濆鍖?
        if (!(module instanceof Module)) {
            return;
        }
        initRules(moduleAlgFile(), CalcStage.PRE);
    }

    protected void initData(AlgCPModel model, IModule module,
            ModuleInput moduleInput,
            Object moduleAlgFile) {
        // 璋冪敤鍩虹被鍒濆鍖?
        super.initData(model, module, moduleInput, moduleAlgFile);

        // 鎸塸artCategoryCode瀵筽artCategoryInputs杩涜鍒嗙粍
        Map<String, PartCategoryInputBase> partConstraintFromReqMap = new LinkedHashMap<>();
        for (PartCategoryInputBase partCategoryInput : moduleInput.getPartCategoryInputs()) {
            partConstraintFromReqMap.put(partCategoryInput.getPartCategoryCode(), partCategoryInput);
        }

        // 濡傛灉module鏈塒artCategorys锛屽垯瀵规瘡涓狿artCategory鍒涘缓骞跺垵濮嬪寲PartCategoryAlgImpl
        if (module instanceof Module) {
            Module bModule = (Module) module;
            for (PartCategory partCategory : bModule.getPartCategorys()) {
                String categoryCode = partCategory.getCode();
                PartCategoryInputBase pc4partCategoryInput = partConstraintFromReqMap
                        .get(categoryCode);
                ModuleBaseAlgImpl pcAlg = null;
                if (pc4partCategoryInput == null) {
                    // No constraint for this PartCategory, create a default input
                    PartCategoryInput defaultInput = new PartCategoryInput();
                    defaultInput.setFilteredCategory(partCategory);
                    pc4partCategoryInput = defaultInput;
                }
                if (partCategory.isSupportMultiInst() && pc4partCategoryInput instanceof MultiInstPartCategoryInput) {
                    pcAlg = new MultiInstPartCategoryAlgImpl();
                    MultiInstPartCategoryInput multiInstPartCategoryInput = (MultiInstPartCategoryInput) pc4partCategoryInput;
                    pcAlg.initData(model, (IModule) multiInstPartCategoryInput, pc4partCategoryInput, moduleAlgFile);
                } else {
                    pcAlg = new SingleInstPartCategoryAlgImpl();
                    pcAlg.initData(model, (IModule) partCategory, pc4partCategoryInput, moduleAlgFile);
                }
                partCategoryAlgs.put(categoryCode, (PartCategoryAlgImpl) pcAlg);
            }
        }
        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    protected void initRules(Object moduleAlgFile, CalcStage calcStage) {
        Map<String, Method> allRuleMethods = buildAllRuleMethods(module, moduleAlgFile);
        // 鍏堟湰韬繖涓ā鍧楃殑鍓嶇疆绠楁硶

        // 鍏堟瀯寤烘湰妯″潡鐨勪紭鍏堢被瑙勫垯
        buildPriorityConstraint(module);

        // 濡傛灉module鏈塒artCategorys锛屽垯瀵规瘡涓狿artCategory鍒涘缓骞跺垵濮嬪寲PartCategoryAlgImpl
        for (PartCategoryAlgImpl partCategoryAlgImpl : this.getPartCategoryAlgs()) {
            partCategoryAlgImpl.initRules(allRuleMethods, moduleAlgFile, calcStage);
        }

        // 鎵ц鏈韩杩欎釜妯″潡鐨勫悗缃畻娉?
        this.initRules(allRuleMethods, moduleAlgFile, calcStage);
    }

    /**
     * 鏄惁鏈変紭鍏堢被瑙勫垯
     * 
     * @return exr
     */
    public boolean hasPriorityRule() {
        return !getAllPriorityConstraintMap().isEmpty();
    }

    private Map<String, PriorityConstraint> getAllPriorityConstraintMap() {
        // ruleCode -> PriorityConstraint
        Map<String, PriorityConstraint> priorityConstraints = new HashMap<>();
        if (getPriorityConstraint() != null) {
            priorityConstraints.put(getPriorityConstraint().getRule().getCode(), getPriorityConstraint());
        }
        for (PartCategoryAlgImpl partCategoryAlgImpl : getPartCategoryAlgs()) {
            PriorityConstraint priorityConstraint = ((ModuleBaseAlgImpl) partCategoryAlgImpl).getPriorityConstraint();
            if (priorityConstraint != null) {
                priorityConstraints.put(
                        priorityConstraint.getRule().getCode(), priorityConstraint);
            }
        }
        return priorityConstraints;
    }

    /**
     * 鑾峰彇鎵€鏈夌殑Priority鍚堝苟鍚庣殑琛ㄨ揪寮?
     *
     * @return exr
     */
    public PartAlgCPLinearExpr queryMergerPriorityConstraintExpr() {
        PartAlgCPLinearExpr mergedExpr = model.newPartLinearExpr("merged_priority_expr");
        for (PriorityConstraint pc : getAllPriorityConstraintMap().values()) {
            Rule rule = pc.getRule();
            PriorityRuleSchema schema = (PriorityRuleSchema) rule.getRawCode();
            PriorityStrategy strategy = schema.getPriorityStrategy();
            // int weight = schema.getWeight();
            int coff = strategy == PriorityStrategy.MAX ? -1 : 1;
            mergedExpr.addExpr(pc.getExpr(), coff);
        }
        return mergedExpr;
    }

    /**
     * Update or create priority objective function for given attribute code.
     *
     * @param attrCode attribute code
     * @param expr     expression containing part-term metadata and numeric terms
     */
    public void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr) {
        if (ruleCode == null || ruleCode.isEmpty()) {
            log.warn("ruleCode is null or empty, skip updating priority objective");
            return;
        }
        PriorityConstraint pConstraint = getAllPriorityConstraintMap().get(ruleCode);
        if (pConstraint == null) {
            log.error("PriorityConstraint not found for ruleCode: {}", ruleCode);
            throw new AlgLoaderException("PriorityConstraint not found for ruleCode: " + ruleCode);
        }
        pConstraint.setExpr(expr);
        log.info("Updated priority objective for ruleCode {}: expr={}", ruleCode,
                expr != null ? expr.toString() : "null");
    }

    /**
     * 鑾峰彇閮ㄤ欢鍒嗙被绠楁硶瀹炰緥
     *
     * @param categoryCode 閮ㄤ欢鍒嗙被浠ｇ爜
     * @return PartCategoryAlgImpl瀹炰緥
     */
    public PartCategoryAlgImpl getPartCategoryAlg(String categoryCode) {
        return (PartCategoryAlgImpl) partCategoryAlgs.get(categoryCode);
    }

    /**
     * 鑾峰彇閮ㄤ欢鍒嗙被绠楁硶瀹炰緥
     *
     * @param categoryCodeInstPrefix 閮ㄤ欢鍒嗙被浠ｇ爜鍓嶇紑
     * @return PartCategoryAlgImpl瀹炰緥
     */
    public List<PartCategoryAlgImpl> getPartCategoryAlgByInstPrefix(String categoryCodeInstPrefix) {
        String instPrefix = categoryCodeInstPrefix + String.valueOf(MultiInstCategoryUtils.INST_PREFIX_CHAR);
        List<PartCategoryAlgImpl> partCategoryAlgImpls = new ArrayList<>();
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgs.values()) {
            if (partCategoryAlgImpl.getCategoryCode().startsWith(instPrefix)) {
                partCategoryAlgImpls.add(partCategoryAlgImpl);
            }
        }
        return partCategoryAlgImpls;
    }

    /**
     * 鑾峰彇鎵€鏈夐儴浠跺垎绫荤畻娉曞疄渚嬪垪琛?
     * 
     * @return 閮ㄤ欢鍒嗙被绠楁硶瀹炰緥鍒楄〃
     */
    public List<PartCategoryAlgImpl> getPartCategoryAlgs() {
        return new ArrayList<>(partCategoryAlgs.values());
    }

    /**
     * 鑾峰彇Module瀵硅薄
     *
     * @return Module瀵硅薄
     */
    public Module getModule() {
        return (Module) super.getModule();
    }

    /**
     * 鑾峰彇鎵€鏈夐儴浠跺彉閲?
     *
     * @return 閮ㄤ欢鍙橀噺鏄犲皠
     */
    public Map<String, PartVarImpl> getPartMap() {
        return partMap;
    }

    /**
     * 鑾峰彇鎵€鏈夊弬鏁板彉閲?
     *
     * @return 鍙傛暟鍙橀噺鏄犲皠
     */
    public Map<String, ParaVarImpl> getParaMap() {
        return paraMap;
    }

    @Override
    protected Object newPartVar(PartVarImpl internalPartVar) {
        return internalPartVar;
    }

    @Override
    protected Object newParaVar(ParaVarImpl internalParaVar) {
        return internalParaVar;
    }

    /**
     * 娣诲姞涓嶅吋瀹规€х害鏉燂紙閮ㄤ欢绾у埆锛?
     * 
     * @param ruleCode          瑙勫垯浠ｇ爜
     * @param leftPartsExprStr  宸︿晶閮ㄤ欢琛ㄨ揪寮忓瓧绗︿覆锛屾牸寮忓 "fatherCode=cpu:CoreNum=4"
     * @param rightPartsExprStr 鍙充晶閮ㄤ欢琛ㄨ揪寮忓瓧绗︿覆
     */
    public void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr) {
        PartsExpr left = filterPartExpr(leftPartsExprStr, true);
        PartsExpr right = filterPartExpr(rightPartsExprStr, false);

        if (left.isEmpty4FilterPars() || right.isEmpty4FilterPars()) {
            log.info("Skip inCompatible constraint: {} - left or right filter is empty", ruleCode);
            return;
        }

        compatibleConstraintAlg.addCompatibleConstraintInCompatible(ruleCode + currrentRulePostName, left, right);
    }

    private PartsExpr filterPartExpr(String partExprStr, boolean isLeft) {
        if (partExprStr == null || partExprStr.isEmpty()) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.error("partExprStr is null or empty, cannot filter part expr");
            throw new AlgLoaderException("partExprStr is null or empty, cannot filter part expr");
        }
        PartsExpr partsExpr = new PartsExpr();
        // 瑙ｆ瀽partCategoryCode鍜宖ilterConditionStr
        String partCategoryCode = "";
        String filterConditionStr = "";

        int colonIndex = partExprStr.indexOf(':');
        if (colonIndex > 0) {
            partCategoryCode = partExprStr.substring(0, colonIndex);
            filterConditionStr = partExprStr.substring(colonIndex + 1);
        } else {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.error("partExprStr is invalid, cannot filter part expr");
            throw new AlgLoaderException("partExprStr is invalid, cannot filter part expr");
        }

        partsExpr.setFilterConditionStr(filterConditionStr);

        PartCategoryAlgImpl partCategoryAlgImpl = isLeft ? currentLeftPartCategoryAlgImpl
                : currentRightPartCategoryAlgImpl;
        if (partCategoryAlgImpl == null) {
            // 鍏煎浠ュ墠鐨?
            partCategoryAlgImpl = this.getPartCategoryAlg(partCategoryCode);
        } else {
            if (!partCategoryAlgImpl.getCategoryCode().equals(partCategoryCode)) {
                // 鎵撴棩蹇楋紝鎶涘紓甯?
                log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
                throw new AlgLoaderException("partCategoryAlgImpl is invalid, cannot filter part expr");
            }
        }

        if (partCategoryAlgImpl == null) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
            return partsExpr;
        }
        // 杞崲涓篜artVar
        partsExpr.setPartVars(partCategoryAlgImpl.getAllPartVars(""));
        Pair<List<PartVarImpl>, List<PartVarImpl>> partVars = partCategoryAlgImpl.filterAllPartVars(filterConditionStr);
        partsExpr.setFilterPartVars(partVars.getFirst());
        partsExpr.setNoFilterPartVars(partVars.getSecond());
        return partsExpr;
    }

    // /**
    // * 瑙ｆ瀽閮ㄤ欢琛ㄨ揪寮忓瓧绗︿覆
    // * 鏍煎紡锛歠atherCode=xxx:filterCondition
    // *
    // * @param partExprStr 閮ㄤ欢琛ㄨ揪寮忓瓧绗︿覆
    // * @return PartsExpr
    // */
    // private PartsExpr filterPartExpr(String partExprStr) {
    // if (partExprStr == null || partExprStr.isEmpty()) {
    // // 鎵撴棩蹇楋紝鎶涘紓甯?
    // log.error("partExprStr is null or empty, cannot filter part expr");
    // throw new AlgLoaderException("partExprStr is null or empty, cannot filter
    // part expr");
    // }
    // PartsExpr partsExpr = new PartsExpr();
    // // 瑙ｆ瀽partCategoryCode鍜宖ilterConditionStr
    // String partCategoryCode = "";
    // String filterConditionStr = "";

    // int colonIndex = partExprStr.indexOf(':');
    // if (colonIndex > 0) {
    // partCategoryCode = partExprStr.substring(0, colonIndex);
    // filterConditionStr = partExprStr.substring(colonIndex + 1);
    // } else {
    // // 鎵撴棩蹇楋紝鎶涘紓甯?
    // log.error("partExprStr is invalid, cannot filter part expr");
    // throw new AlgLoaderException("partExprStr is invalid, cannot filter part
    // expr");
    // }

    // partsExpr.setFilterConditionStr(filterConditionStr);

    // PartCategoryAlgImpl partCategoryAlgImpl =
    // this.getPartCategoryAlg(partCategoryCode);
    // if (partCategoryAlgImpl == null) {
    // // 鎵撴棩蹇楋紝鎶涘紓甯?
    // log.warn("partCategoryAlgImpl is invalid, cannot filter part expr");
    // return partsExpr;
    // }
    // // 鑾峰彇璇ュ垎绫讳笅鐨勬墍鏈夊師瀛愰儴浠?
    // List<Part> atomicParts = partCategoryAlgImpl.getAllAtomicParts();

    // // 鏍规嵁杩囨护鏉′欢绛涢€夐儴浠?
    // List<Part> filterParts;
    // List<Part> noFilterParts;

    // if (filterConditionStr == null || filterConditionStr.isEmpty()) {
    // filterParts = new ArrayList<>();
    // noFilterParts = new ArrayList<>(atomicParts);
    // } else {
    // filterParts = FilterExpressionExecutor.doSelect(atomicParts,
    // filterConditionStr);
    // noFilterParts = subPart(atomicParts, filterParts);
    // }

    // // 杞崲涓篜artVar
    // partsExpr.setPartVars(toPartVar(partCategoryAlgImpl, atomicParts));
    // partsExpr.setFilterPartVars(toPartVar(partCategoryAlgImpl, filterParts));
    // partsExpr.setNoFilterPartVars(toPartVar(partCategoryAlgImpl, noFilterParts));
    // return partsExpr;
    // }

    /**
     * 灏哖art鍒楄〃杞崲涓篜artVar鍒楄〃
     * 
     * @param parts Part鍒楄〃
     * @return PartVar鍒楄〃
     */
    public List<PartVarImpl> toFilterPartVar(PartCategoryAlgImpl partCategoryAlgImpl, String filtedConditionStr) {
        List<Part> atomicParts = partCategoryAlgImpl.getAtomicParts();
        if (filtedConditionStr != null && !filtedConditionStr.trim().isEmpty()) {
            atomicParts = FilterExpressionExecutor.doSelect(atomicParts, filtedConditionStr);
            log.info("Priority-Filtered parts: {} in sum4Parts", PartUtils.toShortString(atomicParts));
        }
        return toPartVar(partCategoryAlgImpl, atomicParts);
    }

    /**
     * 灏哖art鍒楄〃杞崲涓篜artVar鍒楄〃
     * 
     * @param parts Part鍒楄〃
     * @return PartVar鍒楄〃
     */
    public List<PartVarImpl> toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, List<Part> parts) {
        return parts.stream()
                .map(t -> toPartVar(
                        partCategoryAlgImpl, t))
                .filter(pv -> pv != null)
                .collect(Collectors.toList());
    }

    /**
     * 灏哖art瀵硅薄杞崲涓篜artVar
     * 
     * @param part Part瀵硅薄
     * @return PartVarImpl锛屽鏋滀笉瀛樺湪鍒欒繑鍥瀗ull
     */
    public PartVarImpl toPartVar(PartCategoryAlgImpl partCategoryAlgImpl, Part part) {
        PartVarImpl partVar = partCategoryAlgImpl.getPartVar(part.getCode());
        if (partVar == null) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.error("PartVarImpl not found for code: {}", part.getCode());
            throw new AlgLoaderException("PartVarImpl not found for code: " + part.getCode());
        }
        return partVar;
    }

    /**
     * 閫氱敤鐨勯儴浠舵眰鍜屾柟娉曪紝鏍规嵁鎸囧畾鐨勫彉閲忚幏鍙栧嚱鏁拌绠楁€诲拰
     * 
     * @param cofAttrCode        灞炴€т唬鐮侊紝濡傛灉涓簄ull鎴栫┖鍒欎娇鐢ㄩ粯璁ゅ€?
     * @param varGetter          浠嶱artVar鑾峰彇LinearArgument鐨勫嚱鏁帮紙濡俫etIsSelected鎴杇etQty锛?
     * @param varName            鍙橀噺鍚嶇О锛堝"isSelected"鎴?qty"锛夛紝鐢ㄤ簬鏋勫缓瀛楃涓茶〃杈惧紡
     * @param filtedConditionStr 杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    // @Override
    protected PartAlgCPLinearExpr sum4Parts(PartCategoryAlgImpl partCategoryAlgImpl, String cofAttrCode,
            Function<PartVarImpl, LinearArgument> varGetter, String varName, String filtedConditionStr) {
        PartAlgCPLinearExpr expr = buildSumExpr(
                partCategoryAlgImpl,
                cofAttrCode, varName, varGetter,
                filtedConditionStr);
        return expr;
    }

    /**
     * 瀵规寚瀹氶儴浠跺垎绫荤殑閮ㄤ欢姹傚拰锛堥€変腑鐘舵€侊紝鏀寔澶氬垎绫婚€楀彿鍒嗛殧锛?
     * 
     * @param partCategoryCodesStr 閮ㄤ欢鍒嗙被浠ｇ爜锛屾敮鎸侀€楀彿鍒嗛殧锛屽 "driveI0,driveI1"
     * @param cofAttrCode          灞炴€т唬鐮?
     * @param filtedConditionStr   杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    public PartAlgCPLinearExpr sum4Selected(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        if (partCategoryCodesStr == null || partCategoryCodesStr.trim().isEmpty()) {
            return sum4Selected(cofAttrCode, filtedConditionStr);
        }
        List<PartCategoryAlgImpl> partCategoryAlgImpls = toPartCategoryAlgImpls(partCategoryCodesStr);

        return buildSumExpr(
                partCategoryAlgImpls, cofAttrCode, PartVarImpl.ISSELECTED_SHORT_NAME, PartVarImpl::getIsSelected,
                filtedConditionStr);
    }

    /**
     * 瀵规寚瀹氶儴浠跺垎绫荤殑閮ㄤ欢姹傚拰锛堟暟閲忥紝鏀寔澶氬垎绫婚€楀彿鍒嗛殧锛?
     * 
     * @param partCategoryCodesStr 閮ㄤ欢鍒嗙被浠ｇ爜锛屾敮鎸侀€楀彿鍒嗛殧锛屽 "driveI0,driveI1"
     * @param cofAttrCode          灞炴€т唬鐮?
     * @param filtedConditionStr   杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    public PartAlgCPLinearExpr sum4Quantity(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        if (Strings.isEmpty(partCategoryCodesStr)) {
            return sum4Quantity(cofAttrCode, filtedConditionStr);
        }
        List<PartCategoryAlgImpl> partCategoryAlgImpls = toPartCategoryAlgImpls(partCategoryCodesStr);
        return buildSumExpr(partCategoryAlgImpls, cofAttrCode, PartVarImpl.QTY_SHORT_NAME, PartVarImpl::getQty,
                filtedConditionStr);
    }

    private List<PartCategoryAlgImpl> toPartCategoryAlgImpls(String partCategoryCodesStr) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = new ArrayList<>();
        if (partCategoryCodesStr.isEmpty()) {
            return partCategoryAlgImpls;
        }
        if (partCategoryCodesStr.contains("*")) {
            String partCategoryCodePrefix = partCategoryCodesStr.replace("*", "");
            partCategoryAlgImpls.addAll(getPartCategoryAlgByInstPrefix(partCategoryCodePrefix));
            return partCategoryAlgImpls;
        }
        String[] categoryCodes = partCategoryCodesStr.split(",");
        for (String categoryCode : categoryCodes) {
            partCategoryAlgImpls.add(getPartCategoryAlg(categoryCode));
        }
        return partCategoryAlgImpls;
    }

    /**
     * 瀵归€変腑鐨勯儴浠舵眰鍜岋紙甯﹀睘鎬х郴鏁帮級
     *
     * @param cofAttrCode        灞炴€т唬鐮侊紝濡傛灉涓簄ull鎴栫┖鍒欎娇鐢ㄩ粯璁ゅ€?
     * @param filtedConditionStr 杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */

    public PartAlgCPLinearExpr sum4Selected(String cofAttrCode,
            String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg, cofAttrCode, PartVarImpl::getIsSelected,
                PartVarImpl.ISSELECTED_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 瀵归€変腑鐨勯儴浠舵眰鍜岋紙涓嶅甫灞炴€х郴鏁帮級
     *
     * @param filtedConditionStr 杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    public PartAlgCPLinearExpr sum4Selected(String filtedConditionStr) {
        return sum4Selected(null, filtedConditionStr);
    }

    /**
     * 瀵规暟閲忕殑閮ㄤ欢姹傚拰锛堝甫灞炴€х郴鏁帮級
     *
     * @param cofAttrCode        灞炴€т唬鐮侊紝濡傛灉涓簄ull鎴栫┖鍒欎娇鐢ㄩ粯璁ゅ€?
     * @param filtedConditionStr 杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    public PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return sum4Parts((PartCategoryAlgImpl) currentModuleAlg,
                cofAttrCode, PartVarImpl::getQty, PartVarImpl.QTY_SHORT_NAME, filtedConditionStr);
    }

    /**
     * 瀵规暟閲忕殑閮ㄤ欢姹傚拰锛堜笉甯﹀睘鎬х郴鏁帮級
     *
     * @param filtedConditionStr 杩囨护鏉′欢瀛楃涓?
     * @return 姹傚拰鍚庣殑AlgCPLinearExpr琛ㄨ揪寮?
     */
    public PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr) {
        return sum4Quantity(null, filtedConditionStr);
    }

    public void addControlParaEqual(String sumParaCode, String instSumParaCode) {
        // 鍔ㄦ€佽〃杈惧紡 addParaEqual("driveSumQuantity", "drive:SumQuantity");
        ParaVarImpl sumParaVar = this.getParaVar(sumParaCode);
        if (sumParaVar == null) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.error("ParaVarImpl not found for code: {}", sumParaCode);
            throw new AlgLoaderException("ParaVarImpl not found for code: " + sumParaCode);
        }
        // instSumParaCode,濡傦細"drive:SumQuantity", 瑙ｆ瀽鍑篸rive锛孲umQuantity
        String[] parts = instSumParaCode.split(":");
        if (parts.length != 2) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.error("instSumParaCode is invalid: {}", instSumParaCode);
            throw new AlgLoaderException("instSumParaCode is invalid: " + instSumParaCode);
        }

        String partCategoryCodePrefix = parts[0].trim();
        String sumQuantityCode = parts[1].trim();
        // expr=sumParaVar.value + sumParaVar.value + ...
        List<PartCategoryAlgImpl> partCategoryAlgImpls = this.getPartCategoryAlgByInstPrefix(partCategoryCodePrefix);
        if (partCategoryAlgImpls.isEmpty()) {
            // 鎵撴棩蹇楋紝鎶涘紓甯?
            log.warn("PartCategoryAlgImpl not found for code: {}", partCategoryCodePrefix);
            sumParaVar.setHasInputed(false);
        } else {
            Integer sumInputValue = 0;
            boolean hasInput = false;
            for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
                ParaVarImpl instParaVar = partCategoryAlgImpl.getParaVar(sumQuantityCode);
                if (instParaVar == null || instParaVar.getInputValue() == null) {
                    // 鎵撴棩蹇楋紝鎶涘紓甯?
                    log.error("ParaVarImpl not found for code: {}", sumQuantityCode);
                    // 鏆傛椂涓嶆姏寮傚父 throw new AlgLoaderException("ParaVarImpl not found for code: " +
                    // sumQuantityCode);
                    // issue:鏄笉鏄€绘湁涓€涓帶鍒跺弬鏁版槸鏈夎緭鍏ョ殑锛屾柟渚挎墽琛岋紙瀵圭粨鏋滆繘琛屾牎楠岋級
                    continue;
                }
                // paraSumExpr.addTerm(instParaVar.getInputValue(), 1);
                sumInputValue += instParaVar.getInputValue();
                hasInput = true;
            }
            if (hasInput) {
                // 鏋勫缓绛夊紡
                sumParaVar.setHasInputed(true);
                sumParaVar.setInputValue(sumInputValue);

            } else {
                sumParaVar.setHasInputed(false);
            }

        }

    }

    /**
     * Apply decision strategies to part category variables.
     * Called after all variables are created and before rule initialization.
     */
    private void applyDecisionStrategies() {
        ModuleInput mi = getModuleInput();
        if (mi == null || mi.getPartCategoryInputs() == null) {
            return;
        }
        for (PartCategoryInputBase input : mi.getPartCategoryInputs()) {
            PartConstraintReq orgReq = input.getOrgReq();
            if (orgReq == null) {
                continue;
            }
            List<StrategyConfig> strategies = orgReq.getDecisionStrategies();
            if (strategies == null || strategies.isEmpty()) {
                continue;
            }

            String partCategoryCode = input.getPartCategoryCode();

            if (input instanceof MultiInstPartCategoryInput) {
                MultiInstPartCategoryInput multiInput = (MultiInstPartCategoryInput) input;
                for (PartCategoryInput pcInput : multiInput.getPartCategoryInputs()) {
                    PartCategoryAlgImpl pcAlg = getPartCategoryAlg(pcInput.getPartCategoryCode());
                    if (pcAlg != null) {
                        applyStrategies(pcAlg, strategies);
                    }
                }
            } else {
                PartCategoryAlgImpl pcAlg = getPartCategoryAlg(partCategoryCode);
                if (pcAlg != null) {
                    applyStrategies(pcAlg, strategies);
                }
            }
        }
    }

    private void applyStrategies(PartCategoryAlgImpl pcAlg, List<StrategyConfig> strategies) {
        for (StrategyConfig config : strategies) {
            if (config.getStrategyType() == null || config.getStrategyType() == StrategyType.UNSPECIFIED) {
                continue;
            }
            if (config.getSortAttributeCode() == null || config.getSortAttributeCode().isEmpty()) {
                log.warn("Strategy has no sortAttributeCode, skipping");
                continue;
            }
            applySingleStrategy(pcAlg, config);
        }
    }

    private void applySingleStrategy(PartCategoryAlgImpl pcAlg, StrategyConfig config) {
        List<PartVarImpl> partVars = pcAlg.getAllPartVars("");
        if (partVars.isEmpty()) {
            log.warn("No part vars found for category {}, skipping strategy", pcAlg.getCategoryCode());
            return;
        }

        String sortAttr = config.getSortAttributeCode();
        boolean ascending = config.getStrategyType() == StrategyType.ASCENDING;

        // Sort part vars by the sort attribute
        partVars.sort(Comparator.comparingInt(pv -> getSortValue(pv, sortAttr)));

        // Reverse for descending
        if (!ascending) {
            java.util.Collections.reverse(partVars);
        }

        // Get isSelected BoolVars (which are IntVars) in sorted order
        IntVar[] vars = partVars.stream()
                .map(PartVarImpl::getIsSelected)
                .toArray(IntVar[]::new);

        // SELECT_MAX_VALUE: try selecting parts first, which means preferred parts
        // (sorted first) get selected in early solutions
        model.addDecisionStrategy(vars,
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MAX_VALUE);

        log.info("Applied decision strategy: {} on {} vars for category {}, sortAttr={}",
                config.getStrategyType(), vars.length, pcAlg.getCategoryCode(), sortAttr);
    }

    /**
     * Get the integer sort value for a part variable by attribute code.
     * Supports basic attributes like "price" and dynamic attributes.
     */
    private int getSortValue(PartVarImpl partVar, String attrCode) {
        if ("price".equals(attrCode) && partVar.getBase() instanceof Part) {
            Long price = ((Part) partVar.getBase()).getPrice();
            return price != null ? price.intValue() : 0;
        }
        return partVar.getAttr4Int(attrCode);
    }
}
