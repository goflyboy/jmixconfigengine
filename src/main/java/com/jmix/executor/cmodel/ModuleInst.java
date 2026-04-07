package com.jmix.executor.cmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 模块实例类
 * 表示模块的一个具体实例，包含参数实例和部件实例
 *
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleInst extends ModuleBaseInst {

    /**
     * 默认实例配置ID常量
     */
    public static final String DEFAULT_INSTANCE_CONFIG_ID = "0";

    /**
     * 默认数量常量
     */
    public static final int DEFAULT_QUANTITY = 1;

    /**
     * 模块ID
     */
    private Long id;

    /**
     * 实例配置ID
     */
    private String instanceConfigId = DEFAULT_INSTANCE_CONFIG_ID;

    /**
     * 数量
     */
    private Integer quantity = DEFAULT_QUANTITY;

    /**
     * 部件分类实例列表
     */
    private List<PartCategoryInst> partCategorys = new ArrayList<>();

    /**
     * 优先级属性值列表
     * 每个约束的值
     */
    private List<PriorityAttrValue> priorityAttrValues = new ArrayList<>();

    /**
     * 优先级排序号
     */
    private int prioritySortNo = 0;

    /**
     * 优先级综合值
     * 最后综合的值
     */
    private double priorityOverallValue = 0.0;

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
        for (PartCategoryInst partCategoryInst : partCategorys) {
            allParts.addAll(partCategoryInst.getAllParts());
        }
        return allParts;
    }

    /**
     * 获取所有部件分类实例
     *
     * @return 所有部件分类实例列表
     */
    @JsonIgnore
    public List<PartCategoryInst> getAllPartCategorys() {
        List<PartCategoryInst> allPartCategorys = new ArrayList<>();
        for (PartCategoryInst partCategoryInst : partCategorys) {
            allPartCategorys.add(partCategoryInst);
            allPartCategorys.addAll(partCategoryInst.getAllPartCategorys());
        }
        return allPartCategorys;
    }

    /**
     * 添加部件分类实例
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
        for (PartCategoryInst partCategoryInst : partCategorys) {
            partInst = partCategoryInst.queryPart(partCode);
            if (partInst != null) {
                return partInst;
            }
        }
        return null;
    }

    /**
     * 添加优先级属性值信息到字符串构建器
     * 格式：PAs(CA:100,TA:200,....)，attrCode取前两位的大写字符
     *
     * @param sb 字符串构建器
     */
    public void appendPriorityAttrValuesInfo(StringBuilder sb) {
        sb.append(" PO:").append(priorityOverallValue);
        sb.append(" PS:").append(prioritySortNo);
        if (priorityAttrValues == null || priorityAttrValues.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append("PAs(");
        boolean first = true;
        for (PriorityAttrValue priorityAttrValue : priorityAttrValues) {
            if (!first) {
                sb.append(",");
            }
            String shortCode = toAttrShortCode(priorityAttrValue.getAttrCode());
            sb.append(shortCode).append(":").append(priorityAttrValue.getOptimalValue());
            first = false;
        }
        sb.append(")");
    }

    /**
     * 获取优先级属性短编码字符串
     * 输出 attrCode 和 shortCode 的关系
     *
     * @return 优先级属性短编码字符串，格式：shortCode:attrCode，多个用逗号分隔
     */
    @JsonIgnore
    public String getPriorityAttrShortCodeStr() {
        if (priorityAttrValues == null || priorityAttrValues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PAs:priorityAttrValues ").append("PO:priorityOverallValue ")
                .append("PS:prioritySortNo");
        sb.append(" (");
        boolean first = true;
        for (PriorityAttrValue priorityAttrValue : priorityAttrValues) {
            String attrCode = priorityAttrValue.getAttrCode();
            if (attrCode == null || attrCode.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            String shortCode = toAttrShortCode(attrCode);
            sb.append(shortCode).append(":").append(attrCode);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 将属性代码转换为短编码
     * 取 attrCode 的前两位，转换为大写
     *
     * @param attrCode 属性代码
     * @return 短编码，如果 attrCode 为 null 或长度不足则返回空字符串
     */
    private String toAttrShortCode(String attrCode) {
        if (attrCode == null) {
            return "";
        }
        if (attrCode.length() >= 2) {
            return attrCode.substring(0, 2).toUpperCase();
        }
        return attrCode.toUpperCase();
    }

    /**
     * 生成短字符串表示（包含优先级信息）
     *
     * @param isSimple 是否使用简单格式
     * @return 短字符串
     */
    @Override
    public String toShortString(boolean isSimple) {
        StringBuilder sb = new StringBuilder();
        // 当前模块本身的
        sb.append(super.toShortString(isSimple));
        toShortString(sb, isSimple, this.getPartCategorys());
        // 添加优先级属性值信息
        appendPriorityAttrValuesInfo(sb);
        return sb.toString();
    }

    private void toShortString(StringBuilder sb, boolean isSimple, List<PartCategoryInst> pCategoryInsts) {
        if (hasMuitiPartCategoryInst(pCategoryInsts)) {
            // 分类的
            int index = 0;
            for (PartCategoryInst partCategory : this.partCategorys) {
                index++;
                String prefix = partCategory.getCode();
                prefix = "_I" + partCategory.getInstanceId() + "{";
                sb.append(prefix).append(partCategory.toShortString(isSimple)).append("}");
                if (!(index == this.partCategorys.size())) { // isLast
                    sb.append(",");
                }
            }
        } else {
            int index = 1;
            for (PartCategoryInst partCategory : this.partCategorys) {
                index++;
                sb.append(partCategory.toShortString(isSimple));
                if (!(index == this.partCategorys.size())) { // isLast
                    sb.append(",");
                }
            }
        }
    }

    private boolean hasMuitiPartCategoryInst(List<PartCategoryInst> pCategoryInsts) {
        Set<String> pcCodes = new HashSet<>();
        for (PartCategoryInst pCategoryInst : pCategoryInsts) {
            if (pcCodes.contains(pCategoryInst.getCode())) {
                return true;
            }
            pcCodes.add(pCategoryInst.getCode());
        }
        return false;
    }
}
