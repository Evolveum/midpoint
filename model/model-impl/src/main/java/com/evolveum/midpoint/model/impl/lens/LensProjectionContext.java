/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.function.Consumer;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ElementState.CurrentObjectAdjuster;
import com.evolveum.midpoint.model.impl.lens.ElementState.ObjectDefinitionRefiner;
import com.evolveum.midpoint.model.impl.lens.construction.ConstructionTargetKey;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAssignedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PlainResourceObjectConstruction;
import com.evolveum.midpoint.model.impl.lens.executor.ItemChangeApplicationModeConfiguration;
import com.evolveum.midpoint.model.impl.lens.projector.DependencyProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.sync.action.DeleteResourceObjectAction;
import com.evolveum.midpoint.model.impl.sync.action.UnlinkAction;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.jaxb2_commons.lang.Validate;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;

import static com.evolveum.midpoint.model.impl.lens.ChangeExecutionResult.getExecutedDelta;
import static com.evolveum.midpoint.model.impl.lens.ElementState.in;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * @author semancik
 *
 */
public class LensProjectionContext extends LensElementContext<ShadowType> implements ModelProjectionContext {

    private static final Trace LOGGER = TraceManager.getTrace(LensProjectionContext.class);

    private static final boolean DEBUG_DUMP_DEPENDENCIES = false;

    /**
     * Delta that came from the resource (using live sync, async update, import, reconciliation, and so on) that
     * is the basis for the current processing. It is a little bit similar to the primary delta: it is given
     * from the "outside", not computed by the projector. However, unlike primary delta, this delta describes
     * what _has already happened_, not what should be done.
     *
     * See also {@link ModelProjectionContext#getSyncDelta()}.
     */
    private ObjectDelta<ShadowType> syncDelta;

    /**
     * Is this projection the source of the synchronization? (The syncDelta attribute could be used for this but in
     * reality it is not always present.)
     *
     * This information was once used for operation execution recording. It is no longer the case.
     * But since 4.7 we use it for recording result and result status during simulations.
     */
    private boolean synchronizationSource;

    /**
     * If set to true: absolute state of this projection was detected by the synchronization.
     * This is mostly for debugging and visibility. It is not used by projection logic.
     */
    private boolean syncAbsoluteTrigger;

    /**
     * The wave in which this resource should be processed. Initial value of -1 means "undetermined".
     */
    private int wave = -1;

    /**
     * Indicates that the wave computation is still in progress.
     */
    private transient boolean waveIncomplete;

    /**
     * Was the processing of this projection (in its execution wave) complete?
     * We use this flag to avoid re-processing projections when wave is repeated.
     *
     * @see ChangeExecutionResult#projectionRecomputationRequested
     */
    @Experimental
    private boolean completed;

    /**
     * Determines the type of resource object this context deals with - if known - plus additional "discriminating" information,
     * like tag, order, and "gone" flag.
     *
     * Uses of this information:
     *
     * 1. to determine resource object definition (type or class - if at all possible);
     * 2. to ensure there are no duplicate projection contexts (= same resource, kind, intent, tag, order, "gone" flag)
     *
     * TODO what is the exact handling of the "gone" flag in this respect? Can we have two identical "gone" contexts?
     *
     * Note that the information may be incomplete. It is possible that we don't know even the resource OID (for broken links).
     * Or that the shadow is unclassified, so all we know is its object class.
     *
     * Lifecycle: Since 4.6 we try to determine the key as soon as realistically possible; and we try to keep it updated all
     * the time. So we try to update along with the shadow in this projection context. Resource OID, kind, and intent
     * are treated by {@link #onObjectChange(ShadowType)}. The {@link ProjectionContextKey#gone} flag should be set via
     * {@link #markGone()} method.
     *
     * NOTE: When updating the key, think about invalidating the {@link #state}. It's the most safe to use
     * {@link #setKey(ProjectionContextKey)} for this.
     */
    @NotNull private ProjectionContextKey key;

    /**
     * Is the resource object fully loaded from the resource?
     *
     * TODO Sometimes the "true" here means that the object is not loaded, but there's no point in trying to load it.
     *  We need to clarify that.
     */
    private boolean fullShadow;

    /**
     * A temporary solution for the misuse of {@link ElementState#fresh} field for projections.
     * We set this to {@code true} when the projection should be re-loaded, at least from the repository. MID-9944.
     */
    private boolean reloadNeeded;

    /**
     * True if the account is assigned to the user by a valid assignment. It may be false for accounts that are either
     * found to be illegal by live sync, were unassigned from user, etc.
     * If set to null the situation is not yet known. Null is a typical value when the context is constructed.
     */
    private Boolean assigned;

    /**
     * TODO describe
     */
    private Boolean assignedOld;

    /**
     * True if the account should be part of the synchronization. E.g. outbound expression should be applied to it.
     *
     * TODO It looks like this is currently not used. Consider removing.
     */
    private boolean active;

    /**
     * True if there is a valid assignment for this projection and/or the policy allows such projection to exist.
     */
    private Boolean legal;

    /**
     * TODO describe
     */
    private Boolean legalOld;

    /**
     * True if the projection exists (or will exist) on resource. False if it does not exist.
     * NOTE: entire projection is loaded with pointInTime=future. Therefore this does NOT
     * reflect actual situation. If there is a pending operation to create the object then
     * isExists will in fact be true.
     */
    private boolean exists;

    /**
     * True if shadow exists in the repo. It is set to false after projector discovers that a shadow is gone.
     * This is a corner case, but it may happen: if shadow is unintentionally deleted, if the shadow is
     * cleaned up by another thread and so on.
     */
    private transient boolean shadowExistsInRepo = true;

    /**
     * Intention of what we want to do with this projection context.
     *
     * Although there are more values, currently it seems that we use only UNLINK and DELETE ones.
     *
     * Set by the client (see {@link UnlinkAction} and {@link DeleteResourceObjectAction}) or automatically
     * determined from linkRef delete delta by {@link ContextLoader}.
     *
     * See also {@link ModelProjectionContext#getSynchronizationIntent()}.
     */
    private SynchronizationIntent synchronizationIntent;

    /**
     * Decision regarding the account. It indicated what the engine has DECIDED TO DO with the context.
     * If set to null no decision was made yet. Null is also a typical value when the context is created.
     *
     * See also {@link ModelProjectionContext#getSynchronizationPolicyDecision()}.
     */
    private SynchronizationPolicyDecision synchronizationPolicyDecision;

    /**
     * True if we want to reconcile account in this context.
     */
    private boolean doReconciliation;

    /**
     * false if the context should be not taken into the account while synchronizing changes from other resource
     */
    private boolean canProject = true;

    /**
     * Synchronization situation as it was originally detected by the synchronization code (SynchronizationService).
     * This is mostly for debug purposes. Projector and clockwork do not need to care about this.
     * The synchronization intent is used instead.
     */
    private SynchronizationSituationType synchronizationSituationDetected;

    /**
     * Synchronization situation which was the result of synchronization reaction (projector and clockwork run).
     * This is mostly for debug purposes. Projector and clockwork do not care about this (except for setting it).
     * The synchronization decision is used instead.
     */
    private SynchronizationSituationType synchronizationSituationResolved;

    /**
     * Delta set triple for constructions obtained via assignments. Specifies which constructions (projections e.g. accounts)
     * should be added, removed or stay as they are.
     *
     * It tells almost nothing about attributes directly, although the information about attributes are inside
     * each construction.
     *
     * It is an intermediary computation result. It is stored to allow re-computing of constructions during
     * iterative computations.
     *
     * - Source: AssignmentProcessor
     * - Target: ConsolidationProcessor / ReconciliationProcessor (via squeezed structures)
     *
     * Note that relativity is taken to focus OLD state, not to the current state.
     */
    private transient DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<?>> evaluatedAssignedConstructionDeltaSetTriple;

    /**
     * Evaluated "plain" resource object construction obtained from the schema handling configuration for given resource.
     *
     * TODO better name
     *
     * - Source: OutboundProcessor
     * - Target: ConsolidationProcessor / ReconciliationProcessor (via squeezed structures)
     */
    private transient PlainResourceObjectConstruction<?> evaluatedPlainConstruction;

    /**
     * Post-processed triples from the above two properties.
     *
     * - Source: ConsolidationProcessor
     * - Target: ReconciliationProcessor
     */
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> squeezedAttributes;
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>>> squeezedAssociations;
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses;

    /** Dependency-defining beans *with the defaults filled-in*. All of resource OID, kind, and intent are not null. */
    private transient Collection<ResourceObjectTypeDependencyType> dependencies;

    /** This definition is always immutable. */
    private transient ResourceObjectDefinition structuralObjectDefinition;

    /** These definitions are always immutable. */
    private transient Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions;

    /** This definition is always immutable. */
    private transient CompositeObjectDefinition compositeObjectDefinition;

    /**
     * Security policy for given projection; derived from the resource object type definition.
     *
     * *Limitation: Currently does NOT include global security policy.*
     */
    private SecurityPolicyType projectionSecurityPolicy;

    /**
     * Resource that hosts this projection.
     */
    private transient ResourceType resource;

    /**
     * EXPERIMENTAL. A flag that this projection context has to be put into 'history archive'.
     * Necessary to evaluate old state of hasLinkedAccount.
     *
     * TODO implement as non-transient.
     */
    private transient boolean toBeArchived;

    /**
     * Primary identifier + intent + order + resource.
     *
     * TODO why not kind?
     */
    private transient String humanReadableName;

    /**
     * Short characterization of the projection: kind + intent + resource.
     *
     * TODO consider better name
     */
    private transient String humanReadableString;

    /**
     * Cached entitlements for the projection, using OID as the key.
     */
    private final Map<String, PrismObject<ShadowType>> entitlementMap = new HashMap<>();

