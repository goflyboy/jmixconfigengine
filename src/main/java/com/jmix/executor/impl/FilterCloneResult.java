package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.cmodel.ErrorInfo;

import java.util.List;
import java.util.Map;

/**
 * filterClone 方法的返回结果
 *
 * @param filteredModule      过滤后的模块
 * @param partCategoryInputs  部件分类输入列表
 * @param errorInfoMap        错误信息映射，key 为 partCategoryCode
 * @since 2026-04-30
 */
public record FilterCloneResult(Module filteredModule,
                                List<PartCategoryInputBase> partCategoryInputs,
                                Map<String, ErrorInfo> errorInfoMap,
                                List<PartCategoryInputBase> optionalPartCategoryInputs) {

    public FilterCloneResult(Module filteredModule,
                             List<PartCategoryInputBase> partCategoryInputs,
                             Map<String, ErrorInfo> errorInfoMap) {
        this(filteredModule, partCategoryInputs, errorInfoMap, List.of());
    }

    public boolean hasError() {
        return errorInfoMap != null && !errorInfoMap.isEmpty();
    }
}
