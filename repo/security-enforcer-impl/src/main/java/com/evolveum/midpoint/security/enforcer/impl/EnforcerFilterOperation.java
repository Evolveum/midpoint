/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.PhaseSelector.*;
import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.FILTER_TRACE_ENABLED;
import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.List;

import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.query.SelectorToFilterTranslator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

/**
 * Operation that computes a "security filter", i.e. additional restrictions to be applied in a given filtering/searching
 * situation.
 *
 * @param <T> type of the objects being filtered
 * @param <F> type of the filter being produced, see {@link FilterGizmo}
 */
class EnforcerFilterOperation<T, F> extends EnforcerOperation {

    @NotNull private final String[] operationUrls;
    @NotNull private final Class<T> filterType;
    @NotNull private final AuthorizationPreProcessor preProcessor;
    private final boolean includeSpecial;
    private final ObjectFilter origFilter;
    private final String limitAuthorizationAction;
    private final List<OrderConstraintsType> paramOrderConstraints;
    @NotNull private final FilterGizmo<F> gizmo;
    private final String desc;

    EnforcerFilterOperation(
            @NotNull String[] operationUrls,
            @NotNull Class<T> filterType,
            @NotNull AuthorizationPreProcessor preProcessor,
            boolean includeSpecial,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            @NotNull FilterGizmo<F> gizmo,
            String desc,
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Beans beans,
            @NotNull Task task) {
        super(principal, ownerResolver, beans, task);
        this.operationUrls = operationUrls;
        this.filterType = filterType;
        this.preProcessor = preProcessor;
        this.includeSpecial = includeSpecial;
        this.origFilter = origFilter;
        this.limitAuthorizationAction = limitAuthorizationAction;
        this.paramOrderConstraints = paramOrderConstraints;
        this.gizmo = gizmo;
        this.desc = desc;
    }

    F computeSecurityFilter(@Nullable AuthorizationPhaseType phase, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        traceOperationStart();
        F securityFilter;
        if (phase != null) {
            securityFilter = new PartialOp(nonStrict(phase)).computeFilter(result);
        } else {
            F filterBoth = new PartialOp(both()).computeFilter(result);
            F filterRequest = new PartialOp(strict(REQUEST)).computeFilter(result);
            F filterExecution = new PartialOp(strict(EXECUTION)).computeFilter(result);
            securityFilter =
                    gizmo.or(
                            filterBoth,
                            gizmo.and(filterRequest, filterExecution));
        }
        traceOperationEnd(securityFilter);
        return securityFilter;
    }

    /** Computes security filter for given {@link PhaseSelector}. */
    private class PartialOp {

        private final @NotNull PhaseSelector phaseSelector;

        /** TODO */
        @NotNull private final QueryAutzItemPaths queryItemsSpec = new QueryAutzItemPaths();

        /** TODO */
        private F securityFilterAllow = null;

        /** TODO */
        private F securityFilterDeny = null;

        PartialOp(@NotNull PhaseSelector phaseSelector) {
            this.phaseSelector = phaseSelector;
        }

        /**
         * Returns additional security filter. This filter is supposed to be merged with the original filter.
         *
         * See also {@link SelectorToFilterTranslator}
         */
        private F computeFilter(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            queryItemsSpec.addRequiredItems(origFilter); // MID-3916
            tracePartialOperationStart();

            int i = 0;
            for (Authorization authorization : getAuthorizations()) {

                AuthorizationFilterEvaluation<T> autzEvaluation;
                autzEvaluation = new AuthorizationFilterEvaluation<>(
                        String.valueOf(i++),
                        filterType,
                        origFilter,
                        authorization,
                        preProcessor.getSelectors(authorization),
                        preProcessor.getSelectorLabel(),
                        includeSpecial,
                        EnforcerFilterOperation.this,
                        result);

                autzEvaluation.traceStart();

                if (!autzEvaluation.isApplicableToActions(operationUrls)
                        || !autzEvaluation.isApplicableToLimitations(limitAuthorizationAction, operationUrls)
                        || !autzEvaluation.isApplicableToPhase(phaseSelector)
                        || !autzEvaluation.isApplicableToOrderConstraints(paramOrderConstraints)
                        || !preProcessor.isApplicable(autzEvaluation)) {
                    autzEvaluation.traceEndNotApplicable();
                    continue;
                }

                var applicable = autzEvaluation.computeFilter();

                if (applicable) {
                    F autzSecurityFilter =
                            gizmo.adopt(
                                    ObjectQueryUtil.simplify(autzEvaluation.getAutzFilter()),
                                    authorization);
                    // The authorization is applicable to this situation. Now we can process the decision.
                    if (authorization.isAllow()) {
                        securityFilterAllow = gizmo.or(securityFilterAllow, autzSecurityFilter);
                        traceIntermediary("after 'allow' authorization", securityFilterAllow);
                        if (!gizmo.isNone(autzSecurityFilter)) {
                            // TODO resolve for shifted authorizations!
                            queryItemsSpec.collectItems(authorization);
                        }
                    } else { // "deny" type authorization
                        if (authorization.hasItemSpecification()) {
                            // TODO resolve for shifted authorizations!
                            // This is a tricky situation. We have deny authorization, but it only denies access to
                            // some items. Therefore we need to find the objects and then filter out the items.
                            // Therefore do not add this authorization into the filter.
                        } else if (gizmo.isAll(autzSecurityFilter)) {
                            // This is "deny all". We cannot have anything stronger than that.
                            // There is no point in continuing the evaluation.
                            F secFilter = gizmo.createDenyAll();
                            tracePartialOperationEnd(secFilter, "deny all");
                            return secFilter;
                        } else {
                            securityFilterDeny = gizmo.or(securityFilterDeny, autzSecurityFilter);
                        }
                    }
                }
                trace();
            }

            traceDetails();

            List<ItemPath> unsatisfiedItems = queryItemsSpec.evaluateUnsatisfiedItems();
            if (!unsatisfiedItems.isEmpty()) {
                F secFilter = gizmo.createDenyAll();
                // TODO lazy string concatenation?
                tracePartialOperationEnd(secFilter, "deny because items " + unsatisfiedItems + " are not allowed");
                return secFilter;
            }
            securityFilterAllow = gizmo.simplify(securityFilterAllow);
            if (securityFilterAllow == null) {
                // Nothing has been allowed. This means default deny.
                F secFilter = gizmo.createDenyAll();
                tracePartialOperationEnd(secFilter, "default deny");
                return secFilter;
            } else if (securityFilterDeny == null) {
                // Nothing has been denied. We have "allow" filter only.
                tracePartialOperationEnd(securityFilterAllow, "allow");
                return securityFilterAllow;
            } else {
                // Both "allow" and "deny" filters
                F secFilter = gizmo.and(securityFilterAllow, gizmo.not(securityFilterDeny));
                tracePartialOperationEnd(secFilter, "allow with deny clauses");
                return secFilter;
            }
        }

