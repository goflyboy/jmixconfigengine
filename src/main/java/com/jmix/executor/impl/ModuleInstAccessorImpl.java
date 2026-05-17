package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.cmodel.ModuleBaseInst;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * ModuleInstAccessor实现类
 * 基于Module模型和ModuleInst实例提供部件属性读取和参数写入能力
 *
 * @since 2026-05-03
 */
@Slf4j
public class ModuleInstAccessorImpl implements ModuleInstAccessor {

    private final Module module;
    private final ModuleInst moduleInst;

    public ModuleInstAccessorImpl(Module module, ModuleInst moduleInst) {
        this.module = module;
        this.moduleInst = moduleInst;
    }

    // ==================== basic metadata ====================

    @Override
    public Long getModuleId() {
        return moduleInst.getId();
    }

    @Override
    public String getModuleCode() {
        return moduleInst.getCode();
    }

    @Override
    public String getInstanceConfigId() {
        return moduleInst.getInstanceConfigId();
    }

    @Override
    public int getModuleQuantity() {
        return moduleInst.getQuantity() == null ? 0 : moduleInst.getQuantity();
    }

    // ==================== ext/dyn attrs ====================

    @Override
    public String getExtAttr(String extAttrKey) {
        return module.getExtAttr(extAttrKey);
    }

    @Override
    public String getExtAttr(String partCategoryCode, int instId, String extAttrKey) {
        PartCategory category = module.getPartCategory(partCategoryCode);
        if (category == null) {
            throw new AlgLoaderException("PartCategory not found in module: " + partCategoryCode);
        }
        return category.getExtAttr(extAttrKey);
    }

    @Override
    public String getExtAttr(String partCategoryCode, int instId, String partCode, String extAttrKey) {
        return findPartModel(partCode).getExtAttr(extAttrKey);
    }

    @Override
    public String getInstDynAttr(String dynAttrKey) {
        return getMapValue(moduleInst.getExtAttrs(), dynAttrKey);
    }

    @Override
    public void setInstDynAttr(String dynAttrKey, String dynAttrValue) {
        moduleInst.getExtAttrs().put(dynAttrKey, dynAttrValue);
    }

    @Override
    public String getInstDynAttr(String partCategoryCode, int instId, String dynAttrKey) {
        return getMapValue(findPartCategoryInst(partCategoryCode, instId).getExtAttrs(), dynAttrKey);
    }

    @Override
    public void setInstDynAttr(String partCategoryCode, int instId, String dynAttrKey, String dynAttrValue) {
        findPartCategoryInst(partCategoryCode, instId).getExtAttrs().put(dynAttrKey, dynAttrValue);
    }

    @Override
    public String getInstDynAttr(String partCategoryCode, int instId, String partCode, String dynAttrKey) {
        return getMapValue(findPartInst(partCategoryCode, instId, partCode).getExtAttrs(), dynAttrKey);
    }

    @Override
    public void setInstDynAttr(String partCategoryCode, int instId, String partCode, String dynAttrKey,
            String dynAttrValue) {
        PartInst partInst = findPartInst(partCategoryCode, instId, partCode);
        if (partInst.getExtAttrs() == null) {
            partInst.setExtAttrs(new java.util.HashMap<>());
        }
        partInst.getExtAttrs().put(dynAttrKey, dynAttrValue);
    }

    // ==================== setParaValue ====================

    @Override
    public void setParaValue(String paraCode, String value) {
        ParaInst paraInst = findModuleParaInst(paraCode);
        paraInst.setValue(value);
    }

