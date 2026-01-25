package com.jmix.temp3;

import com.jmix.temp3.core.Part;

import lombok.Data;

import java.util.*;

/**
 * 归一化处理器 - 负责将原始属性归一化到[0,100]范围
 * 独立于约束模型，只处理数据
 */
public class NormalizationProcessor {

    // 归一化配置
    public static class NormalizationConfig {
        public String attrName;
        public NormalizationMethod method = NormalizationMethod.LINEAR;
        public double minValue;      // 可选：自定义最小值
        public double maxValue;      // 可选：自定义最大值
        public boolean isQuantityRelated = true; // 是否与数量相关
        public double power = 1.0;   // 幂律归一化的指数
        public int numBins = 10;     // 分箱数量（用于分位数归一化）

        public NormalizationConfig(String attrName) {
            this.attrName = attrName;
        }

        public String getAttrName() { return attrName; }
        public void setMethod(NormalizationMethod method) { this.method = method; }
    }

    // 归一化方法枚举
    public enum NormalizationMethod {
        LINEAR,      // 线性归一化
        LOG,         // 对数归一化
        LOG10,       // 以10为底的对数
        POWER,       // 幂律归一化
        QUANTILE,    // 分位数归一化
        RANK,        // 排名归一化
        PIECEWISE,   // 分段线性
        Z_SCORE      // Z-score归一化
    }

    // 归一化范围
    public static class NormalizationRange {
        public double minValue;
        public double maxValue;
        public double meanValue;
        public double stdValue;
        public List<Double> sortedValues; // 排序后的值（用于分位数）

        public NormalizationRange(double min, double max) {
            this.minValue = min;
            this.maxValue = max;
        }

        public double getRange() {
            return maxValue - minValue;
        }

        public double getMinValue() { return minValue; }
        public double getMaxValue() { return maxValue; }
        public double getMeanValue() { return meanValue; }
        public double getStdValue() { return stdValue; }
        public List<Double> getSortedValues() { return sortedValues; }

        public void setMeanValue(double meanValue) { this.meanValue = meanValue; }
        public void setStdValue(double stdValue) { this.stdValue = stdValue; }
        public void setSortedValues(List<Double> sortedValues) { this.sortedValues = sortedValues; }
    }

    // 归一化结果
    public static class NormalizationResult {
        public String attrName;
        public Map<String, Double> normalizedValues; // partCode -> 归一化值
        public NormalizationRange range;
        public NormalizationMethod method;

        public NormalizationResult(String attrName) {
            this.attrName = attrName;
            this.normalizedValues = new HashMap<>();
        }

        public void addNormalizedValue(String partCode, double value) {
            normalizedValues.put(partCode, value);
        }

        public double getNormalizedValue(String partCode) {
            return normalizedValues.getOrDefault(partCode, 50.0); // 默认50
        }

        public void setRange(NormalizationRange range) { this.range = range; }
        public void setMethod(NormalizationMethod method) { this.method = method; }
    }

    // 主归一化方法
    public NormalizationResult normalize(List<Part> parts, NormalizationConfig config) {
        // 收集原始值
        List<Double> rawValues = collectRawValues(parts, config.attrName);

        // 计算范围
        NormalizationRange range = calculateRange(rawValues, config);

        // 应用归一化方法
        return applyNormalization(parts, config, range);
    }

    // 收集原始值
    private List<Double> collectRawValues(List<Part> parts, String attrName) {
        List<Double> values = new ArrayList<>();
        for (Part part : parts) {
            if (part.hasAttr(attrName)) {
                values.add(part.getAttr(attrName));
            }
        }
        return values;
    }

    // 计算范围
    private NormalizationRange calculateRange(List<Double> values, NormalizationConfig config) {
        if (values.isEmpty()) {
            return new NormalizationRange(0, 1);
        }

        // 如果配置中指定了范围，使用配置的范围
        if (config.minValue != config.maxValue) {
            return new NormalizationRange(config.minValue, config.maxValue);
        }

        // 否则自动计算
        double min = Collections.min(values);
        double max = Collections.max(values);
        double sum = 0.0;

        for (Double val : values) {
            sum += val;
        }
        double mean = sum / values.size();

        // 计算标准差
        double variance = 0.0;
        for (Double val : values) {
            variance += Math.pow(val - mean, 2);
        }
        double std = Math.sqrt(variance / values.size());

        NormalizationRange range = new NormalizationRange(min, max);
        range.setMeanValue(mean);
        range.setStdValue(std);
        range.setSortedValues(new ArrayList<>(values));
        Collections.sort(range.getSortedValues());

        return range;
    }

