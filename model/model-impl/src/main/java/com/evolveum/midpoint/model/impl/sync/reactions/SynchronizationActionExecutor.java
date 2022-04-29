/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.SynchronizationActionDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * Selects and executes a synchronization reaction.
 */
public class SynchronizationActionExecutor<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationActionExecutor.class);

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final SynchronizationPolicy policy;
    @NotNull private final ResourceObjectShadowChangeDescription change;

    public SynchronizationActionExecutor(
            @NotNull SynchronizationContext<F> syncCtx,
            @NotNull ResourceObjectShadowChangeDescription change) {
        this.syncCtx = syncCtx;
        this.policy = syncCtx.getSynchronizationPolicyRequired();
        this.change = change;
    }

    public void react(OperationResult result)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException {

        LOGGER.trace("Synchronization context:\n{}", syncCtx.debugDumpLazily(1));

        SynchronizationReactionDefinition reaction = getReaction(result);

        if (reaction != null) {
            executeReaction(reaction, result);
        } else {
            LOGGER.debug("No reaction is defined for situation {} in {}", syncCtx.getSituation(), syncCtx.getResource());
        }
    }

    public SynchronizationReactionDefinition getReaction(OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {

        SynchronizationSituationType currentSituation = syncCtx.getSituation();
        if (currentSituation == null) {
            LOGGER.warn("No situation determined for {}, skipping synchronization actions", syncCtx);
            return null;
        }
        String currentChannel = syncCtx.getChannel();

        SynchronizationReactionDefinition defaultReaction = null;
        for (SynchronizationReactionDefinition reactionToConsider : policy.getReactions()) {
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

    private boolean conditionMatches(@NotNull SynchronizationReactionDefinition reaction, OperationResult result)
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
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
            boolean value = ExpressionUtil.evaluateConditionDefaultFalse(
                    variables,
                    condition,
                    syncCtx.getExpressionProfile(),
                    syncCtx.getBeans().expressionFactory,
                    desc,
                    task,
                    result);
            if (!value) {
                LOGGER.trace("Skipping reaction {} because the condition was evaluated to false", reaction);
            }
            return value;
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private void executeReaction(@NotNull SynchronizationReactionDefinition reactionDefinition, OperationResult result) {

        // This implements a pre-4.6 behavior. All actions were clockwork-related at that time.
        if (reactionDefinition.isLegacySynchronizeOff()) {
            LOGGER.trace("Skipping clockwork run on {} for situation {} because 'synchronize' is set to false (or there are "
                    + "no actions and 'synchronize' is not set).", syncCtx.getResource(), syncCtx.getSituation());
            return;
        }

        try {
            for (SynchronizationActionDefinition actionDefinition : reactionDefinition.getActions()) {
                syncCtx.getBeans().synchronizationActionFactory
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
}
