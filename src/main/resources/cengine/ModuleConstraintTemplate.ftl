package ${module.packageName};

import com.jmix.executor.southinf.AlgorithmApiVersion;
import com.jmix.executor.southinf.ConstraintAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import java.util.List;

/**
 * Generated constraint class for ${module.code}.
 */
@ModuleAnno(id = ${module.base.id?c})
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "generated")
public class ${module.code}Constraint extends ConstraintAlgBase {

<#list module.paras as para>
    @ParaAnno
    private ParaVar ${para.varName};
</#list>

<#list module.parts as part>
    @PartAnno
    private PartVar ${part.varName};
</#list>

<#list module.rules as rule>
    @CodeRuleAnno(normalNaturalCode = "${(rule.normalNaturalCode!'')?js_string}")
    private void ${rule.code}() {
<#if rule.isCompatibleRule() && rule.left?? && rule.right??>
        List.of(<#list rule.leftFilterCodes as filterCode>"${filterCode}"<#if filterCode_has_next>, </#if></#list>)
                .forEach(option -> List.of(<#list rule.rightFilterCodes as filterCode>"${filterCode}"<#if filterCode_has_next>, </#if></#list>)
                        .forEach(right -> model().compatibilityCoDependent("${rule.code}",
                                ${rule.left.varName}.option(option).selected(),
                                ${rule.right.varName}.option(right).selected())));
<#elseif rule.isCalculateRule()>
        throw new UnsupportedOperationException("Generated calculate rule is not implemented: ${rule.code}");
<#else>
        throw new UnsupportedOperationException("Generated rule type is not implemented: ${rule.code}");
</#if>
    }

</#list>
}
