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
import java.util.Map;

import javax.xml.namespace.QName;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author Radovan Semancik
 *
 */
@Component("securityEnforcer")
public class SecurityEnforcerImpl implements SecurityEnforcer {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
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
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

	@Override
	public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) {
		MidPointPrincipal principal;
		if (userProfileService == null) {
			LOGGER.warn("No user profile service set up in SecurityEnforcer. "
					+ "This is OK in low-level tests but it is a serious problem in running system");
			principal = new MidPointPrincipal(user.asObjectable());
		} else {
			principal = userProfileService.getPrincipal(user);
		}
		Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
        setupPreAuthenticatedSecurityContext(authentication);
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver)
			throws SchemaException {	
		MidPointPrincipal midPointPrincipal = getMidPointPrincipal();
		if (midPointPrincipal == null) {
			// No need to log, the getMidPointPrincipal() already logs the reason
			return false;
		}
		if (phase == null) {
			if (!isAuthorizedInternal(midPointPrincipal, operationUrl, AuthorizationPhaseType.REQUEST, object, delta, target, ownerResolver)) {
				return false;
			}
			return isAuthorizedInternal(midPointPrincipal, operationUrl, AuthorizationPhaseType.EXECUTION, object, delta, target, ownerResolver);
		} else {
			return isAuthorizedInternal(midPointPrincipal, operationUrl, phase, object, delta, target, ownerResolver);
		}
	} 
	
	private <O extends ObjectType, T extends ObjectType> boolean isAuthorizedInternal(MidPointPrincipal midPointPrincipal, String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver)
			throws SchemaException {	
		
		if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(operationUrl)){
			return false;
		}
		
		if (phase == null) {
			throw new IllegalArgumentException("No phase");
		}
		boolean allow = false;
		LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, phase={}, object={}, delta={}, target={}",
				new Object[]{midPointPrincipal, operationUrl, phase, object, delta, target});
		final Collection<ItemPath> allowedItems = new ArrayList<>();
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
					
					// phase
					if (autz.getPhase() == null) {
						LOGGER.trace("  Authorization is applicable for all phases (continuing evaluation)");
					} else {
						if (autz.getPhase() != phase) {
							LOGGER.trace("  Authorization is not applicable for phases {} (breaking evaluation)", phase);
							continue;
						} else {
							LOGGER.trace("  Authorization is applicable for phases {} (continuing evaluation)", phase);
						}
					}
					
					// object
					if (isApplicable(autz.getObject(), object, midPointPrincipal, ownerResolver, "object")) {
						LOGGER.trace("  Authorization applicable for object {} (continuing evaluation)", object);
					} else {
						LOGGER.trace("  Authorization not applicable for object {}, none of the object specifications match (breaking evaluation)", 
								object);
						continue;
					}
					
					// target
					if (isApplicable(autz.getTarget(), target, midPointPrincipal, ownerResolver, "target")) {
						LOGGER.trace("  Authorization applicable for target {} (continuing evaluation)", object);
					} else {
						LOGGER.trace("  Authorization not applicable for target {}, none of the target specifications match (breaking evaluation)", 
								object);
						continue;
					}
					
					// authority is applicable to this situation. now we can process the decision.
					AuthorizationDecisionType decision = autz.getDecision();
					if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
						// if there is more than one role which specify
						// different authz (e.g one role specify allow for whole
						// objet, the other role specify allow only for some
						// attributes. this ended with allow for whole object (MID-2018)
						Collection<ItemPath> allowed = getItems(autz);
						if (allow && allowedItems.isEmpty()){
							LOGGER.trace("  ALLOW operation {} (but continue evaluation)", autz, operationUrl);
						} else if (allow && allowed.isEmpty()){
							allowedItems.clear();
						} else {
							allowedItems.addAll(allowed);
						}
						LOGGER.trace("  ALLOW operation {} (but continue evaluation)", autz, operationUrl);
						allow = true;
						// Do NOT break here. Other authorization statements may still deny the operation
					} else {
						// item
						if (isApplicableItem(autz, object, delta)) {
							LOGGER.trace("  Deny authorization applicable for items (continuing evaluation)");
						} else {
							LOGGER.trace("  Authorization not applicable for items (breaking evaluation)");
							continue;
						}
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
		
		if (allow) {
			// Still check allowedItems. We may still deny the operation.
			if (allowedItems.isEmpty()) {
				// This means all items are allowed. No need to check anything
				LOGGER.trace("  Empty list of allowed items, operation allowed");
			} else {
				// all items in the object and delta must be allowed
				final MutableBoolean itemDecision = new MutableBoolean(true);
				if (delta != null) {
					// If there is delta then consider only the delta.
					Visitor visitor = new Visitor() {
						@Override
						public void visit(Visitable visitable) {
							ItemPath itemPath = getPath(visitable);
							if (itemPath != null && !itemPath.isEmpty()) {
								if (!isInList(itemPath, allowedItems)) {
									LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
									itemDecision.setValue(false);
								}
							}
						}
					};
					delta.accept(visitor);
				} else if (object != null) {
					Visitor visitor = new Visitor() {
						@Override
						public void visit(Visitable visitable) {
							ItemPath itemPath = getPath(visitable);
							if (itemPath != null && !itemPath.isEmpty()) {
								if (!isInList(itemPath, allowedItems)) {
									LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
									itemDecision.setValue(false);
								}
							}
						}
					};
					object.accept(visitor);
				}
				allow = itemDecision.booleanValue();
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ result: principal={}, operation={}: {}", new Object[]{midPointPrincipal, operationUrl, allow});
		}
		return allow;
	}
	
	private ItemPath getPath(Visitable visitable) {
		if (visitable instanceof ItemDelta) {
			return ((ItemDelta)visitable).getPath();
		} else if (visitable instanceof Item) {
			return ((Item)visitable).getPath();
		} else {
			return null;
		}
	}
	
	private boolean isInList(ItemPath itemPath, Collection<ItemPath> allowedItems) {
		boolean itemAllowed = false;
		for (ItemPath allowedPath: allowedItems) {
			if (allowedPath.isSubPathOrEquivalent(itemPath)) {
				itemAllowed = true;
				break;
			}
		}
		return itemAllowed;
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, 
			OperationResult result) throws SecurityViolationException, SchemaException {
		MidPointPrincipal principal = getPrincipal();
		boolean allow = isAuthorized(operationUrl, phase, object, delta, target, ownerResolver);
		if (!allow) {
			String username = getQuotedUsername(principal);
			LOGGER.error("User {} not authorized for operation {}", username, operationUrl);
			AuthorizationException e = new AuthorizationException("User "+username+" not authorized for operation "
			+operationUrl);
//			+":\n"+((MidPointPrincipal)principal).debugDump());
			result.recordFatalError(e.getMessage(), e);
			throw e;
		}
	}
	
	private <O extends ObjectType> boolean isApplicable(List<OwnedObjectSpecificationType> objectSpecTypes, PrismObject<O> object, 
			MidPointPrincipal midPointPrincipal, OwnerResolver ownerResolver, String desc) throws SchemaException {
		if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
			if (object == null) {
				LOGGER.trace("  Authorization not applicable for null "+desc);
				return false;
			}
			for (OwnedObjectSpecificationType autzObject: objectSpecTypes) {
				if (isApplicable(autzObject, object, midPointPrincipal, ownerResolver, desc)) {
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
			MidPointPrincipal principal, OwnerResolver ownerResolver, String desc) throws SchemaException {
		if (objectSpecType == null) {
			LOGGER.trace("  Authorization not applicable for {} because of null object specification");
			return false;
		}
		SearchFilterType specFilterType = objectSpecType.getFilter();
		ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
		QName specTypeQName = objectSpecType.getType();     // now it does not matter if it's unqualified
		PrismObjectDefinition<O> objectDefinition = object.getDefinition();
		
		// Type
		if (specTypeQName != null && !QNameUtil.match(specTypeQName, objectDefinition.getTypeName())) {
			LOGGER.trace("  Authorization not applicable for {} because of type mismatch, expected {}, was {}",
					new Object[]{desc, specTypeQName, objectDefinition.getTypeName()});
			return false;
		}
		
		// Special
		List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (specFilterType != null || specOrgRef != null) {
				throw new SchemaException("Both filter/org and special "+desc+" specification specified in authorization");
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
			return false;
		} else {
			LOGGER.trace("  specials empty: {}", specSpecial);
		}
		
		// Filter
		if (specFilterType != null) {
			ObjectFilter specFilter = QueryJaxbConvertor.createObjectFilter(object.getCompileTimeClass(), specFilterType, object.getPrismContext());
			if (specFilter != null) {
				ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization "+desc+" is not property-only filter");
			}
			try {
				if (!ObjectQuery.match(object, specFilter, matchingRuleRegistry)) {
					LOGGER.trace("  filter authorization not applicable for {}, object OID {}", new Object[] {
							desc, object.getOid() });
					return false;
				}
			} catch (SchemaException ex) {
				throw new SchemaException("Could not apply authorization for " + object + ". "
						+ ex.getMessage(), ex);
			}
		}
			
		// Org	
		if (specOrgRef != null) {
			
			List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
			List<String> objParentOrgOids = new ArrayList<>(objParentOrgRefs.size());
			for (ObjectReferenceType objParentOrgRef: objParentOrgRefs) {
				objParentOrgOids.add(objParentOrgRef.getOid());
			}
			
			boolean anySubordinate = repositoryService.isAnySubordinate(specOrgRef.getOid(), objParentOrgOids);
			if (!anySubordinate) {
				LOGGER.trace("  org authorization not applicable for {}, object OID {} (autz={} parentRefs={})",
						new Object[]{desc, object.getOid(), specOrgRef.getOid(), objParentOrgOids});
				return false;
			}			
		}
		
		if (objectSpecType instanceof OwnedObjectSpecificationType) {
			// Owner
			ObjectSpecificationType ownerSpec = ((OwnedObjectSpecificationType)objectSpecType).getOwner();
			if (ownerSpec != null) {
				if (!object.canRepresent(ShadowType.class)) {
					LOGGER.trace("  owner object spec not applicable for {}, object OID {} because it is not a shadow",
							new Object[]{desc, object.getOid()});
					return false;
				}
				if (ownerResolver == null) {
					ownerResolver = userProfileService;
					if (ownerResolver == null) {
						LOGGER.trace("  owner object spec not applicable for {}, object OID {} because there is no owner resolver",
								new Object[]{desc, object.getOid()});
						return false;
					}
				}
				PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner((PrismObject<ShadowType>)object);
				if (owner == null) {
					LOGGER.trace("  owner object spec not applicable for {}, object OID {} because it has no owner",
							new Object[]{desc, object.getOid()});
					return false;
				}
				boolean ownerApplicable = isApplicable(ownerSpec, owner, principal, ownerResolver, "owner of "+desc);
				if (!ownerApplicable) {
					LOGGER.trace("  owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
							new Object[]{desc, object.getOid(), owner});
					return false;
				}			
			}
		}

		LOGGER.trace("  Authorization applicable for {} (filter)", desc);
		return true;
	}
	
	private <O extends ObjectType, T extends ObjectType> boolean isApplicableItem(Authorization autz,
			PrismObject<O> object, ObjectDelta<O> delta) {
		List<ItemPathType> itemPaths = autz.getItem();
		if (itemPaths == null || itemPaths.isEmpty()) {
			// No item constraints. Applicable for all items.
			LOGGER.trace("  items empty");
			return true;
		}
		for (ItemPathType itemPathType: itemPaths) {
			ItemPath itemPath = itemPathType.getItemPath();
			if (object != null) {
				Item<?> item = object.findItem(itemPath);
				if (item != null && !item.isEmpty()) {
					LOGGER.trace("  applicable object item "+itemPath);
					return true;
				}
			}
			if (delta != null) {
				ItemDelta<PrismValue> itemDelta = delta.findItemDelta(itemPath);
				if (itemDelta != null && !itemDelta.isEmpty()) {
					LOGGER.trace("  applicable delta item "+itemPath);
					return true;
				}
			}
		}
		LOGGER.trace("  no applicable item");
		return false;
	}
	
	private Collection<ItemPath> getItems(Authorization autz) {
		List<ItemPathType> itemPaths = autz.getItem();
		Collection<ItemPath> items = new ArrayList<>(itemPaths.size());
		if (itemPaths != null) {
			for (ItemPathType itemPathType: itemPaths) {
				ItemPath itemPath = itemPathType.getItemPath();
				items.add(itemPath);
			}
		}
		return items;
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

		Collection<String> configActions = getActions(configAttributes);
		
		for(String configAction: configActions) {
			boolean isAuthorized;
			try {
				isAuthorized = isAuthorized(configAction, null, null, null, null, null);
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

	private MidPointPrincipal getMidPointPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			LOGGER.warn("No authentication");
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal == null) {
			LOGGER.warn("Null principal");
			return null;
		}
		if (!(principal instanceof MidPointPrincipal)) {
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principal)){
				LOGGER.trace("AUTZ: deny because user is not logged in");
				return null;
			}
			LOGGER.warn("Unknown principal type {}", principal.getClass());
			return null;
		}
		return (MidPointPrincipal)principal;
	}
	
	@Override
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver) throws SchemaException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (object == null) {
			throw new IllegalArgumentException("Cannot compile security constraints of null object");
		}
		if (principal == null) {
			// No need to log, the getMidPointPrincipal() already logs the reason
			return null;
		}
		LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", principal, object);
		ObjectSecurityConstraintsImpl objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
		Collection<Authorization> authorities = principal.getAuthorities();
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					LOGGER.trace("Evaluating authorization {}", autz);
					
					// skip action applicability evaluation. We are interested in all actions
					
					// object
					if (isApplicable(autz.getObject(), object, principal, ownerResolver, "object")) {
						LOGGER.trace("  Authorization applicable for object {} (continuing evaluation)", object);
					} else {
						LOGGER.trace("  Authorization not applicable for object {}, none of the object specifications match (breaking evaluation)", 
								object);
						continue;
					}
					
					// skip target applicability evaluation. We do not have a target here
					
					List<String> actions = autz.getAction();
					AuthorizationPhaseType phase = autz.getPhase();
					AuthorizationDecisionType decision = autz.getDecision();
					if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
						Collection<ItemPath> items = getItems(autz);
						if (items == null || items.isEmpty()) {
							applyDecision(objectSecurityConstraints.getActionDecisionMap(), actions, phase, AuthorizationDecisionType.ALLOW);
						} else {
							for (ItemPath item: items) {
								applyItemDecision(objectSecurityConstraints.getItemConstraintMap(), item, actions, phase, AuthorizationDecisionType.ALLOW);
							}
						}
					} else {
						Collection<ItemPath> items = getItems(autz);
						if (items == null || items.isEmpty()) {
							applyDecision(objectSecurityConstraints.getActionDecisionMap(), actions, phase, AuthorizationDecisionType.DENY);
						} else {
							for (ItemPath item: items) {
								applyItemDecision(objectSecurityConstraints.getItemConstraintMap(), item, actions, phase, AuthorizationDecisionType.DENY);
							}
						}
					}
					
				} else {
					LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), principal.getUsername());
				}
			}
		}

		return objectSecurityConstraints;
	}

	private void applyItemDecision(Map<ItemPath, ItemSecurityConstraintsImpl> itemConstraintMap, ItemPath item,
			List<String> actions, AuthorizationPhaseType phase, AuthorizationDecisionType decision) {
		ItemSecurityConstraintsImpl entry = itemConstraintMap.get(item);
		if (entry == null) {
			entry = new ItemSecurityConstraintsImpl();
			itemConstraintMap.put(item,entry);
		}
		applyDecision(entry.getActionDecisionMap(), actions, phase, decision);
	}

	private void applyDecision(Map<String, PhaseDecisionImpl> actionDecisionMap,
			List<String> actions, AuthorizationPhaseType phase, AuthorizationDecisionType decision) {
		for (String action: actions) {
			if (phase == null) {
				applyDecisionRequest(actionDecisionMap, action, decision);
				applyDecisionExecution(actionDecisionMap, action, decision);
			} else if (phase == AuthorizationPhaseType.REQUEST){
				applyDecisionRequest(actionDecisionMap, action, decision);
			} else if (phase == AuthorizationPhaseType.EXECUTION) {
				applyDecisionExecution(actionDecisionMap, action, decision);
			} else {
				throw new IllegalArgumentException("Unknown phase "+phase);
			}
		}
	}
	
	private void applyDecisionRequest(Map<String, PhaseDecisionImpl> actionDecisionMap,
			String action, AuthorizationDecisionType decision) {
		PhaseDecisionImpl phaseDecision = actionDecisionMap.get(action);
		if (phaseDecision == null) {
			phaseDecision = new PhaseDecisionImpl();
			phaseDecision.setRequestDecision(decision);
			actionDecisionMap.put(action, phaseDecision);
		} else if (phaseDecision.getRequestDecision() == null ||
				// deny overrides
				(phaseDecision.getRequestDecision() == AuthorizationDecisionType.ALLOW && decision == AuthorizationDecisionType.DENY)) {
			phaseDecision.setRequestDecision(decision);
		}
	}

	private void applyDecisionExecution(Map<String, PhaseDecisionImpl> actionDecisionMap,
			String action, AuthorizationDecisionType decision) {
		PhaseDecisionImpl phaseDecision = actionDecisionMap.get(action);
		if (phaseDecision == null) {
			phaseDecision = new PhaseDecisionImpl();
			phaseDecision.setExecDecision(decision);
			actionDecisionMap.put(action, phaseDecision);
		} else if (phaseDecision.getExecDecision() == null ||
				// deny overrides
				(phaseDecision.getExecDecision() == AuthorizationDecisionType.ALLOW && decision == AuthorizationDecisionType.DENY)) {
			phaseDecision.setExecDecision(decision);
		}
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase, 
			Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter) throws SchemaException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (principal == null) {
			throw new IllegalArgumentException("No vaild principal");
		}
		LOGGER.trace("AUTZ: evaluating search pre-process principal={}, objectType={}: orig filter {}", 
				new Object[]{principal, objectType, origFilter});
		if (origFilter == null) {
			origFilter = AllFilter.createAll();
		}
		ObjectFilter finalFilter;
		if (phase != null) {
			finalFilter = preProcessObjectFilterInternal(principal, operationUrl, phase, true, objectType, object, origFilter);
		} else {
			ObjectFilter filterBoth = preProcessObjectFilterInternal(principal, operationUrl, null, false, objectType, object, origFilter);
			ObjectFilter filterRequest = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.REQUEST, 
					false, objectType, object, origFilter);
			ObjectFilter filterExecution = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.EXECUTION,
					false, objectType, object, origFilter);
			finalFilter = ObjectQueryUtil.filterOr(filterBoth, ObjectQueryUtil.filterAnd(filterRequest, filterExecution));
		}
		LOGGER.trace("AUTZ: evaluated search pre-process principal={}, objectType={}: {}", 
				new Object[]{principal, objectType, finalFilter});
		if (finalFilter instanceof AllFilter) {
			// compatibility
			return null;
		}
		return finalFilter;
	}
	
	private <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilterInternal(MidPointPrincipal principal, String operationUrl, 
			AuthorizationPhaseType phase, boolean includeNullPhase, 
			Class<T> objectType, PrismObject<O> object, ObjectFilter origFilter) throws SchemaException {
		Collection<Authorization> authorities = principal.getAuthorities();
		ObjectFilter securityFilterAllow = null;
		ObjectFilter securityFilterDeny = null;
		boolean hasAllowAll = false;
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					LOGGER.trace("Evaluating authorization {}", autz);
					
					// action
					if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
						LOGGER.trace("  Authorization not applicable for operation {}", operationUrl);
						continue;
					}
	
					// phase
					if (autz.getPhase() == phase || (includeNullPhase && autz.getPhase() == null)) {
						LOGGER.trace("  Authorization is applicable for phases {} (continuing evaluation)", phase);
					} else {
						LOGGER.trace("  Authorization is not applicable for phase {}", phase);
						continue;
					}
	
					// object or target
					ObjectFilter autzObjSecurityFilter = null;
					List<OwnedObjectSpecificationType> objectSpecTypes;
					if (object == null) {
						// object not present. Therefore we are looking for object here
						objectSpecTypes = autz.getObject();
					} else {
						// object present. Therefore we are looking for target
						objectSpecTypes = autz.getTarget();
						
						// .. but we need to decide whether this authorization is applicable to the object
						if (!isApplicableItem(autz, object, null)) {
							LOGGER.trace("  Authorization is not applicable for object {}", object);
						}
					}

					boolean applicable = true;
					if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
						applicable = false;
						for (OwnedObjectSpecificationType objectSpecType: objectSpecTypes) {
							ObjectFilter objSpecSecurityFilter = null;
							TypeFilter objSpecTypeFilter = null;
							SearchFilterType specFilterType = objectSpecType.getFilter();
							ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
							QName specTypeQName = objectSpecType.getType();
							PrismObjectDefinition<T> objectDefinition = null;
							
							// Type
							if (specTypeQName != null) {
                                specTypeQName = prismContext.getSchemaRegistry().qualifyTypeName(specTypeQName);
								PrismObjectDefinition<?> specObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(specTypeQName);
								Class<?> specObjectClass = specObjectDef.getCompileTimeClass();
								if (!objectType.isAssignableFrom(specObjectClass)) {
									LOGGER.trace("  Authorization not applicable for object because of type mismatch, authorization {}, query {}",
											new Object[]{specObjectClass, objectType});
									continue;
								} else {
									LOGGER.trace("  Authorization is applicable for object because of type match, authorization {}, query {}",
											new Object[]{specObjectClass, objectType});
									// The spec type is a subclass of requested type. So it might be returned from the search.
									// We need to use type filter.
									objSpecTypeFilter = TypeFilter.createType(specTypeQName, null);
									// and now we have a more specific object definition to use later in filter processing
									objectDefinition = (PrismObjectDefinition<T>) specObjectDef;
								}
							}
							
							// Owner
							if (objectSpecType.getOwner() != null) {
								LOGGER.trace("  Authorization not applicable for object because it has owner specification (this is not applicable for search)");
								continue;
							}
							
							applicable = true;
							
							// Special
							List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
							if (specSpecial != null && !specSpecial.isEmpty()) {
								if (specFilterType != null || specOrgRef != null) {
									throw new SchemaException("Both filter/org and special object specification specified in authorization");
								}
								ObjectFilter specialFilter = null;
								for (SpecialObjectSpecificationType special: specSpecial) {
									if (special == SpecialObjectSpecificationType.SELF) {
										String principalOid = principal.getOid();
										specialFilter = ObjectQueryUtil.filterOr(specialFilter, InOidFilter.createInOid(principalOid));
									} else {
										throw new SchemaException("Unsupported special object specification specified in authorization: "+special);
									}
								}
                                objSpecSecurityFilter = specTypeQName != null ?
                                        TypeFilter.createType(specTypeQName, specialFilter) : specialFilter;
							} else {
								LOGGER.trace("  specials empty: {}", specSpecial);
							}
							
							// Filter
							if (specFilterType != null) {
								if (objectDefinition == null) {
									objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
								}
								ObjectFilter specFilter = QueryJaxbConvertor.createObjectFilter(objectDefinition, specFilterType, prismContext);
								if (specFilter != null) {
									ObjectQueryUtil.assertNotRaw(specFilter, "Filter in authorization object has undefined items. Maybe a 'type' specification is missing in the authorization?");
									ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization object is not property-only filter");
								}
								LOGGER.trace("  applying property filter "+specFilter);
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter);
							} else {
								LOGGER.trace("  filter empty");
							}
							
							// Org
							if (specOrgRef != null) {
								OrgFilter orgFilter = OrgFilter.createOrg(specOrgRef.getOid());
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter);
								LOGGER.trace("  applying org filter "+orgFilter);
							} else {
								LOGGER.trace("  org empty");
							}
							
							if (objSpecTypeFilter != null) {
								objSpecTypeFilter.setFilter(objSpecSecurityFilter);
								objSpecSecurityFilter = objSpecTypeFilter;
							}
							
							autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter);
						}
					} else {
						LOGGER.trace("  No object specification in authorization (authorization is universaly applicable)");
						autzObjSecurityFilter = AllFilter.createAll();
					}
					
					if (applicable) {
						// authority is applicable to this situation. now we can process the decision.
						AuthorizationDecisionType decision = autz.getDecision();
						if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
							// allow
							if (ObjectQueryUtil.isAll(autzObjSecurityFilter)) {
								// this is "allow all" authorization.
								hasAllowAll = true;
							} else {
								securityFilterAllow = ObjectQueryUtil.filterOr(securityFilterAllow, autzObjSecurityFilter);
							}
						} else {
							// deny
							if (ObjectQueryUtil.isAll(autzObjSecurityFilter)) {
								// This is "deny all". We cannot have anything stronger than that.
								// There is no point in continuing the evaluation.
								LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: deny all", new Object[]{principal.getUsername(), operationUrl});
								return NoneFilter.createNone();
							}
							securityFilterDeny = ObjectQueryUtil.filterOr(securityFilterDeny, autzObjSecurityFilter);
						}
					}
					
				} else {
					LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), principal.getUsername());
				}
			}
		}
		
		ObjectFilter origWithAllowFilter;
		if (hasAllowAll) {
			origWithAllowFilter = origFilter;
		} else if (securityFilterAllow == null) {
			// Nothing has been allowed. This means default deny.
			LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: default deny", new Object[]{principal.getUsername(), operationUrl});
			return NoneFilter.createNone();
		} else {
			origWithAllowFilter = ObjectQueryUtil.filterAnd(origFilter, securityFilterAllow);
		}

		if (securityFilterDeny == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow:\n{}", 
					new Object[]{principal.getUsername(), operationUrl, origWithAllowFilter==null?"null":origWithAllowFilter.debugDump()});
			}
			return origWithAllowFilter;
		} else {
			ObjectFilter secFilter = ObjectQueryUtil.filterAnd(origWithAllowFilter, NotFilter.createNot(securityFilterDeny));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow (with deny clauses):\n{}", 
					new Object[]{principal.getUsername(), operationUrl, secFilter==null?"null":secFilter.debugDump()});
			}
			return secFilter;
		}
	}
	
	
}
