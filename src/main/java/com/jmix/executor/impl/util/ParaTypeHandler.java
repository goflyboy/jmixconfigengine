package com.jmix.executor.impl.util;

import java.util.Arrays;

import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaOption;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.omodel.AlgLoaderException;

/**
 * Para类型处理工具类
 * 用于处理Para类型相关的逻辑，避免代码重复
 */
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
            throw new AlgLoaderException("Para object cannot be null");
        }

        switch (para.getType()) {
            case INTEGER:
                return value;
            case ENUM:
                ParaOption option = para.getOption(value);
                if (option == null) {
                    throw new AlgLoaderException(String.format(
                            "Option not found in parameter %s: %s, available options: %s",
                            para.getCode(), value, Arrays.toString(para.getOptionCodes())));
                }
                return String.valueOf(option.getCodeId());
            default:
                throw new AlgLoaderException(String.format(
                        "Parameter %s type not supported: %s", para.getCode(), para.getType()));
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
            throw new AlgLoaderException("Para object cannot be null");
        }

        switch (para.getType()) {
            case INTEGER:
                return codeIdValue;
            case ENUM:
                // 根据codeId查找对应的选项
                for (ParaOption option : para.getOptions()) {
                    if (String.valueOf(option.getCodeId()).equals(codeIdValue)) {
                        return option.getCode();
                    }
                }
                throw new AlgLoaderException(String.format(
                        "Option with codeId %s not found in parameter %s", para.getCode(), codeIdValue));
            default:
                throw new AlgLoaderException(String.format(
                        "Parameter %s type not supported: %s", para.getCode(), para.getType()));
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
            throw new AlgLoaderException("Para object cannot be null");
        }

        if (para.getType() != ParaType.INTEGER && para.getType() != ParaType.ENUM) {
            throw new AlgLoaderException(String.format(
                    "Parameter %s type not supported: %s", para.getCode(), para.getType()));
        }
    }

    /**
     * 检查ENUM类型的Para是否包含指定选项
     * 
     * @param para       Para对象
     * @param optionCode 选项代码
     * @return 是否包含该选项
     */
    public static boolean hasOption(Para para, String optionCode) {
        if (para == null || para.getType() != ParaType.ENUM) {
            return false;
        }
        return para.getOption(optionCode) != null;
    }
}