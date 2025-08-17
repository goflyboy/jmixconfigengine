package com.jmix.configengine.scenario.tshirt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.configengine.artifact.ModuleAlgArtifactGenerator;
import com.jmix.configengine.model.*;
import com.jmix.configengine.util.ModuleUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ModuleAlgArtifactGenerator.buildConstraintRule 方法测试类
 * 
 * 测试目标：
 * 1. 根据T恤衫模块样例数据生成Module数据
 * 2. 调用ModuleAlgArtifactGenerator.buildConstraintRule生成约束规则代码
 * 3. 验证生成的代码文件是否正确
 */
public class ModuleAlgArtifactGeneratorTest {
    
    private ModuleAlgArtifactGenerator generator;
    private com.jmix.configengine.model.Module tshirtModule;
    private String outputPath;
    //是否删除生成的文件
    private boolean deleteGeneratedFile = true;
    
    @Before
    public void setUp() throws Exception {
        // 初始化生成器
        generator = new ModuleAlgArtifactGenerator();
        
        // 设置输出路径为当前包所在目录
        outputPath = getCurrentPackagePath() + "/TShirtConstraint.java";
        
        // 创建T恤衫模块数据（简化版本，避免JSON反序列化问题）
        tshirtModule = createSimpleTShirtModule();
        
        // 初始化模块
        tshirtModule.init();
    }
    
