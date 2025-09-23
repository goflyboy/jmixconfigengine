package com.jmix.configengine.scenario.autoruletest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.tool.model.CodeRuleAnno;
import com.jmix.tool.model.CommHelper;
import com.jmix.tool.model.CompatiableRuleAnno;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ModuleGenneratorByAnno;
import com.jmix.tool.model.ParaAnno;
import com.jmix.tool.model.PartAnno;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 注解规则测试类
 * 用于测试CompatiableRuleAnno和CodeRuleAnno注解的功能
 */
@Slf4j
/**
 * 注解规则测试约束模型类
 * 
 * @since 2025-09-23
 */
@ModuleAnno(id = 123, code = "AnnotationRule", packageName = "com.jmix.configengine.scenario.ruletest", version = "1.0", description = "注解规则测试模块", sortNo = 1)
public class AnnotationRuleTest extends ConstraintAlgImpl {

    @ParaAnno(code = "Color", description = "颜色参数", type = ParaType.ENUM, options = { "Red", "Blue", "Green" })
    private ParaVar colorVar;

    @ParaAnno(code = "Size", description = "尺寸参数", type = ParaType.ENUM, options = { "Small", "Medium", "Large" })
    private ParaVar sizeVar;

    @PartAnno(code = "Part1", description = "部件1", maxQuantity = 10)
    private PartVar part1Var;

    /**
     * 兼容性规则：如果颜色是红色，则部件1的数量必须是1
     */
    @CompatiableRuleAnno(leftExprCode = "Color.value==\"Red\"", operator = "Requires", rightExprCode = "Part1.qty==1", normalNaturalCode = "如果颜色是红色，则部件1的数量必须是1")
    /**
     * 规则1：兼容性规则
     */
    public void rule1() {
        // 自动生成的约束代码将在这里注入
    }

    /**
     * 代码规则：自定义逻辑
     */
    @CodeRuleAnno(code = "if (Color.value == \"Blue\" && Size.value == \"Large\") { return false; }", normalNaturalCode = "蓝色大尺寸的组合不允许")
    /**
     * 规则2：代码规则
     */
    public void rule2() {
        // 代码规则不需要注入代码
    }

    /**
     * 初始化约束
     */
    @Override
    public void initConstraint() {
        // 约束初始化逻辑
    }

    /**
     * 测试规则生成
     */
    @org.junit.Test
    /**
     * 测试规则生成
     */
    public void testRuleGeneration() {
        // 创建临时路径
        String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);

        // 生成Module
        Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);

        // 验证Module中包含了生成的规则
        assertNotNull("Module should not be null", module);
        assertNotNull("Module rules should not be null", module.getRules());
        assertEquals("Should have 2 rules", 2, module.getRules().size());

        // 验证第一个规则是兼容性规则
        Rule rule1 = module.getRules().get(0);
        assertEquals("First rule should be rule1", "rule1", rule1.getCode());
        assertEquals("First rule should be CompatiableRule", "CDSL.V5.Struct.CompatiableRule",
                rule1.getRuleSchemaTypeFullName());

        // 验证第二个规则是代码规则
        Rule rule2 = module.getRules().get(1);
        assertEquals("Second rule should be rule2", "rule2", rule2.getCode());
        assertEquals("Second rule should be CodeRule", "CDSL.V5.Struct.CodeRule", rule2.getRuleSchemaTypeFullName());

        // 验证参数和部件
        assertNotNull("Module paras should not be null", module.getParas());
        assertEquals("Should have 2 paras", 2, module.getParas().size());

        assertNotNull("Module parts should not be null", module.getParts());
        assertEquals("Should have 1 part", 1, module.getParts().size());

        log.info("✓ Rule generation test passed");
        log.info("  Generated rule count: {}", module.getRules().size());
        log.info("  Generated parameter count: {}", module.getParas().size());
        log.info("  Generated part count: {}", module.getParts().size());
    }

    /**
     * 测试注解解析
     */
    @org.junit.Test
    /**
     * 测试注解解析
     */
    public void testAnnotationParsing() {
        // 测试CompatiableRuleAnno注解解析
        try {
            Method rule1Method = AnnotationRuleTest.class.getMethod("rule1");
            CompatiableRuleAnno compatiableRuleAnno = rule1Method.getAnnotation(CompatiableRuleAnno.class);

            assertNotNull("CompatiableRuleAnno should not be null", compatiableRuleAnno);
            assertEquals("Left expression should match", "Color.value==\"Red\"", compatiableRuleAnno.leftExprCode());
            assertEquals("Operator should match", "Requires", compatiableRuleAnno.operator());
            assertEquals("Right expression should match", "Part1.qty==1", compatiableRuleAnno.rightExprCode());

            log.info("✓ CompatiableRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule1 not found: " + e.getMessage());
        }

        // 测试CodeRuleAnno注解解析
        try {
            Method rule2Method = AnnotationRuleTest.class.getMethod("rule2");
            CodeRuleAnno codeRuleAnno = rule2Method.getAnnotation(CodeRuleAnno.class);

            assertNotNull("CodeRuleAnno should not be null", codeRuleAnno);
            assertTrue("Code should contain Color.value", codeRuleAnno.code().contains("Color.value"));
            assertTrue("Code should contain Size.value", codeRuleAnno.code().contains("Size.value"));

            log.info("✓ CodeRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule2 not found: " + e.getMessage());
        }
    }
}