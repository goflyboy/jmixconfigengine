package com.jmix.executor.cmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块实例类
 * 表示模块的一个具体实例，包含参数实例和部件实例
 * 
 * @since 2025-09-22
 */
@Data
public class ModuleInst {

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
     * 默认实例配置ID常量
     */
    public static final String DEFAULT_INSTANCE_CONFIG_ID = "0";

    /**
     * 默认实例ID常量
     */
    public static final int DEFAULT_INSTANCE_ID = 0;

    /**
     * 默认数量常量
     */
    public static final int DEFAULT_QUANTITY = 1;

    /**
     * 模块ID
     */
    private Long id;

    /**
     * 模块编码
     */
    private String code;

    /**
     * 实例配置ID
     */
    private String instanceConfigId = DEFAULT_INSTANCE_CONFIG_ID;

    /**
     * 实例ID，默认为0，多个实例从0开始，如：0，1,2，....
     */
    private int instanceId = DEFAULT_INSTANCE_ID;

    /**
     * 数量
     */
    private Integer quantity = DEFAULT_QUANTITY;

    /**
     * 参数实例列表
     */
    private List<ParaInst> paras = new ArrayList<>();

    /**
     * 部件实例列表
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
     * 优先级属性值列表
     * 每个约束的值
     */
    private List<PriorityAttrValue> priorityAttrValues = new ArrayList<>();

    /**
     * 优先级排序号
     */
    private int prioritySortNo = 0;

    /**
     * 优先级综合值
     * 最后综合的值
     */
    private double priorityOverallValue = 0.0;

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
     * @return 短字符串，例如：P1(V:1,H:0),P2(V:1,H:1),PT1(Q:20,H:0),Other(x1:1,x2:2,x3:3,x4:5)
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

        // 添加优先级属性值信息
        appendPriorityAttrValuesInfo(sb);

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
    private void appendOtherVariablesInfo(StringBuilder sb) {
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

    /**
     * 添加优先级属性值信息到字符串构建器
     * 格式：PAs(CA:100,TA:200,....)，attrCode取前两位的大写字符
     * 
     * @param sb 字符串构建器
     */
    private void appendPriorityAttrValuesInfo(StringBuilder sb) {
        sb.append(" PO:").append(priorityOverallValue);
        sb.append(" PS:").append(prioritySortNo);
        if (priorityAttrValues == null || priorityAttrValues.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append("PAs(");
        boolean first = true;
        for (PriorityAttrValue priorityAttrValue : priorityAttrValues) {
            if (!first) {
                sb.append(",");
            }
            String shortCode = toAttrShortCode(priorityAttrValue.getAttrCode());
            sb.append(shortCode).append(":").append(priorityAttrValue.getOptimalValue());
            first = false;
        }
        sb.append(")");
    }

    /**
     * 获取优先级属性短编码字符串
     * 输出 attrCode 和 shortCode 的关系
     * 
     * @return 优先级属性短编码字符串，格式：shortCode:attrCode，多个用逗号分隔
     */
    @JsonIgnore
    public String getPriorityAttrShortCodeStr() {
        if (priorityAttrValues == null || priorityAttrValues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PAs:priorityAttrValues ").append("PO:priorityOverallValue ")
                .append("PS:prioritySortNo");
        sb.append(" (");
        boolean first = true;
        for (PriorityAttrValue priorityAttrValue : priorityAttrValues) {
            String attrCode = priorityAttrValue.getAttrCode();
            if (attrCode == null || attrCode.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            String shortCode = toAttrShortCode(attrCode);
            sb.append(shortCode).append(":").append(attrCode);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 将属性代码转换为短编码
     * 取 attrCode 的前两位，转换为大写
     * 
     * @param attrCode 属性代码
     * @return 短编码，如果 attrCode 为 null 或长度不足则返回空字符串
     */
    private String toAttrShortCode(String attrCode) {
        if (attrCode == null) {
            return "";
        }
        if (attrCode.length() >= 2) {
            return attrCode.substring(0, 2).toUpperCase();
        }
        return attrCode.toUpperCase();
    }

}
