package com.jmix.configengine.util;

import com.jmix.configengine.model.Extensible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter Expression Executor
 */
public class FilterExpressionExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(FilterExpressionExecutor.class);
    
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*\"?([^\"]*)\"?");
    
    /**
     * Filter objects based on filter expression
     */
    public static <T extends Extensible> List<T> doSelect(List<T> objects, String filterExpr) {
        log.info("Starting filter expression execution, object count: {}, filter expression: {}", 
                objects != null ? objects.size() : 0, filterExpr);
        
        if (objects == null || objects.isEmpty() || filterExpr == null || filterExpr.trim().isEmpty()) {
            log.warn("Invalid input parameters, returning original object list");
            return objects;
        }
        
        List<T> filterObjects = new ArrayList<>();
        
        for (T object : objects) {
            if (matchesFilter(object, filterExpr)) {
                filterObjects.add(object);
            }
        }
        
        log.info("Filtering completed, found {} objects", filterObjects.size());
        return filterObjects;
    }
    
    /**
     * Check if object matches filter condition
     */
    private static <T extends Extensible> boolean matchesFilter(T object, String filterExpr) {
        try {
            Matcher matcher = FILTER_PATTERN.matcher(filterExpr);
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String operator = matcher.group(2);
                String value = matcher.group(3);
                
                log.debug("Filter expression parsed successfully - field: {}, operator: {}, value: {}", fieldName, operator, value);
                return evaluateCondition(object, fieldName, operator, value);
            } else {
                log.warn("Invalid filter expression format: {}", filterExpr);
            }
        } catch (Exception e) {
            log.error("Exception occurred while parsing filter expression: {}", filterExpr, e);
            // Return false if parsing fails
            return false;
        }
        
        return false;
    }
    
    /**
     * Evaluate condition
     */
    private static <T extends Extensible> boolean evaluateCondition(T object, String fieldName, String operator, String value) {
        try {
            // Try to get field value through reflection
            Object fieldValue = getFieldValue(object, fieldName);
            
            if (fieldValue == null) {
                log.debug("Field {} value is null, condition not matched", fieldName);
                return false;
            }
            
            String fieldValueStr = fieldValue.toString();
            log.debug("Evaluating condition - field: {}, operator: {}, field value: {}, compare value: {}", 
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
                    log.warn("Unsupported operator: {}", operator);
                    return false;
            }
            
            log.debug("Condition evaluation result: {} {} {} = {}", fieldValueStr, operator, value, result);
            return result;
        } catch (Exception e) {
            log.error("Exception occurred while evaluating condition - field: {}, operator: {}, value: {}", fieldName, operator, value, e);
            return false;
        }
    }
    
    /**
     * Get field value
     */
    private static Object getFieldValue(Object object, String fieldName) throws Exception {
        try {
            // Try to get extended attributes
            if (object instanceof Extensible) {
                Extensible extensible = (Extensible) object;
                String extValue = extensible.getExtAttr(fieldName);
                if (extValue != null) {
                    log.debug("Getting field value from extended attributes - field: {}, value: {}", fieldName, extValue);
                    return extValue;
                }
            }
            
            // Try to get field value through reflection
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);
            log.debug("Getting field value through reflection - field: {}, value: {}", fieldName, value);
            return value;
        } catch (Exception e) {
            log.debug("Failed to get field value - field: {}, object type: {}", fieldName, object.getClass().getSimpleName(), e);
            // Return null if getting fails
            return null;
        }
    }
    
    /**
     * Compare values
     */
    private static int compareValues(String value1, String value2) {
        try {
            // Try to convert to numbers for comparison
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            int result = Double.compare(num1, num2);
            log.debug("Numeric comparison: {} vs {} = {}", value1, value2, result);
            return result;
        } catch (NumberFormatException e) {
            // If cannot convert to numbers, compare as strings
            int result = value1.compareTo(value2);
            log.debug("String comparison: {} vs {} = {}", value1, value2, result);
            return result;
        }
    }
} 