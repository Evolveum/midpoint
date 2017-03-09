/**
 * Copyright (c) 2014-2016 Evolveum
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
package com.evolveum.midpoint.security.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.util.MiscUtil;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author Radovan Semancik
 */
public class SecurityUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityUtil.class);

	/**
	 * Returns principal representing currently logged-in user. Returns null if the user is anonymous.
	 */
	public static MidPointPrincipal getPrincipal() throws SecurityViolationException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			SecurityViolationException ex = new SecurityViolationException("No authentication");
			LOGGER.error("No authentication", ex);
			throw ex;
		}
		Object principalObject = authentication.getPrincipal();
		if (!(principalObject instanceof MidPointPrincipal)) {
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principalObject)) {
				return null;
			} else {
				throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+ MiscUtil.getObjectName(principalObject));
			}
		}
		return (MidPointPrincipal) principalObject;
	}
	
	public static boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null);
	}
	
	public static Collection<String> getActions(Collection<ConfigAttribute> configAttributes) {
		Collection<String> actions = new ArrayList<String>(configAttributes.size());
		for (ConfigAttribute attr: configAttributes) {
			actions.add(attr.getAttribute());
		}
		return actions;
	}
	
	public static void logSecurityDeny(Object object, String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Denied access to {} by {} {}", object, getSubjectDescription(), message);
		}
	}
	
	public static void logSecurityDeny(Object object, String message, Throwable cause, Collection<String> requiredAuthorizations) {
		if (LOGGER.isDebugEnabled()) {
			String subjectDesc = getSubjectDescription();
			LOGGER.debug("Denied access to {} by {} {}", object, subjectDesc, message);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Denied access to {} by {} {}; one of the following authorization actions is required: "+requiredAuthorizations, 
						new Object[]{object, subjectDesc, message, cause});
			}
		}
	}

	/**
	 * Returns short description of the subject suitable for log
	 * and error messages.
	 * Does not throw errors. Safe to toString-like methods.
	 * May return null (means anonymous or unknown)
	 */
	public static String getSubjectDescription() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principalObject = authentication.getPrincipal();
		if (principalObject == null) {
			return null;
		}
		if (!(principalObject instanceof MidPointPrincipal)) {
			return principalObject.toString();
		}
		return ((MidPointPrincipal)principalObject).getUsername();
	}
	
	public static <T> T getCredPolicyItem(CredentialPolicyType defaltCredPolicyType, CredentialPolicyType credPolicyType, Function<CredentialPolicyType, T> getter) {
		if (credPolicyType != null) {
			T val = getter.apply(credPolicyType);
			if (val != null) {
				return val;
			}
		}
		if (defaltCredPolicyType != null) {
			T val = getter.apply(defaltCredPolicyType);
			if (val != null) {
				return val;
			}
		}
		return null;
	}
	
}
