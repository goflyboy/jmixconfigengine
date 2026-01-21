package com.jmix.executor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.DynamicAttributerOption;
import com.jmix.executor.bmodel.Para;
import com.jmix.executor.bmodel.ParaType;
import com.jmix.executor.impl.util.ParaTypeHandler;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * ParaTypeHandler测试类
 * 
 * @since 2025-09-23
 */
public class ParaTypeHandlerTest {

    @Test
    public void testGetCodeIdValueInteger() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.INTEGER);

        String result = ParaTypeHandler.getCodeIdValue(para, "123");
        assertEquals("123", result);
    }

    @Test
    public void testGetCodeIdValueEnum() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        DynamicAttributerOption option2 = new DynamicAttributerOption();
        option2.setCode("Blue");
        option2.setCodeId(20);

        para.setOptions(java.util.Arrays.asList(option1, option2));

        String result = ParaTypeHandler.getCodeIdValue(para, "Red");
        assertEquals("10", result);
    }

    @Test
    public void testGetCodeIdValueEnumOptionNotFound() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        para.setOptions(java.util.Arrays.asList(option1));

        assertThrows(RuntimeException.class, () -> {
            ParaTypeHandler.getCodeIdValue(para, "Green");
        });
    }

    @Test
    public void testGetDisplayValueInteger() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.INTEGER);

        String result = ParaTypeHandler.getDisplayValue(para, "123");
        assertEquals("123", result);
    }

    @Test
    public void testGetDisplayValueEnum() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        DynamicAttributerOption option2 = new DynamicAttributerOption();
        option2.setCode("Blue");
        option2.setCodeId(20);

        para.setOptions(java.util.Arrays.asList(option1, option2));

        String result = ParaTypeHandler.getDisplayValue(para, "10");
        assertEquals("Red", result);
    }

    @Test
    public void testGetDisplayValueEnumCodeIdNotFound() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        para.setOptions(java.util.Arrays.asList(option1));

        assertThrows(RuntimeException.class, () -> {
            ParaTypeHandler.getDisplayValue(para, "30");
        });
    }

    @Test
    public void testValidateParaTypeValid() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.INTEGER);

        // 不应该抛出异常
        ParaTypeHandler.validateParaType(para);
    }

    @Test
    public void testValidateParaTypeNull() {
        assertThrows(RuntimeException.class, () -> {
            ParaTypeHandler.validateParaType(null);
        });
    }

    @Test
    public void testHasOptionTrue() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        para.setOptions(java.util.Arrays.asList(option1));

        assertTrue(ParaTypeHandler.hasOption(para, "Red"));
    }

    @Test
    public void testHasOptionFalse() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.ENUM);

        DynamicAttributerOption option1 = new DynamicAttributerOption();
        option1.setCode("Red");
        option1.setCodeId(10);

        para.setOptions(Arrays.asList(option1));

        assertFalse(ParaTypeHandler.hasOption(para, "Blue"));
    }

    @Test
    public void testHasOptionIntegerType() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setParaType(ParaType.INTEGER);

        assertFalse(ParaTypeHandler.hasOption(para, "Red"));
    }
}