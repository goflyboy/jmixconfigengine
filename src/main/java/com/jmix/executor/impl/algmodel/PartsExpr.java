package com.jmix.executor.impl.algmodel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部件表达式
 * 用于存储部件表达式解析后的结果，包含过滤条件和部件列表
 * 
 * @since 2025-09-22
 */
@Data
public class PartsExpr {

    /**
     * 所有部件变量列表
     */
    private List<PartVar> partVars = new ArrayList<>();

    /**
     * 满足过滤条件的部件变量列表
     */
    private List<PartVar> filterPartVars = new ArrayList<>();

    /**
     * 不满足过滤条件的部件变量列表
     */
    private List<PartVar> noFilterPartVars = new ArrayList<>();

    /**
     * 过滤条件字符串
     */
    private String filterConditionStr;

    /**
     * 判断过滤部件是否为空
     * 
     * @return 如果过滤部件列表为空返回true
     */
    public boolean isEmpty4FilterPars() {
        return filterPartVars == null || filterPartVars.isEmpty();
    }

    /**
     * 判断非过滤部件是否为空
     * 
     * @return 如果非过滤部件列表为空返回true
     */
    public boolean isEmpty4NoFilterPars() {
        return noFilterPartVars == null || noFilterPartVars.isEmpty();
    }
}
