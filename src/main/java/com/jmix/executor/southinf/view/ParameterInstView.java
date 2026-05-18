package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

@SouthApiSince(SouthApiVersion.V1_0)
public interface ParameterInstView extends OntoView {

    @SouthApiSince(SouthApiVersion.V1_0)
    String value();

    @SouthApiSince(SouthApiVersion.V1_0)
    void setValue(String value);
}
