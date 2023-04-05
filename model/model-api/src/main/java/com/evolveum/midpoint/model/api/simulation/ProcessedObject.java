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
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.delta.ItemDeltaFilter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 *
 * Used during creation of {@link SimulationResultType} objects, reporting on them, and during testing.
 */
public interface ProcessedObject<O extends ObjectType> extends DebugDumpable, Serializable {

    Map<ChangeType, ObjectProcessingStateType> DELTA_TO_PROCESSING_STATE =
            new ImmutableMap.Builder<ChangeType, ObjectProcessingStateType>()
                    .put(ChangeType.ADD, ObjectProcessingStateType.ADDED)
                    .put(ChangeType.DELETE, ObjectProcessingStateType.DELETED)
                    .put(ChangeType.MODIFY, ObjectProcessingStateType.MODIFIED)
                    .build();

    /**
     * OID of the object whole processing is described by this record. Usually not null but we shouldn't rely on it.
     *
     * @see SimulationResultProcessedObjectType#getOid()
     */
    String getOid();

    /**
     * Type of the object being processed.
     *
     * @see SimulationResultProcessedObjectType#getType()
     */
    @NotNull Class<O> getType();

    /**
     * Name of the object being processed. May not be known; typically for shadows to-be-created.
     *
     * @see SimulationResultProcessedObjectType#getName()
     */
    @Nullable PolyStringType getName();

    /**
     * State of the object.
     *
     * @see #DELTA_TO_PROCESSING_STATE
     * @see SimulationResultProcessedObjectType#getState()
     */
    @NotNull ObjectProcessingStateType getState();

    /**
     * The status of the operation connected to this object.
     *
     * Only "primary" objects have this information.
     *
     * @see SimulationResultProcessedObjectType#getResultStatus()
     */
    @Nullable OperationResultStatus getResultStatus();

    /**
     * The result of the operation connected to this object.
     *
     * Only "primary" objects have this information.
     *
     * @see SimulationResultProcessedObjectType#getResult()
     */
    @Nullable OperationResult getResult();

    /** Returns OIDs of matching event marks for this object. */
    @NotNull Collection<String> getMatchingEventMarksOids();

    /** Returns references of object marks. Primarily on "object before" state (if exists). To be reconsidered. */
    @NotNull Collection<ObjectReferenceType> getEffectiveObjectMarksRefs();

    /**
     * Returns the state of the object before the operation.
     *
     * @see SimulationResultProcessedObjectType#getBefore()
     */
    O getBefore();

    /**
     * Returns the (expected) state of the object after the operation.
     *
     * @see SimulationResultProcessedObjectType#getAfter()
     */
    O getAfter();

    /**
     * Returns the operation that is to be executed.
     *
     * @see SimulationResultProcessedObjectType#getDelta()
     */
    @Nullable ObjectDelta<O> getDelta();

    /**
     * Resolves "estimated old values" for item deltas where they are not known.
     *
     * TEMPORARY; see MID-8536. This will be part of standard processing but because of code freeze doing it as separate code.
     */
    void fixEstimatedOldValuesInDelta();

    default O getAfterOrBefore() {
        return MiscUtil.getFirstNonNull(getAfter(), getBefore());
    }

    default @NotNull O getBeforeOrAfterRequired() {
        return MiscUtil.requireNonNull(
                MiscUtil.getFirstNonNull(getBefore(), getAfter()),
                () -> new IllegalStateException("No object in " + this));
    }

    /**
     * Creates a {@link SimulationResultProcessedObjectType} bean corresponding to this object.
     */
    @NotNull SimulationResultProcessedObjectType toBean();

    /** For diagnostic purposes. */
    @VisibleForTesting
    void resolveEventMarks(OperationResult result);

    @VisibleForTesting
    boolean hasEventMark(@NotNull String eventMarkOid);

    @VisibleForTesting
    boolean hasNoEventMarks();

    /** For processed objects that are shadows: returns the related resource OID. */
    @VisibleForTesting
    @Nullable String getResourceOid();

    /**
     * Returns the collection of (augmented) item deltas related to this "processed object".
     *
     * Limited! We are not able to see inside deltas. So, for example, when looking for `activation/administrativeStatus`
     * property, and the whole `activation` container is added, the delta is not shown.
     *
     * See the implementation and {@link ItemDeltaFilter} for details. Experimental.
     *
     * @param pathsToInclude paths of items that we want to see in the list of deltas
     * @param pathsToExclude paths of items that we do not want to see in the list of deltas
     * @param includeOperationalItems should the operational items be included in the list of deltas?
     */
    @NotNull Collection<ProcessedObjectItemDelta<?,?>> getItemDeltas(
            @Nullable Object pathsToInclude, @Nullable Object pathsToExclude, @Nullable Boolean includeOperationalItems);

