package com.jmix.configengine.scenario.autoruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 注解规则测试类
 * 用于测试CompatiableRuleAnno和CodeRuleAnno注解的功能
 * 
 * @since 2025-09-23
 */
@Slf4j
/**
 * 注解规则测试约束模型类
 * 
 * @since 2025-09-23
 */
@ModuleAnno(id = 123, code = "AnnotationRule", packageName = "com.jmix.configengine.scenario.ruletest", version = "1.0", description = "注解规则测试模块", sortNo = 1)
public class AnnotationRuleTest extends ConstraintAlgImpl {

    @ParaAnno(description = "颜色参数", type = ParaType.ENUM, options = { "Red", "Blue", "Green" })
    private ParaVar colorVar;

    @ParaAnno(description = "尺寸参数", type = ParaType.ENUM, options = { "Small", "Medium", "Large" })
    private ParaVar sizeVar;

    @PartAnno(description = "部件1", maxQuantity = 10)
    private PartVar part1Var;

    /**
     * 兼容性规则：如果颜色是红色，则部件1的数量必须是1
     */
    @CompatiableRuleAnno(leftExprCode = "color.value==\"Red\"", operator = "Requires", rightExprCode = "part1.qty==1", normalNaturalCode = "如果颜色是红色，则部件1的数量必须是1")
    /**
     * 规则1：兼容性规则
     */
    public void rule1() {
        // 自动生成的约束代码将在这里注入
    }

    /**
     * 代码规则：自定义逻辑
     */
    @CodeRuleAnno(code = "if (color.value == \"Blue\" && size.value == \"Large\") { return false; }", normalNaturalCode = "蓝色大尺寸的组合不允许")
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
    @Test
    public void testRuleGeneration() {
        // 创建临时路径
        String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);

        // 生成Module
        Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);

        // 验证Module中包含了生成的规则
        assertNotNull(module, "Module should not be null");
        assertNotNull(module.getRules(), "Module rules should not be null");
        assertEquals(2, module.getRules().size(), "Should have 2 rules");

        // 验证第一个规则是兼容性规则
        Rule rule1 = module.getRules().get(0);
        assertEquals(rule1.getCode(), "rule1", "First rule should be rule1");
        assertEquals(rule1.getRuleSchemaTypeFullName(),
                "CDSL.V5.Struct.CompatiableRule", "First rule should be CompatiableRule");
        // 验证第二个规则是代码规则
        Rule rule2 = module.getRules().get(1);
        assertEquals(rule2.getCode(), "rule2", "Second rule should be rule2");
        assertEquals(rule2.getRuleSchemaTypeFullName(), "CDSL.V5.Struct.CodeRule", "Second rule should be CodeRule");

        // 验证参数和部件
        assertNotNull(module.getParas(), "Module paras should not be null");
        assertEquals(2, module.getParas().size(), "Should have 2 paras");

        assertNotNull(module.getParts(), "Module parts should not be null");
        assertEquals(1, module.getParts().size(), "Should have 1 part");

        log.info("✓ Rule generation test passed");
        log.info("  Generated rule count: {}", module.getRules().size());
        log.info("  Generated parameter count: {}", module.getParas().size());
        log.info("  Generated part count: {}", module.getParts().size());
    }

    /**
     * 测试注解解析
     */
    @Test
    /**
     * 测试注解解析
     */
    public void testAnnotationParsing() {
        // 测试CompatiableRuleAnno注解解析
        try {
            Method rule1Method = AnnotationRuleTest.class.getMethod("rule1");
            CompatiableRuleAnno compatiableRuleAnno = rule1Method.getAnnotation(CompatiableRuleAnno.class);

            assertNotNull(compatiableRuleAnno, "CompatiableRuleAnno should not be null");
            assertEquals(compatiableRuleAnno.leftExprCode(), "Color.value==\"Red\"", "Left expression should match");
            assertEquals(compatiableRuleAnno.operator(), "Requires", "Operator should match");
            assertEquals(compatiableRuleAnno.rightExprCode(), "Part1.qty==1", "Right expression should match");

            log.info("✓ CompatiableRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule1 not found: " + e.getMessage());
        }

        // 测试CodeRuleAnno注解解析
        try {
            Method rule2Method = AnnotationRuleTest.class.getMethod("rule2");
            CodeRuleAnno codeRuleAnno = rule2Method.getAnnotation(CodeRuleAnno.class);

            assertNotNull(codeRuleAnno, "CodeRuleAnno should not be null");
            assertTrue(codeRuleAnno.code().contains("Color.value"), "Code should contain Color.value");
            assertTrue(codeRuleAnno.code().contains("Size.value"), "Code should contain Size.value");

            log.info("✓ CodeRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule2 not found: " + e.getMessage());
        }
    }
}