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
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Context of the synchronization operation. It is created in the early stages of {@link ResourceObjectShadowChangeDescription}
 * progressing in {@link SynchronizationServiceImpl}.
 *
 * @param <F> Type of the matching focus object
 */
public class SynchronizationContext<F extends FocusType> implements PreInboundsContext<F> {

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

    /**
     * Original delta that triggered this synchronization. (If known.)
     */
    private final ObjectDelta<ShadowType> resourceObjectDelta;

    /**
     * The resource. It is updated in {@link #checkNotInMaintenance(OperationResult)}. But it's never null.
     */
    @NotNull private ResourceType resource;

    /** Current system configuration */
    private SystemConfigurationType systemConfiguration;

    private final String channel;

    private ExpressionProfile expressionProfile;

    @NotNull private final Task task;

    private ResourceObjectTypeSynchronizationPolicy synchronizationPolicy;

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

    private String intent;
    private String tag;

    /** Definition of corresponding object type (currently found by kind+intent). Lazily evaluated. TODO reconsider. */
    private ResourceObjectTypeDefinition objectTypeDefinition;

    private boolean reactionEvaluated = false;
    private SynchronizationReactionType reaction;

    private boolean shadowExistsInRepo = true;
    private boolean forceIntentChange;

    @NotNull private final PrismContext prismContext;
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
            @NotNull ShadowType shadowedResourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull ResourceType resource,
            String channel,
            @NotNull ModelBeans beans,
            @NotNull Task task,
            String itemProcessingIdentifier) {
        this.shadowedResourceObject = shadowedResourceObject;
        this.resourceObjectDelta = resourceObjectDelta;
        this.resource = resource;
        this.channel = channel;
        this.task = task;
        this.prismContext = beans.prismContext;
        this.beans = beans;
        this.expressionProfile = MiscSchemaUtil.getExpressionProfile();
        this.itemProcessingIdentifier = itemProcessingIdentifier;
    }

    public boolean isSynchronizationEnabled() {
        return synchronizationPolicy != null
                && BooleanUtils.isNotFalse(synchronizationPolicy.getSynchronizationBean().isEnabled());
    }

    public boolean isProtected() {
        return BooleanUtils.isTrue(shadowedResourceObject.isProtectedObject());
    }

    public ShadowKindType getKind() {

        if (!hasApplicablePolicy()) {
            return ShadowKindType.UNKNOWN;
        }

        return synchronizationPolicy.getKind();
    }

    public String getIntent() throws SchemaException {
        if (!hasApplicablePolicy()) {
            return SchemaConstants.INTENT_UNKNOWN;
        }

        if (intent == null) {
            ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
            ResourceObjectDefinition def = schema.findObjectDefinition(getKind(), null);
            if (def instanceof ResourceObjectTypeDefinition) {
                intent = ((ResourceObjectTypeDefinition) def).getIntent(); // TODO ???
            }
        }
        return intent;
    }

    public CorrelationContext getCorrelationContext() {
        return correlationContext;
    }

    public void setCorrelationContext(CorrelationContext correlationContext) {
        this.correlationContext = correlationContext;
    }

    // TODO reconsider
    public @NotNull ResourceObjectTypeDefinition getObjectTypeDefinition() throws SchemaException, ConfigurationException {
        if (objectTypeDefinition == null) {
            objectTypeDefinition = ResourceSchemaFactory.getCompleteSchemaRequired(resource)
                    .findObjectTypeDefinitionRequired(getKind(), getIntent());
        }
        return objectTypeDefinition;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private ObjectSynchronizationType getSynchronizationBean() {
        return synchronizationPolicy != null ? synchronizationPolicy.getSynchronizationBean() : null;
    }

    public List<ConditionalSearchFilterType> getCorrelation() {
        return getSynchronizationBean().getCorrelation();
    }

    public ExpressionType getConfirmation() {
        return getSynchronizationBean().getConfirmation();
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

        return getSynchronizationBean().getObjectTemplateRef();
    }

    public SynchronizationReactionType getReaction(OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (reactionEvaluated) {
            return reaction;
        }

        SynchronizationReactionType defaultReaction = null;
        for (SynchronizationReactionType reactionToConsider : getSynchronizationBean().getReaction()) {
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
            VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                    getFocusOrPreFocus(), shadowedResourceObject, resource, systemConfiguration);
            variables.put(ExpressionConstants.VAR_RESOURCE_OBJECT_DELTA, resourceObjectDelta, ObjectDelta.class);
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

    boolean hasApplicablePolicy() {
        return synchronizationPolicy != null;
    }

    String getPolicyName() {
        if (synchronizationPolicy == null) {
            return null;
        }
        if (getSynchronizationBean().getName() != null) {
            return getSynchronizationBean().getName();
        }
        return synchronizationPolicy.toString();
    }

    public Boolean isDoReconciliation() {
        if (reaction.isReconcile() != null) {
            return reaction.isReconcile();
        }
        if (getSynchronizationBean().isReconcile() != null) {
            return getSynchronizationBean().isReconcile();
        }
        return null;
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
        if (getSynchronizationBean().isLimitPropagation() != null) {
            return getSynchronizationBean().isLimitPropagation();
        }
        return null;
    }

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

        if (!hasApplicablePolicy()) {
            throw new IllegalStateException("synchronizationPolicy is null");
        }

        QName focusTypeQName = getSynchronizationBean().getFocusType();
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

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public SynchronizationSituationType getSituation() {
        return situation;
    }

    void setObjectSynchronizationPolicy(ResourceObjectTypeSynchronizationPolicy policy) {
        this.intent = policy.getIntent();
        this.synchronizationPolicy = policy;
    }

    public void setFocusClass(Class<F> focusClass) {
        this.focusClass = focusClass;
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

    public void setSystemConfiguration(SystemConfigurationType systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public void setExpressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
    }

    public void setReaction(SynchronizationReactionType reaction) {
        this.reaction = reaction;
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

    public void setForceIntentChange(boolean forceIntentChange) {
        this.forceIntentChange = forceIntentChange;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    ResourceObjectTypeDefinition findObjectTypeDefinition() throws SchemaException {
        ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        ShadowKindType kind = getKind();

        // FIXME this hacking
        String intent = getIntent();
        if (kind == null || kind == ShadowKindType.UNKNOWN) {
            return null; // nothing to look for
        }
        if (SchemaConstants.INTENT_UNKNOWN.equals(intent)) {
            intent = null;
        }

        // FIXME the cast
        return (ResourceObjectTypeDefinition) refinedResourceSchema.findObjectDefinition(kind, intent);
    }

    @Override
    public String toString() {
        String policyDesc = null;
        if (synchronizationPolicy != null) {
            if (getSynchronizationBean().getName() == null) {
                policyDesc = "(kind=" + synchronizationPolicy.getKind() + ", intent="
                        + synchronizationPolicy.getIntent() + ", objectclass="
                        + getSynchronizationBean().getObjectClass() + ")";
            } else {
                policyDesc = getSynchronizationBean().getName();
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
        DebugUtil.debugDumpWithLabelToStringLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "reaction", reaction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowExistsInRepo", shadowExistsInRepo, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "pendingShadowDeltas", pendingShadowDeltas, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "forceIntentChange", forceIntentChange, indent + 1);
        return sb.toString();
    }

    /**
     * Checks whether the source resource is not in maintenance mode.
     * (Throws an exception if it is.)
     *
     * Side-effect: updates the resource prism object (if it was changed).
     */
    void checkNotInMaintenance(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!skipMaintenanceCheck) {
            resource = beans.provisioningService
                    .getObject(ResourceType.class, resource.getOid(), null, task, result)
                    .asObjectable();
            ResourceTypeUtil.checkNotInMaintenance(resource);
        }
    }

    @VisibleForTesting
    public static void setSkipMaintenanceCheck(boolean skipMaintenanceCheck) {
        SynchronizationContext.skipMaintenanceCheck = skipMaintenanceCheck;
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

    @NotNull Collection<ResourceObjectTypeSynchronizationPolicy> getAllSynchronizationPolicies() throws SchemaException {
        List<ResourceObjectTypeSynchronizationPolicy> policies = new ArrayList<>();

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (schema == null) {
            LOGGER.warn("No synchronization policies can be collected from {}. It has no schema.", resource);
            return List.of();
        }

        for (ResourceObjectTypeDefinition typeDef : schema.getObjectTypeDefinitions()) {
            ObjectSynchronizationType syncDef = typeDef.getDefinitionBean().getSynchronization();
            if (syncDef != null) {
                policies.add(ResourceObjectTypeSynchronizationPolicy.forEmbedded(typeDef, syncDef));
            }
        }

        SynchronizationType synchronization = resource.getSynchronization();
        if (synchronization != null) {
            for (ObjectSynchronizationType synchronizationBean : synchronization.getObjectSynchronization()) {
                ResourceObjectTypeSynchronizationPolicy policy =
                        ResourceObjectTypeSynchronizationPolicy.forStandalone(synchronizationBean, schema);
                if (policy != null) {
                    policies.add(policy);
                } else {
                    LOGGER.warn("Synchronization configuration {} cannot be connected to resource object definition in {}",
                            synchronizationBean, resource);
                }
            }
        }

        return policies;
    }

    boolean isShadowAlreadyLinked() {
        return linkedOwner != null
                && linkedOwner.getLinkRef().stream()
                .anyMatch(link -> link.getOid().equals(shadowedResourceObject.getOid()));
    }

    public @NotNull ModelBeans getBeans() {
        return Objects.requireNonNull(beans, () -> "no model Spring beans in " + this);
    }
}
