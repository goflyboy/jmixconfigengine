package com.jmix.configengine.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.configengine.model.*;
import com.jmix.configengine.schema.*;
import com.jmix.configengine.util.FilterExpressionExecutor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块算法制品生成器
 */
@Slf4j
public class ModuleAlgArtifactGenerator {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Configuration freemarkerConfig;
    
    public ModuleAlgArtifactGenerator() {
        // 初始化FreeMarker配置
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "template");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    /**
     * 构建约束规则
     */
    public void buildConstraintRule(com.jmix.configengine.model.Module module, String outputPath) throws Exception {
        log.info("开始生成约束规则，模块: {}", module.getCode());
        
        // 根据Module生成moduleInfo
        ModuleInfo moduleInfo = buildModuleInfo(module);
        
        // 根据moduleInfo和ModuleConstraintTemplate.ftl，使用freemarker生成算法文件
        generateConstraintCode(moduleInfo, outputPath);
        
        log.info("约束规则生成完成，输出路径: {}", outputPath);
    }
    
    /**
     * 构建模块信息
     */
    private ModuleInfo buildModuleInfo(com.jmix.configengine.model.Module module) {
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setCode(module.getCode());
        moduleInfo.setVarName(module.getCode() + "Var");
        
        // 根据Para生成ParaInfo
        if (module.getParas() != null) {
            List<ParaInfo> paraInfos = module.getParas().stream()
                    .map(this::buildParaInfo)
                    .collect(Collectors.toList());
            moduleInfo.setParas(paraInfos);
        }
        
        // 根据Part生成PartInfo
        if (module.getParts() != null) {
            List<PartInfo> partInfos = module.getParts().stream()
                    .map(this::buildPartInfo)
                    .collect(Collectors.toList());
            moduleInfo.setParts(partInfos);
        }
        
        // 调用buildRules生成RuleInfos
        if (module.getRules() != null) {
            List<RuleInfo> ruleInfos = buildRules(module);
            moduleInfo.setRules(ruleInfos);
        }
        
        return moduleInfo;
    }
    
    /**
     * 构建参数信息
     */
    private ParaInfo buildParaInfo(Para para) {
        ParaInfo paraInfo = new ParaInfo();
        paraInfo.setCode(para.getCode());
        paraInfo.setVarName(para.getCode() + "Var");
        
        // 根据ParaOption.codeId生成domain
        if (para.getOptions() != null) {
            long[] domain = para.getOptions().stream()
                    .mapToLong(ParaOption::getCodeId)
                    .toArray();
            paraInfo.setDomain(domain);
            
            // 构建ParaOptionInfo
            List<ParaOptionInfo> optionInfos = para.getOptions().stream()
                    .map(this::buildParaOptionInfo)
                    .collect(Collectors.toList());
            paraInfo.setOptions(optionInfos);
        }
        
        return paraInfo;
    }
    
    /**
     * 构建参数选项信息
     */
    private ParaOptionInfo buildParaOptionInfo(ParaOption option) {
        ParaOptionInfo optionInfo = new ParaOptionInfo();
        optionInfo.setCodeId(option.getCodeId());
        optionInfo.setCode(option.getCode());
        optionInfo.setVarName(option.getCode() + "_" + option.getCodeId() + "_selectVar");
        return optionInfo;
    }
    
    /**
     * 构建部件信息
     */
    private PartInfo buildPartInfo(Part part) {
        PartInfo partInfo = new PartInfo();
        partInfo.setCode(part.getCode());
        partInfo.setVarName(part.getCode() + "Var");
        return partInfo;
    }
    
    /**
     * 构建规则列表
     */
    private List<RuleInfo> buildRules(com.jmix.configengine.model.Module module) {
        return module.getRules().stream()
                .map(rule -> buildRule(module, rule))
                .collect(Collectors.toList());
    }
    
    /**
     * 构建单个规则
     */
    private RuleInfo buildRule(com.jmix.configengine.model.Module module, Rule rule) {
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setCode(rule.getCode());
        ruleInfo.setRuleSchema(rule.getRuleSchema());
        ruleInfo.setName(rule.getName());
        ruleInfo.setNormalNaturalCode(rule.getNormalNaturalCode());
        
        // 根据ruleSchema分函数处理
        try {
            if (rule.getRuleSchema().contains("CompatiableRule")) {
                buildCompatiableRule(ruleInfo, module, rule);
            } else if (rule.getRuleSchema().contains("CalculateRule")) {
                buildCalculateRule(ruleInfo, module, rule);
            } else if (rule.getRuleSchema().contains("SelectRule")) {
                buildSelectRule(ruleInfo, module, rule);
            }
        } catch (Exception e) {
            log.error("构建规则失败: {}", rule.getCode(), e);
        }
        
        return ruleInfo;
    }
    
    /**
     * 构建兼容规则
     */
    private void buildCompatiableRule(RuleInfo ruleInfo, com.jmix.configengine.model.Module module, Rule rule) {
        try {
            // 解析rawCode中的表达式
            Map<String, Object> rawCode = objectMapper.readValue(rule.getRawCode(), Map.class);
            
            // 处理左表达式
            Map<String, Object> leftExpr = (Map<String, Object>) rawCode.get("leftExpr");
            if (leftExpr != null) {
                ruleInfo.setLeftTypeName("ParaVar");
                // 这里简化处理，实际应该根据表达式创建VarInfo
            }
            
            // 处理右表达式
            Map<String, Object> rightExpr = (Map<String, Object>) rawCode.get("rightExpr");
            if (rightExpr != null) {
                ruleInfo.setRightTypeName("ParaVar");
                // 这里简化处理，实际应该根据表达式创建VarInfo
            }
        } catch (Exception e) {
            log.error("解析兼容规则失败: {}", rule.getCode(), e);
        }
    }
    
    /**
     * 构建计算规则
     */
    private void buildCalculateRule(RuleInfo ruleInfo, com.jmix.configengine.model.Module module, Rule rule) {
        ruleInfo.setLeftTypeName("PartVar");
        ruleInfo.setRightTypeName("PartVar");
    }
    
    /**
     * 构建选择规则
     */
    private void buildSelectRule(RuleInfo ruleInfo, com.jmix.configengine.model.Module module, Rule rule) {
        ruleInfo.setLeftTypeName("ParaVar");
        ruleInfo.setRightTypeName("ParaVar");
    }
    
    /**
     * 选择编程对象
     */
    private <F, T> Pair<F, List<T>> doSelectProObjs(ExprSchema exprSchema) {
        // 根据exprSchema选择目标对象
        // 这里简化实现，实际应该根据progObjType、progObjCode、progObjField进行选择
        return null;
    }
    
    /**
     * 生成约束代码
     */
    private void generateConstraintCode(ModuleInfo moduleInfo, String outputPath) throws Exception {
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
        
        log.info("约束代码生成完成: {}", outputPath);
    }
    
    /**
     * 简单的Pair类
     */
    public static class Pair<F, S> {
        private final F first;
        private final S second;
        
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
        
        public F getFirst() {
            return first;
        }
        
        public S getSecond() {
            return second;
        }
    }
} 