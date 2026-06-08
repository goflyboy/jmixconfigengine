package com.jmix.ruletrans.scenario;

import com.jmix.ruletrans.sdk.SdkProfile;

/**
 * Classified rule scenario for prompt and SDK routing.
 */
public record RuleScenario(
        RuleScope scope,
        RuleCalcStage calcStage,
        RuleFamily family,
        SdkProfile sdkProfile) {

    public RuleScenario {
        scope = scope == null ? RuleScope.PRODUCT : scope;
        calcStage = calcStage == null ? RuleCalcStage.NON_POST : calcStage;
        family = family == null ? RuleFamily.UNKNOWN : family;
        sdkProfile = sdkProfile == null
                ? (calcStage == RuleCalcStage.POST ? SdkProfile.POST : SdkProfile.CONSTRAINT)
                : sdkProfile;
    }

    public static RuleScenario constraint(RuleScope scope, RuleFamily family) {
        return new RuleScenario(scope, RuleCalcStage.NON_POST, family, SdkProfile.CONSTRAINT);
    }

    public static RuleScenario post(RuleScope scope, RuleFamily family) {
        return new RuleScenario(scope, RuleCalcStage.POST, family, SdkProfile.POST);
    }

    public boolean isPost() {
        return sdkProfile == SdkProfile.POST || calcStage == RuleCalcStage.POST;
    }
}