    /**
     * 创建简化的T恤衫模块数据
     */
    private com.jmix.configengine.model.Module createSimpleTShirtModule() {
        com.jmix.configengine.model.Module module = new com.jmix.configengine.model.Module();
        module.setCode("TShirt");
        module.setId(123123L);
        module.setVersion("V1.0");
        module.setType(ModuleType.GENERAL);
        module.setDefaultValue(1);
        module.setDescription("T恤衫配置模块，支持颜色、尺寸选择和部件数量约束");
        module.setSortNo(1);
        module.setPackageName("com.jmix.configengine.scenario.tshirt");
        
        // 创建颜色参数
        Para colorPara = new Para();
        colorPara.setCode("Color");
        colorPara.setFatherCode("TShirt");
        colorPara.setType(ParaType.ENUM);
        colorPara.setDefaultValue("Red");
        colorPara.setDescription("T恤衫颜色选择参数");
        colorPara.setSortNo(1);
        
        // 创建颜色选项
        ParaOption redOption = new ParaOption();
        redOption.setCodeId(10);
        redOption.setCode("Red");
        redOption.setFatherCode("Color");
        redOption.setDefaultValue("Red");
        redOption.setDescription("红色T恤衫");
        redOption.setSortNo(1);
        
        ParaOption blackOption = new ParaOption();
        blackOption.setCodeId(20);
        blackOption.setCode("Black");
        blackOption.setFatherCode("Color");
        blackOption.setDefaultValue("Black");
        blackOption.setDescription("黑色T恤衫");
        blackOption.setSortNo(2);
        
        ParaOption whiteOption = new ParaOption();
        whiteOption.setCodeId(30);
        whiteOption.setCode("White");
        whiteOption.setFatherCode("Color");
        whiteOption.setDefaultValue("White");
        whiteOption.setDescription("白色T恤衫");
        whiteOption.setSortNo(3);
        
        colorPara.setOptions(java.util.Arrays.asList(redOption, blackOption, whiteOption));
        
        // 创建尺寸参数
        Para sizePara = new Para();
        sizePara.setCode("Size");
        sizePara.setFatherCode("TShirt");
        sizePara.setType(ParaType.ENUM);
        sizePara.setDefaultValue("Medium");
        sizePara.setDescription("T恤衫尺寸选择参数");
        sizePara.setSortNo(2);
        
        // 创建尺寸选项
        ParaOption bigOption = new ParaOption();
        bigOption.setCodeId(1);
        bigOption.setCode("Big");
        bigOption.setFatherCode("Size");
        bigOption.setDefaultValue("Big");
        bigOption.setDescription("大号尺寸");
        bigOption.setSortNo(1);
        
        ParaOption mediumOption = new ParaOption();
        mediumOption.setCodeId(2);
        mediumOption.setCode("Medium");
        mediumOption.setFatherCode("Size");
        mediumOption.setDefaultValue("Medium");
        mediumOption.setDescription("中号尺寸");
        mediumOption.setSortNo(2);
        
        ParaOption smallOption = new ParaOption();
        smallOption.setCodeId(3);
        smallOption.setCode("Small");
        smallOption.setFatherCode("Size");
        smallOption.setDefaultValue("Small");
        smallOption.setDescription("小号尺寸");
        smallOption.setSortNo(3);
        
        sizePara.setOptions(java.util.Arrays.asList(bigOption, mediumOption, smallOption));
        
        // 创建部件
        Part tshirt11Part = new Part();
        tshirt11Part.setCode("TShirt11");
        tshirt11Part.setFatherCode("TShirt");
        tshirt11Part.setType(PartType.ATOMIC);
        tshirt11Part.setDefaultValue(0);
        tshirt11Part.setDescription("T恤衫主体部件");
        tshirt11Part.setSortNo(1);
        tshirt11Part.setPrice(1500L);
        
        Part tshirt12Part = new Part();
        tshirt12Part.setCode("TShirt12");
        tshirt12Part.setFatherCode("TShirt");
        tshirt12Part.setType(PartType.ATOMIC);
        tshirt12Part.setDefaultValue(0);
        tshirt12Part.setDescription("T恤衫装饰部件");
        tshirt12Part.setSortNo(2);
        tshirt12Part.setPrice(500L);
        
        // 创建规则
        Rule rule1 = new Rule();
        rule1.setCode("rule1");
        rule1.setName("颜色和尺寸兼容关系规则");
        rule1.setProgObjType("Module");
        rule1.setProgObjCode("TShirt");
        rule1.setProgObjField("constraints");
        rule1.setNormalNaturalCode("如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号");
        rule1.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");
        
        // 创建兼容规则Schema
        com.jmix.configengine.schema.CompatiableRuleSchema compatibleRuleSchema = new com.jmix.configengine.schema.CompatiableRuleSchema();
        compatibleRuleSchema.setOperator("Requires");
        
        // 创建左表达式：Color="Red"
        com.jmix.configengine.schema.ExprSchema leftExpr = new com.jmix.configengine.schema.ExprSchema();
        leftExpr.setRawCode("Color=\"Red\"");
        
        com.jmix.configengine.schema.RefProgObjSchema leftRef = new com.jmix.configengine.schema.RefProgObjSchema();
        leftRef.setProgObjType("Para");
        leftRef.setProgObjCode("Color");
        leftRef.setProgObjField("value");
        leftExpr.setRefProgObjs(java.util.Arrays.asList(leftRef));
        
        // 创建右表达式：Size!="Medium"
        com.jmix.configengine.schema.ExprSchema rightExpr = new com.jmix.configengine.schema.ExprSchema();
        rightExpr.setRawCode("Size!=\"Medium\"");
        
        com.jmix.configengine.schema.RefProgObjSchema rightRef = new com.jmix.configengine.schema.RefProgObjSchema();
        rightRef.setProgObjType("Para");
        rightRef.setProgObjCode("Size");
        rightRef.setProgObjField("value");
        rightExpr.setRefProgObjs(java.util.Arrays.asList(rightRef));
        
        compatibleRuleSchema.setLeftExpr(leftExpr);
        compatibleRuleSchema.setRightExpr(rightExpr);
        
        rule1.setRawCode(compatibleRuleSchema);
        
        // 创建规则2：部件数量关系规则
        Rule rule2 = new Rule();
        rule2.setCode("rule2");
        rule2.setName("部件数量关系规则");
        rule2.setProgObjType("Part");
        rule2.setProgObjCode("TShirt12");
        rule2.setProgObjField("quantity");
        rule2.setNormalNaturalCode("装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍");
        rule2.setRuleSchemaTypeFullName("CDSL.V5.Struct.CalculateRule");
        
        // 创建计算规则Schema
        com.jmix.configengine.schema.CalculateRuleSchema calculateRuleSchema = new com.jmix.configengine.schema.CalculateRuleSchema();
        calculateRuleSchema.setType("CalculateRule");
        
        rule2.setRawCode(calculateRuleSchema);
        
        // 创建规则3：颜色选择规则
        Rule rule3 = new Rule();
        rule3.setCode("rule3");
        rule3.setName("颜色选择规则");
        rule3.setProgObjType("Para");
        rule3.setProgObjCode("Color");
        rule3.setProgObjField("options");
        rule3.setNormalNaturalCode("颜色参数必须且只能选择一个选项");
        rule3.setRuleSchemaTypeFullName("CDSL.V5.Struct.SelectRule");
        
        // 创建选择规则Schema
        com.jmix.configengine.schema.SelectRuleSchema selectRuleSchema = new com.jmix.configengine.schema.SelectRuleSchema();
        selectRuleSchema.setType("SelectRule");
        
        rule3.setRawCode(selectRuleSchema);
        
        // 设置模块属性
        module.setParas(java.util.Arrays.asList(colorPara, sizePara));
        module.setParts(java.util.Arrays.asList(tshirt11Part, tshirt12Part));
        module.setRules(java.util.Arrays.asList(rule1, rule2, rule3));
        
        return module;
    }
    
