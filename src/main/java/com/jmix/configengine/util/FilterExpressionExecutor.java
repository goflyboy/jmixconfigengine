package com.jmix.configengine.util;

import com.jmix.configengine.model.Extensible;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 过滤表达式执行器
 */
public class FilterExpressionExecutor {
    
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*\"?([^\"]*)\"?");
    
    /**
     * 根据过滤表达式筛选对象
     */
    public static <T extends Extensible> List<T> doSelect(List<T> objects, String filterExpr) {
        if (objects == null || objects.isEmpty() || filterExpr == null || filterExpr.trim().isEmpty()) {
            return objects;
        }
        
        List<T> filterObjects = new ArrayList<>();
        
        for (T object : objects) {
            if (matchesFilter(object, filterExpr)) {
                filterObjects.add(object);
            }
        }
        
        return filterObjects;
    }
    
    /**
     * 检查对象是否匹配过滤条件
     */
    private static <T extends Extensible> boolean matchesFilter(T object, String filterExpr) {
        try {
            Matcher matcher = FILTER_PATTERN.matcher(filterExpr);
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String operator = matcher.group(2);
                String value = matcher.group(3);
                
                return evaluateCondition(object, fieldName, operator, value);
            }
        } catch (Exception e) {
            // 如果解析失败，返回false
            return false;
        }
        
        return false;
    }
    
    /**
     * 评估条件
     */
    private static <T extends Extensible> boolean evaluateCondition(T object, String fieldName, String operator, String value) {
        try {
            // 尝试通过反射获取字段值
            Object fieldValue = getFieldValue(object, fieldName);
            
            if (fieldValue == null) {
                return false;
            }
            
            String fieldValueStr = fieldValue.toString();
            
            switch (operator) {
                case "=":
                    return fieldValueStr.equals(value);
                case "!=":
                    return !fieldValueStr.equals(value);
                case ">":
                    return compareValues(fieldValueStr, value) > 0;
                case "<":
                    return compareValues(fieldValueStr, value) < 0;
                case ">=":
                    return compareValues(fieldValueStr, value) >= 0;
                case "<=":
                    return compareValues(fieldValueStr, value) <= 0;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取字段值
     */
    private static Object getFieldValue(Object object, String fieldName) throws Exception {
        try {
            // 尝试获取扩展属性
            if (object instanceof Extensible) {
                Extensible extensible = (Extensible) object;
                String extValue = extensible.getExtAttr(fieldName);
                if (extValue != null) {
                    return extValue;
                }
            }
            
            // 尝试通过反射获取字段值
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            // 如果获取失败，返回null
            return null;
        }
    }
    
    /**
     * 比较值
     */
    private static int compareValues(String value1, String value2) {
        try {
            // 尝试转换为数字进行比较
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            // 如果无法转换为数字，则按字符串比较
            return value1.compareTo(value2);
        }
    }
} 