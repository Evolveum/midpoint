/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.authorization.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.impl.authorization.AuthorizationActionValue;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.impl.util.EndPointsUrlMapping;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.security.api.*;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer, SecurityContextManager, AccessDecisionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointGuiAuthorizationEvaluator.class);

    private static final String AUTH_URL = "/" + ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE + "/*";

    private final SecurityEnforcer securityEnforcer;
    private final SecurityContextManager securityContextManager;
    private final TaskManager taskManager;

    public MidPointGuiAuthorizationEvaluator(SecurityEnforcer securityEnforcer, SecurityContextManager securityContextManager, TaskManager taskManager) {
        super();
        this.securityEnforcer = securityEnforcer;
        this.securityContextManager = securityContextManager;
        this.taskManager = taskManager;
    }

    @Override
    public MidPointPrincipalManager getUserProfileService() {
        return securityContextManager.getUserProfileService();
    }

    @Override
    public void setUserProfileService(MidPointPrincipalManager guiProfiledPrincipalManager) {
        securityContextManager.setUserProfileService(guiProfiledPrincipalManager);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        securityContextManager.setupPreAuthenticatedSecurityContext(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        securityContextManager.setupPreAuthenticatedSecurityContext(focus);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(MidPointPrincipal principal) {
        securityContextManager.setupPreAuthenticatedSecurityContext(principal);
    }

    @Override
    public boolean isAuthenticated() {
        return securityContextManager.isAuthenticated();
    }

    @Override
    public Authentication getAuthentication() {
        return securityContextManager.getAuthentication();
    }

    @Override
    public MidPointPrincipal getPrincipal() throws SecurityViolationException {
        return securityContextManager.getPrincipal();
    }

    @Override
    public String getPrincipalOid() {
        return securityContextManager.getPrincipalOid();
    }

    @Override
    public void setTemporaryPrincipalOid(String value) {
        securityContextManager.setTemporaryPrincipalOid(value);
    }

    @Override
    public void clearTemporaryPrincipalOid() {
        securityContextManager.clearTemporaryPrincipalOid();
    }

    @Override
    public void failAuthorization(String operationUrl,
            AuthorizationPhaseType phase, AbstractAuthorizationParameters params,
            OperationResult result) throws SecurityViolationException {
        securityEnforcer.failAuthorization(operationUrl, phase, params, result);
    }

    // MidPoint pages invoke this method (through PageBase)
    @Override
    public boolean isAuthorized(
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return attribute instanceof SecurityConfig
                // class name equals, because WebExpressionConfigAttribute is non public class
                || "org.springframework.security.web.access.expression.WebExpressionConfigAttribute".equals(attribute.getClass().getName());
    }

    @Override
    public boolean supports(Class<?> clazz) {
        if (MethodInvocation.class.isAssignableFrom(clazz)) {
            return true;
        } else {
            return FilterInvocation.class.isAssignableFrom(clazz);
        }
    }

    @Override
    public @Nullable MidPointPrincipal getMidPointPrincipal() {
        return securityEnforcer.getMidPointPrincipal();
    }

    // Spring security invokes this method
    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {

        // Too lound, just for testing
//        LOGGER.trace("decide input: authentication={}, object={}, configAttributes={}",
//                authentication, object, configAttributes);

        if (!(object instanceof FilterInvocation)) {
            LOGGER.trace("DECIDE: PASS because object is not FilterInvocation, it is {}", object);
            return;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        if (isPermitAll(filterInvocation)) {
            LOGGER.trace("DECIDE: authentication={}, object={}: ALLOW ALL (permitAll)",
                    authentication, object);
            return;
        }

        String servletPath = filterInvocation.getRequest().getServletPath();
        if ("".equals(servletPath) || "/".equals(servletPath)) {
            // Special case, this is in fact "magic" redirect to home page or login page. It handles autz in its own way.
            LOGGER.trace("DECIDE: authentication={}, object={}: ALLOW ALL (/)",
                    authentication, object);
            return;
        }

        List<String> requiredActions = new ArrayList<>();

        for (EndPointsUrlMapping urlMapping : EndPointsUrlMapping.values()) {
            addSecurityConfig(filterInvocation, requiredActions, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, AuthorizationActionValue[]> actions = DescriptorLoaderImpl.getActions();
        for (Map.Entry<String, AuthorizationActionValue[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, requiredActions, entry.getKey(), entry.getValue());
        }

        if (requiredActions.isEmpty()) {
            LOGGER.trace("DECIDE: DENY because determined empty required actions from {}", filterInvocation);
            SecurityUtil.logSecurityDeny(object, ": Not authorized (page without authorizations)", null, requiredActions);
            // Sparse exception method by purpose. We do not want to expose details to attacker.
            // Better message is logged.
            throw new AccessDeniedException("Not authorized");
        }

        MidPointPrincipal principal = getPrincipalFromAuthentication(authentication, object, configAttributes);

        Task task = taskManager.createTaskInstance(MidPointGuiAuthorizationEvaluator.class.getName() + ".decide");

        decideInternal(principal, requiredActions, authentication, object, task);
    }

    protected MidPointPrincipal getPrincipalFromAuthentication(Authentication authentication, Object object, Object configAttributes) {
        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof MidPointPrincipal)) {
            if (authentication.getPrincipal() instanceof String && AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principalObject)) {
                SecurityUtil.logSecurityDeny(object, ": Not logged in");
                LOGGER.trace("DECIDE: authentication={}, object={}, configAttributes={}: DENY (not logged in)",
                        authentication, object, configAttributes);
                throw new InsufficientAuthenticationException("Not logged in.");
            }
            LOGGER.trace("DECIDE: authentication={}, object={}, configAttributes={}: ERROR (wrong principal)",
                    authentication, object, configAttributes);
            throw new IllegalArgumentException("Expected that spring security principal will be of type " +
                    MidPointPrincipal.class.getName() + " but it was " + (principalObject == null ? null : principalObject.getClass()));
        }
        return (MidPointPrincipal) principalObject;
    }

    protected void decideInternal(MidPointPrincipal principal, List<String> requiredActions, Authentication authentication, Object object, Task task) {

        AccessDecision decision;
        try {
            decision = securityEnforcer.decideAccess(principal, requiredActions, task, task.getResult());
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException
                | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Error while processing authorization: {}", e.getMessage(), e);
            LOGGER.trace("DECIDE: authentication={}, object={}, requiredActions={}: ERROR {}",
                    authentication, object, requiredActions, e.getMessage());
            throw new SystemException("Error while processing authorization: " + e.getMessage(), e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("DECIDE: authentication={}, object={}, requiredActions={}: {}",
                    authentication, object, requiredActions, decision);
        }

        if (!decision.equals(AccessDecision.ALLOW)) {
            SecurityUtil.logSecurityDeny(object, ": Not authorized", null, requiredActions);
            // Sparse exception method by purpose. We do not want to expose details to attacker.
            // Better message is logged.
            throw new AccessDeniedException("Not authorized");
        }
    }

    private boolean isPermitAll(FilterInvocation filterInvocation) {
        if (filterInvocation.getResponse() != null && filterInvocation.getResponse().isCommitted()
                && new AntPathRequestMatcher(AUTH_URL).matches(filterInvocation.getRequest())) {
            return true;
        }
        for (String url : DescriptorLoaderImpl.getLoginPages()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(filterInvocation.getRequest())) {
                if (AuthSequenceUtil.existLoginPageForActualAuthModule()) {
                    return AuthSequenceUtil.isLoginPageForActualAuthModule(url);
                }
            }
        }

        for (String url : DescriptorLoaderImpl.getPermitAllUrls()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(filterInvocation.getRequest())) {
                return true;
            }
        }
        return false;
    }

    private void addSecurityConfig(FilterInvocation filterInvocation, List<String> requiredActions,
            String url, DisplayableValue<String>[] actions) {

        AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
        if (!matcher.matches(filterInvocation.getRequest()) || actions == null) {
            return;
        }

        for (DisplayableValue<String> action : actions) {
            String actionUri = action.getValue();
            if (StringUtils.isBlank(actionUri)) {
                continue;
            }

            if (!requiredActions.contains(actionUri)) {
                requiredActions.add(actionUri);
            }
        }
    }

    @Override
    public @NotNull <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull List<String> operationUrls,
            @NotNull AuthorizationParameters<O, T> params,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.decideAccess(principal, operationUrls, params, task, result);
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, @Nullable OwnerResolver ownerResolver,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.compileSecurityConstraints(object, ownerResolver, task, result);
    }

    @Override
    public PrismEntityOpConstraints.@NotNull ForValueContent compileOperationConstraints(
            @NotNull PrismObjectValue<?> value,
            @Nullable AuthorizationPhaseType phase,
            @Nullable OwnerResolver ownerResolver,
            @NotNull String[] actionUrls,
            @NotNull CompileConstraintsOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.compileOperationConstraints(value, phase, ownerResolver, actionUrls, options, task, result);
    }

    @Override
    public @Nullable <T> ObjectFilter preProcessObjectFilter(
            String[] operationUrls, AuthorizationPhaseType phase, Class<T> searchResultType,
            @Nullable ObjectFilter origFilter, String limitAuthorizationAction, List<OrderConstraintsType> paramOrderConstraints,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.preProcessObjectFilter(
                operationUrls, phase, searchResultType,
                origFilter, limitAuthorizationAction, paramOrderConstraints, task, result);
    }

    @Override
    public <T extends ObjectType> boolean canSearch(String[] operationUrls,
            AuthorizationPhaseType phase, Class<T> objectType, boolean includeSpecial, ObjectFilter filter,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.canSearch(operationUrls, phase, objectType, includeSpecial, filter, task, result);
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeTargetSecurityFilter(
            MidPointPrincipal principal, String[] operationUrls, AuthorizationPhaseType phase, Class<T> searchResultType,
            @NotNull PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints, FilterGizmo<F> gizmo, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.computeTargetSecurityFilter(
                principal, operationUrls, phase, searchResultType, object, origFilter,
                limitAuthorizationAction, paramOrderConstraints, gizmo, task, result);
    }

    @Override
    public <F extends FocusType> MidPointPrincipal createDonorPrincipal(
            MidPointPrincipal attorneyPrincipal, String attorneyAuthorizationAction, PrismObject<F> donor,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.createDonorPrincipal(attorneyPrincipal, attorneyAuthorizationAction, donor, task, result);
    }

    @Override
    public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return securityContextManager.runAs(producer, user);
    }

    @Override
    public <T> T runPrivileged(Producer<T> producer) {
        return securityContextManager.runPrivileged(producer);
    }

    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal, String actionUri, PrismObject<O> object, PrismObject<R> target,
            OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.getAllowedRequestAssignmentItems(midPointPrincipal, actionUri, object, target, ownerResolver, task, result);
    }

    @Override
    public void storeConnectionInformation(HttpConnectionInformation value) {
        securityContextManager.storeConnectionInformation(value);
    }

    @Override
    public HttpConnectionInformation getStoredConnectionInformation() {
        return securityContextManager.getStoredConnectionInformation();
    }

    @Override
    public <O extends ObjectType> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            String operationUrl,
            AuthorizationPhaseType phase,
            ItemPath itemPath) {
        return securityEnforcer.determineItemDecision(securityConstraints, delta, currentObject, operationUrl, phase, itemPath);
    }

    @Override
    public <C extends Containerable> AccessDecision determineItemDecision(
            ObjectSecurityConstraints securityConstraints, PrismContainerValue<C> containerValue,
            String operationUrl, AuthorizationPhaseType phase, @Nullable ItemPath itemPath,
            PlusMinusZero plusMinusZero, String decisionContextDesc) {
        return securityEnforcer.determineItemDecision(
                securityConstraints, containerValue, operationUrl, phase, itemPath, plusMinusZero, decisionContextDesc);
    }

}
