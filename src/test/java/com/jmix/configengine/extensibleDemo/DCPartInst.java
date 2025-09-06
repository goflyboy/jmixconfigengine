package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的部件实例包装类
 * 扩展了原始PartInst，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCPartInst extends PartInst {
    
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
    public DCPartInst() {
        super();
    }
    
    /**
     * 从原始PartInst创建DCPartInst
     * @param partInst 原始部件实例
     */
    public DCPartInst(PartInst partInst) {
        super();
        if (partInst != null) {
            this.setCode(partInst.getCode());
            this.setQuantity(partInst.getQuantity());
            this.setSelectAttrValue(partInst.getSelectAttrValue());
            this.setHidden(partInst.isHidden());
        }
    }
}
