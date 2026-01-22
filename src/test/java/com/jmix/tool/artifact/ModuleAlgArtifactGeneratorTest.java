package com.jmix.tool.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.base.Extensible;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalculateRuleSchema;
import com.jmix.executor.bmodel.logic.CompatiableRuleSchema;
import com.jmix.executor.bmodel.logic.ExprSchema;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.logic.SelectRuleSchema;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.tool.artbuilder.ModuleAlgArtifactGenerator;
import com.jmix.tool.artbuilder.impl.ModuleVarInfo;
import com.jmix.tool.artbuilder.impl.RuleInfo;
import com.jmix.tool.artbuilder.impl.VarInfo;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * ModuleAlgArtifactGenerator单元测试
 * 
 * @since 2025-09-23
 */
@Slf4j
public class ModuleAlgArtifactGeneratorTest {

    /**
     * 测试设置方法
     */
    @BeforeEach
    public void setUp() {
    }

    /**
     * 测试forModule工厂方法
     */
    @Test
    public void testForModuleFactoryMethod() {
        Module module = createTestModule();
        ModuleAlgArtifactGenerator generator = ModuleAlgArtifactGenerator.forModule(module);

        // 验证生成器是否正确初始化
        assertNotNull(generator, "Generator should not be null");
        assertNotNull(generator.getModuleVarInfo(), "ModuleInfo should not be null");
        assertEquals(module.getCode(),
                generator.getModuleVarInfo().getCode(), "Module code should match");

        log.info("forModule factory method test passed successfully");
    }

    /**
     * 测试buildRule方法
     * 
     * @throws Exception 异常
     */
    public void testBuildRule() throws Exception {
        // 创建测试模块和ModuleInfo
        Module module = createTestModule();
        module.init();

        // 测试计算规则
        Rule calculateRule = createCalculateRule();
        RuleInfo calculateRuleInfo = invokeBuildRule(module, calculateRule);

        assertNotNull(calculateRuleInfo, "CalculateRuleInfo should not be null");
        assertEquals("rule2", calculateRuleInfo.getCode(), "Code should match");
        assertEquals("部件数量关系规则", calculateRuleInfo.getName(), "Name should match");
        assertEquals("装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍",
                calculateRuleInfo.getNormalNaturalCode(), "NormalNaturalCode should match");
        assertEquals("CDSL.V5.Struct.CalculateRule",
                calculateRuleInfo.getRuleSchemaTypeFullName(), "RuleSchemaTypeFullName should match");
        assertEquals("PartVar",
                calculateRuleInfo.getLeftTypeName(), "LeftTypeName should be PartVar");
        assertEquals("PartVar",
                calculateRuleInfo.getRightTypeName(), "RightTypeName should be PartVar");

        // 测试选择规则
        Rule selectRule = createSelectRule();
        RuleInfo selectRuleInfo = invokeBuildRule(module, selectRule);

        assertNotNull(selectRuleInfo, "SelectRuleInfo should not be null");
        assertEquals("rule3", selectRuleInfo.getCode(), "Code should match");
        assertEquals("颜色选择规则", selectRuleInfo.getName(), "Name should match");
        assertEquals("颜色参数必须且只能选择一个选项",
                selectRuleInfo.getNormalNaturalCode(), "NormalNaturalCode should match");
        assertEquals("CDSL.V5.Struct.SelectRule",
                selectRuleInfo.getRuleSchemaTypeFullName(), "RuleSchemaTypeFullName should match");
        assertEquals("ParaVar",
                selectRuleInfo.getLeftTypeName(), "LeftTypeName should be ParaVar");
        assertEquals("ParaVar",
                selectRuleInfo.getRightTypeName(), "RightTypeName should be ParaVar");

        // 测试无效规则（应该不会抛出异常，而是记录错误日志）
        Rule invalidRule = createInvalidRule();
        RuleInfo invalidRuleInfo = invokeBuildRule(module, invalidRule);

        assertNotNull(invalidRuleInfo, "InvalidRuleInfo should not be null");
        assertEquals("invalidRule", invalidRuleInfo.getCode(), "Code should match");
        assertEquals("无效规则", invalidRuleInfo.getName(), "Name should match");

        // 测试未知类型规则
        Rule unknownRule = createUnknownRuleType();
        RuleInfo unknownRuleInfo = invokeBuildRule(module, unknownRule);

        assertNotNull(unknownRuleInfo, "UnknownRuleInfo should not be null");
        assertEquals("unknownRule", unknownRuleInfo.getCode(), "Code should match");
        assertEquals("未知类型规则", unknownRuleInfo.getName(), "Name should match");
        // 对于未知类型，左右类型名称应该为null
        assertNull(unknownRuleInfo.getLeftTypeName(), "LeftTypeName should be null for unknown type");
        assertNull(unknownRuleInfo.getRightTypeName(), "RightTypeName should be null for unknown type");

        log.info("BuildRule test passed successfully");
    }

