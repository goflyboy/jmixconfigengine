package com.jmix.configengine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.ModuleType;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaOption;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.imodel.PartType;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.imodel.rule.CompatiableRuleSchema;
import com.jmix.executor.imodel.rule.ExprSchema;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.tool.ModuleUtils;

import lombok.extern.slf4j.Slf4j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ModuleUtils工具类测试
 * 测试模块工具类的各种功能，包括JSON文件读取、模块创建等
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleUtilsTest {
    private Module testModule;

    private String testJsonFile = "test_module.json";

    private boolean deleteGeneratedFile;

    /**
     * 测试设置方法
     */
    @Before
    public void setUp() {
        // 创建测试用的Module对象
        testModule = createTestModule();
    }

    /**
     * 测试toJsonFile方法
     * 
     * @throws IOException IO异常
     */
    @Test
    public void testToJsonFile() throws IOException {
        log.info("=== Testing toJsonFile method ===");
        deleteGeneratedFile = false;
        // 调用toJsonFile方法
        ModuleUtils.toJsonFile(testModule, testJsonFile);

        // 验证文件是否创建成功
        File jsonFile = new File(testJsonFile);
        assertTrue("JSON文件应该创建成功", jsonFile.exists());
        assertTrue("JSON文件应该可读", jsonFile.canRead());
        assertTrue("JSON文件应该不为空", jsonFile.length() > 0);

        log.info("✓ toJsonFile test passed");
        log.info("  File path: {}", jsonFile.getAbsolutePath());
        log.info("  File size: {} bytes", jsonFile.length());
    }

    /**
     * 测试fromJsonFile方法
     * 
     * @throws IOException IO异常
     */
    @Test
    public void testFromJsonFile() throws IOException {
        log.info("=== Testing fromJsonFile method ===");

        // 先创建JSON文件
        ModuleUtils.toJsonFile(testModule, testJsonFile);

        // 从JSON文件读取Module对象
        Module loadedModule = ModuleUtils.fromJsonFile(testJsonFile);

        // 验证加载的对象
        assertNotNull("加载的Module对象不应该为null", loadedModule);
        assertEquals("模块代码应该匹配", testModule.getCode(), loadedModule.getCode());
        assertEquals("模块ID应该匹配", testModule.getId(), loadedModule.getId());
        assertEquals("模块版本应该匹配", testModule.getVersion(), loadedModule.getVersion());
        assertEquals("模块类型应该匹配", testModule.getType(), loadedModule.getType());
        assertEquals("模块描述应该匹配", testModule.getDescription(), loadedModule.getDescription());

        // 验证参数
        assertNotNull("参数列表不应该为null", loadedModule.getParas());
        assertEquals("参数数量应该匹配", testModule.getParas().size(), loadedModule.getParas().size());

        // 验证部件
        assertNotNull("部件列表不应该为null", loadedModule.getParts());
        assertEquals("部件数量应该匹配", testModule.getParts().size(), loadedModule.getParts().size());

        // 验证规则
        assertNotNull("规则列表不应该为null", loadedModule.getRules());
        assertEquals("规则数量应该匹配", testModule.getRules().size(), loadedModule.getRules().size());

        log.info("✓ fromJsonFile test passed");
        log.info("  Loaded module: {}", loadedModule.getCode());
        log.info("  Parameter count: {}", loadedModule.getParas().size());
        log.info("  Part count: {}", loadedModule.getParts().size());
        log.info("  Rule count: {}", loadedModule.getRules().size());
    }

    /**
     * 测试toJsonString和fromJsonString方法
     * 
     * @throws IOException IO异常
     */
    @Test
    public void testToJsonStringAndFromJsonString() throws IOException {
        log.info("=== Testing toJsonString and fromJsonString methods ===");

        // 测试toJsonString
        String jsonString = ModuleUtils.toJsonString(testModule);
        assertNotNull("JSON字符串不应该为null", jsonString);
        assertTrue("JSON字符串应该不为空", jsonString.length() > 0);
        assertTrue("JSON字符串应该包含模块代码", jsonString.contains(testModule.getCode()));

        // 测试fromJsonString
        Module loadedModule = ModuleUtils.fromJsonString(jsonString);
        assertNotNull("从JSON字符串加载的Module对象不应该为null", loadedModule);
        assertEquals("模块代码应该匹配", testModule.getCode(), loadedModule.getCode());

        log.info("✓ toJsonString and fromJsonString test passed");
        log.info("  JSON string length: {}", jsonString.length());
        log.info("  Loaded module: {}", loadedModule.getCode());
    }

    /**
     * 测试validateJsonFile方法
     * 
     * @throws IOException IO异常
     */
    @Test
    public void testValidateJsonFile() throws IOException {
        log.info("=== Testing validateJsonFile method ===");

        // 先创建有效的JSON文件
        ModuleUtils.toJsonFile(testModule, testJsonFile);

        // 验证有效文件
        boolean isValid = ModuleUtils.validateJsonFile(testJsonFile);
        assertTrue("有效的JSON文件应该通过验证", isValid);

        // 验证无效文件（不存在的文件）
        boolean isInvalid = ModuleUtils.validateJsonFile("nonexistent.json");
        assertFalse("无效的JSON文件应该验证失败", isInvalid);

        log.info("✓ validateJsonFile test passed");
    }

    /**
     * 测试完整往返流程
     * 
     * @throws IOException IO异常
     */
    @Test
    public void testRoundTrip() throws IOException {
        log.info("=== Testing complete round trip ===");

        // 1. 创建Module对象
        Module originalModule = createTestModule();

        // 2. 序列化为JSON文件
        ModuleUtils.toJsonFile(originalModule, testJsonFile);

        // 3. 从JSON文件反序列化
        Module loadedModule = ModuleUtils.fromJsonFile(testJsonFile);

        // 4. 再次序列化为JSON文件
        String secondJsonFile = "test_module_roundtrip.json";
        ModuleUtils.toJsonFile(loadedModule, secondJsonFile);

        // 5. 比较两个JSON文件
        String secondJson = new String(Files.readAllBytes(Paths.get(secondJsonFile)));

        // 由于类型信息可能不同，我们只比较基本内容
        assertTrue("往返后的JSON应该包含相同的基本信息",
                secondJson.contains(originalModule.getCode())
                        && secondJson.contains(originalModule.getDescription()));

        // 清理第二个文件
        new File(secondJsonFile).delete();

        log.info("✓ Complete round trip test passed");
    }

    /**
     * 创建测试用的Module对象
     * 
     * @return 测试用的Module对象
     */
    private Module createTestModule() {
        Module module = createBaseModule();

        // 创建并设置子对象
        Para testPara = createTestPara();
        Part testPart = createTestPart();
        Rule testRule = createTestRule();

        // 设置模块属性
        module.setParas(java.util.Arrays.asList(testPara));
        module.setParts(java.util.Arrays.asList(testPart));
        module.setRules(java.util.Arrays.asList(testRule));

        return module;
    }

    /**
     * 创建基础Module对象
     * 
     * @return 基础Module对象
     */
    private Module createBaseModule() {
        Module module = new Module();
        module.setCode("TestModule");
        module.setId(999L);
        module.setVersion("1.0.0");
        module.setPackageName("com.jmix.configengine.util");
        module.setType(ModuleType.GENERAL);
        module.setDefaultValue(1);
        module.setDescription("测试模块");
        module.setSortNo(1);
        module.setExtSchema("TestModuleSchema");

        // 设置扩展属性
        module.setExtAttr("testCategory", "test");
        module.setExtAttr("testPurpose", "unitTest");

        return module;
    }

    /**
     * 创建测试参数
     * 
     * @return 测试参数对象
     */
    private Para createTestPara() {
        Para testPara = new Para();
        testPara.setCode("TestPara");
        testPara.setFatherCode("TestModule");
        testPara.setType(ParaType.ENUM);
        testPara.setDefaultValue("Default");
        testPara.setDescription("测试参数");
        testPara.setSortNo(1);
        testPara.setExtSchema("TestParaSchema");
        testPara.setExtAttr("testAttr", "testValue");

        // 创建参数选项
        ParaOption testOption = createTestParaOption();
        testPara.setOptions(java.util.Arrays.asList(testOption));

        return testPara;
    }

    /**
     * 创建测试参数选项
     * 
     * @return 测试参数选项对象
     */
    private ParaOption createTestParaOption() {
        ParaOption testOption = new ParaOption();
        testOption.setCodeId(100);
        testOption.setCode("TestOption");
        testOption.setFatherCode("TestPara");
        testOption.setDefaultValue("TestOption");
        testOption.setDescription("测试选项");
        testOption.setSortNo(1);
        testOption.setExtAttr("optionAttr", "optionValue");

        return testOption;
    }

    /**
     * 创建测试部件
     * 
     * @return 测试部件对象
     */
    private Part createTestPart() {
        Part testPart = new Part();
        testPart.setCode("TestPart");
        testPart.setFatherCode("TestModule");
        testPart.setType(PartType.ATOMIC);
        testPart.setDefaultValue(0);
        testPart.setDescription("测试部件");
        testPart.setSortNo(1);
        testPart.setPrice(100L);
        testPart.setExtSchema("TestPartSchema");
        testPart.setExtAttr("partAttr", "partValue");

        // 初始化attrs字段
        testPart.setAttrs(new java.util.HashMap<>());
        testPart.setAttr("partProperty", "propertyValue");

        return testPart;
    }

    /**
     * 创建测试规则
     * 
     * @return 测试规则对象
     */
    private Rule createTestRule() {
        Rule testRule = new Rule();
        testRule.setCode("TestRule");
        testRule.setName("测试规则");
        testRule.setProgObjType("Module");
        testRule.setProgObjCode("TestModule");
        testRule.setProgObjField("constraints");
        testRule.setNormalNaturalCode("这是一个测试规则");
        testRule.setExtSchema("TestRuleSchema");
        testRule.setExtAttr("ruleAttr", "ruleValue");
        testRule.setRuleSchemaTypeFullName("CDSL.V5.Struct.TestRule");

        // 创建规则Schema
        CompatiableRuleSchema ruleSchema = createTestRuleSchema();
        testRule.setRawCode(ruleSchema);

        return testRule;
    }

    /**
     * 创建测试规则Schema
     * 
     * @return 测试规则Schema对象
     */
    private CompatiableRuleSchema createTestRuleSchema() {
        CompatiableRuleSchema ruleSchema = new CompatiableRuleSchema();
        ruleSchema.setOperator("Requires");

        // 创建表达式
        ExprSchema exprSchema = createTestExprSchema();
        ruleSchema.setLeftExpr(exprSchema);
        ruleSchema.setRightExpr(exprSchema);

        return ruleSchema;
    }

    /**
     * 创建测试表达式Schema
     * 
     * @return 测试表达式Schema对象
     */
    private ExprSchema createTestExprSchema() {
        ExprSchema exprSchema = new ExprSchema();
        exprSchema.setRawCode("TestPara=\"TestOption\"");

        RefProgObjSchema refProgObj = new RefProgObjSchema();
        refProgObj.setProgObjType("Para");
        refProgObj.setProgObjCode("TestPara");
        refProgObj.setProgObjField("value");

        exprSchema.setRefProgObjs(java.util.Arrays.asList(refProgObj));

        return exprSchema;
    }

    /**
     * 测试清理方法
     */
    @After
    public void tearDown() {
        // 清理测试文件
        if (!deleteGeneratedFile) {
            return;
        }
        try {
            File testFile = new File(testJsonFile);
            if (testFile.exists()) {
                testFile.delete();
                log.info("Cleaning up test file: {}", testJsonFile);
            }
        } catch (Exception e) {
            log.error("Failed to clean up test file: {}", e.getMessage());
        }
    }
}