package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.model.Module;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的模块包装类
 * 扩展了原始Module，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCModule extends Module {
    
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
    public DCModule() {
        super();
    }
    
    /**
     * 从原始Module创建DCModule
     * @param module 原始模块
     */
    public DCModule(Module module) {
        super();
        if (module != null) {
            this.setId(module.getId());
            this.setCode(module.getCode());
            this.setVersion(module.getVersion());
            this.setPackageName(module.getPackageName());
            this.setType(module.getType());
            this.setParas(module.getParas());
            this.setParts(module.getParts());
            this.setRules(module.getRules());
            this.setAlg(module.getAlg());
            this.init();
        }
    }
}