    /**
     * Cached value metadata to be used for resource object values processed by inbound mappings.
     */
    private transient ValueMetadataType cachedValueMetadata;

    LensProjectionContext(
            @NotNull LensContext<? extends ObjectType> lensContext,
            @NotNull ProjectionContextKey key) {
        super(ShadowType.class, lensContext);
        this.key = key;
    }

    // Used for cloning
    private LensProjectionContext(
            @NotNull LensContext<? extends ObjectType> lensContext,
            @NotNull ProjectionContextKey key,
            @NotNull ElementState<ShadowType> elementState) {
        super(elementState, lensContext);
        this.key = key;
    }

    /**
     * Returns true if the `target` is a target of `dependency` configuration.
     *
     * (Meaning that the source that contains `dependency` among its configured dependencies depends on `target`,
     * i.e. the `dependency` configuration points to `target`.)
     *
     * Precondition: dependency is fully specified (resource, kind, intent are not null), see {@link #getDependencies()}.
     */
    public boolean isDependencyTarget(
            ResourceObjectTypeDependencyType dependency) {
        // The DependencyProcessor looks like a better home for the business logic of the matching
        return DependencyProcessor.matches(this, dependency);
    }

    @Override
    public ObjectDelta<ShadowType> getSyncDelta() {
        return syncDelta;
    }

    @Override
    public void setSyncDelta(ObjectDelta<ShadowType> syncDelta) {
        this.syncDelta = syncDelta;
        state.invalidate(); // sync delta is a parameter for adjuster
    }

    @Override
    public void setLoadedObject(@NotNull PrismObject<ShadowType> object) {
        state.setCurrentAndOptionallyOld(object, !state.hasOldObject() && !isAdd());
        onObjectChange(object.asObjectable());
    }

    @Override
    public void setInitialObject(@NotNull PrismObject<ShadowType> object) {
        super.setInitialObject(object);
        onObjectChange(object.asObjectable());
    }

    @Override
    public void setCurrentObject(@Nullable PrismObject<ShadowType> object) {
        super.setCurrentObject(object);
        if (object != null) {
            onObjectChange(object.asObjectable());
        }
    }

    @Override
    public void setCurrentObjectAndOid(@NotNull PrismObject<ShadowType> object) {
        super.setCurrentObjectAndOid(object);
        onObjectChange(object.asObjectable());
    }

    @Override
    public void replaceOldAndCurrentObject(String oid, PrismObject<ShadowType> objectOld, PrismObject<ShadowType> objectCurrent) {
        super.replaceOldAndCurrentObject(oid, objectOld, objectCurrent);
        if (objectCurrent != null) {
            onObjectChange(objectCurrent.asObjectable()); // just for sure?
        }
    }

    /**
     * 1. Invalidates human readable name, as it is heavily bound to the object
     * 2. Updates and checks the key against the information from shadow: resource OID, kind, intent.
     *
     * We intentionally not deal with the "gone" flag here. The client is responsible for updating that.
     */
    private void onObjectChange(@NotNull ShadowType shadow) {

        invalidateHumanReadableName(); // better safe than sorry

        ProjectionContextKey updated =
                key.checkOrUpdateResourceOid(
                        ShadowUtil.getResourceOidRequired(shadow));

        ResourceObjectTypeIdentification newTypeId = ShadowUtil.getTypeIdentification(shadow);
        if (newTypeId != null) {
            updated = updated.checkOrUpdateTypeIdentification(newTypeId);
        }

        updated = updated.updateTagIfChanged(shadow.getTag()); // currently not updating anything

        if (updated != key) {
            LOGGER.trace("Updating projection key from {} to {}", key, updated);
            setKey(updated);
        }
    }

    /**
     * Returns {@link ObjectDeltaObject} to be used e.g. expression evaluation regarding this projection.
     *
     * We use {@link ElementState#getRelativeObjectDeltaObject()}, although the "absolute" version should
     * provide more or less the same results. Projections are not updated iteratively, unlike the focus.
     */
    public ObjectDeltaObject<ShadowType> getObjectDeltaObject() throws SchemaException, ConfigurationException {
        return state.getRelativeObjectDeltaObject();
    }

    /**
     * Note for implementors: if you introduce a new parameter here (besides syncDelta, OCD, RSD, policy decision),
     * please add {@link ElementState#invalidate()} call to appropriate setter.
     */
    @Override
    @NotNull CurrentObjectAdjuster<ShadowType> getCurrentObjectAdjuster() {
        return objectCurrent -> {
            if (objectCurrent != null) {
                return objectCurrent;
            } else if (ObjectDelta.isAdd(syncDelta)) {
                return syncDelta.getObjectToAdd();
            } else if (shouldCreateObjectCurrent()) {
                ResourceObjectDefinition rOCD = getCompositeObjectDefinition();
                if (rOCD != null) {
                    return rOCD
                            .createBlankShadowWithTag(key.getTag())
                            .getPrismObject();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        };
    }

    @Override
    @NotNull ObjectDefinitionRefiner<ShadowType> getObjectDefinitionRefiner() {
        return (rawDefinition) -> {
            try {
                PrismObjectDefinition<ShadowType> shadowDefinition
                        = ShadowUtil.applyObjectDefinition(rawDefinition, getCompositeObjectDefinitionRequired());
                shadowDefinition.freeze();
                return shadowDefinition;
            } catch (SchemaException | ConfigurationException e) {
                // This should not happen
                throw new SystemException(e.getMessage(), e);
            }
        };
    }

    private boolean shouldCreateObjectCurrent() {
        ObjectDelta<ShadowType> currentDelta = getCurrentDelta();
        return ObjectDelta.isModify(currentDelta) || currentDelta == null && decisionIsAdd();
    }

    @Override
    public ObjectDelta<ShadowType> getSummarySecondaryDelta() {
        return getSecondaryDelta();
    }

    boolean isSyncAbsoluteTrigger() {
        return syncAbsoluteTrigger;
    }

    public void setSyncAbsoluteTrigger(boolean syncAbsoluteTrigger) {
        this.syncAbsoluteTrigger = syncAbsoluteTrigger;
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public boolean isWaveIncomplete() {
        return waveIncomplete;
    }

    public void setWaveIncomplete(boolean waveIncomplete) {
        this.waveIncomplete = waveIncomplete;
    }

    public boolean isDoReconciliation() {
        return doReconciliation;
    }

    public void setDoReconciliation(boolean doReconciliation) {
        this.doReconciliation = doReconciliation;
    }

    @Override
    public @NotNull ProjectionContextKey getKey() {
        return key;
    }

    public void markGone() {
        key = key.gone();
        setExists(false);
        setFullShadow(false);
        humanReadableName = null;
    }

    public void setKey(@NotNull ProjectionContextKey key) {
        this.key = key;
        state.invalidate(); // The key (its coordinates in particular) is a parameter for current object adjuster
    }

    public boolean matches(@NotNull ConstructionTargetKey targetKey) {
        // This is simple because ConstructionTargetKey has resource/kind/intent obligatory. No ambiguities there.
        return targetKey.getResourceOid().equals(key.getResourceOid())
                && targetKey.getKind() == key.getKind()
                && targetKey.getIntent().equals(key.getIntent())
                && Objects.equals(targetKey.getTag(), key.getTag());
    }

    /**
     * Note that we do not consider the project as matching if it's not classified. The reason is that constructions
     * are targeted to specific kind/intent. There is a chance that this particular context points to a shadow that
     * (if properly classified) would match this, but let's ignore this.
     */
    public boolean matches(@NotNull ConstructionType construction, @NotNull String resourceOid) {
        if (!isClassified()) {
            return false;
        }
        if (!Objects.equals(getResourceOid(), resourceOid)) {
            return false;
        }
        if (getKind() != ConstructionTypeUtil.getKind(construction)) {
            return false;
        }
        String constructionIntent = construction.getIntent();
        if (constructionIntent != null) {
            return constructionIntent.equals(key.getIntent());
        } else {
            // Construction is targeted at the default intent for given kind.
            // Let us have a look if our definition is default for its kind.
            ResourceObjectDefinition definition;
            try {
                // TODO what if resource is null here? There will be no definition then.
                definition = getStructuralObjectDefinition();
            } catch (Exception e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't determine object definition for {}", e, this);
                setBroken();
                return false;
            }
            ResourceObjectTypeDefinition typeDefinition = definition != null ? definition.getTypeDefinition() : null;
            if (typeDefinition == null) {
                // Should not occur (as the key is classified), but let's not crash on this
                return false;
            } else {
                return typeDefinition.isDefaultForKind();
            }
        }
    }

    /**
     * Returns true if this context is a "lower order brother" of the specified one.
     */
    public boolean isLowerOrderOf(@NotNull LensProjectionContext other) {
        ProjectionContextKey otherKey = other.getKey();
        if (!Objects.equals(key.getResourceOid(), otherKey.getResourceOid())) {
            return false;
        }
        if (key.getTypeIdentification() != null) {
            if (!Objects.equals(key.getTypeIdentification(), otherKey.getTypeIdentification())) {
                return false;
            }
        } else {
            // No type identification... what can we do? Let us compare object class names. But this is far from reliable,
            // as one context may be classified and the other one may be not.
            if (!Objects.equals(getObjectClassName(), other.getObjectClassName())) {
                return false;
            }
        }
        if (!Objects.equals(key.getTag(), otherKey.getTag())) {
            return false;
        }
        // This is questionable - but this is exactly how it was before 4.6.
        if (key.isGone() != otherKey.isGone()) {
            return false;
        }
        return key.getOrder() < otherKey.getOrder();
    }

    /**
     * TODO clarify this!!!
     */
    public boolean matches(ProjectionContextKey otherKey, boolean compareOrder) {
        Validate.notNull(otherKey.getResourceOid());
        if (!otherKey.getResourceOid().equals(key.getResourceOid())) {
            return false;
        }
        if (key.getTypeIdentification() != null || otherKey.getTypeIdentification() != null) {
            // TODO what if one key is classified, and the other one is not?
            //  They may still point to the same type of objects.
            if (!Objects.equals(key.getTypeIdentification(), otherKey.getTypeIdentification())) {
                return false;
            }
        } else {
            // TODO what should we do in this case?
            //  Both keys are unclassified. They may refer to the same type of objects, or may not. Who knows.
        }
        if (!Objects.equals(otherKey.getTag(), key.getTag())) {
            return false;
        }
        if (otherKey.isGone() != key.isGone()) {
            return false;
        }
        return !compareOrder
                || otherKey.getOrder() == key.getOrder();
    }

    @Override
    public boolean isGone() {
        return key.isGone();
    }

    public boolean isGoneOrReaping() {
        return isGone() || isReaping();
    }

    public boolean isReaping() {
        return getCurrentShadowState() == ShadowLifecycleStateType.REAPING;
    }

    public void addAccountSyncDelta(ObjectDelta<ShadowType> delta) throws SchemaException {
        if (syncDelta == null) {
            syncDelta = delta;
        } else {
            syncDelta.merge(delta);
        }
    }

    @Override
    public boolean isAdd() {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD) {
            return true;
        } else if (synchronizationPolicyDecision != null) {
            return false;
        } else {
            return ObjectDelta.isAdd(state.getPrimaryDelta());
        }
    }

    @Override
    public boolean isModify() {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.KEEP) {
            return true;
        } else if (synchronizationPolicyDecision != null) {
            return false;
        } else {
            return super.isModify();
        }
    }

    @Override
    public boolean isDelete() {
        // Note that there are situations where decision is UNLINK with primary delta being DELETE. (Why?)
        return synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE
                || (synchronizationPolicyDecision == null && synchronizationIntent == SynchronizationIntent.DELETE) // MID-8608
                || ObjectDelta.isDelete(syncDelta)
                || ObjectDelta.isDelete(state.getPrimaryDelta());
    }

    @Override
    public ArchetypeType getArchetype() {
        throw new UnsupportedOperationException("Archetypes are not supported for projections.");
    }

    @Override
    public List<ArchetypeType> getArchetypes() {
        throw new UnsupportedOperationException("Archetypes are not supported for projections.");
    }

    @Override
    public ResourceType getResource() {
        return resource;
    }

    public @NotNull ResourceType getResourceRequired() {
        return MiscUtil.stateNonNull(resource, "No resource in %s", this);
    }

    public void setResource(ResourceType resource) {
        invalidateHumanReadableName();
        this.resource = resource;
        if (resource != null) {
            String existingResourceOid = key.getResourceOid();
            String newResourceOid = Objects.requireNonNull(resource.getOid());
            if (existingResourceOid != null) {
                stateCheck(existingResourceOid.equals(newResourceOid),
                        "Trying to set resource with a different OID than it's there now: new: %s, existing: %s",
                        resource, key);
            } else {
                setKey(key.withResourceOid(newResourceOid));
            }
        }
    }

    public Map<String, PrismObject<ShadowType>> getEntitlementMap() {
        return entitlementMap;
    }

    public Boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean isAssigned) {
        this.assigned = isAssigned;
    }

