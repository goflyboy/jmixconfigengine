package com.jmix.executor.impl;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class MultiInstPartCategoryInput implements IPartCategoryInput {
    private List<PartCategoryInput> partCategoryReqs = new ArrayList<>();

    public void addPartCategoryReq(PartCategoryInput partCategoryReq) {
        partCategoryReqs.add(partCategoryReq);
    }

    @Override
    public String getPartCategoryCode() {
        return partCategoryReqs.get(0).getPartCategoryCode();
    }
}
