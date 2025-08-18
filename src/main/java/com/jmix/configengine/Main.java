package com.jmix.configengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.configengine.artifact.ModuleAlgArtifactGenerator;
import com.jmix.configengine.model.*;
import com.jmix.configengine.constant.RuleTypeConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 主程序类
 */
@Slf4j
public class Main {
    
    public static void main(String[] args) {
        try {
            log.info("开始运行约束规则生成系统");
            
            // 创建T恤模块的示例数据
            com.jmix.configengine.model.Module tshirtModule = createTShirtModule();
            
            // 初始化模块
            tshirtModule.init();
            
            // 创建生成器
            ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
            
            // 生成约束代码
            String outputPath = "generated/TShirtConstraint.java";
            generator.buildConstraintRule(tshirtModule, outputPath);
            
            log.info("程序运行完成");
            
        } catch (Exception e) {
            log.error("程序运行失败", e);
        }
    }
    
    /**
     * 创建T恤模块的示例数据
     */
    private static com.jmix.configengine.model.Module createTShirtModule() {
        com.jmix.configengine.model.Module module = new com.jmix.configengine.model.Module();
        module.setCode("TShirt");
        module.setId(123123L);
        module.setVersion("V1.0");
        module.setType(ModuleType.GENERAL);
        module.setDefaultValue(1);
        module.setDescription("T恤配置模块");
        module.setSortNo(1);
        
        // 创建颜色参数
        Para colorPara = new Para();
        colorPara.setCode("Color");
        colorPara.setType(ParaType.ENUM);
        colorPara.setDefaultValue("1");
        colorPara.setDescription("颜色参数");
        colorPara.setSortNo(1);
        
        ParaOption redOption = new ParaOption();
        redOption.setCodeId(10);
        redOption.setCode("Red");
        redOption.setDescription("红色");
        
        ParaOption blackOption = new ParaOption();
        blackOption.setCodeId(20);
        blackOption.setCode("Black");
        blackOption.setDescription("黑色");
        
        ParaOption whiteOption = new ParaOption();
        whiteOption.setCodeId(30);
        whiteOption.setCode("White");
        whiteOption.setDescription("白色");
        
        colorPara.setOptions(Arrays.asList(redOption, blackOption, whiteOption));
        
        // 创建尺寸参数
        Para sizePara = new Para();
        sizePara.setCode("Size");
        sizePara.setType(ParaType.ENUM);
        sizePara.setDefaultValue("1");
        sizePara.setDescription("尺寸参数");
        sizePara.setSortNo(2);
        
        ParaOption bigOption = new ParaOption();
        bigOption.setCodeId(1);
        bigOption.setCode("Big");
        bigOption.setDescription("大号");
        
        ParaOption mediumOption = new ParaOption();
        mediumOption.setCodeId(2);
        mediumOption.setCode("Medium");
        mediumOption.setDescription("中号");
        
        ParaOption smallOption = new ParaOption();
        smallOption.setCodeId(3);
        smallOption.setCode("Small");
        smallOption.setDescription("小号");
        
        sizePara.setOptions(Arrays.asList(bigOption, mediumOption, smallOption));
        
        // 创建部件
        Part tshirt11Part = new Part();
        tshirt11Part.setCode("TShirt11");
        tshirt11Part.setType(PartType.ATOMIC);
        tshirt11Part.setDefaultValue(0);
        tshirt11Part.setDescription("T恤部件11");
        tshirt11Part.setSortNo(1);
        
        Part tshirt12Part = new Part();
        tshirt12Part.setCode("TShirt12");
        tshirt12Part.setType(PartType.ATOMIC);
        tshirt12Part.setDefaultValue(0);
        tshirt12Part.setDescription("T恤部件12");
        tshirt12Part.setSortNo(2);
        
        // 创建规则
        Rule rule1 = new Rule();
        rule1.setCode("rule1");
        rule1.setName("颜色和大小兼容关系");
        rule1.setProgObjType("Module");
        rule1.setProgObjCode("");
        rule1.setProgObjField("");
        rule1.setNormalNaturalCode("如果颜色是红色，则尺寸必须是大号或小号");
        rule1.setRuleSchemaTypeFullName(RuleTypeConstants.COMPATIABLE_RULE_FULL_NAME);
        // rule1.setRawCode(null); // 暂时不设置，避免类型不匹配问题
        
        Rule rule2 = new Rule();
        rule2.setCode("rule2");
        rule2.setName("部件数量关系");
        rule2.setProgObjType("Part");
        rule2.setProgObjCode("TShirt12");
        rule2.setProgObjField("value");
        rule2.setNormalNaturalCode("TShirt12的数量等于TShirt11数量的2倍");
        rule2.setRuleSchemaTypeFullName("CDSL.V5.Struct.CalculateRule");
        // rule2.setRawCode(null); // 暂时不设置，避免类型不匹配问题
        
        // 设置模块属性
        module.setParas(Arrays.asList(colorPara, sizePara));
        module.setParts(Arrays.asList(tshirt11Part, tshirt12Part));
        module.setRules(Arrays.asList(rule1, rule2));
        
        return module;
    }
} 