    public Boolean isAssignedOld() {
        return assignedOld;
    }

    private void setAssignedOld(Boolean isAssignedOld) {
        this.assignedOld = isAssignedOld;
    }

    /**
     * We want to set "assigned in old state" only once - in projection wave 0 where real "old"
     * values are known.
     *
     * TODO: what should we do with projections that appear later?
     */
    public void setAssignedOldIfUnknown(Boolean value) {
        if (assignedOld == null) {
            setAssignedOld(value);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }

    @Override
    public Boolean isLegal() {
        return legal;
    }

    public void setLegal(Boolean isLegal) {
        this.legal = isLegal;
    }

    public Boolean isLegalOld() {
        return legalOld;
    }

    public void setLegalOld(Boolean isLegalOld) {
        this.legalOld = isLegalOld;
    }

    /**
     * We want to set "legal in old state" only once - in projection wave 0 where real "old"
     * values are known.
     *
     * TODO: what should we do with projections that appear later?
     */
    public void setLegalOldIfUnknown(Boolean value) {
        if (legalOld == null) {
            setLegalOld(value);
        }
    }

    @Override
    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean isShadowExistsInRepo() {
        return shadowExistsInRepo;
    }

    public void setShadowExistsInRepo(boolean shadowExistsInRepo) {
        this.shadowExistsInRepo = shadowExistsInRepo;
    }

    @Override
    public SynchronizationIntent getSynchronizationIntent() {
        return synchronizationIntent;
    }

    public void setSynchronizationIntent(SynchronizationIntent synchronizationIntent) {
        this.synchronizationIntent = synchronizationIntent;
    }

    @Override
    public SynchronizationPolicyDecision getSynchronizationPolicyDecision() {
        return synchronizationPolicyDecision;
    }

    public void setSynchronizationPolicyDecision(SynchronizationPolicyDecision policyDecision) {
        this.synchronizationPolicyDecision = policyDecision;
        state.invalidate(); // policy decision is a parameter for current object adjuster
    }

    public boolean isSynchronizationDecisionAdd() {
        return synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD;
    }

    public void setBroken() {
        setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
    }

    public SynchronizationSituationType getSynchronizationSituationDetected() {
        return synchronizationSituationDetected;
    }

    public void setSynchronizationSituationDetected(
            SynchronizationSituationType synchronizationSituationDetected) {
        this.synchronizationSituationDetected = synchronizationSituationDetected;
    }

    public SynchronizationSituationType getSynchronizationSituationResolved() {
        return synchronizationSituationResolved;
    }

    public void setSynchronizationSituationResolved(SynchronizationSituationType synchronizationSituationResolved) {
        this.synchronizationSituationResolved = synchronizationSituationResolved;
    }

    @Override
    public boolean isFullShadow() {
        return fullShadow;
    }

    /**
     * Returns true if full shadow is available, either loaded or in a create delta.
     *
     * NOTE: The distinction between {@link #isFullShadow()} and this method is subtle but sometimes important.
     * The {@link #isFullShadow()} method is used to determine whether the shadow is fully loaded from the resource.
     * This method can return {@code true} also if we know how the shadow would need to be created. But resource-provided
     * attributes may be missing in such cases! Like {@code uid} in case of LDAP. Inbound mappings could fail in such cases.
     */
    public boolean hasFullShadow() {
        return isSynchronizationDecisionAdd() || isFullShadow();
    }

    public void setFullShadow(boolean fullShadow) {
        this.fullShadow = fullShadow;
    }

    private ItemLoadedStatus getGenericItemLoadedAnswer() throws SchemaException, ConfigurationException {
        if (isFullShadow()) {
            return ItemLoadedStatus.FULL_SHADOW;
        } else if (!isCachedShadowsUseAllowed()) {
            return ItemLoadedStatus.USE_OF_CACHED_NOT_ALLOWED;
        } else {
            return null; // Let's look at the status of the specific item
        }
    }

    /** @see #isAttributeLoaded(QName) */
    public boolean isActivationLoaded() throws SchemaException, ConfigurationException {
        ItemLoadedStatus status;
        var generic = getGenericItemLoadedAnswer();
        if (generic != null) {
            status = generic;
        } else {
            status = ItemLoadedStatus.fromItemCachedStatus(
                    ShadowUtil.getActivationCachedStatus(
                            getObjectCurrent(),
                            getCompositeObjectDefinitionRequired(),
                            getCurrentTime()));
        }
        LOGGER.trace("Activation loaded status: {}", status);
        return status.isLoaded();
    }

    /** @see #isAttributeLoaded(QName) */
    public boolean isPasswordValueLoaded() throws SchemaException, ConfigurationException {
        ItemLoadedStatus status;
        var generic = getGenericItemLoadedAnswer();
        if (generic != null) {
            status = generic;
        } else {
            status = ItemLoadedStatus.fromItemCachedStatus(
                    ShadowUtil.isPasswordValueLoaded(
                            getObjectCurrent(),
                            getCompositeObjectDefinitionRequired(),
                            getCurrentTime()));
        }
        LOGGER.trace("Password value loaded status: {}", status);
        return status.isLoaded();
    }

    /** @see #isAttributeLoaded(QName) */
    public boolean isAuxiliaryObjectClassPropertyLoaded() throws SchemaException, ConfigurationException {
        ItemLoadedStatus status;
        var generic = getGenericItemLoadedAnswer();
        if (generic != null) {
            status = generic;
        } else {
            status = ItemLoadedStatus.fromItemCachedStatus(
                    ShadowUtil.isAuxiliaryObjectClassPropertyLoaded(
                            getObjectCurrent(),
                            getCompositeObjectDefinitionRequired(),
                            getCurrentTime()));
        }
        LOGGER.trace("Auxiliary object class property loaded status: {}", status);
        return status.isLoaded();
    }

    private static XMLGregorianCalendar getCurrentTime() {
        return ModelBeans.get().clock.currentTimeXMLGregorianCalendar();
    }

    /**
     * Returns {@code true} if the attribute is available for processing. It must either be freshly loaded
     * (in the {@link #isFullShadow()} sense) or it must be cached *and* the use of cache for computations
     * must be allowed.
     */
    public boolean isAttributeLoaded(@NotNull QName attrName) throws SchemaException, ConfigurationException {
        return isAttributeLoaded(attrName, null);
    }

    public boolean isAttributeLoaded(@NotNull QName attrName, @Nullable ShadowAttributeDefinition<?, ?, ?, ?> attrDefOverride)
            throws SchemaException, ConfigurationException {
        ItemLoadedStatus status;
        var generic = getGenericItemLoadedAnswer();
        if (generic != null) {
            status = generic;
        } else {
            status = ItemLoadedStatus.fromItemCachedStatus(
                    ShadowUtil.isAttributeLoaded(
                            ItemName.fromQName(attrName),
                            getObjectCurrent(),
                            getCompositeObjectDefinitionRequired(),
                            attrDefOverride,
                            getCurrentTime()));
        }
        LOGGER.trace("Attribute '{}' loaded status: {}", attrName, status);
        return status.isLoaded();
    }

    /** @see #isAttributeLoaded(QName) */
    public boolean isAssociationLoaded(QName assocName) throws SchemaException, ConfigurationException {
        ItemLoadedStatus status;
        var generic = getGenericItemLoadedAnswer();
        if (generic != null) {
            status = generic;
        } else {
            status = ItemLoadedStatus.fromItemCachedStatus(
                    ShadowUtil.isAssociationLoaded(
                            ItemName.fromQName(assocName),
                            getObjectCurrent(),
                            getCompositeObjectDefinitionRequired(),
                            getCurrentTime()));
        }
        LOGGER.trace("Association '{}' loaded status: {}", assocName, status);
        return status.isLoaded();
    }

    public Collection<QName> getCachedAttributesNames() throws SchemaException, ConfigurationException {
        return ShadowUtil.getCachedAttributesNames(
                getObjectCurrent(), getCompositeObjectDefinitionRequired(), getCurrentTime());
    }

    public ShadowKindType getKind() {
        return getKey().getKind();
    }

    public <AH extends AssignmentHolderType> DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<AH>> getEvaluatedAssignedConstructionDeltaSetTriple() {
        //noinspection unchecked,rawtypes
        return (DeltaSetTriple) evaluatedAssignedConstructionDeltaSetTriple;
    }

    public <AH extends AssignmentHolderType> void setEvaluatedAssignedConstructionDeltaSetTriple(DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<AH>> evaluatedAssignedConstructionDeltaSetTriple) {
        //noinspection unchecked,rawtypes
        this.evaluatedAssignedConstructionDeltaSetTriple = (DeltaSetTriple) evaluatedAssignedConstructionDeltaSetTriple;
    }

    public <AH extends AssignmentHolderType> PlainResourceObjectConstruction<AH> getEvaluatedPlainConstruction() {
        //noinspection unchecked
        return (PlainResourceObjectConstruction<AH>) evaluatedPlainConstruction;
    }

    public void setEvaluatedPlainConstruction(PlainResourceObjectConstruction<?> evaluatedPlainConstruction) {
        this.evaluatedPlainConstruction = evaluatedPlainConstruction;
    }

    public Map<QName, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> getSqueezedAttributes() {
        return squeezedAttributes;
    }

    public void setSqueezedAttributes(Map<QName, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> squeezedAttributes) {
        this.squeezedAttributes = squeezedAttributes;
    }

    public Map<QName, DeltaSetTriple<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>>> getSqueezedAssociations() {
        return squeezedAssociations;
    }

    public void setSqueezedAssociations(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<ShadowAssociationValue, ShadowAssociationDefinition>>> squeezedAssociations) {
        this.squeezedAssociations = squeezedAssociations;
    }

    public Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> getSqueezedAuxiliaryObjectClasses() {
        return squeezedAuxiliaryObjectClasses;
    }

    public void setSqueezedAuxiliaryObjectClasses(
            Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses) {
        this.squeezedAuxiliaryObjectClasses = squeezedAuxiliaryObjectClasses;
    }

    public ResourceObjectDefinition getStructuralDefinitionIfNotBroken() throws SchemaException, ConfigurationException {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
            return null; // This was pre-4.6 behavior
        } else {
            return getStructuralObjectDefinition();
        }
    }

