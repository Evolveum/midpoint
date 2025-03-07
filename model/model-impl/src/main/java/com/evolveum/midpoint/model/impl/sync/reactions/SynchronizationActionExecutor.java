/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ObjectSynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.DELETED;

/**
 * Selects and executes a synchronization reaction.
 */
public class SynchronizationActionExecutor<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationActionExecutor.class);

    private static final String OP_REACT = SynchronizationActionExecutor.class.getName() + ".react";

    @NotNull private final SynchronizationContext.Complete<F> syncCtx;
    @NotNull private final SynchronizationPolicy policy;
    @NotNull private final ResourceObjectShadowChangeDescription change;
    @NotNull private final ProvisioningService provisioningService = ModelBeans.get().provisioningService;

    public SynchronizationActionExecutor(
            @NotNull SynchronizationContext.Complete<F> syncCtx) {
        this.syncCtx = syncCtx;
        this.policy = syncCtx.getSynchronizationPolicyRequired();
        this.change = syncCtx.getChange();
    }

    /** Returns true in case of synchronization failure. */
    public boolean react(OperationResult parentResult)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException {

        OperationResult result = parentResult.createSubresult(OP_REACT);
        try {
            LOGGER.trace("Synchronization context:\n{}", syncCtx.debugDumpLazily(1));

            if (syncCtx.getSituation() == DELETED && !isDeleteReactionApplicable(result)) {
                result.recordNotApplicable("DELETED situation is not applicable for the focus/shadow pair");
                return false;
            }

            ObjectSynchronizationReactionDefinition reaction = getReaction(result);
            if (reaction != null) {
                executeReaction(reaction, result);
            } else {
                LOGGER.debug("No reaction is defined for situation {} in {}", syncCtx.getSituation(), syncCtx.getResource());
                result.recordNotApplicable();
            }
        } catch (Throwable t) {
            // Only unexpected exceptions should be here
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }

        return result.getStatus() == FATAL_ERROR;
    }

    private ObjectSynchronizationReactionDefinition getReaction(OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {

        SynchronizationSituationType currentSituation = syncCtx.getSituation();
        if (currentSituation == null) {
            LOGGER.warn("No situation determined for {}, skipping synchronization actions", syncCtx);
            return null;
        }
        String currentChannel = syncCtx.getChannel();

        ObjectSynchronizationReactionDefinition defaultReaction = null;
        for (ObjectSynchronizationReactionDefinition reactionToConsider : policy.getReactions()) {
            TaskExecutionMode executionMode = syncCtx.getTask().getExecutionMode();
            if (!reactionToConsider.isVisible(executionMode)) {
                LOGGER.trace("Skipping {} because it is not visible for current task execution mode: {}",
                        reactionToConsider, executionMode);
                continue;
            }
            if (!reactionToConsider.matchesSituation(currentSituation)) {
                LOGGER.trace("Skipping {} because the situation does not match: {}", reactionToConsider, currentSituation);
                continue;
            }

            Collection<String> channels = reactionToConsider.getChannels();
            if (channels.isEmpty()) {
                if (conditionMatches(reactionToConsider, result)) {
                    defaultReaction = reactionToConsider;
                }
            } else if (channels.contains(currentChannel)) {
                if (conditionMatches(reactionToConsider, result)) {
                    return reactionToConsider;
                }
            } else {
                LOGGER.trace("Skipping reaction {} because the channel does not match {}", reactionToConsider, currentChannel);
            }
        }
        LOGGER.trace("Using default reaction {}", defaultReaction);
        return defaultReaction;
    }

    private boolean conditionMatches(@NotNull ObjectSynchronizationReactionDefinition reaction, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionType condition = reaction.getCondition();
        if (condition == null) {
            return true;
        }

        String desc = "condition in synchronization reaction on " + reaction.getSituations()
                + (reaction.getName() != null ? " (" + reaction.getName() + ")" : "");
        VariablesMap variables = syncCtx.createVariablesMap();
        try {
            Task task = syncCtx.getTask();
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ModelExpressionEnvironment<>(task, result));
            boolean value = ExpressionUtil.evaluateConditionDefaultFalse(
                    variables,
                    condition,
                    syncCtx.getExpressionProfile(),
                    ModelBeans.get().expressionFactory,
                    desc,
                    task,
                    result);
            if (!value) {
                LOGGER.trace("Skipping reaction {} because the condition was evaluated to false", reaction);
            }
            return value;
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private void executeReaction(@NotNull ObjectSynchronizationReactionDefinition reactionDefinition, OperationResult result) {

        // This implements a pre-4.6 behavior. All actions were clockwork-related at that time.
        if (reactionDefinition.isLegacySynchronizeOff()) {
            LOGGER.trace("Skipping clockwork run on {} for situation {} because 'synchronize' is set to false (or there are "
                    + "no actions and 'synchronize' is not set).", syncCtx.getResource(), syncCtx.getSituation());
            return;
        }

        try {
            for (var actionDefinition : reactionDefinition.getActions()) {
                syncCtx.freezeShadowedResourceObject(); // To avoid cloning in the clockwork
                ModelBeans.get().synchronizationActionFactory
                        .getActionInstance(
                                new ActionInstantiationContext<>(
                                        syncCtx,
                                        change,
                                        reactionDefinition,
                                        actionDefinition))
                        .handle(result);
            }
        } catch (Exception e) {
            result.recordFatalError(e);
            LOGGER.error("SYNCHRONIZATION: Error in synchronization on {} for situation {}: {}: {}. Change was {}",
                    syncCtx.getResource(), syncCtx.getSituation(), e.getClass().getSimpleName(), e.getMessage(), change, e);
            // what to do here? We cannot throw the error back. All that the notifyChange method
            // could do is to convert it to SystemException. But that indicates an internal error and it will
            // break whatever code called the notifyChange in the first place. We do not want that.
            // If the clockwork could not do anything with the exception then perhaps nothing can be done at all.
            // So just log the error (the error should be remembered in the result and task already)
            // and then just go on.
        }
    }

    /**
     * {@link SynchronizationSituationType#DELETED} is delicate. We should invoke the corresponding action only if we are
     * sure that there is no "replacement" for the deleted projection.
     *
     * TODO do we really execute this method also for multi-account resources?
     *  It probably makes sense: we don't want e.g. to delete a user if there is still some account on such resource
     *
     * TODO in the future we should probably utilize those already-retrieved shadows (from repo)
     */
    private boolean isDeleteReactionApplicable(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException {
        F owner = syncCtx.getLinkedOwner();
        if (owner == null) {
            LOGGER.trace("Cannot consider the 'realness' of the DELETE situation because there's no linked owner");
            return true;
        }
        for (ObjectReferenceType linkRef : owner.getLinkRef()) {
            var options = GetOperationOptionsBuilder.create()
                    .noFetch()
                    .futurePointInTime()
                    .allowNotFound()
                    .build();
            String shadowOid = linkRef.getOid();
            try {
                ShadowType shadow =
                        provisioningService
                                .getObject(ShadowType.class, shadowOid, options, syncCtx.getTask(), result)
                                .asObjectable();
                ResourceObjectTypeIdentification type = syncCtx.getTypeIdentification();
                if (ShadowUtil.getResourceOidRequired(shadow).equals(syncCtx.getResourceOid())
                        && shadow.getKind() == type.getKind()
                        && Objects.equals(shadow.getIntent(), type.getIntent())
                        && !ShadowUtil.isDead(shadow)) {
                    LOGGER.debug("Found non-dead compatible shadow, the DELETE reaction will not be executed for {} of {}",
                            syncCtx.getShadowedResourceObject(), owner);
                    return false;
                }
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Shadow not existing (or disappeared during provisioning get operation): {}", shadowOid);
                recordMissingShadowInSynchronizationContext(shadowOid, e);
            }
        }
        LOGGER.trace("No non-dead compatible shadow found, the DELETE reaction will be executed");
        return true;
    }

    private void recordMissingShadowInSynchronizationContext(String shadowOid, ObjectNotFoundException e) {
        if (shadowOid.equals(e.getOid())
            && shadowOid.equals(syncCtx.getShadowOid())) {
            LOGGER.trace("Marking repo shadow in synchronization context as missing: {} in {}", shadowOid, syncCtx);
            syncCtx.setShadowExistsInRepo(false);
        }
    }
}
