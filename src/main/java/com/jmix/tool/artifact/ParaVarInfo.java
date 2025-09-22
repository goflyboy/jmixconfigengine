package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Para;

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
    private String code;

    private List<ParaOptionVarInfo> options;

    public ParaVarInfo() {
        super();
    }

    public String getOptionIdsStr() {
        return options.stream().map(ParaOptionVarInfo::getCodeId).map(String::valueOf).collect(Collectors.joining(","));
    }

    public ParaVarInfo(Para para) {
        super(para);
    }
}