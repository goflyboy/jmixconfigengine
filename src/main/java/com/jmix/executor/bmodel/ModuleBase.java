package com.jmix.executor.bmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块基类
 * 包含Part和PartCategory相关的公共字段和方法
 * PartCategory和Module的公共基类
 * 
 * @since 2025-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ModuleBase extends Onto {

    /**
     * 原子部件列表
     */
    protected List<Part> atomicParts = new ArrayList<>();

    /**
     * 部件分类列表
     */
    protected List<PartCategory> partCategorys = new ArrayList<>();

    /**
     * 部件映射表
     */
    @JsonIgnore
    protected Map<String, Part> atomicPartMap = new HashMap<>();

    /**
     * 初始化短代码
     */
    @JsonIgnore
    public void initShortCode() {
        super.initShortCode();
        int index = 0;
        index = 0;
        for (Part part : atomicParts) {
            if (part.getCode().length() <= 3) { // 如果编码长度小于等于3，则直接使用编码
                part.setShortCode(part.getCode());
            } else {
                part.setShortCode(Part.SHORT_CODE_PREFIX + index);
                index++;
            }
        }
        for (PartCategory partCategory : partCategorys) {
            partCategory.initShortCode();
        }
    }

    /**
     * 从模块中查找部件分类
     *
     * @param categoryCode 分类代码
     * @return 找到的PartCategory，如果未找到则返回null
     */
    @JsonIgnore
    public PartCategory findPartCategory(String categoryCode) {
        PartCategory result = null;
        // 根据partCategorys初始化partCategoryMap
        for (PartCategory pc : this.partCategorys) {
            result = findPartCategory(pc, categoryCode);
            if (null != result) {
                return result;
            }
        }
        return result;
    }

    private PartCategory findPartCategory(PartCategory category, String categoryCode) {
        if (category.getCode().equals(categoryCode)) {
            return category;
        }
        PartCategory result = null;
        // 根据partCategorys初始化partCategoryMap
        for (PartCategory pc : category.getPartCategorys()) {
            result = findPartCategory(pc, categoryCode);
            if (null != result) {
                return result;
            }
        }
        return result;
    }

    /**
     * 从模块中查找部件
     * 
     * @param partCode 部件代码
     * @return 找到的Part，如果未找到则返回null
     * @return
     */
    @JsonIgnore
    public Part findAtomicPart(String partCode) {
        if (atomicPartMap == null || atomicPartMap.isEmpty()) {
            // 根据parts初始化partMap
            for (Part part : atomicParts) {
                atomicPartMap.put(part.getCode(), part);
            }
        }
        return atomicPartMap.get(partCode);
    }

    /**
     * 获取所有部件
     * 
     * @return
     */
    @JsonIgnore
    public List<IPart> getAllParts() {
        List<IPart> allParts = new ArrayList<>();
        allParts.addAll(atomicParts);
        for (PartCategory partCategory : partCategorys) {
            allParts.add(partCategory);
            allParts.addAll(partCategory.getAllParts());
        }
        return allParts;
    }

    /**
     * 获取所有的部件
     * 
     * @return
     */
    @JsonIgnore
    public List<PartCategory> getAllPartCategorys() {
        List<PartCategory> allPartCategories = new ArrayList<>();
        // 根据partCategorys初始化partCategoryMap
        for (PartCategory pc : this.partCategorys) {
            getAllPartCategorys(pc, allPartCategories);
        }
        return allPartCategories;
    }

    private void getAllPartCategorys(PartCategory category, List<PartCategory> allPartCategories) {
        allPartCategories.add(category);
        for (PartCategory pc : category.getPartCategorys()) {
            getAllPartCategorys(pc, allPartCategories);
        }
    }

    /**
     * 获取部件
     * 
     * @return
     */
    @JsonIgnore
    public IPart getPart(String code) {
        return getAllParts().stream().filter(part -> part.getCode().equals(code)).findFirst().orElse(null);
    }

    /**
     * 获取所有部件
     * 
     * @return
     */
    @JsonIgnore
    public List<Part> getAllAtomicParts() {
        List<Part> allAtomicParts = new ArrayList<>();
        allAtomicParts.addAll(atomicParts);
        for (PartCategory partCategory : partCategorys) {
            allAtomicParts.addAll(partCategory.getAtomicParts());
        }
        return allAtomicParts;
    }

    /**
     * 添加部件
     * 
     * @param part
     */
    @JsonIgnore
    public void addPart(IPart part) {
        if (part instanceof Part) {
            if (findAtomicPart(part.getCode()) != null) {
                log.warn("part {}, has existed, will be ommited!", part.getCode());
            } else {
                atomicParts.add((Part) part);
                atomicPartMap.put(part.getCode(), (Part) part);
            }

        } else if (part instanceof PartCategory) {
            if (findPartCategory(part.getCode()) != null) {
                log.warn("findPartCategory {}, has existed, will be ommited!", part.getCode());
            } else {
                PartCategory father = findPartCategory((part.getFatherCode()));
                if (null != father && !(father == this)) {
                    father.addPart(part);
                }
                partCategorys.add((PartCategory) part);
            }
        }
    }

    /**
     * 添加部件,不带结构的
     * 
     * @param parts 部件列表
     */
    @JsonIgnore
    public void addAtomicPartsWithoutStructure(List<? extends IPart> parts) {
        for (IPart part : parts) {
            if (part instanceof Part) {
                if (!atomicPartMap.containsKey(part.getCode())) {
                    atomicParts.add((Part) part);
                    atomicPartMap.put(part.getCode(), (Part) part);
                }
            } else {
                log.error("Unsupported part type: {}, only Part instances are supported",
                        part.getClass().getSimpleName());
                throw new IllegalArgumentException("Unsupported part type: " + part.getClass().getSimpleName()
                        + ", only Part instances are supported");
            }
        }
    }

    /**
     * 添加部件
     * 
     * @param parts 部件列表
     */
    @JsonIgnore
    public void addParts(List<? extends IPart> parts) {
        // 先根据fatherCode/code构建好层次广西
        Map<String, IPart> unAddedParts = new HashMap<>();
        Map<String, IPart> allPartMap = new HashMap<>();
        for (IPart part : parts) {
            allPartMap.put(part.getCode(), part);
        }

        for (IPart part : parts) {
            // 没有父节点的part，直接添加到当前列表中
            if (part.getFatherCode() == null || part.getFatherCode().isEmpty()) {
                addPart(part);
                continue;
            }
            // 有父节点，分两种场景
            // A.不在带添加的列表中，已存在当前产品中
            IPart fatherPart = findPartCategory((part.getFatherCode()));
            if (null != fatherPart) {
                ((PartCategory) fatherPart).addPart(part);
            } else {
                fatherPart = allPartMap.get(part.getFatherCode());
                if (null != fatherPart) {
                    ((PartCategory) fatherPart).addPart(part);
                    unAddedParts.put(fatherPart.getCode(), fatherPart);
                } else {
                    log.error("Father part not found for part: {}, fatherCode: {}", part.getCode(),
                            part.getFatherCode());
                    throw new IllegalArgumentException("Father part not found for part: " + part.getCode()
                            + ", fatherCode: " + part.getFatherCode());
                }
            }

        }
        for (IPart part : unAddedParts.values()) {
            addPart(part);
        }
    }

    /**
     * 根据父部件编码获取子部件列表
     * 
     * @param fatherCode 父部件编码
     * @return 子部件列表
     */
    @JsonIgnore
    public List<IPart> getChildrenPart(String fatherCode) {
        return getAllParts().stream()
                .filter(part -> fatherCode.equals(part.getFatherCode()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取所有顶级部件（没有父部件的部件）
     * 
     * @return 顶级部件列表
     */
    @JsonIgnore
    public List<IPart> getTopLevelParts() {
        return getAllParts().stream()
                .filter(part -> part.getFatherCode() == null || part.getFatherCode().isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
}
