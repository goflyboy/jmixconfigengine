package com.jmix.configengine.other;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化的数据验证工具类
 * 用于验证T恤衫模块样例数据的完整性和正确性
 */
public class SimpleDataValidator {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> validationErrors = new ArrayList<>();
    private final List<String> validationWarnings = new ArrayList<>();
    
    /**
     * 验证JSON文件
     */
    public boolean validateJsonFile(String filePath) {
        try {
            System.out.println("开始验证JSON文件: " + filePath);
            
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
        String[] requiredFields = {"code", "id", "version", "type", "description"};
        
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
        String[] requiredFields = {"code", "type", "description"};
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
        
        String[] requiredFields = {"codeId", "code", "description"};
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
        String[] requiredFields = {"code", "type", "description"};
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
        String[] requiredFields = {"code", "name", "ruleSchemaTypeFullName", "normalNaturalCode"};
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
        String[] validTypes = {"General", "SET", "Template", "Tool"};
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
        String[] validTypes = {"EnumType", "Boolean", "Integer", "Float", "Double", "String", "Range", "Date", "MultiEnum", "Group"};
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
        String[] validTypes = {"AtomicPart", "PartCategory", "Bundle", "Group"};
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
        return schema.contains("CompatiableRule") || 
               schema.contains("CalculateRule") || 
               schema.contains("SelectRule");
    }
    
    /**
     * 添加错误信息
     */
    private void addError(String error) {
        validationErrors.add("ERROR: " + error);
        System.err.println("ERROR: " + error);
    }
    
    /**
     * 添加警告信息
     */
    private void addWarning(String warning) {
        validationWarnings.add("WARNING: " + warning);
        System.out.println("WARNING: " + warning);
    }
    
    /**
     * 获取验证结果
     */
    public ValidationResult getValidationResult() {
        return new ValidationResult(validationErrors, validationWarnings);
    }
    
    /**
     * 打印验证结果
     */
    public void printValidationResult() {
        System.out.println("=== 数据验证结果 ===");
        
        if (validationErrors.isEmpty() && validationWarnings.isEmpty()) {
            System.out.println("✅ 验证通过，数据格式正确！");
            return;
        }
        
        if (!validationErrors.isEmpty()) {
            System.out.println("\n❌ 验证错误:");
            for (String error : validationErrors) {
                System.out.println("  " + error);
            }
        }
        
        if (!validationWarnings.isEmpty()) {
            System.out.println("\n⚠️  验证警告:");
            for (String warning : validationWarnings) {
                System.out.println("  " + warning);
            }
        }
        
        System.out.println("\n总计: " + validationErrors.size() + " 个错误, " + validationWarnings.size() + " 个警告");
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public boolean isValid() { return errors.isEmpty(); }
        public int getErrorCount() { return errors.size(); }
        public int getWarningCount() { return warnings.size(); }
    }
    
    /**
     * 主方法，用于测试验证功能
     */
    public static void main(String[] args) {
        System.out.println("🚀 启动T恤衫模块数据验证工具");
        
        SimpleDataValidator validator = new SimpleDataValidator();
        
        // 验证样例数据文件
        String jsonFilePath = "doc/T恤衫模块样例数据.json";
        
        System.out.println("\n📁 开始验证文件: " + jsonFilePath);
        System.out.println("==================================");
        
        boolean isValid = validator.validateJsonFile(jsonFilePath);
        
        // 打印验证结果
        validator.printValidationResult();
        
        // 输出详细结果
        ValidationResult result = validator.getValidationResult();
        if (result.isValid()) {
            System.out.println("\n🎉 数据验证成功！可以用于约束规则生成。");
        } else {
            System.out.println("\n❌ 数据验证失败，请修复上述错误后重试。");
        }
        
        System.out.println("\n📊 验证统计:");
        System.out.println("  错误数量: " + result.getErrorCount());
        System.out.println("  警告数量: " + result.getWarningCount());
        System.out.println("  验证状态: " + (result.isValid() ? "通过" : "失败"));
    }
} 