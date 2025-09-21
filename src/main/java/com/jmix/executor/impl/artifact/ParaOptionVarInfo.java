package com.jmix.executor.impl.artifact;

import com.jmix.executor.imodel.ParaOption;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOptionVarInfo extends VarInfo<ParaOption> {
    /**
     * 选项编码ID
     */
    private int codeId;

    /**
     * 选项编码
     */
    private String code;

    public ParaOptionVarInfo() {
        super(); // 调用VarInfo的默认构造函数
    }

    /**
     * 构造函数
     */
    public ParaOptionVarInfo(ParaOption option) {
        super(option); // 调用VarInfo的构造函数，传入ParaOption（因为ParaOption继承自Extensible）
    }
}