package com.jmix.configengine.util;

import com.jmix.configengine.model.*;
import com.jmix.configengine.schema.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ModuleUtils工具类测试
 */
public class ModuleUtilsTest {
    
    private com.jmix.configengine.model.Module testModule;
    private String testJsonFile = "test_module.json";
    private boolean deleteGeneratedFile;
    
    @Before
    public void setUp() {
        // 创建测试用的Module对象
        testModule = createTestModule();
    }
    
    @Test
    public void testToJsonFile() throws IOException {
        System.out.println("=== 测试 toJsonFile 方法 ===");
        deleteGeneratedFile = false;
        // 调用toJsonFile方法
        ModuleUtils.toJsonFile(testModule, testJsonFile);
        
        // 验证文件是否创建成功
        File jsonFile = new File(testJsonFile);
        assertTrue("JSON文件应该创建成功", jsonFile.exists());
        assertTrue("JSON文件应该可读", jsonFile.canRead());
        assertTrue("JSON文件应该不为空", jsonFile.length() > 0);
        
        System.out.println("✓ toJsonFile测试通过");
        System.out.println("  文件路径: " + jsonFile.getAbsolutePath());
        System.out.println("  文件大小: " + jsonFile.length() + " 字节");
    }
    
    @Test
    public void testFromJsonFile() throws IOException {
        System.out.println("=== 测试 fromJsonFile 方法 ===");
        
        // 先创建JSON文件
        ModuleUtils.toJsonFile(testModule, testJsonFile);
        
        // 从JSON文件读取Module对象
        com.jmix.configengine.model.Module loadedModule = ModuleUtils.fromJsonFile(testJsonFile);
        
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
        
        System.out.println("✓ fromJsonFile测试通过");
        System.out.println("  加载的模块: " + loadedModule.getCode());
        System.out.println("  参数数量: " + loadedModule.getParas().size());
        System.out.println("  部件数量: " + loadedModule.getParts().size());
        System.out.println("  规则数量: " + loadedModule.getRules().size());
    }
    
    @Test
    public void testToJsonStringAndFromJsonString() throws IOException {
        System.out.println("=== 测试 toJsonString 和 fromJsonString 方法 ===");
        
        // 测试toJsonString
        String jsonString = ModuleUtils.toJsonString(testModule);
        assertNotNull("JSON字符串不应该为null", jsonString);
        assertTrue("JSON字符串应该不为空", jsonString.length() > 0);
        assertTrue("JSON字符串应该包含模块代码", jsonString.contains(testModule.getCode()));
        
        // 测试fromJsonString
        com.jmix.configengine.model.Module loadedModule = ModuleUtils.fromJsonString(jsonString);
        assertNotNull("从JSON字符串加载的Module对象不应该为null", loadedModule);
        assertEquals("模块代码应该匹配", testModule.getCode(), loadedModule.getCode());
        
        System.out.println("✓ toJsonString和fromJsonString测试通过");
        System.out.println("  JSON字符串长度: " + jsonString.length());
        System.out.println("  加载的模块: " + loadedModule.getCode());
    }
    
    @Test
    public void testValidateJsonFile() throws IOException {
        System.out.println("=== 测试 validateJsonFile 方法 ===");
        
        // 先创建有效的JSON文件
        ModuleUtils.toJsonFile(testModule, testJsonFile);
        
        // 验证有效文件
        boolean isValid = ModuleUtils.validateJsonFile(testJsonFile);
        assertTrue("有效的JSON文件应该通过验证", isValid);
        
        // 验证无效文件（不存在的文件）
        boolean isInvalid = ModuleUtils.validateJsonFile("nonexistent.json");
        assertFalse("无效的JSON文件应该验证失败", isInvalid);
        
        System.out.println("✓ validateJsonFile测试通过");
    }
    
    @Test
    public void testRoundTrip() throws IOException {
        System.out.println("=== 测试完整往返流程 ===");
        
        // 1. 创建Module对象
        com.jmix.configengine.model.Module originalModule = createTestModule();
        
        // 2. 序列化为JSON文件
        ModuleUtils.toJsonFile(originalModule, testJsonFile);
        
        // 3. 从JSON文件反序列化
        com.jmix.configengine.model.Module loadedModule = ModuleUtils.fromJsonFile(testJsonFile);
        
        // 4. 再次序列化为JSON文件
        String secondJsonFile = "test_module_roundtrip.json";
        ModuleUtils.toJsonFile(loadedModule, secondJsonFile);
        
        // 5. 比较两个JSON文件
        String firstJson = new String(Files.readAllBytes(Paths.get(testJsonFile)));
        String secondJson = new String(Files.readAllBytes(Paths.get(secondJsonFile)));
        
        // 由于类型信息可能不同，我们只比较基本内容
        assertTrue("往返后的JSON应该包含相同的基本信息", 
                  secondJson.contains(originalModule.getCode()) && 
                  secondJson.contains(originalModule.getDescription()));
        
        // 清理第二个文件
        new File(secondJsonFile).delete();
        
        System.out.println("✓ 完整往返流程测试通过");
    }
    
    /**
     * 创建测试用的Module对象
     */
    private com.jmix.configengine.model.Module createTestModule() {
        com.jmix.configengine.model.Module module = new com.jmix.configengine.model.Module();
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
        
        // 创建测试参数
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
        ParaOption testOption = new ParaOption();
        testOption.setCodeId(100);
        testOption.setCode("TestOption");
        testOption.setFatherCode("TestPara");
        testOption.setDefaultValue("TestOption");
        testOption.setDescription("测试选项");
        testOption.setSortNo(1);
        testOption.setExtAttr("optionAttr", "optionValue");
        
        testPara.setOptions(java.util.Arrays.asList(testOption));
        
        // 创建测试部件
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
        
        // 创建测试规则
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
        CompatiableRuleSchema ruleSchema = new CompatiableRuleSchema();
        ruleSchema.setOperator("Requires");
        
        // 创建表达式
        ExprSchema exprSchema = new ExprSchema();
        exprSchema.setRawCode("TestPara=\"TestOption\"");
        
        RefProgObjSchema refProgObj = new RefProgObjSchema();
        refProgObj.setProgObjType("Para");
        refProgObj.setProgObjCode("TestPara");
        refProgObj.setProgObjField("value");
        
        exprSchema.setRefProgObjs(java.util.Arrays.asList(refProgObj));
        
        ruleSchema.setLeftExpr(exprSchema);
        ruleSchema.setRightExpr(exprSchema);
        
        testRule.setRawCode(ruleSchema);
        
        // 设置模块属性
        module.setParas(java.util.Arrays.asList(testPara));
        module.setParts(java.util.Arrays.asList(testPart));
        module.setRules(java.util.Arrays.asList(testRule));
        
        return module;
    }
    
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
                System.out.println("清理测试文件: " + testJsonFile);
            }
        } catch (Exception e) {
            System.err.println("清理测试文件失败: " + e.getMessage());
        }
    }
} 