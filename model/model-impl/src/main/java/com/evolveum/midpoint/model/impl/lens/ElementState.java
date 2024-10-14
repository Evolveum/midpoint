/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.internals.ThreadLocalOperationsMonitor.OperationExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.cid.ContainerValueIdGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.evolveum.midpoint.prism.delta.ObjectDelta.isAdd;
import static com.evolveum.midpoint.schema.internals.ThreadLocalOperationsMonitor.recordEndEmbedded;
import static com.evolveum.midpoint.schema.internals.ThreadLocalOperationsMonitor.recordStartEmbedded;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationType.*;

/**
 * Encapsulates the maintenance of the state of an element (focus, projection).
 *
 * Holds mainly:
 *
 *  - basic information like type, definition, and OID
 *  - state: old, current, new
 *  - deltas: primary, secondary, current (i.e. current -> new), summary (i.e. old -> new)
 *  - archived secondary deltas
 *  - flags related to the validity of computed components (see below), to avoid needless recomputation
 *  - various other state flags (see in the description)
 *
 * Implementation notes:
 *
 * 1. Computed components (current, summary delta, adjusted current object, new object) are evaluated lazily.
 * Their validity is indicated by appropriate flags, driving their (re)computation.
 *
 * 2. Because of their complexity and interdependence, state-changing operations
 * are grouped in a separate region: State updates.
 *
 * Intentionally not public.
 */
class ElementState<O extends ObjectType> implements Serializable, Cloneable {

    private static final Trace LOGGER = TraceManager.getTrace(ElementState.class);

    /**
     * Type of object represented by this context.
     */
    @NotNull private final Class<O> objectTypeClass;

    /**
     * Definition of the object. Lazily evaluated.
     */
    private PrismObjectDefinition<O> objectDefinition;

    /**
     * OID of object represented by this context.
     *
     * Beware, it can change during the operation e.g. when handling ObjectNotFound exceptions
     * (or on many other occasions).
     */
    private String oid;

    /**
     * "Old" state of the object i.e. the one that was present when the clockwork started.
     * It can be present on the beginning or filled-in during projector execution by the context loader.
     *
     * It is used as an "old state" for resource object mappings (in constructions or resources),
     * persona mappings, notifications, policy rules, and so on.
     */
    private PrismObject<O> oldObject;

    /**
     * "Current" state of the object i.e. the one that was present when the current projection
     * started. (I.e. when the Projector was entered.)
     *
     * It is typically filled-in by the context loader.
     *
     * It is used as an "old state" for focus mappings (in object template or assigned ones).
     */
    private PrismObject<O> currentObject;

    /** See {@link LensProjectionContext#getStateBeforeSimulatedOperation()} for explanation. */
    private PrismObject<O> currentShadowBeforeSimulatedDeltaExecution;

    /** Number of simulated executions carried out. Should be 0-1 for projections, 0-N for focus. */
    private int simulatedExecutions;

    /**
     * Adjusted current state of the object. It is used as a workaround for projection context
     * computations, where we need coordinates in the new object even if the current delta is (not yet) present.
     *
     * Therefore we create an adjusted current object, and base new object computation on it.
     * We also use it in object-delta-object structures.
     */
    private PrismObject<O> adjustedCurrentObject;

    /**
     * Expected state of the object after application of currentDelta i.e. item deltas computed
     * during current projection.
     */
    private PrismObject<O> newObject;

    /**
     * Indicates that the objectOld and objectCurrent contain relevant values.
     * Generally set to true by {@link ContextLoader}
     * and reset to false by {@link LensContext#rot(String)} and {@link LensContext#rotAfterExecution()} methods.
     *
     * FIXME For projections, this flag is misused: some shadows are marked as fresh, even if they are clearly out of date.
     *  This causes problems especially when shadow caching is enabled. The {@link LensProjectionContext#reloadNeeded}
     *  was created as a workaround. MID-9944.
     *
     * @see LensContext#isFresh
     */
    private boolean fresh;

    /**
     * Primary delta i.e. one that the caller specified that has to be executed.
     *
     * Sometimes it is also cleared e.g. in `ConsolidationProcessor`. TODO is this ok?
     */
    private ObjectDelta<O> primaryDelta;

