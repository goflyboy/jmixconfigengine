package com.jmix.executor.southinf.view;

public interface ModuleInstView extends OntoView {

    Long moduleId();

    String instanceConfigId();

    int quantity();

    ParameterInstView parameter(String code);

    PartInstView part(String code);

    PartCategoryInstView partCategory(String code);

    PartCategoryInstView partCategory(String code, int instId);

    PartCategoryInstSumView partCategorySum(String code);
}
