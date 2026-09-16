/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.*;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.prism.xml.ns._public.types_3.*;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;

/**
 * @author Radovan Semancik
 */
public class ItemDeltaCelValue<V extends PrismValue, D extends ItemDefinition<?>> extends AbstractStructuredCelValue<Object> implements MidPointValueProducer<ItemDelta<V,D>> {

    public static final String ITEM_DELTA_PACKAGE_NAME = ItemDeltaType.class.getTypeName();
    private static final String F_PATH = ItemDeltaType.F_PATH.getLocalPart();
    private static final String F_VALUES_TO_ADD = "valuesToAdd";
    private static final String F_VALUES_TO_DELETE = "valuesToDelete";
    private static final String F_VALUES_TO_REPLACE = "valuesToReplace";
    private static final String F_ESTIMATED_OLD_VALUES = "estimatedOldValues";
    public static final CelType CEL_TYPE = createCelType();

    private final ItemDelta<V,D> itemDelta;

    ItemDeltaCelValue(ItemDelta<V,D> itemDelta) {
        this.itemDelta = itemDelta;
    }

    public static <V extends PrismValue, D extends ItemDefinition<?>> ItemDeltaCelValue<V,D> create(ItemDelta<V,D> itemDelta) {
        return new ItemDeltaCelValue<V,D>(itemDelta);
    }

    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_PATH, wrapItemPath(itemDelta.getPath()));
        value.put(F_VALUES_TO_ADD, wrapRealValues(itemDelta.getRealValuesToAdd()));
        value.put(F_VALUES_TO_DELETE, wrapRealValues(itemDelta.getRealValuesToDelete()));
        value.put(F_VALUES_TO_REPLACE, wrapRealValues(itemDelta.getRealValuesToReplace()));
        value.put(F_ESTIMATED_OLD_VALUES, wrapPrismValues(itemDelta.getEstimatedOldValues()));
        return value;
    }

    private Object wrapRealValues(Collection<?> values) {
        return values == null ? NullValue.NULL_VALUE : values.stream().map(this::wrapRealValue).toList();
    }

    private Object wrapRealValue(Object value) {
        return value == null ? NullValue.NULL_VALUE : CelTypeMapper.toCelValue(value);
    }

    private Object wrapPrismValues(Collection<V> pvalues) {
        return pvalues == null ? NullValue.NULL_VALUE : pvalues.stream().map(this::wrapPrismValue).toList();
    }

    private Object wrapPrismValue(V pvalue) {
        return pvalue == null ? NullValue.NULL_VALUE :
                pvalue.getRealValue() == null ? NullValue.NULL_VALUE : CelTypeMapper.toCelValue(pvalue.getRealValue());
    }

    @Override
    public ItemDelta<V,D> getJavaValue() {
        return itemDelta;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        final ImmutableSet<String> fieldNames = ImmutableSet.of(F_PATH,
                F_VALUES_TO_ADD, F_VALUES_TO_DELETE, F_VALUES_TO_REPLACE, F_ESTIMATED_OLD_VALUES);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_PATH.equals(fieldName)) {
                return Optional.of(ItemPathCelValue.CEL_TYPE);
            } else if (F_VALUES_TO_ADD.equals(fieldName) || F_VALUES_TO_DELETE.equals(fieldName)
                    || F_VALUES_TO_REPLACE.equals(fieldName) || F_ESTIMATED_OLD_VALUES.equals(fieldName)) {
                return Optional.of(ListType.create(SimpleType.ANY));
            } else {
                throw new IllegalStateException("Illegal request for ItemDelta field " + fieldName);
            }
        };
        return StructType.create(ITEM_DELTA_PACKAGE_NAME, fieldNames, fieldResolver);
    }
}
