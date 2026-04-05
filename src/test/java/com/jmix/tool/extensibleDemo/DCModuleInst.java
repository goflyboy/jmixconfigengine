package com.jmix.tool.extensibleDemo;

import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartCategoryInst;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

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
     *
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
            this.setParas(new ArrayList<>(moduleInst.getParas()));
            this.setParts(new ArrayList<>(moduleInst.getParts()));
            // 复制部件分类实例
            this.setPartCategorys(new ArrayList<>(moduleInst.getPartCategorys()));
        }
    }
}
