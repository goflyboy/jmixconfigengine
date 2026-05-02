package com.jmix.executor.cmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 诊断约束 — 描述在冲突诊断中被系统内部放宽的约束。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticConstraint {

    /** 约束编码, 如 rule3、input_para_size_small */
    private String code;

    /** 约束类型: RULE / INPUT / SYSTEM */
    private String constraintType;

    /** 人类可读描述, 优先来自 Rule.normalNaturalCode */
    private String naturalCode;

    /** 来源对象, 如 Module / PartCategory / Para / Part */
    private String source;

    /** 诊断权重。用于解释优先级, 不暴露松弛变量名 */
    private int weight;

    /** 用户可读说明 */
    private String description;

    public static final String TYPE_RULE = "RULE";
    public static final String TYPE_INPUT = "INPUT";
    public static final String TYPE_SYSTEM = "SYSTEM";
}
