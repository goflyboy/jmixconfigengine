package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块算法实现类
 * 实现ConstraintAlg接口，提供约束求解的具体实现
 * 继承自ModuleBaseAlgImpl，支持模块级别和部件分类级别的求和操作
 *
 * @since 2025-04-05
 */
@Slf4j
public class ModuleAlgImpl extends ModuleBaseAlgImpl {

    /**
     * 部件分类算法实例映射表
     */
    protected Map<String, PartCategoryAlgImpl> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 初始化模块算法实例
     * 按partCategoryCode对partConstraintFromReqs进行分组，然后初始化本层和子层的变量与规则
     * 重写基类方法，添加PartCategoryAlgImpl的初始化
     *
     * @param model                    CP约束模型
     * @param module                   模块对象
     * @param partConstraintFromReqs   来自请求的部件约束列表
     */
    @Override
    public void init(AlgCPModel model, IModule module,
                     java.util.List<com.jmix.executor.model.ParConstraint> partConstraintFromReqs) {
        // 调用基类初始化
        super.init(model, module, partConstraintFromReqs);

        // 按partCategoryCode对partConstraintFromReqs进行分组
        Map<String, java.util.List<com.jmix.executor.model.ParConstraint>> partConstraintFromReqMap =
                groupConstraintsByPartCategory(partConstraintFromReqs);

        // 构建规则方法映射
        Map<String, Method> allRuleMethods = buildAllRuleMethods(module);

        // 如果module有PartCategorys，则对每个PartCategory创建并初始化PartCategoryAlgImpl
        if (module instanceof Module) {
            Module bModule = (Module) module;
            if (bModule.getPartCategorys() != null && !bModule.getPartCategorys().isEmpty()) {
                for (PartCategory partCategory : bModule.getPartCategorys()) {
                    String categoryCode = partCategory.getCode();
                    java.util.List<com.jmix.executor.model.ParConstraint> pc4PartConstraintFromReqs =
                            partConstraintFromReqMap.get(categoryCode);

                    PartCategoryAlgImpl pcAlg = new PartCategoryAlgImpl();
                    pcAlg.init(model, partCategory, pc4PartConstraintFromReqs, allRuleMethods);

                    // 执行PartCategoryAlgImpl的规则
                    pcAlg.initRule(allRuleMethods);

                    partCategoryAlgs.put(categoryCode, pcAlg);
                }
            }
        }

        log.info("ModuleAlgImpl initialized with {} partCategory algorithms", partCategoryAlgs.size());
    }

    /**
     * 获取部件分类算法实例
     *
     * @param categoryCode 部件分类代码
     * @return PartCategoryAlgImpl实例
     */
    public PartCategoryAlgImpl getPartCategoryAlg(String categoryCode) {
        return partCategoryAlgs.get(categoryCode);
    }

    /**
     * 获取所有部件分类算法实例
     *
     * @return 部件分类算法映射表
     */
    public Map<String, PartCategoryAlgImpl> getPartCategoryAlgs() {
        return partCategoryAlgs;
    }

    /**
     * 获取Module对象
     *
     * @return Module对象
     */
    public Module getModule() {
        return (Module) super.getModule();
    }

    /**
     * 获取所有部件变量
     *
     * @return 部件变量映射
     */
    public Map<String, PartVar> getPartMap() {
        return partMap;
    }

    /**
     * 获取所有参数变量
     *
     * @return 参数变量映射
     */
    public Map<String, ParaVar> getParaMap() {
        return paraMap;
    }
}
