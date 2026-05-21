package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

/**
 * Validator should also work when a module only has Code rules.
 */
public class CodeRuleOnlyValidateTest extends ModuleScenarioTestBase {

    public CodeRuleOnlyValidateTest() {
        super(CodeOnlyValidateConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @ModuleAnno(id = 7018L)
    public static class CodeOnlyValidateConstraint extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;

        @CodeRuleAnno(leftProObjsStr = "cpu:Select", rightProObjsStr = "drive:Select")
        public void cpu8NotDrive5400() {
            model().addImplication(cpu8.selectedVar(), drive5400.selectedVar().not());
        }
    }

    @Test
    public void testCodeRuleOnlyValidation() {
        assertTrue(validData("cpu4", "drive5400"));

        ModuleValidateResp resp = validateData("cpu8", "drive5400");
        assertFalse(resp.isValid());
        assertTrue(resp.getViolatedRuleCodes().contains("cpu8NotDrive5400"));
    }
}
