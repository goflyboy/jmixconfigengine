package com.jmix.executor.cmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 部件分类实例类
 * 表示部件分类的一个具体实例，包含参数实例和部件实例
 *
 * @since 2025-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartCategoryInst extends ModuleBaseInst {

    /**
     * 子部件分类实例列表
     */
    private List<PartCategoryInst> partCategorys = new ArrayList<>();

    /**
     * 获取所有部件实例（当前层 + 所有子PartCategoryInst的部件）
     *
     * @return 所有部件实例列表
     */
    @JsonIgnore
    public List<PartInst> getAllParts() {
        List<PartInst> allParts = new ArrayList<>();
        // 添加当前层的部件
        allParts.addAll(getParts());
        // 递归添加所有子PartCategoryInst的部件
        for (PartCategoryInst childCategoryInst : partCategorys) {
            allParts.addAll(childCategoryInst.getAllParts());
        }
        return allParts;
    }

    /**
     * 获取所有部件分类实例
     *
     * @return 所有部件分类实例列表（递归获取）
     */
    @JsonIgnore
    public List<PartCategoryInst> getAllPartCategorys() {
        List<PartCategoryInst> allPartCategorys = new ArrayList<>();
        for (PartCategoryInst childCategoryInst : partCategorys) {
            allPartCategorys.add(childCategoryInst);
            allPartCategorys.addAll(childCategoryInst.getAllPartCategorys());
        }
        return allPartCategorys;
    }

    /**
     * 添加子部件分类实例
     *
     * @param partCategoryInst 部件分类实例
     */
    public void addPartCategoryInst(PartCategoryInst partCategoryInst) {
        partCategorys.add(partCategoryInst);
    }

    /**
     * 根据部件代码查询部件实例（从所有部件中查询）
     *
     * @param partCode 部件代码
     * @return 部件实例，如果不存在则返回null
     */
    @Override
    public PartInst queryPart(String partCode) {
        // 先在当前层查找
        PartInst partInst = super.queryPart(partCode);
        if (partInst != null) {
            return partInst;
        }
        // 在所有子PartCategoryInst中查找
        for (PartCategoryInst childCategoryInst : partCategorys) {
            partInst = childCategoryInst.queryPart(partCode);
            if (partInst != null) {
                return partInst;
            }
        }
        return null;
    }
}