    /**
     * Secondary delta for current projection/execution wave.
     * For focus, it is archived and cleared after execution.
     *
     * Currently, it is always a MODIFY delta.
     */
    private ObjectDelta<O> secondaryDelta;

    /**
     * Archived secondary deltas.
     *
     * TODO better description
     */
    @NotNull private final ObjectDeltaWaves<O> archivedSecondaryDeltas;

    /**
     * Delta that moves {@link #currentObject} to {@link #newObject}.
     *
     * WARNING: But not always! For projections, it is possible that {@link #currentObject} is `null` but this delta is `MODIFY`.
     * It is because we have no place to put the "add" information to, if it's computed by midPoint: it should not go to the
     * primary delta (as it is, in fact, not primary ~ user requested), and the secondary delta is currently always "modify" one.
     * *TODO this should be fixed somehow*
     */
    private ObjectDelta<O> currentDelta;

    /**
     * Delta that moves {@link #oldObject} to {@link #newObject}.
     *
     * *TODO for projections being added or deleted, it can be misleading, see {@link #currentDelta}*
     */
    private ObjectDelta<O> summaryDelta;

    /** Creates adjusted current object. (Currently used for projection contexts.) */
    @NotNull private final CurrentObjectAdjuster<O> currentObjectAdjuster;

    /** Refines "raw" object definition. (Currently used for projection contexts.) */
    @NotNull private final ObjectDefinitionRefiner<O> objectDefinitionRefiner;

    /** Is the current delta valid? */
    private boolean currentDeltaValid;

    /** Is the summary delta valid? */
    private boolean summaryDeltaValid;

    /** Is the adjusted current object valid? */
    private boolean adjustedCurrentObjectValid;

    /** Is the new object valid? */
    private boolean newObjectValid;

    /** Was the primary delta executed? If so, it won't be taken into account when (re)computing the current delta. */
    private boolean wasPrimaryDeltaExecuted;

    ElementState(@NotNull Class<O> objectTypeClass,
            @NotNull CurrentObjectAdjuster<O> currentObjectAdjuster,
            @NotNull ObjectDefinitionRefiner<O> objectDefinitionRefiner) {
        this.objectTypeClass = objectTypeClass;
        this.currentObjectAdjuster = currentObjectAdjuster;
        this.objectDefinitionRefiner = objectDefinitionRefiner;
        this.archivedSecondaryDeltas = new ObjectDeltaWaves<>();
    }

    @SuppressWarnings("CopyConstructorMissesField")
    private ElementState(@NotNull ElementState<O> other) {
        this.objectTypeClass = other.objectTypeClass;
        this.currentObjectAdjuster = other.currentObjectAdjuster;
        this.objectDefinitionRefiner = other.objectDefinitionRefiner;
        this.oid = other.oid;
        this.oldObject = CloneUtil.clone(other.oldObject);
        this.currentObject = CloneUtil.clone(other.currentObject);
        this.primaryDelta = CloneUtil.clone(other.primaryDelta);
        this.secondaryDelta = CloneUtil.clone(other.secondaryDelta);
        this.fresh = other.fresh;
        this.wasPrimaryDeltaExecuted = other.wasPrimaryDeltaExecuted;
        // computed components and validity flags do not need to be copied
        this.archivedSecondaryDeltas = other.archivedSecondaryDeltas.clone();
    }

    //region Type and definition
    @NotNull Class<O> getObjectTypeClass() {
        return objectTypeClass;
    }

    boolean isOfType(Class<?> aClass) {
        if (aClass.isAssignableFrom(objectTypeClass)) {
            return true;
        }
        PrismObject<O> object = getNewOrCurrentOrOld();
        return object != null && aClass.isAssignableFrom(object.asObjectable().getClass());
    }

    @NotNull PrismObjectDefinition<O> getObjectDefinition() {
        if (objectDefinition == null) {
            PrismObjectDefinition<O> rawDefinition = null;
            if (oldObject != null) {
                rawDefinition = oldObject.getDefinition();
            }
            if (rawDefinition == null && currentObject != null) {
                rawDefinition = currentObject.getDefinition();
            }
            if (rawDefinition == null && newObject != null) {
                rawDefinition = newObject.getDefinition();
            }
            if (rawDefinition == null) {
                rawDefinition = Objects.requireNonNull(
                        PrismContext.get().getSchemaRegistry()
                                .findObjectDefinitionByCompileTimeClass(getObjectTypeClass()),
                        () -> "No definition for " + getObjectTypeClass());
            }
            objectDefinition = objectDefinitionRefiner.refine(rawDefinition);
        }
        return objectDefinition;
    }
    //endregion

