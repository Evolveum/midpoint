/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.security.enforcer.api.AccessDecision;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer, SecurityContextManager, AccessDecisionManager {
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointGuiAuthorizationEvaluator.class);

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
	public UserProfileService getUserProfileService() {
		return securityContextManager.getUserProfileService();
	}

	@Override
	public void setUserProfileService(UserProfileService userProfileService) {
		securityContextManager.setUserProfileService(userProfileService);
	}

    @Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
    	securityContextManager.setupPreAuthenticatedSecurityContext(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException {
    	securityContextManager.setupPreAuthenticatedSecurityContext(user);
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
	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return securityContextManager.getPrincipal();
	}
    
	@Override
	public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl,
			AuthorizationPhaseType phase, AuthorizationParameters<O,T> params,
			OperationResult result) throws SecurityViolationException {
    	securityEnforcer.failAuthorization(operationUrl, phase, params, result);
	}

	// MidPoint pages invoke this method (through PageBase)
	@Override
	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
	}
	
	@Override
	public boolean supports(ConfigAttribute attribute) {
		if (attribute instanceof SecurityConfig
				// class name equals, because WebExpressionConfigAttribute is non public class
				|| "org.springframework.security.web.access.expression.WebExpressionConfigAttribute".equals(attribute.getClass().getName())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean supports(Class<?> clazz) {
		if (MethodInvocation.class.isAssignableFrom(clazz)) {
			return true;
		} else if (FilterInvocation.class.isAssignableFrom(clazz)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, Task task, OperationResult result)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		securityEnforcer.authorize(operationUrl, phase, params, ownerResolver, task, result);
	}

	// Spring security invokes this method
	@Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {
		
		// Too lound, just for testing
		LOGGER.trace("decide input: authentication={}, object={}, configAttributes={}",
				authentication, object, configAttributes);
				
        if (!(object instanceof FilterInvocation)) {
        	// TODO: is this OK?
        	LOGGER.trace("DECIDE: PASS because object is not FilterInvocation, it is {}", object);
            return;
        }

        if (configAttributes == null) {
        	// TODO: is this OK?
        	LOGGER.trace("DECIDE: PASS because config attributes are null");
            return;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        List<String> requiredActions = new ArrayList<>(); 

        for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
            addSecurityConfig(filterInvocation, requiredActions, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, DisplayableValue<String>[]> actions = DescriptorLoader.getActions();
        for (Map.Entry<String, DisplayableValue<String>[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, requiredActions, entry.getKey(), entry.getValue());
        }
        
        LOGGER.info("REQ: {}", requiredActions);
        
        if (requiredActions.isEmpty()) {
        	// TODO: is this OK?
        	// E.g. login pages go through this. Maybe add permitAll annotation to that page?
        	LOGGER.trace("DECIDE: PASS because determined empty required actions from {}", filterInvocation);
            return;
        }
        
        //all users has permission to access these resources
        if (requiredActions.contains(AuthorizationConstants.AUTZ_UI_PERMIT_ALL_URL)) {
        	LOGGER.trace("DECIDE: authentication={}, object={}, requiredActions={}: ALLOW ALL",
            			authentication, object, requiredActions);
            return;
        }
        
		Object principalObject = authentication.getPrincipal();
		if (!(principalObject instanceof MidPointPrincipal)) {
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principalObject)){
				SecurityUtil.logSecurityDeny(object, ": Not logged in");
				LOGGER.trace("DECIDE: authentication={}, object={}, configAttributes={}: DENY (not logged in)",
            			authentication, object, configAttributes);
				throw new InsufficientAuthenticationException("Not logged in.");
			}
			LOGGER.trace("DECIDE: authentication={}, object={}, configAttributes={}: ERROR (wrong principal)",
        			authentication, object, configAttributes);
			throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+principalObject.getClass());
		}
		MidPointPrincipal principal = (MidPointPrincipal)principalObject;

                
        Task task = taskManager.createTaskInstance(MidPointGuiAuthorizationEvaluator.class.getName() + ".decide");
        
        
		AccessDecision decision;
		try {
			decision = securityEnforcer.decideAccess(principal, requiredActions, task, task.getResult());
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException
				| CommunicationException | ConfigurationException | SecurityViolationException e) {
			LOGGER.error("Error while processing authorization: {}", e.getMessage(), e);
			LOGGER.trace("DECIDE: authentication={}, object={}, requiredActions={}: ERROR {}",
        			authentication, object, requiredActions, e.getMessage());
			throw new SystemException("Error while processing authorization: "+e.getMessage(), e);
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
	public AccessDecision decideAccess(MidPointPrincipal principal, List<String> requiredActions, Task task,
			OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.decideAccess(principal, requiredActions, task, result);
	}

    @Override
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.compileSecurityConstraints(object, ownerResolver, task, result);
	}

    @Override
	public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.preProcessObjectFilter(operationUrl, phase, objectType, object, origFilter, limitAuthorizationAction, task, result);
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> boolean canSearch(String operationUrl,
			AuthorizationPhaseType phase, Class<T> objectType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.canSearch(operationUrl, phase, objectType, object, includeSpecial, filter, task, result);
	}
	
	@Override
	public MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal,
			String attorneyAuthorizationAction, PrismObject<UserType> donor, Task task,
			OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		return securityEnforcer.createDonorPrincipal(attorneyPrincipal, attorneyAuthorizationAction, donor, task, result);
	}


	@Override
	public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException {
		return securityContextManager.runAs(producer, user);
	}

	@Override
	public <T> T runPrivileged(Producer<T> producer) {
		return securityContextManager.runPrivileged(producer);
	}

	@Override
	public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityDecisions getAllowedRequestAssignmentItems(
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

}
