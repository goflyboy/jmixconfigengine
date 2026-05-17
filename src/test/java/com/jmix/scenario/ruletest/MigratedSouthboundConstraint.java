package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.AlgorithmApiVersion;
import com.jmix.executor.southinf.ConstraintAlgBase;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

@ModuleAnno(id = 5005L)
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0005-test")
public class MigratedSouthboundConstraint extends ConstraintAlgBase {

    @PartAnno
    private PartVar part1;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(part1.quantity(), 1);
    }
}
