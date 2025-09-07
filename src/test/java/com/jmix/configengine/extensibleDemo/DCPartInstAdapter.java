package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import lombok.extern.slf4j.Slf4j;

/**
 * DC部件实例适配器
 * 负责PartInst到DCPartInst的字段映射和扩展属性处理
 */
@Slf4j
public class DCPartInstAdapter extends AdapterInfo {
    
    @Override
    protected void initMappings() {
        // 字段映射关系 - PartInst和DCPartInst字段基本相同
        fieldMappings.put("code", "code");
        fieldMappings.put("quantity", "quantity");
        fieldMappings.put("selectAttrValue", "selectAttrValue");
        fieldMappings.put("isHidden", "isHidden");
        
        // 扩展属性定义
        extAttrs.put("partNumber", "String");     // 部件编号属性
        extAttrs.put("deliveryType", "String");   // 交付类型属性
    }
    
    /**
     * 将PartInst适配为DCPartInst
     * @param partInst 原始部件实例
     * @return DC部件实例
     */
    public DCPartInst adapt(PartInst partInst) {
        if (partInst == null) {
            return null;
        }
        
        DCPartInst dcPartInst = new DCPartInst(partInst);
        
        // 设置扩展属性 - 默认值
        dcPartInst.setPartNumber(partInst.getCode()); // 使用code作为partNumber
        dcPartInst.setDeliveryType("Standard");       // 默认交付类型
        
        log.debug("Adapted PartInst to DCPartInst: {} with partNumber: {}, deliveryType: {}", 
                partInst.getCode(), dcPartInst.getPartNumber(), dcPartInst.getDeliveryType());
        return dcPartInst;
    }
    
    /**
     * 从扩展属性字符串解析属性
     * 格式: "attrName=partNumber,attrType=String"
     * @param extAttrStr 扩展属性字符串
     * @return 解析后的属性名
     */
    public String parseExtAttr(String extAttrStr) {
        if (extAttrStr == null || extAttrStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = extAttrStr.split(",");
            for (String part : parts) {
                if (part.contains("=")) {
                    String[] kv = part.split("=", 2);
                    if ("attrName".equals(kv[0].trim())) {
                        return kv[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse ext attr: {}", extAttrStr, e);
        }
        
        return null;
    }
}
