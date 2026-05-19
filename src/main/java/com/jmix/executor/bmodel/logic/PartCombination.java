package com.jmix.executor.bmodel.logic;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化规则展开后的部件编码组合。
 *
 * @since 2026-05-19
 */
@Data
public class PartCombination {
    private String code1;

    private String code2;

    private String code3;

    private String sourceSubRuleCode;

    public PartCombination() {
    }

    public PartCombination(String code1, String code2, String sourceSubRuleCode) {
        this.code1 = code1;
        this.code2 = code2;
        this.sourceSubRuleCode = sourceSubRuleCode;
    }

    public PartCombination(String code1, String code2, String code3, String sourceSubRuleCode) {
        this.code1 = code1;
        this.code2 = code2;
        this.code3 = code3;
        this.sourceSubRuleCode = sourceSubRuleCode;
    }

    public List<String> getCodes(int arity) {
        List<String> codes = new ArrayList<>();
        if (arity >= 1) {
            codes.add(code1);
        }
        if (arity >= 2) {
            codes.add(code2);
        }
        if (arity >= 3) {
            codes.add(code3);
        }
        if (arity < 1 || arity > 3) {
            throw new IllegalArgumentException("Unsupported arity: " + arity);
        }
        return codes;
    }
}
