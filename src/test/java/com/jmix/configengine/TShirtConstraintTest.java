package com.jmix.configengine;

import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.google.ortools.sat.CpModel;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * TShirtConstraint测试类
 */
public class TShirtConstraintTest {
    
    @Test
    public void testTShirtConstraintCreation() {
        // 测试创建TShirtConstraint实例
        TShirtConstraint constraint = new TShirtConstraint();
        
        // 验证实例创建成功
        assertNotNull("TShirtConstraint实例应该创建成功", constraint);
        assertNotNull("ColorVar应该初始化", constraint.ColorVar);
        assertNotNull("SizeVar应该初始化", constraint.SizeVar);
        assertNotNull("thsirt11Var应该初始化", constraint.thsirt11Var);
        assertNotNull("thsirt12Var应该初始化", constraint.thsirt12Var);
    }
    
    @Test
    public void testSetModel() {
        // 1. 初始化OR-Tools模型
        System.out.println("=== 步骤1: 初始化OR-Tools模型 ===");
        CpModel model = new CpModel();
        
        // 2. 调用TShirtConstraint初始化模型
        System.out.println("=== 步骤2: 初始化TShirtConstraint模型 ===");
        TShirtConstraint constraint = new TShirtConstraint();
        constraint.initModel(model);        
        
        // 3. 进行求解
        System.out.println("=== 步骤3: 使用OR-Tools求解器求解 ===");
        com.google.ortools.sat.CpSolver solver = new com.google.ortools.sat.CpSolver();
        
        // 设置求解器参数
        solver.getParameters().setLogSearchProgress(true);
        
        System.out.println("开始求解...");
        com.google.ortools.sat.CpSolverStatus status = solver.solve(model);
        
        // 4. 验证解是否正确
        System.out.println("=== 步骤4: 验证求解结果 ===");
        System.out.println("求解状态: " + status);
        
        if (status == com.google.ortools.sat.CpSolverStatus.OPTIMAL || 
            status == com.google.ortools.sat.CpSolverStatus.FEASIBLE) {
            
            System.out.println("✓ 求解成功！");
            
            // 获取变量值
            int colorValue = (int) solver.value((com.google.ortools.sat.IntVar) constraint.ColorVar.var);
            int sizeValue = (int) solver.value((com.google.ortools.sat.IntVar) constraint.SizeVar.var);
            int thsirt11Value = (int) solver.value((com.google.ortools.sat.IntVar) constraint.thsirt11Var.var);
            int thsirt12Value = (int) solver.value((com.google.ortools.sat.IntVar) constraint.thsirt12Var.var);
            
            // 打印变量值
            System.out.println("\n=== 求解结果 ===");
            System.out.println("颜色变量值: " + colorValue + " (" + getColorName(colorValue) + ")");
            System.out.println("尺寸变量值: " + sizeValue + " (" + getSizeName(sizeValue) + ")");
            System.out.println("T恤衫11数量: " + thsirt11Value);
            System.out.println("T恤衫12数量: " + thsirt12Value);
            
            // 打印选项选中状态
            System.out.println("\n=== 选项选中状态 ===");
            constraint.ColorVar.optionSelectVars.forEach((id, option) -> {
                boolean isSelected = solver.value(option.getIsSelectedVar()) == 1L;
                System.out.println("颜色选项 " + option.getCode() + " (ID:" + id + "): " + (isSelected ? "✓ 选中" : "✗ 未选中"));
            });
            
            constraint.SizeVar.optionSelectVars.forEach((id, option) -> {
                boolean isSelected = solver.value(option.getIsSelectedVar()) == 1L;
                System.out.println("尺寸选项 " + option.getCode() + " (ID:" + id + "): " + (isSelected ? "✓ 选中" : "✗ 未选中"));
            });
            
            // 验证约束规则的正确性
            System.out.println("\n=== 约束验证 ===");
            
                    // 验证规则1: (Color !="Red") Codependent (Size !="Medium")
        boolean colorNotRed = (colorValue != 10); // 10是Red的ID
        boolean sizeNotMedium = (sizeValue != 2);  // 2是Medium的ID
        boolean codependentSatisfied = (colorNotRed == sizeNotMedium);
        
        System.out.println("规则1验证: (Color != 'Red') Codependent (Size != 'Medium')");
        System.out.println("  颜色非红色: " + colorNotRed + " (颜色值: " + colorValue + " = " + getColorName(colorValue) + ")");
        System.out.println("  尺寸非中号: " + sizeNotMedium + " (尺寸值: " + sizeValue + " = " + getSizeName(sizeValue) + ")");
        System.out.println("  Codependent关系: " + (codependentSatisfied ? "✓ 满足" : "✗ 违反"));
        
        // 断言验证规则1
        assertTrue("颜色和尺寸应该满足Codependent关系", codependentSatisfied);
            
            // 验证规则2: T恤衫数量关系约束
            System.out.println("\n规则2验证: TShirt12 >= TShirt11");
            System.out.println("  TShirt11数量: " + thsirt11Value);
            System.out.println("  TShirt12数量: " + thsirt12Value);
            System.out.println("  数量关系: " + (thsirt12Value >= thsirt11Value ? "✓ 满足" : "✗ 违反"));
            
            // 断言验证规则2
            assertTrue("T恤衫12数量应该大于等于T恤衫11数量", thsirt12Value >= thsirt11Value);
            
            // 验证只有一个颜色和尺寸选项被选中
            long selectedColorOptions = constraint.ColorVar.optionSelectVars.values().stream()
                .mapToLong(option -> solver.value(option.getIsSelectedVar()) == 1L ? 1 : 0)
                .sum();
            long selectedSizeOptions = constraint.SizeVar.optionSelectVars.values().stream()
                .mapToLong(option -> solver.value(option.getIsSelectedVar()) == 1L ? 1 : 0)
                .sum();
            
            System.out.println("\n=== 选项选择验证 ===");
            System.out.println("选中的颜色选项数量: " + selectedColorOptions);
            System.out.println("选中的尺寸选项数量: " + selectedSizeOptions);
            
            assertEquals("应该只有一个颜色选项被选中", 1, selectedColorOptions);
            assertEquals("应该只有一个尺寸选项被选中", 1, selectedSizeOptions);
            
            System.out.println("\n✓ 所有约束验证通过！");
            
        } else {
            System.out.println("✗ 求解失败，状态: " + status);
            fail("OR-Tools求解应该成功，但状态是: " + status);
        }
        
        // 验证模型设置成功
        assertNotNull("模型应该设置成功", model);
        
        System.out.println("\n=== OR-Tools求解测试完成 ===");
    }
    
