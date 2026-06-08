package com.jmix.ruletrans.sdk;

import java.util.List;

/**
 * SDK surface exposed to a generated rule method body.
 */
public interface SdkContext {

    SdkProfile profile();

    List<String> allowedApis();

    List<String> forbiddenApis();

    default String allowedApisText() {
        return String.join(", ", allowedApis());
    }

    default String forbiddenApisText() {
        return String.join(", ", forbiddenApis());
    }
}
