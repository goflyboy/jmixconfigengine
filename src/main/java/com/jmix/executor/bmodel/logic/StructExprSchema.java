package com.jmix.executor.bmodel.logic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向维护态的结构化对象属性表达式。
 *
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StructExprSchema extends ExprSchema {
    private String objectType = RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY;

    private String objectCode;

    private String attrCode;

    private StructCompareOperator operator;

    private List<String> values = new ArrayList<>();

    public List<RefProgObjSchema> getRefProgObjs() {
        List<RefProgObjSchema> refs = new ArrayList<>();
        if (objectCode != null && !objectCode.isEmpty()) {
            refs.add(new RefProgObjSchema(objectType, objectCode, attrCode));
        }
        refs.addAll(super.getRefProgObjs());
        return refs;
    }
}
