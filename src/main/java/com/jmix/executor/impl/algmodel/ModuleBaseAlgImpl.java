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
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.impl.PriorityConstraint;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.view.ModuleInstView;
import com.jmix.executor.southinf.var.Var;
import com.jmix.executor.impl.southbridge.SouthboundModuleAlgAdapter;

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
 * 缁犳纭堕崺铏硅
 * 鐎规矮绠熷Ο鈥虫健/闁劋娆㈤崚鍡欒缁狙冨焼缁犳纭堕惃鍕彆閸忚鲸鏌熷▔?
 * 
 * @since 2025-12-27
 */
@Slf4j
public abstract class ModuleBaseAlgImpl implements IModuleAlg {

    /**
     * CP缁撅附娼Ч鍌澬掑Ο鈥崇€风€圭偘绶?
     */
    protected AlgCPModel model;

    /**
     * module閻ㄥ嫬鐔€绾偓娣団剝浼?
     */
    protected IModule module;

    /**
     * 闁劋娆㈤崣姗€鍣洪弰鐘茬殸鐞涱煉绱濈€涙ê鍋峜ode閸掔櫃artVar閻ㄥ嫭妲х亸?
     */
    protected Map<String, PartVarImpl> partMap = new LinkedHashMap<>();

    /**
     * 閸欏倹鏆熼崣姗€鍣洪弰鐘茬殸鐞涱煉绱濈€涙ê鍋峜ode閸掔櫃araVar閻ㄥ嫭妲х亸?
     */
    protected Map<String, ParaVarImpl> paraMap = new LinkedHashMap<>();

    /**
     * 閸欐妯夊蹇曞閺夌喐甯堕崚璺哄讲鐟欎焦鈧呮畱缂傛牜鐖滈梿鍡楁値閿涘矁鐑︽潻鍥帛鐠併倗绮︾€?
     */
    protected Set<String> codesOfHiddenConstraint = new HashSet<>();

    /**
     * 閹碘偓閺堝顫夐崚娆愭煙濞夋洜娈戦弰鐘茬殸鐞涱煉绱濈€涙ê鍋嶇憴鍕灟娴狅絿鐖滈崚鏉款嚠鎼存梹鏌熷▔鏇犳畱閺勭姴鐨?
     */
    protected Map<String, Method> ruleMethods = new HashMap<>();

    /**
     * 娴兼ê鍘涚痪褏瀹抽弶鐔告Ё鐏忓嫯銆冮敍灞界摠閸屻劌鐫橀幀褌鍞惍浣稿煂娴兼ê鍘涚痪褏瀹抽弶鐔烘畱閺勭姴鐨犻敍灞肩矌閼宠姤鏁幐浣风閺夆€茬喘閸忓牏楠囩痪锔芥将
     */
    @Getter
    @Setter
    protected PriorityConstraint priorityConstraint;

    /**
     * 閸忕厧顔愰幀褏瀹抽弶鐔虹暬濞夋洖鐤勬笟?
     */
    protected CompatibleConstraintAlg compatibleConstraintAlg;

    /**
     * 瑜版挸澧犲Ο鈥虫健,缁犳纭惰ぐ鎾冲閹笛嗩攽濡€虫健
     */
    protected IModule currentModule;

    /**
     * 瑜版挸澧犲Ο鈥虫健缁犳纭剁€圭偘绶?
     */
    protected ModuleBaseAlgImpl currentModuleAlg;

    /**
     * 瑜版挸澧犻柈銊ゆ缁撅附娼?
     */
    protected IModuleInput moduleInput;

    /**
     * Current module instance view for POST-stage rules.
     */
    private ModuleInstView currentModuleInstView;

    /**
     * 閼惧嘲褰囩€圭偘绶D
     * 
     * @return 鐎圭偘绶D
     */
    public int getInstId() {
        return ModuleInst.DEFAULT_INSTANCE_ID;
    }

    /**
     * 濞ｈ濮為崪宀勬閽樺繒娴夐崗宕囨畱缁撅附娼惃鍒卆r
     * 
     * @param hiddenVars 闂団偓鐟曚焦鍧婇崝鐘绘閽樺繒瀹抽弶鐔烘畱閸欐﹢鍣洪弫鎵矋
     */
    protected void addVarAboutHiddenConstraints(VarImpl<?>... hiddenVars) {
        for (VarImpl<?> v : hiddenVars) {
            codesOfHiddenConstraint.add(v.getCode());
        }
    }

