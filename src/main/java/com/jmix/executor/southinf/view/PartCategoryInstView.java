package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

import java.util.List;

/**
 * View interface for part category instance access.
 */
public interface PartCategoryInstView extends OntoView {

    @SouthApiSince(SouthApiVersion.V1_0)
    int sumQuantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    int instanceId();

    @SouthApiSince(SouthApiVersion.V1_0)
    ParameterInstView parameter(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartInstView part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartInstView> parts();
}
