package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Extensible;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaOption;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.imodel.ProgrammableObject;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.imodel.rule.CompatiableRuleSchema;
import com.jmix.executor.imodel.rule.ExprSchema;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.imodel.rule.RuleSchema;
import com.jmix.executor.imodel.rule.RuleTypeConstants;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * 模块算法制品生成器
 * 每个Module都需要新建一个实例来处理
 * 
 * generator.buildConstraintRule(module, "output/constraint.java");
 * </pre>
 *
 * 主要特性：
 * 1. 每个Module都需要新建一个实例，避免状态混乱
 * 2. 提供paraMaps和partMaps映射表，简化内部处理
 * 3. 支持快速查找参数和部件
 * 4. 提供便利的getter方法访问内部状态
 */
@Slf4j
public class ModuleAlgArtifactGenerator {
    private final Configuration freemarkerConfig;

    // 当前处理的模块信息
    private ModuleVarInfo moduleVarInfo;

    /**
     * 默认构造函数
     * 初始化FreeMarker配置
     */
    public ModuleAlgArtifactGenerator() {
        // 初始化FreeMarker配置
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "template");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    /**
     * 构建约束规则
     * 
     * @param module     模块对象
     * @param outputPath 输出文件路径
     * @throws Exception 生成过程中的异常
     */
    public void buildConstraintRule(Module module, String outputPath) throws Exception {
        log.info("Starting constraint rule generation for module: {}", module.getCode());

        // 根据Module生成moduleInfo
        moduleVarInfo = buildModuleInfo(module);

        // 根据moduleInfo和ModuleConstraintTemplate.ftl，使用freemarker生成算法文件
        generateConstraintCode(moduleVarInfo, outputPath);

        log.info("Constraint rule generation completed, output path: {}", outputPath);
    }

    /**
     * 构建模块信息
     * 
     * @param module 模块对象
     * @return 构建的模块变量信息
     */
    public ModuleVarInfo buildModuleInfo(Module module) {
        ModuleVarInfo moduleVarInfo = buildModuleInfoBase(module);

        // 调用buildRules生成RuleInfos
        if (module.getRules() != null) {
            List<RuleInfo> ruleInfos = buildRules(moduleVarInfo);
            moduleVarInfo.setRules(ruleInfos);
        }

        return moduleVarInfo;
    }

    /**
     * 构建模块信息基础数据
     * 
     * @param module 模块对象
     * @return 构建的模块变量信息
     */
    public ModuleVarInfo buildModuleInfoBase(Module module) {
        ModuleVarInfo moduleVarInfo = new ModuleVarInfo(module);
        moduleVarInfo.setCode(module.getCode());
        moduleVarInfo.setVarName(module.getCode() + "Var");
        moduleVarInfo.setPackageName(module.getPackageName());

        // 根据Para生成ParaVarInfo
        if (module.getParas() != null) {
            List<ParaVarInfo> paraInfos = module.getParas().stream()
                    .map(this::buildParaInfo)
                    .collect(Collectors.toList());
            moduleVarInfo.setParas(paraInfos);

        }

        // 根据Part生成PartVarInfo
        if (module.getParts() != null) {
            List<PartVarInfo> partInfos = module.getParts().stream()
                    .map(this::buildPartInfo)
                    .collect(Collectors.toList());
            moduleVarInfo.setParts(partInfos);
        }
        return moduleVarInfo;
    }

    /**
     * 构建参数信息
     */
    private ParaVarInfo buildParaInfo(Para para) {
        ParaVarInfo paraInfo = new ParaVarInfo(para);
        paraInfo.setCode(para.getCode());
        paraInfo.setVarName(para.getCode() + "Var");

        // 根据ParaOption.codeId生成domain
        if (para.getOptions() != null) {
            // 构建ParaOptionVarInfo
            List<ParaOptionVarInfo> optionInfos = para.getOptions().stream()
                    .map(this::buildParaOptionInfo)
                    .collect(Collectors.toList());
            paraInfo.setOptions(optionInfos);
        }

        return paraInfo;
    }

    /**
     * 构建参数选项信息
     */
    private ParaOptionVarInfo buildParaOptionInfo(ParaOption option) {
        ParaOptionVarInfo optionInfo = new ParaOptionVarInfo(option);
        optionInfo.setCodeId(option.getCodeId());
        optionInfo.setCode(option.getCode());
        optionInfo.setVarName(option.getCode() + "_" + option.getCodeId() + "_selectVar");
        return optionInfo;
    }

    /**
     * 构建部件信息
     */
    private PartVarInfo buildPartInfo(Part part) {
        PartVarInfo partInfo = new PartVarInfo(part);
        partInfo.setCode(part.getCode());
        partInfo.setFatherCode(part.getFatherCode());
        partInfo.setVarName(part.getCode() + "Var");
        return partInfo;
    }

