package com.jmix.executor.omodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

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

    // module id
    private Long id;

    // module code
    private String code;

    // instance config id
    private String instanceConfigId;

    // instance id 默认为0，多个实例，从0开始，如：0，1,2，....
    private int instanceId;

    private Integer quantity;

    private List<ParaInst> paras;

    private List<PartInst> parts;

    private Map<String, Object> extAttrs = new HashMap<>();

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

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
    public String toShortString() {
        StringBuilder sb = new StringBuilder();

        // 添加参数信息
        if (paras != null) {
            for (ParaInst para : paras) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(para.toShortString());
            }
        }

        // 添加部件信息
        if (parts != null) {
            for (PartInst part : parts) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(part.toShortString());
            }
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
            sb.append(")");
        }
    }

}
