/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;

/**
 * CEL representation of a processed simulation item delta.
 *
 * Exposes the item path, before/after and added/deleted real values,
 * and values with their simulation states.
 */
public class SimulationItemDeltaCelValue extends AbstractStructuredCelValue<Object>
        implements MidPointValueProducer<ProcessedObject.ProcessedObjectItemDelta<?, ?>> {

    private static final String F_PATH = "path";
    private static final String F_REAL_VALUES_BEFORE = "realValuesBefore";
    private static final String F_REAL_VALUES_AFTER = "realValuesAfter";
    private static final String F_REAL_VALUES_ADDED = "realValuesAdded";
    private static final String F_REAL_VALUES_DELETED = "realValuesDeleted";
    private static final String F_VALUES_WITH_STATES = "valuesWithStates";
    public static final CelType CEL_TYPE = createCelType();

    private final ProcessedObject.ProcessedObjectItemDelta<?, ?> itemDelta;

    private SimulationItemDeltaCelValue(ProcessedObject.ProcessedObjectItemDelta<?, ?> itemDelta) {
        this.itemDelta = itemDelta;
    }

    public static SimulationItemDeltaCelValue create(ProcessedObject.ProcessedObjectItemDelta<?, ?> itemDelta) {
        return new SimulationItemDeltaCelValue(itemDelta);
    }

    @Override
    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_PATH, itemDelta.getPath() != null
                ? ItemPathCelValue.create(itemDelta.getPath())
                : NullValue.NULL_VALUE);
        value.put(F_REAL_VALUES_BEFORE, wrapRealValues(itemDelta.getRealValuesBefore()));
        value.put(F_REAL_VALUES_AFTER, wrapRealValues(itemDelta.getRealValuesAfter()));
        value.put(F_REAL_VALUES_ADDED, wrapRealValues(itemDelta.getRealValuesAdded()));
        value.put(F_REAL_VALUES_DELETED, wrapRealValues(itemDelta.getRealValuesDeleted()));
        value.put(F_VALUES_WITH_STATES, itemDelta.getValuesWithStates().stream()
                .map(SimulationValueWithStateCelValue::create)
                .toList());
        return value;
    }

    private static List<Object> wrapRealValues(Collection<?> values) {
        return values.stream()
                .map(value -> value != null ? OpaqueJavaCelValue.create(value) : NullValue.NULL_VALUE)
                .map(Object.class::cast)
                .toList();
    }

    public ProcessedObject.ProcessedObjectItemDelta<?, ?> getItemDelta() {
        return itemDelta;
    }

    @Override
    public ProcessedObject.ProcessedObjectItemDelta<?, ?> getJavaValue() {
        return itemDelta;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        var fields = ImmutableSet.of(
                F_PATH,
                F_REAL_VALUES_BEFORE,
                F_REAL_VALUES_AFTER,
                F_REAL_VALUES_ADDED,
                F_REAL_VALUES_DELETED,
                F_VALUES_WITH_STATES);
        StructType.FieldResolver resolver = field -> Optional.of(switch (field) {
            case F_PATH -> NullableType.create(ItemPathCelValue.CEL_TYPE);
            case F_REAL_VALUES_BEFORE, F_REAL_VALUES_AFTER, F_REAL_VALUES_ADDED, F_REAL_VALUES_DELETED ->
                    ListType.create(NullableType.create(OpaqueJavaCelValue.CEL_TYPE));
            case F_VALUES_WITH_STATES -> ListType.create(SimulationValueWithStateCelValue.CEL_TYPE);
            default -> throw new IllegalStateException("Illegal simulation item delta field " + field);
        });
        return StructType.create(ProcessedObject.ProcessedObjectItemDelta.class.getCanonicalName(), fields, resolver);
    }
}