    //region OID
    public String getOid() {
        if (oid == null) {
            oid = determineOid();
        }
        return oid;
    }

    private String determineOid() {
        if (getOldObject() != null && getOldObject().getOid() != null) {
            return getOldObject().getOid();
        }
        if (getCurrentObject() != null && getCurrentObject().getOid() != null) {
            return getCurrentObject().getOid();
        }
        if (getNewObject() != null && getNewObject().getOid() != null) {
            return getNewObject().getOid();
        }
        if (getPrimaryDelta() != null && getPrimaryDelta().getOid() != null) {
            return getPrimaryDelta().getOid();
        }
        return null;
    }
    //endregion

    //region Old, current, adjusted current, new, and "any" object state - without computations
    PrismObject<O> getOldObject() {
        return oldObject;
    }

    boolean hasOldObject() {
        return oldObject != null;
    }

    PrismObject<O> getCurrentObject() {
        return currentObject;
    }

    /** See {@link LensProjectionContext#getStateBeforeSimulatedOperation()} for explanation. Only for shadows. */
    PrismObject<O> getCurrentShadowBeforeSimulatedDeltaExecution() {
        return simulatedExecutions > 0 ? currentShadowBeforeSimulatedDeltaExecution : currentObject;
    }

    private PrismObject<O> getAdjustedCurrentObject() throws SchemaException, ConfigurationException {
        if (!adjustedCurrentObjectValid) {
            adjustedCurrentObject = currentObjectAdjuster.adjust(currentObject);
            adjustedCurrentObjectValid = true;
        }
        return adjustedCurrentObject;
    }

    @Nullable PrismObject<O> getNewOrCurrentOrOld() {
        PrismObject<O> newObject = getNewObject();
        if (newObject != null) {
            return newObject;
        } else if (currentObject != null) {
            return currentObject;
        } else {
            return oldObject;
        }
    }

    @Nullable PrismObject<O> getCurrentOrNewObject() {
        if (currentObject != null) {
            return currentObject;
        } else {
            return getNewObject();
        }
    }

    @NotNull PrismObject<O> getNewOrCurrentObjectRequired() {
        PrismObject<O> newObjectLocal = getNewObject();
        if (newObjectLocal != null) {
            return newObjectLocal;
        } else if (currentObject != null) {
            return currentObject;
        } else {
            throw new IllegalStateException("Both object new and object current are null");
        }
    }

    PrismObject<O> getCurrentOrOldObject() {
        return currentObject != null ? currentObject : oldObject;
    }

    public PrismObject<O> getNewObject() {
        if (!newObjectValid) {
            newObject = computeNewObject();
            newObjectValid = true;
        }
        return newObject;
    }
    //endregion

    //region Deltas (without computations)
    ObjectDelta<O> getPrimaryDelta() {
        return primaryDelta;
    }

    boolean hasPrimaryDelta() {
        return !ObjectDelta.isEmpty(primaryDelta);
    }

    ObjectDelta<O> getSecondaryDelta() {
        return secondaryDelta;
    }

    boolean hasSecondaryDelta() {
        return !ObjectDelta.isEmpty(secondaryDelta);
    }

    public ObjectDelta<O> getCurrentDelta() {
        if (!currentDeltaValid) {
            currentDelta = computeCurrentDelta();
            currentDeltaValid = true;
        }
        return currentDelta;
    }

    public ObjectDelta<O> getSummaryDelta() {
        if (!summaryDeltaValid) {
            summaryDelta = computeSummaryDelta();
            summaryDeltaValid = true;
        }
        return summaryDelta;
    }

    @NotNull ObjectDeltaWaves<O> getArchivedSecondaryDeltas() {
        return archivedSecondaryDeltas;
    }
    //endregion

