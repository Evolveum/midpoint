/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AuthorizationEvaluator implements AccessDecisionManager {
	
	private static final String WILDCARD = "*";
	
	private ActivationComputer activationComputer;

	public ActivationComputer getActivationComputer() {
		return activationComputer;
	}

	public void setActivationComputer(ActivationComputer activationComputer) {
		this.activationComputer = activationComputer;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.AccessDecisionManager#decide(org.springframework.security.core.Authentication, java.lang.Object, java.util.Collection)
	 */
	@Override
	public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
			throws AccessDeniedException, InsufficientAuthenticationException {
		if (object instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation)object;
			// TODO
		} else if (object instanceof FilterInvocation) {
			FilterInvocation filterInvocation = (FilterInvocation)object;
			// TODO
		} else {
			throw new IllegalArgumentException("Unknown type of secure object "+object.getClass());
		}

		Object principalObject = authentication.getPrincipal();
		if (!(principalObject instanceof MidPointPrincipal)) {
			throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+principalObject.getClass());
		}
		MidPointPrincipal principal = (MidPointPrincipal)principalObject;

		Collection<String> configActions = getActions(configAttributes);
		
		for(String configAction: configActions) {
			if (isAuthorized(principal, configAction)) {
				return;
			}
		}
		
		throw new AccessDeniedException("Access denied, insufficient authorization (required actions "+configActions+")");
	}

	private Collection<String> getActions(Collection<ConfigAttribute> configAttributes) {
		Collection<String> actions = new ArrayList<String>(configAttributes.size());
		for (ConfigAttribute attr: configAttributes) {
			actions.add(attr.getAttribute());
		}
		return actions;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.AccessDecisionManager#supports(org.springframework.security.access.ConfigAttribute)
	 */
	@Override
	public boolean supports(ConfigAttribute attribute) {
		if (attribute instanceof SecurityConfig) {
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.AccessDecisionManager#supports(java.lang.Class)
	 */
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
	
	public boolean isAuthorized(String action) {
		SecurityContext context = SecurityContextHolder.getContext();
		if (context == null) {
			throw new IllegalStateException("No spring security context");
		}
		Object principal = context.getAuthentication().getPrincipal();
		if (!(principal instanceof MidPointPrincipal)) {
			throw new IllegalStateException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+principal.getClass());
		}
		return isAuthorized((MidPointPrincipal)principal, action);
	}

	private boolean isAuthorized(MidPointPrincipal principal, String action) {
		if (!isActive(principal)) {
			return false;
		}
		Collection<Authorization> authorities = principal.getAuthorities();
		if (authorities == null || authorities.isEmpty()) {
			return false;
		}
		for (Authorization authorization: authorities) {
			// Deny is always stronger than allow
			if (authorization.getDecision() == AuthorizationDecisionType.DENY && appliesTo(authorization, action)) {
				return false;
			}
		}
		for (Authorization authorization: authorities) {
			// Deny is always stronger than allow
			if (authorization.getDecision() == AuthorizationDecisionType.ALLOW && appliesTo(authorization, action)) {
				return true;
			}
		}
		return false;
	}

	private boolean isActive(MidPointPrincipal principal) {
		UserType userType = principal.getUser();
		return activationComputer.isActive(userType.getActivation());
	}

	private boolean appliesTo(Authorization authorization, String action) {
		List<String> autzActions = authorization.getAction();
		for (String autzAction: autzActions) {
			if (autzAction.equals(AuthorizationConstants.AUTZ_ALL_URL)) {
				// This is a placeholder for all the actions
				return true;
			}
			if (autzAction.equals(action)) {
				return true;
			}
			QName autzActionQname = QNameUtil.uriToQName(autzAction);
			if (autzActionQname.getLocalPart() != null && autzActionQname.getLocalPart().equals(WILDCARD)) {
				QName actionQname = QNameUtil.uriToQName(action);
				if (autzActionQname.getNamespaceURI().equals(actionQname.getNamespaceURI())) {
					return true;
				}
			}
		}
		return false;
	}
	
}