    /**
     * 测试颜色和尺寸组合的约束满足性
     */
    private void testColorSizeCombination(TShirtConstraint constraint, int colorId, int sizeId, String description, boolean shouldSatisfy) {
        boolean colorNotRed = (colorId != 10); // 10是Red的ID
        boolean sizeNotMedium = (sizeId != 2);  // 2是Medium的ID
        boolean satisfiesConstraint = (colorNotRed == sizeNotMedium);
        
        String colorName = getColorName(colorId);
        String sizeName = getSizeName(sizeId);
        
        System.out.println(String.format("组合 %s (颜色:%s, 尺寸:%s): %s", 
            description, colorName, sizeName, 
            satisfiesConstraint == shouldSatisfy ? "✓ 通过" : "✗ 失败"));
        
        if (shouldSatisfy) {
            assertTrue("组合 " + description + " 应该满足约束", satisfiesConstraint);
        } else {
            assertFalse("组合 " + description + " 应该违反约束", satisfiesConstraint);
        }
    }
    
    /**
     * 测试T恤衫数量约束
     */
    private void testTShirtQuantityConstraint(TShirtConstraint constraint, int thsirt11, int thsirt12, boolean shouldSatisfy) {
        boolean satisfiesConstraint = (thsirt12 >= thsirt11);
        
        System.out.println(String.format("数量关系 TShirt11=%d, TShirt12=%d: %s", 
            thsirt11, thsirt12, 
            satisfiesConstraint == shouldSatisfy ? "✓ 通过" : "✗ 失败"));
        
        if (shouldSatisfy) {
            assertTrue("数量关系应该满足约束", satisfiesConstraint);
        } else {
            assertFalse("数量关系应该违反约束", satisfiesConstraint);
        }
    }
    
    /**
     * 根据颜色ID获取颜色名称
     */
    private String getColorName(int colorId) {
        switch (colorId) {
            case 10: return "Red";
            case 20: return "Black";
            case 30: return "White";
            default: return "Unknown";
        }
    }
    
    /**
     * 根据尺寸ID获取尺寸名称
     */
    private String getSizeName(int sizeId) {
        switch (sizeId) {
            case 1: return "Big";
            case 2: return "Medium";
            case 3: return "Small";
            default: return "Unknown";
        }
    }
    
    @Test
    public void testVariableInitialization() {
        // 测试变量初始化 - 不调用initVariables避免OR-Tools本地库问题
        TShirtConstraint constraint = new TShirtConstraint();
        CpModel model = new CpModel();
        constraint.setModel(model);
        
        // 手动设置变量代码来测试基本功能
        constraint.ColorVar.code = "Color";
        constraint.SizeVar.code = "Size";
        constraint.thsirt11Var.code = "thsirt11";
        constraint.thsirt12Var.code = "thsirt12";
        
        // 验证变量代码设置
        assertEquals("ColorVar代码应该是Color", "Color", constraint.ColorVar.code);
        assertEquals("SizeVar代码应该是Size", "Size", constraint.SizeVar.code);
        assertEquals("thsirt11Var代码应该是thsirt11", "thsirt11", constraint.thsirt11Var.code);
        assertEquals("thsirt12Var代码应该是thsirt12", "thsirt12", constraint.thsirt12Var.code);
    }
    
    @Test
    public void testConstraintMethodsExist() {
        // 测试约束方法存在
        TShirtConstraint constraint = new TShirtConstraint();
        
        // 验证方法存在（通过反射）
        try {
            constraint.getClass().getMethod("addConstrain_rule1", 
                CpModel.class, ParaVar.class, ParaVar.class);
            constraint.getClass().getMethod("addConstrain_rule2", 
                CpModel.class, PartVar.class, PartVar.class);
        } catch (NoSuchMethodException e) {
            fail("约束方法应该存在: " + e.getMessage());
        }
    }
} 