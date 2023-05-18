/*
 * Copyright (C) 2014-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO provide some description here
 *
 * Until it's done, please see
 *
 * - https://docs.evolveum.com/midpoint/reference/security/authorization/configuration/
 * - https://docs.evolveum.com/midpoint/reference/diag/troubleshooting/authorizations/
 *
 * The processing is divided into the following layers:
 *
 * - enforcer operation layer: see {@link EnforcerOperation} and its children
 * - single authorization evaluation layer: see {@link AuthorizationEvaluation} and its child
 * - object selector evaluation layer: see {@link ObjectSelectorEvaluation} and its child
 * - object selector clause evaluation layer, see `clauses` package
 *
 * @author Radovan Semancik
 */
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    static final boolean FILTER_TRACE_ENABLED = true;

    @Autowired private Beans beans;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    @Override
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        var decision = decideAccessInternal(
                getMidPointPrincipal(), operationUrl, phase, params, ownerResolver, null, task, result);
        return decision == AccessDecision.ALLOW;
    }

    @Override
    public @NotNull <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull List<String> operationUrls,
            @NotNull AuthorizationParameters<O, T> params,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        AccessDecision finalDecision = AccessDecision.DEFAULT;
        for (String operationUrl : operationUrls) {
            AccessDecision decision = decideAccessInternal(
                    principal, operationUrl, null, params, null, null, task, result);
            switch (decision) {
                case DENY:
                    return AccessDecision.DENY;
                case ALLOW:
                    finalDecision = AccessDecision.ALLOW;
                    break;
                case DEFAULT:
                    // no change of the final decision
            }
        }
        return finalDecision;
    }

    @Override
    public <O extends ObjectType, T extends ObjectType> void failAuthorization(
            String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params, OperationResult result)
            throws SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        String username = getQuotedUsername(principal);
        PrismObject<T> target = params.getTarget();
        PrismObject<O> object = params.getAnyObject();
        String message;
        // TODO are double quotes around username intentional?
        if (target == null && object == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl;
        } else if (target == null) {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + object;
        } else {
            message = "User '" + username + "' not authorized for operation " + operationUrl + " on " + object + " with target " + target;
        }
        LOGGER.error("{}", message);
        AuthorizationException e = new AuthorizationException(message);
        // Not using "record" because the operation may continue; actually, it is questionable if we should set the status at all.
        result.setFatalError(e);
        throw e;
    }

    private String getQuotedUsername(MidPointPrincipal principal) {
        if (principal != null) {
            return "'" + principal.getUsername() + "'";
        } else {
            return "(none)";
        }
    }

    private <O extends ObjectType, T extends ObjectType> @NotNull AccessDecision decideAccessInternal(
            @Nullable MidPointPrincipal principal,
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<O, T> params,
            @Nullable OwnerResolver ownerResolver,
            @Nullable Consumer<Authorization> applicableAutzConsumer,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new EnforcerDecisionOperation<>(
                operationUrl, params, applicableAutzConsumer, principal, ownerResolver, beans, task)
                .decideAccess(phase, result);
    }

    @Override
    public @Nullable MidPointPrincipal getMidPointPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            LOGGER.warn("No authentication");
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            LOGGER.warn("Null principal");
            return null;
        }
        if (principal instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            return null;
        }
        LOGGER.warn("Unknown principal type {}", principal.getClass());
        return null;
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new OtherEnforcerOperation<O>(getMidPointPrincipal(), ownerResolver, beans, task)
                .compileSecurityConstraints(object, result);
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectOperationConstraints compileOperationConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Collection<String> actionUrls, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new OtherEnforcerOperation<O>(getMidPointPrincipal(), ownerResolver, beans, task)
                .compileOperationConstraints(object, actionUrls, result);
    }

    @Override
    public @NotNull <O extends ObjectType> PrismEntityOpConstraints.ForValueContent compileValueOperationConstraints(
            @NotNull PrismObject<O> object,
            @Nullable AuthorizationPhaseType phase,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Collection<String> actionUrls,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return new OtherEnforcerOperation<O>(getMidPointPrincipal(), ownerResolver, beans, task)
                .compileValueOperationConstraints(object, phase, actionUrls, result);
    }

    @Override
    public @Nullable <O extends ObjectType> ObjectFilter preProcessObjectFilter(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<O> searchResultType,
            @Nullable ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl();
        ObjectFilter securityFilter = computeSecurityFilterInternal(
                operationUrls, phase, searchResultType, null, SearchType.OBJECT, true, origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "filter pre-processing",
                task, result);
        ObjectFilter finalFilter = gizmo.and(origFilter, securityFilter);
        LOGGER.trace("AUTZ: pre-processed object filter (combined with the original one):\n{}",
                DebugUtil.debugDumpLazily(finalFilter, 1));
        if (finalFilter instanceof AllFilter) {
            return null; // compatibility
        } else {
            return finalFilter;
        }
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeTargetSecurityFilter(
            MidPointPrincipal principal,
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            @NotNull PrismObject<O> object,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return computeSecurityFilterInternal(
                operationUrls, phase, searchResultType, object, SearchType.TARGET, true, origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "security filter computation",
                task, result);
    }

    private <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilterInternal(
            String[] operationUrls,
            @Nullable AuthorizationPhaseType phase,
            Class<T> searchResultType,
            PrismObject<O> object,
            @NotNull SearchType searchType,
            boolean includeSpecial,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            String desc,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new EnforcerFilterOperation<>(
                operationUrls, searchResultType, object, searchType, includeSpecial, origFilter, limitAuthorizationAction,
                paramOrderConstraints, gizmo, desc, getMidPointPrincipal(), null, beans, task)
                .computeSecurityFilter(phase, result);
    }

    @Override
    public <T extends ObjectType> boolean canSearch(
            String[] operationUrls,
            AuthorizationPhaseType phase,
            Class<T> searchResultType,
            boolean includeSpecial,
            ObjectFilter origFilter,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl();
        var securityFilter = computeSecurityFilterInternal(
                operationUrls, phase, searchResultType, null, SearchType.OBJECT, includeSpecial, origFilter,
                null, null, gizmo, "canSearch decision",
                task, result);
        ObjectFilter finalFilter =
                ObjectQueryUtil.simplify(
                        ObjectQueryUtil.filterAnd(origFilter, securityFilter));
        return !(finalFilter instanceof NoneFilter);
    }

    static String prettyActionUrl(String fullUrl) {
        return DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrl);
    }

    static String prettyActionUrl(Collection<String> fullUrls) {
        return fullUrls.stream()
                .map(url -> prettyActionUrl(url))
                .collect(Collectors.joining(", "));
    }

    static String prettyActionUrl(String[] fullUrls) {
        if (fullUrls.length == 1) {
            return prettyActionUrl(fullUrls[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fullUrls.length; i++) {
                sb.append(prettyActionUrl(fullUrls[i]));
                if (i < fullUrls.length - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
    }


    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal, String operationUrl, PrismObject<O> object,
            PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        EnforcerOperation<?> ctx = new EnforcerOperation<>(midPointPrincipal, null, beans, task);
        ItemSecurityConstraintsImpl itemConstraints = new ItemSecurityConstraintsImpl();
        for (Authorization autz : ctx.getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(autz, ctx, result);
            if (evaluation.isApplicableToAction(operationUrl)
                    && evaluation.isApplicableToPhase(REQUEST, true)
                    && evaluation.isApplicableToObject(object)
                    && evaluation.isApplicableToTarget(target)) {
                itemConstraints.collectItems(autz);
            }
        }
        return itemConstraints;
    }

    @Override
    public <F extends FocusType> MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal,
            String attorneyAuthorizationAction, PrismObject<F> donor, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (attorneyPrincipal.getAttorney() != null) {
            throw new UnsupportedOperationException("Transitive attorney is not supported yet");
        }

        AuthorizationLimitationsCollector limitationsCollector = new AuthorizationLimitationsCollector();
        AuthorizationParameters<F, ObjectType> autzParams = AuthorizationParameters.Builder.buildObject(donor);
        AccessDecision decision = decideAccessInternal(
                attorneyPrincipal, attorneyAuthorizationAction, null, autzParams,
                null, limitationsCollector, task, result);
        if (decision != AccessDecision.ALLOW) {
            failAuthorization(attorneyAuthorizationAction, null, autzParams, result);
        }

        MidPointPrincipal donorPrincipal =
                securityContextManager.getUserProfileService().getPrincipal(donor, limitationsCollector, result);
        donorPrincipal.setAttorney(attorneyPrincipal.getFocus());

        // chain principals so we can easily drop the power of attorney and return back to original identity
        donorPrincipal.setPreviousPrincipal(attorneyPrincipal);

        return donorPrincipal;
    }

    @Override
    public <O extends ObjectType> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            String operationUrl,
            AuthorizationPhaseType phase,
            ItemPath itemPath) {
        return new ItemDecisionOperation()
                .onSecurityConstraints(securityConstraints, delta, currentObject, operationUrl, phase, itemPath);
    }

    @Override
    public <C extends Containerable> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            PrismContainerValue<C> containerValue,
            String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemPath,
            PlusMinusZero plusMinusZero,
            String decisionContextDesc) {
        boolean removingContainer = plusMinusZero == PlusMinusZero.MINUS;
        return new ItemDecisionOperation()
                .onSecurityConstraints2(
                        securityConstraints, containerValue, removingContainer, operationUrl, phase, itemPath, decisionContextDesc);
    }

    // TODO name
    enum SearchType {
        OBJECT, TARGET
    }
}
