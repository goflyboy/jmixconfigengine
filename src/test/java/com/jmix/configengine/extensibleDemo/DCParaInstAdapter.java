package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import lombok.extern.slf4j.Slf4j;

/**
 * DC参数实例适配器
 * 负责ParaInst到DCParaInst的字段映射和扩展属性处理
 */
@Slf4j
public class DCParaInstAdapter extends AdapterInfo {
    
    @Override
    protected void initMappings() {
        // 字段映射关系 - ParaInst和DCParaInst字段基本相同
        fieldMappings.put("code", "code");
        fieldMappings.put("value", "value");
        fieldMappings.put("options", "options");
        fieldMappings.put("isHidden", "isHidden");
        
        // 扩展属性定义
        extAttrs.put("deliveryType", "String");  // 交付类型属性
    }
    
    /**
     * 将ParaInst适配为DCParaInst
     * @param paraInst 原始参数实例
     * @return DC参数实例
     */
    public DCParaInst adapt(ParaInst paraInst) {
        if (paraInst == null) {
            return null;
        }
        
        DCParaInst dcParaInst = new DCParaInst(paraInst);
        
        // 设置扩展属性 - 默认交付类型
        dcParaInst.setDeliveryType("Standard");
        
        log.debug("Adapted ParaInst to DCParaInst: {} with deliveryType: {}", 
                paraInst.getCode(), dcParaInst.getDeliveryType());
        return dcParaInst;
    }
    
    /**
     * 从扩展属性字符串解析属性
     * 格式: "attrName=deliveryType,attrType=String"
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
