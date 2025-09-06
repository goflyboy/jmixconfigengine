package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.ModuleType;
import lombok.extern.slf4j.Slf4j;

/**
 * DC模块适配器
 * 负责Module到DCModule的字段映射和扩展属性处理
 */
@Slf4j
public class DCModuleAdapter extends AdapterInfo {
    
    @Override
    protected void initMappings() {
        // 字段映射关系
        fieldMappings.put("code", "dccode");  // Module.code -> DCModule.dccode
        // Module.type -> DCModule.type (相同，但DC没有这个字段，使用默认值)
        
        // 扩展属性定义
        extAttrs.put("season", "String");     // 季节属性
        
        // 默认值映射
        defaultValues.put("type", ModuleType.GENERAL);  // DC没有type字段，使用默认值
    }
    
    /**
     * 将Module适配为DCModule
     * @param module 原始模块
     * @return DC模块
     */
    public DCModule adapt(Module module) {
        if (module == null) {
            return null;
        }
        
        DCModule dcModule = new DCModule(module);
        
        // 应用字段映射
        dcModule.setDccode(module.getCode());  // code -> dccode
        
        // 设置扩展属性
        dcModule.setSeason("Spring");  // 默认季节
        
        log.debug("Adapted Module to DCModule: {} -> {}", module.getCode(), dcModule.getDccode());
        return dcModule;
    }
    
    /**
     * 从扩展属性字符串解析属性
     * 格式: "attrName=season,attrType=String"
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
