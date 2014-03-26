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
package com.evolveum.midpoint.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.QueryConvertor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
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
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);
	
	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;
	
	private UserProfileService userProfileService = null;
	
	@Override
	public UserProfileService getUserProfileService() {
		return userProfileService;
	}

	@Override
	public void setUserProfileService(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@Override
	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return SecurityUtil.getPrincipal();
	}

	@Override
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

	@Override
	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target)
			throws SchemaException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			LOGGER.warn("No authentication");
			return false;
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
							if (isApplicable(autz.getObject(), object, midPointPrincipal, "object")) {
								LOGGER.trace("  Authorization applicable for object {} (continuing evaluation)", object);
							} else {
								LOGGER.trace("  Authorization not applicable for object {}, none of the object specifications match (breaking evaluation)", 
										object);
								continue;
							}
							
							// target
							if (isApplicable(autz.getTarget(), target, midPointPrincipal, "target")) {
								LOGGER.trace("  Authorization applicable for target {} (continuing evaluation)", object);
							} else {
								LOGGER.trace("  Authorization not applicable for target {}, none of the target specifications match (breaking evaluation)", 
										object);
								continue;
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
				if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principal)){
					LOGGER.trace("AUTZ: deny because user is not logged in");
					return false;
				}
				LOGGER.warn("Unknown principal type {}", principal.getClass());
				return false;
			}
		} else {
			LOGGER.warn("Null principal");
			return false;
		}
		
		if (LOGGER.isTraceEnabled()) {
			String username = getQuotedUsername(authentication);
			LOGGER.trace("AUTZ result: principal={}, operation={}: {}", new Object[]{username, operationUrl, allow});
		}
		return allow;
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, 
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, 
			OperationResult result) throws SecurityViolationException, SchemaException {
		MidPointPrincipal principal = getPrincipal();
		boolean allow = isAuthorized(operationUrl, object, delta, target);
		if (!allow) {
			String username = getQuotedUsername(principal);
			LOGGER.error("User {} not authorized for operation {}", username, operationUrl);
			SecurityViolationException e = new SecurityViolationException("User "+username+" not authorized for operation "
			+operationUrl);
//			+":\n"+((MidPointPrincipal)principal).debugDump());
			result.recordFatalError(e.getMessage(), e);
			throw e;
		}
	}
	
	private <O extends ObjectType> boolean isApplicable(List<ObjectSpecificationType> objectSpecTypes, PrismObject<O> object, 
			MidPointPrincipal midPointPrincipal, String desc) throws SchemaException {
		if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
			if (object == null) {
				LOGGER.trace("  Authorization not applicable for null "+desc);
				return false;
			}
			for (ObjectSpecificationType autzObject: objectSpecTypes) {
				if (isApplicable(autzObject, object, midPointPrincipal, desc)) {
					return true;
				}
			}
			return false;
		} else {
			LOGGER.trace("  No "+desc+" specification in authorization (authorization is applicable)");
			return true;
		}
	}
	
	private <O extends ObjectType> boolean isApplicable(ObjectSpecificationType objectSpecType, PrismObject<O> object, 
			MidPointPrincipal principal, String desc) throws SchemaException {
		if (objectSpecType == null) {
			LOGGER.trace("  Authorization not applicable for {} because of null object specification");
			return false;
		}
		QueryType specFilter = objectSpecType.getFilter();
		QName specTypeQName = objectSpecType.getType();
		PrismObjectDefinition<O> objectDefinition = object.getDefinition();
		if (specTypeQName != null && !QNameUtil.match(specTypeQName, objectDefinition.getTypeName())) {
			LOGGER.trace("  Authorization not applicable for {} because of type mismatch, expected {}, was {}",
					new Object[]{desc, specTypeQName, objectDefinition.getTypeName()});
			return false;
		}
		List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (specFilter != null) {
				throw new SchemaException("Both filter and special "+desc+" specification specified in authorization");
			}
			for (SpecialObjectSpecificationType special: specSpecial) {
				if (special == SpecialObjectSpecificationType.SELF) {
					String principalOid = principal.getOid();
					if (principalOid == null) {
						// This is a rare case. It should not normally happen. But it may happen in tests
						// or during initial import. Therefore we are not going to die here. Just ignore it.
					} else {
						if (principalOid.equals(object.getOid())) {
							LOGGER.trace("  'self' authorization applicable for {}", desc);
							return true;
						} else {
							LOGGER.trace("  'self' authorization not applicable for {}, principal OID: {}, {} OID {}",
									new Object[]{desc, principalOid, desc, object.getOid()});
						}
					}
				} else {
					throw new SchemaException("Unsupported special "+desc+" specification specified in authorization: "+special);
				}
			}
		} else {
			LOGGER.trace("  specials empty: {}", specSpecial);
		}
		if (specFilter != null) {
			ObjectQuery q = QueryConvertor.parseQuery(object.getCompileTimeClass(), specFilter, object.getPrismContext());
			boolean applicable = ObjectQuery.match(object, q.getFilter(), matchingRuleRegistry);
			if (applicable) {
				LOGGER.trace("  Authorization applicable for {} (filter)", desc);
			} else {
				LOGGER.trace("  Authorization not applicable for {} (filter)", desc);
			}
			return applicable;
		}
		return false;
	}
	
	/**
	 * Spring security method. It is practically applicable only for simple cases.
	 */
	@Override
	public void decide(Authentication authentication, Object object,
			Collection<ConfigAttribute> configAttributes) throws AccessDeniedException,
			InsufficientAuthenticationException {
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
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principalObject)){
				throw new InsufficientAuthenticationException("Not logged in.");
			}
			throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+principalObject.getClass());
		}
		MidPointPrincipal principal = (MidPointPrincipal)principalObject;

		Collection<String> configActions = getActions(configAttributes);
		
		for(String configAction: configActions) {
			boolean isAuthorized;
			try {
				isAuthorized = isAuthorized(configAction, null, null, null);
			} catch (SchemaException e) {
				throw new SystemException(e.getMessage(), e);
			}
			if (isAuthorized) {
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

	@Override
	public boolean supports(ConfigAttribute attribute) {
		if (attribute instanceof SecurityConfig) {
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

	private String getQuotedUsername(Authentication authentication) {
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
	
	private String getQuotedUsername(MidPointPrincipal principal) {
		if (principal == null) {
			return "(none)";
		}
		return "'"+((MidPointPrincipal)principal).getUsername()+"'";
	}
	
}
