/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.PhaseSelector.nonStrict;
import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.function.Consumer;

import com.evolveum.midpoint.security.enforcer.api.AbstractAuthorizationParameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/**
 * Operation that determines {@link AccessDecision} for a given situation, described by operation URL, parameters, and so on.
 */
class EnforcerDecisionOperation extends EnforcerOperation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final String operationUrl;
    @NotNull private final AbstractAuthorizationParameters params;
    @Nullable private final Consumer<Authorization> applicableAutzConsumer;

    EnforcerDecisionOperation(
            @NotNull String operationUrl,
            @NotNull AbstractAuthorizationParameters params,
            @Nullable Consumer<Authorization> applicableAutzConsumer,
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Beans beans,
            @NotNull Task task) {
        super(principal, ownerResolver, beans, task);

        this.operationUrl = operationUrl;
        this.params = params;
        this.applicableAutzConsumer = applicableAutzConsumer;
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
            var evaluation = new AuthorizationEvaluation(String.valueOf(i++), authorization, this, result);
            evaluation.traceStart();
            if (!evaluation.isApplicableToAction(operationUrl)
                    || !evaluation.isApplicableToPhase(nonStrict(phase))
                    || !evaluation.isApplicableToParameters(params)) {
                evaluation.traceEndNotApplicable();
                continue;
            }

            if (applicableAutzConsumer != null) {
                applicableAutzConsumer.accept(authorization);
            }

            // The authorization is applicable to this situation. Now we can process the decision.
            if (authorization.isAllow()) {
                allowedItems.collectItems(authorization);
                traceAuthorizationAllow();
                overallDecision = AccessDecision.ALLOW;
                // Do NOT break here. Other authorization statements may still deny the operation
            } else { // "deny" authorization
                if (evaluation.matchesItems(params)) {
                    traceAuthorizationDenyRelevant();
                    overallDecision = AccessDecision.DENY;
                    break; // Break right here. Deny cannot be overridden by allow. This decision cannot be changed.
                } else {
                    traceAuthorizationDenyIrrelevant();
                }
            }
        }

        // Step 2: Checking the collected info on allowed items. We may still deny the operation.

        if (overallDecision == AccessDecision.ALLOW) {
            if (allowedItems.includesAllItems()) {
                LOGGER.trace("  Empty list of allowed items, operation allowed");
            } else {
                // The object and delta must not contain any item that is not explicitly allowed.
                LOGGER.trace("  Checking for allowed items: {}", allowedItems);
                var itemsDecision = new ItemDecisionOperation().onAllowedItems(allowedItems, phase, params);
                if (itemsDecision != AccessDecision.ALLOW) {
                    LOGGER.trace("    NOT ALLOWED operation because the item decision is {}", itemsDecision);
                    overallDecision = AccessDecision.DEFAULT;
                }
            }
        }

        tracePhasedDecisionOperationEnd(phase, overallDecision);
        return overallDecision;
    }

    private void tracePhasedDecisionOperationStart(AuthorizationPhaseType phase) {
        if (traceEnabled) {
            LOGGER.trace("SEC: START access decision for principal={}, op={}, phase={}, {}",
                    username, prettyActionUrl(operationUrl), phase, params.shortDump());
        }
    }

    private void tracePhasedDecisionOperationEnd(AuthorizationPhaseType phase, AccessDecision decision) {
        if (traceEnabled) {
            LOGGER.trace("AUTZ END access decision: principal={}, op={}, phase={}: {}",
                    username, prettyActionUrl(operationUrl), phase, decision);
        }
    }

    private void traceAuthorizationAllow() {
        if (traceEnabled) {
            LOGGER.trace("    ALLOW operation {} => but continuing evaluation of other authorizations", operationUrl);
        }
    }

    private void traceAuthorizationDenyIrrelevant() {
        if (traceEnabled) {
            LOGGER.trace("    DENY authorization not matching items => continuing evaluation of other authorizations");
        }
    }

    private void traceAuthorizationDenyRelevant() {
        if (traceEnabled) {
            LOGGER.trace("    DENY authorization matching items => denying the whole operation: {}", operationUrl);
        }
    }
}
