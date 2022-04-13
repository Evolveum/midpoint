/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Context of the synchronization operation. It is created in the early stages of {@link ResourceObjectShadowChangeDescription}
 * progressing in {@link SynchronizationServiceImpl}.
 *
 * @param <F> Type of the matching focus object
 */
public class SynchronizationContext<F extends FocusType>
        implements PreInboundsContext<F>, ResourceObjectProcessingContext {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContext.class);

    @VisibleForTesting
    private static boolean skipMaintenanceCheck;

    /**
     * Normally, this is shadowed resource object, i.e. shadow + attributes (simply saying).
     * In the case of object deletion, the last known shadow can be used, i.e. without attributes.
     *
     * See {@link ResourceObjectShadowChangeDescription#getShadowedResourceObject()}.
     */
    @NotNull private final ShadowType shadowedResourceObject;

    /** Original delta that triggered this synchronization. (If known.) */
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;

    @NotNull private final ResourceType resource;

    /** Current system configuration */
    private final SystemConfigurationType systemConfiguration;

    /** TODO */
    private final String channel;

    private final ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

    @NotNull private final Task task;

    /** Definition of corresponding object type (currently found by kind+intent). */
    @Nullable private final ResourceObjectTypeDefinition objectTypeDefinition;

    @Nullable private final ResourceObjectTypeSynchronizationPolicy synchronizationPolicy;

    /**
     * Preliminary focus object - a result pre pre-mappings execution.
     *
     * Lazily created on the first getter call.
     */
    private F preFocus;

    /**
     * Lazily evaluated on the first getter call.
     */
    private Class<F> focusClass;

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

    private boolean reactionEvaluated = false;
    private SynchronizationReactionType reaction;

    private boolean shadowExistsInRepo = true;
    private final boolean forceIntentChange;

    @NotNull private final PrismContext prismContext = PrismContext.get();
    @NotNull private final ModelBeans beans;

    /** TODO maybe will be removed */
    @Experimental
    private final String itemProcessingIdentifier;

    /**
     * Deltas that should be written to the shadow along with other sync metadata.
     *
     * They are already applied to the shadow - immediately as they are added to the list.
     */
    @NotNull private final List<ItemDelta<?, ?>> pendingShadowDeltas = new ArrayList<>();

    public SynchronizationContext(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceObjectTypeDefinition objectTypeDefinition,
            @Nullable ResourceObjectTypeSynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
            @Nullable String tag,
            @Nullable String itemProcessingIdentifier) {
        this.shadowedResourceObject = processingContext.getShadowedResourceObject();
        this.resourceObjectDelta = processingContext.getResourceObjectDelta();
        this.resource = processingContext.getResource();
        this.channel = processingContext.getChannel();
        this.systemConfiguration = processingContext.getSystemConfiguration();
        this.task = processingContext.getTask();
        this.beans = processingContext.getBeans();
        this.objectTypeDefinition = objectTypeDefinition;
        this.synchronizationPolicy = synchronizationPolicy;
        this.tag = tag;
        this.itemProcessingIdentifier = itemProcessingIdentifier;
        if (sorterResult != null) {
            this.forceIntentChange = true;
            LOGGER.trace("Setting synchronization situation to synchronization context: {}",
                    sorterResult.getSynchronizationSituation());
            situation = sorterResult.getSynchronizationSituation();
            LOGGER.trace("Setting correlated owner in synchronization context: {}", sorterResult.getOwner());
            //noinspection unchecked
            this.correlatedOwner = (F) sorterResult.getOwner();
        } else {
            this.forceIntentChange = false;
        }
    }

    public boolean isSynchronizationEnabled() {
        return synchronizationPolicy != null
                && BooleanUtils.isNotFalse(synchronizationPolicy.getSynchronizationBean().isEnabled());
    }

    public boolean isProtected() {
        return BooleanUtils.isTrue(shadowedResourceObject.isProtectedObject());
    }

    /** May be unknown! */
    public @NotNull ShadowKindType getKind() {
        return objectTypeDefinition != null ? objectTypeDefinition.getKind() : ShadowKindType.UNKNOWN;
    }

    /** May be unknown! */
    public @NotNull String getIntent() throws SchemaException {
        return objectTypeDefinition != null ? objectTypeDefinition.getIntent() : SchemaConstants.INTENT_UNKNOWN;
    }

    public CorrelationContext getCorrelationContext() {
        return correlationContext;
    }

    public void setCorrelationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
    }

    public @NotNull ResourceObjectTypeDefinition getObjectTypeDefinitionRequired()
            throws SchemaException, ConfigurationException {
        return MiscUtil.requireNonNull(objectTypeDefinition, () -> new IllegalStateException("No object type definition"));
    }

    public String getTag() {
        return tag;
    }

    private ObjectSynchronizationType getSynchronizationBean() {
        return synchronizationPolicy != null ? synchronizationPolicy.getSynchronizationBean() : null;
    }

    @NotNull private ObjectSynchronizationType getSynchronizationBeanRequired() {
        return getSynchronizationPolicyRequired().getSynchronizationBean();
    }

    /**
     * Assumes synchronization is defined.
     */
    public @NotNull CompositeCorrelatorType getCorrelators() {
        ObjectSynchronizationType objectSynchronization = getSynchronizationBean();
        assert objectSynchronization != null;
        CorrelationDefinitionType correlationDefinition = objectSynchronization.getCorrelationDefinition();
        CompositeCorrelatorType correlators = correlationDefinition != null ? correlationDefinition.getCorrelators() : null;
        if (correlators != null) {
            return correlators;
        } else if (objectSynchronization.getCorrelation().isEmpty()) {
            LOGGER.debug("No correlation information present. Will always find no owner. In: {}", this);
            return new CompositeCorrelatorType()
                    .beginNone().end();
        } else {
            CompositeCorrelatorType composite =
                    new CompositeCorrelatorType()
                            .beginFilter()
                            .confirmation(CloneUtil.clone(objectSynchronization.getConfirmation()))
                            .end();
            composite.getFilter().get(0).getOwnerFilter().addAll(
                    CloneUtil.cloneCollectionMembers(objectSynchronization.getCorrelation()));
            return composite;
        }
    }

    public ObjectReferenceType getObjectTemplateRef() {
        if (reaction.getObjectTemplateRef() != null) {
            return reaction.getObjectTemplateRef();
        }
        ObjectSynchronizationType bean = getSynchronizationBean();
        return bean != null ? bean.getObjectTemplateRef() : null;
    }

    public SynchronizationReactionType getReaction(OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (reactionEvaluated) {
            return reaction;
        }

        SynchronizationReactionType defaultReaction = null;
        for (SynchronizationReactionType reactionToConsider : getSynchronizationBeanRequired().getReaction()) {
            SynchronizationSituationType reactionSituation = reactionToConsider.getSituation();
            if (reactionSituation == null) {
                throw new ConfigurationException("No situation defined for a reaction in " + resource);
            }
            if (reactionSituation == situation) {
                List<String> channels = reactionToConsider.getChannel();
                // The second and third conditions are suspicious but let's keep them here for historical reasons.
                if (channels.isEmpty() || channels.contains("") || channels.contains(null)) {
                    if (conditionMatches(reactionToConsider, result)) {
                        defaultReaction = reactionToConsider;
                    }
                } else if (channels.contains(this.channel)) {
                    if (conditionMatches(reactionToConsider, result)) {
                        reaction = reactionToConsider;
                        reactionEvaluated = true;
                        return reaction;
                    }
                } else {
                    LOGGER.trace("Skipping reaction {} because the channel does not match {}", reaction, this.channel);
                }
            }
        }
        LOGGER.trace("Using default reaction {}", defaultReaction);
        reaction = defaultReaction;
        reactionEvaluated = true;
        return reaction;
    }

    private boolean conditionMatches(SynchronizationReactionType reaction, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        if (reaction.getCondition() != null) {
            ExpressionType expression = reaction.getCondition();
            String desc = "condition in synchronization reaction on " + reaction.getSituation()
                    + (reaction.getName() != null ? " (" + reaction.getName() + ")" : "");
            VariablesMap variables = createVariablesMap();
            try {
                ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
                boolean value = ExpressionUtil.evaluateConditionDefaultFalse(variables, expression,
                        expressionProfile, beans.expressionFactory, desc, task, result);
                if (!value) {
                    LOGGER.trace("Skipping reaction {} because the condition was evaluated to false", reaction);
                }
                return value;
            } finally {
                ModelExpressionThreadLocalHolder.popExpressionEnvironment();
            }
        } else {
            return true;
        }
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

    @Nullable ResourceObjectTypeSynchronizationPolicy getSynchronizationPolicy() {
        return synchronizationPolicy;
    }

    public @NotNull ResourceObjectTypeSynchronizationPolicy getSynchronizationPolicyRequired() {
        return MiscUtil.requireNonNull(synchronizationPolicy, () -> new IllegalStateException("No synchronization policy"));
    }

    boolean hasApplicablePolicy() {
        return synchronizationPolicy != null;
    }

    String getPolicyName() {
        if (synchronizationPolicy == null) {
            return null;
        }
        String name = getSynchronizationBeanRequired().getName();
        if (name != null) {
            return name;
        }
        return synchronizationPolicy.toString();
    }

    public Boolean isDoReconciliation() {
        if (reaction.isReconcile() != null) {
            return reaction.isReconcile();
        }
        return getSynchronizationBeanRequired().isReconcile();
    }

    public ModelExecuteOptionsType getExecuteOptions() {
        return reaction.getExecuteOptions();
    }

    Boolean isLimitPropagation() {
        if (StringUtils.isNotBlank(channel)) {
            QName channelQName = QNameUtil.uriToQName(channel);
            // Discovery channel is used when compensating some inconsistent
            // state. Therefore we do not want to propagate changes to other
            // resources. We only want to resolve the problem and continue in
            // previous provisioning/synchronization during which this
            // compensation was triggered.
            if (SchemaConstants.CHANNEL_DISCOVERY.equals(channelQName)
                    && SynchronizationSituationType.DELETED != reaction.getSituation()) {
                return true;
            }
        }

        if (reaction.isLimitPropagation() != null) {
            return reaction.isLimitPropagation();
        }
        return getSynchronizationBeanRequired().isLimitPropagation();
    }

    @Override
    public @NotNull ShadowType getShadowedResourceObject() {
        return shadowedResourceObject;
    }

    public @Nullable ObjectDelta<ShadowType> getResourceObjectDelta() {
        return resourceObjectDelta;
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull Class<F> getFocusClass() throws SchemaException {

        if (focusClass != null) {
            return focusClass;
        }

        QName focusTypeQName = getSynchronizationBeanRequired().getFocusType();
        if (focusTypeQName == null) {
            //noinspection unchecked
            this.focusClass = (Class<F>) UserType.class;
            return focusClass;
        }
        ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(focusTypeQName);
        if (objectType == null) {
            throw new SchemaException("Unknown focus type " + focusTypeQName + " in synchronization policy in " + resource);
        }

        focusClass = objectType.getClassDefinition();
        return focusClass;
    }

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

    public @NotNull PrismObject<F> getPreFocusAsPrismObject() {
        //noinspection unchecked
        return (PrismObject<F>) preFocus.asPrismObject();
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

    public void setLinkedOwner(F owner) {
        this.linkedOwner = owner;
    }

    public void setCorrelatedOwner(F correlatedFocus) {
        this.correlatedOwner = correlatedFocus;
    }

    public void setSituation(SynchronizationSituationType situation) {
        this.situation = situation;
    }

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    public String getChannel() {
        return channel;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public @NotNull Task getTask() {
        return task;
    }

    boolean isShadowExistsInRepo() {
        return shadowExistsInRepo;
    }

    @SuppressWarnings("SameParameterValue")
    void setShadowExistsInRepo(boolean shadowExistsInRepo) {
        this.shadowExistsInRepo = shadowExistsInRepo;
    }

    public boolean isForceIntentChange() {
        return forceIntentChange;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    @Override
    public String toString() {
        String policyDesc = null;
        if (synchronizationPolicy != null) {
            ObjectSynchronizationType bean = synchronizationPolicy.getSynchronizationBean();
            if (bean.getName() == null) {
                policyDesc = "(kind=" + synchronizationPolicy.getKind() + ", intent="
                        + synchronizationPolicy.getIntent() + ", objectclass="
                        + bean.getObjectClass() + ")";
            } else {
                policyDesc = bean.getName();
            }
        }

        return policyDesc;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(SynchronizationContext.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedResourceObject", shadowedResourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "systemConfiguration", systemConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "channel", channel, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "expressionProfile", expressionProfile, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "synchronizationPolicy", synchronizationPolicy, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "focusClass", focusClass, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "currentOwner", linkedOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "correlatedOwner", correlatedOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "situation", situation, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectTypeDefinition", objectTypeDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "reaction", reaction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowExistsInRepo", shadowExistsInRepo, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pendingShadowDeltas", pendingShadowDeltas, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "forceIntentChange", forceIntentChange, indent + 1);
        return sb.toString();
    }

    @VisibleForTesting
    public static void setSkipMaintenanceCheck(boolean skipMaintenanceCheck) {
        SynchronizationContext.skipMaintenanceCheck = skipMaintenanceCheck;
    }

    static boolean isSkipMaintenanceCheck() {
        return SynchronizationContext.skipMaintenanceCheck;
    }

    @Nullable CorrelationDefinitionType getCorrelationDefinitionBean() {
        ObjectSynchronizationType objectSynchronization = getSynchronizationBean();
        return objectSynchronization != null ? objectSynchronization.getCorrelationDefinition() : null;
    }

    @NotNull List<ItemDelta<?, ?>> getPendingShadowDeltas() {
        return pendingShadowDeltas;
    }

    void clearPendingShadowDeltas() {
        pendingShadowDeltas.clear();
    }

    void addShadowDeltas(@NotNull Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
        for (ItemDelta<?, ?> delta : deltas) {
            pendingShadowDeltas.add(delta);
            delta.applyTo(shadowedResourceObject.asPrismObject());
        }
    }

    /**
     * Should we update correlators' state? (With or without re-correlation, at least for the time being.)
     *
     * Currently a temporary implementation based on checking id-match related flag in task extension.
     */
    public boolean isCorrelatorsUpdateRequested() {
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

    public SystemConfigurationType getSystemConfigurationBean() {
        return systemConfiguration;
    }

    public @NotNull ModelBeans getBeans() {
        return beans;
    }
}
