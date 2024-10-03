/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.TaskUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessingContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.isSyncInboundDisabled;

import static java.util.Objects.requireNonNull;

/**
 * Context of the synchronization operation. It is created in the early stages of {@link ResourceObjectShadowChangeDescription}
 * progressing in {@link SynchronizationServiceImpl}.
 *
 * @param <F> Type of the matching focus object
 */
public abstract class SynchronizationContext<F extends FocusType>
        implements SingleShadowInboundsProcessingContext<F>, ResourceObjectProcessingContext {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContext.class);

    @VisibleForTesting
    private static boolean skipMaintenanceCheck;

    @NotNull private final ResourceObjectShadowChangeDescription change;

    /**
     * Normally, this is shadowed resource object, i.e. shadow + attributes (simply saying).
     * In the case of object deletion, the last known shadow can be used, i.e. without attributes.
     *
     * See {@link ResourceObjectShadowChangeDescription#getShadowedResourceObject()}.
     */
    @NotNull private final ShadowType shadowedResourceObject;

    /**
     * The (cloned) state of {@link #shadowedResourceObject} at the moment when this context is created.
     * To be independent of any deltas applied to the object during processing.
     */
    @NotNull private final ShadowType shadowedResourceObjectBefore;

    /** Original delta that triggered this synchronization. (If known.) */
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;

    @NotNull private final ResourceType resource;

    /** Current system configuration */
    private final SystemConfigurationType systemConfiguration;

    /** TODO */
    private final String channel;

    private final ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

    @NotNull private final Task task;

    @NotNull private final ExecutionModeType executionMode;

    /** Kind+intent, if known. */
    @Nullable private final ResourceObjectTypeIdentification typeIdentification;

    @Override
    public @NotNull InboundMappingContextSpecification getMappingContextSpecification() {
        return new InboundMappingContextSpecification(typeIdentification, null, tag);
    }

    /**
     * Definition of corresponding object (currently found by kind+intent).
     */
    @Nullable protected final ResourceObjectDefinition resourceObjectDefinition;

    @Nullable private final SynchronizationPolicy synchronizationPolicy;

    /**
     * Preliminary focus object - a result pre pre-mappings execution.
     *
     * Lazily created on the first getter call.
     */
    private F preFocus;

    /**
     * Correlation configuration can be present also in the object template.
     * Therefore, here we provide the one (if applicable).
     */
    private ObjectTemplateType objectTemplateForCorrelation;

    /** Owner that was found to be linked (in repo) to the shadow being synchronized. */
    private F linkedOwner;

    /** Owner that was found by synchronization sorter or correlation expression(s). */
    private F correlatedOwner;

    /** Situation determined by the sorter or the synchronization service. */
    private SynchronizationSituationType situation;

    /**
     * Correlation context - in case the correlation was run.
     * For some correlators it contains the correlation state (to be stored in the shadow).
     */
    private CorrelationContext correlationContext;

    private final String tag;

    private boolean shadowExistsInRepo = true;

    /**
     * True if we want to update shadow classification even if the shadow is already classified.
     * It is used in connection with synchronization sorter - its answers are always applied.
     */
    private final boolean forceClassificationUpdate;

    @NotNull private final PrismContext prismContext = PrismContext.get();

    /** TODO maybe will be removed */
    @Experimental
    private final String itemProcessingIdentifier;

    /**
     * Helper object that updates the shadow (in memory and in repo) with correlation and/or synchronization metadata.
     */
    @NotNull private final ShadowUpdater updater;

    public SynchronizationContext(
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @Nullable ResourceObjectDefinition objectDefinition,
            @Nullable SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
            @Nullable String tag) {
        this.change = change;
        this.shadowedResourceObject = processingContext.getShadowRequired();
        this.shadowedResourceObjectBefore = this.shadowedResourceObject.clone();
        this.resourceObjectDelta = processingContext.getResourceObjectDelta();
        this.resource = processingContext.getResource();
        this.channel = processingContext.getChannel();
        this.systemConfiguration = processingContext.getSystemConfiguration();
        this.task = processingContext.getTask();
        this.executionMode = TaskUtil.getExecutionMode(task);
        this.typeIdentification = typeIdentification;
        this.resourceObjectDefinition = objectDefinition;
        this.synchronizationPolicy = synchronizationPolicy;
        this.tag = tag;
        this.itemProcessingIdentifier = change.getItemProcessingIdentifier();
        if (sorterResult != null) {
            this.forceClassificationUpdate = true;
            LOGGER.trace("Setting synchronization situation to synchronization context: {}",
                    sorterResult.getSynchronizationSituation());
            situation = sorterResult.getSynchronizationSituation();
            LOGGER.trace("Setting correlated owner in synchronization context: {}", sorterResult.getOwner());
            //noinspection unchecked
            setCorrelatedOwner((F) sorterResult.getOwner());
        } else {
            this.forceClassificationUpdate = false;
        }
        this.updater = new ShadowUpdater(this);
    }

    boolean isSynchronizationEnabled() {
        return synchronizationPolicy != null
                && synchronizationPolicy.isSynchronizationEnabled();
    }

    boolean isMarkedSkipSynchronization(OperationResult result) throws ConfigurationException {
        var policy = shadowedResourceObject.getEffectiveOperationPolicy();
        // Policy should not be null if was provided by provisioning-impl. TODO Remove this check later
        if (policy == null) {
            LOGGER.warn("Missing effective operation policy in {}.", shadowedResourceObject);
            policy = ObjectOperationPolicyHelper.get().computeEffectivePolicy(
                    shadowedResourceObject, task.getExecutionMode(), result);
        }
        return isSyncInboundDisabled(policy);
    }

    public boolean isProtected() {
        return BooleanUtils.isTrue(shadowedResourceObject.isProtectedObject());
    }

    /**
     * Returns the identification of the (determined) type definition - or null if the type is not known.
     *
     * Note that it's not necessary to look at the shadow kind/intent if this method returns `null`, because this type
     * is derived directly from the values in the shadow. It can be even more precise, because the shadow may be
     * unclassified when this context is created.
     */
    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    public CorrelationContext getCorrelationContext() {
        return correlationContext;
    }

    public void setCorrelationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
    }

    @Override
    public @NotNull ResourceObjectDefinition getObjectDefinitionRequired() {
        return MiscUtil.stateNonNull(resourceObjectDefinition, () -> "No object definition");
    }

    public String getTag() {
        return tag;
    }

    @Override
    public @NotNull VariablesMap createVariablesMap() {
        VariablesMap variablesMap = ModelImplUtils.getDefaultVariablesMap(
                getFocusOrPreFocus(), shadowedResourceObject, resource, systemConfiguration);
        variablesMap.put(ExpressionConstants.VAR_RESOURCE_OBJECT_DELTA, resourceObjectDelta, ObjectDelta.class);
        return variablesMap;
    }

    @SuppressWarnings("ReplaceNullCheck")
    private @NotNull ObjectType getFocusOrPreFocus() {
        if (linkedOwner != null) {
            return linkedOwner;
        } else if (correlatedOwner != null) {
            return correlatedOwner;
        } else {
            return getPreFocus();
        }
    }

    @Nullable SynchronizationPolicy getSynchronizationPolicy() {
        return synchronizationPolicy;
    }

    public @NotNull SynchronizationPolicy getSynchronizationPolicyRequired() {
        return MiscUtil.requireNonNull(synchronizationPolicy, () -> new IllegalStateException("No synchronization policy"));
    }

    String getPolicyName() {
        if (synchronizationPolicy == null) {
            return null;
        }
        String name = synchronizationPolicy.getName();
        if (name != null) {
            return name;
        }
        return synchronizationPolicy.toString();
    }

    @Override
    public @NotNull ShadowLikeValue getShadowLikeValue() {
        return AbstractShadow.of(shadowedResourceObject);
    }

    @NotNull ShadowType getShadowedResourceObjectBefore() {
        return shadowedResourceObjectBefore;
    }

    /** Normally should be non-null, but we are not sure enough to mark as NotNull. */
    public String getShadowOid() {
        return shadowedResourceObject.getOid();
    }

    @Override
    public @Nullable ObjectDelta<ShadowType> getResourceObjectDelta() {
        return resourceObjectDelta;
    }

    @Override
    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull String getResourceOid() {
        return requireNonNull(getResource().getOid());
    }

    public @NotNull Class<F> getFocusClass() throws SchemaException {
        assert synchronizationPolicy != null;
        //noinspection unchecked
        return (Class<F>) synchronizationPolicy.getFocusClass();
    }

    @Override
    public @Nullable String getArchetypeOid() {
        return synchronizationPolicy != null ? synchronizationPolicy.getArchetypeOid() : null;
    }

    @Override
    public @NotNull F getPreFocus() {
        if (preFocus != null) {
            return preFocus;
        }
        try {
            preFocus = prismContext.createObjectable(
                    getFocusClass());
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when creating pre-focus");
        }
        return preFocus;
    }

    ObjectTemplateType getObjectTemplateForCorrelation() {
        return objectTemplateForCorrelation;
    }

    void setObjectTemplateForCorrelation(ObjectTemplateType objectTemplateForCorrelation) {
        this.objectTemplateForCorrelation = objectTemplateForCorrelation;
    }

    public F getLinkedOwner() {
        return linkedOwner;
    }

    public F getCorrelatedOwner() {
        return correlatedOwner;
    }

    public SynchronizationSituationType getSituation() {
        return situation;
    }

    void setSituationIfNull(SynchronizationSituationType situation) {
        if (this.situation == null) {
            this.situation = situation;
        }
    }

    void setLinkedOwner(F owner) {
        this.linkedOwner = owner;
    }

    void setCorrelatedOwner(F correlatedFocus) {
        this.correlatedOwner = correlatedFocus;
    }

    @Override
    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    @Override
    public @NotNull Task getTask() {
        return task;
    }

    public boolean isShadowExistsInRepo() {
        return shadowExistsInRepo;
    }

    @SuppressWarnings("SameParameterValue")
    public void setShadowExistsInRepo(boolean shadowExistsInRepo) {
        this.shadowExistsInRepo = shadowExistsInRepo;
    }

    boolean isForceClassificationUpdate() {
        return forceClassificationUpdate;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    @Override
    public String toString() {
        if (synchronizationPolicy != null) {
            return "SynchronizationContext.Complete(kind=" + synchronizationPolicy.getKind()
                    + ", intent=" + synchronizationPolicy.getIntent()
                    + ", objectclass=" + synchronizationPolicy.getObjectClassName()
                    + ")";
        } else {
            return "SynchronizationContext.Incomplete (no sync policy)";
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "change", change, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedResourceObject", shadowedResourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "systemConfiguration", systemConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "channel", channel, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "expressionProfile", expressionProfile, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "synchronizationPolicy", synchronizationPolicy, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "currentOwner", linkedOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "correlatedOwner", correlatedOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "situation", situation, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectTypeDefinition", resourceObjectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowExistsInRepo", shadowExistsInRepo, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pendingShadowDeltas", updater.getDeltas(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "forceIntentChange", forceClassificationUpdate, indent + 1);
        return sb.toString();
    }

    @VisibleForTesting
    public static void setSkipMaintenanceCheck(boolean skipMaintenanceCheck) {
        SynchronizationContext.skipMaintenanceCheck = skipMaintenanceCheck;
    }

    static boolean isSkipMaintenanceCheck() {
        return SynchronizationContext.skipMaintenanceCheck;
    }

    void applyShadowDeltas(@NotNull Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
        updater.applyShadowDeltas(deltas);
    }

    /**
     * Should we update correlators' state? (With or without re-correlation, at least for the time being.)
     *
     * Currently a temporary implementation based on checking id-match related flag in task extension.
     */
    boolean isCorrelatorsUpdateRequested() {
        return Boolean.TRUE.equals(
                task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_UPDATE_ID_MATCH));
    }

    /**
     * Are we updating the correlators' state and ignoring the (potentially updated) correlation result?
     *
     * This is a temporary response to the question of what we have to do if the correlator comes
     * to a conclusion different from the original one: we ignore it.
     */
    boolean isUpdatingCorrelatorsOnly() {
        return isCorrelatorsUpdateRequested() && getLinkedOwner() != null;
    }

    SystemConfigurationType getSystemConfigurationBean() {
        return systemConfiguration;
    }

    public @NotNull ExecutionModeType getExecutionMode() {
        return executionMode;
    }

    public boolean isDryRun() {
        return executionMode == ExecutionModeType.DRY_RUN;
    }

    boolean isNotDryRunLikeMode() {
        return !isDryRun() && !task.areShadowChangesSimulated();
    }

    public boolean isFullMode() {
        return executionMode == ExecutionModeType.FULL
                && task.isExecutionFullyPersistent();
    }

    public @NotNull ResourceObjectShadowChangeDescription getChange() {
        return change;
    }

    void recordSyncExclusionInTask(SynchronizationExclusionReasonType reason) {
        task.onSynchronizationExclusion(itemProcessingIdentifier, reason);
    }

    void recordSyncStartInTask() {
        task.onSynchronizationStart(itemProcessingIdentifier, shadowedResourceObject.getOid(), situation);
    }

    @NotNull ShadowUpdater getUpdater() {
        return updater;
    }

    public abstract boolean isComplete();

    boolean isExecutionFullyPersistent() {
        return task.isExecutionFullyPersistent();
    }

    /** I.e. we are either in full persistence or at least in shadow persistence mode. */
    boolean areShadowChangesPersistent() {
        return !task.areShadowChangesSimulated();
    }

    public boolean isVisible() {
        return SimulationUtil.isVisible(resource, resourceObjectDefinition, task.getExecutionMode());
    }

    public @NotNull ShadowType getShadowedResourceObject() {
        return shadowedResourceObject;
    }

    /**
     * Synchronization context ready for the synchronization, i.e. it has type identification and synchronization policy present.
     */
    public static class Complete<F extends FocusType> extends SynchronizationContext<F> {

        Complete(
                @NotNull ResourceObjectShadowChangeDescription change,
                @NotNull ResourceObjectProcessingContextImpl processingContext,
                @NotNull ResourceObjectTypeIdentification typeIdentification,
                @NotNull ResourceObjectDefinition objectDefinition,
                @NotNull SynchronizationPolicy synchronizationPolicy,
                @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
                @Nullable String tag) {
            super(change, processingContext, typeIdentification, objectDefinition, synchronizationPolicy, sorterResult, tag);
        }

        @Override
        public @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
            return requireNonNull(super.getTypeIdentification());
        }

        @Override
        public @NotNull SynchronizationPolicy getSynchronizationPolicy() {
            return requireNonNull(super.getSynchronizationPolicy());
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public @NotNull ResourceObjectInboundDefinition getInboundDefinition() throws SchemaException, ConfigurationException {
            return getObjectDefinitionRequired();
        }
    }

    /**
     * Synchronization context not ready for the synchronization; policy is not present.
     * Such context cannot be used for synchronization - the sync will be skipped in this case.
     */
    static class Incomplete<F extends FocusType> extends SynchronizationContext<F> {

        Incomplete(
                @NotNull ResourceObjectShadowChangeDescription change,
                @NotNull ResourceObjectProcessingContextImpl processingContext,
                @Nullable ResourceObjectTypeIdentification typeIdentification,
                @Nullable ResourceObjectDefinition objectDefinition,
                @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
                @Nullable String tag) {
            super(change, processingContext, typeIdentification, objectDefinition, null, sorterResult, tag);
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public @NotNull ResourceObjectInboundDefinition getInboundDefinition() throws SchemaException, ConfigurationException {
            if (resourceObjectDefinition != null) {
                return resourceObjectDefinition;
            } else {
                throw new IllegalStateException("No object definition in " + this + ", this method should not have been called");
            }
        }
    }
}