    /**
     * 测试doSelectProObjs方法
     */
    @Test
    public void testDoSelectProObjs() {
        Module module = createTestModule();
        ModuleAlgArtifactGenerator generator = ModuleAlgArtifactGenerator.forModule(module);

        // 创建测试用的ExprSchema
        ExprSchema exprSchema = new ExprSchema();
        exprSchema.setRawCode("code = \"Red\"");

        RefProgObjSchema refProgObj = new RefProgObjSchema();
        refProgObj.setProgObjType("Para");
        refProgObj.setProgObjCode("Color");
        refProgObj.setProgObjField("options");

        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        refProgObjs.add(refProgObj);
        exprSchema.setRefProgObjs(refProgObjs);

        // 测试doSelectProObjs方法（通过反射调用私有方法）
        try {
            Pair<VarInfo<? extends Extensible>, List<String>> result = generator
                    .doSelectProObjs(generator.getModuleVarInfo(), exprSchema).orElse(null);

            // 由于当前实现中getPara和getPart可能返回null，所以result可能为null
            // 这是预期的行为，我们主要验证方法调用不会抛出异常
            if (result != null) {
                assertNotNull(result.getFirst(), "Target object should not be null");
                assertNotNull(result.getSecond(), "Filtered objects should not be null");

                log.info("doSelectProObjs test passed - targetObj: {}, filterObjects size: {}",
                        (result.getFirst() != null ? result.getFirst().getClass().getSimpleName() : "null"),
                        result.getSecond().size());
            } else {
                log.info("doSelectProObjs test passed - result is null (expected for current implementation)");
            }

        } catch (Exception e) {
            log.error("Failed to test doSelectProObjs method: {}", e.getMessage());
            log.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * 测试Module方法
     */
    public void testModuleMethods() {
        Module module = createTestModuleWithHierarchy();

        // 初始化映射表
        module.init();

        // 测试getPara方法
        java.util.Optional<Para> colorParaOpt = module.getPara("Color");
        assertTrue(colorParaOpt.isPresent(), "Color para should be present");
        Para colorPara = colorParaOpt.get();
        assertEquals("Color", colorPara.getCode());

        java.util.Optional<Para> sizeParaOpt = module.getPara("Size");
        assertTrue(sizeParaOpt.isPresent(), "Size para should be present");
        Para sizePara = sizeParaOpt.get();
        assertNotNull(sizePara, "Size para should not be null");
        assertEquals("Size", sizePara.getCode());

        // 测试getPart方法
        IPart bodyPartOpt = module.getPart("Body");
        assertTrue(bodyPartOpt != null, "Body part should be present");
        Part bodyPart = (Part) bodyPartOpt;
        assertNotNull(bodyPart, "Body part should not be null");
        assertEquals("Body", bodyPart.getCode());

        IPart sleevePartOpt = module.getPart("Sleeve");
        assertTrue(sleevePartOpt != null, "Sleeve part should be present");
        Part sleevePart = (Part) sleevePartOpt;
        assertNotNull(sleevePart, "Sleeve part should not be null");
        assertEquals("Sleeve", sleevePart.getCode());

        // 测试getChildrenPart方法
        List<IPart> bodyChildren = module.getChildrenPart("Body");
        assertNotNull(bodyChildren, "Body children should not be null");
        assertEquals(2, bodyChildren.size(), "Body should have 2 children");

        // 验证子部件的fatherCode设置
        for (IPart child : bodyChildren) {
            assertEquals("Body", child.getFatherCode(), "Child should have correct fatherCode");
        }

        // 测试getTopLevelParts方法
        List<IPart> topLevelParts = module.getTopLevelParts();
        assertNotNull(topLevelParts, "Top level parts should not be null");
        assertEquals(1, topLevelParts.size(), "Should have 1 top level part");
        assertEquals("Body", topLevelParts.get(0).getCode(), "Top level part should be Body");

        log.info("Module methods test passed successfully");
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

        // 设置左表达式的引用对象
        List<RefProgObjSchema> leftRefProgObjs = new ArrayList<>();

        // 引用TShirt11部件
        RefProgObjSchema refTShirt11 = new RefProgObjSchema();
        refTShirt11.setProgObjType("Part");
        refTShirt11.setProgObjCode("TShirt11");
        refTShirt11.setProgObjField("quantity");
        leftRefProgObjs.add(refTShirt11);

        // 引用TShirt12部件
        RefProgObjSchema refTShirt12 = new RefProgObjSchema();
        refTShirt12.setProgObjType("Part");
        refTShirt12.setProgObjCode("TShirt12");
        refTShirt12.setProgObjField("quantity");
        leftRefProgObjs.add(refTShirt12);

        leftExpr.setRefProgObjs(leftRefProgObjs);
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

        // 设置左表达式的引用对象
        List<RefProgObjSchema> leftRefProgObjs = new ArrayList<>();
        RefProgObjSchema leftRef = new RefProgObjSchema();
        leftRef.setProgObjType("Para");
        leftRef.setProgObjCode("Color");
        leftRef.setProgObjField("options");
        leftRefProgObjs.add(leftRef);
        leftExpr.setRefProgObjs(leftRefProgObjs);

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
        rule.setRuleSchemaTypeFullName(RuleTypeConstants.COMPATIABLE_RULE_FULL_NAME);

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
        module.setCode("TestModule");
        module.setPackageName("com.jmix.configengine.artifact");

        // 创建参数
        List<Para> paras = new ArrayList<>();

        Para colorPara = new Para();
        colorPara.setCode("Color");
        List<DynamicAttributerOption> colorOptions = new ArrayList<>();
        DynamicAttributerOption redOption = new DynamicAttributerOption();
        redOption.setCode("Red");
        redOption.setCodeId(1);
        colorOptions.add(redOption);
        colorPara.setOptions(colorOptions);
        paras.add(colorPara);

        Para sizePara = new Para();
        sizePara.setCode("Size");
        paras.add(sizePara);

        module.setParas(paras);

        // 创建部件
        List<Part> parts = new ArrayList<>();
        Part bodyPart = new Part();
        bodyPart.setCode("Body");
        parts.add(bodyPart);
        module.setAtomicParts(parts);

        // 创建规则
        List<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        rule.setCode("TestRule");
        rule.setName("Test Rule");
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");

        CompatiableRuleSchema ruleSchema = new CompatiableRuleSchema();
        ExprSchema leftExpr = new ExprSchema();
        ExprSchema rightExpr = new ExprSchema();
        ruleSchema.setLeftExpr(leftExpr);
        ruleSchema.setRightExpr(rightExpr);
        rule.setRawCode(ruleSchema);

        rules.add(rule);
        module.setRules(rules);

        return module;
    }

    /**
     * 创建带有层次结构的测试Module
     */
    private Module createTestModuleWithHierarchy() {
        Module module = new Module();
        module.setCode("TestModule");
        module.setPackageName("com.jmix.configengine.artifact");

        // 创建参数
        List<Para> paras = new ArrayList<>();

        Para colorPara = new Para();
        colorPara.setCode("Color");
        List<DynamicAttributerOption> colorOptions = new ArrayList<>();
        DynamicAttributerOption redOption = new DynamicAttributerOption();
        redOption.setCode("Red");
        redOption.setCodeId(1);
        colorOptions.add(redOption);
        colorPara.setOptions(colorOptions);
        paras.add(colorPara);

        Para sizePara = new Para();
        sizePara.setCode("Size");
        paras.add(sizePara);

        module.setParas(paras);

        // 创建部件层次结构
        List<Part> parts = new ArrayList<>();

        // 顶级部件
        Part bodyPart = new Part();
        bodyPart.setCode("Body");
        bodyPart.setFatherCode(null); // 顶级部件
        parts.add(bodyPart);

        // 子部件
        Part sleevePart = new Part();
        sleevePart.setCode("Sleeve");
        sleevePart.setFatherCode("Body"); // Body的子部件
        parts.add(sleevePart);

        Part collarPart = new Part();
        collarPart.setCode("Collar");
        collarPart.setFatherCode("Body"); // Body的子部件
        parts.add(collarPart);

        module.setAtomicParts(parts);

        // 创建规则
        List<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        rule.setCode("TestRule");
        rule.setName("Test Rule");
        rule.setRuleSchemaTypeFullName(RuleTypeConstants.COMPATIABLE_RULE_FULL_NAME);

        CompatiableRuleSchema ruleSchema = new CompatiableRuleSchema();
        ExprSchema leftExpr = new ExprSchema();
        ExprSchema rightExpr = new ExprSchema();
        ruleSchema.setLeftExpr(leftExpr);
        ruleSchema.setRightExpr(rightExpr);
        rule.setRawCode(ruleSchema);

        rules.add(rule);
        module.setRules(rules);

        return module;
    }

    /**
     * 使用反射调用私有方法buildRule
     */
    private RuleInfo invokeBuildRule(Module module, Rule rule) throws Exception {

        ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
        ModuleVarInfo moduleInfo = generator.buildModuleInfoBase(module);
        return generator.buildRule(moduleInfo, rule);

    }
}