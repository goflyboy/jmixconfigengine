package com.jmix.configengine.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Rule;
import com.jmix.configengine.schema.RuleSchema;
import com.jmix.configengine.schema.CompatiableRuleSchema;
import com.jmix.configengine.schema.CalculateRuleSchema;
import com.jmix.configengine.schema.SelectRuleSchema;
import com.jmix.configengine.schema.ExprSchema;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ModuleAlgArtifactGenerator单元测试
 */
public class ModuleAlgArtifactGeneratorTest {
    
    private ModuleAlgArtifactGenerator generator;
    private ObjectMapper objectMapper;
    
    @Before
    public void setUp() {
        generator = new ModuleAlgArtifactGenerator();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    public void testBuildCompatiableRule() throws Exception {
        // 准备测试数据 - 兼容规则
        Rule rule = createCompatiableRule();
        Module module = createTestModule();
        
        // 使用反射调用私有方法buildRule
        RuleInfo ruleInfo = invokeBuildRule(module, rule);
        
        // 验证规则基本信息
        assertNotNull(ruleInfo);
        assertEquals("rule1", ruleInfo.getCode());
        assertEquals("颜色和尺寸兼容关系规则", ruleInfo.getName());
        assertEquals("如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号", ruleInfo.getNormalNaturalCode());
        assertEquals("CDSL.V5.Struct.CompatiableRule", ruleInfo.getRuleSchemaTypeFullName());
        
        // 验证兼容规则特有的属性
        assertEquals("ParaVar", ruleInfo.getLeftTypeName());
        assertEquals("ParaVar", ruleInfo.getRightTypeName());
    }
    
    @Test
    public void testBuildCalculateRule() throws Exception {
        // 准备测试数据 - 计算规则
        Rule rule = createCalculateRule();
        Module module = createTestModule();
        
        // 使用反射调用私有方法buildRule
        RuleInfo ruleInfo = invokeBuildRule(module, rule);
        
        // 验证规则基本信息
        assertNotNull(ruleInfo);
        assertEquals("rule2", ruleInfo.getCode());
        assertEquals("部件数量关系规则", ruleInfo.getName());
        assertEquals("装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍", ruleInfo.getNormalNaturalCode());
        assertEquals("CDSL.V5.Struct.CalculateRule", ruleInfo.getRuleSchemaTypeFullName());
        
        // 验证计算规则特有的属性
        assertEquals("PartVar", ruleInfo.getLeftTypeName());
        assertEquals("PartVar", ruleInfo.getRightTypeName());
    }
    
    @Test
    public void testBuildSelectRule() throws Exception {
        // 准备测试数据 - 选择规则
        Rule rule = createSelectRule();
        Module module = createTestModule();
        
        // 使用反射调用私有方法buildRule
        RuleInfo ruleInfo = invokeBuildRule(module, rule);
        
        // 验证规则基本信息
        assertNotNull(ruleInfo);
        assertEquals("rule3", ruleInfo.getCode());
        assertEquals("颜色选择规则", ruleInfo.getName());
        assertEquals("颜色参数必须且只能选择一个选项", ruleInfo.getNormalNaturalCode());
        assertEquals("CDSL.V5.Struct.SelectRule", ruleInfo.getRuleSchemaTypeFullName());
        
        // 验证选择规则特有的属性
        assertEquals("ParaVar", ruleInfo.getLeftTypeName());
        assertEquals("ParaVar", ruleInfo.getRightTypeName());
    }
    
    @Test
    public void testBuildRuleWithException() throws Exception {
        // 准备一个会导致异常的规则
        Rule rule = createInvalidRule();
        Module module = createTestModule();
        
        // 使用反射调用私有方法buildRule，应该不会抛出异常，而是记录错误日志
        RuleInfo ruleInfo = invokeBuildRule(module, rule);
        
        // 验证即使有异常，方法仍然返回RuleInfo对象
        assertNotNull(ruleInfo);
        assertEquals("invalidRule", ruleInfo.getCode());
    }
    
    @Test
    public void testBuildUnknownRuleType() throws Exception {
        // 准备一个未知类型的规则
        Rule rule = createUnknownRuleType();
        Module module = createTestModule();
        
        // 使用反射调用私有方法buildRule
        RuleInfo ruleInfo = invokeBuildRule(module, rule);
        
        // 验证规则基本信息仍然正确设置
        assertNotNull(ruleInfo);
        assertEquals("unknownRule", ruleInfo.getCode());
        assertEquals("未知类型规则", ruleInfo.getName());
        
        // 验证leftTypeName和rightTypeName没有被设置（保持默认值null）
        assertNull(ruleInfo.getLeftTypeName());
        assertNull(ruleInfo.getRightTypeName());
    }
    
    /**
     * 创建兼容规则测试数据
     */
    private Rule createCompatiableRule() throws Exception {
        Rule rule = new Rule();
        rule.setCode("rule1");
        rule.setName("颜色和尺寸兼容关系规则");
        rule.setNormalNaturalCode("如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");
        
        // 构建rawCode - 使用CompatiableRuleSchema对象
        CompatiableRuleSchema rawCode = new CompatiableRuleSchema();
        rawCode.setType("CompatiableRule");
        rawCode.setVersion("1.0");
        
        // 左表达式
        ExprSchema leftExpr = new ExprSchema();
        leftExpr.setRawCode("Color=\"Red\"");
        rawCode.setLeftExpr(leftExpr);
        
        // 操作符
        rawCode.setOperator("Requires");
        
        // 右表达式
        ExprSchema rightExpr = new ExprSchema();
        rightExpr.setRawCode("Size!=\"Medium\"");
        rawCode.setRightExpr(rightExpr);
        
        rule.setRawCode(rawCode);
        
        return rule;
    }
    
    /**
     * 创建计算规则测试数据
     */
    private Rule createCalculateRule() throws Exception {
        Rule rule = new Rule();
        rule.setCode("rule2");
        rule.setName("部件数量关系规则");
        rule.setNormalNaturalCode("装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CalculateRule");
        
        // 构建rawCode - 使用CalculateRuleSchema对象
        CalculateRuleSchema rawCode = new CalculateRuleSchema();
        rawCode.setType("CalculateRule");
        rawCode.setVersion("1.0");
        
        // 左表达式
        ExprSchema leftExpr = new ExprSchema();
        leftExpr.setRawCode("TShirt12.quantity = TShirt11.quantity * 2");
        rawCode.setLeftExpr(leftExpr);
        
        rule.setRawCode(rawCode);
        
        return rule;
    }
    
    /**
     * 创建选择规则测试数据
     */
    private Rule createSelectRule() throws Exception {
        Rule rule = new Rule();
        rule.setCode("rule3");
        rule.setName("颜色选择规则");
        rule.setNormalNaturalCode("颜色参数必须且只能选择一个选项");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.SelectRule");
        
        // 构建rawCode - 使用SelectRuleSchema对象
        SelectRuleSchema rawCode = new SelectRuleSchema();
        rawCode.setType("SelectRule");
        rawCode.setVersion("1.0");
        
        // 左表达式
        ExprSchema leftExpr = new ExprSchema();
        leftExpr.setRawCode("Color.options.count = 1");
        rawCode.setLeftExpr(leftExpr);
        
        rule.setRawCode(rawCode);
        
        return rule;
    }
    
    /**
     * 创建无效规则测试数据（会导致异常）
     */
    private Rule createInvalidRule() {
        Rule rule = new Rule();
        rule.setCode("invalidRule");
        rule.setName("无效规则");
        rule.setNormalNaturalCode("这是一个无效的规则");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");
        
        // 设置null的rawCode，模拟无效情况
        rule.setRawCode(null);
        
        return rule;
    }
    
    /**
     * 创建未知类型规则测试数据
     */
    private Rule createUnknownRuleType() {
        Rule rule = new Rule();
        rule.setCode("unknownRule");
        rule.setName("未知类型规则");
        rule.setNormalNaturalCode("这是一个未知类型的规则");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.UnknownRule");
        
        return rule;
    }
    
    /**
     * 创建测试模块
     */
    private Module createTestModule() {
        Module module = new Module();
        module.setCode("TShirt");
        return module;
    }
    
    /**
     * 使用反射调用私有方法buildRule
     */
    private RuleInfo invokeBuildRule(Module module, Rule rule) throws Exception {
        try {
            java.lang.reflect.Method method = ModuleAlgArtifactGenerator.class
                    .getDeclaredMethod("buildRule", Module.class, Rule.class);
            method.setAccessible(true);
            return (RuleInfo) method.invoke(generator, module, rule);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke buildRule method", e);
        }
    }
} 