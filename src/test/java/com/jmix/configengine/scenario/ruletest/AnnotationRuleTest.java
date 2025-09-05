package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.artifact.ConstraintAlg;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.scenario.base.*;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Rule;
import static org.junit.Assert.*;

/**
 * 注解规则测试类
 * 用于测试CompatiableRuleAnno和CodeRuleAnno注解的功能
 */
@ModuleAnno(
    id = 123,
    code = "AnnotationRule",
    packageName = "com.jmix.configengine.scenario.ruletest",
    version = "1.0",
    description = "注解规则测试模块",
    sortNo = 1
)
public class AnnotationRuleTest extends ConstraintAlgImpl {
    
    @ParaAnno(
        code = "Color",
        description = "颜色参数",
        type = com.jmix.configengine.model.ParaType.ENUM,
        options = {"Red", "Blue", "Green"}
    )
    private ParaVar colorVar;
    
    @ParaAnno(
        code = "Size",
        description = "尺寸参数",
        type = com.jmix.configengine.model.ParaType.ENUM,
        options = {"Small", "Medium", "Large"}
    )
    private ParaVar sizeVar;
    
    @PartAnno(
        code = "Part1",
        description = "部件1",
        maxQuantity = 10
    )
    private PartVar part1Var;
    
    /**
     * 兼容性规则：如果颜色是红色，则部件1的数量必须是1
     */
    @CompatiableRuleAnno(
        leftExprCode = "Color.value==\"Red\"",
        operator = "Requires",
        rightExprCode = "Part1.qty==1",
        normalNaturalCode = "如果颜色是红色，则部件1的数量必须是1"
    )
    public void rule1() {
        // 自动生成的约束代码将在这里注入
    }
    
    /**
     * 代码规则：自定义逻辑
     */
    @CodeRuleAnno(
        code = "if (Color.value == \"Blue\" && Size.value == \"Large\") { return false; }",
        normalNaturalCode = "蓝色大尺寸的组合不允许"
    )
    public void rule2() {
        // 代码规则不需要注入代码
    }
    
    @Override
    public void initConstraint() {
        // 约束初始化逻辑
    }
    
    /**
     * 测试规则生成
     */
    @org.junit.Test
    public void testRuleGeneration() {
        // 创建临时路径
        String tempPath = com.jmix.configengine.scenario.base.CommHelper.createTempPath(AnnotationRuleTest.class);
        
        // 生成Module
        Module module = com.jmix.configengine.scenario.base.ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);
        
        // 验证Module中包含了生成的规则
        assertNotNull("Module should not be null", module);
        assertNotNull("Module rules should not be null", module.getRules());
        assertEquals("Should have 2 rules", 2, module.getRules().size());
        
        // 验证第一个规则是兼容性规则
        Rule rule1 = module.getRules().get(0);
        assertEquals("First rule should be rule1", "rule1", rule1.getCode());
        assertEquals("First rule should be CompatiableRule", "CDSL.V5.Struct.CompatiableRule", rule1.getRuleSchemaTypeFullName());
        
        // 验证第二个规则是代码规则
        Rule rule2 = module.getRules().get(1);
        assertEquals("Second rule should be rule2", "rule2", rule2.getCode());
        assertEquals("Second rule should be CodeRule", "CDSL.V5.Struct.CodeRule", rule2.getRuleSchemaTypeFullName());
        
        // 验证参数和部件
        assertNotNull("Module paras should not be null", module.getParas());
        assertEquals("Should have 2 paras", 2, module.getParas().size());
        
        assertNotNull("Module parts should not be null", module.getParts());
        assertEquals("Should have 1 part", 1, module.getParts().size());
        
        System.out.println("✓ 规则生成测试通过");
        System.out.println("  生成的规则数量: " + module.getRules().size());
        System.out.println("  生成的参数数量: " + module.getParas().size());
        System.out.println("  生成的部件数量: " + module.getParts().size());
    }
    
    /**
     * 测试注解解析
     */
    @org.junit.Test
    public void testAnnotationParsing() {
        // 测试CompatiableRuleAnno注解解析
        try {
            java.lang.reflect.Method rule1Method = AnnotationRuleTest.class.getMethod("rule1");
            CompatiableRuleAnno compatiableRuleAnno = rule1Method.getAnnotation(CompatiableRuleAnno.class);
            
            assertNotNull("CompatiableRuleAnno should not be null", compatiableRuleAnno);
            assertEquals("Left expression should match", "Color.value==\"Red\"", compatiableRuleAnno.leftExprCode());
            assertEquals("Operator should match", "Requires", compatiableRuleAnno.operator());
            assertEquals("Right expression should match", "Part1.qty==1", compatiableRuleAnno.rightExprCode());
            
            System.out.println("✓ CompatiableRuleAnno注解解析测试通过");
        } catch (NoSuchMethodException e) {
            fail("Method rule1 not found: " + e.getMessage());
        }
        
        // 测试CodeRuleAnno注解解析
        try {
            java.lang.reflect.Method rule2Method = AnnotationRuleTest.class.getMethod("rule2");
            CodeRuleAnno codeRuleAnno = rule2Method.getAnnotation(CodeRuleAnno.class);
            
            assertNotNull("CodeRuleAnno should not be null", codeRuleAnno);
            assertTrue("Code should contain Color.value", codeRuleAnno.code().contains("Color.value"));
            assertTrue("Code should contain Size.value", codeRuleAnno.code().contains("Size.value"));
            
            System.out.println("✓ CodeRuleAnno注解解析测试通过");
        } catch (NoSuchMethodException e) {
            fail("Method rule2 not found: " + e.getMessage());
        }
    }
} 