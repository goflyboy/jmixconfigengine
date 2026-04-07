package com.jmix.executor.cmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块基础实例类
 * ModuleInst和PartCategoryInst的公共基类
 * 提供实例的公共属性和方法
 *
 * @since 2025-01-XX
 */
@Data
public class ModuleBaseInst {

    /**
     * 默认实例ID常量
     */
    public static final int DEFAULT_INSTANCE_ID = 0;

    /**
     * 其他变量值键名
     * 用于存储其他变量的值映射，格式：v1:1,v2:2,v3:3,v4:1
     */
    public static final String OTHER_VARIABLES_VALUE_KEY = "OTHER_VARIABLES_VALUE";

    /**
     * 其他变量备注键名
     * 用于存储其他变量的备注信息，格式：v1:a1OrA3, v2:b1OrB2OrB3
     */
    public static final String OTHER_VARIABLES_MEMO_KEY = "OTHER_VARIABLES_MEMO";

    /**
     * 实例ID，默认为0，多个实例从0开始，如：0，1,2，....
     */
    private int instanceId = DEFAULT_INSTANCE_ID;

    /**
     * 编码
     */
    private String code;

    /**
     * 参数实例列表
     */
    private List<ParaInst> paras = new ArrayList<>();

    /**
     * 部件实例列表（仅当前层的部件）
     */
    private List<PartInst> parts = new ArrayList<>();

    /**
     * 扩展属性
     */
    private Map<String, Object> extAttrs = new HashMap<>();

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

    /**
     * 根据部件代码查询部件实例
     *
     * @param partCode 部件代码
     * @return 部件实例，如果不存在则返回null
     */
    public PartInst queryPart(String partCode) {
        if (partCode == null || partCode.isEmpty()) {
            return null;
        }
        for (PartInst partInst : parts) {
            if (partCode.equals(partInst.getCode())) {
                return partInst;
            }
        }
        return null;
    }

    /**
     * 添加参数实例
     *
     * @param paraInst 参数实例
     */
    public void addParaInst(ParaInst paraInst) {
        paras.add(paraInst);
    }

    /**
     * 添加部件实例
     *
     * @param partInst 部件实例
     */
    public void addPartInst(PartInst partInst) {
        parts.add(partInst);
    }

    /**
     * 生成短字符串表示
     *
     * @param isSimple 是否使用简单格式
     * @return 短字符串
     */
    public String toShortString(boolean isSimple) {
        StringBuilder sb = new StringBuilder();
        // 对paras按code升序排序
        Collections.sort(paras, (a, b) -> a.getCode().compareTo(b.getCode()));
        // 对parts按code升序排序
        Collections.sort(parts, (a, b) -> a.getCode().compareTo(b.getCode()));
        // 添加参数信息
        for (ParaInst para : paras) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(para.toShortString());
        }

        // 添加部件信息
        for (PartInst part : parts) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(part.toShortString(isSimple));
        }
        // 添加其他变量信息
        appendOtherVariablesInfo(sb);

        // 如果有shortCode，则使用shortCode作为前缀
        if (shortCode != null) {
            return shortCode + "(" + sb.toString() + ")";
        }

        return sb.toString();
    }

    /**
     * 添加其他变量信息到字符串构建器
     *
     * @param sb 字符串构建器
     */
    protected void appendOtherVariablesInfo(StringBuilder sb) {
        Object otherVarsValue = extAttrs.get(OTHER_VARIABLES_VALUE_KEY);
        if (!(otherVarsValue instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Long> otherVarKeyMap = (Map<String, Long>) otherVarsValue;
        if (otherVarKeyMap.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append("Other(");
        boolean first = true;
        for (Map.Entry<String, Long> entry : otherVarKeyMap.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        sb.append(")");
    }
}
