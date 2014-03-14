/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.model.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public class AuthorizationEnforcer {
	
	private static final Trace LOGGER = TraceManager.getTrace(AuthorizationEnforcer.class);

	public static <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, 
			PrismObject<O> object, PrismObject<T> target, Task task, OperationResult parentResult) throws SecurityViolationException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new SecurityViolationException("No authentication");
		}
		Object principal = authentication.getPrincipal();
		boolean allow = false;
		if (principal != null) {
			if (principal instanceof MidPointPrincipal) {
				MidPointPrincipal midPointPrincipal = (MidPointPrincipal)principal;
				Collection<Authorization> authorities = midPointPrincipal.getAuthorities();
				if (authorities != null) {
					for (GrantedAuthority authority: authorities) {
						if (authority instanceof Authorization) {
							Authorization autz = (Authorization)authority;
							if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
								LOGGER.trace("Evaluating authorization {}: not applicable for operation {}", autz, operationUrl);
								continue;
							}
							LOGGER.trace("Evaluating authorization {}: ALLOW operation {}", autz, operationUrl);
							allow = true;
							break;
						} else {
							LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), midPointPrincipal.getUsername());
						}
					}
				}
			} else {
				LOGGER.warn("Unknown principal type {}", principal.getClass());
			}
		} else {
			LOGGER.warn("Null principal");
		}
		
		if (LOGGER.isTraceEnabled()) {
			String username = getUsername(authentication);
			LOGGER.trace("AUTZ operation {}, principal {}: {}", new Object[]{operationUrl, username, allow});
		}
		if (!allow) {
			String username = getUsername(authentication);
			LOGGER.error("User {} not authorized for operation {}", username, operationUrl);
			throw new SecurityViolationException("User "+username+" not authorized for operation "+operationUrl+":\n"+((MidPointPrincipal)principal).debugDump());
		}
	}
	
	private static String getUsername(Authentication authentication) {
		String username = "(none)";
		Object principal = authentication.getPrincipal();
		if (principal != null) {
			if (principal instanceof MidPointPrincipal) {
				username = "'"+((MidPointPrincipal)principal).getUsername()+"'";
			} else {
				username = "(unknown:"+principal+")";
			}
		}
		return username;
	}
	
}
