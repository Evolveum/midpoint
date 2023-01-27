/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import com.evolveum.midpoint.prism.delta.ChangeType;
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

import java.util.Collection;
import java.util.Map;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 */
public interface ProcessedObject<O extends ObjectType> extends DebugDumpable {

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
    @NotNull Collection<String> getEventTags();
    @Nullable Map<String, TagType> getEventTagsMap();

    void setEventTagsMap(Map<String, TagType> eventTagsMap);
    O getBefore();
    O getAfter();
    @Nullable ObjectDelta<O> getDelta();
    O getAfterOrBefore();
    boolean matches(SimulationObjectPredicateType predicate, Task task, OperationResult result)
            throws CommonException;

    interface Factory {
        <O extends ObjectType> ProcessedObject<O> create(
                @Nullable O stateBefore,
                @Nullable ObjectDelta<O> simulatedDelta,
                @NotNull Collection<String> eventTags) throws SchemaException;
    }
}
