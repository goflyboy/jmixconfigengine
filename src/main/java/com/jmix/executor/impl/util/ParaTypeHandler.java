package com.jmix.executor.impl.util;

import com.jmix.executor.imodel.DynamicAttributerOption;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.omodel.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

/**
 * Para类型处理工具类
 * 用于处理Para类型相关的逻辑，避免代码重复
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ParaTypeHandler {

    /**
     * 根据Para类型和值，获取对应的codeId值
     *
     * @param para  Para对象
     * @param value 输入值
     * @return 对应的codeId字符串
     * @throws AlgLoaderException 当参数不存在、选项不存在或类型不支持时
     */
    public static String getCodeIdValue(Para para, String value) {
        if (para == null) {
            log.error("Para object cannot be null");
            throw new AlgLoaderException("Para object cannot be null");
        }

        switch (para.getParaType()) {
            case INTEGER:
                // INTEGER类型直接返回value作为codeId
                return value;
            case ENUM:
            case GROUP:
                // ENUM/GROUP类型需要查找选项
                DynamicAttributerOption option = para.getOption(value).orElse(null);
                if (option == null) {
                    log.error("Option not found in parameter {}: {}, available options: {}",
                            para.getCode(), value, Arrays.toString(para.getOptionCodes()));
                    throw new AlgLoaderException(String.format(
                            "Option not found in parameter %s: %s, available options: %s",
                            para.getCode(), value, Arrays.toString(para.getOptionCodes())));
                }
                return String.valueOf(option.getCodeId());
            default:
                log.error("Parameter {} type not supported: {}", para.getCode(), para.getParaType());
                throw new AlgLoaderException(String.format(
                        "Parameter %s type not supported: %s", para.getCode(), para.getParaType()));
        }
    }

    /**
     * 根据Para类型和codeId值，获取对应的显示值
     *
     * @param para        Para对象
     * @param codeIdValue codeId字符串值
     * @return 对应的显示值
     * @throws AlgLoaderException 当参数不存在、选项不存在或类型不支持时
     */
    public static String getDisplayValue(Para para, String codeIdValue) {
        if (para == null) {
            log.error("Para object cannot be null");
            throw new AlgLoaderException("Para object cannot be null");
        }

        switch (para.getParaType()) {
            case INTEGER:
                return codeIdValue;
            case ENUM:
                // 根据codeId查找对应的选项
                Optional<DynamicAttributerOption> option = para.getOption(Integer.parseInt(codeIdValue));
                if (option.isPresent()) {
                    return option.get().getCode();
                }
                log.error("Option with codeId {} not found in parameter {}", codeIdValue, para.getCode());
                throw new AlgLoaderException(String.format(
                        "Option with codeId %s not found in parameter %s", para.getCode(), codeIdValue));
            default:
                log.error("Parameter {} type not supported: {}", para.getCode(), para.getParaType());
                throw new AlgLoaderException(String.format(
                        "Parameter %s type not supported: %s", para.getCode(), para.getParaType()));
        }
    }

    /**
     * 验证Para类型是否支持
     *
     * @param para Para对象
     * @throws AlgLoaderException 当类型不支持时
     */
    public static void validateParaType(Para para) {
        if (para == null) {
            log.error("Para object cannot be null");
            throw new AlgLoaderException("Para object cannot be null");
        }

        // 支持所有ParaType类型
        ParaType paraType = para.getParaType();
        if (paraType != ParaType.INTEGER && paraType != ParaType.ENUM &&
                paraType != ParaType.GROUP) {
            log.error("Parameter {} type not supported: {}", para.getCode(), para.getParaType());
            throw new AlgLoaderException(String.format(
                    "Parameter %s type not supported: %s", para.getCode(), para.getParaType()));
        }
    }

    /**
     * 检查GROUP类型的Para是否包含指定选项
     *
     * @param para       Para对象
     * @param optionCode 选项代码
     * @return 是否包含该选项
     */
    public static boolean hasOption(Para para, String optionCode) {
        if (para == null || (para.getParaType() != ParaType.ENUM && para.getParaType() != ParaType.GROUP)) {
            return false;
        }
        return para.getOption(optionCode).isPresent();
    }
}