package com.jmix.ruletrans.rulescenario;

import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleCalcStage;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.ruletrans.sdk.SdkProfile;

import org.junit.jupiter.api.Test;

import java.util.List;

class ChineseRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testChineseCategoryIdentificationValidatesMappedCodes() {
        Module module = productContext(ProductCompatibilityRuleTransScenarioTest.CpuDriveFacts.class).module();
        CategoryIdentifier identifier = new CategoryIdentifier(
                realLlmInvoker(),
                new PromptBuilder());

        List<String> codes = identifier.identify("处理器是四核时不能选择 5400 转硬盘", module);

        assertEquals(List.of("cpu", "drive"), codes);
    }

    @Test
    void testChineseScenarioClassifierRoutesPostAndConstraintProfiles() {
        RuleScenarioClassifier classifier = new RuleScenarioClassifier();

        RuleScenario post = classifier.classify(
                "把硬盘容量总和写回产品参数",
                RuleContextFactory.product(
                        productContext(ProductCompatibilityRuleTransScenarioTest.CpuDriveFacts.class).module()));
        assertEquals(RuleCalcStage.POST, post.calcStage());
        assertEquals(SdkProfile.POST, post.sdkProfile());
        assertEquals(RuleFamily.POST, post.family());

        RuleScenario compatible = classifier.classify(
                "处理器是四核时不能选择 5400 转硬盘",
                RuleContextFactory.product(
                        productContext(ProductCompatibilityRuleTransScenarioTest.CpuDriveFacts.class).module()));
        assertEquals(RuleScope.PRODUCT, compatible.scope());
        assertEquals(SdkProfile.CONSTRAINT, compatible.sdkProfile());
        assertEquals(RuleFamily.COMPATIBLE, compatible.family());
    }

    @Test
    void testChineseSelectionWordsRouteToSelectionFamily() {
        RuleScenario scenario = new RuleScenarioClassifier().classify(
                "CPU 最多配置一块",
                partCategoryContext(SelectionRuleTransScenarioTest.SelectionFacts.class, "cpu"));

        assertEquals(RuleScope.PART_CATEGORY, scenario.scope());
        assertEquals(SdkProfile.CONSTRAINT, scenario.sdkProfile());
        assertTrue(scenario.family() == RuleFamily.SELECT || scenario.family() == RuleFamily.AGGREGATE);
    }

}
