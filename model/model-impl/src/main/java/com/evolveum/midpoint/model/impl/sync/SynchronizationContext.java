/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.Nullable;

public class SynchronizationContext<F extends FocusType> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContext.class);

    private PrismObject<ShadowType> applicableShadow;
    private final PrismObject<ShadowType> currentShadow;

    /**
     * Original delta that triggered this synchronization. (If known.)
     */
    private ObjectDelta<ShadowType> resourceObjectDelta;

    private PrismObject<ResourceType> resource;
    private PrismObject<SystemConfigurationType> systemConfiguration;
    private String channel;
    private ExpressionProfile expressionProfile;

    private Task task;

    private ObjectSynchronizationType objectSynchronization;
    private Class<F> focusClass;
    private F currentOwner;
    private F correlatedOwner;
    private SynchronizationSituationType situation;

    private String intent;

    private String tag;

    private boolean reactionEvaluated = false;
    private SynchronizationReactionType reaction;

    private boolean unrelatedChange = false;

    private boolean shadowExistsInRepo = true;
    private boolean forceIntentChange;

    private PrismContext prismContext;
    private ExpressionFactory expressionFactory;

    public SynchronizationContext(PrismObject<ShadowType> applicableShadow, PrismObject<ShadowType> currentShadow,
            ObjectDelta<ShadowType> resourceObjectDelta, PrismObject<ResourceType> resource, String channel,
            PrismContext prismContext, ExpressionFactory expressionFactory, Task task) {
        this.applicableShadow = applicableShadow;
        this.currentShadow = currentShadow;
        this.resourceObjectDelta = resourceObjectDelta;
        this.resource = resource;
        this.channel = channel;
        this.task = task;
        this.prismContext = prismContext;
        this.expressionFactory = expressionFactory;
        this.expressionProfile = MiscSchemaUtil.getExpressionProfile();
    }

    public boolean isSynchronizationEnabled() {
        return objectSynchronization != null && BooleanUtils.isNotFalse(objectSynchronization.isEnabled());
    }

    public boolean isProtected() {
        if (applicableShadow == null) {
            return false;
        }

        ShadowType currentShadowType = applicableShadow.asObjectable();
        return BooleanUtils.isTrue(currentShadowType.isProtectedObject());
    }

