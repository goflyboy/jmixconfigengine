package com.jmix.executor.impl.algmodel;

/**
 * 比较操作符枚举
 * 定义约束中支持的比较操作
 *
 * @since 2025-12-27
 */
public enum ComparisonOperator {
    EQUAL("==") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addEquality(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " == " + value;
        }
    },
    NOT_EQUAL("!=") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addDifferent(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " != " + value;
        }
    },
    LESS_THAN("<") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addLessThan(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " < " + value;
        }
    },
    LESS_OR_EQUAL("<=") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addLessOrEqual(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " <= " + value;
        }
    },
    GREATER_THAN(">") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addGreaterThan(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " > " + value;
        }
    },
    GREATER_OR_EQUAL(">=") {
        @Override
        public void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value) {
            cpModel.addGreaterOrEqual(expr, value);
        }

        @Override
        public String getFormulaString(String expr, int value) {
            return expr + " >= " + value;
        }
    };

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * 获取操作符符号
     *
     * @return 操作符字符串
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * 应用约束到CP模型
     *
     * @param cpModel CP模型
     * @param expr    线性表达式
     * @param value   比较值
     */
    public abstract void applyConstraint(AlgCPModel cpModel, AlgCPLinearExpr expr, int value);

    /**
     * 获取公式字符串表示
     *
     * @param expr  表达式字符串
     * @param value 比较值
     * @return 完整的公式字符串
     */
    public abstract String getFormulaString(String expr, int value);

    /**
     * 根据符号获取对应的枚举值
     *
     * @param symbol 操作符符号
     * @return 对应的枚举值
     * @throws IllegalArgumentException 当符号不支持时抛出
     */
    public static ComparisonOperator fromSymbol(String symbol) {
        for (ComparisonOperator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unsupported comparison operator: " + symbol);
    }
}