    public CompleteResourceSchema getResourceSchema() throws SchemaException, ConfigurationException {
        if (resource == null) {
            return null;
        }
        return ResourceSchemaFactory.getCompleteSchema(resource, LayerType.MODEL);
    }

    public @NotNull CompleteResourceSchema getResourceSchemaRequired() throws SchemaException, ConfigurationException {
        return stateNonNull(getResourceSchema(), "No resource schema in %s", this);
    }

    /** Returns immutable definition. */
    public @Nullable ResourceObjectDefinition getStructuralObjectDefinition() throws SchemaException, ConfigurationException {
        if (structuralObjectDefinition == null) {
            var resourceSchema = getResourceSchema();
            if (resourceSchema == null) {
                LOGGER.trace("No resource schema -> no structural object definition");
                return null;
            }
            ResourceObjectTypeIdentification typeIdentification = key.getTypeIdentification();
            if (typeIdentification != null) {
                // TODO - or we allow missing definition?
                //  That could mask a lot of other issues (missing mappings etc).
                // Note that the definition is requested for kind/intent but may be object class at the end;
                // if fallback for account/default is applied.
                structuralObjectDefinition =
                        resourceSchema.findObjectDefinitionRequired(typeIdentification.getKind(), typeIdentification.getIntent());
                LOGGER.trace("Obtained structural object definition for {}: {}", typeIdentification, structuralObjectDefinition);
            } else {
                QName objectClassName = getObjectClassName();
                if (objectClassName != null) {
                    // Note the definition may be class-level but also type-level (if default-for-object-class is present).
                    structuralObjectDefinition = resourceSchema.findDefinitionForObjectClassRequired(objectClassName);
                    LOGGER.trace("Obtained structural object definition for {}: {}", objectClassName, structuralObjectDefinition);
                } else {
                    LOGGER.trace("No type nor object class name -> no structural object definition for {}", this);
                    return null;
                }
            }
            if (structuralObjectDefinition != null) {
                structuralObjectDefinition.freeze();
            }
        }
        return structuralObjectDefinition;
    }

