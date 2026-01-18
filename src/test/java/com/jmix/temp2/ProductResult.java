package com.jmix.temp2;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品求解结果
 * 
 * @since 2025-01-14
 */
@Data
public class ProductResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 解的列表，每个解是一个 PartVar 列表，包含每个部件的 qty 和 isSelected
     */
    private List<List<PartVarSolution>> solutions = new ArrayList<>();

    /**
     * 单个解的 PartVar 值
     */
    @Data
    public static class PartVarSolution {
        /**
         * 部件代码
         */
        private String code;

        /**
         * 数量
         */
        private int qty;

        /**
         * 是否选中
         */
        private boolean isSelected;
    }

    /**
     * 创建成功结果
     */
    public static ProductResult success(List<List<PartVarSolution>> solutions) {
        ProductResult result = new ProductResult();
        result.setSuccess(true);
        result.setSolutions(solutions);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ProductResult failed(String errorMessage) {
        ProductResult result = new ProductResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}

