package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.para.Para;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        initShortCode(1);
    }

    @JsonIgnore
    public int initShortCode(int startIndex) {
        int index = super.initShortCode(startIndex);
        for (Part part : atomicParts) {
            if (part.getCode().length() <= 4) { // 如果编码长度小于等于3，则直接使用编码
                part.setShortCode(part.getCode());
            } else {
                part.setShortCode(Part.SHORT_CODE_PREFIX + index);
                index++;
            }
        }
        for (PartCategory partCategory : partCategorys) {
            index = partCategory.initShortCode(index);
        }
        return index;
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
     * 获取所有规则列表（包括子分类中的规则）
     * 递归收集所有子分类中的规则
     * 
     * @return 所有规则列表
     */
    @JsonIgnore
    public List<Rule> getAllRules() {
        List<Rule> allRules = new ArrayList<>();
        // 添加当前模块的规则
        List<Rule> rules = getRules();
        if (rules != null) {
            allRules.addAll(rules);
        }
        // 递归收集所有子分类中的规则
        for (PartCategory partCategory : partCategorys) {
            allRules.addAll(partCategory.getAllRules());
        }
        return allRules;
    }

    /**
     * 获取指定计算阶段的所有规则列表（包括子分类中的规则）
     * 递归收集所有子分类中的规则
     * 
     * @param calcStage 计算阶段
     * @return 指定计算阶段的所有规则列表
     */
    @JsonIgnore
    public List<Rule> getAllRules(CalcStage calcStage) {
        if (calcStage == null) {
            return new ArrayList<>();
        }
        List<Rule> allRules = new ArrayList<>();
        // 添加当前模块的规则
        List<Rule> rules = getRules(calcStage);
        if (rules != null) {
            allRules.addAll(rules);
        }
        // 递归收集所有子分类中的规则
        for (PartCategory partCategory : partCategorys) {
            allRules.addAll(partCategory.getAllRules(calcStage));
        }
        return allRules;
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
     * 根据部件分类编码获取原子部件列表
     *
     * @param partCategoryCode 部件分类编码
     * @return 指定分类下的原子部件列表
     */
    @JsonIgnore
    public List<Part> getAllAtomicParts(String partCategoryCode) {
        List<Part> result = new ArrayList<>();
        PartCategory category = findPartCategory(partCategoryCode);
        if (category != null) {
            result.addAll(category.getAllAtomicParts());
        }
        return result;
    }

    @Override
    public Optional<Para> getPara(String code) {
        Optional<Para> result = super.getPara(code);
        if (result.isPresent()) {
            return result;
        }
        for (PartCategory partCategory : partCategorys) {
            result = partCategory.getPara(code);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Rule> getRule(String code) {
        Optional<Rule> result = super.getRule(code);
        if (result.isPresent()) {
            return result;
        }
        for (PartCategory partCategory : partCategorys) {
            result = partCategory.getRule(code);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * 获取所有参数列表
     * 递归收集所有子分类中的参数
     *
     * @return 所有参数列表
     */
    @JsonIgnore
    public List<Para> getAllParas() {
        List<Para> allParas = new ArrayList<>();
        List<Para> paras = getParas();
        if (paras != null) {
            allParas.addAll(paras);
        }
        for (PartCategory partCategory : partCategorys) {
            allParas.addAll(partCategory.getAllParas());
        }
        return allParas;
    }

    /**
     * 查询所有优先级规则
     *
     * @return 优先级规则列表
     */
    @JsonIgnore
    public List<Rule> queryPriorityRules() {
        List<Rule> priorityRules = new ArrayList<>();
        List<Rule> rules = getRules();
        if (rules == null || rules.isEmpty()) {
            return priorityRules;
        }
        for (Rule rule : rules) {
            if (RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName())) {
                priorityRules.add(rule);
            }
        }
        return priorityRules;
    }

    /**
     * 判断是否包含所有优先级规则
     *
     * @return 如果包含所有优先级规则返回true，否则返回false
     */
    @JsonIgnore
    public boolean hasAllPriorityRule() {
        if (hasPriorityRule()) {
            return true;
        }
        for (PartCategory partCategory : partCategorys) {
            if (partCategory.hasAllPriorityRule()) {
                return true;
            }
        }
        return false;
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

    /**
     * 克隆ModuleBase对象
     *
     * @param to 目标对象
     * @return 克隆的ModuleBase对象
     */
    public void clone(ModuleBase to) {
        super.clone(to);
        // 过滤后的atomicParts和partCategorys都可能被过滤掉，不复制
    }

    /**
     * 添加部件分类
     * 
     * @param partCategory 部件分类
     */
    @JsonIgnore
    public void addPartCategory(PartCategory partCategory) {
        partCategorys.add(partCategory);
    }
}
