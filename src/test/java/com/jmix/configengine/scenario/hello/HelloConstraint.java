package com.jmix.configengine.scenario.hello;

import com.jmix.configengine.artifact.ConstraintAlg;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import com.google.ortools.sat.CpModel;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello模块约束算法
 */
@Slf4j
@ModuleAnno(
    id = 123L,
    code = "Hello",
    packageName = "com.jmix.configengine.scenario.hello",
    description = "Hello模块约束算法",
    extSchema = "HelloModuleSchema"
)
public class HelloConstraint extends ConstraintAlgImpl {
    
    @ParaAnno(
        code = "Color",
        fatherCode = "Hello",
        defaultValue = "Red",
        description = "颜色参数",
        options = {"Red", "Black", "White"},
        extAttrs = {"category:color", "required:true"}
    )
    private ParaVar ColorVar;
    
    @PartAnno(
        code = "TShirt11",
        fatherCode = "Hello",
        description = "T恤衫主体部件",
        price = 100L,
        attrs = {"weight:180g", "size:M"},
        extAttrs = {"material:cotton", "brand:Hello"}
    )
    private PartVar TShirt11Var;
 
    @Override
    protected void initConstraint() {
        log.info("初始化Hello约束");
        // 初始化约束
    }
} 