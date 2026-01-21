package com.jmix.executor.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.logic.CodeRuleSchema;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.RuleSchema;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeRuleSchema测试类
 */
public class CodeRuleSchemaTest {

    /**
     * 测试CodeRuleSchema创建
     */
    @Test
    public void testCodeRuleSchemaCreation() {
        // 创建CodeRuleSchema实例
        CodeRuleSchema codeRule = new CodeRuleSchema();

        // 设置基本属性
        codeRule.setType("CodeRule");
        codeRule.setVersion("1.0");
        codeRule.setRawCode("if (param1 > 10) { return false; }");

        // 创建引用编程对象
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        RefProgObjSchema refObj = new RefProgObjSchema();
        refObj.setProgObjType("Parameter");
        refObj.setProgObjCode("param1");
        refObj.setProgObjField("value");
        refProgObjs.add(refObj);

        codeRule.setLeftRefProgObjs(refProgObjs);
        codeRule.setRightRefProgObjs(new ArrayList<>());

        // 验证属性设置
        assertEquals("CodeRule", codeRule.getType());
        assertEquals("1.0", codeRule.getVersion());
        assertEquals("if (param1 > 10) { return false; }", codeRule.getRawCode());
        assertEquals(1, codeRule.getLeftRefProgObjs().size());
        assertEquals("Parameter", codeRule.getLeftRefProgObjs().get(0).getProgObjType());
        assertEquals("param1", codeRule.getLeftRefProgObjs().get(0).getProgObjCode());
        assertEquals("value", codeRule.getLeftRefProgObjs().get(0).getProgObjField());
    }

    /**
     * 测试CodeRuleSchema继承
     */
    @Test
    public void testCodeRuleSchemaInheritance() {
        // 验证CodeRuleSchema正确继承了RuleSchema
        CodeRuleSchema codeRule = new CodeRuleSchema();
        assertTrue(codeRule instanceof RuleSchema);
    }

    /**
     * 测试CodeRuleSchemaJSON序列化
     * 
     * @throws Exception 异常
     */
    @Test
    public void testCodeRuleSchemaJsonSerialization() throws Exception {
        // 测试JSON序列化和反序列化
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建CodeRuleSchema实例
        CodeRuleSchema codeRule = new CodeRuleSchema();
        codeRule.setType("CodeRule");
        codeRule.setVersion("1.0");
        codeRule.setRawCode("if (param1 > 10) { return false; }");

        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        RefProgObjSchema refObj = new RefProgObjSchema();
        refObj.setProgObjType("Parameter");
        refObj.setProgObjCode("param1");
        refObj.setProgObjField("value");
        refProgObjs.add(refObj);
        codeRule.setLeftRefProgObjs(refProgObjs);
        codeRule.setRightRefProgObjs(new ArrayList<>());

        // 序列化为JSON
        String json = mapper.writeValueAsString(codeRule);
        assertNotNull(json);
        assertTrue(json.contains("CodeRule"));
        assertTrue(json.contains("if (param1 > 10) { return false; }"));

        // 反序列化回对象
        CodeRuleSchema deserialized = mapper.readValue(json, CodeRuleSchema.class);
        assertEquals("CodeRule", deserialized.getType());
        assertEquals("1.0", deserialized.getVersion());
        assertEquals("if (param1 > 10) { return false; }", deserialized.getRawCode());
        assertEquals(1, deserialized.getLeftRefProgObjs().size());
    }
}