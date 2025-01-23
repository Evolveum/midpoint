/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.authorization.evaluator;

import java.util.*;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationActionValue;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.authorization.EndPointsUrlMapping;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.security.api.*;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
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

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer, SecurityContextManager, AccessDecisionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointGuiAuthorizationEvaluator.class);

    private static final String AUTH_URL = "/" + ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE + "/*";

    private final SecurityEnforcer securityEnforcer;
    private final SecurityContextManager securityContextManager;
    private final TaskManager taskManager;
    private final ApplicationContext applicationContext;

    /** Used to retrieve REST handler methods' information. Lazily evaluated. */
    private List<HandlerMapping> handlerMappingBeans = List.of();

    public MidPointGuiAuthorizationEvaluator(
            SecurityEnforcer securityEnforcer,
            SecurityContextManager securityContextManager,
            TaskManager taskManager,
            ApplicationContext applicationContext) {
        this.securityEnforcer = securityEnforcer;
        this.securityContextManager = securityContextManager;
        this.taskManager = taskManager;
        this.applicationContext = applicationContext;
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
    public void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        securityContextManager.setupPreAuthenticatedSecurityContext(focus, result);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(
            PrismObject<? extends FocusType> focus, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        securityContextManager.setupPreAuthenticatedSecurityContext(focus, options, result);
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

    @Override
    public @NotNull AccessDecision decideAccess(
            @Nullable MidPointPrincipal principal,
            @NotNull String operationUrl,
            @Nullable AuthorizationPhaseType phase,
            @NotNull AbstractAuthorizationParameters params,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.decideAccess(
                principal, operationUrl, phase, params, options, task, result);
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

        if (!(object instanceof FilterInvocation filterInvocation)) {
            LOGGER.trace("DECIDE: PASS because object is not FilterInvocation, it is {}", object);
            return;
        }

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

        Set<String> requiredActions = new HashSet<>();

        for (EndPointsUrlMapping urlMapping : EndPointsUrlMapping.values()) {
            addSecurityConfig(filterInvocation, requiredActions, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, AuthorizationActionValue[]> actions = DescriptorLoaderImpl.getActions();
        for (Map.Entry<String, AuthorizationActionValue[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, requiredActions, entry.getKey(), entry.getValue());
        }

        HandlerMethod restHandlerMethod = getRestHandlerMethod(filterInvocation.getRequest());
        if (restHandlerMethod != null) {
            addSecurityConfig(requiredActions, restHandlerMethod);
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

    private HandlerMethod getRestHandlerMethod(HttpServletRequest request) {
        if (handlerMappingBeans.isEmpty()) {
            handlerMappingBeans = new ArrayList<>(
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(
                            applicationContext, HandlerMapping.class, true, false).values());
        }

        for (HandlerMapping handlerMapping : handlerMappingBeans) {
            try {
                HandlerExecutionChain handler = handlerMapping.getHandler(request);
                if (handler != null && handler.getHandler() instanceof HandlerMethod method) {
                    return method;
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
        return null;
    }

    protected MidPointPrincipal getPrincipalFromAuthentication(
            Authentication authentication, Object object, Object configAttributes) {
        Object principalObject = authentication.getPrincipal();
        if (principalObject instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principalObject;
        }
        if (authentication.getPrincipal() instanceof String
                && AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principalObject)) {
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

    protected void decideInternal(
            MidPointPrincipal principal, Set<String> requiredActions, Authentication authentication, Object object, Task task) {

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

    private void addSecurityConfig(
            FilterInvocation filterInvocation, Set<String> requiredActions, String url, DisplayableValue<String>[] actions) {

        AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
        if (!matcher.matches(filterInvocation.getRequest()) || actions == null) {
            return;
        }

        for (DisplayableValue<String> action : actions) {
            String actionUri = action.getValue();
            if (!StringUtils.isBlank(actionUri)) {
                requiredActions.add(actionUri);
            }
        }
    }

    /** Adds a required action specific to given REST handler method. */
    private void addSecurityConfig(Set<String> requiredActions, HandlerMethod restHandlerMethod) {
        var annotation = restHandlerMethod.getMethodAnnotation(RestHandlerMethod.class);
        if (annotation != null) {
            requiredActions.add(annotation.authorization().getUri());
        } else {
            // No additional info. We do the authorization in a traditional way (`rest-3#all` or cluster authentication).
        }
    }

    @Override
    public @NotNull <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(
            @NotNull PrismObject<O> object, boolean fullInformationAvailable, @NotNull Options options,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.compileSecurityConstraints(object, fullInformationAvailable, options, task, result);
    }

    @Override
    public @NotNull PrismEntityOpConstraints.ForValueContent compileOperationConstraints(
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
        return securityEnforcer.compileOperationConstraints(
                principal, value, phase, actionUrls, enforcerOptions, compileConstraintsOptions, task, result);
    }

    @Override
    public @Nullable <T> ObjectFilter preProcessObjectFilter(
            @Nullable MidPointPrincipal principal,
            @NotNull String[] operationUrls,
            @NotNull String[] searchByOperationUrls,
            @Nullable AuthorizationPhaseType phase,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter origFilter,
            @Nullable String limitAuthorizationAction,
            @NotNull List<OrderConstraintsType> paramOrderConstraints,
            @NotNull Options options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.preProcessObjectFilter(
                principal, operationUrls, searchByOperationUrls, phase,
                filterType, origFilter, limitAuthorizationAction, paramOrderConstraints, options, task, result);
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeTargetSecurityFilter(
            MidPointPrincipal principal, String[] operationUrls, @NotNull String[] searchByOperationUrls,
            AuthorizationPhaseType phase, Class<T> searchResultType,
            @NotNull PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction,
            List<OrderConstraintsType> paramOrderConstraints, FilterGizmo<F> gizmo, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.computeTargetSecurityFilter(
                principal, operationUrls, searchByOperationUrls, phase, searchResultType, object,
                origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, task, result);
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
    public <T> T runAs(
            @NotNull ResultAwareProducer<T> producer,
            @Nullable PrismObject<? extends FocusType> newPrincipalObject,
            boolean privileged,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        return securityContextManager.runAs(producer, newPrincipalObject, privileged, result);
    }

    @Override
    public <T> T runPrivileged(@NotNull Producer<T> producer) {
        return securityContextManager.runPrivileged(producer);
    }

    @Override
    public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(
            MidPointPrincipal midPointPrincipal, String actionUri, PrismObject<O> object, PrismObject<R> target,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return securityEnforcer.getAllowedRequestAssignmentItems(
                midPointPrincipal, actionUri, object, target, task, result);
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
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            @NotNull ItemPath itemPath) {
        return securityEnforcer.determineItemDecision(securityConstraints, delta, currentObject, operationUrl, phase, itemPath);
    }

    @Override
    public <C extends Containerable> AccessDecision determineItemValueDecision(
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull PrismContainerValue<C> containerValue,
            @NotNull String operationUrl,
            @NotNull AuthorizationPhaseType phase,
            boolean consideringCreation,
            @NotNull String decisionContextDesc) {
        return securityEnforcer.determineItemValueDecision(
                securityConstraints, containerValue, operationUrl, phase, consideringCreation, decisionContextDesc);
    }
}
