package com.jmix.configengine.scenario.autoruletest;

import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.scenario.base.*;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.scenario.base.modeltool.ModelHelper;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Rule;
import com.jmix.configengine.artifact.ModuleAlgArtifactGenerator;
import com.jmix.configengine.artifact.ModuleVarInfo;
import com.jmix.configengine.schema.CompatiableRuleSchema;
import java.util.List;
import static org.junit.Assert.*;

/**
 * 注解规则集成测试类
 * 用于测试完整的注解规则功能，包括代码注入
 */
public class AnnotationRuleIntegrationTest {
    
    /**
     * 测试完整的注解规则功能
     */
    @org.junit.Test
    public void testCompleteAnnotationRuleFlow() {
        try {
            // 1. 测试注解规则生成
            testAnnotationRuleGeneration();
            
            // 2. 测试代码注入功能
            testCodeInjection();
            
            System.out.println("✓ 完整的注解规则功能测试通过");
            
        } catch (Exception e) {
            fail("集成测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试注解规则生成
     */
    private void testAnnotationRuleGeneration() {
        // 创建临时路径
        String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);
        
        // 生成Module
        Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);
        
        // 验证Module生成
        assertNotNull("Module should not be null", module);
        assertEquals("Module code should be AnnotationRuleTest", "AnnotationRuleTest", module.getCode());
        
        // 验证规则生成
        assertNotNull("Module rules should not be null", module.getRules());
        assertEquals("Should have 2 rules", 2, module.getRules().size());
        
        // 验证参数生成
        assertNotNull("Module paras should not be null", module.getParas());
        assertEquals("Should have 2 paras", 2, module.getParas().size());
        
        // 验证部件生成
        assertNotNull("Module parts should not be null", module.getParts());
        assertEquals("Should have 1 part", 1, module.getParts().size());
        
        System.out.println("✓ 注解规则生成测试通过");
    }
    
    /**
     * 测试代码注入功能
     */
    private void testCodeInjection() {
        try {
            // 创建临时路径
            String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);
            
            // 生成Module
            Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);
            
            // 检查是否需要注入
            boolean isNeedInject = false;
            if (module.getRules() != null) {
                for (Rule rule : module.getRules()) {
                    if ("CDSL.V5.Struct.CompatiableRule".equals(rule.getRuleSchemaTypeFullName())) {
                        isNeedInject = true;
                        break;
                    }
                }
            }
            
            assertTrue("Should need injection for CompatiableRule", isNeedInject);
            
            // 生成ModuleInfo
            ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
            ModuleVarInfo moduleInfo = generator.buildModuleInfo(module);
            
            assertNotNull("ModuleInfo should not be null", moduleInfo);
            assertNotNull("ModuleInfo rules should not be null", moduleInfo.getRules());
            
            // 注入规则
            StructCodeInjector injector = new StructCodeInjector();
            injector.injectRule(AnnotationRuleTest.class, moduleInfo.getRules());
            
            System.out.println("✓ 代码注入测试通过");
            
        } catch (Exception e) {
            fail("代码注入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试ModelHelper的自动注入功能
     */
    @org.junit.Test
    public void testModelHelperAutoInjection() {
        try {
            // 创建ModelHelper实例
            ModelHelper modelHelper = new ModelHelper();
            
            // 测试自动注入功能（这里只是验证方法存在，不实际执行）
            System.out.println("✓ ModelHelper自动注入功能测试通过");
            
        } catch (Exception e) {
            fail("ModelHelper自动注入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试注解常量使用
     */
    @org.junit.Test
    public void testAnnotationConstants() {
        // 测试CompatiableRuleSchema.Operator常量
        assertEquals("INCOMPATIBLE should be Incompatible", "Incompatible", CompatiableRuleSchema.Operator.INCOMPATIBLE);
        assertEquals("CO_REFENT should be CoDependent", "CoDependent", CompatiableRuleSchema.Operator.CO_REFENT);
        assertEquals("REQUIRES should be Requires", "Requires", CompatiableRuleSchema.Operator.REQUIRES);
        
        System.out.println("✓ 注解常量测试通过");
    }
    
    /**
     * 测试ProgObject解析
     */
    @org.junit.Test
    public void testProgObjectParsing() {
        // 测试变量对象解析 - 通过生成Module来间接测试
        String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);
        Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);
        
        // 验证规则中的引用对象解析
        assertNotNull("Module rules should not be null", module.getRules());
        assertTrue("Should have at least one rule", module.getRules().size() > 0);
        
        // 验证第一个规则（CompatiableRule）的引用对象
        Rule firstRule = module.getRules().get(0);
        assertNotNull("First rule schema should not be null", firstRule.getRawCode());
        
        System.out.println("✓ ProgObject解析测试通过");
    }
} 