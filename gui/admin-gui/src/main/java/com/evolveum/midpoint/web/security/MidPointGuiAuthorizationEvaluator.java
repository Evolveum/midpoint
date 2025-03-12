/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.util.*;

import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;

import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.http.HttpServletRequest;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer, SecurityContextManager, AccessDecisionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointGuiAuthorizationEvaluator.class);

    private final String authUrl = "/" + ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE + "/*";

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
    public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl,
            AuthorizationPhaseType phase, AuthorizationParameters<O, T> params,
            OperationResult result) throws SecurityViolationException {
        securityEnforcer.failAuthorization(operationUrl, phase, params, result);
    }

    // MidPoint pages invoke this method (through PageBase)
    @Override
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
            AuthorizationParameters<O, T> params, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
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
    public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
            AuthorizationParameters<O, T> params, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(operationUrl, phase, params, ownerResolver, task, result);
    }

    @Override
    public MidPointPrincipal getMidPointPrincipal() {
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

        Set<String> requiredActions = new HashSet<>();

        for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
            addSecurityConfig(filterInvocation, requiredActions, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, DisplayableValue<String>[]> actions = DescriptorLoader.getActions();
        for (Map.Entry<String, DisplayableValue<String>[]> entry : actions.entrySet()) {
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

        boolean hasPathAttribute = ServletRequestPathUtils.hasCachedPath(request);

        try {
            if (!hasPathAttribute) {
                try {
                    ServletRequestPathUtils.parseAndCache(request);
                } catch (Exception e) {
                    // ignore exception
                }
            }
            for (HandlerMapping handlerMapping : handlerMappingBeans) {
                try {
                    HandlerExecutionChain handler = handlerMapping.getHandler(request);
                    if (handler != null && handler.getHandler() instanceof HandlerMethod) {
                        return (HandlerMethod) handler.getHandler();
                    }
                } catch (Exception e) {
                    // ignore exception
                }
            }
        } finally {
            if(!hasPathAttribute) {
                try {
                    ServletRequestPathUtils.clearParsedRequestPath(request);
                } catch (Exception e) {
                    // ignore exception
                }
            }
        }
        return null;
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
                && new AntPathRequestMatcher(authUrl).matches(filterInvocation.getRequest())) {
            return true;
        }
        for (String url : DescriptorLoader.getLoginPages()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(filterInvocation.getRequest())) {
                if (SecurityUtils.existLoginPageForActualAuthModule()) {
                    return SecurityUtils.isLoginPageForActualAuthModule(url);
                }
            }
        }

        for (String url : DescriptorLoader.getPermitAllUrls()) {
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
    public AccessDecision decideAccess(MidPointPrincipal principal, Collection<String> requiredActions, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.decideAccess(principal, requiredActions, task, result);
    }

    @Override
    public <O extends ObjectType, T extends ObjectType> AccessDecision decideAccess(MidPointPrincipal principal, Collection<String> requiredActions, AuthorizationParameters<O, T> params, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.decideAccess(principal, requiredActions, params, task, result);
    }

    @Override
    public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, boolean fullInformationAvailable, OwnerResolver ownerResolver, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.compileSecurityConstraints(object, fullInformationAvailable, ownerResolver, task, result);
    }

    @Override
    public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String[] operationUrls, AuthorizationPhaseType phase,
            Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction, List<OrderConstraintsType> paramOrderConstraints, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.preProcessObjectFilter(operationUrls, phase, objectType, object, origFilter, limitAuthorizationAction, paramOrderConstraints, task, result);
    }

    @Override
    public <T extends ObjectType, O extends ObjectType> boolean canSearch(String[] operationUrls,
            AuthorizationPhaseType phase, Class<T> objectType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.canSearch(operationUrls, phase, objectType, object, includeSpecial, filter, task, result);
    }

    @Override
    public <T extends ObjectType, O extends ObjectType, F> F computeSecurityFilter(MidPointPrincipal principal, String[] operationUrls, AuthorizationPhaseType phase, Class<T> searchResultType, PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction, List<OrderConstraintsType> paramOrderConstraints, FilterGizmo<F> gizmo, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return securityEnforcer.computeSecurityFilter(principal, operationUrls, phase, searchResultType, object, origFilter, limitAuthorizationAction, paramOrderConstraints, gizmo, task, result);
    }

    @Override
    public <F extends FocusType> MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal,
            String attorneyAuthorizationAction, PrismObject<F> donor, Task task,
            OperationResult result)
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
    public <O extends ObjectType> AccessDecision determineSubitemDecision(
            ObjectSecurityConstraints securityConstraints, ObjectDelta<O> delta, PrismObject<O> currentObject, String operationUrl,
            AuthorizationPhaseType phase, ItemPath subitemRootPath) {
        return securityEnforcer.determineSubitemDecision(securityConstraints, delta, currentObject, operationUrl, phase, subitemRootPath);
    }

    @Override
    public <C extends Containerable> AccessDecision determineSubitemDecision(
            ObjectSecurityConstraints securityConstraints, PrismContainerValue<C> containerValue,
            String operationUrl, AuthorizationPhaseType phase, ItemPath subitemRootPath,
            PlusMinusZero plusMinusZero, String decisionContextDesc) {
        return securityEnforcer.determineSubitemDecision(securityConstraints, containerValue, operationUrl, phase, subitemRootPath, plusMinusZero, decisionContextDesc);
    }

}
