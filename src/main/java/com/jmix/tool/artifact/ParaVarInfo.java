package com.jmix.tool.artifact;

import com.jmix.executor.bmodel.Para;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数变量信息类
 * 包含参数的变量信息，用于代码生成
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaVarInfo extends VarInfo<Para> {
    /**
     * 参数编码
     */
    private String code;

    /**
     * 参数选项变量信息列表
     */
    private List<ParaOptionVarInfo> options;

    /**
     * 默认构造函数
     */
    public ParaVarInfo() {
        super();
    }

    /**
     * 获取选项ID字符串，用逗号分隔
     * 
     * @return 选项ID字符串
     */
    public String getOptionIdsStr() {
        return options.stream().map(ParaOptionVarInfo::getCodeId).map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 带参数对象的构造函数
     * 
     * @param para 参数对象
     */
    public ParaVarInfo(Para para) {
        super(para);
    }
}