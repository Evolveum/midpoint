/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.PhaseSelector.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.query.SelectorToFilterTranslator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.FilterOperationFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.FilterOperationStarted;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.PartialFilterOperationFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.PartialFilterOperationStarted;
import com.evolveum.midpoint.task.api.Task;
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

    private static final String PART_ID_PREFIX = "PART";

    @NotNull private final String[] operationUrls;
    @NotNull final Class<T> filterType;
    @NotNull final AuthorizationSelectorExtractor selectorExtractor;
    final ObjectFilter origFilter;
    private final String limitAuthorizationAction;
    private final List<OrderConstraintsType> paramOrderConstraints;
    @NotNull private final FilterGizmo<F> gizmo;
    private final String desc;

    EnforcerFilterOperation(
            @NotNull String[] operationUrls,
            @NotNull Class<T> filterType,
            @NotNull AuthorizationSelectorExtractor selectorExtractor,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            @NotNull FilterGizmo<F> gizmo,
            String desc,
            @Nullable MidPointPrincipal principal,
            @NotNull SecurityEnforcer.Options options,
            @NotNull Beans beans,
            @NotNull Task task) {
        super(principal, options, beans, task);
        this.operationUrls = operationUrls;
        this.filterType = filterType;
        this.selectorExtractor = selectorExtractor;
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
            F filterRequest = new PartialOp( strict(REQUEST)).computeFilter(result);
            F filterExecution = new PartialOp(strict(EXECUTION)).computeFilter(result);
            securityFilter =
                    gizmo.or(
                            filterBoth,
                            gizmo.and(filterRequest, filterExecution));
        }
        traceOperationEnd(securityFilter);
        return securityFilter;
    }

    public String getDesc() {
        return desc;
    }

    @SuppressWarnings("SameParameterValue")
    String debugDumpFilter(F filter, int indent) {
        return gizmo.debugDumpFilter(filter, indent);
    }

    /** Computes security filter for given {@link PhaseSelector}. */
    class PartialOp {

        @NotNull private final String id;

        private final @NotNull PhaseSelector phaseSelector;

        /** What objects/items do we need the access to (to evaluate the query), and do we have it? */
        @NotNull private final QueryObjectsAutzCoverage queryObjectsAutzCoverage = new QueryObjectsAutzCoverage();

        /** TODO */
        private F securityFilterAllow = null;

        /** TODO */
        private F securityFilterDeny = null;

        PartialOp(@NotNull PhaseSelector phaseSelector) {
            this.id = PART_ID_PREFIX + phaseSelector.getSymbol();
            this.phaseSelector = phaseSelector;
        }

        @NotNull EnforcerFilterOperation<T, F> getEnforcerFilterOperation() {
            return EnforcerFilterOperation.this;
        }

        /**
         * Returns additional security filter. This filter is supposed to be merged with the original filter.
         *
         * See also {@link SelectorToFilterTranslator}
         */
        private F computeFilter(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            queryObjectsAutzCoverage.addRequiredItems(filterType, origFilter); // MID-3916, MID-10206

            tracePartialOperationStarted();

            int i = 0;
            for (Authorization authorization : getAuthorizations()) {

                AuthorizationFilterEvaluation<T> autzEvaluation;
                autzEvaluation = new AuthorizationFilterEvaluation<>(
                        i++,
                        filterType,
                        origFilter,
                        authorization,
                        selectorExtractor.getSelectors(authorization),
                        selectorExtractor.getSelectorLabel(),
                        EnforcerFilterOperation.this,
                        result);

                autzEvaluation.traceStart();

                if (!autzEvaluation.isApplicableToActions(operationUrls)
                        || !autzEvaluation.isApplicableToLimitations(limitAuthorizationAction, operationUrls)
                        || !autzEvaluation.isApplicableToPhase(phaseSelector)
                        || !autzEvaluation.isApplicableToOrderConstraints(paramOrderConstraints)
                        || !selectorExtractor.isAuthorizationApplicable(autzEvaluation)) {
                    autzEvaluation.traceEndNotApplicable();
                    continue;
                }

                var applicable = autzEvaluation.computeFilter();

                // the end of processing of given authorization was logged as part of the method call above

                if (applicable) {
                    F autzSecurityFilter =
                            gizmo.adopt(
                                    ObjectQueryUtil.simplify(autzEvaluation.getAutzFilter()),
                                    authorization);
                    // The authorization is applicable to this situation. Now we can process the decision.
                    if (authorization.isAllow()) {
                        securityFilterAllow = gizmo.or(securityFilterAllow, autzSecurityFilter);
                        if (!gizmo.isNone(autzSecurityFilter)) {
                            collectItems(i, authorization, result);
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
                            tracePartialOperationFinished(secFilter, "deny all");
                            return secFilter;
                        } else {
                            securityFilterDeny = gizmo.or(securityFilterDeny, autzSecurityFilter);
                        }
                    }
                }
            }

            String unsatisfiedItemsDescription = queryObjectsAutzCoverage.getUnsatisfiedItemsDescription();
            if (unsatisfiedItemsDescription != null) {
                F secFilter = gizmo.createDenyAll();
                tracePartialOperationFinished(
                        secFilter, "deny because items " + unsatisfiedItemsDescription + " are not allowed");
                return secFilter;
            }
            securityFilterAllow = gizmo.simplify(securityFilterAllow);
            if (securityFilterAllow == null) {
                // Nothing has been allowed. This means default deny.
                F secFilter = gizmo.createDenyAll();
                tracePartialOperationFinished(secFilter, "nothing allowed => default is deny");
                return secFilter;
            } else if (securityFilterDeny == null) {
                // Nothing has been denied. We have "allow" filter only.
                tracePartialOperationFinished(securityFilterAllow, "nothing denied, something allowed");
                return securityFilterAllow;
            } else {
                // Both "allow" and "deny" filters
                F secFilter = gizmo.and(securityFilterAllow, gizmo.not(securityFilterDeny));
                tracePartialOperationFinished(secFilter, "allow with deny clauses");
                return secFilter;
            }
        }

        private void collectItems(int i, Authorization authorization, OperationResult result) throws ConfigurationException {
            for (var queryObjectAutzCoverageEntry : queryObjectsAutzCoverage.getAllEntries()) {
                Class<? extends ObjectType> type = queryObjectAutzCoverageEntry.getKey();
                var aEval = new AuthorizationSearchItemsEvaluation<>(
                        i, type, authorization, EnforcerFilterOperation.this, result);

                var authorizedSearchItems = aEval.getAuthorizedSearchItems();
                if (authorizedSearchItems != null) {
                    tracePartialOperationNote(
                            "Providing path sets for '%s' authorization for %s: positives: %s, negatives: %s",
                            authorization.getDecision(), type.getSimpleName(),
                            authorizedSearchItems.positives(), authorizedSearchItems.negatives());
                    queryObjectAutzCoverageEntry.getValue()
                            .processSearchItems(authorizedSearchItems, authorization.isAllow());
                }
            }
        }

        private void tracePartialOperationStarted() {
            if (tracer.isEnabled()) {
                tracer.trace(
                        new PartialFilterOperationStarted<>(
                                this,
                                phaseSelector,
                                queryObjectsAutzCoverage.shortDump()));
            }
        }

        private void tracePartialOperationNote(@Nullable String message, Object... arguments) {
            if (tracer.isEnabled()) {
                tracer.trace(
                        new SecurityTraceEvent.PartialFilterOperationNote<>(this, message, arguments));
            }
        }

        private void tracePartialOperationFinished(F secFilter, String comment) {
            if (tracer.isEnabled()) {
                tracer.trace(
                        new PartialFilterOperationFinished<>(
                                this,
                                phaseSelector,
                                secFilter,
                                comment));
            }
        }

        /** For diagnostics purposes. */
        @NotNull QueryObjectsAutzCoverage getQueryObjectsAutzCoverage() {
            return queryObjectsAutzCoverage;
        }

        /** For diagnostics purposes. */
        F getSecurityFilterAllow() {
            return securityFilterAllow;
        }

        /** For diagnostics purposes. */
        F getSecurityFilterDeny() {
            return securityFilterDeny;
        }

        public @NotNull String getId() {
            return id;
        }
    }

    private void traceOperationStart() {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new FilterOperationStarted(this));
        }
    }

    private void traceOperationEnd(F securityFilter) {
        if (tracer.isEnabled()) {
            tracer.trace(
                    new FilterOperationFinished<>(this, securityFilter));
        }
    }

    /**
     * Extracts relevant parts of authorizations for an {@link EnforcerFilterOperation}.
     */
    static abstract class AuthorizationSelectorExtractor {

        static AuthorizationSelectorExtractor forObject() {
            return new ForObject();
        }

        static AuthorizationSelectorExtractor forTarget(@NotNull PrismObject<? extends ObjectType> object) {
            return new ForTarget(object);
        }

        abstract List<ValueSelector> getSelectors(Authorization authorization) throws ConfigurationException;

        abstract String getSelectorLabel();

        /**
         * Returns `true` if the authorization is applicable in the current context. For example, when constructing
         * target-related filter, we may look if the authorization does match the object = assignment holder.
         */
        abstract boolean isAuthorizationApplicable(AuthorizationFilterEvaluation<?> autzEvaluation)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException;

        static class ForObject extends AuthorizationSelectorExtractor {

            @Override
            List<ValueSelector> getSelectors(Authorization authorization) throws ConfigurationException {
                return authorization.getParsedObjectSelectors();
            }

            @Override
            String getSelectorLabel() {
                return "object";
            }

            @Override
            boolean isAuthorizationApplicable(AuthorizationFilterEvaluation<?> autzEvaluation) {
                return true;
            }

            @Override
            public String toString() {
                return "object";
            }
        }

        static class ForTarget extends AuthorizationSelectorExtractor {
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
            boolean isAuthorizationApplicable(AuthorizationFilterEvaluation<?> autzEvaluation)
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
