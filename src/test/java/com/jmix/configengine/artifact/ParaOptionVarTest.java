package com.jmix.configengine.artifact;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ParaOptionVar测试类
 */
public class ParaOptionVarTest {
    
    @Test
    public void testDefaultConstructor() {
        // 测试默认构造函数
        ParaOptionVar optionVar = new ParaOptionVar();
        
        assertNotNull("ParaOptionVar实例应该创建成功", optionVar);
        assertNull("默认构造函数中code应该为null", optionVar.getCode());
        assertEquals("默认构造函数中codeId应该为0", 0, optionVar.getCodeId());
        assertNull("默认构造函数中isSelectedVar应该为null", optionVar.getIsSelectedVar());
        assertNull("默认构造函数中isHiddenVar应该为null", optionVar.getIsHiddenVar());
        assertNull("默认构造函数中quantityVar应该为null", optionVar.getQuantityVar());
        assertNotNull("默认构造函数中subOptionSelectedVars应该初始化", optionVar.getSubOptionSelectedVars());
        assertEquals("默认构造函数中subOptionSelectedVars应该为空", 0, optionVar.getSubOptionSelectedVars().size());
    }
    
    @Test
    public void testConstructorWithCodeAndCodeId() {
        // 测试带code和codeId的构造函数
        ParaOptionVar optionVar = new ParaOptionVar("Red", 10);
        
        assertEquals("code应该设置为Red", "Red", optionVar.getCode());
        assertEquals("codeId应该设置为10", 10, optionVar.getCodeId());
        assertNull("isSelectedVar应该为null", optionVar.getIsSelectedVar());
        assertNull("isHiddenVar应该为null", optionVar.getIsHiddenVar());
        assertNull("quantityVar应该为null", optionVar.getQuantityVar());
    }
    
    @Test
    public void testConstructorWithCodeCodeIdAndIsSelectedVar() {
        // 测试带code、codeId和isSelectedVar的构造函数
        // 注意：这里我们不能直接创建BoolVar，因为需要OR-Tools环境
        ParaOptionVar optionVar = new ParaOptionVar("Blue", 20, null);
        
        assertEquals("code应该设置为Blue", "Blue", optionVar.getCode());
        assertEquals("codeId应该设置为20", 20, optionVar.getCodeId());
        assertNull("isSelectedVar应该为null", optionVar.getIsSelectedVar());
    }
    
    @Test
    public void testSettersAndGetters() {
        // 测试setter和getter方法
        ParaOptionVar optionVar = new ParaOptionVar();
        
        // 设置基本属性
        optionVar.setCode("Green");
        optionVar.setCodeId(30);
        
        assertEquals("code应该设置为Green", "Green", optionVar.getCode());
        assertEquals("codeId应该设置为30", 30, optionVar.getCodeId());
    }
    
    @Test
    public void testSubOptionSelectedVars() {
        // 测试子选项选中状态映射
        ParaOptionVar optionVar = new ParaOptionVar();
        
        // 验证初始状态
        assertNotNull("subOptionSelectedVars应该初始化", optionVar.getSubOptionSelectedVars());
        assertEquals("初始时subOptionSelectedVars应该为空", 0, optionVar.getSubOptionSelectedVars().size());
        
        // 添加子选项（模拟，实际使用时需要BoolVar）
        optionVar.getSubOptionSelectedVars().put("DarkRed", null);
        optionVar.getSubOptionSelectedVars().put("LightRed", null);
        
        assertEquals("添加子选项后size应该为2", 2, optionVar.getSubOptionSelectedVars().size());
        assertTrue("应该包含DarkRed键", optionVar.getSubOptionSelectedVars().containsKey("DarkRed"));
        assertTrue("应该包含LightRed键", optionVar.getSubOptionSelectedVars().containsKey("LightRed"));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // 测试equals和hashCode方法（由Lombok生成）
        ParaOptionVar optionVar1 = new ParaOptionVar("Red", 10);
        ParaOptionVar optionVar2 = new ParaOptionVar("Red", 10);
        ParaOptionVar optionVar3 = new ParaOptionVar("Blue", 20);
        
        // 测试equals
        assertEquals("相同code和codeId的实例应该相等", optionVar1, optionVar2);
        assertNotEquals("不同code和codeId的实例不应该相等", optionVar1, optionVar3);
        
        // 测试hashCode
        assertEquals("相同实例的hashCode应该相等", optionVar1.hashCode(), optionVar2.hashCode());
        assertNotEquals("不同实例的hashCode不应该相等", optionVar1.hashCode(), optionVar3.hashCode());
    }
    
    @Test
    public void testToString() {
        // 测试toString方法（由Lombok生成）
        ParaOptionVar optionVar = new ParaOptionVar("Red", 10);
        String toString = optionVar.toString();
        
        assertNotNull("toString不应该为null", toString);
        assertTrue("toString应该包含code", toString.contains("Red"));
        assertTrue("toString应该包含codeId", toString.contains("10"));
    }
} 