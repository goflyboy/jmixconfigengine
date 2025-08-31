package com.jmix.configengine.util;

import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.ParaType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ParaTypeHandler测试类
 */
public class ParaTypeHandlerTest {
    
    @Test
    public void testGetCodeIdValue_Integer() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.INTEGER);
        
        String result = ParaTypeHandler.getCodeIdValue(para, "123");
        assertEquals("123", result);
    }
    
    @Test
    public void testGetCodeIdValue_Enum() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        ParaOption option2 = new ParaOption();
        option2.setCode("Blue");
        option2.setCodeId(20);
        
        para.setOptions(java.util.Arrays.asList(option1, option2));
        
        String result = ParaTypeHandler.getCodeIdValue(para, "Red");
        assertEquals("10", result);
    }
    
    @Test(expected = RuntimeException.class)
    public void testGetCodeIdValue_Enum_OptionNotFound() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        para.setOptions(java.util.Arrays.asList(option1));
        
        ParaTypeHandler.getCodeIdValue(para, "Green");
    }
    
    @Test
    public void testGetDisplayValue_Integer() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.INTEGER);
        
        String result = ParaTypeHandler.getDisplayValue(para, "123");
        assertEquals("123", result);
    }
    
    @Test
    public void testGetDisplayValue_Enum() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        ParaOption option2 = new ParaOption();
        option2.setCode("Blue");
        option2.setCodeId(20);
        
        para.setOptions(java.util.Arrays.asList(option1, option2));
        
        String result = ParaTypeHandler.getDisplayValue(para, "10");
        assertEquals("Red", result);
    }
    
    @Test(expected = RuntimeException.class)
    public void testGetDisplayValue_Enum_CodeIdNotFound() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        para.setOptions(java.util.Arrays.asList(option1));
        
        ParaTypeHandler.getDisplayValue(para, "30");
    }
    
    @Test
    public void testValidateParaType_Valid() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.INTEGER);
        
        // 不应该抛出异常
        ParaTypeHandler.validateParaType(para);
    }
    
    @Test(expected = RuntimeException.class)
    public void testValidateParaType_Null() {
        ParaTypeHandler.validateParaType(null);
    }
    
    @Test
    public void testHasOption_True() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        para.setOptions(java.util.Arrays.asList(option1));
        
        assertTrue(ParaTypeHandler.hasOption(para, "Red"));
    }
    
    @Test
    public void testHasOption_False() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.ENUM);
        
        ParaOption option1 = new ParaOption();
        option1.setCode("Red");
        option1.setCodeId(10);
        
        para.setOptions(java.util.Arrays.asList(option1));
        
        assertFalse(ParaTypeHandler.hasOption(para, "Blue"));
    }
    
    @Test
    public void testHasOption_IntegerType() {
        Para para = new Para();
        para.setCode("TestPara");
        para.setType(ParaType.INTEGER);
        
        assertFalse(ParaTypeHandler.hasOption(para, "Red"));
    }
} 