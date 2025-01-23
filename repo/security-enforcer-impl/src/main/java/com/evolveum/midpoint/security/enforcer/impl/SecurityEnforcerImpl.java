/*
 * Copyright (C) 2014-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.EnforcerFilterOperation.AuthorizationSelectorExtractor.forObject;
import static com.evolveum.midpoint.security.enforcer.impl.EnforcerFilterOperation.AuthorizationSelectorExtractor.forTarget;
import static com.evolveum.midpoint.security.enforcer.impl.PhaseSelector.nonStrict;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.REQUEST;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.security.enforcer.impl.EnforcerFilterOperation.AuthorizationSelectorExtractor;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
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
 * - object selector evaluation layer: see {@link SelectorEvaluation} and its child
 * - object selector clause evaluation layer, see `clauses` package
 *
 * @author Radovan Semancik
 */
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @Autowired private Beans beans;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    @Override
    public void failAuthorization(
            String operationUrl, AuthorizationPhaseType phase, AbstractAuthorizationParameters params, OperationResult result)
            throws SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        String username = getQuotedUsername(principal);
        Object object;
        PrismObject<?> target;
        if (params instanceof AuthorizationParameters<?, ?> ap) {
            object = ap.getAnyObject();
            target = ap.getTarget();
        } else if (params instanceof ValueAuthorizationParameters<?> vp) {
            PrismValue value = vp.getValue();
            object = value != null ? MiscUtil.getDiagInfo(value) : null; // TODO better name
            target = null;
        } else {
            throw new NotHereAssertionError();
        }
        String message;
        SingleLocalizableMessage userFriendlyMessage;
        String translatedOperation = beans.expressionFactory.getLocalizationService().translate(
                new SingleLocalizableMessage(operationUrl));
        String translatedOperationEng = beans.expressionFactory.getLocalizationService().translate(
                new SingleLocalizableMessage(operationUrl), new Locale("en", "US"));
        // TODO are double quotes around username intentional?
        if (target == null && object == null) {
            message = "User '" + username + "' not authorized for operation " + translatedOperationEng;
            userFriendlyMessage = new SingleLocalizableMessage("security.enforcer.message.notAuthorized",
                    new Object[]{username, translatedOperation}, message);
        } else if (target == null) {
            message = "User '" + username + "' not authorized for operation " + translatedOperationEng + " on " + object;
            userFriendlyMessage = new SingleLocalizableMessage("security.enforcer.message.notAuthorized.onObject",
                    new Object[]{username, translatedOperation, object}, message);
        } else {
            message = "User '" + username + "' not authorized for operation " + translatedOperationEng + " on " + object + " with target " + target;
            userFriendlyMessage = new SingleLocalizableMessage("security.enforcer.message.notAuthorized.onObject.withTarget",
                    new Object[]{username, translatedOperation, object, target}, message);
        }
        LOGGER.error("{}", message);
        AuthorizationException e = new AuthorizationException(userFriendlyMessage);
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

    @Override
    public @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new EnforcerDecisionOperation(operationUrl, params, principal, options, beans, task)
                .decideAccess(phase, result);
    }

    @Override
    public @Nullable MidPointPrincipal getMidPointPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            LOGGER.debug("No authentication");
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            LOGGER.debug("Null principal");
            return null;
        }
        if (principal instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            return null;
        }
        LOGGER.debug("Unknown principal type {}", principal.getClass());
        return null;
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, boolean fullInformationAvailable, @NotNull Options options,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new CompileConstraintsOperation<O>(
                getMidPointPrincipal(),
                options,
                beans,
                CompileConstraintsOptions.create().withFullInformationAvailable(fullInformationAvailable),
                task)
                .compileSecurityConstraints(object, result);
    }

    @Override
    public PrismEntityOpConstraints.ForValueContent compileOperationConstraints(
            @Nullable MidPointPrincipal principal,
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @NotNull String[] actionUrls,
            @NotNull Options enforcerOptions,
            @NotNull CompileConstraintsOptions compileConstraintsOptions,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return new CompileConstraintsOperation<>(
                principal, enforcerOptions, beans, compileConstraintsOptions, task)
                .compileValueOperationConstraints(value, phase, actionUrls, result);
    }

    @Override
    public @Nullable <T> ObjectFilter preProcessObjectFilter(
            @Nullable MidPointPrincipal principal,
            @NotNull String @NotNull [] operationUrls,
            @Nullable AuthorizationPhaseType phase,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter origFilter,
            @Nullable String limitAuthorizationAction,
            @NotNull List<OrderConstraintsType> paramOrderConstraints,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        FilterGizmo<ObjectFilter> gizmo = new FilterGizmoObjectFilterImpl();
        ObjectFilter securityFilter = computeSecurityFilterInternal(
                principal, operationUrls, phase, filterType, forObject(), origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "filter pre-processing",
                options, task, result);
        ObjectFilter finalFilter = gizmo.and(origFilter, securityFilter);
        LOGGER.trace("SEC: pre-processed object filter (combined with the original one):\n{}",
                DebugUtil.debugDumpLazily(finalFilter, 1)); // This is not a part of the flexible tracing
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
            Class<T> filterType,
            @NotNull PrismObject<O> object,
            ObjectFilter origFilter,
            String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints,
            FilterGizmo<F> gizmo,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return computeSecurityFilterInternal(
                principal, operationUrls, phase, filterType, forTarget(object), origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, "security filter computation",
                Options.create(), task, result);
    }

    private <T, F> F computeSecurityFilterInternal(
            @Nullable MidPointPrincipal principal,
            @NotNull String[] operationUrls,
            @Nullable AuthorizationPhaseType phase,
            @NotNull Class<T> filterType,
            @NotNull AuthorizationSelectorExtractor selectorExtractor,
            @Nullable ObjectFilter origFilter,
            @Nullable String limitAuthorizationAction,
            @Nullable List<OrderConstraintsType> paramOrderConstraints,
            @NotNull FilterGizmo<F> gizmo,
            String desc,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new EnforcerFilterOperation<>(
                operationUrls, filterType, selectorExtractor, origFilter, limitAuthorizationAction,
                paramOrderConstraints, gizmo, desc, principal, options, beans, task)
                .computeSecurityFilter(phase, result);
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
            PrismObject<R> target, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        EnforcerOperation ctx = new EnforcerOperation(midPointPrincipal, Options.create(), beans, task);
        ItemSecurityConstraintsImpl itemConstraints = new ItemSecurityConstraintsImpl();
        int i = 0;
        for (Authorization autz : ctx.getAuthorizations()) {
            var evaluation = new AuthorizationEvaluation(i++, autz, ctx, result);
            evaluation.traceStart();
            if (evaluation.isApplicableToAction(operationUrl)
                    && evaluation.isApplicableToPhase(nonStrict(REQUEST))
                    && evaluation.isApplicableToObject(object)
                    && evaluation.isApplicableToTarget(target)) {
                itemConstraints.collectItems(autz);
                evaluation.traceEndApplied();
            } else {
                evaluation.traceEndNotApplicable();
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
        AccessDecision decision = decideAccess(
                attorneyPrincipal, attorneyAuthorizationAction, null, autzParams,
                Options.create().withApplicableAutzConsumer(limitationsCollector),
                task, result);
        if (decision != AccessDecision.ALLOW) {
            failAuthorization(attorneyAuthorizationAction, null, autzParams, result);
        }

        // For running operations by Power Of Attorney, we don't need to support GUI config
        MidPointPrincipal donorPrincipal =
                securityContextManager.getUserProfileService().getPrincipal(
                        donor,
                        limitationsCollector,
                        ProfileCompilerOptions.createNotCompileGuiAdminConfiguration()
                                .locateSecurityPolicy(false),
                        result);
        donorPrincipal.setAttorney(attorneyPrincipal.getFocus());

        // chain principals so we can easily drop the power of attorney and return back to original identity
        donorPrincipal.setPreviousPrincipal(attorneyPrincipal);

        return donorPrincipal;
    }

    @Override
    public <O extends ObjectType> AccessDecision determineItemDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            @NotNull ItemPath itemPath) {
        return new ItemDecisionOperation(createSimpleTracer())
                .determineItemDecision(securityConstraints, delta, currentObject, operationUrl, phase, itemPath);
    }

    @Override
    public <C extends Containerable> AccessDecision determineItemValueDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull PrismContainerValue<C> containerValue,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            boolean consideringCreation,
            @NotNull String decisionContextDesc) {
        return new ItemDecisionOperation(createSimpleTracer())
                .determineItemValueDecision(
                        securityConstraints, containerValue, !consideringCreation, operationUrl, phase, decisionContextDesc);
    }

    /** Temporary implementation. */
    private ItemDecisionOperation.SimpleTracer createSimpleTracer() {
        if (LOGGER.isTraceEnabled()) {
            return (message, params) -> LOGGER.trace(message, params);
        } else {
            return (message, params) -> {};
        }
    }
}
