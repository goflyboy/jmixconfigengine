package com.jmix.executor.southinf.view;

import java.util.List;

public interface PartCategoryInstView extends QuantityInstView {

    int instanceId();

    ParameterInstView parameter(String code);

    PartInstView part(String code);

    List<PartInstView> parts();
}
