package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的参数实例包装类
 * 扩展了原始ParaInst，添加了DC特有的字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCParaInst extends ParaInst {
    
    /**
     * 交付类型 - 扩展字段
     */
    private String deliveryType;
    
    /**
     * 默认构造函数
     */
    public DCParaInst() {
        super();
    }
    
    /**
     * 从原始ParaInst创建DCParaInst
     * @param paraInst 原始参数实例
     */
    public DCParaInst(ParaInst paraInst) {
        super();
        if (paraInst != null) {
            this.setCode(paraInst.getCode());
            this.setValue(paraInst.getValue());
            this.setOptions(paraInst.getOptions());
            this.setHidden(paraInst.isHidden());
        }
    }
}
