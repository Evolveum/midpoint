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
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ItemSecurityConstraints;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;

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
		MidPointPrincipal midPointPrincipal = getMidPointPrincipal();
		if (midPointPrincipal == null) {
			// No need to log, the getMidPointPrincipal() already logs the reason
			return false;
		}
		boolean allow = false;
		LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, object={}, delta={}, target={}",
				new Object[]{midPointPrincipal, operationUrl, object, delta, target});
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
						allowedItems.addAll(getItems(autz));
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
							if (itemPath != null) {
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
							if (itemPath != null) {
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
			if (allowedPath.equivalent(itemPath)) {
				itemAllowed = true;
				break;
			}
		}
		return itemAllowed;
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
		SearchFilterType specFilterType = objectSpecType.getFilter();
		ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
		QName specTypeQName = objectSpecType.getType();
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
			if (!ObjectQuery.match(object, specFilter, matchingRuleRegistry)) {
				LOGGER.trace("  filter authorization not applicable for {}, object OID {}",
						new Object[]{desc, object.getOid()});
				return false;
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
			
            // this is temporary code
//            boolean applicable = false;
//            if (!applicable) {
//                throw new UnsupportedOperationException("fix this!");
//            }
            // end of temporary code
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
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityContraints(PrismObject<O> object) throws SchemaException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (object == null) {
			throw new IllegalArgumentException("Cannot compile security constraints of null object");
		}
		if (principal == null) {
			// No need to log, the getMidPointPrincipal() already logs the reason
			return null;
		}
		LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", principal, object);
		ObjectSecurityConstraints objectSecurityConstraints = new ObjectSecurityConstraints();
		Collection<Authorization> authorities = principal.getAuthorities();
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					LOGGER.trace("Evaluating authorization {}", autz);
					
					// skip action applicability evaluation. We are interested in all actions
					
					// object
					if (isApplicable(autz.getObject(), object, principal, "object")) {
						LOGGER.trace("  Authorization applicable for object {} (continuing evaluation)", object);
					} else {
						LOGGER.trace("  Authorization not applicable for object {}, none of the object specifications match (breaking evaluation)", 
								object);
						continue;
					}
					
					// skip target applicability evaluation. We do not have a target here
					
					List<String> actions = autz.getAction();
					AuthorizationDecisionType decision = autz.getDecision();
					if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
						Collection<ItemPath> items = getItems(autz);
						if (items == null || items.isEmpty()) {
							applyDecision(objectSecurityConstraints.getActionDecisionMap(), actions, AuthorizationDecisionType.ALLOW);
						} else {
							for (ItemPath item: items) {
								applyItemDecision(objectSecurityConstraints.getItemConstraintMap(), item, actions, AuthorizationDecisionType.ALLOW);
							}
						}
					} else {
						Collection<ItemPath> items = getItems(autz);
						if (items == null || items.isEmpty()) {
							applyDecision(objectSecurityConstraints.getActionDecisionMap(), actions, AuthorizationDecisionType.DENY);
						} else {
							for (ItemPath item: items) {
								applyItemDecision(objectSecurityConstraints.getItemConstraintMap(), item, actions, AuthorizationDecisionType.DENY);
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

	private void applyItemDecision(Map<ItemPath, ItemSecurityConstraints> itemConstraintMap, ItemPath item,
			List<String> actions, AuthorizationDecisionType decision) {
		ItemSecurityConstraints entry = itemConstraintMap.get(item);
		if (entry == null) {
			entry = new ItemSecurityConstraints();
			itemConstraintMap.put(item,entry);
		}
		applyDecision(entry.getActionDecisionMap(), actions, decision);
	}

	private void applyDecision(Map<String, AuthorizationDecisionType> actionDecisionMap,
			List<String> actions, AuthorizationDecisionType decision) {
		for (String action: actions) {
			AuthorizationDecisionType existingDecision = actionDecisionMap.get(action);
			if (existingDecision == null) {
				actionDecisionMap.put(action, decision);
			} else if (existingDecision == AuthorizationDecisionType.ALLOW && decision == AuthorizationDecisionType.DENY) {
				// deny overrides
				actionDecisionMap.put(action, decision);
			}
		}
	}

	@Override
	public <O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, Class<O> objectType, 
			ObjectFilter origFilter) throws SchemaException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (principal == null) {
			throw new IllegalArgumentException("No vaild principal");
		}
		LOGGER.trace("AUTZ: evaluating search pre-process principal={}, objectType={}", principal, objectType);
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
					
					// object
					ObjectFilter autzObjSecurityFilter = null;
					List<ObjectSpecificationType> objectSpecTypes = autz.getObject();
					boolean applicable = true;
					if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
						applicable = false;
						for (ObjectSpecificationType objectSpecType: objectSpecTypes) {
							SearchFilterType specFilterType = objectSpecType.getFilter();
							ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
							QName specTypeQName = objectSpecType.getType();
							PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
							ObjectFilter objSpecSecurityFilter = null;
							
							// Type
							if (specTypeQName != null && !QNameUtil.match(specTypeQName, objectDefinition.getTypeName())) {
								LOGGER.trace("  Authorization not applicable for object because of type mismatch, expected {}, was {}",
										new Object[]{specTypeQName, objectDefinition.getTypeName()});
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
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specialFilter);
							} else {
								LOGGER.trace("  specials empty: {}", specSpecial);
							}
							
							// Filter
							if (specFilterType != null) {
								ObjectFilter specFilter = QueryJaxbConvertor.createObjectFilter(objectDefinition, specFilterType, prismContext);
								if (specFilter != null) {
									ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization object is not property-only filter");
								}
								LOGGER.trace("  applying property filter "+specFilter);
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter);
							} else {
								LOGGER.trace("  filter empty");
							}
							
							LOGGER.trace("objSpecSecurityFilter 4 {}", objSpecSecurityFilter);
							
							// Org
							if (specOrgRef != null) {
								OrgFilter orgFilter = OrgFilter.createOrg(specOrgRef.getOid());
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter);
								LOGGER.trace("  applying org filter "+orgFilter);
							} else {
								LOGGER.trace("  org empty");
							}
							
							LOGGER.trace("objSpecSecurityFilter 5 {}", objSpecSecurityFilter);
							
							LOGGER.trace("autzObjSecurityFilter 1 {}", autzObjSecurityFilter);
							autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter);
							LOGGER.trace("autzObjSecurityFilter 2 {}", autzObjSecurityFilter);
						}
					} else {
						LOGGER.trace("  No object specification in authorization (authorization is universaly applicable)");
					}
					
					LOGGER.trace("autzObjSecurityFilter 3 {}", autzObjSecurityFilter);
					
					if (applicable) {
						// authority is applicable to this situation. now we can process the decision.
						AuthorizationDecisionType decision = autz.getDecision();
						if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
							// allow
							if (autzObjSecurityFilter == null) {
								// this is "allow all" authorization.
								hasAllowAll = true;
							} else {
								securityFilterAllow = ObjectQueryUtil.filterOr(securityFilterAllow, autzObjSecurityFilter);
							}
						} else {
							// deny
							if (autzObjSecurityFilter == null) {
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
		
		LOGGER.trace("securityFilterAllow E {}", securityFilterAllow);
		LOGGER.trace("securityFilterDeny E {}", securityFilterDeny);
		LOGGER.trace("hasAllowAll E {}", hasAllowAll);
		
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

		LOGGER.trace("origWithAllowFilter X {}", origWithAllowFilter);
		
		if (securityFilterDeny == null) {
			LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow: {}", 
					new Object[]{principal.getUsername(), operationUrl, origWithAllowFilter});
			return origWithAllowFilter;
		} else {
			ObjectFilter secFilter = ObjectQueryUtil.filterAnd(origWithAllowFilter, NotFilter.createNot(securityFilterDeny));
			LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow (with deny clauses): {}", 
					new Object[]{principal.getUsername(), operationUrl, secFilter});
			return secFilter;
		}
	}
	
	
}