    //region Computations
    private ObjectDelta<O> computeCurrentDelta() {
        OperationExecution execution = recordStartEmbedded(CURRENT_DELTA_COMPUTATION);
        try {
            if (wasPrimaryDeltaExecuted) {
                return secondaryDelta;
            } else {
                try {
                    return ObjectDeltaCollectionsUtil.union(primaryDelta, secondaryDelta);
                } catch (SchemaException e) {
                    throw new SystemException("Unexpected schema exception while merging deltas: " + e.getMessage(), e);
                }
            }
        } finally {
            recordEndEmbedded(execution);
        }
    }

    private ObjectDelta<O> computeSummaryDelta() {
        OperationExecution execution = recordStartEmbedded(SUMMARY_DELTA_COMPUTATION);
        try {
            List<ObjectDelta<O>> allDeltas = new ArrayList<>();
            CollectionUtils.addIgnoreNull(allDeltas, primaryDelta);
            collectSecondaryDeltas(allDeltas);
            return ObjectDeltaCollectionsUtil.summarize(allDeltas);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while merging deltas: " + e.getMessage(), e);
        } finally {
            recordEndEmbedded(execution);
        }
    }

    private void collectSecondaryDeltas(List<ObjectDelta<O>> allDeltas) {
        for (ObjectDelta<O> archivedSecondaryDelta : archivedSecondaryDeltas) {
            CollectionUtils.addIgnoreNull(allDeltas, archivedSecondaryDelta);
        }
        CollectionUtils.addIgnoreNull(allDeltas, secondaryDelta);
    }

    ObjectDelta<O> getSummarySecondaryDelta() {
        try {
            List<ObjectDelta<O>> allSecondaryDeltas = new ArrayList<>();
            collectSecondaryDeltas(allSecondaryDeltas);
            return ObjectDeltaCollectionsUtil.summarize(allSecondaryDeltas);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while merging secondary deltas: " + e.getMessage(), e);
        }
    }

    private PrismObject<O> computeNewObject() {
        OperationExecution execution = recordStartEmbedded(NEW_OBJECT_COMPUTATION);
        try {
            PrismObject<O> adjustedCurrentObject = getAdjustedCurrentObject();
            ObjectDelta<O> currentDelta = getCurrentDelta();

            LOGGER.trace("compute new object: adjusted current = {}, current delta = {}", adjustedCurrentObject, currentDelta);
            return applyDeltaToObject(currentDelta, adjustedCurrentObject);
        } catch (ConfigurationException | SchemaException e) {
            // FIXME This is temporary. We should propagate this exception upwards. (But probably not to all getNewObject calls.)
            throw new SystemException("Unexpected schema exception when computing new object: " + e.getMessage(), e);
        } finally {
            recordEndEmbedded(execution);
        }
    }

    private PrismObject<O> applyDeltaToObject(ObjectDelta<O> delta, PrismObject<O> object) throws SchemaException {
        if (ObjectDelta.isEmpty(delta)) {
            return object;
        } else {
            return delta.computeChangedObject(object);
        }
    }

    /**
     * Computes the new object without relying on current object adjuster.
     *
     * The only use for this method is to avoid endless loop between new object computation and the adjuster that relies
     * on the new object value.
     */
    PrismObject<O> computeUnadjustedNewObject() throws SchemaException {
        ObjectDelta<O> currentDelta = getCurrentDelta();
        LOGGER.trace("compute unadjusted new object: current object = {}, current delta = {}", currentObject, currentDelta);
        return applyDeltaToObject(currentDelta, currentObject);
    }

    //endregion

    //region Invalidations
    /**
     * Invalidates all computed values. Called e.g. when parameters for current object adjuster change.
     */
    void invalidate() {
        invalidateCurrentDelta();
        invalidateSummaryDelta();
        invalidateAdjustedCurrentObject();
        invalidateNewObject();
    }

    private void invalidateCurrentObjectDependencies() {
        invalidateAdjustedCurrentObject();
        invalidateNewObject();
        // current and summary deltas are not related
    }

    private void invalidatePrimaryDeltaDependencies() {
        invalidateCurrentDelta();
        invalidateSummaryDelta();
        invalidateNewObject();
        // adjusted current object is not related
    }

    private void invalidateSecondaryDeltaDependencies() {
        invalidateCurrentDelta();
        invalidateSummaryDelta();
        invalidateAdjustedCurrentObject(); // It depends on the delta.
        invalidateNewObject();
    }