    /**
     * 构建规则列表
     */
    private List<RuleInfo> buildRules(ModuleVarInfo moduleInfo) {
        return moduleInfo.getBase().getRules().stream()
                .map(rule -> buildRule(moduleInfo, rule))
                .collect(Collectors.toList());
    }

    /**
     * 构建单个规则信息
     * 
     * @param moduleInfo 模块信息
     * @param rule       规则对象
     * @return 构建的规则信息
     */
    public RuleInfo buildRule(ModuleVarInfo moduleInfo, Rule rule) {
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setCode(rule.getCode());
        ruleInfo.setRuleSchemaTypeFullName(rule.getRuleSchemaTypeFullName());
        ruleInfo.setName(rule.getName());
        ruleInfo.setNormalNaturalCode(rule.getNormalNaturalCode());

        // 根据ruleSchemaTypeFullName分函数处理
        try {
            if (RuleTypeConstants.isCompatiableRule(rule.getRuleSchemaTypeFullName())) {
                buildCompatiableRule(ruleInfo, moduleInfo, rule);
            } else if (RuleTypeConstants.isCalculateRule(rule.getRuleSchemaTypeFullName())) {
                buildCalculateRule(ruleInfo, moduleInfo, rule);
            } else if (RuleTypeConstants.isSelectRule(rule.getRuleSchemaTypeFullName())) {
                buildSelectRule(ruleInfo, moduleInfo, rule);
            }
        } catch (Exception e) {
            log.error("Failed to build rule: {}", rule.getCode(), e);
        }

        return ruleInfo;
    }

