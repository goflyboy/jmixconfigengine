package com.jmix.configengine.util;

import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.ParaType;

import java.util.Arrays;

/**
 * Para类型处理工具类
 * 用于处理Para类型相关的逻辑，避免代码重复
 */
public class ParaTypeHandler {
    
    /**
     * 根据Para类型和值，获取对应的codeId值
     * 
     * @param para Para对象
     * @param value 输入值
     * @return 对应的codeId字符串
     * @throws RuntimeException 当参数不存在、选项不存在或类型不支持时
     */
    public static String getCodeIdValue(Para para, String value) {
        if (para == null) {
            throw new RuntimeException("Para对象不能为空");
        }
        
        switch (para.getType()) {
            case INTEGER:
                return value;
            case ENUM:
                ParaOption option = para.getOption(value);
                if (option == null) {
                    throw new RuntimeException(String.format(
                        "参数 %s 中未找到选项: %s，可用选项: %s", 
                        para.getCode(), value, Arrays.toString(para.getOptionCodes())));
                }
                return String.valueOf(option.getCodeId());
            default:
                throw new RuntimeException(String.format(
                    "参数 %s 类型不支持: %s", para.getCode(), para.getType()));
        }
    }
    
    /**
     * 根据Para类型和codeId值，获取对应的显示值
     * 
     * @param para Para对象
     * @param codeIdValue codeId字符串值
     * @return 对应的显示值
     * @throws RuntimeException 当参数不存在、选项不存在或类型不支持时
     */
    public static String getDisplayValue(Para para, String codeIdValue) {
        if (para == null) {
            throw new RuntimeException("Para对象不能为空");
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
                throw new RuntimeException(String.format(
                    "参数 %s 中未找到codeId为 %s 的选项", para.getCode(), codeIdValue));
            default:
                throw new RuntimeException(String.format(
                    "参数 %s 类型不支持: %s", para.getCode(), para.getType()));
        }
    }
    
    /**
     * 验证Para类型是否支持
     * 
     * @param para Para对象
     * @throws RuntimeException 当类型不支持时
     */
    public static void validateParaType(Para para) {
        if (para == null) {
            throw new RuntimeException("Para对象不能为空");
        }
        
        if (para.getType() != ParaType.INTEGER && para.getType() != ParaType.ENUM) {
            throw new RuntimeException(String.format(
                "参数 %s 类型不支持: %s", para.getCode(), para.getType()));
        }
    }
    
    /**
     * 检查ENUM类型的Para是否包含指定选项
     * 
     * @param para Para对象
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