    private void invalidateOldObjectDependencies() {
        // This is really tricky. Technically, neither adjusted current object, new object, nor any of the deltas
        // (current, summary) depend on the old object.
        //
        // Even the new object does not: it is computed from the current object and current delta.
        // Neither of which are affected by changing the old object.
    }

    private void invalidateAdjustedCurrentObject() {
        adjustedCurrentObject = null;
        adjustedCurrentObjectValid = false;
    }

    private void invalidateNewObject() {
        newObject = null;
        newObjectValid = false;
    }

    private void invalidateCurrentDelta() {
        currentDelta = null;
        currentDeltaValid = false;
    }

    private void invalidateSummaryDelta() {
        summaryDelta = null;
        summaryDeltaValid = false;
    }
    //endregion

    //region Object-delta-objects
    ObjectDeltaObject<O> getRelativeObjectDeltaObject() throws SchemaException, ConfigurationException {
        return new ObjectDeltaObject<>(
                getAdjustedCurrentObject(),
                getCurrentDelta(),
                getNewObject(),
                getObjectDefinition());
    }

    @SuppressWarnings("unused") // TODO why not used?
    public ObjectDeltaObject<O> getAbsoluteObjectDeltaObject() {
        // We assume that current object + current delta = old object + summary delta.
        return new ObjectDeltaObject<>(
                oldObject,
                getSummaryDelta(),
                getNewObject(),
                getObjectDefinition());
    }
    //endregion

    //region Simple delta updates
    public void swallowToSecondaryDelta(LensElementContext<?> context, ItemDelta<?, ?> itemDelta) throws SchemaException {
        if (ItemDelta.isEmpty(itemDelta)) {
            return;
        }

        LOGGER.trace("Going to swallow to secondary delta (for {}):\n{}", context, itemDelta.debugDumpLazily(1));

        // TODO change this "contains modification" check (but how?)
        if (isDeltaAlreadyPresent(itemDelta)) {
            return;
        }

        LensUtil.setDeltaOldValue(context, itemDelta);

        if (secondaryDelta == null) {
            secondaryDelta = createEmptyDelta();
        }
        secondaryDelta.swallow(itemDelta);

        // TODO directly add to current and summary deltas and object new
        invalidateSecondaryDeltaDependencies();
    }

    private boolean isDeltaAlreadyPresent(ItemDelta<?, ?> itemDelta) {
        // This gradual check (primary then secondary) is there to avoid the computation of the current delta
        return !wasPrimaryDeltaExecuted && isDeltaPresentIn(itemDelta, primaryDelta)
                || isDeltaPresentIn(itemDelta, secondaryDelta);
    }

    private boolean isDeltaPresentIn(ItemDelta<?, ?> itemDelta, ObjectDelta<O> objectDelta) {
        return objectDelta != null
                && objectDelta.containsModification(itemDelta, EquivalenceStrategy.DATA.exceptForValueMetadata());
    }

    /** Creates the delta if needed. */
    void modifyPrimaryDelta(LensElementContext.DeltaModifier<O> modifier) throws SchemaException {
        if (primaryDelta == null) {
            primaryDelta = createEmptyDelta();
        }

        if (primaryDelta.isImmutable()) {
            primaryDelta = primaryDelta.clone();
            modifier.modify(primaryDelta);
            primaryDelta.freeze();
        } else {
            modifier.modify(primaryDelta);
        }

        invalidatePrimaryDeltaDependencies();
    }

    void modifySecondaryDelta(LensElementContext.DeltaModifier<O> modifier) throws SchemaException {
        if (secondaryDelta == null) {
            secondaryDelta = createEmptyDelta();
        }
        modifier.modify(secondaryDelta);
        invalidateSecondaryDeltaDependencies();
    }

    void deleteEmptyPrimaryDelta() {
        if (ObjectDelta.isModify(primaryDelta) && primaryDelta.isEmpty()) {
            primaryDelta = null; // no need to invalidate anything
        }
    }

    private ObjectDelta<O> createEmptyDelta() {
        ObjectDelta<O> newDelta = PrismContext.get().deltaFactory().object()
                .create(objectTypeClass, ChangeType.MODIFY);
        newDelta.setOid(getOid());
        return newDelta;
    }
    //endregion

