package com.jmix.tool.model;

import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.artifact.RuleInfo;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构化代码注入器
 * 生成方法体片段并写回源文件，通过文本方式定位方法体，使用标记进行幂等替换
 * 
 * @since 2025-09-22
 */
@Slf4j
public class StructCodeInjector {

    private static final String GEN_START = "//自动生成，请勿编辑--start";

    private static final String GEN_END = "//自动生成，请勿编辑--end";

    private final PseudoToJavaConverter converter = new PseudoToJavaConverter();

    /**
     * 将代码注入到指定方法中
     * 
     * @param source       源代码
     * @param methodName   方法名
     * @param injectedCode 要注入的代码
     * @return 注入后的源代码
     */
    private String injectIntoMethod(String source, String methodName, String injectedCode) {
        // 粗略匹配方法声明行到对应的花括号范围
        // 支持常见修饰符与返回类型，方法名固定，用第一个左花括号定位方法体起始
        Pattern sig = Pattern.compile("(^\\n|^)\\s*([\\t ]*)([a-zA-Z0-9_<>\\[\\] ,@]+)?\\b" + Pattern.quote(methodName)
                + "\\s*\\([^)]*\\)\\s*\\{", Pattern.MULTILINE);
        Matcher m = sig.matcher(source);
        if (!m.find()) {
            return source; // 未找到方法，跳过
        }

        int braceOpen = source.indexOf('{', m.end() - 1);
        if (braceOpen < 0) {
            return source;
        }

        int bodyStart = braceOpen + 1;
        int bodyEnd = findMatchingBrace(source, braceOpen);
        if (bodyEnd < 0) {
            return source;
        }

        String body = source.substring(bodyStart, bodyEnd);

        // 计算缩进：以方法体第一行缩进为准，否则沿用声明行缩进+一个制表符/4空格
        String indent = detectIndentForBody(body).orElseGet(() -> defaultIndentFromMatcher(m));

        // 生成片段
        String generated = converter.convert(injectedCode, indent);
        log.info("Generated code: \n{}", generated);
        // 若已有标记，替换标记之间内容
        int existingStart = body.indexOf(GEN_START);
        int existingEnd = body.indexOf(GEN_END);
        String newBody;
        if (existingStart >= 0 && existingEnd > existingStart) {
            int replaceStart = existingStart;
            int replaceEnd = existingEnd + GEN_END.length();
            newBody = body.substring(0, replaceStart) + generated + body.substring(replaceEnd);
        } else {
            // 否则插入到方法体顶部（去除首行换行）
            String prefix = body.startsWith(System.lineSeparator()) ? "" : System.lineSeparator();
            newBody = prefix + generated + body;
        }

        return source.substring(0, bodyStart) + newBody + source.substring(bodyEnd);
    }

    /**
     * 检测方法体的缩进
     * 
     * @param body 方法体字符串
     * @return 检测到的缩进字符串，如果未找到则返回空
     */
    private Optional<String> detectIndentForBody(String body) {
        String[] lines = body.split(System.lineSeparator(), -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = 0;
            while (idx < line.length() && (line.charAt(idx) == ' ' || line.charAt(idx) == '\t')) {
                idx++;
            }
            return Optional.of(line.substring(0, idx));
        }
        return Optional.empty();
    }

    /**
     * 从匹配器获取默认缩进
     * 
     * @param m 正则表达式匹配器
     * @return 默认缩进字符串
     */
    private String defaultIndentFromMatcher(Matcher m) {
        String declIndent = m.group(2) == null ? "" : m.group(2);
        // 保持风格：若声明行以tab开头则+"\t"，否则+4空格
        if (declIndent.contains("\t")) {
            return declIndent + "\t";
        }
        return declIndent + "    ";
    }

