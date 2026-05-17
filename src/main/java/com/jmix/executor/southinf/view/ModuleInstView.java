package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * View interface for module instance access.
 */
public interface ModuleInstView extends OntoView {

    @SouthApiSince(SouthApiVersion.V1_0)
    Long moduleId();

    @SouthApiSince(SouthApiVersion.V1_0)
    String instanceConfigId();

    @SouthApiSince(SouthApiVersion.V1_0)
    int quantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    ParameterInstView parameter(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartInstView part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView partCategory(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView partCategory(String code, int instId);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstSumView partCategorySum(String code);
}
