package com.jmix.executor.impl;

import java.util.List;

/**
 * 模块实例访问器接口
 * 提供对ModuleInst中部件、属性、参数的读取和写入能力
 *
 * @since 2026-05-03
 */
public interface ModuleInstAccessor {

    void setParaValue(String paraCode, String value);

    void setParaValue(String partCategoryCode, String paraCode, String value);

    void setParaValue(String partCategoryCode, int instId, String paraCode, String value);

    String getParaValue(String paraCode);

    String getParaValue(String partCategoryCode, String paraCode);

    String getParaValue(String partCategoryCode, int instId, String paraCode);

    String getDynAttr(String partCategoryCode, String attrCode);

    String getDynAttr(String partCategoryCode, int instId, String attrCode);

    List<String> getDynAttrValues(String partCategoryCode, String attrCode);

    List<String> getDynAttrValues(String partCategoryCode, int instId, String attrCode);

    String getSumDynAttr(String partCategoryCode, String attrCode);

    String getSumDynAttr(String partCategoryCode, int instId, String attrCode);

    int getQuantity(String partCategoryCode);

    int getQuantity(String partCategoryCode, int instId);

    List<Integer> getInstanceIds(String partCategoryCode);
}