    /**
     * Returns the collection of {@link Metric} values for this object.
     *
     * @param showEventMarks Should we include mark-based metrics?
     * @param showExplicitMetrics Should we include explicitly defined metrics?
     */
    @NotNull Collection<Metric> getMetrics(@Nullable Boolean showEventMarks, @Nullable Boolean showExplicitMetrics);

    /**
     * Applies the definitions (currently, resource schema related to specific shadow) to the object(s) before/after,
     * and the delta.
     */
    void applyDefinitions(@NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException;

    @SuppressWarnings("unused") // used by scripts
    default boolean isAddition() {
        return getState() == ObjectProcessingStateType.ADDED;
    }

    @SuppressWarnings("unused") // used by scripts
    default boolean isModification() {
        return getState() == ObjectProcessingStateType.MODIFIED;
    }

    @SuppressWarnings("unused") // used by scripts
    default boolean isDeletion() {
        return getState() == ObjectProcessingStateType.DELETED;
    }

    @SuppressWarnings("unused") // used by scripts
    default boolean isNoChange() {
        return getState() == ObjectProcessingStateType.UNMODIFIED;
    }

    @SuppressWarnings("unused") // used in scripts
    boolean isFocus();

    @SuppressWarnings("unused") // used in scripts
    default boolean isOfFocusType() {
        return FocusType.class.isAssignableFrom(getType());
    }

    @SuppressWarnings("unused") // used in scripts
    default boolean isShadow() {
        return ShadowType.class.isAssignableFrom(getType());
    }

    /**
     * {@link ItemDelta} augmented with functionality needed to display it in a complex way, for example,
     * with the information on real change(s) to the object.
     *
     * Highly experimental, and currently not very sound. For example, the {@link #getRealValuesAfter()} or
     * {@link #getRealValuesModified()} may return values modified not by this delta. (Like when having `assignment`
     * ADD/DELETE delta accompanied with a couple of `assignment/[1]/activation/...` deltas.)
     */
    @SuppressWarnings("unused") // used by the reports
    @Experimental
    interface ProcessedObjectItemDelta<V extends PrismValue, D extends ItemDefinition<?>> extends ItemDelta<V, D> {
        /** Real values of the corresponding item before execution of this delta. */
        @NotNull Collection<?> getRealValuesBefore();
        /** Prism values of the corresponding item before execution of this delta. */
        @NotNull Set<? extends PrismValue> getPrismValuesBefore();
        /** Real values of the corresponding item after execution of this delta; beware - may contain results of related deltas */
        @NotNull Collection<?> getRealValuesAfter();
        /** Prism values of the corresponding item after execution of this delta; beware - may contain results of related deltas */
        @NotNull Set<? extends PrismValue> getPrismValuesAfter();
        /** Real values added by this delta. (Phantom ones are filtered out.) */
        @NotNull Collection<?> getRealValuesAdded();
        /** Real values deleted by this delta. (Phantom ones are filtered out.) */
        @NotNull Collection<?> getRealValuesDeleted();
        /** Real values modified by this delta - their identity is known by PCV ID. */
        @NotNull Set<?> getRealValuesModified();
        /** Real values unchanged by this delta. */
        @NotNull Collection<?> getRealValuesUnchanged();
        /** All values (added, deleted, modified) with the corresponding state. Modified values returned only for REPLACE deltas. */
        @NotNull Collection<ValueWithState> getValuesWithStates();
        /** The state of the assignment pointed to by the delta of `assignment/[nn]/...` kind. */
        @Nullable AssignmentType getRelatedAssignment();
    }

    /** Value touched by a delta, along with their processing {@link State}. */
    @Experimental
    class ValueWithState implements Serializable {

        /** We hope this object is serializable, as it originated in a delta (which is serializable). */
        @NotNull private final Object value;

        @NotNull private final State state;

        public ValueWithState(@NotNull Object value, @NotNull State state) {
            this.value = value;
            this.state = state;
        }

        public @NotNull Object getValue() {
            return value;
        }

        public @NotNull State getState() {
            return state;
        }

        @Override
        public String toString() {
            return String.format("'%s' (%s)", value, state);
        }

        public enum State {
            UNCHANGED, ADDED, DELETED, MODIFIED
        }
    }

    /** Metric information, currently for reporting purposes. */
    interface Metric extends Serializable {

        /** Reference to event mark (for event mark based metrics). */
        @Nullable ObjectReferenceType getEventMarkRef();

        /** String identifier (for explicit metrics). */
        @Nullable String getId();

        /** Is this object selected with regards to given metric? */
        boolean isSelected();

        /** Value of given metric for this object. */
        BigDecimal getValue();
    }
}