    //region State updates
    /**
     * Sets OID of the new object but also to the deltas (if applicable).
     */
    void setOid(String oid) {
        this.oid = oid;

        if (oid != null && primaryDelta != null && !primaryDelta.isImmutable()) {
            primaryDelta.setOid(oid);
        }
        if (oid != null && secondaryDelta != null) {
            secondaryDelta.setOid(oid);
        }
        if (oid != null && currentDelta != null && !currentDelta.isImmutable()) {
            currentDelta.setOid(oid);
        }
        if (oid != null && summaryDelta != null && !summaryDelta.isImmutable()) {
            summaryDelta.setOid(oid);
        }
        archivedSecondaryDeltas.setOid(oid);

        if (oid != null && newObject != null && !newObject.isImmutable()) {
            newObject.setOid(oid);
        }
    }

    /** Internally used. */
    private void setOldObject(PrismObject<O> oldObject) {
        this.oldObject = oldObject;
        invalidateOldObjectDependencies(); // currently no-op
    }

    /**
     * Sets the current object.
     *
     * Should be used only by the context loader.
     */
    void setCurrentObject(PrismObject<O> currentObject) {
        this.currentObject = currentObject;
        invalidateCurrentObjectDependencies();
    }

    /**
     * Clears the current state, e.g. when determining that the object does not exist anymore.
     */
    void clearCurrentObject() {
        setCurrentObject(null);
    }

    /**
     * Sets the current object and updates the OID as well. Used when OID may change (on projections).
     *
     * Should be used only by the context loader.
     */
    void setCurrentObjectAndOid(@NotNull PrismObject<O> object) {
        setCurrentObject(object);
        setOid(object.getOid());
    }

    /**
     * Sets both current and (if needed) also the old object.
     *
     * Should be used only by the context loader.
     */
    void setCurrentAndOptionallyOld(@NotNull PrismObject<O> object, boolean setAlsoOld) {
        setCurrentObject(object.cloneIfImmutable());
        if (setAlsoOld) {
            setOldObject(object.clone());
        }
    }

    /**
     * Sets both current and (if the operation is not `ADD`) also the old object.
     */
    void setInitialObject(@NotNull PrismObject<O> object) {
        setCurrentAndOptionallyOld(object, true);
    }

    /**
     * Sets the primary delta.
     *
     * Avoid calling this method from the outside of this class, because the primary delta should be
     * practically immutable after the clockwork is started.
     */
    void setPrimaryDelta(ObjectDelta<O> primaryDelta) {
        this.primaryDelta = primaryDelta;
        invalidatePrimaryDeltaDependencies();
    }

