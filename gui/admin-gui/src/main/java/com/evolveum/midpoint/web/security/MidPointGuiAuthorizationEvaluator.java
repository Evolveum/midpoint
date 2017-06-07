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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer {
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointGuiAuthorizationEvaluator.class);

	private SecurityEnforcer securityEnforcer;
	
    public MidPointGuiAuthorizationEvaluator(SecurityEnforcer securityEnforcer) {
		super();
		this.securityEnforcer = securityEnforcer;
	}

    @Override
	public UserProfileService getUserProfileService() {
		return securityEnforcer.getUserProfileService();
	}

	@Override
	public void setUserProfileService(UserProfileService userProfileService) {
		securityEnforcer.setUserProfileService(userProfileService);
	}

    @Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        securityEnforcer.setupPreAuthenticatedSecurityContext(authentication);
    }

    @Override
    public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException {
		securityEnforcer.setupPreAuthenticatedSecurityContext(user);
	}
    
    @Override
	public boolean isAuthenticated() {
		return securityEnforcer.isAuthenticated();
	}

    @Override
	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return securityEnforcer.getPrincipal();
	}
    
    @Override
	public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl,
			AuthorizationPhaseType phase, PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target,
			OperationResult result) throws SecurityViolationException {
    	securityEnforcer.failAuthorization(operationUrl, phase, object, delta, target, result);
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver) throws SchemaException {
		return securityEnforcer.isAuthorized(operationUrl, phase, object, delta, target, ownerResolver);
	}

    @Override
	public boolean supports(ConfigAttribute attribute) {
		return securityEnforcer.supports(attribute);
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, OperationResult result)
			throws SecurityViolationException, SchemaException {
		securityEnforcer.authorize(operationUrl, phase, object, delta, target, ownerResolver, result);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return securityEnforcer.supports(clazz);
	}

	@Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {

        if (!(object instanceof FilterInvocation)) {
            return;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        Collection<ConfigAttribute> guiConfigAttr = new ArrayList<>();

        for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, DisplayableValue<String>[]> actions = DescriptorLoader.getActions();
        for (Map.Entry<String, DisplayableValue<String>[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, entry.getKey(), entry.getValue());
        }

        if (configAttributes == null || guiConfigAttr.isEmpty()) {
            return;
        }
        
        Collection<ConfigAttribute> configAttributesToUse = guiConfigAttr;
        if (guiConfigAttr.isEmpty()) {
        	configAttributesToUse = configAttributes;
        }
        
        try {
        	securityEnforcer.decide(authentication, object, configAttributesToUse);
        	
        	if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("DECIDE: authentication={}, object={}, configAttributesToUse={}: OK", authentication, object, configAttributesToUse);
            }
        } catch (AccessDeniedException | InsufficientAuthenticationException e) {
        	if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("DECIDE: authentication={}, object={}, configAttributesToUse={}: {}", authentication, object, configAttributesToUse, e);
            }
        	throw e;
        }
    }

    private void addSecurityConfig(FilterInvocation filterInvocation, Collection<ConfigAttribute> guiConfigAttr,
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

            //all users has permission to access these resources
            if (action.equals(AuthorizationConstants.AUTZ_UI_PERMIT_ALL_URL)) {
                return;
            }

            SecurityConfig config = new SecurityConfig(actionUri);
			if (!guiConfigAttr.contains(config)) {
				guiConfigAttr.add(config);
			}
        }
    }

    @Override
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver)
			throws SchemaException {
		return securityEnforcer.compileSecurityConstraints(object, ownerResolver);
	}

    @Override
	public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter) throws SchemaException {
		return securityEnforcer.preProcessObjectFilter(operationUrl, phase, objectType, object, origFilter);
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> boolean canSearch(String operationUrl,
			AuthorizationPhaseType phase, Class<T> objectType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter)
			throws SchemaException {
		return securityEnforcer.canSearch(operationUrl, phase, objectType, object, includeSpecial, filter);
	}

	@Override
	public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException {
		return securityEnforcer.runAs(producer, user);
	}

	@Override
	public <T> T runPrivileged(Producer<T> producer) {
		return securityEnforcer.runPrivileged(producer);
	}

	@Override
	public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityDecisions getAllowedRequestAssignmentItems(
			MidPointPrincipal midPointPrincipal, String actionUri, PrismObject<O> object, PrismObject<R> target,
			OwnerResolver ownerResolver) throws SchemaException {
		return securityEnforcer.getAllowedRequestAssignmentItems(midPointPrincipal, actionUri, object, target, ownerResolver);
	}

	@Override
	public void storeConnectionInformation(HttpConnectionInformation value) {
		securityEnforcer.storeConnectionInformation(value);
	}

	@Override
	public HttpConnectionInformation getStoredConnectionInformation() {
		return securityEnforcer.getStoredConnectionInformation();
	}
}
