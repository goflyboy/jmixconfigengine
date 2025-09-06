package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.model.Part;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的部件包装类
 * 扩展了原始Part，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCPart extends Part {
    
    /**
     * DC公司特有的部件编号
     */
    private String partNumber;
    
    /**
     * 交付类型 - 扩展字段
     */
    private String deliveryType;
    
    /**
     * 默认构造函数
     */
    public DCPart() {
        super();
    }
    
    /**
     * 从原始Part创建DCPart
     * @param part 原始部件
     */
    public DCPart(Part part) {
        super();
        if (part != null) {
            this.setCode(part.getCode());
            this.setType(part.getType());
            this.setPrice(part.getPrice());
            this.setMaxQuantity(part.getMaxQuantity());
            this.setAttrs(part.getAttrs());
            this.setAttrSchemas(part.getAttrSchemas());
            this.setDefaultValue(part.getDefaultValue());
            this.setFatherCode(part.getFatherCode());
        }
    }
}
