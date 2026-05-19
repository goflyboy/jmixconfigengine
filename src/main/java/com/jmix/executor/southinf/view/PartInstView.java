package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

@SouthApiSince(SouthApiVersion.V1_0)
public interface PartInstView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    int quantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    void setQuantity(int quantity);

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean selected();
}
