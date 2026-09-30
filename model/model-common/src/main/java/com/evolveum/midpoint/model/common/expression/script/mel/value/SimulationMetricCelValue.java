/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;

/**
 * CEL representation of a processed simulation metric.
 *
 * Exposes metric metadata while keeping its value opaque so the original
 * Java representation can pass through MEL unchanged.
 */
public class SimulationMetricCelValue extends AbstractStructuredCelValue<Object>
        implements MidPointValueProducer<ProcessedObject.Metric> {

    private static final String F_EVENT_MARK_REF = "eventMarkRef";
    private static final String F_ID = "id";
    private static final String F_SELECTED = "selected";
    private static final String F_VALUE = "value";
    public static final CelType CEL_TYPE = createCelType();

    private final ProcessedObject.Metric metric;

    private SimulationMetricCelValue(ProcessedObject.Metric metric) {
        this.metric = metric;
    }

    public static SimulationMetricCelValue create(ProcessedObject.Metric metric) {
        return new SimulationMetricCelValue(metric);
    }

    @Override
    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_EVENT_MARK_REF, metric.getEventMarkRef() != null
                ? ReferenceCelValue.create(metric.getEventMarkRef().asReferenceValue())
                : NullValue.NULL_VALUE);
        value.put(F_ID, metric.getId() != null ? metric.getId() : NullValue.NULL_VALUE);
        value.put(F_SELECTED, metric.isSelected());
        value.put(F_VALUE, metric.getValue() != null
                ? OpaqueJavaCelValue.create(metric.getValue())
                : NullValue.NULL_VALUE);
        return value;
    }

    @Override
    public ProcessedObject.Metric getJavaValue() {
        return metric;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        var fields = ImmutableSet.of(F_EVENT_MARK_REF, F_ID, F_SELECTED, F_VALUE);
        StructType.FieldResolver resolver = field -> Optional.of(switch (field) {
            case F_EVENT_MARK_REF -> NullableType.create(ReferenceCelValue.CEL_TYPE);
            case F_ID -> NullableType.create(SimpleType.STRING);
            case F_SELECTED -> SimpleType.BOOL;
            case F_VALUE -> NullableType.create(OpaqueJavaCelValue.CEL_TYPE);
            default -> throw new IllegalStateException("Illegal simulation metric field " + field);
        });
        return StructType.create(ProcessedObject.Metric.class.getCanonicalName(), fields, resolver);
    }
}
