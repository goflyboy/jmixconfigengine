package com.jmix.executor.impl.southbridge;

import com.jmix.executor.cmodel.ModuleBaseInst;
import com.jmix.executor.impl.ModuleInstAccessor;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.view.ModuleInstView;
import com.jmix.executor.southinf.view.OntoView;
import com.jmix.executor.southinf.view.ParameterInstView;
import com.jmix.executor.southinf.view.PartCategoryInstSumView;
import com.jmix.executor.southinf.view.PartCategoryInstView;
import com.jmix.executor.southinf.view.PartInstView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * View adapters over ModuleInstAccessor for POST-stage rules.
 */
public final class SouthboundInstViews {

    private SouthboundInstViews() {
    }

    public static class ModuleInstViewImpl implements ModuleInstView {

        private final ModuleInstAccessor accessor;

        public ModuleInstViewImpl(ModuleInstAccessor accessor) {
            this.accessor = accessor;
        }

        @Override
        public String code() {
            return accessor.getModuleCode();
        }

        @Override
        public String extAttr(String extAttrKey) {
            return accessor.getExtAttr(extAttrKey);
        }

        @Override
        public int extAttr4Int(String extAttrKey) {
            return parseInt(extAttr(extAttrKey), extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return accessor.getInstDynAttr(dynAttrKey);
        }

        @Override
        public int dynAttr4Int(String dynAttrKey) {
            return parseInt(dynAttr(dynAttrKey), dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            accessor.setInstDynAttr(dynAttrKey, dynAttrValue);
        }

        @Override
        public Long moduleId() {
            return accessor.getModuleId();
        }

        @Override
        public String instanceConfigId() {
            return accessor.getInstanceConfigId();
        }

        @Override
        public int quantity() {
            return accessor.getModuleQuantity();
        }

        @Override
        public ParameterInstView parameter(String code) {
            return new ModuleParameterInstView(accessor, code);
        }

        @Override
        public PartInstView part(String code) {
            return new PartInstViewImpl(accessor, "", ModuleBaseInst.DEFAULT_INSTANCE_ID, code);
        }

        @Override
        public PartCategoryInstView partCategory(String code) {
            return partCategory(code, ModuleBaseInst.DEFAULT_INSTANCE_ID);
        }

        @Override
        public PartCategoryInstView partCategory(String code, int instId) {
            return new PartCategoryInstViewImpl(accessor, code, instId);
        }

        @Override
        public PartCategoryInstSumView partCategorySum(String code) {
            return new PartCategoryInstSumViewImpl(accessor, code);
        }
    }

    private abstract static class BaseOntoView implements OntoView {

        protected final ModuleInstAccessor accessor;

        protected final String code;

        BaseOntoView(ModuleInstAccessor accessor, String code) {
            this.accessor = accessor;
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public int extAttr4Int(String extAttrKey) {
            return parseInt(extAttr(extAttrKey), extAttrKey);
        }

        @Override
        public int dynAttr4Int(String dynAttrKey) {
            return parseInt(dynAttr(dynAttrKey), dynAttrKey);
        }
    }

    private static class ModuleParameterInstView extends BaseOntoView implements ParameterInstView {

        ModuleParameterInstView(ModuleInstAccessor accessor, String code) {
            super(accessor, code);
        }

        @Override
        public String extAttr(String extAttrKey) {
            return null;
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return null;
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            throw new UnsupportedOperationException("Parameter dynamic attributes are not supported");
        }

        @Override
        public String value() {
            return accessor.getParaValue(code);
        }

        @Override
        public void setValue(String value) {
            accessor.setParaValue(code, value);
        }
    }

    private static class CategoryParameterInstView extends BaseOntoView implements ParameterInstView {

        private final String categoryCode;

        private final int instId;

        CategoryParameterInstView(ModuleInstAccessor accessor, String categoryCode, int instId, String code) {
            super(accessor, code);
            this.categoryCode = categoryCode;
            this.instId = instId;
        }

        @Override
        public String extAttr(String extAttrKey) {
            return null;
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return null;
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            throw new UnsupportedOperationException("Parameter dynamic attributes are not supported");
        }

        @Override
        public String value() {
            return accessor.getParaValue(categoryCode, instId, code);
        }

        @Override
        public void setValue(String value) {
            accessor.setParaValue(categoryCode, instId, code, value);
        }
    }

    private static class PartCategoryInstViewImpl extends BaseOntoView implements PartCategoryInstView {

        private final int instId;

        PartCategoryInstViewImpl(ModuleInstAccessor accessor, String code, int instId) {
            super(accessor, code);
            this.instId = instId;
        }

        @Override
        public String extAttr(String extAttrKey) {
            return accessor.getExtAttr(code, instId, extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return accessor.getInstDynAttr(code, instId, dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            accessor.setInstDynAttr(code, instId, dynAttrKey, dynAttrValue);
        } 
        
        @Override
        public int instanceId() {
            return instId;
        }

        @Override
        public ParameterInstView parameter(String paraCode) {
            return new CategoryParameterInstView(accessor, code, instId, paraCode);
        }

        @Override
        public PartInstView part(String partCode) {
            return new PartInstViewImpl(accessor, code, instId, partCode);
        }

        @Override
        public List<PartInstView> parts() {
            return accessor.getPartCodes(code, instId).stream()
                    .map(this::part)
                    .collect(Collectors.toList());
        }

        @Override
        public int sumQuantity() { 
            return parts().stream()
                    .map(PartInstView::quantity)
                    .reduce(0, Integer::sum);
        }
    }

    private static class PartCategoryInstSumViewImpl extends BaseOntoView implements PartCategoryInstSumView {

        PartCategoryInstSumViewImpl(ModuleInstAccessor accessor, String code) {
            super(accessor, code);
        }

        @Override
        public String extAttr(String extAttrKey) {
            return accessor.getExtAttr(code, ModuleBaseInst.DEFAULT_INSTANCE_ID, extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return accessor.getDynAttr(code, dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            inst(ModuleBaseInst.DEFAULT_INSTANCE_ID).setDynAttr(dynAttrKey, dynAttrValue);
        }

        @Override
        public PartCategoryInstView inst(int instId) {
            return new PartCategoryInstViewImpl(accessor, code, instId);
        }

        @Override
        public List<PartCategoryInstView> insts() {
            return accessor.getInstanceIds(code).stream()
                    .map(this::inst)
                    .collect(Collectors.toList());
        }

        @Override
        public String sumDynAttr(String dynAttrKey) {
            return accessor.getSumDynAttr(code, dynAttrKey);
        }

        @Override
        public int sumDynAttr4Int(String dynAttrKey) {
            return parseInt(sumDynAttr(dynAttrKey), dynAttrKey);
        }

        @Override
        public List<String> dynAttrs(String dynAttrKey) {
            return accessor.getDynAttrValues(code, dynAttrKey);
        }

        @Override
        public List<Integer> dynAttrs4Int(String dynAttrKey) {
            return dynAttrs(dynAttrKey).stream()
                    .map(value -> parseInt(value, dynAttrKey))
                    .collect(Collectors.toList());
        }

        @Override
        public int sumSumQuantity() {
            return insts().stream()
                    .map(PartCategoryInstView::sumQuantity)
                    .reduce(0, Integer::sum);
        }
    }

    private static class PartInstViewImpl extends BaseOntoView implements PartInstView {

        private final String categoryCode;

        private final int instId;

        PartInstViewImpl(ModuleInstAccessor accessor, String categoryCode, int instId, String code) {
            super(accessor, code);
            this.categoryCode = categoryCode;
            this.instId = instId;
        }

        @Override
        public String extAttr(String extAttrKey) {
            return accessor.getExtAttr(categoryCode, instId, code, extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return accessor.getInstDynAttr(categoryCode, instId, code, dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            accessor.setInstDynAttr(categoryCode, instId, code, dynAttrKey, dynAttrValue);
        }

        @Override
        public int quantity() {
            return accessor.getPartQuantity(categoryCode, instId, code);
        }

        @Override
        public void setQuantity(int quantity) {
            accessor.setPartQuantity(categoryCode, instId, code, quantity);
        }

        @Override
        public boolean selected() {
            return accessor.isPartSelected(categoryCode, instId, code);
        }
    }

    private static int parseInt(String value, String key) {
        if (value == null || value.isEmpty()) {
            throw new AlgLoaderException("Cannot convert empty value to int for key: " + key);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AlgLoaderException("Cannot convert value to int for key: " + key + ", value=" + value, e);
        }
    }
}