    // 应用归一化
    private NormalizationResult applyNormalization(List<Part> parts,
                                                  NormalizationConfig config,
                                                  NormalizationRange range) {
        NormalizationResult result = new NormalizationResult(config.attrName);
        result.setRange(range);
        result.setMethod(config.method);

        for (Part part : parts) {
            if (!part.hasAttr(config.attrName)) {
                continue;
            }

            double rawValue = part.getAttr(config.attrName);
            double normalizedValue;

            switch (config.method) {
                case LINEAR:
                    normalizedValue = linearNormalize(rawValue, range);
                    break;
                case LOG:
                    normalizedValue = logNormalize(rawValue, range);
                    break;
                case LOG10:
                    normalizedValue = log10Normalize(rawValue, range);
                    break;
                case POWER:
                    normalizedValue = powerNormalize(rawValue, range, config.power);
                    break;
                case QUANTILE:
                    normalizedValue = quantileNormalize(rawValue, range, config.numBins);
                    break;
                case RANK:
                    normalizedValue = rankNormalize(rawValue, range);
                    break;
                case PIECEWISE:
                    normalizedValue = piecewiseNormalize(rawValue, range);
                    break;
                case Z_SCORE:
                    normalizedValue = zScoreNormalize(rawValue, range);
                    break;
                default:
                    normalizedValue = linearNormalize(rawValue, range);
            }

            // 确保在[0, 100]范围内
            normalizedValue = Math.max(0, Math.min(100, normalizedValue));

            // 存储结果
            result.addNormalizedValue(part.code, normalizedValue);

            // 同时更新Part对象的归一化属性
            part.setNormalizedAttr(config.attrName, normalizedValue);
        }

        return result;
    }

    // 线性归一化
    private double linearNormalize(double value, NormalizationRange range) {
        if (range.getRange() == 0) return 50.0;
        return ((value - range.getMinValue()) / range.getRange()) * 100;
    }

    // 对数归一化
    private double logNormalize(double value, NormalizationRange range) {
        if (value <= 0) value = 0.1; // 避免log(0)
        double logValue = Math.log(value + 1);
        double logMin = Math.log(range.getMinValue() + 1);
        double logMax = Math.log(range.getMaxValue() + 1);

        if (logMax - logMin == 0) return 50.0;
        return ((logValue - logMin) / (logMax - logMin)) * 100;
    }

    // 以10为底的对数归一化
    private double log10Normalize(double value, NormalizationRange range) {
        if (value <= 0) value = 0.1;
        double log10Value = Math.log10(value + 1);
        double log10Min = Math.log10(range.getMinValue() + 1);
        double log10Max = Math.log10(range.getMaxValue() + 1);

        if (log10Max - log10Min == 0) return 50.0;
        return ((log10Value - log10Min) / (log10Max - log10Min)) * 100;
    }

    // 幂律归一化
    private double powerNormalize(double value, NormalizationRange range, double power) {
        double poweredValue = Math.pow(value, power);
        double poweredMin = Math.pow(range.getMinValue(), power);
        double poweredMax = Math.pow(range.getMaxValue(), power);

        if (poweredMax - poweredMin == 0) return 50.0;
        return ((poweredValue - poweredMin) / (poweredMax - poweredMin)) * 100;
    }

    // 分位数归一化
    private double quantileNormalize(double value, NormalizationRange range, int numBins) {
        List<Double> sortedValues = range.getSortedValues();
        if (sortedValues == null || sortedValues.isEmpty()) return 50.0;

        // 找到value在排序列表中的位置
        int index = Collections.binarySearch(sortedValues, value);
        if (index < 0) {
            index = -(index + 1); // 插入位置
        }

        // 计算百分位数
        double percentile = (double) index / sortedValues.size() * 100;
        return percentile;
    }

    // 排名归一化
    private double rankNormalize(double value, NormalizationRange range) {
        List<Double> sortedValues = range.getSortedValues();
        if (sortedValues == null || sortedValues.isEmpty()) return 50.0;

        // 计算排名（从1开始）
        int rank = 1;
        for (Double val : sortedValues) {
            if (val < value) rank++;
        }

        // 归一化到[0, 100]
        double normalizedRank = ((double) rank / sortedValues.size()) * 100;
        return normalizedRank;
    }

    // 分段线性归一化（简化版）
    private double piecewiseNormalize(double value, NormalizationRange range) {
        // 简单实现：三等分
        double segment = range.getRange() / 3;

        if (value <= range.getMinValue() + segment) {
            return 33.3 * (value - range.getMinValue()) / segment;
        } else if (value <= range.getMinValue() + 2 * segment) {
            return 33.3 + 33.3 * (value - range.getMinValue() - segment) / segment;
        } else {
            return 66.6 + 33.3 * (value - range.getMinValue() - 2 * segment) / segment;
        }
    }

    // Z-score归一化
    private double zScoreNormalize(double value, NormalizationRange range) {
        double zScore = (value - range.getMeanValue()) / range.getStdValue();
        // 将Z-score映射到[0, 100]，假设大部分数据在[-3, 3]之间
        double normalized = 50 + (zScore * 50 / 3);
        return Math.max(0, Math.min(100, normalized));
    }

    // 批量归一化多个属性
    public Map<String, NormalizationResult> normalizeBatch(List<Part> parts,
                                                          List<NormalizationConfig> configs) {
        Map<String, NormalizationResult> results = new HashMap<>();

        for (NormalizationConfig config : configs) {
            NormalizationResult result = normalize(parts, config);
            results.put(config.getAttrName(), result);
        }

        return results;
    }
}