    @Test
    public void testBuildConstraintRule() throws Exception {
        deleteGeneratedFile = false;
        System.out.println("=== 开始测试 buildConstraintRule 方法 ===");
        System.out.println("输出路径: " + outputPath);
        
        // 验证模块数据是否正确创建
        assertNotNull("T恤衫模块应该创建成功", tshirtModule);
        assertEquals("模块代码应该是TShirt", "TShirt", tshirtModule.getCode());
        assertNotNull("参数列表不应该为空", tshirtModule.getParas());
        assertNotNull("部件列表不应该为空", tshirtModule.getParts());
        assertNotNull("规则列表不应该为空", tshirtModule.getRules());
        
        System.out.println("✓ 模块数据验证通过");
        System.out.println("  参数数量: " + tshirtModule.getParas().size());
        System.out.println("  部件数量: " + tshirtModule.getParts().size());
        System.out.println("  规则数量: " + tshirtModule.getRules().size());
        
        // 调用buildConstraintRule方法
        System.out.println("\n=== 调用 buildConstraintRule 方法 ===");
        
        // 由于模板需要rule.left和rule.right属性，我们需要先手动构建这些信息
        // 或者修改模板来处理这种情况
        try {
            generator.buildConstraintRule(tshirtModule, outputPath);
        } catch (Exception e) {
            System.out.println("buildConstraintRule 调用失败，错误信息: " + e.getMessage());
            // 这是一个已知问题，模板需要rule.left和rule.right属性
            // 但当前的实现没有正确设置这些属性
            throw e;
        }
        
        // 验证生成的代码文件是否存在
        File generatedFile = new File(outputPath);
        assertTrue("生成的约束代码文件应该存在", generatedFile.exists());
        assertTrue("生成的约束代码文件应该可读", generatedFile.canRead());
        
        System.out.println("✓ 约束代码文件生成成功");
        System.out.println("  文件路径: " + generatedFile.getAbsolutePath());
        System.out.println("  文件大小: " + generatedFile.length() + " 字节");
        
        // 验证生成的代码内容
        validateGeneratedCode();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 验证生成的代码内容
     */
    private void validateGeneratedCode() throws Exception {
        Path path = Paths.get(outputPath);
        String content = new String(Files.readAllBytes(path));
        
        // 验证基本结构
        assertTrue("生成的代码应该包含类定义", content.contains("public class TShirtConstraint"));
        assertTrue("生成的代码应该继承ConstraintAlgImpl", content.contains("extends ConstraintAlgImpl"));
        
        // 验证参数变量
        assertTrue("应该包含Color参数变量", content.contains("private ParaVar ColorVar"));
        assertTrue("应该包含Size参数变量", content.contains("private ParaVar SizeVar"));
        
        // 验证部件变量
        assertTrue("应该包含TShirt11部件变量", content.contains("private PartVar TShirt11Var"));
        assertTrue("应该包含TShirt12部件变量", content.contains("private PartVar TShirt12Var"));
        
        // 验证方法
        assertTrue("应该包含initVariables方法", content.contains("public void initVariables()"));
        assertTrue("应该包含initConstraint方法", content.contains("public void initConstraint()"));
        
        // 验证约束规则方法
        assertTrue("应该包含规则1的约束方法", content.contains("addConstraint_rule1"));
        assertTrue("应该包含规则2的约束方法", content.contains("addConstraint_rule2"));
        assertTrue("应该包含规则3的约束方法", content.contains("addConstraint_rule3"));
        
        System.out.println("✓ 生成的代码内容验证通过");
        System.out.println("  包含正确的类定义");
        System.out.println("  包含所有参数变量");
        System.out.println("  包含所有部件变量");
        System.out.println("  包含所有约束规则方法");
    }
    
    /**
     * 获取当前包所在目录路径
     */
    private String getCurrentPackagePath() {
        // 获取当前类的包路径
        String packagePath = this.getClass().getPackage().getName().replace('.', '/');
        
        // 构建完整的目录路径
        String baseDir = "src/test/java";
        String fullPath = baseDir + "/" + packagePath;
        
        // 确保目录存在
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return fullPath;
    }
    
    @After
    public void tearDown() {
        // 清理生成的测试文件
        if (!deleteGeneratedFile) {
            return;
        }

        try {
            File generatedFile = new File(outputPath);
            if (generatedFile.exists()) {
                generatedFile.delete();
                System.out.println("清理测试文件: " + outputPath);
            }
        } catch (Exception e) {
            System.err.println("清理测试文件失败: " + e.getMessage());
        }
    }
} 