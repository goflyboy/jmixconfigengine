package com.jmix.executor.impl.southbridge;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.cmodel.ModuleBaseInst;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.view.ModuleInstView;
import com.jmix.executor.southinf.view.OntoView;
import com.jmix.executor.southinf.view.ParameterInstView;
import com.jmix.executor.southinf.view.PartCategoryInstSumView;
import com.jmix.executor.southinf.view.PartCategoryInstView;
import com.jmix.executor.southinf.view.PartInstView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SouthboundInstViews {

    private SouthboundInstViews() {
    }

    public static class ModuleInstViewImpl implements ModuleInstView {

        private final InstContext context;

        public ModuleInstViewImpl(Module module, ModuleInst moduleInst) {
            this.context = new InstContext(module, moduleInst);
        }

        @Override
        public String code() {
            return context.moduleInst.getCode();
        }

        @Override
        public String extAttr(String extAttrKey) {
            return context.module.getExtAttr(extAttrKey);
        }

        @Override
        public int extAttr4Int(String extAttrKey) {
            return parseInt(extAttr(extAttrKey), extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return getMapValue(context.moduleInst.getExtAttrs(), dynAttrKey);
        }

        @Override
        public int dynAttr4Int(String dynAttrKey) {
            return parseInt(dynAttr(dynAttrKey), dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            context.moduleInst.getExtAttrs().put(dynAttrKey, dynAttrValue);
        }

        @Override
        public Long moduleId() {
            return context.moduleInst.getId();
        }

        @Override
        public String instanceConfigId() {
            return context.moduleInst.getInstanceConfigId();
        }

        @Override
        public int quantity() {
            return context.moduleInst.getQuantity() == null ? 0 : context.moduleInst.getQuantity();
        }

        @Override
        public ParameterInstView parameter(String code) {
            return new ModuleParameterInstView(context, code);
        }

        @Override
        public PartInstView part(String code) {
            return new PartInstViewImpl(context, "", ModuleBaseInst.DEFAULT_INSTANCE_ID, code);
        }

        @Override
        public PartCategoryInstView partCategory(String code) {
            return partCategory(code, ModuleBaseInst.DEFAULT_INSTANCE_ID);
        }

        @Override
        public PartCategoryInstView partCategory(String code, int instId) {
            return new PartCategoryInstViewImpl(context, code, instId);
        }

        @Override
        public PartCategoryInstSumView partCategorySum(String code) {
            return new PartCategoryInstSumViewImpl(context, code);
        }
    }

    private abstract static class BaseOntoView implements OntoView {

        protected final InstContext context;

        protected final String code;

        BaseOntoView(InstContext context, String code) {
            this.context = context;
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

        ModuleParameterInstView(InstContext context, String code) {
            super(context, code);
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
            return context.findModuleParaInst(code).getValue();
        }

        @Override
        public void setValue(String value) {
            context.findModuleParaInst(code).setValue(value);
        }
    }

    private static class CategoryParameterInstView extends BaseOntoView implements ParameterInstView {

        private final String categoryCode;

        private final int instId;

        CategoryParameterInstView(InstContext context, String categoryCode, int instId, String code) {
            super(context, code);
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
            return context.findParaInst(context.findPartCategoryInst(categoryCode, instId), code).getValue();
        }

        @Override
        public void setValue(String value) {
            context.findParaInst(context.findPartCategoryInst(categoryCode, instId), code).setValue(value);
        }
    }

    private static class PartCategoryInstViewImpl extends BaseOntoView implements PartCategoryInstView {

        private final int instId;

        PartCategoryInstViewImpl(InstContext context, String code, int instId) {
            super(context, code);
            this.instId = instId;
        }

        @Override
        public String extAttr(String extAttrKey) {
            PartCategory category = context.module.getPartCategory(code);
            if (category == null) {
                throw new AlgLoaderException("PartCategory not found in module: " + code);
            }
            return category.getExtAttr(extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return getMapValue(context.findPartCategoryInst(code, instId).getExtAttrs(), dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            context.findPartCategoryInst(code, instId).getExtAttrs().put(dynAttrKey, dynAttrValue);
        }

        @Override
        public int instanceId() {
            return instId;
        }

        @Override
        public ParameterInstView parameter(String paraCode) {
            return new CategoryParameterInstView(context, code, instId, paraCode);
        }

        @Override
        public PartInstView part(String partCode) {
            return new PartInstViewImpl(context, code, instId, partCode);
        }

        @Override
        public List<PartInstView> parts() {
            return context.findPartCategoryInst(code, instId).getParts().stream()
                    .map(partInst -> part(partInst.getCode()))
                    .collect(Collectors.toList());
        }

        @Override
        public int sumQuantity() {
            return context.effectiveParts(code, instId).stream()
                    .map(PartInst::getQuantity)
                    .reduce(0, Integer::sum);
        }
    }

    private static class PartCategoryInstSumViewImpl extends BaseOntoView implements PartCategoryInstSumView {

        PartCategoryInstSumViewImpl(InstContext context, String code) {
            super(context, code);
        }

        @Override
        public String extAttr(String extAttrKey) {
            PartCategory category = context.module.getPartCategory(code);
            if (category == null) {
                throw new AlgLoaderException("PartCategory not found in module: " + code);
            }
            return category.getExtAttr(extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            List<PartInst> parts = context.effectiveParts(code, ModuleBaseInst.DEFAULT_INSTANCE_ID);
            if (parts.isEmpty()) {
                return null;
            }
            return context.partAttr(parts.get(0).getCode(), dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            inst(ModuleBaseInst.DEFAULT_INSTANCE_ID).setDynAttr(dynAttrKey, dynAttrValue);
        }

        @Override
        public PartCategoryInstView inst(int instId) {
            return new PartCategoryInstViewImpl(context, code, instId);
        }

        @Override
        public List<PartCategoryInstView> insts() {
            return context.instanceIds(code).stream()
                    .map(this::inst)
                    .collect(Collectors.toList());
        }

        @Override
        public String sumDynAttr(String dynAttrKey) {
            int sum = 0;
            for (PartInst partInst : context.effectiveParts(code, ModuleBaseInst.DEFAULT_INSTANCE_ID)) {
                sum += parseInt(context.partAttr(partInst.getCode(), dynAttrKey), dynAttrKey)
                        * partInst.getQuantity();
            }
            return String.valueOf(sum);
        }

        @Override
        public int sumDynAttr4Int(String dynAttrKey) {
            return parseInt(sumDynAttr(dynAttrKey), dynAttrKey);
        }

        @Override
        public List<String> dynAttrs(String dynAttrKey) {
            return context.effectiveParts(code, ModuleBaseInst.DEFAULT_INSTANCE_ID).stream()
                    .map(part -> context.partAttr(part.getCode(), dynAttrKey))
                    .collect(Collectors.toList());
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

        PartInstViewImpl(InstContext context, String categoryCode, int instId, String code) {
            super(context, code);
            this.categoryCode = categoryCode;
            this.instId = instId;
        }

        @Override
        public String extAttr(String extAttrKey) {
            return context.findPartModel(code).getExtAttr(extAttrKey);
        }

        @Override
        public String dynAttr(String dynAttrKey) {
            return getMapValue(context.findPartInst(categoryCode, instId, code).getExtAttrs(), dynAttrKey);
        }

        @Override
        public void setDynAttr(String dynAttrKey, String dynAttrValue) {
            PartInst partInst = context.findPartInst(categoryCode, instId, code);
            if (partInst.getExtAttrs() == null) {
                partInst.setExtAttrs(new java.util.HashMap<>());
            }
            partInst.getExtAttrs().put(dynAttrKey, dynAttrValue);
        }

        @Override
        public int quantity() {
            Integer quantity = context.findPartInst(categoryCode, instId, code).getQuantity();
            return quantity == null ? 0 : quantity;
        }

        @Override
        public void setQuantity(int quantity) {
            PartInst partInst = context.findPartInst(categoryCode, instId, code);
            partInst.setQuantity(quantity);
            partInst.setSelected(quantity > 0);
        }

        @Override
        public boolean selected() {
            return context.findPartInst(categoryCode, instId, code).isSelected();
        }
    }

    private static class InstContext {
        private final Module module;

        private final ModuleInst moduleInst;

        InstContext(Module module, ModuleInst moduleInst) {
            this.module = module;
            this.moduleInst = moduleInst;
        }

        List<PartInst> effectiveParts(String partCategoryCode, int instId) {
            PartCategoryInst pcInst = findPartCategoryInst(partCategoryCode, instId);
            List<PartInst> result = new ArrayList<>();
            for (PartInst partInst : pcInst.getParts()) {
                if (partInst.isSelected() && partInst.getQuantity() != null && partInst.getQuantity() > 0) {
                    result.add(partInst);
                } else if (!partInst.isSelected() && partInst.getQuantity() != null && partInst.getQuantity() > 0) {
                    result.add(partInst);
                }
            }
            return result;
        }

        String partAttr(String partCode, String attrCode) {
            String attrValue = findPartModel(partCode).getDynAttr().get(attrCode);
            if (attrValue == null) {
                throw new AlgLoaderException(
                        "Attribute '" + attrCode + "' not found on part '" + partCode + "'");
            }
            return attrValue;
        }

        Part findPartModel(String partCode) {
            for (Part part : module.getAllAtomicParts()) {
                if (partCode.equals(part.getCode())) {
                    return part;
                }
            }
            throw new AlgLoaderException("Part not found in module: " + partCode);
        }

        PartInst findPartInst(String partCategoryCode, int instId, String partCode) {
            PartInst partInst;
            if (partCategoryCode == null || partCategoryCode.isEmpty()) {
                partInst = moduleInst.queryPart(partCode);
            } else {
                partInst = findPartCategoryInst(partCategoryCode, instId).queryPart(partCode);
            }
            if (partInst == null) {
                throw new AlgLoaderException(
                        "PartInst not found: category=" + partCategoryCode + ", instId=" + instId
                                + ", partCode=" + partCode);
            }
            return partInst;
        }

        PartCategoryInst findPartCategoryInst(String partCategoryCode, int instId) {
            for (PartCategoryInst pcInst : moduleInst.getAllPartCategorys()) {
                if (partCategoryCode.equals(pcInst.getCode()) && pcInst.getInstanceId() == instId) {
                    return pcInst;
                }
            }
            throw new AlgLoaderException(
                    "PartCategoryInst not found: code=" + partCategoryCode + ", instId=" + instId);
        }

        ParaInst findModuleParaInst(String paraCode) {
            return findParaInst(moduleInst, paraCode);
        }

        ParaInst findParaInst(ModuleBaseInst baseInst, String paraCode) {
            for (ParaInst paraInst : baseInst.getParas()) {
                if (paraCode.equals(paraInst.getCode())) {
                    return paraInst;
                }
            }
            throw new AlgLoaderException(
                    "ParaInst not found for code: " + paraCode + " in " + baseInst.getCode());
        }

        List<Integer> instanceIds(String partCategoryCode) {
            List<Integer> ids = new ArrayList<>();
            for (PartCategoryInst pcInst : moduleInst.getPartCategorys()) {
                if (partCategoryCode.equals(pcInst.getCode())) {
                    ids.add(pcInst.getInstanceId());
                }
            }
            if (ids.isEmpty()) {
                throw new AlgLoaderException("PartCategory not found in ModuleInst: " + partCategoryCode);
            }
            return ids;
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

    private static String getMapValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