//    public boolean isSatisfyTaskConstraints() throws SchemaException {
//
//        ShadowKindType kind = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_KIND);
//        String intent = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_INTENT);
//        QName objectClass = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_OBJECTCLASS);
//
//        LOGGER.trace("checking task constraints: {}", task);
//
//        boolean isApplicable = SynchronizationUtils.isPolicyApplicable(objectClass, kind, intent, objectSynchronization, resource, true);
//        //this mean that kind/intent are null in the task..but this can be a case, so check if at least the objectClass is the same
//        if (!isApplicable && objectClass != null) {
//            return QNameUtil.matchAny(objectClass, objectSynchronization.getObjectClass());
//        }
//
//        return isApplicable;
//    }
//
//    //TODO multi-threaded tasks?
//    private <T> T getTaskPropertyValue(QName propertyName) {
//        PrismProperty<T> prop = task.getExtensionPropertyOrClone(ItemName.fromQName(propertyName));
//        if (prop == null || prop.isEmpty()) {
//            return null;
//        }
//
//        return prop.getRealValue();
//    }

    public ShadowKindType getKind() {

        if (!hasApplicablePolicy()) {
            return ShadowKindType.UNKNOWN;
        }

        if (objectSynchronization.getKind() == null) {
            return ShadowKindType.ACCOUNT;
        }

        return objectSynchronization.getKind();
    }

    public String getIntent() throws SchemaException {
        if (!hasApplicablePolicy()) {
            return SchemaConstants.INTENT_UNKNOWN;
        }

        if (intent == null) {
            RefinedResourceSchema schema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
            ObjectClassComplexTypeDefinition occtd = schema.findDefaultObjectClassDefinition(getKind());
            intent = occtd.getIntent();
        }
        return intent;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<ConditionalSearchFilterType> getCorrelation() {
        return objectSynchronization.getCorrelation();
    }

    public ExpressionType getConfirmation() {
        return objectSynchronization.getConfirmation();
    }

    public ObjectReferenceType getObjectTemplateRef() {
        if (reaction.getObjectTemplateRef() != null) {
            return reaction.getObjectTemplateRef();
        }

        return objectSynchronization.getObjectTemplateRef();
    }

    public SynchronizationReactionType getReaction(OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (reactionEvaluated) {
            return reaction;
        }

        SynchronizationReactionType defaultReaction = null;
        for (SynchronizationReactionType reactionToConsider : objectSynchronization.getReaction()) {
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
            ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(getFocus(), applicableShadow, null,
                    resource, systemConfiguration, null, prismContext);
            variables.put(ExpressionConstants.VAR_RESOURCE_OBJECT_DELTA, resourceObjectDelta, ObjectDelta.class);
            try {
                ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
                PrismPropertyValue<Boolean> evaluateCondition = ExpressionUtil.evaluateCondition(variables, expression,
                        expressionProfile, expressionFactory, desc, task, result);
                boolean value = Boolean.TRUE.equals(evaluateCondition.getValue());
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

    @Nullable
    private PrismObject<? extends ObjectType> getFocus() {
        PrismObject<? extends ObjectType> focus;
        if (currentOwner != null) {
            focus = currentOwner.asPrismObject();
        } else if (correlatedOwner != null) {
            focus = correlatedOwner.asPrismObject();
        } else {
            focus = null;
        }
        return focus;
    }

    public boolean hasApplicablePolicy() {
        return objectSynchronization != null;
    }

    public String getPolicyName() {
        if (objectSynchronization == null) {
            return null;
        }
        if (objectSynchronization.getName() != null) {
            return objectSynchronization.getName();
        }
        return objectSynchronization.toString();
    }

    public Boolean isDoReconciliation() {
        if (reaction.isReconcile() != null) {
            return reaction.isReconcile();
        }
        if (objectSynchronization.isReconcile() != null) {
            return objectSynchronization.isReconcile();
        }
        return null;
    }

    public ModelExecuteOptionsType getExecuteOptions() {
        return reaction.getExecuteOptions();
    }

    public Boolean isLimitPropagation() {
        if (StringUtils.isNotBlank(channel)) {
            QName channelQName = QNameUtil.uriToQName(channel);
            // Discovery channel is used when compensating some inconsistent
            // state. Therefore we do not want to propagate changes to other
            // resources. We only want to resolve the problem and continue in
            // previous provisioning/synchronization during which this
            // compensation was triggered.
            if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY.equals(channelQName)
                    && SynchronizationSituationType.DELETED != reaction.getSituation()) {
                return true;
            }
        }

        if (reaction.isLimitPropagation() != null) {
            return reaction.isLimitPropagation();
        }
        if (objectSynchronization.isLimitPropagation() != null) {
            return objectSynchronization.isLimitPropagation();
        }
        return null;
    }

    //TODO obejctClass???
//    public QName getObjectClass() {
//        if (objectSynchronization.getObjectClass() != )
//    }


    public PrismObject<ShadowType> getApplicableShadow() {
        return applicableShadow;
    }
    public PrismObject<ShadowType> getCurrentShadow() {
        return currentShadow;
    }
    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public Class<F> getFocusClass() throws SchemaException {

        if (focusClass != null) {
            return focusClass;
        }

        if (!hasApplicablePolicy()) {
            throw new IllegalStateException("synchronizationPolicy is null");
        }

        QName focusTypeQName = objectSynchronization.getFocusType();
        if (focusTypeQName == null) {
            //noinspection unchecked
            this.focusClass = (Class<F>) UserType.class;
            return focusClass;
        }
        ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(focusTypeQName);
        if (objectType == null) {
            throw new SchemaException("Unknown focus type " + focusTypeQName + " in synchronization policy in " + resource);
        }
        //noinspection unchecked
        this.focusClass = (Class<F>) objectType.getClassDefinition();
        return focusClass;
    }

    public F getCurrentOwner() {
        return currentOwner;
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

    public void setObjectSynchronization(ObjectSynchronizationType objectSynchronization) {
        this.intent = objectSynchronization.getIntent();
        this.objectSynchronization = objectSynchronization;
    }

    public void setFocusClass(Class<F> focusClass) {
        this.focusClass = focusClass;
    }

    public void setCurrentOwner(F owner) {
        this.currentOwner = owner;
    }

    public void setCorrelatedOwner(F correlatedFocus) {
        this.correlatedOwner = correlatedFocus;
    }

    public void setSituation(SynchronizationSituationType situation) {
        this.situation = situation;
    }

    public PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return systemConfiguration;
    }
    public String getChannel() {
        return channel;
    }
    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }
    public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }
    public void setChannel(String channel) {
        this.channel = channel;
    }

//    public SynchronizationReactionType getReaction() {
//        return reaction;
//    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public void setExpressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
    }

    public void setReaction(SynchronizationReactionType reaction) {
        this.reaction = reaction;
    }

    public boolean isUnrelatedChange() {
        return unrelatedChange;
    }

    public void setUnrelatedChange(boolean unrelatedChange) {
        this.unrelatedChange = unrelatedChange;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean isShadowExistsInRepo() {
        return shadowExistsInRepo;
    }

    public void setShadowExistsInRepo(boolean shadowExistsInRepo) {
        this.shadowExistsInRepo = shadowExistsInRepo;
    }

    public boolean isForceIntentChange() {
        return forceIntentChange;
    }

    public void setForceIntentChange(boolean forceIntentChange) {
        this.forceIntentChange = forceIntentChange;
    }

    public RefinedObjectClassDefinition findRefinedObjectClassDefinition() throws SchemaException {
        RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
        return refinedResourceSchema.getRefinedDefinition(getKind(), getIntent());
    }


    @Override
    public String toString() {
        String policyDesc = null;
        if (objectSynchronization != null) {
            if (objectSynchronization.getName() == null) {
                policyDesc = "(kind=" + objectSynchronization.getKind() + ", intent="
                        + objectSynchronization.getIntent() + ", objectclass="
                        + objectSynchronization.getObjectClass() + ")";
            } else {
                policyDesc = objectSynchronization.getName();
            }
        }

        return policyDesc;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(SynchronizationContext.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "applicableShadow", applicableShadow, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "currentShadow", currentShadow, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "systemConfiguration", systemConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "channel", channel, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "expressionProfile", expressionProfile, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectSynchronization", objectSynchronization, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "focusClass", focusClass, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "currentOwner", currentOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "correlatedOwner", correlatedOwner, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "situation", situation, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "tag", tag, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "reaction", reaction, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "unrelatedChange", unrelatedChange, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowExistsInRepo", shadowExistsInRepo, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "forceIntentChange", forceIntentChange, indent + 1);
        return sb.toString();
    }

}
