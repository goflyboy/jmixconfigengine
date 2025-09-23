package com.jmix.configengine.other;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化的数据验证工具类
 * 用于验证T恤衫模块样例数据的完整性和正确性
 */
@Slf4j
public class SimpleDataValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> validationErrors = new ArrayList<>();

    private final List<String> validationWarnings = new ArrayList<>();

    /**
     * 验证JSON文件
     * 
     * @param filePath JSON文件路径
     * @return 验证是否成功
     */
    public boolean validateJsonFile(String filePath) {
        try {
            log.info("Starting JSON file validation: {}", filePath);

            // 读取JSON文件
            File file = new File(filePath);
            if (!file.exists()) {
                addError("文件不存在: " + filePath);
                return false;
            }

            JsonNode rootNode = objectMapper.readTree(file);
            return validateModuleData(rootNode);

        } catch (IOException e) {
            addError("读取JSON文件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证模块数据
     */
    private boolean validateModuleData(JsonNode rootNode) {
        boolean isValid = true;

        // 验证必需字段
        isValid &= validateRequiredFields(rootNode);

        // 验证参数数据
        if (rootNode.has("paras")) {
            isValid &= validateParas(rootNode.get("paras"));
        }

        // 验证部件数据
        if (rootNode.has("parts")) {
            isValid &= validateParts(rootNode.get("parts"));
        }

        // 验证规则数据
        if (rootNode.has("rules")) {
            isValid &= validateRules(rootNode.get("rules"));
        }

        return isValid;
    }

    /**
     * 验证必需字段
     */
    private boolean validateRequiredFields(JsonNode rootNode) {
        boolean isValid = true;
        String[] requiredFields = { "code", "id", "version", "type", "description" };

        for (String field : requiredFields) {
            if (!rootNode.has(field) || rootNode.get(field).isNull()) {
                addError("缺少必需字段: " + field);
                isValid = false;
            }
        }

        // 验证模块类型
        if (rootNode.has("type")) {
            String type = rootNode.get("type").asText();
            if (!isValidModuleType(type)) {
                addError("无效的模块类型: " + type);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * 验证参数数据
     */
    private boolean validateParas(JsonNode parasNode) {
        if (!parasNode.isArray()) {
            addError("paras字段必须是数组");
            return false;
        }

        boolean isValid = true;
        for (int i = 0; i < parasNode.size(); i++) {
            JsonNode paraNode = parasNode.get(i);
            isValid &= validatePara(paraNode, i);
        }

        return isValid;
    }

    /**
     * 验证单个参数
     */
    private boolean validatePara(JsonNode paraNode, int index) {
        boolean isValid = true;

        // 验证参数必需字段
        String[] requiredFields = { "code", "type", "description" };
        for (String field : requiredFields) {
            if (!paraNode.has(field) || paraNode.get(field).isNull()) {
                addError("参数[" + index + "]缺少必需字段: " + field);
                isValid = false;
            }
        }

        // 验证参数类型
        if (paraNode.has("type")) {
            String type = paraNode.get("type").asText();
            if (!isValidParaType(type)) {
                addError("参数[" + index + "]无效的类型: " + type);
                isValid = false;
            }
        }

        // 验证选项
        if (paraNode.has("options")) {
            JsonNode optionsNode = paraNode.get("options");
            if (optionsNode.isArray()) {
                for (int j = 0; j < optionsNode.size(); j++) {
                    isValid &= validateParaOption(optionsNode.get(j), index, j);
                }
            }
        }

        return isValid;
    }

    /**
     * 验证参数选项
     */
    private boolean validateParaOption(JsonNode optionNode, int paraIndex, int optionIndex) {
        boolean isValid = true;

        String[] requiredFields = { "codeId", "code", "description" };
        for (String field : requiredFields) {
            if (!optionNode.has(field) || optionNode.get(field).isNull()) {
                addError("参数[" + paraIndex + "]选项[" + optionIndex + "]缺少必需字段: " + field);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * 验证部件数据
     */
    private boolean validateParts(JsonNode partsNode) {
        if (!partsNode.isArray()) {
            addError("parts字段必须是数组");
            return false;
        }

        boolean isValid = true;
        for (int i = 0; i < partsNode.size(); i++) {
            JsonNode partNode = partsNode.get(i);
            isValid &= validatePart(partNode, i);
        }

        return isValid;
    }

    /**
     * 验证单个部件
     */
    private boolean validatePart(JsonNode partNode, int index) {
        boolean isValid = true;

        // 验证部件必需字段
        String[] requiredFields = { "code", "type", "description" };
        for (String field : requiredFields) {
            if (!partNode.has(field) || partNode.get(field).isNull()) {
                addError("部件[" + index + "]缺少必需字段: " + field);
                isValid = false;
            }
        }

        // 验证部件类型
        if (partNode.has("type")) {
            String type = partNode.get("type").asText();
            if (!isValidPartType(type)) {
                addError("部件[" + index + "]无效的类型: " + type);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * 验证规则数据
     */
    private boolean validateRules(JsonNode rulesNode) {
        if (!rulesNode.isArray()) {
            addError("rules字段必须是数组");
            return false;
        }

        boolean isValid = true;
        for (int i = 0; i < rulesNode.size(); i++) {
            JsonNode ruleNode = rulesNode.get(i);
            isValid &= validateRule(ruleNode, i);
        }

        return isValid;
    }

    /**
     * 验证单个规则
     */
    private boolean validateRule(JsonNode ruleNode, int index) {
        boolean isValid = true;

        // 验证规则必需字段
        String[] requiredFields = { "code", "name", "ruleSchemaTypeFullName", "normalNaturalCode" };
        for (String field : requiredFields) {
            if (!ruleNode.has(field) || ruleNode.get(field).isNull()) {
                addError("规则[" + index + "]缺少必需字段: " + field);
                isValid = false;
            }
        }

        // 验证规则Schema
        if (ruleNode.has("ruleSchemaTypeFullName")) {
            String schema = ruleNode.get("ruleSchemaTypeFullName").asText();
            if (!isValidRuleSchemaTypeFullName(schema)) {
                addError("规则[" + index + "]无效的Schema: " + schema);
                isValid = false;
            }
        }

        // 验证rawCode
        if (ruleNode.has("rawCode")) {
            JsonNode rawCodeNode = ruleNode.get("rawCode");
            if (!rawCodeNode.isObject()) {
                addWarning("规则[" + index + "]rawCode应该是对象格式");
            }
        }

        return isValid;
    }

    /**
     * 验证模块类型
     */
    private boolean isValidModuleType(String type) {
        String[] validTypes = { "General", "SET", "Template", "Tool" };
        for (String validType : validTypes) {
            if (validType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证参数类型
     */
    private boolean isValidParaType(String type) {
        String[] validTypes = { "EnumType", "Boolean", "Integer", "Float", "Double", "String", "Range", "Date",
                "MultiEnum", "Group" };
        for (String validType : validTypes) {
            if (validType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证部件类型
     */
    private boolean isValidPartType(String type) {
        String[] validTypes = { "AtomicPart", "PartCategory", "Bundle", "Group" };
        for (String validType : validTypes) {
            if (validType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证规则Schema
     */
    private boolean isValidRuleSchemaTypeFullName(String schema) {
        return schema.contains("CompatiableRule")
                || schema.contains("CalculateRule")
                || schema.contains("SelectRule");
    }

    /**
     * 添加错误信息
     */
    private void addError(String error) {
        validationErrors.add("ERROR: " + error);
        log.error("ERROR: {}", error);
    }

    /**
     * 添加警告信息
     */
    private void addWarning(String warning) {
        validationWarnings.add("WARNING: " + warning);
        log.warn("WARNING: {}", warning);
    }

    /**
     * 获取验证结果
     * 
     * @return 验证结果对象，包含错误和警告信息
     */
    public ValidationResult getValidationResult() {
        return new ValidationResult(validationErrors, validationWarnings);
    }

    /**
     * 打印验证结果
     */
    public void printValidationResult() {
        log.info("=== Data Validation Results ===");

        if (validationErrors.isEmpty() && validationWarnings.isEmpty()) {
            log.info("✅ Validation passed, data format is correct!");
            return;
        }

        if (!validationErrors.isEmpty()) {
            log.error("\n❌ Validation errors:");
            for (String error : validationErrors) {
                log.error("  {}", error);
            }
        }

        if (!validationWarnings.isEmpty()) {
            log.warn("\n⚠️  Validation warnings:");
            for (String warning : validationWarnings) {
                log.warn("  {}", warning);
            }
        }

        log.info("\nTotal: {} errors, {} warnings", validationErrors.size(), validationWarnings.size());
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final List<String> errors;

        private final List<String> warnings;

        /**
         * 构造验证结果对象
         * 
         * @param errors   错误信息列表
         * @param warnings 警告信息列表
         */
        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        /**
         * 获取错误信息列表
         * 
         * @return 错误信息列表
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * 获取警告信息列表
         * 
         * @return 警告信息列表
         */
        public List<String> getWarnings() {
            return warnings;
        }

        /**
         * 检查验证是否通过
         * 
         * @return 如果没有错误则返回true，否则返回false
         */
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * 获取错误数量
         * 
         * @return 错误数量
         */
        public int getErrorCount() {
            return errors.size();
        }

        /**
         * 获取警告数量
         * 
         * @return 警告数量
         */
        public int getWarningCount() {
            return warnings.size();
        }
    }

    /**
     * 主方法，用于测试验证功能
     */
    public static void main(String[] args) {
        log.info("🚀 Starting T-shirt module data validation tool");

        SimpleDataValidator validator = new SimpleDataValidator();

        // 验证样例数据文件
        String jsonFilePath = "doc/T恤衫模块样例数据.json";

        log.info("\n📁 Starting file validation: {}", jsonFilePath);
        log.info("==================================");

        validator.validateJsonFile(jsonFilePath);

        // 打印验证结果
        validator.printValidationResult();

        // 输出详细结果
        ValidationResult result = validator.getValidationResult();
        if (result.isValid()) {
            log.info("\n🎉 Data validation successful! Ready for constraint rule generation.");
        } else {
            log.error("\n❌ Data validation failed, please fix the above errors and retry.");
        }

        log.info("\n📊 Validation statistics:");
        log.info("  Error count: {}", result.getErrorCount());
        log.info("  Warning count: {}", result.getWarningCount());
        log.info("  Validation status: {}", (result.isValid() ? "PASSED" : "FAILED"));
    }
}