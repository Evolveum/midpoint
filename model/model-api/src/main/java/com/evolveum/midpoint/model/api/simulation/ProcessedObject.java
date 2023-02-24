/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 */
public interface ProcessedObject<O extends ObjectType> extends DebugDumpable, Serializable {

    // TODO document all this!!!!!!!!!

    Map<ChangeType, ObjectProcessingStateType> DELTA_TO_PROCESSING_STATE =
            new ImmutableMap.Builder<ChangeType, ObjectProcessingStateType>()
                    .put(ChangeType.ADD, ObjectProcessingStateType.ADDED)
                    .put(ChangeType.DELETE, ObjectProcessingStateType.DELETED)
                    .put(ChangeType.MODIFY, ObjectProcessingStateType.MODIFIED)
                    .build();
    String getOid();
    @NotNull Class<O> getType();
    @Nullable PolyStringType getName();
    @NotNull ObjectProcessingStateType getState();
    @NotNull Collection<String> getMatchingEventMarks();
    @Nullable Map<String, MarkType> getEventMarksMap();

    default boolean isAddition() {
        return getState() == ObjectProcessingStateType.ADDED;
    }

    default boolean isModification() {
        return getState() == ObjectProcessingStateType.MODIFIED;
    }

    default boolean isDeletion() {
        return getState() == ObjectProcessingStateType.DELETED;
    }

    default boolean isNoChange() {
        return getState() == ObjectProcessingStateType.UNMODIFIED;
    }

    void setEventMarksMap(Map<String, MarkType> eventMarksMap);
    O getBefore();
    O getAfter();
    @Nullable ObjectDelta<O> getDelta();
    O getAfterOrBefore();

    boolean matches(@NotNull SimulationObjectPredicateType predicate, @NotNull Task task, @NotNull OperationResult result)
            throws CommonException;
    void resolveEventMarks(OperationResult result);
    boolean hasEventMark(@NotNull String eventMarkOid);
    boolean hasNoEventMarks();
    @Nullable String getResourceOid();

    default @NotNull Collection<ProcessedObjectItemDelta<?,?>> getItemDeltas() {
        return getItemDeltas(null, null, true);
    }

    @NotNull Collection<ProcessedObjectItemDelta<?,?>> getItemDeltas(
            @Nullable Object pathsToInclude, @Nullable Object pathsToExclude, @Nullable Boolean includeOperationalItems);

    interface ProcessedObjectItemDelta<V extends PrismValue, D extends ItemDefinition<?>> extends ItemDelta<V, D> {

        @NotNull Collection<?> getRealValuesBefore();
        @NotNull Collection<?> getRealValuesAfter();
        @NotNull Collection<?> getRealValuesAdded();
        @NotNull Collection<?> getRealValuesDeleted();
    }

    interface Factory {
        <O extends ObjectType> ProcessedObject<O> create(
                @Nullable O stateBefore,
                @Nullable ObjectDelta<O> simulatedDelta,
                @NotNull Collection<String> eventMarks) throws SchemaException;
    }
}
