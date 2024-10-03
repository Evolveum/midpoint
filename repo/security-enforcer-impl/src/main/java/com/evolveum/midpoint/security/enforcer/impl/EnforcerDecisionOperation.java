/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.PhaseSelector.nonStrict;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AbstractAuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.PhasedDecisionOperationFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.PhasedDecisionOperationNote;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.PhasedDecisionOperationStarted;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/**
 * Operation that determines {@link AccessDecision} for a given situation, described by operation URL, parameters, and so on.
 */
class EnforcerDecisionOperation extends EnforcerOperation {

    @NotNull final String operationUrl;
    @NotNull final AbstractAuthorizationParameters params;

    EnforcerDecisionOperation(
            @NotNull String operationUrl,
            @NotNull AbstractAuthorizationParameters params,
            @Nullable MidPointPrincipal principal,
            @NotNull SecurityEnforcer.Options options,
            @NotNull Beans beans,
            @NotNull Task task) {
        super(principal, options, beans, task);
        this.operationUrl = operationUrl;
        this.params = params;
    }

    @NotNull AccessDecision decideAccess(@Nullable AuthorizationPhaseType phase, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (phase == null) {
            var decision = decideAccessForPhase(REQUEST, result);
            if (decision != AccessDecision.ALLOW) {
                return decision;
            }
            return decideAccessForPhase(EXECUTION, result);
        } else {
            return decideAccessForPhase(phase, result);
        }
    }

    private @NotNull AccessDecision decideAccessForPhase(
            @NotNull AuthorizationPhaseType phase,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(operationUrl)) {
            return AccessDecision.DENY;
        }

        tracePhasedDecisionOperationStart(phase);

        // Step 1: Iterating through authorizations, computing the overall decision and the items we are allowed to touch.
        // (They will be used in step 2.)

        AccessDecision overallDecision = AccessDecision.DEFAULT;
        AutzItemPaths allowedItems = new AutzItemPaths();
        int i = 0;
        for (Authorization authorization : getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(i++, authorization, this, result);
            evaluation.traceStart();
            if (!evaluation.isApplicableToAction(operationUrl)
                    || !evaluation.isApplicableToPhase(nonStrict(phase))
                    || !evaluation.isApplicableToParameters(params)) {
                evaluation.traceEndNotApplicable();
                continue;
            }

            var autzConsumer = options.applicableAutzConsumer();
            if (autzConsumer != null) {
                autzConsumer.accept(authorization);
            }

            // The authorization is applicable to this situation. Now we can process the decision.
            if (authorization.isAllow()) {
                allowedItems.collectItems(authorization);
                evaluation.traceAuthorizationAllow(operationUrl);
                overallDecision = AccessDecision.ALLOW;
                // Do NOT break here. Other authorization statements may still deny the operation
            } else { // "deny" authorization
                var itemsMatchResult = evaluation.matchesOnItems(params);
                if (itemsMatchResult.value()) {
                    evaluation.traceAuthorizationDenyRelevant(operationUrl, itemsMatchResult);
                    overallDecision = AccessDecision.DENY;
                    break; // Break right here. Deny cannot be overridden by allow. This decision cannot be changed.
                } else {
                    evaluation.traceAuthorizationDenyIrrelevant(operationUrl, itemsMatchResult);
                }
            }

            // not recording evaluation end, because it was done in the code above
        }

        // Step 2: Checking the collected info on allowed items. We may still deny the operation.

        if (overallDecision == AccessDecision.ALLOW) {
            if (allowedItems.includesAllItems()) {
                tracePhasedDecisionOperationNote(phase, "Allowing all items => operation allowed");
            } else {
                // The object or delta must not contain any item that is not explicitly allowed.
                tracePhasedDecisionOperationNote(phase, "Checking for allowed items: %s", allowedItems);
                ItemDecisionOperation.SimpleTracer simpleTracer =
                        (message, msgParams) ->
                                tracePhasedDecisionOperationNote(
                                        phase,
                                        message.replace("{}", "%s"), // for LOGGER and .format based logging
                                        msgParams);
                var itemsDecision = new ItemDecisionOperation(simpleTracer).decideUsingAllowedItems(allowedItems, phase, params);
                if (itemsDecision != AccessDecision.ALLOW) {
                    tracePhasedDecisionOperationNote(
                            phase, "NOT ALLOWED operation because the 'items' decision is %s", itemsDecision);
                    overallDecision = AccessDecision.DEFAULT;
                }
            }
        }

        tracePhasedDecisionOperationEnd(phase, overallDecision);
        return overallDecision;
    }

    private void tracePhasedDecisionOperationStart(@NotNull AuthorizationPhaseType phase) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new PhasedDecisionOperationStarted(this, phase));
        }
    }

    private void tracePhasedDecisionOperationEnd(@NotNull AuthorizationPhaseType phase, AccessDecision decision) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new PhasedDecisionOperationFinished(this, phase, decision));
        }
    }

    private void tracePhasedDecisionOperationNote(@NotNull AuthorizationPhaseType phase, String message, Object... arguments) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new PhasedDecisionOperationNote(this, phase, message, arguments));
        }
    }

    @Override
    public boolean isFullInformationAvailable() {
        return params.isFullInformationAvailable();
    }
}
