package com.jmix.ruletrans.sdk;

import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.scenario.RuleScenario;

/**
 * Builds the SDK context independently from the domain fact projection.
 */
public final class SdkContextBuilder {

    public SdkContext build(RuleContext context, RuleScenario scenario) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        return scenario != null && scenario.isPost()
                ? new PostSdkContext()
                : new ConstraintSdkContext();
    }
}
