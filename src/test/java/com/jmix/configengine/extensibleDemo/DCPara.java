package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.model.Para;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的参数包装类
 * 扩展了原始Para，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCPara extends Para {
    
    /**
     * DC公司特有的参数编码
     */
    private String dcCode;
    
    /**
     * 交付类型 - 扩展字段
     */
    private String deliveryType;
    
    /**
     * 默认构造函数
     */
    public DCPara() {
        super();
    }
    
    /**
     * 从原始Para创建DCPara
     * @param para 原始参数
     */
    public DCPara(Para para) {
        super();
        if (para != null) {
            this.setCode(para.getCode());
            this.setType(para.getType());
            this.setDefaultValue(para.getDefaultValue());
            this.setOptions(para.getOptions());
            this.setFatherCode(para.getFatherCode());
            this.setDescription(para.getDescription());
            this.setSortNo(para.getSortNo());
        }
    }
}
