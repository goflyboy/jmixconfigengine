package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor.ModuleInst;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的模块实例包装类
 * 扩展了原始ModuleInst，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCModuleInst extends ModuleInst {
    
    /**
     * DC公司特有的模块编码
     */
    private String dccode;
    
    /**
     * 季节属性 - 扩展字段
     */
    private String season;
    
    /**
     * 默认构造函数
     */
    public DCModuleInst() {
        super();
    }
    
    /**
     * 从原始ModuleInst创建DCModuleInst
     * @param moduleInst 原始模块实例
     */
    public DCModuleInst(ModuleInst moduleInst) {
        super();
        if (moduleInst != null) {
            this.setId(moduleInst.getId());
            this.setCode(moduleInst.getCode());
            this.setInstanceConfigId(moduleInst.getInstanceConfigId());
            this.setInstanceId(moduleInst.getInstanceId());
            this.setQuantity(moduleInst.getQuantity());
            this.setParas(moduleInst.getParas());
            this.setParts(moduleInst.getParts());
        }
    }
}
