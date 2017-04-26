/**
 * Copyright (c) 2014-2017 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ItemSecurityDecisions;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgRelationObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSelectorType;
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

	private static final boolean FILTER_TRACE_ENABLED = false;
	
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
	public boolean isAuthenticated() {
		return SecurityUtil.isAuthenticated();
	}

	@Override
    public void setupPreAuthenticatedSecurityContext(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

	@Override
	public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) throws SchemaException {
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
		Collection<Authorization> authorities = getAuthorities(midPointPrincipal);
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					String autzHumanReadableDesc = autz.getHumanReadableDesc();
					LOGGER.trace("Evaluating {}", autzHumanReadableDesc);
					
					// First check if the authorization is applicable.
					
					// action
					if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
						LOGGER.trace("  {} not applicable for operation {}", autzHumanReadableDesc, operationUrl);
						continue;
					}
					
					// phase
					if (autz.getPhase() == null) {
						LOGGER.trace("  {} is applicable for all phases (continuing evaluation)", autzHumanReadableDesc);
					} else {
						if (autz.getPhase() != phase) {
							LOGGER.trace("  {} is not applicable for phases {} (breaking evaluation)", autzHumanReadableDesc, phase);
							continue;
						} else {
							LOGGER.trace("  {} is applicable for phases {} (continuing evaluation)", autzHumanReadableDesc, phase);
						}
					}
					
					// object
					if (isApplicable(autz.getObject(), object, midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc)) {
						LOGGER.trace("  {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
					} else {
						LOGGER.trace("  {} not applicable for object {}, none of the object specifications match (breaking evaluation)", 
								autzHumanReadableDesc, object);
						continue;
					}
					
					// target
					if (isApplicable(autz.getTarget(), target, midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc)) {
						LOGGER.trace("  {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, object);
					} else {
						LOGGER.trace("  {} not applicable for target {}, none of the target specifications match (breaking evaluation)", 
								autzHumanReadableDesc, object);
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
							LOGGER.trace("  {}: ALLOW operation {} (but continue evaluation)", autzHumanReadableDesc, operationUrl);
						} else if (allow && allowed.isEmpty()){
							allowedItems.clear();
						} else {
							allowedItems.addAll(allowed);
						}
						LOGGER.trace("  {}: ALLOW operation {} (but continue evaluation)", autzHumanReadableDesc, operationUrl);
						allow = true;
						// Do NOT break here. Other authorization statements may still deny the operation
					} else {
						// item
						if (isApplicableItem(autz, object, delta)) {
							LOGGER.trace("  {}: Deny authorization applicable for items (continuing evaluation)", autzHumanReadableDesc);
						} else {
							LOGGER.trace("  {} not applicable for items (breaking evaluation)", autzHumanReadableDesc);
							continue;
						}
						LOGGER.trace("  {}: DENY operation {}", autzHumanReadableDesc, operationUrl);
						allow = false;
						// Break right here. Deny cannot be overridden by allow. This decision cannot be changed. 
						break;
					}
					
				} else {
					LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), getUsername(midPointPrincipal));
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
				
				if (delta != null) {
					allow = processAuthorizationDelta(delta, allowedItems);
				} else if (object != null) {
					allow = processAuthorizationObject(object, allowedItems);
				}
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ result: principal={}, operation={}: {}", new Object[]{midPointPrincipal, operationUrl, allow});
		}
		return allow;
	}
	
	private <O extends ObjectType> boolean processAuthorizationObject(PrismContainer<O> object, final Collection<ItemPath> allowedItems) {
		return isContainerAllowed(object.getValue(), allowedItems);
	}
	
	private <C extends Containerable> boolean processAuthorizationContainerDelta(ContainerDelta<C> cdelta, final Collection<ItemPath> allowedItems) {
		final MutableBoolean itemDecision = new MutableBoolean(true);
		cdelta.foreach(cval -> {
			if (!isContainerAllowed(cval, allowedItems)) {
				itemDecision.setValue(false);
			}
		});
		return itemDecision.booleanValue();
	}
	
	private Visitor createItemVisitor(final Collection<ItemPath> allowedItems, final MutableBoolean itemDecision) {
		return new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable instanceof Item) {
					
					// TODO: problem with empty containers such as
					// orderConstraint in assignment. Skip all 
					// empty items ... for now.
					if (((Item)visitable).isEmpty()) {
						return;
					}
					
					ItemPath itemPath = ((Item)visitable).getPath();
					if (itemPath != null && !itemPath.isEmpty()) {
						if (!isInList(itemPath, allowedItems)) {
							LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
							itemDecision.setValue(false);
						}
					}
				}
			}
		};
	}
	
	private boolean isContainerAllowed(PrismContainerValue<?> cval, Collection<ItemPath> allowedItems) {
		if (cval.isEmpty()) {
			// TODO: problem with empty containers such as
			// orderConstraint in assignment. Skip all 
			// empty items ... for now.
			return true;
		}
		boolean decision = true;
		for (Item<?, ?> item: cval.getItems()) {
			ItemPath itemPath = item.getPath();
			if (item instanceof PrismContainer<?>) {
				if (isInList(itemPath, allowedItems)) {
					// entire container is allowed. We do not need to go deeper
				} else {
					List<PrismContainerValue<?>> subValues = (List)((PrismContainer<?>)item).getValues();
					for (PrismContainerValue<?> subValue: subValues) {
						if (!isContainerAllowed(subValue, allowedItems)) {
							decision = false;
						}
					}
				}
			} else {
				if (!isInList(itemPath, allowedItems)) {
					LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
					decision = false;
				}
			}
		}
		return decision;
	}
	
	private <O extends ObjectType> boolean processAuthorizationDelta(ObjectDelta<O> delta, final Collection<ItemPath> allowedItems) {
		if (delta.isAdd()) {
			return processAuthorizationObject(delta.getObjectToAdd(), allowedItems);
		} else {
			for (ItemDelta<?,?> itemDelta: delta.getModifications()) {
				ItemPath itemPath = itemDelta.getPath();
				if (itemDelta instanceof ContainerDelta<?>) {
					if (!isInList(itemPath, allowedItems)) {
						if (!processAuthorizationContainerDelta((ContainerDelta<?>)itemDelta, allowedItems)) {
							return false;
						}
					}
				} else {
					if (!isInList(itemPath, allowedItems)) {
						LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
						return false;
					}
				}
			}
			return true;
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
		boolean allow = isAuthorized(operationUrl, phase, object, delta, target, ownerResolver);
		if (!allow) {
			failAuthorization(operationUrl, phase, object, delta, target, result);
		}
	}
	
	@Override
	public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OperationResult result) throws SecurityViolationException {
		MidPointPrincipal principal = getPrincipal();
		String username = getQuotedUsername(principal);
		String message;
		if (target == null && object == null) {
			message = "User '"+username+"' not authorized for operation "+ operationUrl;
		} else if (target == null) {
			message = "User '"+username+"' not authorized for operation "+ operationUrl + " on " + object;
		} else {
			message = "User '"+username+"' not authorized for operation "+ operationUrl + " on " + object + " with target " + target;
		}
		LOGGER.error("{}", message);
		AuthorizationException e = new AuthorizationException(message);
		result.recordFatalError(e.getMessage(), e);
		throw e;
	}
	
	private <O extends ObjectType> boolean isApplicable(List<OwnedObjectSelectorType> objectSpecTypes, PrismObject<O> object, 
			MidPointPrincipal midPointPrincipal, OwnerResolver ownerResolver, String desc, String autzHumanReadableDesc) throws SchemaException {
		if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
			if (object == null) {
				LOGGER.trace("  {} not applicable for null {}", autzHumanReadableDesc, desc);
				return false;
			}
			for (OwnedObjectSelectorType autzObject: objectSpecTypes) {
				if (isApplicable(autzObject, object, midPointPrincipal, ownerResolver, desc, autzHumanReadableDesc)) {
					return true;
				}
			}
			return false;
		} else {
			LOGGER.trace("  {}: No {} specification in authorization (authorization is applicable)", autzHumanReadableDesc, desc);
			return true;
		}
	}
	
	private <O extends ObjectType> boolean isApplicable(SubjectedObjectSelectorType objectSelector, PrismObject<O> object, 
			MidPointPrincipal principal, OwnerResolver ownerResolver, String desc, String autzHumanReadableDesc) throws SchemaException {
		if (!repositoryService.selectorMatches(objectSelector, object, LOGGER, "  " + autzHumanReadableDesc + " not applicable for " + desc + " because of ")) {
			return false;
		}
		
		OrgRelationObjectSpecificationType specOrgRelation = objectSelector.getOrgRelation();
				
		// Special
		List<SpecialObjectSpecificationType> specSpecial = objectSelector.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (objectSelector.getFilter() != null || objectSelector.getOrgRef() != null || specOrgRelation != null) {
				throw new SchemaException("Both filter/org and special "+desc+" specification specified in "+autzHumanReadableDesc);
			}
			for (SpecialObjectSpecificationType special: specSpecial) {
				if (special == SpecialObjectSpecificationType.SELF) {
					String principalOid = principal.getOid();
					if (principalOid == null) {
						// This is a rare case. It should not normally happen. But it may happen in tests
						// or during initial import. Therefore we are not going to die here. Just ignore it.
					} else {
						if (principalOid.equals(object.getOid())) {
							LOGGER.trace("  {}: 'self' authorization applicable for {}", autzHumanReadableDesc, desc);
							return true;
						} else {
							LOGGER.trace("  {}: 'self' authorization not applicable for {}, principal OID: {}, {} OID {}",
									new Object[]{autzHumanReadableDesc, desc, principalOid, desc, object.getOid()});
						}
					}
				} else {
					throw new SchemaException("Unsupported special "+desc+" specification specified in "+autzHumanReadableDesc+": "+special);
				}
			}
			return false;
		} else {
			LOGGER.trace("  {}: specials empty: {}", autzHumanReadableDesc, specSpecial);
		}
		
		// orgRelation
		if (specOrgRelation != null) {
			boolean match = false;
			for (ObjectReferenceType subjectParentOrgRef: principal.getUser().getParentOrgRef()) {
				if (matchesOrgRelation(object, subjectParentOrgRef, specOrgRelation, autzHumanReadableDesc, desc)) {
					LOGGER.trace("  org {} applicable for {}, object OID {} because subject org {} matches",
							new Object[]{autzHumanReadableDesc, desc, object.getOid(), subjectParentOrgRef.getOid()});
					match = true;
					break;
				}
			}
			if (!match) {
				LOGGER.trace("  org {} not applicable for {}, object OID {} because none of the subject orgs matches",
						new Object[]{autzHumanReadableDesc, desc, object.getOid()});
				return false;
			}			
		}
		
		if (objectSelector instanceof OwnedObjectSelectorType) {
			// Owner
			SubjectedObjectSelectorType ownerSpec = ((OwnedObjectSelectorType)objectSelector).getOwner();
			if (ownerSpec != null) {
				if (ownerResolver == null) {
					ownerResolver = userProfileService;
					if (ownerResolver == null) {
						LOGGER.trace("  {}: owner object spec not applicable for {}, object OID {} because there is no owner resolver",
								autzHumanReadableDesc, desc, object.getOid());
						return false;
					}
				}
				PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object);
				if (owner == null) {
					LOGGER.trace("  {}: owner object spec not applicable for {}, object OID {} because it has no owner",
							autzHumanReadableDesc, desc, object.getOid());
					return false;
				}
				boolean ownerApplicable = isApplicable(ownerSpec, owner, principal, ownerResolver, "owner of "+desc, autzHumanReadableDesc);
				if (!ownerApplicable) {
					LOGGER.trace("  {}: owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
							autzHumanReadableDesc, desc, object.getOid(), owner);
					return false;
				}			
			}
		}

		LOGGER.trace("  {} applicable for {} (filter)", autzHumanReadableDesc, desc);
		return true;
	}
	
	private <O extends ObjectType> boolean matchesOrgRelation(PrismObject<O> object, ObjectReferenceType subjectParentOrgRef,
			OrgRelationObjectSpecificationType specOrgRelation, String autzHumanReadableDesc, String desc) throws SchemaException {
		if (!MiscSchemaUtil.compareRelation(specOrgRelation.getSubjectRelation(), subjectParentOrgRef.getRelation())) {
			return false;
		}
		if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg()) && subjectParentOrgRef.getOid().equals(object.getOid())) {
			return true;
		}
		if (specOrgRelation.getScope() == null) {
			return repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
		}
		switch (specOrgRelation.getScope()) {
			case ALL_DESCENDANTS:
				return repositoryService.isDescendant(object, subjectParentOrgRef.getOid());
			case DIRECT_DESCENDANTS:
				return hasParentOrgRef(object, subjectParentOrgRef.getOid());
			case ALL_ANCESTORS:
				return repositoryService.isAncestor(object, subjectParentOrgRef.getOid());
			default:
				throw new UnsupportedOperationException("Unknown orgRelation scope "+specOrgRelation.getScope());
		}
	}
	
	private <O extends ObjectType> boolean hasParentOrgRef(PrismObject<O> object, String oid) {
		List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
		for (ObjectReferenceType objParentOrgRef: objParentOrgRefs) {
			if (oid.equals(objParentOrgRef.getOid())) {
				return true;
			}
		}
		return false;
	}
	
	private <O extends ObjectType, T extends ObjectType> boolean isApplicableItem(Authorization autz,
			PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
		List<ItemPathType> itemPaths = autz.getItem();
		if (itemPaths == null || itemPaths.isEmpty()) {
			// No item constraints. Applicable for all items.
			LOGGER.trace("  items empty");
			return true;
		}
		for (ItemPathType itemPathType: itemPaths) {
			ItemPath itemPath = itemPathType.getItemPath();
			if (delta == null) {
				if (object != null) {
					if (object.containsItem(itemPath, false)) {
						LOGGER.trace("  applicable object item "+itemPath);
						return true;
					}
				}
			} else {
				ItemDelta<?,?> itemDelta = delta.findItemDelta(itemPath);
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
			SecurityUtil.logSecurityDeny(object, ": Unknown type of secure object");
			throw new IllegalArgumentException("Unknown type of secure object");
		}
		
		Object principalObject = authentication.getPrincipal();
		if (!(principalObject instanceof MidPointPrincipal)) {
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principalObject)){
				SecurityUtil.logSecurityDeny(object, ": Not logged in");
				throw new InsufficientAuthenticationException("Not logged in.");
			}
			throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+principalObject.getClass());
		}

		Collection<String> configActions = SecurityUtil.getActions(configAttributes);
		
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
		
		SecurityUtil.logSecurityDeny(object, ": Not authorized", null, configActions);
		
		// Sparse exception method by purpose. We do not want to expose details to attacker.
		// Better message is logged.
		throw new AccessDeniedException("Not authorized");
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
				return null;
			}
			LOGGER.warn("Unknown principal type {}", principal.getClass());
			return null;
		}
		return (MidPointPrincipal)principal;
	}
	
	private Collection<Authorization> getAuthorities(MidPointPrincipal principal) {
		if (principal == null) {
			// Anonymous access, possibly with elevated privileges
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			Collection<Authorization> authorizations = new ArrayList<>();
                        if (authentication != null) {
                            for (GrantedAuthority authority: authentication.getAuthorities()) {
                                    if (authority instanceof Authorization) {
                                            authorizations.add((Authorization)authority);
                                    }
                            }
                        }
			return authorizations;
		} else {
			return principal.getAuthorities();
		}
	}
	
	@Override
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver) throws SchemaException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (object == null) {
			throw new IllegalArgumentException("Cannot compile security constraints of null object");
		}
		LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", principal, object);
		ObjectSecurityConstraintsImpl objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
		Collection<Authorization> authorities = getAuthorities(principal);
		if (authorities != null) {
			for (Authorization autz: authorities) {
				String autzHumanReadableDesc = autz.getHumanReadableDesc();
				LOGGER.trace("Evaluating {}", autzHumanReadableDesc);
				
				// skip action applicability evaluation. We are interested in all actions
				
				// object
				if (isApplicable(autz.getObject(), object, principal, ownerResolver, "object", autzHumanReadableDesc)) {
					LOGGER.trace("  {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
				} else {
					LOGGER.trace("  {} not applicable for object {}, none of the object specifications match (breaking evaluation)", 
							autzHumanReadableDesc, object);
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
					
			}
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}", 
					principal, object, objectSecurityConstraints.debugDump());
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
		LOGGER.trace("AUTZ: evaluating search pre-process principal={}, objectType={}: orig filter {}", 
				new Object[]{principal, objectType, origFilter});
		if (origFilter == null) {
			origFilter = AllFilter.createAll();
		}
		ObjectFilter finalFilter;
		if (phase != null) {
			finalFilter = preProcessObjectFilterInternal(principal, operationUrl, phase, 
					true, objectType, object, origFilter);
		} else {
			ObjectFilter filterBoth = preProcessObjectFilterInternal(principal, operationUrl, null, 
					false, objectType, object, origFilter);
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
		Collection<Authorization> authorities = getAuthorities(principal);
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
					List<OwnedObjectSelectorType> objectSpecTypes;
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
						for (OwnedObjectSelectorType objectSpecType: objectSpecTypes) {
							ObjectFilter objSpecSecurityFilter = null;
							TypeFilter objSpecTypeFilter = null;
							SearchFilterType specFilterType = objectSpecType.getFilter();
							ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
							OrgRelationObjectSpecificationType specOrgRelation = objectSpecType.getOrgRelation();
							QName specTypeQName = objectSpecType.getType();
							PrismObjectDefinition<T> objectDefinition = null;
							
							// Type
							if (specTypeQName != null) {
                                specTypeQName = prismContext.getSchemaRegistry().qualifyTypeName(specTypeQName);
								PrismObjectDefinition<?> specObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(specTypeQName);
								if (specObjectDef == null) {
									throw new SchemaException("Unknown object type "+specTypeQName+" in "+autz.getHumanReadableDesc());
								}
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
								if (AbstractRoleType.class.isAssignableFrom(objectType)) {
									if (objectDefinition == null) {
										objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
									}
									ItemPath ownerRefPath = new ItemPath(AbstractRoleType.F_OWNER_REF);
									PrismReferenceDefinition ownerRefDef = objectDefinition.findReferenceDefinition(ownerRefPath);
									S_AtomicFilterExit builder = QueryBuilder.queryFor(AbstractRoleType.class, prismContext)
											.item(ownerRefPath, ownerRefDef).ref(principal.getUser().getOid());
									// TODO don't understand this code
									for (ObjectReferenceType subjectParentOrgRef: principal.getUser().getParentOrgRef()) {
										if (ObjectTypeUtil.isDefaultRelation(subjectParentOrgRef.getRelation())) {
											builder = builder.or().item(ownerRefPath, ownerRefDef).ref(subjectParentOrgRef.getOid());
										}
									}
									ObjectFilter objSpecOwnerFilter = builder.buildFilter();
									objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOwnerFilter);
									LOGGER.trace("  applying owner filter {}", objSpecOwnerFilter);
								} else {
									LOGGER.trace("  Authorization not applicable for object because it has owner specification (this is not applicable for search)");
									continue;
								}								
							}
							
							applicable = true;
							
							// Special
							List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
							if (specSpecial != null && !specSpecial.isEmpty()) {
								if (specFilterType != null || specOrgRef != null || specOrgRelation != null) {
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
								LOGGER.trace("  applying property filter {}", specFilter);
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter);
							} else {
								LOGGER.trace("  filter empty");
							}
							
							// Org
							if (specOrgRef != null) {
								ObjectFilter orgFilter = QueryBuilder.queryFor(ObjectType.class, prismContext)
										.isChildOf(specOrgRef.getOid()).buildFilter();
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter);
								LOGGER.trace("  applying org filter {}", orgFilter);
							} else {
								LOGGER.trace("  org empty");
							}
							
							// orgRelation
							if (specOrgRelation != null) {
								ObjectFilter objSpecOrgRelationFilter = null;
								QName subjectRelation = specOrgRelation.getSubjectRelation();
								for (ObjectReferenceType subjectParentOrgRef: principal.getUser().getParentOrgRef()) {
									if (MiscSchemaUtil.compareRelation(subjectRelation, subjectParentOrgRef.getRelation())) {
										S_FilterEntryOrEmpty q = QueryBuilder.queryFor(ObjectType.class, prismContext);
										S_AtomicFilterExit q2;
										if (specOrgRelation.getScope() == null || specOrgRelation.getScope() == OrgScopeType.ALL_DESCENDANTS) {
											q2 = q.isChildOf(subjectParentOrgRef.getOid());
										} else if (specOrgRelation.getScope() == OrgScopeType.DIRECT_DESCENDANTS) {
											q2 = q.isDirectChildOf(subjectParentOrgRef.getOid());
										} else if (specOrgRelation.getScope() == OrgScopeType.ALL_ANCESTORS) {
											q2 = q.isParentOf(subjectParentOrgRef.getOid());
										} else {
											throw new UnsupportedOperationException("Unknown orgRelation scope "+specOrgRelation.getScope());
										}
										if (BooleanUtils.isTrue(specOrgRelation.isIncludeReferenceOrg())) {
											q2 = q2.or().id(subjectParentOrgRef.getOid());
										}
										objSpecOrgRelationFilter = ObjectQueryUtil.filterOr(objSpecOrgRelationFilter, q2.buildFilter());
									}
								}
								if (objSpecOrgRelationFilter == null) {
									objSpecOrgRelationFilter = NoneFilter.createNone();
								}
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecOrgRelationFilter);
								LOGGER.trace("  applying orgRelation filter {}", objSpecOrgRelationFilter);
							} else {
								LOGGER.trace("  orgRelation empty");
							}
							
							if (objSpecTypeFilter != null) {
								objSpecTypeFilter.setFilter(objSpecSecurityFilter);
								objSpecSecurityFilter = objSpecTypeFilter;
							}
							
							traceFilter("objSpecSecurityFilter", objectSpecType, objSpecSecurityFilter);
							autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter);
						}
					} else {
						LOGGER.trace("  No object specification in authorization (authorization is universaly applicable)");
						autzObjSecurityFilter = AllFilter.createAll();
					}
					
					traceFilter("autzObjSecurityFilter", autz, autzObjSecurityFilter);
					
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
							if (autz.getItem() != null && !autz.getItem().isEmpty()) {
								// This is a tricky situation. We have deny authorization, but it only denies access to
								// some items. Therefore we need to find the objects and then filter out the items.
								// Therefore do not add this authorization into the filter.
							} else {
								if (ObjectQueryUtil.isAll(autzObjSecurityFilter)) {
									// This is "deny all". We cannot have anything stronger than that.
									// There is no point in continuing the evaluation.
									LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: deny all", new Object[]{getUsername(principal), operationUrl});
									NoneFilter secFilter = NoneFilter.createNone();
									traceFilter("secFilter", null, secFilter);
									return secFilter;
								}
								securityFilterDeny = ObjectQueryUtil.filterOr(securityFilterDeny, autzObjSecurityFilter);
							}
						}
					}
					
					traceFilter("securityFilterAllow", autz, securityFilterAllow);
					traceFilter("securityFilterDeny", autz, securityFilterDeny);
					
				} else {
					LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), getUsername(principal));
				}
			}
		}
		
		traceFilter("securityFilterAllow", null, securityFilterAllow);
		traceFilter("securityFilterDeny", null, securityFilterDeny);
		
		ObjectFilter origWithAllowFilter;
		if (hasAllowAll) {
			origWithAllowFilter = origFilter;
		} else if (securityFilterAllow == null) {
			// Nothing has been allowed. This means default deny.
			LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: default deny",  new Object[]{getUsername(principal), operationUrl});
			NoneFilter secFilter = NoneFilter.createNone();
			traceFilter("secFilter", null, secFilter);
			return secFilter;
		} else {
			origWithAllowFilter = ObjectQueryUtil.filterAnd(origFilter, securityFilterAllow);
		}

		if (securityFilterDeny == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow:\n{}", 
					new Object[]{getUsername(principal), operationUrl, origWithAllowFilter==null?"null":origWithAllowFilter.debugDump()});
			}
			traceFilter("origWithAllowFilter", null, origWithAllowFilter);
			return origWithAllowFilter;
		} else {
			ObjectFilter secFilter = ObjectQueryUtil.filterAnd(origWithAllowFilter, NotFilter.createNot(securityFilterDeny));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ search pre-process: principal={}, operation={}: allow (with deny clauses):\n{}", 
					new Object[]{getUsername(principal), operationUrl, secFilter==null?"null":secFilter.debugDump()});
			}
			traceFilter("secFilter", null, secFilter);
			return secFilter;
		}
	}

	private void traceFilter(String message, Object forObj, ObjectFilter filter) {
		if (FILTER_TRACE_ENABLED) {
			LOGGER.trace("FILTER {} for {}:\n{}", message, forObj, filter==null?null:filter.debugDump(1));
		}
		
	}

	private Object getUsername(MidPointPrincipal principal) {
		return principal==null?null:principal.getUsername();
	}

	@Override
	public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityDecisions getAllowedRequestAssignmentItems( MidPointPrincipal midPointPrincipal, PrismObject<O> object, PrismObject<R> target, OwnerResolver ownerResolver) throws SchemaException {
		
		ItemSecurityDecisions decisions = new ItemSecurityDecisions();
		
		for(Authorization autz: getAuthorities(midPointPrincipal)) {
			String autzHumanReadableDesc = autz.getHumanReadableDesc();
			LOGGER.trace("Evaluating {}", autzHumanReadableDesc);
			
			// First check if the authorization is applicable.
			
			// action
			if (!autz.getAction().contains(AuthorizationConstants.AUTZ_UI_ASSIGN_ACTION_URL) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
				LOGGER.trace("  {} not applicable for operation {}", autzHumanReadableDesc, AuthorizationConstants.AUTZ_UI_ASSIGN_ACTION_URL);
				continue;
			}
			
			// phase
			if (autz.getPhase() != null && autz.getPhase() != AuthorizationPhaseType.REQUEST) {
				LOGGER.trace("  {} is not applicable for phase {} (breaking evaluation)", autzHumanReadableDesc, AuthorizationPhaseType.REQUEST);
				continue;
			}
						
			// object
			if (isApplicable(autz.getObject(), object, midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc)) {
				LOGGER.trace("  {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
			} else {
				LOGGER.trace("  {} not applicable for object {}, none of the object specifications match (breaking evaluation)", 
						autzHumanReadableDesc, object);
				continue;
			}
			
			// target
			if (isApplicable(autz.getTarget(), target, midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc)) {
				LOGGER.trace("  {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, object);
			} else {
				LOGGER.trace("  {} not applicable for target {}, none of the target specifications match (breaking evaluation)", 
						autzHumanReadableDesc, object);
				continue;
			}
			
			// authority is applicable to this situation. now we can process the decision.
			AuthorizationDecisionType decision = autz.getDecision();
			if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
				Collection<ItemPath> items = getItems(autz);
				if (items.isEmpty()) {
					LOGGER.trace("  {}: ALLOW all items (but continue evaluation)", autzHumanReadableDesc);
					if (decisions.getDefaultDecision() != AuthorizationDecisionType.DENY) {
						decisions.setDefaultDecision(AuthorizationDecisionType.ALLOW);
					}
				} else {
					for(ItemPath item: items) {
						LOGGER.trace("  {}: ALLOW item {} (but continue evaluation)", autzHumanReadableDesc, item);
						if (decisions.getItemDecisionMap().get(item) != AuthorizationDecisionType.DENY) {
							decisions.getItemDecisionMap().put(item, AuthorizationDecisionType.ALLOW);
						}
					}
				}
			} else {
				Collection<ItemPath> items = getItems(autz);
				if (items.isEmpty()) {
					LOGGER.trace("  {}: DENY all items (breaking evaluation)", autzHumanReadableDesc);
					// Total deny. Reset everything. Return just deny
					decisions = new ItemSecurityDecisions();
					decisions.setDefaultDecision(AuthorizationDecisionType.DENY);
					break;
				} else {
					for(ItemPath item: items) {
						LOGGER.trace("  {}: DENY item {} (but continue evaluation)", autzHumanReadableDesc, item);
						decisions.getItemDecisionMap().put(item, AuthorizationDecisionType.DENY);
					}
				}
			}

		}
		
		return decisions;
	}

	@Override
	public <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException {

		LOGGER.debug("Running {} as {}", producer, user);
		Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
		setupPreAuthenticatedSecurityContext(user);
		try {
			return producer.run();
		} finally {
			SecurityContextHolder.getContext().setAuthentication(origAuthentication);
			LOGGER.debug("Finished running {} as {}", producer, user);
		}
	}

	@Override
	public <T> T runPrivileged(Producer<T> producer) {
		LOGGER.debug("Running {} as privileged", producer);
		Authentication origAuthentication = SecurityContextHolder.getContext().getAuthentication();
		LOGGER.trace("ORIG auth {}", origAuthentication);
		
		// Try to reuse the original identity as much as possible. All we need to is add AUTZ_ALL
		// to the list of authorities
		Authorization privilegedAuthorization = createPrivilegedAuthorization();
		Object newPrincipal = null;
		
		if (origAuthentication != null) {
			Object origPrincipal = origAuthentication.getPrincipal();
			if (origAuthentication instanceof AnonymousAuthenticationToken) {
				newPrincipal = origPrincipal;
			} else {
				LOGGER.trace("ORIG principal {} ({})", origPrincipal, origPrincipal.getClass());
				if (origPrincipal != null) {
					if (origPrincipal instanceof MidPointPrincipal) {
						MidPointPrincipal newMidPointPrincipal = ((MidPointPrincipal)origPrincipal).clone();
						newMidPointPrincipal.getAuthorities().add(privilegedAuthorization);
					}
				}
			}
			
			Collection newAuthorities = new ArrayList<>(); 
			newAuthorities.addAll(origAuthentication.getAuthorities());
			newAuthorities.add(privilegedAuthorization);
			PreAuthenticatedAuthenticationToken newAuthorization = new PreAuthenticatedAuthenticationToken(newPrincipal, null, newAuthorities);
			
			LOGGER.trace("NEW auth {}", newAuthorization);
			SecurityContextHolder.getContext().setAuthentication(newAuthorization);
		} else {
			LOGGER.debug("No original authentication, do NOT setting any privileged security context");
		}
		
		
		try {
			return producer.run();
		} finally {
			SecurityContextHolder.getContext().setAuthentication(origAuthentication);
			LOGGER.debug("Finished running {} as privileged", producer);
			LOGGER.trace("Security context after privileged operation: {}", SecurityContextHolder.getContext());
		}
		
	}
	
	
	private Authorization createPrivilegedAuthorization() {
		AuthorizationType authorizationType = new AuthorizationType();
		authorizationType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
		return new Authorization(authorizationType);
	}
	
}

	