    /**
     * 閺嶈宓佹径鏍劥娴肩姴鍙嗛惃鍕劥娴犲墎瀹抽弶鐕傜礄濮瑰倸鎷扮痪锔芥将閿涘顔曠純顔碱嚠鎼存梻娈戦崣鍌涙殶鏉堟挸鍙嗛崐?
     *
     * @param partConstraints 闁劋娆㈢痪锔芥将閸掓銆冮敍灞藉讲娴犮儰璐焠ull
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
        // 婢舵艾鐤勬笟瀣倵閿涘奔绗夐弨顖涘瘮鏉╂瑧顫掗弬鐟扮础閿涘ntoCode = pt.getFilteredCategory().getCode(); String paraCode =
        // ontoCode + "Sum" + pt.getSumAttrCode();
        String paraCode = ipt.getAttrType().name() + AttrPara.CODE_SEPARATOR + ipt.getSumAttrCode();

        ParaVarImpl pVar = this.getParaVar(paraCode);
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
     * 鐠佸墽鐤嗘妯款吇閸欘垵顫嗛幀褏瀹抽弶?
     * 鐎甸€涚艾濞屸剝婀侀弰鎯х础閹貉冨煑閸欘垵顫嗛幀褏娈戦崣姗€鍣洪敍宀冾啎缂?isHiddenVar == 0閿涘牆宓嗘妯款吇閸欘垵顫嗛敍?
     * 闁秴宸婚幍鈧張澶婂綁闁插骏绱濇稉鐑樼梾閺堝妯夊蹇撳讲鐟欎焦鈧勫付閸掑墎娈戦崣姗€鍣虹拋鍓х枂姒涙顓婚崣顖濐潌閻樿埖鈧?
     * 
     * @throws AlgLoaderException 瀵倸鐖?
     */
    protected void setDefaultVisibilityConstraints() {
        boolean isFirst = true;
        for (ParaVarImpl paraVar : this.getParaVars()) {
            if (codesOfHiddenConstraint.contains(paraVar.getCode())) {
                continue;
            }
            if (isFirst) {
                this.model.setRelax4SysRule("hiddensrule");
                isFirst = false;
            }
            if (paraVar.getBase().getAssignType() == AssignType.CALC) {
                // 閺嗗倹妞傚▽鈩冩箒濞ｈ濮為崚鐗堟緱瀵稑褰夐柌蹇涘櫡
                model.addEquality(paraVar.getIsHidden(), 0);
            }
        }
        for (PartVarImpl partVar : this.getPartVars()) {
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
            String varName, Function<PartVarImpl, LinearArgument> varGetter, String filtedConditionStr) {
        boolean isWithoutAttr = attrCode == null || attrCode.isEmpty();
        List<PartVarImpl> partVars = partCategoryAlgImpl.getAllPartVars(filtedConditionStr);
        for (PartVarImpl partVar : partVars) {
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
            String varName, Function<PartVarImpl, LinearArgument> varGetter, String filtedConditionStr) {
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
            String varName, Function<PartVarImpl, LinearArgument> varGetter, String filtedConditionStr) {
        PartAlgCPLinearExpr algExpr = new PartAlgCPLinearExpr(
                partCategoryAlgImpl.getCategoryCode() + "_" + partCategoryAlgImpl.getInstId() + "_sumPars_"
                        + (attrCode == null ? "" : attrCode) + "_" + varName);
        return buildSumExprInternal(algExpr, partCategoryAlgImpl, attrCode, varName, varGetter, filtedConditionStr);
    }

    protected void sumFunConstraint(ModuleBaseAlgImpl moduleBaseAlgImpl,
            PartCategoryInputBase partConstraint) {
        // 鐎涙劗琚紒褎澹欑€圭偟骞?
    }

    /**
     * 濞ｈ濮為弶鎯х幢閻╊喗鐖ｉ崙鑺ユ殶
     * 閻╊喗鐖ｉ崙鑺ユ殶閿涙碍娓剁亸蹇撳闂団偓鐟曚焦婢楀娑氭畱缁撅附娼弫浼村櫤閿涘牆鐢弶鍐櫢閿?
     */
    public void addRelaxObjectFunction() {
        if (!model.isIsAttachRelax()) {
            return;
        }

        // 閻╊喗鐖ｉ崙鑺ユ殶閿涙碍娓剁亸蹇撳闂団偓鐟曚焦婢楀娑氭畱缁撅附娼弫浼村櫤閿涘牆鐢弶鍐櫢閿?
        List<RelaxVar> relaxVars = new ArrayList<>(model.getRelaxVarMap().values());
        if (!relaxVars.isEmpty()) {
            // 閺嬪嫬缂撻崝鐘虫綀閻╊喗鐖ｉ崙鑺ユ殶閿涙in(relaxVar[0].value * relaxVar[0].weight + relaxVar[1].value *
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
     * 閹笛嗩攽閸楁洑閲滅憴鍕灟閺傝纭?
     *
     * @param ruleCode 鐟欏嫬鍨禒锝囩垳
     * @param method   鐟欏嫬鍨弬瑙勭《
     * @throws AlgLoaderException 瀵倸鐖?
     */
    protected void executeRuleMethod(Rule rule, Method method) {
        setCurrentModule4Rule(this, this);
        executeRuleMethod(rule, this, method);
        setCurrentModule4Rule(this, null);
    }

    protected void executeRuleMethod(Rule rule, Object moduleAlgFile, Method method) {
        if (method == null) {
            log.error("Rule method not found for execution: " + rule.getCode());
            throw new AlgLoaderException("Rule method not found for execution: " + rule.getCode());
        }
        try {
            // 鐠佸墽鐤嗚ぐ鎾冲閺夋儳绱遍崣姗€鍣洪崥宥囆?
            this.model.setRelax4CustomRule(rule.getCode());

            // 閹笛嗩攽鐟欏嫬鍨弬瑙勭《
            method.setAccessible(true);
            method.invoke(moduleAlgFile);
            log.info("Executed rule method: {}", method.getName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to execute rule method: " + method.getName(), e);
            throw new AlgLoaderException("Failed to execute rule method: " + method.getName(), e);
        }
    }

    /**
     * 濡€崇€烽崚婵嗩潗閸栨牕鎮楅惃鍕礀鐠嬪啯鏌熷▔?
     * 鐎涙劗琚崣顖欎簰闁插秴鍟撳銈嗘煙濞夋洘娼电€圭偟骞囬懛顏勭暰娑斿娈戦崚婵嗩潗閸栨牠鈧槒绶?
     * 
     * @param model CP濡€崇€风€圭偘绶?
     */
    protected void initModelAfter(AlgCPModel model) {

    }

    /**
     * 閸掓繂顫愰崠鏍ㄥ閺堝褰夐柌蹇ョ礄paras閸滃arts閿?
     * 闁秴宸籱odule閻ㄥ埦aras閿涘苯鍨卞绡渁raVar楠炶埖鏂侀崚鐨僡raMap
     * 闁秴宸籱odule閻ㄥ埦arts閿涘苯鍨卞绡渁rtVar楠炶埖鏂侀崚鐨僡rtMap
     */
    protected void initAll(Object moduleAlgFile) {
        if (module == null) {
            log.warn("Module is null, skip initAll");
            return;
        }

        // 閸掓繂顫愰崠鏍ㄥ閺堝「aras
        for (Para para : module.getParas()) {
            ParaVarImpl paraVar = initParaVar(para);
            paraMap.put(para.getCode(), paraVar);
        }
        log.info("Initialized {} para variables", paraMap.size());

        // 閸掓繂顫愰崠鏍ㄥ閺堝「arts閿涘牆甯€涙劙鍎存禒璁圭礆
        for (Part part : module.getAtomicParts()) {
            PartVarImpl partVar = initPartVar(part);
            partMap.put(part.getCode(), partVar);
        }
        log.info("Initialized {} part variables", partMap.size());
    }

    /**
     * 閸掓繂顫愰崠鏈卼trParas閿涘苯鍨卞鍝勵嚠鎼存梻娈慞ara閸滃araVar
     * 
     * @param attrParas 鐏炵偞鈧冨棘閺佹澘鍨悰?
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
     * 閸掓繂顫愰崠鏍у礋娑撶嫕ttrPara閿涘苯鍨卞鍝勵嚠鎼存梻娈慞ara閸滃araVar
     * 
     * @param attrPara 鐏炵偞鈧冨棘閺?
     */
    protected void newAttrParaVar(AttrPara attrPara) {
        if (attrPara == null || attrPara.getAttrCode() == null) {
            return;
        }
        String paraCode = attrPara.getType().name() + AttrPara.CODE_SEPARATOR + attrPara.getAttrCode();
        newAttrParaVar(paraCode);
    }

    protected ParaVarImpl newAttrParaVar(String paraCode) {
        if (paraMap.containsKey(paraCode)) {
            log.error("Para already exists for attrCode: {}, skipping", paraCode);
            throw new AlgLoaderException("Para already exists for attrCode: " + paraCode);
        }
        Para para = new Para();
        para.setCode(paraCode);
        para.setAssignType(AssignType.INPUT);
        // 娴犲骸顕惔鏃傛畱鐏炵偞鈧囧櫡闂堛垼骞忛崣鏍ㄦ殶閹圭敹ssue閿涘潰in閿涘ax缁涘绱?
        para.setParaType(ParaType.INTEGER);
        // ModuleBase tempModule = (ModuleBase) module;
        // tempModule.addPara(para);
        ParaVarImpl pVar = initParaVar(para);
        paraMap.put(paraCode, pVar);
        return pVar;
    }

    /**
     * 閸掓繂顫愰崠鏍у棘閺佹澘褰夐柌?
     * 閺嶈宓侀崣鍌涙殶缁鐎烽崚娑樼紦鐎电懓绨查惃鍕綁闁插骏绱橧NTEGER/ENUM閿?
     * 
     * @param para 閸欏倹鏆熺€电钖?
     * @return 閸掓稑缂撻惃鍕棘閺佹澘褰夐柌?
     */
    protected ParaVarImpl initParaVar(Para para) {
        String code = para.getCode();
        ParaVarImpl paraVar = new ParaVarImpl();
        paraVar.setBase(para);

        if (para.getAssignType() == AssignType.INPUT) {
            return paraVar;
        }
        String ipf = toInstPrefix();
        switch (para.getParaType()) {
            case INTEGER:
                paraVar.setValue(newIntVar(Integer.parseInt(para.getMinValue()), Integer.parseInt(para.getMaxValue()),
                        f(ParaVarImpl.VALUE_PATTERN, ipf, code)));
                break;
            case ENUM:
                List<DynamicAttributerOption> options = para.getOptions();
                if (options == null || options.isEmpty()) {
                    log.error("Para options not found for code: {}", code);
                    throw new AlgLoaderException("Para options not found for code: " + code);
                }
                paraVar.setValue(newIntVarFromDomain(para.getOptionIds(), f(ParaVarImpl.VALUE_PATTERN, ipf, code)));

                for (DynamicAttributerOption option : options) {
                    ParaOptionVarImpl optionVar = createParaOptionVar(para.getCode(), option.getCode());
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
        paraVar.setIsHidden(newBoolVar(f(ParaVarImpl.HIDDEN_PATTERN, ipf, code)));
        paraVar.setInstId(getInstId());
        return paraVar;
    }

    private String toInstPrefix() {
        final String prefix = (getInstId() == ModuleInst.DEFAULT_INSTANCE_ID ? "" : "I" + getInstId() + "_");
        return prefix;
    }

    /**
     * 閸掓繂顫愰崠鏍劥娴犺泛褰夐柌?
     * 閸掓稑缂撻柈銊ゆ閻ㄥ嫭鏆熼柌蹇嬧偓渚€鈧鑵戦悩鑸碘偓浣告嫲闂呮劘妫岄悩鑸碘偓浣稿綁闁?
     * 
     * @param part 闁劋娆㈢€电钖?
     * @return 閸掓稑缂撻惃鍕劥娴犺泛褰夐柌?
     */
    protected PartVarImpl initPartVar(Part part) {
        String ipf = toInstPrefix();
        PartVarImpl partVar = new PartVarImpl();
        partVar.setBase(part);
        partVar.setQty(newIntVar(0, part.getMaxQuantity(), f(PartVarImpl.QTY_PATTERN, ipf, part.getCode())));
        partVar.setIsHidden(newBoolVar(f(PartVarImpl.HIDDEN_PATTERN, ipf, part.getCode())));
        partVar.setIsSelected(newBoolVar(f(PartVarImpl.ISSELECTED_PATTERN, ipf, part.getCode())));
        partVar.setInstId(getInstId());
        // 濞ｈ濮濹ty閸滃瓥sSelected閻ㄥ嫬鍙х化?
        model.addGreaterOrEqual(partVar.getQty(), 1).onlyEnforceIf(partVar.getIsSelected());
        model.addEquality(partVar.getQty(), 0).onlyEnforceIf(partVar.getIsSelected().not());

        return partVar;
    }

    /**
     * 閼惧嘲褰囬幍鈧張濉抋rtVar閸掓銆?
     * 
     * @return PartVar閸掓銆?
     */
    public List<PartVarImpl> getPartVars() {
        return new ArrayList<>(partMap.values());
    }

    /**
     * 閼惧嘲褰囬幍鈧張濉抋raVar閸掓銆?
     * 
     * @return ParaVar閸掓銆?
     */
    public List<ParaVarImpl> getParaVars() {
        return new ArrayList<>(paraMap.values());
    }

    /**
     * 鐎涙劗琚柌宥呭晸濮濄倖鏌熷▔鏇熸降鐎圭偟骞囬懛顏勭暰娑斿褰夐柌蹇撳灥婵瀵查柅鏄忕帆
     */
    protected void onInitCustomVariables() {
        // 姒涙顓荤粚鍝勭杽閻滃府绱濈€涙劗琚崣顖欎簰闁插秴鍟?
    }

    /**
     * 鏉╁洦鎶ら崙鐑樺瘹鐎规瓲atherCode閻ㄥ嫯顫夐崚?
     *
     * @param rules      閹碘偓閺堝顫夐崚娆忓灙鐞?
     * @param fatherCode 閻栧墎楠囨禒锝囩垳閿涘苯顩ч弸婊€璐焠ull閸掓瑨骞忛崣鏍侀崸妤冮獓閸掝偆娈戠憴鍕灟
     * @return 鏉╁洦鎶ら崥搴ｆ畱鐟欏嫬鍨崚妤勩€?
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
     * 閺嶈宓乵odule.getAllRules()閺嬪嫬缂撻幍鈧張澶庮潐閸掓瑦鏌熷▔鏇犳畱閺勭姴鐨?
     *
     * @param module 濡€虫健鐎电钖?
     * @return 鐟欏嫬鍨禒锝囩垳閸掔増鏌熷▔鏇烆嚠鐠烇紕娈戦弰鐘茬殸
     */
    protected Map<String, Method> buildAllRuleMethods(IModule module) {
        return buildAllRuleMethods(module, this);
    }

    protected Map<String, Method> buildAllRuleMethods(IModule module, Object moduleAlgFile) {
        if (module == null || module.getAllRules() == null) {
            return new HashMap<>();
        }

        // 閼惧嘲褰囪ぐ鎾冲缁崵娈戦幍鈧張澶嬫煙濞?
        Method[] methods = moduleAlgFile.getClass().getDeclaredMethods();
        Map<String, Method> allMethods = new HashMap<>();

        // 閺嬪嫬缂撻弬瑙勭《閸氬秴鍩孧ethod鐎电钖勯惃鍕Ё鐏?
        for (Method method : methods) {
            allMethods.put(method.getName(), method);
        }

        // 閺嶈宓乵odule.getAllRules()閺夈儲鐎绨塽leMethods
        Map<String, Method> ruleMethods = new HashMap<>();
        for (Rule rule : module.getAllRules()) {
            String ruleCode = rule.getCode();
            Method method = allMethods.get(ruleCode);
            if (method != null) {
                ruleMethods.put(ruleCode, method);
                log.info("Built rule method mapping: {} -> {}", ruleCode, method.getName());
                // // 濡偓閺屻儲妲搁崥锔芥ЦPriorityRule閿涘苯顩ч弸婊勬Ц閸掓瑦鐎杞扮喘閸忓牏楠囩痪锔芥将
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
     * 閺嬪嫬缂撴导妯哄帥缁狙呭閺?
     * 
     * @param rule 鐟欏嫬鍨€电钖?
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
     * 鐏忓棗褰夐柌蹇撳晸閸ョ偛鐡у▓纰夌礉娣囨繆鐦夌憴鍕灟娴ｈ法鏁ら崣姗€鍣洪弰顖氭倱娑撯偓娑?
     */
    protected void writeBackToFields(Object moduleAlgFile) {
        Map<String, Field> fieldMap = getAllFieldVariables(moduleAlgFile);
        // 婢跺嫮鎮奝artVar
        for (Map.Entry<String, PartVarImpl> entry : partMap.entrySet()) {
            String code = entry.getKey();
            PartVarImpl partVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Object tVar = newPartVarForModuleField(moduleAlgFile, partVar, field);
                setVariableField(moduleAlgFile, tVar, field);
            }
        }

        // 婢跺嫮鎮奝araVar
        for (Map.Entry<String, ParaVarImpl> entry : paraMap.entrySet()) {
            String code = entry.getKey();
            ParaVarImpl paraVar = entry.getValue();
            Field field = fieldMap.get(code);
            if (field != null) {
                Object tVar = newParaVarForModuleField(moduleAlgFile, paraVar, field);
                setVariableField(moduleAlgFile, tVar, field);
            }
        }
    }

    private Object newPartVarForModuleField(Object moduleAlgFile, PartVarImpl internalPartVar, Field field) {
        if (moduleAlgFile instanceof ModuleBaseAlgImpl algFileImpl) {
            return algFileImpl.newPartVarForField(internalPartVar, field);
        }
        if (moduleAlgFile instanceof ModuleAlgBase southboundAlg) {
            return SouthboundModuleAlgAdapter.newPartVarForField(southboundAlg, internalPartVar, field);
        }
        return newPartVarForField(internalPartVar, field);
    }

    private Object newParaVarForModuleField(Object moduleAlgFile, ParaVarImpl internalParaVar, Field field) {
        if (moduleAlgFile instanceof ModuleBaseAlgImpl algFileImpl) {
            return algFileImpl.newParaVarForField(internalParaVar, field);
        }
        if (moduleAlgFile instanceof ModuleAlgBase southboundAlg) {
            return SouthboundModuleAlgAdapter.newParaVarForField(southboundAlg, internalParaVar, field);
        }
        return newParaVarForField(internalParaVar, field);
    }

    /**
     * 閹笛嗩攽閺堫剙鐪伴惃鍕潐閸?
     *
     * @param allRuleMethods 閹碘偓閺堝顫夐崚娆愭煙濞夋洘妲х亸?
     */
    protected void executeModuleRules(Object moduleAlgFile, Map<String, Method> allRuleMethods,
            CalcStage calcStage) {
        if (module == null || module.getAllRules() == null || allRuleMethods == null) {
            return;
        }

        List<Rule> moduleRules = module.getRules(calcStage);
        executeModuleRules(moduleRules, moduleAlgFile, allRuleMethods, calcStage);
    }

    /**
     * 閹笛嗩攽閺堫剙鐪伴惃鍕潐閸?
     *
     * @param allRuleMethods 閹碘偓閺堝顫夐崚娆愭煙濞夋洘妲х亸?
     */
    protected void executeModuleRules(List<Rule> rules, Object moduleAlgFile, Map<String, Method> allRuleMethods,
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

    private void setCurrentModule4Rule(Object moduleAlgFile, ModuleBaseAlgImpl tmpModuleAlg) {
        if (moduleAlgFile instanceof ModuleBaseAlgImpl algFileImpl) {
            algFileImpl.currentModule = (tmpModuleAlg == null ? null : tmpModuleAlg.getModule());
            algFileImpl.currentModuleAlg = tmpModuleAlg;
        }
        if (moduleAlgFile instanceof ModuleAlgBase southboundAlg) {
            SouthboundModuleAlgAdapter.updateRuntimeContext(southboundAlg, tmpModuleAlg);
        }
    }

    protected void initData(AlgCPModel model, IModule module, IModuleInput moduleInput,
            Object moduleAlgFile) {
        // Module缁狙冨焼
        this.model = model;
        this.module = module;
        this.moduleInput = moduleInput;
        // 閸掓繂顫愰崠鏍у悑鐎硅鈧呭閺夌喓鐣诲▔鏇炵杽娓?
        this.compatibleConstraintAlg = new CompatibleConstraintAlg(model);
        initModelAfter(model);

        // 閸掓繂顫愰崠鏍ㄦ拱鐏炲倸褰夐柌蹇ョ礄paras閸滃arts閿?
        initAll(moduleAlgFile);
    }

    protected void initInput(Object moduleAlgFile) {
        // 鐏忓棗褰夐柌蹇撳晸閸ョ偛鐡у▓?
        writeBackToFields(moduleAlgFile);

        log.info("ModuleBaseAlgImpl initInput {}", module.getClass().getSimpleName());
    }

    public void initRules(Map<String, Method> allRuleMethods, Object moduleAlgFile, CalcStage calcStage) {
        // 閹笛嗩攽閺堫剙鐪伴惃鍕潐閸?
        executeModuleRules(moduleAlgFile, allRuleMethods, calcStage);

        if (calcStage == CalcStage.MID) {
            // 鐠佸墽鐤嗘妯款吇閸欘垵顫嗛幀褏瀹抽弶?
            setDefaultVisibilityConstraints();
        }

        log.info("ModuleBaseAlgImpl initRules {}", module.getClass().getSimpleName());
    }

    /**
     * 閸掓稑缂撻柈銊ゆ閸欐﹢鍣? 缂佈勫缁褰叉禒銉╁櫢鏉?
     * 
     * @param internalPartVar 閸愬懘鍎撮柈銊ゆ閸欐﹢鍣?
     * @return 閸掓稑缂撻惃鍕劥娴犺泛褰夐柌?
     */
    protected abstract Object newPartVar(PartVarImpl internalPartVar);

    protected Object newPartVarForField(PartVarImpl internalPartVar, Field field) {
        return newPartVar(internalPartVar);
    }

    /**
     * 閸掓稑缂撻崣鍌涙殶閸欐﹢鍣? 缂佈勫缁褰叉禒銉╁櫢鏉?
     * 
     * @param internalParaVar 閸愬懘鍎撮崣鍌涙殶閸欐﹢鍣?
     * @return 閸掓稑缂撻惃鍕棘閺佹澘褰夐柌?
     */
    protected abstract Object newParaVar(ParaVarImpl internalParaVar);

    protected Object newParaVarForField(ParaVarImpl internalParaVar, Field field) {
        return newParaVar(internalParaVar);
    }

    /**
     * 鐠佸墽鐤嗛崡鏇氶嚋閸欐﹢鍣虹€涙顔?
     * 
     * @param v     閸欐﹢鍣洪崐?
     * @param field 鐎涙顔岀€电钖?
     * @throws AlgLoaderException 瀵倸鐖?
     */
    protected void setVariableField(Object v, Field field) throws AlgLoaderException {
        setVariableField(this, v, field);
    }

    protected void setVariableField(Object moduleAlgFile, Object v, Field field) throws AlgLoaderException {
        String variableCode = variableCode(v);
        if (field == null) {
            log.error("Field not found for code: null {}", variableCode);
            throw new AlgLoaderException("Field not found for code: null " + variableCode);
        }
        try {
            field.setAccessible(true);
            field.set(moduleAlgFile, v);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("Failed to write back variable to field: " + variableCode, e);
            throw new AlgLoaderException("Failed to write back variable to field: " + variableCode, e);
        }
    }

    private String variableCode(Object value) {
        if (value instanceof VarImpl) {
            return ((VarImpl<?>) value).getCode();
        }
        if (value instanceof Var) {
            return ((Var) value).code();
        }
        return String.valueOf(value);
    }

    /**
     * 閼惧嘲褰囬幍鈧張澶婄摟濞堥潧褰夐柌?
     * 
     * @return 鐎涙顔岄弰鐘茬殸鐞?
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
     * 缁犳纭堕弬鍥︽缁紮绱濋懢宄板絿閹碘偓閺堝鐡у▓闈涘綁闁?
     * 
     * @return 鐎涙顔岄弰鐘茬殸鐞?
     */
    protected Map<String, Field> getAllFieldVariables(Object moduleAlgFile) {
        Field[] fields = moduleAlgFile.getClass().getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            fieldMap.put(extractCodeFromFieldName(field.getName()), field);
        }
        return fieldMap;
    }

    /**
     * 娴犲骸鐡у▓闈涙倳閹绘劕褰囨禒锝囩垳
     * 娓氬顩? colorVar -> Color, tShirt11Var -> TShirt11
     * 
     * @param fieldName 鐎涙顔岄崥?
     * @return 閹绘劕褰囬惃鍕敩閻緤绱濇俊鍌涚亯鐎涙顔岄崥宥勭瑝娴?VarImpl"缂佹挸鐔崚娆掔箲閸ョ偛甯€涙顔岄崥?
     */
    private String extractCodeFromFieldName(String fieldName) {
        // 缁夊娅庨張顐㈢啲閻?VarImpl"
        if (fieldName.endsWith("VarImpl")) {
            return fieldName.substring(0, fieldName.length() - "VarImpl".length());
        }
        if (fieldName.endsWith("Var")) {
            return fieldName.substring(0, fieldName.length() - "Var".length());
        }
        return fieldName;
    }

    /**
     * 闂堟瑦鈧焦鏌熷▔鏇窗娴犲孩瀵氱€规艾鈧吋鏆熺紒鍕灡瀵ょ儤鏆ｉ弫鏉垮綁闁?
     * 
     * @param model  CP濡€崇€风€圭偘绶?
     * @param values 閸忎浇顔忛惃鍕偓鍏兼殶缂?
     * @param name   閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    public static IntVar newIntVarFromDomain(AlgCPModel model, long[] values, String name) {
        return model.newIntVarFromDomain(Domain.fromValues(values), name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewIntVar閺傝纭?
     * 
     * @param left  閺堚偓鐏忓繐鈧?
     * @param right 閺堚偓婢堆冣偓?
     * @param name  閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    protected IntVar newIntVar(long left, long right, String name) {
        return this.model.newIntVar(left, right, name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewIntVarFromDomain閺傝纭?- 閸楁洑閲滈崐?
     * 
     * @param value 閸楁洑閲滈崐?
     * @param name  閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    protected IntVar newIntVarFromDomain(long value, String name) {
        return this.model.newIntVarFromDomain(value, name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewIntVarFromDomain閺傝纭?- 婢舵矮閲滈崐?
     * 
     * @param values 閸忎浇顔忛惃鍕偓鍏兼殶缂?
     * @param name   閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    protected IntVar newIntVarFromDomain(long[] values, String name) {
        return this.model.newIntVarFromDomain(values, name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewIntVarFromDomain閺傝纭?- 閸栨椽妫?
     * 
     * @param intervals 閸栨椽妫块弫鎵矋
     * @param name      閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    protected IntVar newIntVarFromDomain(long[][] intervals, String name) {
        return this.model.newIntVarFromDomain(intervals, name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewIntVarFromDomain閺傝纭?- 鐎瑰本鏆ｉ崺?
     * 
     * @param name 閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕殻閺佹澘褰夐柌?
     */
    protected IntVar newIntVarFromDomain(String name) {
        return this.model.newIntVarFromDomain(name);
    }

    /**
     * 鐏忎浇顥奀pModel閻ㄥ埖ewBoolVar閺傝纭?
     * 
     * @param name 閸欐﹢鍣洪崥宥囆?
     * @return 閸掓稑缂撻惃鍕鐏忔柨褰夐柌?
     */
    protected BoolVar newBoolVar(String name) {
        return this.model.newBoolVar(name);
    }

    /**
     * 閼惧嘲褰嘋P缁撅附娼Ч鍌澬掑Ο鈥崇€风€圭偘绶?
     * 
     * @return CP缁撅附娼Ч鍌澬掑Ο鈥崇€风€圭偘绶?
     */
    public AlgCPModel getModel() {
        return model;
    }

    /**
     * 閼惧嘲褰囧Ο鈥虫健鐎电钖?
     *
     * @return 濡€虫健鐎电钖?
     */
    public IModule getModule() {
        return module;
    }

    public ParaVarImpl getParaVar(String code) {
        return paraMap.get(code);
    }

    public ParaVarImpl getSumSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumSumParaByAttr'");
    }

    public ParaVarImpl getSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumParaByAttr'");
    }

    /**
     * 閼惧嘲褰囬柈銊ゆ閸欐﹢鍣?
     * 
     * @param code 閸欐﹢鍣烘禒锝囩垳
     * @return 闁劋娆㈤崣姗€鍣虹€圭偘绶?
     * @throws AlgLoaderException 瀵倸鐖?
     */
    public PartVarImpl getPartVar(String code) {
        return partMap.get(code);
    }

    /**
     * Return list of atomic parts used by sum4Selected, optionally filtered by
     * condition.
     *
     * @param filtedConditionStr filter expression, may be null
     * @return filtered list of atomic parts
     */
    public List<PartVarImpl> getInternalPartVars(String filtedConditionStr) {
        List<Part> atomicParts = currentModule.getAtomicParts();
        if (filtedConditionStr != null && !filtedConditionStr.trim().isEmpty()) {
            atomicParts = FilterExpressionExecutor.doSelect(atomicParts, filtedConditionStr);
            log.info("Priority-Filtered parts: {} in getPartVars", PartUtils.toShortString(atomicParts));
        }
        List<PartVarImpl> partVars = new ArrayList<>();
        for (Part part : atomicParts) {
            partVars.add(currentModuleAlg.getPartVar(part.getCode()));
        }
        return partVars;
    }

    /**
     * 閸掓稑缂撻崣鍌涙殶闁銆嶉崣姗€鍣?
     * 
     * @param paraCode   閸欏倹鏆熸禒锝囩垳
     * @param optionCode 闁銆嶆禒锝囩垳
     * @return 閸掓稑缂撻惃鍕棘閺佷即鈧銆嶉崣姗€鍣?
     */
    protected ParaOptionVarImpl createParaOptionVar(String paraCode, String optionCode) {
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
        ParaOptionVarImpl optionVar = new ParaOptionVarImpl(option);
        optionVar.setIsSelectedVar(newBoolVar(f(ParaVarImpl.OPTIONS_PATTERN, ipf, paraCode, option.getCode())));
        return optionVar;
    }

    /**
     * 閸忕厧顔愰幀褑顫夐崚娆欑窗Requires閸忓磭閮寸痪锔芥将
     * 鐟欏嫬鍨崘鍛啇閿涙艾顩ч弸婊冧箯娓氀冨棘閺佷即鈧瀚ㄩ幐鍥х暰闁銆嶉敍灞藉灟閸欏厖鏅堕崣鍌涙殶韫囧懘銆忛柅澶嬪閹稿洤鐣鹃柅澶愩€?
     * 娓氬顩ч敍?a1,a3) Requires (b1,b2,b3) 鐞涖劎銇氭俊鍌涚亯A闁瀚╝1閹存溂3閿涘苯鍨疊韫囧懘銆忛柅澶嬪b1閵嗕攻2閹存溇3
     * 
     * @param ruleCode                   鐟欏嫬鍨禒锝囩垳
     * @param leftParaVar                瀹革缚鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param leftParaFilterOptionCodes  瀹革缚鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     * @param rightParaVar               閸欏厖鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param rightParaFilterOptionCodes 閸欏厖鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     */
    public void addCompatibleConstraintRequires(String ruleCode, ParaVarImpl leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVarImpl rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(leftParaVar);
        // right:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(rightParaVar);

        // 鐎规矮绠熷锔挎櫠閺夆€叉閿涙艾涔忔笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 鐎规矮绠熼崣鍏呮櫠閺夆€叉閿涙艾褰告笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 鐎圭偟骞嘡equires閸忓磭閮撮敍姘洤閺嬫粌涔忔笟褎娼禒鏈佃礋true閿涘苯鍨崣鍏呮櫠閺夆€叉韫囧懘銆忔稉绨峳ue
        model.addImplication(leftCond, rightCond);
    }

    /**
     * 濞ｈ濮為柈銊ゆ閺佷即鍣洪惄鍝ョ搼缁撅附娼?
     * 閺嶈宓乸artCode閹垫儳鍩岀€电懓绨查惃鍒綼rtVar閿涘苯鑻熷ǎ璇插閺佷即鍣洪惄鍝ョ搼缁撅附娼?
     * 
     * @param partCode     闁劋娆㈡禒锝囩垳
     * @param partQuantity 閺堢喐婀滈惃鍕劥娴犺埖鏆熼柌?
     * @throws AlgLoaderException 瀵倸鐖?
     */
    public void addPartEquality(String partCode, int partQuantity) {
        // 鐠佸墽鐤嗚ぐ鎾冲閺夋儳绱遍崣姗€鍣洪崥宥囆?
        this.model.setRelax4SysRule("addPartEquality_" + partCode + "_" + partQuantity);
        // 1. 閺嶈宓乸artCode閹垫儳鍩岀€电懓绨查惃鍒綼rtVar
        PartVarImpl partVar = partMap.get(partCode);
        if (partVar == null) {
            log.error("PartVarImpl not found for code: {}", partCode);
            throw new AlgLoaderException("PartVarImpl not found for code: " + partCode);
        }
        // 2. 娴ｈ法鏁odel.addEquality濞ｈ濮為弫浼村櫤缁撅附娼?
        model.addEquality(partVar.getQty(), partQuantity);
    }

    /**
     * 濞ｈ濮為崣鍌涙殶閻╁摜鐡戠痪锔芥将
     * 
     * @param paraCode  閸欏倹鏆熸禒锝囩垳
     * @param paraValue 閸欏倹鏆熼崐?
     * @throws AlgLoaderException 瀵倸鐖?
     */
    public void addParaEquality(String paraCode, String paraValue) {
        // 鐠佸墽鐤嗚ぐ鎾冲閺夋儳绱遍崣姗€鍣洪崥宥囆?
        this.model.setRelax4SysRule("addParaEquality_" + paraCode + "_" + paraValue);
        ParaVarImpl paraVar = paraMap.get(paraCode);
        if (paraVar == null) {
            log.error("ParaVarImpl not found for code: {}", paraCode);
            throw new AlgLoaderException("ParaVarImpl not found for code: " + paraCode);
        }
        model.addEquality(paraVar.getValue(), Integer.parseInt(paraValue));
    }

    /**
     * 閸忕厧顔愰幀褑顫夐崚娆欑窗CoDependent閸忓磭閮寸痪锔芥将
     * 鐟欏嫬鍨崘鍛啇閿涙艾寮婚崥鎴滅贩鐠ф牕鍙х化浼欑礉瀹革缚鏅堕崣鍌涙殶閸滃苯褰告笟褍寮弫鏉跨箑妞よ婀€电懓绨查惃鍕矋閸愬懏鍨ㄧ紒鍕樆
     * 娓氬顩ч敍?a1,a3) CoDependent (b1,b2,b3) 鐞涖劎銇氶敍?
     * - 婵″倹鐏堿闁瀚╝1閹存溂3閿涘苯鍨疊韫囧懘銆忛柅澶嬪b1閵嗕攻2閹存溇3
     * - 婵″倹鐏堿闁瀚╝2閵嗕工4閹存溂5閿涘苯鍨疊韫囧懘銆忛柅澶嬪b4閹存溇5
     * - 婵″倹鐏塀闁瀚╞1閵嗕攻2閹存溇3閿涘苯鍨疉韫囧懘銆忛柅澶嬪a1閹存溂3
     * - 婵″倹鐏塀闁瀚╞4閹存溇5閿涘苯鍨疉韫囧懘銆忛柅澶嬪a2閵嗕工4閹存溂5
     * 
     * @param ruleCode                   鐟欏嫬鍨禒锝囩垳
     * @param leftParaVar                瀹革缚鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param leftParaFilterOptionCodes  瀹革缚鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     * @param rightParaVar               閸欏厖鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param rightParaFilterOptionCodes 閸欏厖鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     */
    public void addCompatibleConstraintCoDependent(String ruleCode, ParaVarImpl leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVarImpl rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(leftParaVar);
        // right:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(rightParaVar);

        // 鐎规矮绠熷锔挎櫠閺夆€叉閿涙艾涔忔笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 鐎规矮绠熼崣鍏呮櫠閺夆€叉閿涙艾褰告笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 鐎规矮绠熷锔挎櫠闂堢偞娼禒璁圭窗瀹革缚鏅堕梿鍡楁値婢舵牞鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬
        BoolVar leftNotCond = createNotSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                "leftNotCond");

        // 鐎规矮绠熼崣鍏呮櫠闂堢偞娼禒璁圭窗閸欏厖鏅堕梿鍡楁値婢舵牞鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬
        BoolVar rightNotCond = createNotSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes,
                "rightNotCond");

        // 鐎圭偟骞嘋oDependent閸欏苯鎮滈崗宕囬兇閿?
        // 濮濓絽鎮?.1閿涙艾顩ч弸婊冧箯娓氀勬蒋娴犳湹璐焧rue閿涘苯鍨崣鍏呮櫠閺夆€叉韫囧懘銆忔稉绨峳ue
        model.addImplication(leftCond, rightCond);

        // 濮濓絽鎮?.2閿涙艾顩ч弸婊冧箯娓氀囨姜閺夆€叉娑撶皪rue閿涘苯鍨崣鍏呮櫠闂堢偞娼禒璺虹箑妞よ璐焧rue
        model.addImplication(leftNotCond, rightNotCond);

        // 閸欏秴鎮?.1閿涙艾顩ч弸婊冨礁娓氀勬蒋娴犳湹璐焧rue閿涘苯鍨锔挎櫠閺夆€叉韫囧懘銆忔稉绨峳ue
        model.addImplication(rightCond, leftCond);

        // 閸欏秴鎮?.2閿涙艾顩ч弸婊冨礁娓氀囨姜閺夆€叉娑撶皪rue閿涘苯鍨锔挎櫠闂堢偞娼禒璺虹箑妞よ璐焧rue
        model.addImplication(rightNotCond, leftNotCond);
    }

    /**
     * 閸忕厧顔愰幀褑顫夐崚娆欑窗Incompatible閸忓磭閮寸痪锔芥将
     * 鐟欏嫬鍨崘鍛啇閿涙艾寮婚崥鎴滅瑝閸忕厧顔愰崗宕囬兇閿涘苯涔忔笟褍寮弫鏉挎嫲閸欏厖鏅堕崣鍌涙殶娑撳秷鍏橀崷銊ヮ嚠鎼存梻娈戠紒鍕敶閸氬本妞傜€涙ê婀?
     * 娓氬顩ч敍?a1,a3) Incompatible (b1,b2,b3) 鐞涖劎銇氶敍?
     * - 婵″倹鐏堿闁瀚╝1閹存溂3閿涘苯鍨疊娑撳秷鍏橀柅澶嬪b1閵嗕攻2閹存溇3閿涘牆绻€妞ゅ鈧瀚╞4閹存溇5閿?
     * - 婵″倹鐏堿闁瀚╝2閵嗕工4閹存溂5閿涘苯鍨疊閸欘垯浜掗柅澶嬪娴犵粯鍓伴崐?
     * - 婵″倹鐏塀闁瀚╞1閵嗕攻2閹存溇3閿涘苯鍨疉娑撳秷鍏橀柅澶嬪a1閹存溂3閿涘牆绻€妞ゅ鈧瀚╝2閵嗕工4閹存溂5閿?
     * - 婵″倹鐏塀闁瀚╞4閹存溇5閿涘苯鍨疉閸欘垯浜掗柅澶嬪娴犵粯鍓伴崐?
     * 
     * @param ruleCode                   鐟欏嫬鍨禒锝囩垳
     * @param leftParaVar                瀹革缚鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param leftParaFilterOptionCodes  瀹革缚鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     * @param rightParaVar               閸欏厖鏅堕崣鍌涙殶閸欐﹢鍣?
     * @param rightParaFilterOptionCodes 閸欏厖鏅堕崣鍌涙殶鏉╁洦鎶ら柅澶愩€嶆禒锝囩垳閸掓銆?
     */
    public void addCompatibleConstraintInCompatible(String ruleCode, ParaVarImpl leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVarImpl rightParaVar, List<String> rightParaFilterOptionCodes) {
        // left:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(leftParaVar);
        // right:绾喕绻氶崣顏呮箒娑撯偓娑擃亜寮弫浼粹偓澶愩€嶇悮顐︹偓澶夎厬
        addExactlyOneConstraint(rightParaVar);

        // 鐎规矮绠熷锔挎櫠閺夆€叉閿涙艾涔忔笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar leftCond = createSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes, "leftCond");

        // 鐎规矮绠熼崣鍏呮櫠閺夆€叉閿涙艾褰告笟褔娉﹂崥鍫滆厬閼峰啿鐨稉鈧稉顏囶潶闁鑵?
        BoolVar rightCond = createSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes, "rightCond");

        // 鐎规矮绠熷锔挎櫠闂堢偞娼禒璁圭窗瀹革缚鏅堕梿鍡楁値婢舵牞鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬
        BoolVar leftNotCond = createNotSelectedCondition(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                "leftNotCond");

        // 鐎规矮绠熼崣鍏呮櫠闂堢偞娼禒璁圭窗閸欏厖鏅堕梿鍡楁値婢舵牞鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬
        BoolVar rightNotCond = createNotSelectedCondition(ruleCode, rightParaVar, rightParaFilterOptionCodes,
                "rightNotCond");

        // 鐎圭偟骞嘔ncompatible閸欏苯鎮滈崗宕囬兇閿?
        // 濮濓絽鎮?.1閿涙艾顩ч弸婊冧箯娓氀勬蒋娴犳湹璐焧rue閿涘苯鍨崣鍏呮櫠閺夆€叉韫囧懘銆忔稉绡篴lse閿涘牆褰告笟褔娼弶鈥叉韫囧懘銆忔稉绨峳ue閿?
        model.addImplication(leftCond, rightNotCond);

        // 濮濓絽鎮?.2閿涙艾顩ч弸婊冧箯娓氀囨姜閺夆€叉娑撶皪rue閿涘苯鍨崣鍏呮櫠閸欘垯浜掗弰顖欐崲閹板繐鈧》绱欓弮鐘靛閺夌噦绱?
        // 鏉╂瑤閲滅痪锔芥将娑撳秹娓剁憰浣规▔瀵繑鍧婇崝鐙呯礉閸ョ姳璐熸妯款吇閸忎浇顔?

        // 閸欏秴鎮?.1閿涙艾顩ч弸婊冨礁娓氀勬蒋娴犳湹璐焧rue閿涘苯鍨锔挎櫠閺夆€叉韫囧懘銆忔稉绡篴lse閿涘牆涔忔笟褔娼弶鈥叉韫囧懘銆忔稉绨峳ue閿?
        model.addImplication(rightCond, leftNotCond);

        // 閸欏秴鎮?.2閿涙艾顩ч弸婊冨礁娓氀囨姜閺夆€叉娑撶皪rue閿涘苯鍨锔挎櫠閸欘垯浜掗弰顖欐崲閹板繐鈧》绱欓弮鐘靛閺夌噦绱?
        // 鏉╂瑤閲滅痪锔芥将娑撳秹娓剁憰浣规▔瀵繑鍧婇崝鐙呯礉閸ョ姳璐熸妯款吇閸忎浇顔?
    }

    /**
     * 鐎涙顑佹稉鍙夌壐瀵繐瀵插銉ュ徔閺傝纭?
     * 
     * @param fstr   閺嶇厧绱￠崠鏍х摟缁楋缚瑕?
     * @param values 閸欏倹鏆熼崐?
     * @return 閺嶇厧绱￠崠鏍ф倵閻ㄥ嫬鐡х粭锔胯
     */
    private String f(String fstr, String... values) {
        return String.format(Locale.ROOT, fstr, (Object[]) values);
    }

    /**
     * 閸掓稑缂撶€涙顑佹稉鎻掑灙鐞涖劎娈戝銉ュ徔閺傝纭?
     * 鐏忎浇顥夾rrays.asList閿涘本褰佹笟娑欐纯缁犫偓濞蹭胶娈慉PI
     * 
     * @param codes 鐎涙顑佹稉鍙夋殶缂?
     * @return List<String>
     */
    protected List<String> listOf(String... codes) {
        return Arrays.asList(codes);
    }

    /**
     * 娑撳搫寮弫鏉垮綁闁插繑鍧婇崝?绾喕绻氶崣顏呮箒娑撯偓娑擃亪鈧銆嶇悮顐︹偓澶夎厬"閻ㄥ嫮瀹抽弶?
     * 
     * @param paraVar 閸欏倹鏆熼崣姗€鍣?
     */
    private void addExactlyOneConstraint(ParaVarImpl paraVar) {
        model.addExactlyOne(paraVar.getOptionSelectVars().values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));
    }

    /**
     * 閸掓稑缂?闂嗗棗鎮庢稉顓″殾鐏忔垳绔存稉顏囶潶闁鑵?閻ㄥ嫭娼禒璺哄綁闁插繐鎷扮痪锔芥将
     * 
     * @param ruleCode          鐟欏嫬鍨禒锝囩垳
     * @param paraVar           閸欏倹鏆熼崣姗€鍣?
     * @param filterOptionCodes 鏉╁洦鎶ら惃鍕偓澶愩€嶆禒锝囩垳閸掓銆?
     * @param conditionSuffix   閺夆€叉閸氬海绱戦敍鍫濐洤"leftCond"閵?rightCond"閿?
     * @return 閺夆€叉閸欐﹢鍣?
     */
    private BoolVar createSelectedCondition(String ruleCode, ParaVarImpl paraVar,
            List<String> filterOptionCodes, String conditionSuffix) {
        // 鐎规矮绠熼弶鈥叉閸欐﹢鍣?
        BoolVar condition = newBoolVar(f("%s_%s", ruleCode, conditionSuffix));

        // 閼惧嘲褰囬柅澶夎厬閻ㄥ嫰鈧銆?
        Literal[] selected = paraVar.getOptionSelectVars().values().stream()
                .filter(option -> filterOptionCodes.contains(option.getCode()))
                .map(option -> option.getIsSelectedVar())
                .toArray(Literal[]::new);

        // 瑜版挻娼禒鏈佃礋true閺冭绱濋梿鍡楁値娑擃叀鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬閿涙稑缍嬫稉绡篴lse閺冭绱濋梿鍡楁値閸忋劑鍎存稉宥堫潶闁鑵?
        model.addBoolOr(selected).onlyEnforceIf(condition);
        model.addBoolAnd(Arrays.stream(selected).map(Literal::not).toArray(Literal[]::new))
                .onlyEnforceIf(condition.not());

        return condition;
    }

    /**
     * 閸掓稑缂?闂嗗棗鎮庢径鏍殾鐏忔垳绔存稉顏囶潶闁鑵?閻ㄥ嫭娼禒璺哄綁闁插繐鎷扮痪锔芥将
     * 
     * @param ruleCode          鐟欏嫬鍨禒锝囩垳
     * @param paraVar           閸欏倹鏆熼崣姗€鍣?
     * @param filterOptionCodes 鏉╁洦鎶ら惃鍕偓澶愩€嶆禒锝囩垳閸掓銆冮敍鍫ｇ箹娴滄盯鈧銆嶆稉宥呮躬闂嗗棗鎮庨崘鍜冪礆
     * @param conditionSuffix   閺夆€叉閸氬海绱戦敍鍫濐洤"leftNotCond"閵?rightNotCond"閿?
     * @return 閺夆€叉閸欐﹢鍣?
     */
    private BoolVar createNotSelectedCondition(String ruleCode, ParaVarImpl paraVar,
            List<String> filterOptionCodes, String conditionSuffix) {
        // 鐎规矮绠熼弶鈥叉閸欐﹢鍣?
        BoolVar condition = newBoolVar(f("%s_%s", ruleCode, conditionSuffix));

        // 閼惧嘲褰囬梿鍡楁値婢舵牠鈧鑵戦惃鍕偓澶愩€?
        Literal[] notSelected = paraVar.getOptionSelectVars().values().stream()
                .filter(option -> !filterOptionCodes.contains(option.getCode()))
                .map(option -> option.getIsSelectedVar())
                .toArray(Literal[]::new);

        // 瑜版挻娼禒鏈佃礋true閺冭绱濋梿鍡楁値婢舵牞鍤︾亸鎴滅娑擃亣顫﹂柅澶夎厬閿涙稑缍嬫稉绡篴lse閺冭绱濋梿鍡楁値婢舵牕鍙忛柈銊ょ瑝鐞氼偊鈧鑵?
        model.addBoolOr(notSelected).onlyEnforceIf(condition);
        model.addBoolAnd(Arrays.stream(notSelected).map(Literal::not).toArray(Literal[]::new))
                .onlyEnforceIf(condition.not());

        return condition;
    }

    /**
     * 鐠佸墽鐤嗛柈銊ゆ娑撶儤婀柅澶夎厬閻樿埖鈧緤绱欓幍褰掑櫤閿?
     * 闁秴宸荤拫鍐暏 setPartUnSelected(Part part)
     *
     * @param parts 闁劋娆㈤崚妤勩€?
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
     * 鐠佸墽鐤嗛柈銊ゆ娑撶儤婀柅澶夎厬閻樿埖鈧?
     * 閺嶈宓?part.code 閹垫儳鍩岀€电懓绨?partVar閿涘奔濞囬悽?ortools 鐠佸墽鐤嗛敍?
     * - partVar.isSelected == 0
     * - partVar.qty == 0
     *
     * @param part 闁劋娆?
     */
    public void setPartUnSelected(Part part) {
        if (part == null) {
            log.warn("Part is null, cannot set unselected");
            return;
        }

        // 閺嶈宓?part.code 閹垫儳鍩岀€电懓绨?partVar
        PartVarImpl partVar = getPartVar(part.getCode());

        // 娴ｈ法鏁?ortools 鐠佸墽鐤嗙痪锔芥将
        // partVar.isSelected == 0
        model.addEquality(partVar.getIsSelected(), 0);

        // partVar.qty == 0
        model.addEquality(partVar.getQty(), 0);

        log.info("Set part unselected: code={}, isSelected=0, qty=0", part.getCode());
    }

    public List<PartVarImpl> getAllPartVars(String filterConditionStr) {
        if (filterConditionStr == null || filterConditionStr.isEmpty()) {
            return new ArrayList<>(partMap.values());
        } else {
            return FilterExpressionExecutor.doSelect(new ArrayList<>(partMap.values()), filterConditionStr);
        }
    }

    /**
     * 鏉╁洦鎶ら幍鈧張澶愬劥娴犺泛褰夐柌?
     * 
     * @param filterConditionStr
     * @return 缁楊兛绔存稉顏呮Ц鏉╁洦鎶ら崥搴礉缁楊兛绔存稉顏呯梾閺堝绻冨銈囨畱
     */
    public Pair<List<PartVarImpl>, List<PartVarImpl>> filterAllPartVars(String filterConditionStr) {
        List<PartVarImpl> filterPartVars = getAllPartVars(filterConditionStr);
        Set<String> filterPartVarCodes = filterPartVars.stream().map(PartVarImpl::getCode).collect(Collectors.toSet());
        List<PartVarImpl> noFilterPartVars = new ArrayList<>();
        for (PartVarImpl partVar : partMap.values()) {
            if (!filterPartVarCodes.contains(partVar.getCode())) {
                noFilterPartVars.add(partVar);
            }
        }
        return Pair.of(filterPartVars, noFilterPartVars);
    }

    public String toString() {
        return this.module.getCode();
    }

    // ==================== POST instance view binding ====================

    public void bindModuleInstView(ModuleInstView view) {
        this.currentModuleInstView = view;
    }

    public void clearModuleInstView() {
        this.currentModuleInstView = null;
    }

    protected ModuleInstView currentModuleInstView() {
        if (currentModuleInstView == null) {
            throw new AlgLoaderException(
                    "ModuleInstView is not bound. This method is only available in POST context.");
        }
        return currentModuleInstView;
    }

    public String getDynAttr(String partCategoryCode, String attrCode) {
        return currentModuleInstView().partCategorySum(partCategoryCode).dynAttr(attrCode);
    }

    public String getDynAttr(String partCategoryCode, int instId, String attrCode) {
        return currentModuleInstView().partCategory(partCategoryCode, instId).dynAttr(attrCode);
    }

    public List<String> getDynAttrValues(String partCategoryCode, String attrCode) {
        return currentModuleInstView().partCategorySum(partCategoryCode).dynAttrs(attrCode);
    }

    public List<String> getDynAttrValues(String partCategoryCode, int instId, String attrCode) {
        return currentModuleInstView().partCategory(partCategoryCode, instId).parts().stream()
                .filter(part -> part.selected() || part.quantity() > 0)
                .map(part -> part.dynAttr(attrCode))
                .collect(Collectors.toList());
    }

    public String getSumDynAttr(String partCategoryCode, String attrCode) {
        return currentModuleInstView().partCategorySum(partCategoryCode).sumDynAttr(attrCode);
    }

    public String getSumDynAttr(String partCategoryCode, int instId, String attrCode) {
        int sum = currentModuleInstView().partCategory(partCategoryCode, instId).parts().stream()
                .filter(part -> part.selected() || part.quantity() > 0)
                .map(part -> toInt(part.dynAttr(attrCode)) * part.quantity())
                .reduce(0, Integer::sum);
        return String.valueOf(sum);
    }

    public int getQuantity(String partCategoryCode) {
        return currentModuleInstView().partCategory(partCategoryCode).sumQuantity();
    }

    public int getQuantity(String partCategoryCode, int instId) {
        return currentModuleInstView().partCategory(partCategoryCode, instId).sumQuantity();
    }

    public List<Integer> getInstanceIds(String partCategoryCode) {
        return currentModuleInstView().partCategorySum(partCategoryCode).insts().stream()
                .map(inst -> inst.instanceId())
                .collect(Collectors.toList());
    }

    public void setParaValue(String paraCode, String value) {
        currentModuleInstView().parameter(paraCode).setValue(value);
    }

    public void setParaValue(String partCategoryCode, String paraCode, String value) {
        currentModuleInstView().partCategory(partCategoryCode).parameter(paraCode).setValue(value);
    }

    public void setParaValue(String partCategoryCode, int instId, String paraCode, String value) {
        currentModuleInstView().partCategory(partCategoryCode, instId).parameter(paraCode).setValue(value);
    }

    public String getParaValue(String paraCode) {
        return currentModuleInstView().parameter(paraCode).value();
    }

    public String getParaValue(String partCategoryCode, String paraCode) {
        return currentModuleInstView().partCategory(partCategoryCode).parameter(paraCode).value();
    }

    public String getParaValue(String partCategoryCode, int instId, String paraCode) {
        return currentModuleInstView().partCategory(partCategoryCode, instId).parameter(paraCode).value();
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
