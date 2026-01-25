package com.jmix.executor.impl.util;

import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求工具类
 * 提供请求对象相关的工具方法
 * 
 * @since 2025-01-XX
 */
public final class ReqUtils {
    /**
     * 私有构造器，防止工具类被实例化
     */
    private ReqUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 构建输入编程对象列表
     * 根据req.mainPartInst、preParaInsts、prePartInsts构建inputProgObjs（三个结合相加)
     * 
     * @param req 参数反推请求
     * @return 输入编程对象代码列表
     */
    public static List<String> buildInputProgObjs(InferParasReq req) {
        List<String> inputProgObjs = new ArrayList<>();

        // 根据req.mainPartInst、preParaInsts、prePartInsts构建inputProgObjs（三个结合相加)
        if (req.getMainPartInst() != null) {
            inputProgObjs.add(req.getMainPartInst().getCode());
        }
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                inputProgObjs.add(paraInst.getCode());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                inputProgObjs.add(partInst.getCode());
            }
        }

        return inputProgObjs;
    }
}
