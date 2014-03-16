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
package com.evolveum.midpoint.common.security;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class SecurityEnforcer {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcer.class);
	
	private UserProfileService userProfileService = null;
	
	public UserProfileService getUserProfileService() {
		return userProfileService;
	}

	public void setUserProfileService(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) {
		MidPointPrincipal principal = null;
		if (userProfileService == null) {
			LOGGER.warn("No user profile service set up in SecurityEnforcer. "
					+ "This is OK in low-level tests but it is a serious problem in running system");
			principal = new MidPointPrincipal(user.asObjectable());
		} else {
			principal = userProfileService.getPrincipal(user);
		}
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
		securityContext.setAuthentication(authentication);
	}

	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, 
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, 
			OperationResult result) throws SecurityViolationException, SchemaException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new SecurityViolationException("No authentication");
		}
		Object principal = authentication.getPrincipal();
		boolean allow = false;
		if (principal != null) {
			if (principal instanceof MidPointPrincipal) {
				MidPointPrincipal midPointPrincipal = (MidPointPrincipal)principal;
				LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, object={}, delta={}, target={}",
						new Object[]{midPointPrincipal, operationUrl, object, delta, target});
				Collection<Authorization> authorities = midPointPrincipal.getAuthorities();
				if (authorities != null) {
					for (GrantedAuthority authority: authorities) {
						if (authority instanceof Authorization) {
							Authorization autz = (Authorization)authority;
							LOGGER.trace("Evaluating authorization {}", autz);
							// First check if the authorization is applicable.
							
							// action
							if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
								LOGGER.trace("  Authorization not applicable for operation {}", operationUrl);
								continue;
							}
							
							// object
							List<ObjectSpecificationType> autzObjects = autz.getObject();
							if (autzObjects != null && !autzObjects.isEmpty()) {
								if (object == null) {
									LOGGER.trace("  Authorization not applicable for null object");
									continue;
								}
								boolean applicable = false;
								for (ObjectSpecificationType autzObject: autzObjects) {
									if (isApplicable(autzObject, object, midPointPrincipal)) {
										applicable = true;
										break;
									}
								}
								if (applicable) {
									LOGGER.trace("  Authorization applicable for object {}", object);
								} else {
									LOGGER.trace("  Authorization not applicable for object {}, none of the object specifications match", 
											object);
								}
							} else {
								LOGGER.trace("  No object specification in authorization (authorization is applicable)");
							}
							
							// authority is applicable to this situation. now we can process the decision.
							AuthorizationDecisionType decision = autz.getDecision();
							if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
								LOGGER.trace("  ALLOW operation {} (but continue evaluation)", autz, operationUrl);
								allow = true;
								// Do NOT break here. Other authorization statements may still deny the operation
							} else {
								LOGGER.trace("  DENY operation {}", autz, operationUrl);
								allow = false;
								// Break right here. Deny cannot be overridden by allow. This decision cannot be changed. 
								break;
							}
							
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
			LOGGER.trace("AUTZ result: principal={}, operation={}: {}", new Object[]{username, operationUrl, allow});
		}
		if (!allow) {
			String username = getUsername(authentication);
			LOGGER.error("User {} not authorized for operation {}", username, operationUrl);
			SecurityViolationException e = new SecurityViolationException("User "+username+" not authorized for operation "
			+operationUrl);
//			+":\n"+((MidPointPrincipal)principal).debugDump());
			result.recordFatalError(e.getMessage(), e);
			throw e;
		}
	}
	
	private <O extends ObjectType> boolean isApplicable(ObjectSpecificationType objectSpecType, PrismObject<O> object, 
			MidPointPrincipal principal) throws SchemaException {
		if (objectSpecType == null) {
			return false;
		}
		QueryType specFilter = objectSpecType.getFilter();
		QName specTypeQName = objectSpecType.getType();
		List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (specFilter != null) {
				throw new SchemaException("Both filter and special object specification specified in authorization");
			}
			for (SpecialObjectSpecificationType special: specSpecial) {
				if (special == SpecialObjectSpecificationType.SELF) {
					String principalOid = principal.getOid();
					if (principalOid != null) {
						// This is a rare case. It should not normally happen. But it may happen in tests
						// or during initial import. Therefore we are not going to die here. Just ignore it.
					} else {
						if (principalOid.equals(object.getOid())) {
							return true;
						}
					}
				} else {
					throw new SchemaException("Unsupported special object specification specified in authorization: "+special);
				}
			}
		}
		// TODO: filter
		return false;
	}
	
	private String getUsername(Authentication authentication) {
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
