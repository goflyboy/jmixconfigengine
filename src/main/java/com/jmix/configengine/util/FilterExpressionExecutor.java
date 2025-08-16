package com.jmix.configengine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.jmix.configengine.model.Extensible;

/**
 * Filter Expression Executor
 */
public class FilterExpressionExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(FilterExpressionExecutor.class);
    
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)\\s*([=!<>]+)\\s*\"?([^\"]*)\"?");
    
    // Class field cache to improve performance
    private static final Map<Class<?>, Map<String, java.lang.reflect.Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Filter condition representation
     */
    private static class FilterCondition {
        private final String fieldName;
        private final String operator;
        private final String value;
        
        public FilterCondition(String fieldName, String operator, String value) {
            this.fieldName = fieldName;
            this.operator = operator;
            this.value = value;
        }
        
        public String getFieldName() { return fieldName; }
        public String getOperator() { return operator; }
        public String getValue() { return value; }
    }
    
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
        
        // Parse filter expression once
        FilterCondition condition = parseFilterExpression(filterExpr);
        if (condition == null) {
            log.warn("Failed to parse filter expression: {}, returning original object list", filterExpr);
            return objects;
        }
        
        log.debug("Filter expression parsed successfully - field: {}, operator: {}, value: {}", 
                condition.getFieldName(), condition.getOperator(), condition.getValue());
        
        List<T> filterObjects = new ArrayList<>();
        
        for (T object : objects) {
            if (matchesFilter(object, condition)) {
                filterObjects.add(object);
            }
        }
        
        log.info("Filtering completed, found {} objects", filterObjects.size());
        return filterObjects;
    }
    
    /**
     * Parse filter expression once and return FilterCondition
     */
    private static FilterCondition parseFilterExpression(String filterExpr) {
        try {
            Matcher matcher = FILTER_PATTERN.matcher(filterExpr);
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String operator = matcher.group(2);
                String value = matcher.group(3);
                
                return new FilterCondition(fieldName, operator, value);
            } else {
                log.warn("Invalid filter expression format: {}", filterExpr);
                return null;
            }
        } catch (Exception e) {
            log.error("Exception occurred while parsing filter expression: {}", filterExpr, e);
            return null;
        }
    }
    
    /**
     * Check if object matches filter condition
     */
    private static <T extends Extensible> boolean matchesFilter(T object, FilterCondition condition) {
        try {
            return evaluateCondition(object, condition.getFieldName(), condition.getOperator(), condition.getValue());
        } catch (Exception e) {
            log.error("Exception occurred while evaluating filter condition for object: {}", object.getClass().getSimpleName(), e);
            return false;
        }
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
     * Get field value with caching and Spring ReflectionUtils
     */
    private static Object getFieldValue(Object object, String fieldName) throws Exception {
        try {
            // Try to get extended attributes first
            if (object instanceof Extensible) {
                Extensible extensible = (Extensible) object;
                String extValue = extensible.getExtAttr(fieldName);
                if (extValue != null) {
                    log.debug("Getting field value from extended attributes - field: {}, value: {}", fieldName, extValue);
                    return extValue;
                }
            }
            
            // Get field from cache or find it using Spring ReflectionUtils
            java.lang.reflect.Field field = getFieldFromCache(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(object);
                log.debug("Getting field value through reflection - field: {}, value: {}, class: {}", 
                         fieldName, value, field.getDeclaringClass().getSimpleName());
                return value;
            }
            
            log.debug("Field {} not found in class {} or its parent classes", fieldName, object.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.debug("Failed to get field value - field: {}, object type: {}", fieldName, object.getClass().getSimpleName(), e);
            // Return null if getting fails
            return null;
        }
    }
    
    /**
     * Get field from cache or find it using Spring ReflectionUtils
     */
    private static java.lang.reflect.Field getFieldFromCache(Class<?> clazz, String fieldName) {
        // Get class field map from cache
        Map<String, java.lang.reflect.Field> classFields = FIELD_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        
        // Check if field is already cached
        java.lang.reflect.Field cachedField = classFields.get(fieldName);
        if (cachedField != null) {
            return cachedField;
        }
        
        // Find field using Spring ReflectionUtils (searches through entire class hierarchy)
        java.lang.reflect.Field field = ReflectionUtils.findField(clazz, fieldName);
        if (field != null) {
            // Cache the found field
            classFields.put(fieldName, field);
            log.debug("Field {} found and cached for class {}", fieldName, clazz.getSimpleName());
        }
        
        return field;
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