    /**
     * Sets the secondary delta. For private use.
     */
    private void setSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        this.secondaryDelta = secondaryDelta;
        invalidateSecondaryDeltaDependencies();
    }

    /**
     * Removes the secondary delta.
     */
    void clearSecondaryDelta() {
        secondaryDelta = null;
        invalidateSecondaryDeltaDependencies();
    }

    /** Manages the deltas after execution. */
    void updateDeltasAfterExecution(int executionWave) {
        wasPrimaryDeltaExecuted = true;
        archivedSecondaryDeltas.add(executionWave, secondaryDelta);
        clearSecondaryDelta();
    }

    /**
     * Simulates the execution of the delta. In simulation mode we don't have the luxury of executing the deltas and then
     * re-loading the current object. So we have to apply the deltas onto the object ourselves.
     *
     * Beware, the delta may be different from {@link #currentDelta}. Projection delta execution creates "deltas to execute"
     * in quite a special way.
     */
    void simulateDeltaExecution(ObjectDelta<O> delta) throws SchemaException {
        if (simulatedExecutions == 0) {
            if (currentObject != null && currentObject.asObjectable() instanceof ShadowType) {
                currentShadowBeforeSimulatedDeltaExecution = currentObject.clone();
            } else {
                // not necessary for focus objects
            }
        }
        simulatedExecutions++;

        if (currentObject == null) {
            if (isAdd(delta)) {
                setCurrentObject(delta.getObjectToAdd());
            } else if (delta != null) {
                LOGGER.warn("No current object and current delta is not add? Ignoring:\n{}", delta.debugDump());
            }
        } else if (delta != null) {
            if (delta.isAdd()) {
                LOGGER.warn("Current object exists and current delta is ADD? Ignoring. Object:\n{}\nDelta:\n{}",
                        currentObject.debugDump(1), delta.debugDump(1));
            } else if (delta.isDelete()) {
                clearCurrentObject();
            } else {
                delta.applyTo(currentObject);
                invalidateCurrentObjectDependencies();
            }
        }
        if (currentObject != null) {
            if (generateMissingContainerIds(currentObject)) {
                invalidateCurrentObjectDependencies();
            }
        }
    }

    private boolean generateMissingContainerIds(PrismObject<O> currentObject) throws SchemaException {
        ContainerValueIdGenerator generator = new ContainerValueIdGenerator(currentObject);
        generator.generateForNewObject();
        return generator.getGenerated() > 0;
    }

    /**
     * Sets the initial state of the element (old, current, primary delta).
     */
    void initializeState(String oid, PrismObject<O> objectOld, PrismObject<O> objectCurrent, ObjectDelta<O> primaryDelta) {
        setOid(oid);
        setOldObject(objectOld);
        setCurrentObject(objectCurrent);
        setPrimaryDelta(primaryDelta);
        clearSecondaryDelta();
    }

    /**
     * Replaces OID, old, current. Deltas are untouched. Very dangerous.
     */
    void replaceOldAndCurrentObject(String oid, PrismObject<O> objectOld, PrismObject<O> objectCurrent) {
        setOid(oid);
        setOldObject(objectOld);
        setCurrentObject(objectCurrent);
    }
    //endregion

    //region Remembering and restoring the state
    /**
     * Creates a representation of a state to be (maybe) restored later.
     *
     * Currently comprises only the secondary deltas.
     * Later we may add also current and summary deltas and new object.
     */
    @NotNull RememberedElementState<O> rememberState() {
        return new RememberedElementState<>(
                CloneUtil.cloneCloneable(secondaryDelta));
    }

    void restoreState(@NotNull RememberedElementState<O> rememberedState) {
        setSecondaryDelta(
                CloneUtil.cloneCloneable(
                        rememberedState.getSecondaryDelta()));
    }
    //endregion

    //region Other
    public void normalize() {
        normalizePrimaryDelta();
        if (secondaryDelta != null) {
            secondaryDelta.normalize();
        }
        if (currentDelta != null && !currentDelta.isImmutable()) {
            currentDelta.normalize();
        }
        if (summaryDelta != null && !summaryDelta.isImmutable()) {
            summaryDelta.normalize();
        }
        archivedSecondaryDeltas.normalize();
    }

    void normalizePrimaryDelta() {
        if (primaryDelta != null && !primaryDelta.isImmutable()) {
            primaryDelta.normalize();
        }
    }

    void freezePrimaryDelta() {
        if (primaryDelta != null) {
            primaryDelta.freeze();
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ElementState<O> clone() {
        return new ElementState<>(this);
    }

    public void adopt(PrismContext prismContext) throws SchemaException {
        if (newObject != null) {
            prismContext.adopt(newObject);
        }
        if (oldObject != null) {
            prismContext.adopt(oldObject);
        }
        if (currentObject != null) {
            prismContext.adopt(currentObject);
        }
        if (primaryDelta != null) {
            prismContext.adopt(primaryDelta);
        }
        if (secondaryDelta != null) {
            prismContext.adopt(secondaryDelta);
        }
        archivedSecondaryDeltas.adopt(prismContext);
        // TODO: object definition?
    }

    void checkEncrypted() {
        if (newObject != null) {
            CryptoUtil.checkEncrypted(newObject);
        }
        if (oldObject != null) {
            CryptoUtil.checkEncrypted(oldObject);
        }
        if (currentObject != null) {
            CryptoUtil.checkEncrypted(currentObject);
        }
        if (primaryDelta != null) {
            CryptoUtil.checkEncrypted(primaryDelta);
        }
        if (secondaryDelta != null) {
            CryptoUtil.checkEncrypted(secondaryDelta);
        }
        archivedSecondaryDeltas.checkEncrypted("secondary deltas");
    }

    void forEachObject(Consumer<PrismObject<O>> consumer) {
        if (currentObject != null) {
            consumer.accept(currentObject);
        }
        if (oldObject != null) {
            consumer.accept(oldObject);
        }
        if (newObject != null) {
            consumer.accept(newObject);
        }
    }

    void forEachDelta(Consumer<ObjectDelta<O>> consumer) {
        if (primaryDelta != null) {
            consumer.accept(primaryDelta);
        }
        if (secondaryDelta != null) {
            consumer.accept(secondaryDelta);
        }
        if (currentDelta != null) {
            consumer.accept(currentDelta);
        }
        if (summaryDelta != null) {
            consumer.accept(summaryDelta);
        }
        for (ObjectDelta<O> secondaryDelta : archivedSecondaryDeltas) {
            consumer.accept(secondaryDelta);
        }
    }

    boolean isFresh() {
        return fresh;
    }

    void setFresh(boolean fresh) {
        this.fresh = fresh;
    }
    //endregion

    //region Consistency checks
    void checkConsistence(@NotNull LensElementContext<O> elementCtx, @Nullable String globalCtxDesc) {
        String elementDesc = elementCtx.getElementDesc();
        if (oldObject != null) {
            checkObjectConsistence(oldObject, "old " + elementDesc, elementCtx, globalCtxDesc);
        }
        if (currentObject != null) {
            checkObjectConsistence(currentObject, "current " + elementDesc, elementCtx, globalCtxDesc);
        }
        if (primaryDelta != null) {
            checkDeltaConsistence(primaryDelta, false, elementCtx,
                    elementDesc + " primary delta in " + elementCtx + in(globalCtxDesc));
        }
        if (newObject != null) {
            checkObjectConsistence(newObject, "new " + elementDesc, elementCtx, globalCtxDesc);
        }
    }

    @NotNull
    static String in(@Nullable String globalCtxDesc) {
        return globalCtxDesc == null ? "" : " in " + globalCtxDesc;
    }

    void checkSecondaryDeltaConsistence(boolean requireOid, LensElementContext<O> elementContext, String contextDesc) {
        if (secondaryDelta != null) {
            checkDeltaConsistence(secondaryDelta, requireOid, elementContext,
                    elementContext.getElementDesc() + " secondary delta in " + elementContext + in(contextDesc));
        }
    }

    private void checkDeltaConsistence(ObjectDelta<O> delta, boolean requireOid, @NotNull LensElementContext<O> elementCtx,
            String contextDesc) {
        try {
            delta.checkConsistence(requireOid, true, true, ConsistencyCheckScope.THOROUGH);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + "; in " + contextDesc, e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + "; in " + contextDesc, e);
        }
        if (delta.isAdd()) {
            checkObjectConsistence(delta.getObjectToAdd(), "add object", elementCtx, contextDesc);
        }
    }

    private void checkObjectConsistence(@NotNull PrismObject<O> object, String elementDesc,
            LensElementContext<O> elementContext, String contextDesc) {
        String desc = elementDesc + " in " + elementContext + in(contextDesc);
        try {
            object.checkConsistence(true, ConsistencyCheckScope.THOROUGH);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + "; in " + desc, e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + "; in " + desc, e);
        }
        if (object.getDefinition() == null) {
            throw new IllegalStateException("No " + elementDesc + " definition " + desc);
        }
        O objectType = object.asObjectable();
        if (objectType instanceof ShadowType) {
            //noinspection unchecked
            ShadowUtil.checkConsistence((PrismObject<? extends ShadowType>) object, desc);
        }
        elementContext.doExtraObjectConsistenceCheck(object, elementDesc, contextDesc);
    }
    //endregion

    /** Creates adjusted current object. */
    interface CurrentObjectAdjuster<O extends ObjectType> extends Serializable {
        @Nullable PrismObject<O> adjust(@Nullable PrismObject<O> objectCurrent) throws SchemaException, ConfigurationException;
    }

    /** Refines "raw" object definition. */
    interface ObjectDefinitionRefiner<O extends ObjectType> extends Serializable {
        @NotNull PrismObjectDefinition<O> refine(@NotNull PrismObjectDefinition<O> rawDefinition);
    }
}
