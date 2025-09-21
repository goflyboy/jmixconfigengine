package com.jmix.configengine.artifact;

import java.util.HashMap;
import java.util.Map;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;
import com.jmix.configengine.model.Part;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部件变量
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartVar extends Var<Part> {
    /**
     * 部件变量命名模式常量
     */
    public static final String VAR_PATTEN_PREFIX = "##Part.";
    public static final String QTY_PATTEN = VAR_PATTEN_PREFIX + "qty.%s"; // ##Part.qty.{code}
    public static final String HIDDEN_PATTEN = VAR_PATTEN_PREFIX + "isHidden.%s"; // ##Part.isHidden.{code}
    public static final String QTY_SHORT_NAME = "Q";
    public static final String HIDDEN_SHORT_NAME = "H";

    /**
     * 部件的数量值
     */
    public IntVar qty;

    /**
     * 显示隐藏属性
     */
    public BoolVar isHidden;

    /**
     * 子部件选中状态(Part.code -> BoolVar)
     */
    public Map<String, BoolVar> subPartSelectedVars = new HashMap<>();

    @Override
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("PartVar{code=").append(getCode());
        if (qty != null) {
            sb.append(", qty=").append(solutionCallback.value(this.qty));
        }
        if (isHidden != null) {
            sb.append(", hidden=").append(solutionCallback.value(this.isHidden));
        }
        if (!subPartSelectedVars.isEmpty()) {
            sb.append(", subParts=").append(subPartSelectedVars.size());
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        // P1(Q:1,H:0)
        return String.format("%s(%s:%s,%s:%s)", getCode(), QTY_SHORT_NAME,
                solutionCallback.value((IntVar) this.qty), HIDDEN_SHORT_NAME, solutionCallback.value(this.isHidden));
    }
}