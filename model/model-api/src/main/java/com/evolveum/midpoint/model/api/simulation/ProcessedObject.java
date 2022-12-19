/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 *
 * TEMPORARY
 */
public class ProcessedObject<O extends ObjectType> {

    private static final Map<ChangeType, ObjectProcessingStateType> DELTA_TO_PROCESSING_STATE =
            new ImmutableMap.Builder<ChangeType, ObjectProcessingStateType>()
                    .put(ChangeType.ADD, ObjectProcessingStateType.ADDED)
                    .put(ChangeType.DELETE, ObjectProcessingStateType.DELETED)
                    .put(ChangeType.MODIFY, ObjectProcessingStateType.MODIFIED)
                    .build();

    private final String oid; // TODO may be null?
    @NotNull private final Class<O> type;
    private final PolyStringType name;
    @NotNull private final ObjectProcessingStateType state;
    @NotNull private final Collection<String> eventTags;
    @Nullable private final O before;
    @Nullable private final O after;
    @Nullable private final ObjectDelta<O> delta;

    private ProcessedObject(
            String oid,
            @NotNull Class<O> type,
            PolyStringType name,
            @NotNull ObjectProcessingStateType state,
            @NotNull Collection<String> eventTags,
            @Nullable O before,
            @Nullable O after,
            @Nullable ObjectDelta<O> delta) {
        this.oid = oid;
        this.type = type;
        this.name = name;
        this.state = state;
        this.eventTags = eventTags;
        this.before = before;
        this.after = after;
        this.delta = delta;
    }

    public static <O extends ObjectType>  ProcessedObject<O> parse(@NotNull SimulationResultProcessedObjectType bean)
            throws SchemaException {
        Class<?> type = PrismContext.get().getSchemaRegistry().determineClassForTypeRequired(bean.getType());
        argCheck(ObjectType.class.isAssignableFrom(type), "Type is not an ObjectType: %s", type);
        //noinspection unchecked
        return new ProcessedObject<>(
                bean.getOid(),
                (Class<O>) type,
                bean.getName(),
                MiscUtil.argNonNull(bean.getState(), () -> "No processing state in " + bean),
                bean.getMetricIdentifier(),
                (O) bean.getBefore(),
                (O) bean.getAfter(),
                DeltaConvertor.createObjectDelta(bean.getDelta()));
    }

    public static <O extends ObjectType> ProcessedObject<?> create(
            O stateBefore, ObjectDelta<O> delta, Collection<String> eventTags) throws SchemaException {

        Class<O> type;
        if (stateBefore != null) {
            //noinspection unchecked
            type = (Class<O>) stateBefore.getClass();
        } else if (delta != null) {
            type = delta.getObjectTypeClass();
        } else {
            return null;
        }

        @Nullable O stateAfter = computeStateAfter(stateBefore, delta);
        @Nullable O anyState = MiscUtil.getFirstNonNull(stateAfter, stateBefore);

        // We may consider returning null if anyState is null (meaning that the delta is MODIFY/DELETE with null stateBefore)

        return new ProcessedObject<>(
                determineOid(anyState, delta),
                type,
                anyState != null ? anyState.getName() : null,
                delta != null ?
                        DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()) :
                        ObjectProcessingStateType.UNMODIFIED,
                eventTags,
                stateBefore,
                stateAfter,
                delta);
    }

    public String getOid() {
        return oid;
    }

    public @NotNull Class<O> getType() {
        return type;
    }

    public @NotNull ObjectProcessingStateType getState() {
        return state;
    }

    public @NotNull Collection<String> getEventTags() {
        return eventTags;
    }

    @Nullable
    public O getBefore() {
        return before;
    }

    @Nullable
    public O getAfter() {
        return after;
    }

    @Nullable
    public ObjectDelta<O> getDelta() {
        return delta;
    }

    private static <O extends ObjectType> O computeStateAfter(O stateBefore, ObjectDelta<O> delta) throws SchemaException {
        if (stateBefore == null) {
            if (delta == null) {
                return null;
            } else if (delta.isAdd()) {
                return delta.getObjectToAdd().asObjectable();
            } else {
                // We may relax this before release - we may still store the delta
                throw new IllegalStateException("No initial state and MODIFY/DELETE delta? Delta: " + delta);
            }
        } else if (delta != null) {
            //noinspection unchecked
            PrismObject<O> clone = (PrismObject<O>) stateBefore.asPrismObject().clone();
            delta.applyTo(clone);
            return clone.asObjectable();
        } else {
            return stateBefore;
        }
    }

    private static <O extends ObjectType> @Nullable String determineOid(O anyState, ObjectDelta<O> delta) {
        if (anyState != null) {
            String oid = anyState.getOid();
            if (oid != null) {
                return oid;
            }
        }
        if (delta != null) {
            return delta.getOid();
        }
        return null;
    }

    public SimulationResultProcessedObjectType toBean() throws SchemaException {
        SimulationResultProcessedObjectType processedObject = new SimulationResultProcessedObjectType()
                .oid(oid)
                .type(
                        PrismContext.get().getSchemaRegistry().determineTypeForClass(type))
                .name(name)
                .state(state)
                .before(before != null ? before.clone() : null)
                .after(after != null ? after.clone() : null)
                .delta(DeltaConvertor.toObjectDeltaType(delta));
        processedObject.getMetricIdentifier().addAll(eventTags);
        return processedObject;
    }
}