        void trace() {
            traceIntermediary("total 'allow' after authorization", securityFilterAllow);
            traceIntermediary("total 'deny' after authorization", securityFilterDeny);
            tracePaths("after authorization", queryItemsSpec);
        }

        void traceDetails() {
            traceIntermediary( "(total 'allow') after all authorizations", securityFilterAllow);
            traceIntermediary( "(total 'deny') after all authorizations", securityFilterDeny);
            tracePaths("after all authorizations", queryItemsSpec);
        }

        private void tracePartialOperationStart() {
            if (traceEnabled) {
                LOGGER.trace("{} Starting partial filter determination ({}) for phase={}, initial query items specification: {}",
                        OP_START, desc, phaseSelector, queryItemsSpec.shortDumpLazily());
            }
        }

        private void tracePartialOperationEnd(F secFilter, String comment) {
            if (traceEnabled) {
                LOGGER.trace("{} Finished partial filter determination ({}) for phase={}: {}\n{}",
                        OP_END, desc, phaseSelector, comment, gizmo.debugDumpFilter(secFilter, 1));
            }
        }

        private void traceIntermediary(String message, F filter) {
            if (FILTER_TRACE_ENABLED && traceEnabled) {
                LOGGER.trace("{} filter {}:\n{}",
                        OP, message, gizmo.debugDumpFilter(filter, 1));
            }
        }

        private void tracePaths(String message, QueryAutzItemPaths paths) {
            if (FILTER_TRACE_ENABLED && traceEnabled) {
                LOGGER.trace("{} paths {}: {}", OP, message, paths.shortDump());
            }
        }
    }

    private void traceOperationStart() {
        if (traceEnabled) {
            // TODO desc?
            LOGGER.trace(
                    "{} computing security filter principal={}, searchResultType={}, searchType={}: orig filter {}",
                    SEC_START, username, TracingUtil.getTypeName(filterType), preProcessor, origFilter);
        }
    }

    private void traceOperationEnd(F securityFilter) {
        if (traceEnabled) {
            // TODO desc?
            LOGGER.trace("{} computed security filter principal={}, searchResultType={}: {}\n{}",
                    SEC_END, username, TracingUtil.getTypeName(filterType),
                    securityFilter, DebugUtil.debugDump(securityFilter, 1));
        }
    }

    /**
     * Extracts relevant parts of authorizations for an {@link EnforcerFilterOperation}.
     *
     * TODO better name
     */
    static abstract class AuthorizationPreProcessor {

        static AuthorizationPreProcessor forObject() {
            return new ForObject();
        }

        static AuthorizationPreProcessor forTarget(@NotNull PrismObject<? extends ObjectType> object) {
            return new ForTarget(object);
        }

        abstract List<ValueSelector> getSelectors(Authorization authorization) throws ConfigurationException;

        abstract String getSelectorLabel();

        abstract boolean isApplicable(AuthorizationFilterEvaluation<?> autzEvaluation)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException;

        static class ForObject extends AuthorizationPreProcessor {

            @Override
            List<ValueSelector> getSelectors(Authorization authorization) throws ConfigurationException {
                return authorization.getParsedObjectSelectors();
            }

            @Override
            String getSelectorLabel() {
                return "object";
            }

            @Override
            boolean isApplicable(AuthorizationFilterEvaluation<?> autzEvaluation) {
                return true;
            }

            @Override
            public String toString() {
                return "object";
            }
        }

        static class ForTarget extends AuthorizationPreProcessor {
            @NotNull private final PrismObject<? extends ObjectType> object;

            ForTarget(@NotNull PrismObject<? extends ObjectType> object) {
                this.object = object;
            }

            @Override
            List<ValueSelector> getSelectors(Authorization authorization) throws ConfigurationException {
                return authorization.getParsedTargetSelectors();
            }

            @Override
            String getSelectorLabel() {
                return "target";
            }

            @Override
            boolean isApplicable(AuthorizationFilterEvaluation<?> autzEvaluation)
                    throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                    ConfigurationException, ObjectNotFoundException {
                return autzEvaluation.isApplicableToObject(object);
            }

            @Override
            public String toString() {
                return "target for " + object;
            }
        }
    }
}
