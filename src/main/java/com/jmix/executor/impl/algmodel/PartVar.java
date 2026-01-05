package com.jmix.executor.impl.algmodel;

import com.jmix.executor.imodel.Part;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件变量
 * 表示约束求解中的部件变量，包含数量和隐藏状态
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartVar extends Var<Part> {
    /**
     * 部件变量命名模式常量
     */
    public static final String VAR_PATTERN_PREFIX = "##Part.";

    /**
     * 部件数量Var的命名模式
     */
    public static final String QTY_PATTERN = VAR_PATTERN_PREFIX + "qty.%s";

    /**
     * 显示隐藏Var的命名模式
     */
    public static final String HIDDEN_PATTERN = VAR_PATTERN_PREFIX + "isHidden.%s";

    /**
     * 部件数量Var的短名称
     */
    public static final String QTY_SHORT_NAME = "Q";

    /**
     * 显示隐藏Var的短名称
     */
    public static final String HIDDEN_SHORT_NAME = "H";

    /**
     * 部件的数量值
     */
    private IntVar qty;

    /**
     * 显示隐藏属性
     */
    private BoolVar isHidden;

    /**
     * 子部件列表
     */
    private List<PartVar> subParts = new ArrayList<>();

    /**
     * 子部件映射表(Part.code -> PartVar)
     */
    private Map<String, PartVar> subPartMap = new HashMap<>();

    /**
     * 是否选中
     */
    private BoolVar isSelected;

    /**
     * 获取变量字符串表示
     * 
     * @param solutionCallback 求解回调
     * @return 变量字符串表示
     */
    @Override
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("PartVar{code=").append(getCode());
        sb.append(", qty=").append(this.qty == null ? "null" : solutionCallback.value(this.getQty()));
        sb.append(", hidden=").append(this.isHidden == null ? "null" : solutionCallback.value(this.getIsHidden()));
        sb.append(", subParts=").append(getSubParts().size());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取短字符串表示
     * 
     * @param solutionCallback 求解回调
     * @return 短字符串表示
     */
    @Override
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        // P1(Q:1,H:0)
        return String.format("%s(%s:%s,%s:%s)", getCode(), QTY_SHORT_NAME,
                this.qty == null ? "null" : solutionCallback.value(this.qty),
                HIDDEN_SHORT_NAME,
                this.isHidden == null ? "null" : solutionCallback.value(this.isHidden));
    }
}
