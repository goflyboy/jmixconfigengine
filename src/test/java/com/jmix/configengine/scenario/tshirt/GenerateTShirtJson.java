package com.jmix.configengine.scenario.tshirt;

import com.jmix.configengine.model.*;
import com.jmix.configengine.schema.*;
import com.jmix.configengine.util.ModuleUtils;

/**
 * 生成增强后的T恤衫模块JSON数据
 */
public class GenerateTShirtJson {
    
    public static void main(String[] args) {
        try {
            // 创建T恤衫模块数据
            com.jmix.configengine.model.Module tshirtModule = createEnhancedTShirtModule();
            
            // 生成JSON文件
            String outputPath = "doc/T恤衫模块样例数据_增强版.json";
            ModuleUtils.toJsonFile(tshirtModule, outputPath);
            
            System.out.println("✓ 成功生成增强版T恤衫模块JSON数据: " + outputPath);
            
            // 验证JSON文件
            com.jmix.configengine.model.Module loadedModule = ModuleUtils.fromJsonFile(outputPath);
            System.out.println("✓ 成功验证JSON文件，加载的模块: " + loadedModule.getCode());
            
        } catch (Exception e) {
            System.err.println("生成JSON失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建增强后的T恤衫模块数据
     */
    private static com.jmix.configengine.model.Module createEnhancedTShirtModule() {
        com.jmix.configengine.model.Module module = new com.jmix.configengine.model.Module();
        module.setCode("TShirt");
        module.setId(123123L);
        module.setVersion("V1.0");
        module.setPackageName("com.jmix.configengine.scenario.tshirt");
        module.setType(ModuleType.GENERAL);
        module.setDefaultValue(1);
        module.setDescription("T恤衫配置模块，支持颜色、尺寸选择和部件数量约束");
        module.setSortNo(1);
        module.setExtSchema("TShirtModuleSchema");
        
        // 设置扩展属性
        module.setExtAttr("category", "clothing");
        module.setExtAttr("season", "all");
        
        // 创建颜色参数
        Para colorPara = createEnhancedColorPara();
        
        // 创建尺寸参数
        Para sizePara = createEnhancedSizePara();
        
        // 创建部件
        Part tshirt11Part = createEnhancedTShirt11Part();
        Part tshirt12Part = createEnhancedTShirt12Part();
        
        // 创建规则
        Rule rule1 = createEnhancedRule1();
        Rule rule2 = createEnhancedRule2();
        Rule rule3 = createEnhancedRule3();
        
        // 设置模块属性
        module.setParas(java.util.Arrays.asList(colorPara, sizePara));
        module.setParts(java.util.Arrays.asList(tshirt11Part, tshirt12Part));
        module.setRules(java.util.Arrays.asList(rule1, rule2, rule3));
        
        return module;
    }
    
    /**
     * 创建增强后的颜色参数
     */
    private static Para createEnhancedColorPara() {
        Para para = new Para();
        para.setCode("Color");
        para.setFatherCode("TShirt");
        para.setType(ParaType.ENUM);
        para.setDefaultValue("Red");
        para.setDescription("T恤衫颜色选择参数");
        para.setSortNo(1);
        para.setExtSchema("ColorParaSchema");
        
        // 设置扩展属性
        para.setExtAttr("colorGroup", "basic");
        para.setExtAttr("availability", "inStock");
        
        // 创建颜色选项
        ParaOption redOption = new ParaOption();
        redOption.setCodeId(10);
        redOption.setCode("Red");
        redOption.setFatherCode("Color");
        redOption.setDefaultValue("Red");
        redOption.setDescription("红色T恤衫");
        redOption.setSortNo(1);
        redOption.setExtAttr("hexCode", "#FF0000");
        redOption.setExtAttr("popularity", "high");
        
        ParaOption blackOption = new ParaOption();
        blackOption.setCodeId(20);
        blackOption.setCode("Black");
        blackOption.setFatherCode("Color");
        blackOption.setDefaultValue("Black");
        blackOption.setDescription("黑色T恤衫");
        blackOption.setSortNo(2);
        blackOption.setExtAttr("hexCode", "#000000");
        blackOption.setExtAttr("popularity", "veryHigh");
        
        ParaOption whiteOption = new ParaOption();
        whiteOption.setCodeId(30);
        whiteOption.setCode("White");
        whiteOption.setFatherCode("Color");
        whiteOption.setDefaultValue("White");
        whiteOption.setDescription("白色T恤衫");
        whiteOption.setSortNo(3);
        whiteOption.setExtAttr("hexCode", "#FFFFFF");
        whiteOption.setExtAttr("popularity", "high");
        
        para.setOptions(java.util.Arrays.asList(redOption, blackOption, whiteOption));
        return para;
    }
    
    /**
     * 创建增强后的尺寸参数
     */
    private static Para createEnhancedSizePara() {
        Para para = new Para();
        para.setCode("Size");
        para.setFatherCode("TShirt");
        para.setType(ParaType.ENUM);
        para.setDefaultValue("Medium");
        para.setDescription("T恤衫尺寸选择参数");
        para.setSortNo(2);
        para.setExtSchema("SizeParaSchema");
        
        // 设置扩展属性
        para.setExtAttr("measurement", "EU");
        para.setExtAttr("fit", "regular");
        
        // 创建尺寸选项
        ParaOption bigOption = new ParaOption();
        bigOption.setCodeId(1);
        bigOption.setCode("Big");
        bigOption.setFatherCode("Size");
        bigOption.setDefaultValue("Big");
        bigOption.setDescription("大号尺寸");
        bigOption.setSortNo(1);
        bigOption.setExtAttr("chest", "110-120cm");
        bigOption.setExtAttr("length", "70-75cm");
        
        ParaOption mediumOption = new ParaOption();
        mediumOption.setCodeId(2);
        mediumOption.setCode("Medium");
        mediumOption.setFatherCode("Size");
        mediumOption.setDefaultValue("Medium");
        mediumOption.setDescription("中号尺寸");
        mediumOption.setSortNo(2);
        mediumOption.setExtAttr("chest", "100-110cm");
        mediumOption.setExtAttr("length", "65-70cm");
        
        ParaOption smallOption = new ParaOption();
        smallOption.setCodeId(3);
        smallOption.setCode("Small");
        smallOption.setFatherCode("Size");
        smallOption.setDefaultValue("Small");
        smallOption.setDescription("小号尺寸");
        smallOption.setSortNo(3);
        smallOption.setExtAttr("chest", "90-100cm");
        smallOption.setExtAttr("length", "60-65cm");
        
        para.setOptions(java.util.Arrays.asList(bigOption, mediumOption, smallOption));
        return para;
    }
    
    /**
     * 创建增强后的T恤衫11部件
     */
    private static Part createEnhancedTShirt11Part() {
        Part part = new Part();
        part.setCode("TShirt11");
        part.setFatherCode("TShirt");
        part.setType(PartType.ATOMIC);
        part.setDefaultValue(0);
        part.setDescription("T恤衫主体部件");
        part.setSortNo(1);
        part.setPrice(1500L);
        part.setExtSchema("TShirtPartSchema");
        
        // 设置扩展属性
        part.setExtAttr("material", "cotton");
        part.setExtAttr("weight", "180g");
        
        // 设置属性
        part.setAttr("fabric", "100%棉");
        part.setAttr("thickness", "中等");
        part.setAttr("elasticity", "适中");
        
        return part;
    }
    
    /**
     * 创建增强后的T恤衫12部件
     */
    private static Part createEnhancedTShirt12Part() {
        Part part = new Part();
        part.setCode("TShirt12");
        part.setFatherCode("TShirt");
        part.setType(PartType.ATOMIC);
        part.setDefaultValue(0);
        part.setDescription("T恤衫装饰部件");
        part.setSortNo(2);
        part.setPrice(500L);
        part.setExtSchema("TShirtPartSchema");
        
        // 设置扩展属性
        part.setExtAttr("material", "polyester");
        part.setExtAttr("weight", "50g");
        
        // 设置属性
        part.setAttr("pattern", "印花");
        part.setAttr("position", "胸前");
        part.setAttr("size", "10x8cm");
        
        return part;
    }
    
    /**
     * 创建增强后的规则1：颜色和尺寸兼容关系规则
     */
    private static Rule createEnhancedRule1() {
        Rule rule = new Rule();
        rule.setCode("rule1");
        rule.setName("颜色和尺寸兼容关系规则");
        rule.setProgObjType("Module");
        rule.setProgObjCode("TShirt");
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode("如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号");
        rule.setExtSchema("CompatiableRuleSchema");
        
        // 设置扩展属性
        rule.setExtAttr("priority", "high");
        rule.setExtAttr("enforcement", "strict");
        
        // 创建兼容规则Schema
        CompatiableRuleSchema compatibleRuleSchema = new CompatiableRuleSchema();
        compatibleRuleSchema.setOperator("Requires");
        
        // 创建左表达式：Color="Red"
        ExprSchema leftExpr = new ExprSchema();
        leftExpr.setRawCode("Color=\"Red\"");
        
        RefProgObjSchema leftRef = new RefProgObjSchema();
        leftRef.setProgObjType("Para");
        leftRef.setProgObjCode("Color");
        leftRef.setProgObjField("value");
        leftExpr.setRefProgObjs(java.util.Arrays.asList(leftRef));
        
        // 创建右表达式：Size!="Medium"
        ExprSchema rightExpr = new ExprSchema();
        rightExpr.setRawCode("Size!=\"Medium\"");
        
        RefProgObjSchema rightRef = new RefProgObjSchema();
        rightRef.setProgObjType("Para");
        rightRef.setProgObjCode("Size");
        rightRef.setProgObjField("value");
        rightExpr.setRefProgObjs(java.util.Arrays.asList(rightRef));
        
        compatibleRuleSchema.setLeftExpr(leftExpr);
        compatibleRuleSchema.setRightExpr(rightExpr);
        
        rule.setRawCode(compatibleRuleSchema);
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");
        
        return rule;
    }
    
    /**
     * 创建增强后的规则2：部件数量关系规则
     */
    private static Rule createEnhancedRule2() {
        Rule rule = new Rule();
        rule.setCode("rule2");
        rule.setName("部件数量关系规则");
        rule.setProgObjType("Part");
        rule.setProgObjCode("TShirt12");
        rule.setProgObjField("quantity");
        rule.setNormalNaturalCode("装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍");
        rule.setExtSchema("CalculateRuleSchema");
        
        // 设置扩展属性
        rule.setExtAttr("priority", "medium");
        rule.setExtAttr("enforcement", "flexible");
        
        // 创建计算规则Schema
        CalculateRuleSchema calculateRuleSchema = new CalculateRuleSchema();
        calculateRuleSchema.setType("CalculateRule");
        
        rule.setRawCode(calculateRuleSchema);
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CalculateRule");
        
        return rule;
    }
    
    /**
     * 创建增强后的规则3：颜色选择规则
     */
    private static Rule createEnhancedRule3() {
        Rule rule = new Rule();
        rule.setCode("rule3");
        rule.setName("颜色选择规则");
        rule.setProgObjType("Para");
        rule.setProgObjCode("Color");
        rule.setProgObjField("options");
        rule.setNormalNaturalCode("颜色参数必须且只能选择一个选项");
        rule.setExtSchema("SelectRuleSchema");
        
        // 设置扩展属性
        rule.setExtAttr("priority", "high");
        rule.setExtAttr("enforcement", "strict");
        
        // 创建选择规则Schema
        SelectRuleSchema selectRuleSchema = new SelectRuleSchema();
        selectRuleSchema.setType("SelectRule");
        
        rule.setRawCode(selectRuleSchema);
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.SelectRule");
        
        return rule;
    }
} 