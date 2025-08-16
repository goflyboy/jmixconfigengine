package com.jmix.configengine.util;

import com.jmix.configengine.model.Extensible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 过滤表达式执行器
 */
public class FilterExpressionExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(FilterExpressionExecutor.class);
    
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*\"?([^\"]*)\"?");
    
    /**
     * 根据过滤表达式筛选对象
     */
    public static <T extends Extensible> List<T> doSelect(List<T> objects, String filterExpr) {
        log.info("开始执行过滤表达式筛选，对象数量: {}, 过滤表达式: {}", 
                objects != null ? objects.size() : 0, filterExpr);
        
        if (objects == null || objects.isEmpty() || filterExpr == null || filterExpr.trim().isEmpty()) {
            log.warn("输入参数无效，返回原始对象列表");
            return objects;
        }
        
        List<T> filterObjects = new ArrayList<>();
        
        for (T object : objects) {
            if (matchesFilter(object, filterExpr)) {
                filterObjects.add(object);
            }
        }
        
        log.info("过滤完成，筛选出 {} 个对象", filterObjects.size());
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
                
                log.debug("解析过滤表达式成功 - 字段: {}, 操作符: {}, 值: {}", fieldName, operator, value);
                return evaluateCondition(object, fieldName, operator, value);
            } else {
                log.warn("过滤表达式格式不正确: {}", filterExpr);
            }
        } catch (Exception e) {
            log.error("解析过滤表达式时发生异常: {}", filterExpr, e);
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
                log.debug("字段 {} 的值为null，条件不匹配", fieldName);
                return false;
            }
            
            String fieldValueStr = fieldValue.toString();
            log.debug("评估条件 - 字段: {}, 操作符: {}, 字段值: {}, 比较值: {}", 
                     fieldName, operator, fieldValueStr, value);
            
            boolean result = false;
            switch (operator) {
                case "=":
                    result = fieldValueStr.equals(value);
                    break;
                case "!=":
                    result = !fieldValueStr.equals(value);
                    break;
                case ">":
                    result = compareValues(fieldValueStr, value) > 0;
                    break;
                case "<":
                    result = compareValues(fieldValueStr, value) < 0;
                    break;
                case ">=":
                    result = compareValues(fieldValueStr, value) >= 0;
                    break;
                case "<=":
                    result = compareValues(fieldValueStr, value) <= 0;
                    break;
                default:
                    log.warn("不支持的操作符: {}", operator);
                    return false;
            }
            
            log.debug("条件评估结果: {} {} {} = {}", fieldValueStr, operator, value, result);
            return result;
        } catch (Exception e) {
            log.error("评估条件时发生异常 - 字段: {}, 操作符: {}, 值: {}", fieldName, operator, value, e);
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
                    log.debug("从扩展属性获取字段值 - 字段: {}, 值: {}", fieldName, extValue);
                    return extValue;
                }
            }
            
            // 尝试通过反射获取字段值
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);
            log.debug("通过反射获取字段值 - 字段: {}, 值: {}", fieldName, value);
            return value;
        } catch (Exception e) {
            log.debug("获取字段值失败 - 字段: {}, 对象类型: {}", fieldName, object.getClass().getSimpleName(), e);
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
            int result = Double.compare(num1, num2);
            log.debug("数值比较: {} 与 {} = {}", value1, value2, result);
            return result;
        } catch (NumberFormatException e) {
            // 如果无法转换为数字，则按字符串比较
            int result = value1.compareTo(value2);
            log.debug("字符串比较: {} 与 {} = {}", value1, value2, result);
            return result;
        }
    }
} 