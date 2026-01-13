package com.jmix.executor.impl.util;

import com.jmix.executor.imodel.Extensible;
import com.jmix.executor.imodel.InstanceDynAttrValueItem;
import com.jmix.executor.imodel.Part;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 过滤表达式执行器
 * 用于执行过滤表达式，支持对象属性过滤
 * 
 * @since 2025-09-22
 */
@Slf4j
public final class FilterExpressionExecutor {

    // 操作符常量
    public static final String OP_EQUALS = "=";
    public static final String OP_EQUALS_DOUBLE = "==";
    public static final String OP_NOT_EQUALS = "!=";
    public static final String OP_GREATER_THAN = ">";
    public static final String OP_LESS_THAN = "<";
    public static final String OP_GREATER_EQUALS = ">=";
    public static final String OP_LESS_EQUALS = "<=";
    public static final String OP_LIKE = "like";

    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)\\s*([=!<>]+|like)\\s*\"?([^\"]*)\"?");

    // Class field cache to improve performance
    private static final Map<Class<?>, Map<String, java.lang.reflect.Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造器，防止工具类被实例化
     */
    private FilterExpressionExecutor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Filter condition representation
     */
    @Data
    @AllArgsConstructor
    public static class FilterCondition {

        private final String fieldName;

        private final String operator;

        private final String value;
    }

    /**
     * 根据过滤表达式过滤对象列表
     * 
     * @param objects    要过滤的对象列表
     * @param filterExpr 过滤表达式
     * @param <T>        对象类型，必须继承自Extensible
     * @return 过滤后的对象列表
     */
    public static <T extends Extensible> List<T> doSelect(List<T> objects, String filterExpr) {
        log.info("Starting filter expression execution, object count: {}, filter expression: {}",
                objects != null ? objects.size() : 0, filterExpr);

        if (objects == null || objects.isEmpty() || filterExpr == null || filterExpr.trim().isEmpty()) {
            log.warn("Invalid input parameters, returning original object list");
            return objects;
        }
        Optional<FilterCondition> filterCondition = parseFilterExpression(filterExpr);
        return doSelect(objects, filterCondition.get());
    }

    /**
     * 根据过滤表达式过滤对象列表
     * 
     * @param objects         要过滤的对象列表
     * @param filterCondition 过滤表达式
     * @param <T>             对象类型，必须继承自Extensible
     * @return 过滤后的对象列表
     */
    public static <T extends Extensible> List<T> doSelect(List<T> objects, FilterCondition filterCondition) {
        if (filterCondition == null) {
            log.warn("Filter condition is null, returning original object list");
            return objects;
        }

        FilterCondition condition = filterCondition;

        log.info("Filter expression parsed successfully - field: {}, operator: {}, value: {}",
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
     * 
     * @param filterExpr 过滤表达式字符串
     * @return 解析后的过滤条件，如果解析失败则返回空
     */
    public static Optional<FilterCondition> parseFilterExpression(String filterExpr) {
        try {
            Matcher matcher = FILTER_PATTERN.matcher(filterExpr);
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String operator = matcher.group(2);
                String value = matcher.group(3);

                return Optional.of(new FilterCondition(fieldName, operator, value));
            } else {
                log.warn("Invalid filter expression format: {}", filterExpr);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Exception occurred while parsing filter expression: {}", filterExpr, e);
            return Optional.empty();
        }
    }

    /**
     * Check if object matches filter condition
     * 
     * @param object    要检查的对象
     * @param condition 过滤条件
     * @return 如果对象匹配条件则返回true，否则返回false
     */
    private static <T extends Extensible> boolean matchesFilter(T object, FilterCondition condition) {
        try {
            return evaluateCondition(object, condition.getFieldName(), condition.getOperator(), condition.getValue());
        } catch (Exception e) {
            log.error("Exception occurred while evaluating filter condition for object: {}",
                    object.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * Evaluate condition
     * 
     * @param object    要检查的对象
     * @param fieldName 字段名
     * @param operator  操作符
     * @param value     比较值
     * @return 如果条件满足则返回true，否则返回false
     */
    private static <T extends Extensible> boolean evaluateCondition(T object, String fieldName, String operator,
            String value) {
        try {
            // Try to get field value through reflection
            Optional<Object> fieldValueOpt = getFieldValue(object, fieldName);

            if (!fieldValueOpt.isPresent()) {
                log.info("Field {} value is null, condition not matched", fieldName);
                return false;
            }

            Object fieldValue = fieldValueOpt.get();
            String fieldValueStr = fieldValue.toString();
            log.info("Evaluating condition - field: {}, operator: {}, field value: {}, compare value: {}",
                    fieldName, operator, fieldValueStr, value);

            boolean result = false;
            switch (operator) {
                case OP_EQUALS:
                case OP_EQUALS_DOUBLE:
                    result = fieldValueStr.equals(value);
                    break;
                case OP_NOT_EQUALS:
                    result = !fieldValueStr.equals(value);
                    break;
                case OP_GREATER_THAN:
                    result = compareValues(fieldValueStr, value) > 0;
                    break;
                case OP_LESS_THAN:
                    result = compareValues(fieldValueStr, value) < 0;
                    break;
                case OP_GREATER_EQUALS:
                    result = compareValues(fieldValueStr, value) >= 0;
                    break;
                case OP_LESS_EQUALS:
                    result = compareValues(fieldValueStr, value) <= 0;
                    break;
                case OP_LIKE:
                    result = matchesLikePattern(fieldValueStr, value);
                    break;
                default:
                    log.warn("Unsupported operator: {}", operator);
                    return false;
            }

            log.info("Condition evaluation result: {} {} {} = {}", fieldValueStr, operator, value, result);
            return result;
        } catch (Exception e) {
            log.error("Exception occurred while evaluating condition - field: {}, operator: {}, value: {}", fieldName,
                    operator, value, e);
            return false;
        }
    }

    /**
     * Get field value with caching and Spring ReflectionUtils
     * 
     * @param object    要获取字段值的对象
     * @param fieldName 字段名
     * @return 字段值，如果获取失败则返回空
     */
    private static Optional<Object> getFieldValue(Object object, String fieldName) {
        try {
            // Try to get extended attributes first
            if (object instanceof Extensible) {
                Extensible extensible = (Extensible) object;
                String extValue = extensible.getExtAttr(fieldName);
                if (extValue != null) {
                    log.info("Getting field value from extended attributes - field: {}, value: {}", fieldName,
                            extValue);
                    return Optional.of(extValue);
                }
            }
            // Try to get dyn attributes first
            if (object instanceof Part) {
                Part part = (Part) object;
                String attValue = part.getAttr(fieldName);
                if (attValue != null) {
                    log.info("Getting field value from dyn attributes - field: {}, value: {}", fieldName,
                            attValue);
                    return Optional.of(attValue);
                }
            }
            // Try to get dyn attributes first
            if (object instanceof InstanceDynAttrValueItem) {
                InstanceDynAttrValueItem intAttrValue = (InstanceDynAttrValueItem) object;
                String attValue = intAttrValue.getInstAttr(fieldName);
                if (attValue != null) {
                    log.info("Getting field value from InstanceDynAttrValueItem attributes - field: {}, value: {}",
                            fieldName, attValue);
                    return Optional.of(attValue);
                }
            }

            // Get field from cache or find it using Spring ReflectionUtils
            java.lang.reflect.Field field = getFieldFromCache(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(object);
                log.info("Getting field value through reflection - field: {}, value: {}, class: {}",
                        fieldName, value, field.getDeclaringClass().getSimpleName());
                return Optional.ofNullable(value);
            }
            // Try to get extended attributes first
            if (object instanceof Part) {
                Part part = (Part) object;
                String value = part.getAttr(fieldName);
                if (value != null) {
                    log.info("Getting field value from part attributes - field: {}, value: {}", fieldName, value);
                    return Optional.of(value);
                }
            }
            log.info("Field {} not found in class {} or its parent classes", fieldName,
                    object.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception e) {
            log.info("Failed to get field value - field: {}, object type: {}", fieldName,
                    object.getClass().getSimpleName(), e);
            // Return empty if getting fails
            return Optional.empty();
        }
    }

    /**
     * Get field from cache or find it using Spring ReflectionUtils
     * 
     * @param clazz     要查找字段的类
     * @param fieldName 字段名
     * @return 找到的字段，如果未找到则返回null
     */
    private static java.lang.reflect.Field getFieldFromCache(Class<?> clazz, String fieldName) {
        // Get class field map from cache
        Map<String, java.lang.reflect.Field> classFields = FIELD_CACHE.computeIfAbsent(clazz,
                k -> new ConcurrentHashMap<>());

        // Check if field is already cached
        java.lang.reflect.Field cachedField = classFields.get(fieldName);
        if (cachedField != null) {
            return cachedField;
        }

        // Find field using Spring ReflectionUtils (searches through entire class
        // hierarchy)
        java.lang.reflect.Field field = ReflectionUtils.findField(clazz, fieldName);
        if (field != null) {
            // Cache the found field
            classFields.put(fieldName, field);
            log.info("Field {} found and cached for class {}", fieldName, clazz.getSimpleName());
        }

        return field;
    }

    /**
     * Check if value matches like pattern
     *
     * @param fieldValue 字段值
     * @param pattern    like模式字符串，支持%和_通配符
     * @return 如果匹配则返回true，否则返回false
     */
    private static boolean matchesLikePattern(String fieldValue, String pattern) {
        try {
            // Convert SQL like pattern to regex pattern
            String regex = convertLikePatternToRegex(pattern);
            boolean result = fieldValue.matches(regex);
            log.info("Like pattern matching: '{}' like '{}' -> regex: '{}' -> result: {}",
                    fieldValue, pattern, regex, result);
            return result;
        } catch (Exception e) {
            log.error("Exception occurred while matching like pattern - value: {}, pattern: {}", fieldValue, pattern,
                    e);
            return false;
        }
    }

    /**
     * Convert SQL like pattern to regex pattern
     *
     * @param likePattern SQL like模式字符串
     * @return 转换后的正则表达式
     */
    private static String convertLikePatternToRegex(String likePattern) {
        // Escape special regex characters except % and _
        String escaped = likePattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)");

        // Convert SQL wildcards to regex
        String regex = escaped
                .replace("%", ".*") // % matches any sequence of characters
                .replace("_", "."); // _ matches any single character

        // Add anchors to match entire string
        return "^" + regex + "$";
    }

    /**
     * Compare values
     *
     * @param value1 第一个值
     * @param value2 第二个值
     * @return 比较结果：负数表示value1小于value2，0表示相等，正数表示value1大于value2
     */
    private static int compareValues(String value1, String value2) {
        try {
            // Try to convert to numbers for comparison
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            int result = Double.compare(num1, num2);
            log.info("Numeric comparison: {} vs {} = {}", value1, value2, result);
            return result;
        } catch (NumberFormatException e) {
            // If cannot convert to numbers, compare as strings
            int result = value1.compareTo(value2);
            log.info("String comparison: {} vs {} = {}", value1, value2, result);
            return result;
        }
    }
}