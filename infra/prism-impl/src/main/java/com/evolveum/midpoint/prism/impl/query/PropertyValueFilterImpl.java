/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PropertyValueFilterImpl<T> extends ValueFilterImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> implements
        PropertyValueFilter<T> {

    PropertyValueFilterImpl(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition, @Nullable QName matchingRule,
            @Nullable List<PrismPropertyValue<T>> values, @Nullable ExpressionWrapper expression,
            @Nullable ItemPath rightHandSidePath, @Nullable ItemDefinition rightHandSideDefinition) {
        super(path, definition, matchingRule, values, expression, rightHandSidePath, rightHandSideDefinition);
    }

//    static protected <T> List<PrismPropertyValue<T>> createPropertyList(@NotNull PrismPropertyDefinition itemDefinition, @Nullable PrismPropertyValue<T> pValue) {
//        List<PrismPropertyValue<T>> pValues = new ArrayList<>();
//        if (pValue == null) {
//            return pValues;
//        } else {
//            PrismUtil.recomputePrismPropertyValue(pValue, itemDefinition.getPrismContext());
//            pValues.add(pValue);
//            return pValues;
//        }
//    }
//
//    static <T> List<PrismPropertyValue<T>> createPropertyList(PrismPropertyDefinition itemDefinition, PrismPropertyValue<T>[] values) {
//        Validate.notNull(itemDefinition, "Item definition in substring filter must not be null.");
//
//        List<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>();
//
//        for (PrismPropertyValue<T> val : values){
//            PrismUtil.recomputePrismPropertyValue(val, itemDefinition.getPrismContext());
//            pValues.add(val);
//        }
//
//        return pValues;
//    }

    @NotNull
    static <T> List<PrismPropertyValue<T>> anyArrayToPropertyValueList(PrismContext prismContext, Object[] values) {
        List<PrismPropertyValue<T>> pVals = new ArrayList<>();
        if (values != null) {
            for (Object value : values) {
                addToPrismValues((List) pVals, prismContext, value);
            }
        }
        return pVals;
    }

    @NotNull
    static <T> List<PrismPropertyValue<T>> anyValueToPropertyValueList(PrismContext prismContext, Object value) {
        List<PrismPropertyValue<T>> pVals = new ArrayList<>();
        if (value != null) {
            addToPrismValues((List) pVals, prismContext, value);
        }
        return pVals;
    }

    private static void addToPrismValues(List<PrismPropertyValue<?>> pVals, PrismContext prismContext, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            for (Object o : (Collection) value) {
                addToPrismValues(pVals, prismContext, o);
            }
            return;
        }
        if (value.getClass().isArray()) {
            throw new IllegalStateException("Array within array in filter creation: " + value);
        }

        PrismPropertyValue<?> pVal;
        if (value instanceof PrismPropertyValue) {
            pVal = (PrismPropertyValue<?>) value;
            if (pVal.getParent() != null) {
                pVal = pVal.clone();
            }
        } else {
            pVal = new PrismPropertyValueImpl<>(value);
        }
        PrismUtil.recomputePrismPropertyValue(pVal, prismContext);
        pVals.add(pVal);
    }

//    static <T> List<PrismPropertyValue<T>> realValueToPropertyList(PrismPropertyDefinition itemDefinition, Object realValue) {
//        List<PrismPropertyValue<T>> pVals = new ArrayList<>();
//        if (realValue == null) {
//            return pVals;
//        }
//
//        if (realValue.getClass() != null && Collection.class.isAssignableFrom(realValue.getClass())) {
//            for (Object o : (Iterable)realValue){
//                if (o instanceof PrismPropertyValue){
//                    PrismPropertyValue pVal = (PrismPropertyValue) o;
//                    PrismUtil.recomputePrismPropertyValue(pVal, itemDefinition.getPrismContext());
//                    pVals.add(pVal);
//                }else{
//                    // TODO what's this???
//                    pVals.addAll(PrismPropertyValue.createCollection((Collection<T>) realValue));
//                }
//            }
//
//        } else {
//            PrismUtil.recomputeRealValue(realValue, itemDefinition.getPrismContext());
//            pVals.add(new PrismPropertyValueImpl<T>(realValue));
//        }
//        return pVals;
//    }


//    protected void cloneValues(PropertyValueFilter<V> clone) {
//        clone.values = getCloneValuesList();
//        if (clone.values != null) {
//            for (V clonedValue: clone.values) {
//                clonedValue.setParent(clone);
//            }
//        }
//    }

    // TODO cleanup this mess - how values are cloned, that expression is not cloned in LT/GT filter etc

    public abstract PropertyValueFilterImpl clone();

    void checkPrismPropertyValue(Object value) {
        if (!(value instanceof PrismPropertyValue)) {
            throw new IllegalArgumentException("Not supported prism value for equals filter. It must be an instance of PrismPropertyValue but it is " + value.getClass());
        }
    }
}