    /**
     * 构建兼容性规则
     * 
     * @param ruleInfo   规则信息
     * @param moduleInfo 模块信息
     * @param rule       规则对象
     */
    public void buildCompatiableRule(RuleInfo ruleInfo, ModuleVarInfo moduleInfo, Rule rule) {
        try {
            // 直接使用RuleSchema对象
            RuleSchema rawCode = rule.getRawCode();
            if (rawCode instanceof CompatiableRuleSchema) {
                CompatiableRuleSchema compatiableRule = (CompatiableRuleSchema) rawCode;

                // 设置兼容性操作符
                ruleInfo.setCompatiableOperator(compatiableRule.getOperator());

                // 处理左表达式
                if (compatiableRule.getLeftExpr() != null) {
                    ruleInfo.setLeftTypeName("ParaVar");
                    // 调用doSelectProObjs方法
                    Optional<Pair<VarInfo<? extends Extensible>, List<String>>> pairOpt = doSelectProObjs(
                            moduleInfo,
                            compatiableRule.getLeftExpr());
                    if (pairOpt.isPresent()) {
                        Pair<VarInfo<? extends Extensible>, List<String>> pair = pairOpt.get();
                        ruleInfo.setLeft(pair.getFirst());
                        ruleInfo.setLeftFilterCodes(pair.getSecond());
                        // 设置左变量名称
                        ruleInfo.setLeftVarName(pair.getFirst().getVarName());
                    } else {
                        log.error("Failed to select programming objects for rule: {}", rule.getCode());
                        throw new AlgLoaderException(
                                "Failed to select programming objects for rule: " + rule.getCode());
                    }
                }

                // 处理右表达式
                if (compatiableRule.getRightExpr() != null) {
                    ruleInfo.setRightTypeName("ParaVar");
                    // 调用doSelectProObjs方法
                    Optional<Pair<VarInfo<? extends Extensible>, List<String>>> pairOpt = doSelectProObjs(
                            moduleInfo,
                            compatiableRule.getRightExpr());
                    if (pairOpt.isPresent()) {
                        Pair<VarInfo<? extends Extensible>, List<String>> pair = pairOpt.get();
                        ruleInfo.setRight(pair.getFirst());
                        ruleInfo.setRightFilterCodes(pair.getSecond());
                        // 设置右变量名称
                        ruleInfo.setRightVarName(pair.getFirst().getVarName());
                    } else {
                        log.error("Failed to select programming objects for rule: {}", rule.getCode());
                        throw new AlgLoaderException(
                                "Failed to select programming objects for rule: " + rule.getCode());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse compatible rule: {}", rule.getCode(), e);
        }
    }

    /**
     * 构建计算规则
     * 
     * @param ruleInfo   规则信息
     * @param moduleInfo 模块信息
     * @param rule       规则对象
     */
    public void buildCalculateRule(RuleInfo ruleInfo, ModuleVarInfo moduleInfo, Rule rule) {
        ruleInfo.setLeftTypeName("PartVar");
        ruleInfo.setRightTypeName("PartVar");
    }

    /**
     * 构建选择规则
     * 
     * @param ruleInfo   规则信息
     * @param moduleInfo 模块信息
     * @param rule       规则对象
     */
    public void buildSelectRule(RuleInfo ruleInfo, ModuleVarInfo moduleInfo, Rule rule) {
        ruleInfo.setLeftTypeName("ParaVar");
        ruleInfo.setRightTypeName("ParaVar");
    }

    /**
     * 选择编程对象
     * 
     * @param moduleVarInfo 模块变量信息
     * @param exprSchema    表达式模式
     * @return 包含目标对象和过滤对象代码列表的可选结果
     */
    @SuppressWarnings("rawtypes")
    public Optional<Pair<VarInfo<? extends Extensible>, List<String>>> doSelectProObjs(
            ModuleVarInfo moduleVarInfo,
            ExprSchema exprSchema) {
        // 1. 根据exprSchema的progObjType、progObjCode、progObjField根据待过滤objects
        if (exprSchema.getRefProgObjs() == null || exprSchema.getRefProgObjs().isEmpty()) {
            log.warn("No reference programming objects found in expression schema");
            return Optional.empty();
        }

        RefProgObjSchema refProgObj = exprSchema.getRefProgObjs().get(0); // 取第一个引用对象
        String progObjType = refProgObj.getProgObjType();
        String progObjCode = refProgObj.getProgObjCode();

        VarInfo<? extends Extensible> targetObj = null;
        List<? extends Extensible> objects = null;
        String parsedRawCode = exprSchema.getRawCode();

        // 根据exprSchema.refProgObjs.progObjType = Para
        if ("Para".equals(progObjType)) {
            ParaVarInfo paraInfo = moduleVarInfo.getPara(progObjCode);
            if (paraInfo != null) {
                targetObj = paraInfo;
                Optional<Para> paraOpt = moduleVarInfo.getBase().getPara(progObjCode);
                if (paraOpt.isPresent()) {
                    objects = paraOpt.get().getOptions();
                }
                // parsedRawCode = replaceToOptionCode(exprSchema.getRawCode(), progObjCode,
                // "code");
                // 例如：sizeVar.value == Small --> sizeVar.code == Small(本质是对option的code进行搜索)
                parsedRawCode = replaceToOptionCode(exprSchema.getRawCode(), "value", "code");
            }
        } else if ("Part".equals(progObjType)) { // 根据exprSchema.refProgObjs.progObjType = Part
            PartVarInfo part = moduleVarInfo.getPart(progObjCode);
            if (part != null) {
                targetObj = part;
                objects = moduleVarInfo.getBase().getChildrenPart(progObjCode);
            }
        }

        if (targetObj == null || objects == null) {
            log.warn("Failed to find target object or objects for type: {}, code: {}", progObjType, progObjCode);
            return Optional.empty();
        }

        // 2. 调用FilterExpressionExecutor.doSelect进行过滤
        List<? extends Extensible> filterObjects = FilterExpressionExecutor.doSelect(objects, parsedRawCode);

        // 3.根据filterObjects的code，生成filterObjectCodes(每个Extensible对象强制转化为ProgrammableObject)
        List<String> filterObjectCodes = filterObjects.stream()
                .filter(obj -> obj instanceof ProgrammableObject)
                .map(obj -> ((ProgrammableObject) obj).getCode())
                .collect(Collectors.toList());

        // 4.返回结果
        Pair<VarInfo<? extends Extensible>, List<String>> result = new Pair<>(targetObj, filterObjectCodes);
        return Optional.of(result);
    }

    /**
     * 将rawCode中的progObjCode替换为指定的字段名
     */
    private String replaceToOptionCode(String rawCode, String from, String to) {
        if (rawCode == null || from == null || to == null) {
            return rawCode;
        }
        return rawCode.replace(from, to);
    }

    /**
     * 生成约束代码
     */
    private void generateConstraintCode(ModuleVarInfo moduleInfo, String outputPath) throws Exception {
        // 准备模板数据
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("module", moduleInfo);

        // 获取模板
        Template template = freemarkerConfig.getTemplate("ModuleConstraintTemplate.ftl");

        // 生成代码
        StringWriter writer = new StringWriter();
        template.process(templateData, writer);
        String generatedCode = writer.toString();

        // 写入文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            fileWriter.write(generatedCode);
        }

        log.info("Constraint code generation completed: {}", outputPath);
    }

    /**
     * 获取当前模块信息
     * 
     * @return 当前模块信息
     */
    public ModuleVarInfo getModuleVarInfo() {
        return moduleVarInfo;
    }

    /**
     * 静态工厂方法：为指定的Module创建新的生成器实例
     * 每个Module都需要新建一个实例来处理
     * 
     * @param module 模块对象
     * @return 新的生成器实例
     */
    public static ModuleAlgArtifactGenerator forModule(Module module) {
        ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
        try {
            // 只初始化内部结构，不生成代码文件
            generator.moduleVarInfo = generator.buildModuleInfo(module);
        } catch (Exception e) {
            log.warn("Failed to initialize generator for module: {}", module.getCode(), e);
        }
        return generator;
    }
}