    @Override
    public void setParaValue(String partCategoryCode, String paraCode, String value) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID);
        setParaValue(pcInst, paraCode, value);
    }

    @Override
    public void setParaValue(String partCategoryCode, int instId, String paraCode, String value) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        setParaValue(pcInst, paraCode, value);
    }

    private void setParaValue(ModuleBaseInst baseInst, String paraCode, String value) {
        for (ParaInst pi : baseInst.getParas()) {
            if (paraCode.equals(pi.getCode())) {
                pi.setValue(value);
                return;
            }
        }
        throw new AlgLoaderException(
                "ParaInst not found for code: " + paraCode + " in " + baseInst.getCode());
    }

    // ==================== getParaValue ====================

    @Override
    public String getParaValue(String paraCode) {
        ParaInst pi = findModuleParaInst(paraCode);
        return pi.getValue();
    }

    @Override
    public String getParaValue(String partCategoryCode, String paraCode) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID);
        return getParaValue(pcInst, paraCode);
    }

    @Override
    public String getParaValue(String partCategoryCode, int instId, String paraCode) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        return getParaValue(pcInst, paraCode);
    }

    private String getParaValue(ModuleBaseInst baseInst, String paraCode) {
        for (ParaInst pi : baseInst.getParas()) {
            if (paraCode.equals(pi.getCode())) {
                return pi.getValue();
            }
        }
        throw new AlgLoaderException(
                "ParaInst not found for code: " + paraCode + " in " + baseInst.getCode());
    }

    // ==================== getDynAttr ====================

    @Override
    public String getDynAttr(String partCategoryCode, String attrCode) {
        return getDynAttr(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID, attrCode);
    }

    @Override
    public String getDynAttr(String partCategoryCode, int instId, String attrCode) {
        List<PartInst> effectiveParts = getEffectiveParts(partCategoryCode, instId);
        if (effectiveParts.isEmpty()) {
            return null;
        }
        PartInst firstPart = effectiveParts.get(0);
        String attrValue = getPartAttrValue(firstPart.getCode(), attrCode);
        if (attrValue == null) {
            throw new AlgLoaderException(
                    "Attribute '" + attrCode + "' not found on part '" + firstPart.getCode() + "'");
        }
        return attrValue;
    }

    // ==================== getDynAttrValues ====================

    @Override
    public List<String> getDynAttrValues(String partCategoryCode, String attrCode) {
        return getDynAttrValues(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID, attrCode);
    }

    @Override
    public List<String> getDynAttrValues(String partCategoryCode, int instId, String attrCode) {
        List<PartInst> effectiveParts = getEffectiveParts(partCategoryCode, instId);
        List<String> values = new ArrayList<>();
        for (PartInst partInst : effectiveParts) {
            String attrValue = getPartAttrValue(partInst.getCode(), attrCode);
            if (attrValue == null) {
                throw new AlgLoaderException(
                        "Attribute '" + attrCode + "' not found on part '" + partInst.getCode() + "'");
            }
            values.add(attrValue);
        }
        return values;
    }

    // ==================== getSumDynAttr ====================

    @Override
    public String getSumDynAttr(String partCategoryCode, String attrCode) {
        return getSumDynAttr(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID, attrCode);
    }

    @Override
    public String getSumDynAttr(String partCategoryCode, int instId, String attrCode) {
        List<PartInst> effectiveParts = getEffectiveParts(partCategoryCode, instId);
        int sum = 0;
        for (PartInst partInst : effectiveParts) {
            String attrValue = getPartAttrValue(partInst.getCode(), attrCode);
            if (attrValue == null) {
                throw new AlgLoaderException(
                        "Attribute '" + attrCode + "' not found on part '" + partInst.getCode() + "'");
            }
            int intValue;
            try {
                intValue = Integer.parseInt(attrValue);
            } catch (NumberFormatException e) {
                throw new AlgLoaderException(
                        "Attribute '" + attrCode + "' value '" + attrValue
                                + "' on part '" + partInst.getCode() + "' is not a valid integer");
            }
            sum += intValue * partInst.getQuantity();
        }
        return String.valueOf(sum);
    }

    // ==================== getQuantity ====================

    @Override
    public int getQuantity(String partCategoryCode) {
        return getQuantity(partCategoryCode, ModuleInst.DEFAULT_INSTANCE_ID);
    }

    @Override
    public int getQuantity(String partCategoryCode, int instId) {
        List<PartInst> effectiveParts = getEffectiveParts(partCategoryCode, instId);
        int totalQty = 0;
        for (PartInst partInst : effectiveParts) {
            if (partInst.getQuantity() != null) {
                totalQty += partInst.getQuantity();
            }
        }
        return totalQty;
    }

    @Override
    public void setQuantity(String partCategoryCode, int instId, int quantity) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        pcInst.getExtAttrs().put("quantity", quantity);
    }

    @Override
    public int getPartQuantity(String partCategoryCode, int instId, String partCode) {
        PartInst partInst = findPartInst(partCategoryCode, instId, partCode);
        return partInst.getQuantity() == null ? 0 : partInst.getQuantity();
    }

    @Override
    public void setPartQuantity(String partCategoryCode, int instId, String partCode, int quantity) {
        PartInst partInst = findPartInst(partCategoryCode, instId, partCode);
        partInst.setQuantity(quantity);
        partInst.setSelected(quantity > 0);
    }

    @Override
    public boolean isPartSelected(String partCategoryCode, int instId, String partCode) {
        return findPartInst(partCategoryCode, instId, partCode).isSelected();
    }

    @Override
    public List<String> getPartCodes(String partCategoryCode, int instId) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        List<String> codes = new ArrayList<>();
        for (PartInst partInst : pcInst.getParts()) {
            codes.add(partInst.getCode());
        }
        return codes;
    }

    // ==================== getInstanceIds ====================

    @Override
    public List<Integer> getInstanceIds(String partCategoryCode) {
        List<Integer> ids = new ArrayList<>();
        for (PartCategoryInst pcInst : moduleInst.getPartCategorys()) {
            if (partCategoryCode.equals(pcInst.getCode())) {
                ids.add(pcInst.getInstanceId());
            }
        }
        if (ids.isEmpty()) {
            throw new AlgLoaderException(
                    "PartCategory not found in ModuleInst: " + partCategoryCode);
        }
        return ids;
    }

    // ==================== internal helpers ====================

    /**
     * 获取有效部件列表（isSelected && quantity > 0）
     */
    private List<PartInst> getEffectiveParts(String partCategoryCode, int instId) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        List<PartInst> result = new ArrayList<>();
        for (PartInst partInst : pcInst.getParts()) {
            if (partInst.isSelected() && partInst.getQuantity() != null && partInst.getQuantity() > 0) {
                result.add(partInst);
            } else if (!partInst.isSelected() && partInst.getQuantity() != null && partInst.getQuantity() > 0) {
                log.warn("Part {} is not selected but has quantity > 0, including in effective parts",
                        partInst.getCode());
                result.add(partInst);
            }
        }
        return result;
    }

    /**
     * 根据部件代码从Module模型中获取属性值
     */
    private String getPartAttrValue(String partCode, String attrCode) {
        Part part = findPartModel(partCode);
        return part.getDynAttr().get(attrCode);
    }

    private Part findPartModel(String partCode) {
        List<Part> allAtomicParts = module.getAllAtomicParts();
        for (Part part : allAtomicParts) {
            if (partCode.equals(part.getCode())) {
                return part;
            }
        }
        throw new AlgLoaderException("Part not found in module: " + partCode);
    }

    private PartInst findPartInst(String partCategoryCode, int instId, String partCode) {
        PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
        PartInst partInst = pcInst.queryPart(partCode);
        if (partInst == null) {
            throw new AlgLoaderException(
                    "PartInst not found: category=" + partCategoryCode + ", instId=" + instId
                            + ", partCode=" + partCode);
        }
        return partInst;
    }

    private String getMapValue(java.util.Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private PartCategoryInst findPartCategoryInst(String partCategoryCode, int instId) {
        for (PartCategoryInst pcInst : moduleInst.getAllPartCategorys()) {
            if (partCategoryCode.equals(pcInst.getCode()) && pcInst.getInstanceId() == instId) {
                return pcInst;
            }
        }
        throw new AlgLoaderException(
                "PartCategoryInst not found: code=" + partCategoryCode + ", instId=" + instId);
    }

    private ParaInst findModuleParaInst(String paraCode) {
        for (ParaInst pi : moduleInst.getParas()) {
            if (paraCode.equals(pi.getCode())) {
                return pi;
            }
        }
        throw new AlgLoaderException("ParaInst not found in module: " + paraCode);
    }
}