    public @NotNull ResourceObjectDefinition getStructuralObjectDefinitionRequired()
            throws SchemaException, ConfigurationException {
        var objectDef = getStructuralObjectDefinition();
        if (objectDef != null) {
            return objectDef;
        } else {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}",
                    key, lensContext.debugDump(1));
            throw new IllegalStateException("Definition for " + key + " not found in the context, but it should be there");
        }
    }

    private QName getObjectClassName() {
        PrismObject<ShadowType> anyShadow = getObjectAny();
        return anyShadow != null ? anyShadow.asObjectable().getObjectClass() : null;
    }

    public Collection<ResourceObjectDefinition> getAuxiliaryObjectClassDefinitions()
            throws SchemaException, ConfigurationException {
        if (auxiliaryObjectClassDefinitions == null) {
            refreshAuxiliaryObjectClassDefinitions();
        }
        return auxiliaryObjectClassDefinitions;
    }

    public void refreshAuxiliaryObjectClassDefinitions() throws SchemaException, ConfigurationException {
        ResourceSchema schema = getResourceSchema();
        if (schema == null) {
            return;
        }
        List<QName> auxiliaryObjectClassQNames = new ArrayList<>();
        addAuxiliaryObjectClassNames(auxiliaryObjectClassQNames, getObjectOld());
        addAuxiliaryObjectClassNames(auxiliaryObjectClassQNames, state.computeUnadjustedNewObject());
        auxiliaryObjectClassDefinitions = new ArrayList<>(auxiliaryObjectClassQNames.size());
        for (QName auxiliaryObjectClassQName: auxiliaryObjectClassQNames) {
            ResourceObjectDefinition auxiliaryObjectClassDef =
                    schema.findDefinitionForObjectClass(auxiliaryObjectClassQName);
            if (auxiliaryObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class %s specified in %s does not exist".formatted(
                        auxiliaryObjectClassQName, this));
            }
            auxiliaryObjectClassDef.checkImmutable();
            auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDef);
        }
        resetCompositeObjectDefinition();
    }

    private void resetCompositeObjectDefinition() {
        compositeObjectDefinition = null;
        itemChangeApplicationModeConfiguration = null;
    }

    @Override
    public CompositeObjectDefinition getCompositeObjectDefinition() throws SchemaException, ConfigurationException {
        if (compositeObjectDefinition == null) {
            ResourceObjectDefinition structuralDefinition = getStructuralObjectDefinition();
            if (structuralDefinition != null) {
                compositeObjectDefinition =
                        CompositeObjectDefinition.of(structuralDefinition, getAuxiliaryObjectClassDefinitions());
            }
            state.invalidate(); // composite OCD is a parameter for current object adjuster
        }
        return compositeObjectDefinition;
    }

    private void addAuxiliaryObjectClassNames(List<QName> auxiliaryObjectClassQNames,
            PrismObject<ShadowType> shadow) {
        if (shadow == null) {
            return;
        }
        for (QName aux: shadow.asObjectable().getAuxiliaryObjectClass()) {
            if (!auxiliaryObjectClassQNames.contains(aux)) {
                auxiliaryObjectClassQNames.add(aux);
            }
        }
    }

    public ShadowAttributeDefinition<?, ?, ?, ?> findAttributeDefinition(QName attrName)
            throws SchemaException, ConfigurationException {
        var fromStructuralDef = getStructuralObjectDefinitionRequired().findAttributeDefinition(attrName);
        if (fromStructuralDef != null) {
            return fromStructuralDef;
        }
        return getAuxiliaryObjectClassDefinitions().stream()
                .map(auxOcDef -> auxOcDef.findAttributeDefinition(attrName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Collection<ResourceObjectTypeDependencyType> getDependencies() throws SchemaException, ConfigurationException {
        if (dependencies == null) {
            ResourceObjectDefinition objectDefinition = getStructuralDefinitionIfNotBroken();
            if (objectDefinition == null) {
                LOGGER.trace("No object definition, no dependencies for {}", this);
                // There is a problem. Maybe the projection is broken? Or not initialized yet? Let's not cache this result.
                return List.of();
            }
            ResourceObjectTypeIdentification typeIdentification = objectDefinition.getTypeIdentification();
            if (typeIdentification == null) {
                LOGGER.trace("No type identification (i.e. not classified), no dependencies for {}", this);
                // Is the projection not classified yet? Again, let's not cache the result.
                return List.of();
            }
            dependencies = fillInDependencyDefaults(
                    objectDefinition.getDefinitionBean().getDependency(),
                    typeIdentification.getKind());
            LOGGER.trace("Found {} dependencies for {}", dependencies.size(), this);
        }
        return dependencies;
    }

    private Collection<ResourceObjectTypeDependencyType> fillInDependencyDefaults(
            List<ResourceObjectTypeDependencyType> rawBeans, @NotNull ShadowKindType defaultKind) {
        Collection<ResourceObjectTypeDependencyType> processedBeans = new ArrayList<>(rawBeans.size());
        for (ResourceObjectTypeDependencyType rawBean : rawBeans) {
            ResourceObjectTypeDependencyType processedBean = rawBean.clone();
            if (processedBean.getResourceRef() == null) {
                processedBean.setResourceRef(
                        ObjectTypeUtil.createObjectRef(resource));
            }
            if (processedBean.getKind() == null) {
                processedBean.setKind(defaultKind);
            }
            processedBeans.add(processedBean);
        }
        return processedBeans;
    }

    public SecurityPolicyType getProjectionSecurityPolicy() {
        return projectionSecurityPolicy;
    }

    public void setProjectionSecurityPolicy(SecurityPolicyType projectionSecurityPolicy) {
        this.projectionSecurityPolicy = projectionSecurityPolicy;
    }

    public void setCanProject(boolean canProject) {
        this.canProject = canProject;
    }

    public boolean isCanProject() {
        return canProject;
    }

    public @NotNull AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementMode()
            throws SchemaException, ConfigurationException {
        // FIXME Note here is an irregularity: if ProjectionPolicyType with empty assignmentPolicyEnforcement is present
        //  in the object class, it is ignored. But if the same is present at the resource level, it is taken into
        //  account (applying the default i.e. RELATIVE mode of enforcement).
        ProjectionPolicyType objectClassLevelPolicy = determineObjectClassProjectionPolicy();
        if (objectClassLevelPolicy != null && objectClassLevelPolicy.getAssignmentPolicyEnforcement() != null) {
            return objectClassLevelPolicy.getAssignmentPolicyEnforcement();
        }

        ResourceType resource = getResource();
        ProjectionPolicyType resourceLevelPolicy = resource != null ? resource.getProjection() : null;
        ProjectionPolicyType effectivePolicy = resourceLevelPolicy != null ?
                resourceLevelPolicy : lensContext.getAccountSynchronizationSettings();
        return MiscSchemaUtil.getAssignmentPolicyEnforcementMode(effectivePolicy);
    }

    public boolean isLegalize() throws SchemaException, ConfigurationException {
        ProjectionPolicyType objectClassPolicy = determineObjectClassProjectionPolicy();
        if (objectClassPolicy != null) {
            return BooleanUtils.isTrue(objectClassPolicy.isLegalize());
        }

        ResourceType resource = getResource();
        if (resource != null) {
            ProjectionPolicyType resourcePolicy = resource.getProjection();
            if (resourcePolicy != null) {
                return BooleanUtils.isTrue(resourcePolicy.isLegalize());
            }
        }

        ProjectionPolicyType globalPolicy = getLensContext().getAccountSynchronizationSettings();
        return globalPolicy != null
                && BooleanUtils.isTrue(globalPolicy.isLegalize());
    }

    private ProjectionPolicyType determineObjectClassProjectionPolicy() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDef = getStructuralObjectDefinition();
        return objectDef != null ?
                objectDef.getProjectionPolicy() : null;
    }

    /**
     * We sometimes need the 'object new' to exist before any real modifications are computed.
     * An example is when outbound mappings reference $projection/tag (see MID-6899).
     */
    private boolean decisionIsAdd() {
        return synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD;
    }

    public void clearIntermediateResults() {
        //constructionDeltaSetTriple = null;
        evaluatedPlainConstruction = null;
        squeezedAttributes = null;
    }

    @Override
    public ObjectDelta<ShadowType> getExecutableDelta() throws SchemaException, ConfigurationException {
        SynchronizationPolicyDecision policyDecision = getSynchronizationPolicyDecision();
        ObjectDelta<ShadowType> origDelta = getCurrentDelta();
        if (policyDecision == SynchronizationPolicyDecision.ADD) {
            // let's try to retrieve original (non-fixed) delta. Maybe it's ADD delta so we spare fixing it.
            origDelta = getSummaryDelta(); // TODO check this
            if (origDelta == null || origDelta.isModify()) {
                // We need to convert modify delta to ADD
                ObjectDelta<ShadowType> addDelta = PrismContext.get().deltaFactory().object().create(getObjectTypeClass(),
                    ChangeType.ADD);
                addDelta.setObjectToAdd(
                        getCompositeObjectDefinitionRequired()
                                .createBlankShadowWithTag(key.getTag())
                                .getPrismObject());
                if (origDelta != null) {
                    addDelta.merge(origDelta);
                }
                return addDelta;
            }
        } else if (policyDecision == SynchronizationPolicyDecision.KEEP) {
            // (Almost) any delta is OK
            if (exists && ObjectDelta.isAdd(origDelta)) {
                LOGGER.trace("Projection exists and we try to create it anew. This probably means that the primary ADD delta"
                        + " should be ignored. Using secondary delta only. Current delta is:\n{}\nSecondary delta that will"
                        + " be used instead is:\n{}", origDelta.debugDumpLazily(), DebugUtil.debugDumpLazily(getSecondaryDelta()));
                origDelta = getSecondaryDelta();
            }
        } else if (policyDecision == SynchronizationPolicyDecision.DELETE) {
            ObjectDelta<ShadowType> deleteDelta = PrismContext.get().deltaFactory().object().create(getObjectTypeClass(),
                ChangeType.DELETE);
            String oid = getOid();
            if (oid == null) {
                throw new IllegalStateException(
                        "Internal error: account context OID is null during attempt to create delete secondary delta; context="
                                +this);
            }
            deleteDelta.setOid(oid);
            return deleteDelta;
        } else {
            // This is either UNLINK or null, both are in fact the same as KEEP
            // Any delta is OK
        }
        if (origDelta != null && origDelta.isImmutable()) {
            // E.g. locked primary delta.
            // We need modifiable delta for execution, e.g. to set metadata, oid and so on.
            return origDelta.clone();
        } else {
            return origDelta;
        }
    }

    public void checkConsistenceIfNeeded() {
        if (InternalsConfig.consistencyChecks) {
            checkConsistence("", lensContext.isFresh(), lensContext.isForce());
        }
    }

    @Override
    public void checkConsistence(String contextDesc) {
        // Intentionally not calling super.checkConsistence here. This is done in the parameterized method below.
        checkConsistence(contextDesc, true, false);
    }

    public void checkConsistence(String contextDesc, boolean fresh, boolean force) {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE) {
            // No not check these. they may be quite wild.
            return;
        }

        state.checkConsistence(this, contextDesc);

        // Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
        state.checkSecondaryDeltaConsistence(isRequireSecondaryDeltaOid(), this, contextDesc);

        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
            return;
        }
        if (fresh && !force && !isGone()) {
            if (resource == null) {
                throw new IllegalStateException("Null resource in " + this + in(contextDesc));
            }
            if (key == null) {
                throw new IllegalStateException("Null resource account type in " + this + in(contextDesc));
            }
        }
        if (syncDelta != null) {
            try {
                syncDelta.checkConsistence(true, true, true, ConsistencyCheckScope.THOROUGH);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage() + "; in " + getElementDesc() + " sync delta in " + this + in(contextDesc), e);
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + "; in " + getElementDesc() + " sync delta in " + this + in(contextDesc), e);
            }
        }
    }

    @Override
    void doExtraObjectConsistenceCheck(@NotNull PrismObject<ShadowType> object, String elementDesc, String contextDesc) {
        ShadowAttributesContainer attributesContainer = ShadowUtil.getAttributesContainer(object);
        if (attributesContainer != null) {
            ResourceType resource = getResource();
            if (resource != null) {
                for (var attribute : attributesContainer.getAttributes()) {
                    QName attrName = attribute.getElementName();
                    if (SchemaConstants.NS_ICF_SCHEMA.equals(attrName.getNamespaceURI())) {
                        continue;
                    }
                    if (MidPointConstants.NS_RI.equals(attrName.getNamespaceURI())) {
                        continue;
                    }
                    String desc = elementDesc + " in " + this + (contextDesc == null ? "" : " in " + contextDesc);
                    throw new IllegalStateException("Invalid namespace for attribute " + attrName + " in " + desc);
                }
            }
        }
    }

    private boolean isRequireSecondaryDeltaOid() {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD ||
                synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN ||
                synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE) {
            return false;
        }
        if (isHigherOrder()) {
            // These may not have the OID yet
            return false;
        }
        return state.getPrimaryDelta() == null;
    }

    @Override
    public void cleanup() {
        checkIfShouldArchive();

        // We will clean up this projection context fully only if there's a chance we will touch it again.
        if (!completed) {
            synchronizationPolicyDecision = null;
            assigned = null;
            active = false;
        }

        // However, selected items are still cleaned up, in order to preserve existing behavior.
        // This might be important e.g. for inbound mappings that take previous deltas into account.
        state.clearSecondaryDelta();

//      isLegal = null;
//      isLegalOld = null;
//      isAssignedOld = false;  // ??? [med]
    }

    @Override
    public void normalize() {
        super.normalize();
        if (syncDelta != null) {
            syncDelta.normalize();
        }
    }

    private void checkIfShouldArchive() {
        if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE ||
                synchronizationPolicyDecision == SynchronizationPolicyDecision.UNLINK) {
            toBeArchived = true;
        } else if (synchronizationPolicyDecision != null) {
            toBeArchived = false;
        }
    }

    @Override
    public void adopt(PrismContext prismContext) throws SchemaException {
        super.adopt(prismContext);
        if (syncDelta != null) {
            prismContext.adopt(syncDelta);
        }
    }

    public LensProjectionContext clone(LensContext<? extends ObjectType> lensContext) {
        LensProjectionContext clone = new LensProjectionContext(lensContext, key, state);
        copyValues(clone);
        return clone;
    }

    private void copyValues(LensProjectionContext clone) {
        super.copyValues(clone);
        // do NOT clone transient values such as accountConstructionDeltaSetTriple
        // these are not meant to be cloned and they are also not directly cloneable
        clone.dependencies = this.dependencies;
        clone.doReconciliation = this.doReconciliation;
        clone.fullShadow = this.fullShadow;
        clone.assigned = this.assigned;
        clone.assignedOld = this.assignedOld;
        clone.evaluatedPlainConstruction = this.evaluatedPlainConstruction;
        clone.synchronizationPolicyDecision = this.synchronizationPolicyDecision;
        clone.resource = this.resource;
        clone.key = this.key;
        clone.squeezedAttributes = cloneSqueezedAttributes();
        if (this.syncDelta != null) {
            clone.syncDelta = this.syncDelta.clone();
        }
        clone.wave = this.wave;
        clone.synchronizationSource = this.synchronizationSource;
    }

    private Map<QName, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> cloneSqueezedAttributes() {
        if (squeezedAttributes == null) {
            return null;
        }
        Map<QName, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> clonedMap = new HashMap<>();
        for (var entry: squeezedAttributes.entrySet()) {
            clonedMap.put(entry.getKey(), entry.getValue().clone(ItemValueWithOrigin::clone));
        }
        return clonedMap;
    }

    @Override
    public void checkEncrypted() {
        super.checkEncrypted();
        if (syncDelta != null) {
            CryptoUtil.checkEncrypted(syncDelta);
        }
    }

    @Override
    public @NotNull String getHumanReadableName() {
        if (humanReadableName == null) {
            var sb = new StringBuilder();
            sb.append("shadow(");
            String humanReadableAccountIdentifier = getHumanReadableIdentifier();
            if (StringUtils.isEmpty(humanReadableAccountIdentifier)) {
                sb.append("no ID");
            } else {
                sb.append("ID ");
                sb.append(humanReadableAccountIdentifier);
            }
            sb.append(", ");
            var typeId = key.getTypeIdentification();
            if (typeId != null) {
                sb.append(typeId);
            } else {
                sb.append("untyped (");
                sb.append(PrettyPrinter.prettyPrint(getObjectClassName()));
                sb.append(")");
            }
            sb.append(", ");
            if (key.getOrder() != 0) {
                sb.append("order ").append(key.getOrder()).append(", ");
            }
            sb.append(getResource());
            sb.append(")");
            humanReadableName = sb.toString();
        }
        return humanReadableName;
    }

    private void invalidateHumanReadableName() {
        humanReadableName = null;
    }

    private String getHumanReadableIdentifier() {
        PrismObject<ShadowType> object = getObjectAny();
        if (object == null) {
            return null;
        }
        if (object.canRepresent(ShadowType.class)) { // probably always the case
            Collection<ShadowSimpleAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(object);
            if (identifiers == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            Iterator<ShadowSimpleAttribute<?>> iterator = identifiers.iterator();
            while (iterator.hasNext()) {
                ShadowSimpleAttribute<?> id = iterator.next();
                sb.append(id.toHumanReadableString());
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            return sb.toString();
        } else {
            return object.toString();
        }
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        SchemaDebugUtil.indentDebugDump(sb, indent);
        sb.append("PROJECTION ");
        getObjectTypeClass();
        sb.append(getObjectTypeClass().getSimpleName());
        sb.append(" ");
        sb.append(getKey());
        if (resource != null) {
            sb.append(" : ");
            sb.append(resource.getName().getOrig());
        }
        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("OID: ").append(getOid());
        sb.append(", wave ").append(wave);
        if (fullShadow) {
            sb.append(", full");
        } else {
            sb.append(", shadow-only (not full) ");
            var current = getObjectCurrent();
            if (current != null) {
                ResourceObjectDefinition definition;
                try {
                    definition = getStructuralObjectDefinition();
                    if (definition == null) {
                        sb.append("(no definition)");
                    } else {
                        sb.append("(caching status: ");
                        sb.append(
                                ShadowUtil.getShadowCachedStatus(
                                        current, ModelBeans.get().clock.currentTimeXMLGregorianCalendar()));
                        sb.append(")");
                    }
                } catch (Throwable t) {
                    sb.append("(definition/freshness error: ").append(t.getMessage()).append(")");
                }
            } else {
                sb.append("(no current)");
            }
        }
        sb.append(", exists=").append(exists);
        if (!shadowExistsInRepo) {
            sb.append(" (shadow not in repo)");
        }
        sb.append(", assigned=").append(assignedOld).append("->").append(assigned);
        sb.append(", active=").append(active);
        sb.append(", legal=").append(legalOld).append("->").append(legal);
        sb.append(", recon=").append(doReconciliation);
        sb.append(", canProject=").append(canProject);
        sb.append(", syncIntent=").append(getSynchronizationIntent());
        sb.append(", decision=").append(synchronizationPolicyDecision);
        sb.append(", state=").append(getCurrentShadowState());
        if (!isFresh()) {
            sb.append(", NOT FRESH");
        } else {
            sb.append(", fresh");
        }
        if (reloadNeeded) {
            sb.append(", reload needed");
        }
        if (isGone()) {
            sb.append(", GONE");
        }
        if (syncAbsoluteTrigger) {
            sb.append(", SYNC TRIGGER");
        }
        if (getIteration() != 0) {
            sb.append(", iteration=").append(getIteration()).append(" (").append(getIterationToken()).append(")");
        }
        if (completed) {
            sb.append(", completed");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("secondary delta"), getSecondaryDelta(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("sync delta"), syncDelta, indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);

        if (showTriples) {

            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("evaluatedAssignedConstructionDeltaSetTriple"), evaluatedAssignedConstructionDeltaSetTriple, indent + 1);

            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("plain (schemaHandling) construction"), evaluatedPlainConstruction, indent + 1);

            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed attributes"), squeezedAttributes, indent + 1);

            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed associations"), squeezedAssociations, indent + 1);

            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed auxiliary object classes"), squeezedAuxiliaryObjectClasses, indent + 1);

            if (DEBUG_DUMP_DEPENDENCIES) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent);
                sb.append(getDebugDumpTitle("dependencies\n"));
                sb.append(DebugUtil.debugDump(dependencies, indent + 1));
            }
        }

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("composite object definition"), String.valueOf(compositeObjectDefinition), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("auxiliary object class definition"), String.valueOf(auxiliaryObjectClassDefinitions), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Policy rules context", policyRulesContext, indent + 1);

        return sb.toString();
    }

    public ShadowLifecycleStateType getCurrentShadowState() {
        PrismObject<ShadowType> current = getObjectCurrent();
        return current != null ? current.asObjectable().getShadowLifecycleState() : null;
    }

    @Override
    protected String getElementDefaultDesc() {
        return "projection";
    }

    @Override
    public String toString() {
        try {
            return getHumanReadableName();
        } catch (Throwable t) {
            return simpleToString() + " (error getting human readable name: " + MiscUtil.getClassWithMessage(t) + ")";
        }
    }

    private @NotNull String simpleToString() {
        return "LensProjectionContext(shadow %s of %s%s)".formatted(
                getOid(), key, resource != null ? " (" + resource.getName() + ")" : "");
    }

    /**
     * Return a human readable name of the projection object suitable for logs.
     */
    public String toHumanReadableString() {
        if (humanReadableString == null) {
            if (resource != null) {
                humanReadableString = "("+getKindValue(key.getKind()) + " ("+ key.getIntent()+") on " + resource + ")";
            } else {
                humanReadableString = "("+getKindValue(key.getKind()) + " ("+ key.getIntent()+") on " + key.getResourceOid() + ")";
            }
        }
        return humanReadableString;
    }

    public String getHumanReadableKind() {
        return getKindValue(key.getKind());
    }

    private String getKindValue(ShadowKindType kind) {
        if (kind == null) {
            return "null";
        }
        return kind.value();
    }

    @Override
    protected String getElementDesc() {
        return getKindValue(key.getKind());
    }

    LensProjectionContextType toBean(LensContext.ExportType exportType) throws SchemaException {
        LensProjectionContextType bean = new LensProjectionContextType();
        super.storeIntoBean(bean, exportType);
        bean.setWave(wave);
        bean.setCompleted(completed);
        bean.setResourceShadowDiscriminator(key.toResourceShadowDiscriminatorType());
        bean.setFullShadow(fullShadow);
        bean.setIsExists(exists);
        bean.setSynchronizationIntent(synchronizationIntent != null ? synchronizationIntent.toSynchronizationIntentType() : null);
        bean.setSynchronizationPolicyDecision(synchronizationPolicyDecision != null ? synchronizationPolicyDecision.toSynchronizationPolicyDecisionType() : null);
        bean.setDoReconciliation(doReconciliation);
        bean.setSynchronizationSituationDetected(synchronizationSituationDetected);
        bean.setSynchronizationSituationResolved(synchronizationSituationResolved);
        if (exportType != LensContext.ExportType.MINIMAL) {
            bean.setSyncDelta(syncDelta != null ? DeltaConvertor.toObjectDeltaType(syncDelta) : null);
            bean.setIsAssigned(assigned);
            bean.setIsAssignedOld(assignedOld);
            bean.setIsActive(active);
            bean.setIsLegal(legal);
            bean.setIsLegalOld(legalOld);
            if (exportType != LensContext.ExportType.REDUCED && projectionSecurityPolicy != null) {
                ObjectReferenceType secRef = new ObjectReferenceType();
                secRef.asReferenceValue().setObject(projectionSecurityPolicy.asPrismObject());
                bean.setProjectionSecurityPolicyRef(secRef);
            }
            bean.setSyncAbsoluteTrigger(syncAbsoluteTrigger);
        }
        return bean;
    }

    @NotNull
    static LensProjectionContext fromLensProjectionContextBean(LensProjectionContextType bean,
            @NotNull LensContext<?> lensContext, Task task, OperationResult result) throws SchemaException {

        String objectTypeClassString = bean.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensProjectionContextType");
        }
        ProjectionContextKey key =
                ProjectionContextKey.fromBean(
                        bean.getResourceShadowDiscriminator());

        LensProjectionContext ctx = new LensProjectionContext(lensContext, key);

        PrismContext prismContext = PrismContext.get();
        ctx.retrieveFromLensElementContextBean(bean, task, result);
        if (bean.getSyncDelta() != null) {
            ctx.syncDelta = DeltaConvertor.createObjectDelta(bean.getSyncDelta(), prismContext);
        } else {
            ctx.syncDelta = null;
        }
        ctx.wave = bean.getWave() != null ? bean.getWave() : 0;
        ctx.completed = BooleanUtils.isTrue(bean.isCompleted());
        ctx.fullShadow = bean.isFullShadow() != null ? bean.isFullShadow() : false;
        ctx.assigned = bean.isIsAssigned() != null ? bean.isIsAssigned() : false;
        ctx.assignedOld = bean.isIsAssignedOld() != null ? bean.isIsAssignedOld() : false;
        ctx.active = bean.isIsActive() != null ? bean.isIsActive() : false;
        ctx.legal = bean.isIsLegal();
        ctx.legalOld = bean.isIsLegalOld();
        ctx.exists = bean.isIsExists() != null ? bean.isIsExists() : false;
        ctx.synchronizationIntent = SynchronizationIntent.fromSynchronizationIntentType(bean.getSynchronizationIntent());
        ctx.synchronizationPolicyDecision =
                SynchronizationPolicyDecision.fromSynchronizationPolicyDecisionType(bean.getSynchronizationPolicyDecision());
        ctx.doReconciliation = bean.isDoReconciliation() != null ? bean.isDoReconciliation() : false;
        ctx.synchronizationSituationDetected = bean.getSynchronizationSituationDetected();
        ctx.synchronizationSituationResolved = bean.getSynchronizationSituationResolved();
        ObjectReferenceType projectionSecurityPolicyRef = bean.getProjectionSecurityPolicyRef();
        if (projectionSecurityPolicyRef != null) {
            ctx.projectionSecurityPolicy = (SecurityPolicyType) projectionSecurityPolicyRef.getObjectable();
        }
        ctx.syncAbsoluteTrigger = bean.isSyncAbsoluteTrigger();

        return ctx;
    }

    /**
     * Sets the full shadow flag, based on the situation (including the fetch result).
     */
    public void determineFullShadowFlag(ShadowType loadedShadow) {
        setFullShadow(
                computeFullShadowFlag(loadedShadow));
    }

    private boolean computeFullShadowFlag(ShadowType loadedShadow) {
        if (ShadowUtil.isDead(loadedShadow) || !ShadowUtil.isExists(loadedShadow)) {
            LOGGER.trace("Shadow is dead or non-existent -> fullShadow = false");
            return false;
        }
        // Maintenance mode means the fetchResult is PARTIAL_ERROR (since 4.7).
        var status = ObjectTypeUtil.getFetchStatus(loadedShadow);
        boolean full = // TODO what about other kinds of status? [e.g. in-progress]
                status != OperationResultStatusType.PARTIAL_ERROR
                        && status != OperationResultStatusType.FATAL_ERROR;
        LOGGER.trace("Fetch result status is {} -> fullShadow = {}", status, full);
        return full;
    }

    public boolean isToBeArchived() {
        return toBeArchived;
    }

    public String getResourceOid() {
        return resource != null ? resource.getOid() : key.getResourceOid();
    }

    public @NotNull String getResourceOidRequired() {
        return Objects.requireNonNull(getResourceOid(), () -> "No resource OID in " + this);
    }

    ResourceObjectVolatilityType getVolatility() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition structuralObjectClassDefinition = getStructuralObjectDefinition();
        return structuralObjectClassDefinition != null ? structuralObjectClassDefinition.getVolatility() : null;
    }

    @Override
    public void forEachDelta(Consumer<ObjectDelta<ShadowType>> consumer) {
        super.forEachDelta(consumer);
        if (syncDelta != null) {
            consumer.accept(syncDelta);
        }
    }

    PolyString resolveNameIfKnown(Class<? extends ObjectType> objectClass, String oid) {
        if (ResourceType.class.equals(objectClass)) {
            if (resource != null && oid.equals(resource.getOid())) {
                return PolyString.toPolyString(resource.getName());
            }
        } else if (ShadowType.class.equals(objectClass)) {
            PrismObject<ShadowType> object = getObjectAny();
            if (object != null && oid.equals(object.getOid())) {
                if (object.getName() != null) {
                    return object.getName();
                } else {
                    try {
                        return ShadowUtil.determineShadowName(object);
                    } catch (SchemaException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine shadow name for {}", e, object);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public String getResourceName() {
        ResourceType resource = getResource();
        return resource != null ? PolyString.getOrig(resource.getName()) : getResourceOid();
    }

    public void setSynchronizationSource(boolean synchronizationSource) {
        this.synchronizationSource = synchronizationSource;
    }

    public boolean isSynchronizationSource() {
        return synchronizationSource;
    }

    public String getDescription() {
        if (resource != null) {
            return resource + "("+ key.getIntent()+")";
        } else {
            return key.toString();
        }
    }

    /**
     * Returns true if the projection is "current" i.e. it was not completed and its wave is
     * either not yet determined or equal to the current projection wave.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Experimental
    public boolean isCurrentForProjection() {
        return !completed
                && (!hasProjectionWave() || isCurrentProjectionWave());
    }

    public boolean hasProjectionWave() {
        return wave != -1;
    }

    /** Assumes the wave is computed. */
    public boolean isCurrentProjectionWave() {
        stateCheck(hasProjectionWave(), "Projection wave was not yet determined for %s", this);
        return wave == getLensContext().getProjectionWave();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public void rot() {
        super.rot();
        setFullShadow(false);
        cachedValueMetadata = null;
    }

    boolean rotAfterExecution() {
        if (wave != lensContext.getExecutionWave()) {
            LOGGER.trace("Context rot: projection {} NOT rotten because of wrong wave number", this);
            return false;
        }

        // We do not care if the delta execution was successful. We want to rot the context even in the case of failure.
        ObjectDelta<ShadowType> execDelta = getExecutedDelta(lastChangeExecutionResult);
        if (!isShadowDeltaSignificant(execDelta)) {
            LOGGER.trace("Context rot: projection {} NOT rotten because there's no significant delta", this);
            return false;
        }

        LOGGER.debug("Context rot: projection {} rotten because of executable delta {}", this, execDelta);

        rot();

        if (modifiesCachedAttributes(execDelta)) {
            setReloadNeeded();
        }

        // Propagate to higher-order projections
        for (LensProjectionContext relCtx : lensContext.findRelatedContexts(this)) {
            relCtx.rot();
        }

        return true;
    }

    private boolean modifiesCachedAttributes(ObjectDelta<ShadowType> execDelta) {
        if (!ObjectDelta.isModify(execDelta)) {
            return false;
        }
        ResourceObjectDefinition def;
        try {
            def = getStructuralObjectDefinition();
        } catch (SchemaException | ConfigurationException e) {
            LoggingUtils.logUnexpectedException(
                    LOGGER, "Got unexpected exception while getting structural object definition for {}; "
                            + "assuming that shadow need to be reloaded", e, this);
            return true;
        }
        if (def == null) {
            LOGGER.debug("No object definition for {}, assuming that shadow need to be reloaded", this);
            return true;
        }
        for (var modifiedItemPath : execDelta.getModifiedItems()) {
            if (def.isEffectivelyCached(modifiedItemPath)) {
                LOGGER.trace("Modified item '{}' is effectively cached -> shadow needs to be reloaded: {}",
                        modifiedItemPath, this);
                return true;
            }
        }
        LOGGER.trace("Found no modified items that are effectively cached -> shadow does not need to be reloaded: {}", this);
        return false;
    }

    private void setReloadNeeded() {
        reloadNeeded = true;
    }

    public void setReloadNotNeeded() {
        reloadNeeded = false;
    }

    public boolean isReloadNeeded() {
        return reloadNeeded;
    }

    private <P extends ObjectType> boolean isShadowDeltaSignificant(ObjectDelta<P> delta) {
        if (delta == null || delta.isEmpty()) {
            return false;
        }
        // The "hasResourceModifications" check seems to be correct, but makes TestImportRecon failing because of subtle
        // interactions between "dummy resource" and "dummy resource lime". No time to fix this now. So, here
        // we temporarily returned the pre-4.7 (most probably not quite correct) behavior.
        return delta.isAdd()
                || delta.isDelete()
                || ShadowUtil.hasAttributeModifications(delta.getModifications());
                //|| ShadowUtil.hasResourceModifications(delta.getModifications());
    }

    public ValueMetadataType getCachedValueMetadata() {
        return cachedValueMetadata;
    }

    public void setCachedValueMetadata(ValueMetadataType cachedValueMetadata) {
        this.cachedValueMetadata = cachedValueMetadata;
    }

    public boolean isHigherOrder() {
        return key.getOrder() > 0;
    }

    public boolean isBroken() {
        return synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN;
    }

    public boolean isIgnored() {
        return synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE;
    }

    /**
     * We set reconciliation:=TRUE for volatile accounts.
     *
     * This is to ensure that the changes on such resources will be read back to midPoint.
     * (It is a bit of hack but it looks OK.) See also MID-2436 - volatile objects.
     */
    public void setDoReconciliationFlagIfVolatile() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition = getStructuralDefinitionIfNotBroken();
        if (objectDefinition != null
                && objectDefinition.getVolatility() == ResourceObjectVolatilityType.UNPREDICTABLE
                && !isDoReconciliation()) {
            LOGGER.trace("Resource object volatility is UNPREDICTABLE => setting doReconciliation to TRUE for {}", getKey());
            setDoReconciliation(true);
        }
    }

    /** TODO */
    public void rotWithDeltaDeletion() {
        rot();
        state.clearSecondaryDelta();
    }

    /**
     * Returns true if there is any context that depends on us.
     * (Note that "dependency source" means the context that depends on the "dependency target". We are the target here.)
     */
    public boolean hasDependentContext() throws SchemaException, ConfigurationException {
        for (LensProjectionContext candidateSource : lensContext.getProjectionContexts()) {
            for (ResourceObjectTypeDependencyType dependency : candidateSource.getDependencies()) {
                if (isDependencyTarget(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isClassified() {
        return key.isClassified();
    }

    public int getOrder() {
        return key.getOrder();
    }

    public String getTag() {
        return key.getTag();
    }

    /** Use with care! */
    @Experimental
    public boolean isDefaultForKind(@NotNull ShadowKindType kind) {
        ResourceObjectDefinition definition;
        try {
            definition = getStructuralObjectDefinition();
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.debug("Couldn't determine structural object definition for {}; considering it as not being default for {}",
                    this, kind, e);
            return false;
        }
        if (definition != null) {
            return definition.isDefaultFor(kind);
        } else {
            LOGGER.debug("No definition: considering {} as not being default for {}", this, kind);
            return false;
        }
    }

    /**
     * Returns configured focus archetype OID corresponding to resource object type of this projection (if there is one).
     */
    public @Nullable String getConfiguredFocusArchetypeOid() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition = getStructuralObjectDefinition();
        if (objectDefinition == null) {
            return null;
        }
        return objectDefinition.getFocusSpecification().getArchetypeOid();
    }

    /** Returns focus identity source information for data created from this projection. */
    public @NotNull FocusIdentitySourceType getFocusIdentitySource() {
        FocusIdentitySourceType source = new FocusIdentitySourceType()
                .resourceRef(getResourceOid(), ResourceType.COMPLEX_TYPE)
                .kind(key.getKind())
                .intent(key.getIntent())
                .tag(key.getTag());
        String shadowOid = getOid();
        if (shadowOid != null) {
            source.shadowRef(shadowOid, ShadowType.COMPLEX_TYPE);
        }
        // TODO originRef
        return source;
    }

    public boolean isInMaintenance() {
        return ResourceTypeUtil.isInMaintenance(resource);
    }

    /** Is the resource or object class/type visible for the current task execution mode? */
    public boolean isVisible() throws SchemaException, ConfigurationException {
        if (resource == null) {
            throw new IllegalStateException("No resource"); // temporary
        }
        return SimulationUtil.isVisible(resource, getStructuralObjectDefinition(), getTaskExecutionMode());
    }

    public boolean hasResourceAndIsVisible() throws SchemaException, ConfigurationException {
        return resource != null
                && SimulationUtil.isVisible(resource, getStructuralObjectDefinition(), getTaskExecutionMode());
    }

    private @NotNull TaskExecutionMode getTaskExecutionMode() {
        return lensContext.getTaskExecutionMode();
    }

    public boolean hasResource() {
        return resource != null;
    }

    public boolean isAdministrativeStatusSupported() throws SchemaException, ConfigurationException {
        return resource != null
                && CapabilityUtil.isActivationStatusCapabilityEnabled(resource, getStructuralObjectDefinition());
    }

    @Override
    @NotNull ItemChangeApplicationModeConfiguration createItemChangeApplicationModeConfiguration()
            throws SchemaException, ConfigurationException {
        return ItemChangeApplicationModeConfiguration.of(getCompositeObjectDefinition());
    }

    /**
     * Returns the (estimated) state before the simulated operation began.
     *
     * Ideally, we would like to return "old object", just as we do in
     * {@link LensFocusContext#getStateBeforeSimulatedOperation()}.
     *
     * Unfortunately, the "old object" usually contains only the repo shadow, not the state from the resource
     * (called full shadow). The reason is that when the full shadow is is loaded during processing, it is put into "current",
     * not into "old" object.
     *
     * Hence it's best to take "current object" for projections.
     * Unfortunately, this object is changed when the simulated operation is executed.
     * Therefore, we store its copy before we apply the operation.
     *
     * TODO: Maybe we should get rid of all this hackery, and explicitly remember the first full shadow loaded.
     *  But it is not trivial to do so: there are many places where full shadow is (tried to be) loaded.
     *  And it is not always clear if we get the full shadow or not. So it is doable, but definitely not simple.
     */
    @Override
    public PrismObject<ShadowType> getStateBeforeSimulatedOperation() {
        return state.getCurrentShadowBeforeSimulatedDeltaExecution();
    }

    @Override
    public void simulateDeltaExecution(@NotNull ObjectDelta<ShadowType> delta) throws SchemaException {
        super.simulateDeltaExecution(delta);
        if (delta.isDelete()) {
            // This is something that is normally done by the context loaded.
            // However, in simulation mode the context is not rotten, so no re-loading is done.
            // (Or, should we do some fake loading instead? Not sure, probably this is the best approach.)
            setFullShadow(false);
            setShadowExistsInRepo(false);
        }
    }

    boolean isProjectionRecomputationRequested() {
        return ChangeExecutionResult.isProjectionRecomputationRequested(lastChangeExecutionResult);
    }

    @Nullable
    private ObjectOperationPolicyType operationPolicy(OperationResult result) throws ConfigurationException {
        var object = getObjectNewOrCurrentOrOld();
        if (object == null) {
            return null;
        }
        return ObjectOperationPolicyHelper.get().getEffectivePolicy(
                object.asObjectable(),
                getTaskExecutionMode(),
                result);
    }

    public boolean isMarkedReadOnly(OperationResult result) throws ConfigurationException {
        var policy = operationPolicy(result);
        if (policy == null) {
            return false;
        }
        return isAddDisabled(policy) && isModifyDisabled(policy) && isDeleteDisabled(policy);
    }

    public boolean areInboundSyncMappingsDisabled(OperationResult result) throws ConfigurationException {
        var policy = operationPolicy(result);
        if (policy == null) {
            return false;
        }
        return areSyncInboundMappingsDisabled(policy); // TODO severity
    }

    public boolean isOutboundSyncDisabled(OperationResult result) throws ConfigurationException {
        return Objects.requireNonNullElse(
                isOutboundSyncDisabledNullable(result),
                false);
    }

    public Boolean isOutboundSyncDisabledNullable(OperationResult result) throws ConfigurationException {
        var policy = operationPolicy(result);
        if (policy == null) {
            return null;
        }
        return isSyncOutboundDisabled(policy); // TODO severity
    }

    /**
     * Returns the projection contexts bound to this one via data dependency that are known (or supposed) to be modified.
     */
    public List<LensProjectionContext> getModifiedDataBoundDependees() throws SchemaException, ConfigurationException {
        List<LensProjectionContext> matching = new ArrayList<>();
        for (ResourceObjectTypeDependencyType dependencyBean : getDependencies()) {
            if (ResourceObjectTypeDependencyTypeUtil.isDataBindingPresent(dependencyBean)) {
                lensContext.getProjectionContexts().stream()
                        .filter(ctx -> ctx != this)
                        .filter(ctx -> ctx.isDependencyTarget(dependencyBean))
                        .filter(ctx -> ctx.hasOnResourceModificationAttempt())
                        .forEach(matching::add);
            }
        }
        return matching;
    }

    /**
     * Returns true if there was an operation (or attempted operation) dealing with the on-resource state.
     *
     * We consider ADD and DELETE operations as such, mainly to be sure.
     * For MODIFY operations we check the individual modifications.
     */
    private boolean hasOnResourceModificationAttempt() {
        for (LensObjectDeltaOperation<ShadowType> odo : getExecutedDeltas()) {
            var delta = odo.getObjectDelta();
            if (ObjectDelta.isAdd(delta)
                    || ObjectDelta.isDelete(delta)
                    || ObjectDelta.isModify(delta) && ShadowUtil.hasResourceModifications(delta.getModifications())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCachedShadowsUseAllowed() throws SchemaException, ConfigurationException {
        return getCachedShadowsUse() != CachedShadowsUseType.USE_FRESH;
    }

    public @NotNull CachedShadowsUseType getCachedShadowsUse() throws SchemaException, ConfigurationException {
        // From model execution options
        var fromOptions = ModelExecuteOptions.getCachedShadowsUse(lensContext.getOptions());
        if (fromOptions != null) {
            return fromOptions;
        }
        // From the resource (if accessible)
        var objectDefinition = getStructuralObjectDefinition();
        if (objectDefinition != null) {
            var fromResource = objectDefinition.getEffectiveShadowCachingPolicy().getDefaultCacheUse();
            if (fromResource != null) {
                return fromResource;
            }
        }
        // The default
        return CachedShadowsUseType.USE_FRESH;
    }

    public boolean hasLowerOrderContext() {
        return lensContext.hasLowerOrderContextThan(this);
    }
}
