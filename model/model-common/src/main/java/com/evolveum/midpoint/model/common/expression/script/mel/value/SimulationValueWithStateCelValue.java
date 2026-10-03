/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;

/**
 * CEL representation of a simulation value together with its change state.
 *
 * Keeps the original value opaque while exposing the simulation state to MEL.
 */
public class SimulationValueWithStateCelValue extends AbstractStructuredCelValue<Object>
        implements MidPointValueProducer<ProcessedObject.ValueWithState> {

    private static final String F_STATE = "state";
    private static final String F_VALUE = "value";
    public static final CelType CEL_TYPE = createCelType();

    private final ProcessedObject.ValueWithState valueWithState;

    private SimulationValueWithStateCelValue(ProcessedObject.ValueWithState valueWithState) {
        this.valueWithState = valueWithState;
    }

    public static SimulationValueWithStateCelValue create(ProcessedObject.ValueWithState valueWithState) {
        return new SimulationValueWithStateCelValue(valueWithState);
    }

    @Override
    protected Map<String, Object> createMapValue() {
        return Map.of(
                F_STATE, valueWithState.getState().name(),
                F_VALUE, PrismCelValue.create(valueWithState.getPrismValue()));
    }

    @Override
    public ProcessedObject.ValueWithState getJavaValue() {
        return valueWithState;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        var fields = ImmutableSet.of(F_STATE, F_VALUE);
        StructType.FieldResolver resolver = field -> Optional.of(switch (field) {
            case F_STATE -> SimpleType.STRING;
            case F_VALUE -> PrismCelValue.CEL_TYPE;
            default -> throw new IllegalStateException("Illegal simulation value-with-state field " + field);
        });
        return StructType.create(ProcessedObject.ValueWithState.class.getCanonicalName(), fields, resolver);
    }
}