    /**
     * 查找匹配的闭合大括号
     * 
     * @param s         字符串
     * @param openIndex 开始大括号的索引
     * @return 匹配的闭合大括号索引，如果未找到则返回-1
     */
    private int findMatchingBrace(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i; // 返回匹配的闭合大括号索引
                }
            } else {
                continue;
            }
        }
        return -1;
    }

    /**
     * 简易伪代码到Java片段转换器。
     * 目前支持：
     * if <CondVar>.var==<Value> then <TargetVar>.var=<Value>
     * 结尾多余右括号会被忽略。
     */
    public static class PseudoToJavaConverter {

        private static final Pattern SIMPLE_IF_ASSIGN = Pattern.compile(
                "^\\s*if\\s+([A-Za-z_][A-Za-z0-9_]*)\\.var\\s*==\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*then\\s*([A-Za-z_]"
                        + "[A-Za-z0-9_]*)\\.var\\s*=\\s*([A-Za-z_][A-Za-z0-9_]*)\\)?\\s*$",
                Pattern.CASE_INSENSITIVE);

        /**
         * 将简单伪代码转换为Java代码块（不含方法签名）。
         * 生成的字段访问按 .var 形式，比较使用字符串常量包装。
         */
        public String convert(String injectedCode, String indent) {
            if (injectedCode == null) {
                return "";
            }
            String code = injectedCode.trim();
            // 移除末尾单个多余右括号
            if (code.endsWith(")")) {
                code = code.substring(0, code.length() - 1).trim();
            }
            Matcher m = SIMPLE_IF_ASSIGN.matcher(code);
            if (m.matches()) {
                String condVar = m.group(1);
                String condVal = m.group(2);
                String targetVar = m.group(3);
                String targetVal = m.group(4);
                StringBuilder sb = new StringBuilder();
                sb.append(indent).append("//自动生成，请勿编辑--start").append(System.lineSeparator());
                sb.append(indent).append("if (\"").append(condVal).append("\".equals(String.valueOf(")
                        .append(condVar).append(".var))) {").append(System.lineSeparator());
                sb.append(indent).append("\t").append(targetVar).append(".var = \"")
                        .append(targetVal).append("\";").append(System.lineSeparator());
                sb.append(indent).append("}").append(System.lineSeparator());
                sb.append(indent).append("//自动生成，请勿编辑--end").append(System.lineSeparator());
                return sb.toString();
            }
            // 默认回退为注释块，避免破坏编译
            return indent + System.lineSeparator() + System.lineSeparator() + indent + "//自动生成，请勿编辑--start"
                    + System.lineSeparator()
                    // + indent + "// 未识别的伪代码: " + escapeForComment(code) + System.lineSeparator()
                    + indent + escapeForComment(code) + System.lineSeparator()
                    + indent + "//自动生成，请勿编辑--end" + System.lineSeparator();
        }

        /**
         * 转义注释中的特殊字符
         * 
         * @param s 需要转义的字符串
         * @return 转义后的字符串
         */
        private String escapeForComment(String s) {
            return s.replace("*/", "* /");
        }
    }

    /**
     * 注入规则到类中
     * 
     * @param clazz     目标类
     * @param ruleInfos 规则信息列表
     */
    public void injectRule(Class<?> clazz, List<RuleInfo> ruleInfos) {
        for (RuleInfo ruleInfo : ruleInfos) {
            injectRule(clazz, ruleInfo);
        }
    }

    /**
     * 注入单个规则到类中
     * 
     * @param clazz    目标类
     * @param ruleInfo 规则信息
     */
    public void injectRule(Class<?> clazz, RuleInfo ruleInfo) {
        // 根据ruleInfo：对CompatiableRule做下列处理，其它暂时不处理
        if (!ruleInfo.getRuleSchemaTypeFullName().contains("CompatiableRule")) {
            return; // 暂时只处理CompatiableRule
        }

        // 根据clazz得到source
        String sourceFile = getSourceFile(clazz);
        try {
            String content = new String(Files.readAllBytes(Paths.get(sourceFile)), StandardCharsets.UTF_8);
            String methodName = ruleInfo.getCode();
            String injectedCodeTmp = generatorInjectorCode(ruleInfo);
            String updated = injectIntoMethod(content, methodName, injectedCodeTmp);

            // 注入
            if (!updated.equals(content)) {
                Files.write(Paths.get(sourceFile), updated.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new AlgLoaderException("Failed to read/write source file: " + sourceFile, e);
        }
    }

    /**
     * 获取类的源文件路径
     * 
     * @param clazz 类对象
     * @return 源文件路径
     */
    private String getSourceFile(Class<?> clazz) {
        String fileName = clazz.getSimpleName();
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null) {
            // 是内部类，设置父类名称
            fileName = enclosingClass.getSimpleName();
        }
        String packagePath = CommHelper.getResourcePath(clazz);
        return packagePath + File.separator + fileName + ".java";
    }

    /**
     * 生成注入代码
     * 
     * @param ruleInfo 规则信息
     * @return 生成的注入代码
     * @throws AlgLoaderException 当规则类型不支持时
     */
    private String generatorInjectorCode(RuleInfo ruleInfo) {
        if (ruleInfo.getRuleSchemaTypeFullName().contains("CompatiableRule")) {
            return generatorInjectorCode4Compatible(ruleInfo);
        }
        throw new AlgLoaderException("Unsupported rule type: " + ruleInfo.getRuleSchemaTypeFullName());
    }

    /**
     * 生成兼容性规则的注入代码
     * 
     * @param ruleInfo 规则信息
     * @return 生成的兼容性规则注入代码
     */
    private String generatorInjectorCode4Compatible(RuleInfo ruleInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("addCompatibleConstraint").append(ruleInfo.getCompatiableOperator()).append("(\"")
                .append(ruleInfo.getCode()).append("\", ");

        // 这里简化处理，实际应该根据ruleInfo的schema解析左右表达式
        sb.append("this.").append(ruleInfo.getLeftVarName()).append(", ");
        sb.append(toArgumentString(ruleInfo.getLeftFilterCodes()));
        sb.append(", this.").append(ruleInfo.getRightVarName()).append(", ");
        sb.append(toArgumentString(ruleInfo.getRightFilterCodes()));
        sb.append(");");
        log.info("Generated method body: \n{}", sb.toString());
        return sb.toString();
    }

    /**
     * 将过滤代码列表转换为参数字符串
     * 
     * @param filterCodes 过滤代码列表
     * @return 格式化的参数字符串
     */
    private String toArgumentString(List<String> filterCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("listOf(");
        for (int i = 0; i < filterCodes.size(); i++) {
            sb.append("\"").append(filterCodes.get(i)).append("\"");
            if (i < filterCodes.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}