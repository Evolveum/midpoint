/*
 * Copyright (c) 2014-2018 Evolveum
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
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AccessDecision;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgRelationObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleRelationObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	@Autowired private TaskManager taskManager;
	@Autowired private ExpressionFactory expressionFactory;
	@Autowired private PrismContext prismContext;
	
	@Autowired
	@Qualifier("securityContextManager")
	private SecurityContextManager securityContextManager;

	@Override
	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		AccessDecision decision = isAuthorizedInternal(getMidPointPrincipal(), operationUrl, phase, params, ownerResolver, null, task, result);
		return decision.equals(AccessDecision.ALLOW);
	}
	
	private <O extends ObjectType, T extends ObjectType> AccessDecision isAuthorizedInternal(MidPointPrincipal midPointPrincipal, String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, 
			Consumer<Authorization> applicableAutzConsumer, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (phase == null) {
			AccessDecision requestPhaseDecision = isAuthorizedPhase(midPointPrincipal, operationUrl, AuthorizationPhaseType.REQUEST, params, ownerResolver, applicableAutzConsumer, task, result);
			if (!requestPhaseDecision.equals(AccessDecision.ALLOW)) {
				return requestPhaseDecision;
			}
			return isAuthorizedPhase(midPointPrincipal, operationUrl, AuthorizationPhaseType.EXECUTION, params, ownerResolver, applicableAutzConsumer, task, result);
		} else {
			return isAuthorizedPhase(midPointPrincipal, operationUrl, phase, params, ownerResolver, applicableAutzConsumer, task, result);
		}
	}

	private <O extends ObjectType, T extends ObjectType> AccessDecision isAuthorizedPhase(MidPointPrincipal midPointPrincipal, String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver, 
			Consumer<Authorization> applicableAutzConsumer, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(operationUrl)) {
			return AccessDecision.DENY;
		}

		if (phase == null) {
			throw new IllegalArgumentException("No phase");
		}
		AccessDecision decision = AccessDecision.DEFAULT;
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluating authorization principal={}, op={}, phase={}, {}",
				getUsername(midPointPrincipal), operationUrl, phase, params.shortDump());
		}
		final AutzItemPaths allowedItems = new AutzItemPaths();
		Collection<Authorization> authorities = getAuthorities(midPointPrincipal);
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					String autzHumanReadableDesc = autz.getHumanReadableDesc();
					LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

					// First check if the authorization is applicable.

					// action
					if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
						LOGGER.trace("    {} not applicable for operation {}", autzHumanReadableDesc, operationUrl);
						continue;
					}

					// phase
					if (autz.getPhase() == null) {
						LOGGER.trace("    {} is applicable for all phases (continuing evaluation)", autzHumanReadableDesc);
					} else {
						if (autz.getPhase() != phase) {
							LOGGER.trace("    {} is not applicable for phases {} (breaking evaluation)", autzHumanReadableDesc, phase);
							continue;
						} else {
							LOGGER.trace("    {} is applicable for phases {} (continuing evaluation)", autzHumanReadableDesc, phase);
						}
					}
					
					// relation
					if (!isApplicableRelation(autz, params.getRelation())) {
						LOGGER.trace("    {} not applicable for relation {}", autzHumanReadableDesc, params.getRelation());
						continue;
					}

					// object
					if (isApplicable(autz.getObject(), params.getObject(), midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
						LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, params.getObject());
					} else {
						LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
								autzHumanReadableDesc, params.getObject());
						continue;
					}

					// target
					if (isApplicable(autz.getTarget(), params.getTarget(), midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc, task, result)) {
						LOGGER.trace("    {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, params.getObject());
					} else {
						LOGGER.trace("    {} not applicable for target {}, none of the target specifications match (breaking evaluation)",
								autzHumanReadableDesc, params.getObject());
						continue;
					}
					
					if (applicableAutzConsumer != null) {
						applicableAutzConsumer.accept(autz);
					}

					// authority is applicable to this situation. now we can process the decision.
					AuthorizationDecisionType autzDecision = autz.getDecision();
					if (autzDecision == null || autzDecision.equals(AuthorizationDecisionType.ALLOW)) {
						allowedItems.collectItems(autz);
						LOGGER.trace("    {}: ALLOW operation {} (but continue evaluation)", autzHumanReadableDesc, operationUrl);
						decision = AccessDecision.ALLOW;
						// Do NOT break here. Other authorization statements may still deny the operation
					} else {
						// item
						if (isApplicableItem(autz, params.getObject(), params.getDelta())) {
							LOGGER.trace("    {}: Deny authorization applicable for items (continuing evaluation)", autzHumanReadableDesc);
						} else {
							LOGGER.trace("    {} not applicable for items (breaking evaluation)", autzHumanReadableDesc);
							continue;
						}
						LOGGER.trace("    {}: DENY operation {}", autzHumanReadableDesc, operationUrl);
						decision = AccessDecision.DENY;
						// Break right here. Deny cannot be overridden by allow. This decision cannot be changed.
						break;
					}

				} else {
					LOGGER.warn("Unknown authority type {} in user {}", authority.getClass(), getUsername(midPointPrincipal));
				}
			}
		}

		if (decision.equals(AccessDecision.ALLOW)) {
			// Still check allowedItems. We may still deny the operation.
			if (allowedItems.isAllItems()) {
				// This means all items are allowed. No need to check anything
				LOGGER.trace("  Empty list of allowed items, operation allowed");
			} else {
				// all items in the object and delta must be allowed
				LOGGER.trace("  Checking for allowed items: {}", allowedItems);

				ItemDecisionFunction itemDecisionFunction = (itemPath, removingContainer) -> decideAllowedItems(itemPath,  allowedItems, phase, removingContainer);
				AccessDecision itemsDecision = null;
				if (params.hasDelta()) {
					itemsDecision = determineDeltaDecision(params.getDelta(), params.getObject(), itemDecisionFunction);
				} else if (params.hasObject()) {
					itemsDecision = determineObjectDecision(params.getObject(), itemDecisionFunction);
				}
				if (itemsDecision != AccessDecision.ALLOW) {
					LOGGER.trace("    NOT ALLOWED operation because the item decision is {}", itemsDecision);
					decision = AccessDecision.DEFAULT;
				}
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ result: principal={}, operation={}: {}",
					getUsername(midPointPrincipal), prettyActionUrl(operationUrl), decision);
		}
		return decision;
	}
	
	private AccessDecision decideAllowedItems(final ItemPath itemPath, final AutzItemPaths allowedItems, final AuthorizationPhaseType phase, final boolean removingContainer) {
		if (isAllowedItem(itemPath, allowedItems, phase, removingContainer)) {
			return AccessDecision.ALLOW;
		} else {
			return AccessDecision.DEFAULT;
		}
	}

	private <O extends ObjectType> AccessDecision determineObjectDecision(PrismContainer<O> object, ItemDecisionFunction itemDecitionFunction) {
		AccessDecision containerDecision = determineContainerDecision(object.getValue(), itemDecitionFunction, false, "object");
		if (containerDecision == null && object.isEmpty()) {
			// There are no items in the object. Therefore there is no item that is allowed. Therefore decision is DEFAULT.
			// But also there is no item that is denied or not allowed. 
			// This is a corner case. But this approach is often used by GUI to determine if
			// a specific class of object is allowed, e.g. if it is allowed to create (some) roles. This is used to
			// determine whether to display a particular menu item.
			// Therefore we should allow such cases.
			return AccessDecision.ALLOW;
		}
		return containerDecision;
	}

	private <C extends Containerable, O extends ObjectType> AccessDecision determineContainerDeltaDecision(ContainerDelta<C> cdelta, PrismObject<O> currentObject, ItemDecisionFunction itemDecitionFunction) {
		AccessDecision decision = null;
		ItemPath path = cdelta.getPath();
		
		// Everything is plain and simple for add. No need for any additional checks.
		Collection<PrismContainerValue<C>> valuesToAdd = cdelta.getValuesToAdd();
		if (valuesToAdd != null) {
			for (PrismContainerValue<C> cval: valuesToAdd) {
				AccessDecision subdecision = determineContainerDecision(cval, itemDecitionFunction, false, "delta add");
				decision = AccessDecision.combine(decision, subdecision);
			}
		}
		
		// For deleted container values watch out for id-only deltas. Those deltas do not have
		// any subitems in them. So we need to use data from currentObject for autz evaluation.
		Collection<PrismContainerValue<C>> valuesToDelete = cdelta.getValuesToDelete();
		if (valuesToDelete != null) {
			for (PrismContainerValue<C> cval: valuesToDelete) {
				AccessDecision subdecision = null;
				if (cval.isIdOnly()) {
					PrismContainerValue<C> currentObjectCval = determineContainerValueFromCurrentObject(path, cval.getId(), currentObject);
					if (currentObjectCval != null) {
						subdecision = determineContainerDecision(currentObjectCval, itemDecitionFunction, true, "delta delete (current value)");
					}
				} else {
					subdecision = determineContainerDecision(cval, itemDecitionFunction, true, "delta delete");
				}
				if (subdecision != null) {
					decision = AccessDecision.combine(decision, subdecision);
				}
			}
		}
		
		// Values to replace should pass the ordinary check. But we also need to check old values
		// in currentObject, because those values are efficiently deleted.
		Collection<PrismContainerValue<C>> valuesToReplace = cdelta.getValuesToReplace();
		if (valuesToReplace != null) {
			for (PrismContainerValue<C> cval: valuesToReplace) {
				AccessDecision subdecision = determineContainerDecision(cval, itemDecitionFunction, false, "delta replace");
				decision = AccessDecision.combine(decision, subdecision);
			}
			Collection<PrismContainerValue<C>> oldCvals = determineContainerValuesFromCurrentObject(path, currentObject);
			if (oldCvals != null) {
				for (PrismContainerValue<C> cval: oldCvals) {
					AccessDecision subdecision = determineContainerDecision(cval, itemDecitionFunction, true, "delta replace (removed current value)");
					decision = AccessDecision.combine(decision, subdecision);
				}
			}
		}
		
		return decision;
	}

	private <C extends Containerable> void logSubitemContainerDecision(AccessDecision subdecision, String location, PrismContainerValue<C> cval) {
		if (LOGGER.isTraceEnabled()) {
			if (subdecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuhotizationLog()) {
				LOGGER.trace("    container {} for {} (processed subitems): decision={}", cval.getPath(), location, subdecision);
			}
		}
	}
	
	private <C extends Containerable> void logSubitemDecision(AccessDecision subdecision, String location, ItemPath path) {
		if (LOGGER.isTraceEnabled()) {
			if (subdecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuhotizationLog()) {
				LOGGER.trace("    item {} for {}: decision={}", path, location, subdecision);
			}
		}
	}

	private <C extends Containerable, O extends ObjectType> PrismContainerValue<C> determineContainerValueFromCurrentObject(ItemPath path,
			long id, PrismObject<O> currentObject) {
		Collection<PrismContainerValue<C>> oldCvals = determineContainerValuesFromCurrentObject(path, currentObject);
		if (oldCvals == null) {
			return null;
		}
		for (PrismContainerValue<C> oldCval: oldCvals) {
			if (id == oldCval.getId()) {
				return oldCval;
			}
		}
		return null;
	}

	private <C extends Containerable, O extends ObjectType> Collection<PrismContainerValue<C>> determineContainerValuesFromCurrentObject(ItemPath path,
			PrismObject<O> currentObject) {
		PrismContainer<C> container = currentObject.findContainer(path);
		if (container == null) {
			return null;
		}
		return container.getValues();
	}

	private AccessDecision determineContainerDecision(PrismContainerValue<?> cval, ItemDecisionFunction itemDecitionFunction, boolean removingContainer, String decisionContextDesc) {
		List<Item<?,?>> items = cval.getItems();
		// Note: cval.isEmpty() will also check for id. We do not care about that.
		if (items == null || items.isEmpty()) {
			// TODO: problem with empty containers such as
			// orderConstraint in assignment. Skip all
			// empty items ... for now.
			logSubitemContainerDecision(null, decisionContextDesc, cval);
			return null;
		}
		AccessDecision decision = null;
		for (Item<?, ?> item: items) {
			ItemPath itemPath = item.getPath();
			AccessDecision itemDecision = itemDecitionFunction.decide(itemPath.namedSegmentsOnly(), removingContainer);
			logSubitemDecision(itemDecision, decisionContextDesc, itemPath);
			if (itemDecision == null) {
				// null decision means: skip this
				continue;
			}
			if (itemDecision == AccessDecision.DEFAULT && item instanceof PrismContainer<?>) {
				// No decision for entire container. Subitems will dictate the decision.
				List<PrismContainerValue<?>> subValues = (List)((PrismContainer<?>)item).getValues();
				AccessDecision containerDecision = null;
				for (PrismContainerValue<?> subValue: subValues) {
					AccessDecision subdecision = determineContainerDecision(subValue, itemDecitionFunction, removingContainer, decisionContextDesc);
					containerDecision = AccessDecision.combine(containerDecision, subdecision);
					// We do not want to break the loop immediately here. We want all the denied items to get logged
				}
				if (containerDecision == null) {
					// All items that were inside all the container values are ignored (e.g. metadata).
					// This is efficiently the same situation as an empty container.
					// So just ignore it.
					continue;
				} else {
					decision = AccessDecision.combine(decision, containerDecision);
				}
			} else {
				if (itemDecision == AccessDecision.DENY) {
					LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
					// We do not want to break the loop immediately here. We want all the denied items to get logged
				}
				decision = AccessDecision.combine(decision, itemDecision);
			}
		}
		logSubitemContainerDecision(decision, decisionContextDesc, cval);
		return decision;
	}

	/**
	 * The currentObject parameter is the state of the object as we have seen it (the more recent the better).
	 * This is used to check authorization for id-only delete deltas and replace deltas for containers.
	 */
	private <O extends ObjectType> AccessDecision determineDeltaDecision(ObjectDelta<O> delta, PrismObject<O> currentObject, ItemDecisionFunction itemDecisionFunction) {
		if (delta.isAdd()) {
			return determineObjectDecision(delta.getObjectToAdd(), itemDecisionFunction);
		} else {
			AccessDecision decision = null;
			for (ItemDelta<?,?> itemDelta: delta.getModifications()) {
				ItemPath itemPath = itemDelta.getPath();
				AccessDecision itemDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), false);
				if (itemDecision == null) {
					// null decision means: skip this
					continue;
				}
				if (itemDecision == AccessDecision.DEFAULT && itemDelta instanceof ContainerDelta<?>) {
					// No decision for entire container. Subitems will dictate the decision.
					AccessDecision subdecision = determineContainerDeltaDecision((ContainerDelta<?>)itemDelta, currentObject, itemDecisionFunction);
					decision = AccessDecision.combine(decision, subdecision);
				} else {
					if (itemDecision == AccessDecision.DENY) {
						LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
						// We do not want to break the loop immediately here. We want all the denied items to get logged
					}
					decision = AccessDecision.combine(decision, itemDecision);
				}
			}
			return decision;
		}
	}
	
	private boolean isAllowedItem(ItemPath nameOnlyItemPath, AutzItemPaths allowedItems, AuthorizationPhaseType phase, boolean removingContainer) {
		if (removingContainer && isInList(nameOnlyItemPath, AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE)) {
			return true;
		}
		if (AuthorizationPhaseType.EXECUTION.equals(phase) && isInList(nameOnlyItemPath, AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)) {
			return true;
		}
		return allowedItems.isApplicable(nameOnlyItemPath);
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
			AuthorizationParameters<O,T> params, OwnerResolver ownerResolver,
			Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		boolean allow = isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
		if (!allow) {
			failAuthorization(operationUrl, phase, params, result);
		}
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void failAuthorization(String operationUrl, AuthorizationPhaseType phase,
			AuthorizationParameters<O,T> params, OperationResult result) throws SecurityViolationException {
		MidPointPrincipal principal = securityContextManager.getPrincipal();
		String username = getQuotedUsername(principal);
		String message;
		if (params.getTarget() == null && params.getObject() == null) {
			message = "User '"+username+"' not authorized for operation "+ operationUrl;
		} else if (params.getTarget() == null) {
			message = "User '"+username+"' not authorized for operation "+ operationUrl + " on " + params.getObject();
		} else {
			message = "User '"+username+"' not authorized for operation "+ operationUrl + " on " + params.getObject() + " with target " + params.getTarget();
		}
		LOGGER.error("{}", message);
		AuthorizationException e = new AuthorizationException(message);
		result.recordFatalError(e.getMessage(), e);
		throw e;
	}

	private <O extends ObjectType> boolean isApplicable(List<OwnedObjectSelectorType> objectSpecTypes, PrismObject<O> object,
			MidPointPrincipal midPointPrincipal, OwnerResolver ownerResolver, String desc, String autzHumanReadableDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (objectSpecTypes != null && !objectSpecTypes.isEmpty()) {
			if (object == null) {
				LOGGER.trace("  {} not applicable for null {}", autzHumanReadableDesc, desc);
				return false;
			}
			for (OwnedObjectSelectorType autzObject: objectSpecTypes) {
				if (isApplicable(autzObject, object, midPointPrincipal, ownerResolver, desc, autzHumanReadableDesc, task, result)) {
					return true;
				}
			}
			return false;
		} else {
			LOGGER.trace("    {}: No {} specification in authorization (authorization is applicable)", autzHumanReadableDesc, desc);
			return true;
		}
	}

	private <O extends ObjectType> boolean isApplicable(SubjectedObjectSelectorType objectSelector, PrismObject<O> object,
			MidPointPrincipal principal, OwnerResolver ownerResolver, String desc, String autzHumanReadableDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		ObjectFilterExpressionEvaluator filterExpressionEvaluator = createFilterEvaluator(principal, desc, autzHumanReadableDesc, task, result);
		if (!repositoryService.selectorMatches(objectSelector, object, filterExpressionEvaluator, LOGGER, "    " + autzHumanReadableDesc + " not applicable for " + desc + " because of")) {
			return false;
		}

		OrgRelationObjectSpecificationType specOrgRelation = objectSelector.getOrgRelation();
		RoleRelationObjectSpecificationType specRoleRelation = objectSelector.getRoleRelation();

		// Special
		List<SpecialObjectSpecificationType> specSpecial = objectSelector.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (objectSelector.getFilter() != null || objectSelector.getOrgRef() != null || specOrgRelation != null || specRoleRelation != null) {
				throw new SchemaException("Both filter/org/role and special "+desc+" specification specified in "+autzHumanReadableDesc);
			}
			for (SpecialObjectSpecificationType special: specSpecial) {
				if (special == SpecialObjectSpecificationType.SELF) {
					String principalOid = principal != null ? principal.getOid() : null;
					if (principalOid == null) {
						// This is a rare case. It should not normally happen. But it may happen in tests
						// or during initial import. Therefore we are not going to die here. Just ignore it.
					} else {
						if (principalOid.equals(object.getOid())) {
							LOGGER.trace("    {}: 'self' authorization applicable for {}", autzHumanReadableDesc, desc);
							return true;
						} else {
							LOGGER.trace("    {}: 'self' authorization not applicable for {}, principal OID: {}, {} OID {}",
									autzHumanReadableDesc, desc, principalOid, desc, object.getOid());
						}
					}
				} else {
					throw new SchemaException("Unsupported special "+desc+" specification specified in "+autzHumanReadableDesc+": "+special);
				}
			}
			return false;
		} else {
			LOGGER.trace("    {}: specials empty: {}", autzHumanReadableDesc, specSpecial);
		}

		// orgRelation
		if (specOrgRelation != null) {
			boolean match = false;
			for (ObjectReferenceType subjectParentOrgRef: principal.getUser().getParentOrgRef()) {
				if (matchesOrgRelation(object, subjectParentOrgRef, specOrgRelation, autzHumanReadableDesc, desc)) {
					LOGGER.trace("    org {} applicable for {}, object OID {} because subject org {} matches",
							autzHumanReadableDesc, desc, object.getOid(), subjectParentOrgRef.getOid());
					match = true;
					break;
				}
			}
			if (!match) {
				LOGGER.trace("    org {} not applicable for {}, object OID {} because none of the subject orgs matches",
						autzHumanReadableDesc, desc, object.getOid());
				return false;
			}
		}

		// roleRelation
		if (specRoleRelation != null) {
			boolean match = false;
			for (ObjectReferenceType subjectRoleMembershipRef: principal.getUser().getRoleMembershipRef()) {
				if (matchesRoleRelation(object, subjectRoleMembershipRef, specRoleRelation, autzHumanReadableDesc, desc)) {
					LOGGER.trace("    {} applicable for {}, object OID {} because subject role relation {} matches",
							autzHumanReadableDesc, desc, object.getOid(), subjectRoleMembershipRef.getOid());
					match = true;
					break;
				}
			}
			if (!match) {
				LOGGER.trace("    {} not applicable for {}, object OID {} because none of the subject roles matches",
						autzHumanReadableDesc, desc, object.getOid());
				return false;
			}
		}

		if (objectSelector instanceof OwnedObjectSelectorType) {
			// Owner
			SubjectedObjectSelectorType ownerSpec = ((OwnedObjectSelectorType)objectSelector).getOwner();
			if (ownerSpec != null) {
				if (ownerResolver == null) {
					ownerResolver = securityContextManager.getUserProfileService();
					if (ownerResolver == null) {
						LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because there is no owner resolver",
								autzHumanReadableDesc, desc, object.getOid());
						return false;
					}
				}
				PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object);
				if (owner == null) {
					LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because it has no owner",
							autzHumanReadableDesc, desc, object.getOid());
					return false;
				}
				boolean ownerApplicable = isApplicable(ownerSpec, owner, principal, ownerResolver, "owner of "+desc, autzHumanReadableDesc, task, result);
				if (!ownerApplicable) {
					LOGGER.trace("    {}: owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
							autzHumanReadableDesc, desc, object.getOid(), owner);
					return false;
				}
			}

			// Delegator
			SubjectedObjectSelectorType delegatorSpec = ((OwnedObjectSelectorType)objectSelector).getDelegator();
			if (delegatorSpec != null) {
				if (!isSelf(delegatorSpec)) {
					throw new SchemaException("Unsupported non-self delegator clause");
				}
				if (!object.canRepresent(UserType.class)) {
					LOGGER.trace("    {}: delegator object spec not applicable for {}, because the object is not user",
							autzHumanReadableDesc, desc);
					return false;
				}
				boolean found = false;
				for (ObjectReferenceType objectDelegatedRef: ((UserType)object.asObjectable()).getDelegatedRef()) {
					if (principal.getOid().equals(objectDelegatedRef.getOid())) {
						found = true;
						break;
					}
				}
				if (!found) {
					if (BooleanUtils.isTrue(delegatorSpec.isAllowInactive())) {
						for (AssignmentType objectAssignment: ((UserType)object.asObjectable()).getAssignment()) {
							ObjectReferenceType objectAssignmentTargetRef = objectAssignment.getTargetRef();
							if (objectAssignmentTargetRef == null) {
								continue;
							}
							if (principal.getOid().equals(objectAssignmentTargetRef.getOid())) {
								if (QNameUtil.match(SchemaConstants.ORG_DEPUTY, objectAssignmentTargetRef.getRelation())) {
									found = true;
									break;
								}
							}
						}
					}
					
					if (!found) {
						LOGGER.trace("    {}: delegator object spec not applicable for {}, object OID {} because delegator does not match",
								autzHumanReadableDesc, desc, object.getOid());
						return false;
					}
				}
				
			}
		}

		LOGGER.trace("    {} applicable for {} (filter)", autzHumanReadableDesc, desc);
		return true;
	}

	private ObjectFilterExpressionEvaluator createFilterEvaluator(MidPointPrincipal principal, String objectTargetDesc, String autzHumanReadableDesc, Task task, OperationResult result) {
		return filter -> {
			if (filter == null) {
				return null;
			}
			ExpressionVariables variables = new ExpressionVariables();
			PrismObject<UserType> subject = null;
			if (principal != null) {
				UserType userType = principal.getUser();
				if (userType != null) {
					subject = userType.asPrismObject();
				}
			}
			variables.addVariableDefinition(ExpressionConstants.VAR_SUBJECT, subject);
			return ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext, 
					"expression in " + objectTargetDesc + " in authorization " + autzHumanReadableDesc, task, result);
		};
	}
	
	private <O extends ObjectType> ObjectFilter parseAndEvaluateFilter(MidPointPrincipal principal, PrismObjectDefinition<O> objectDefinition, 
			SearchFilterType specFilterType, String objectTargetDesc, String autzHumanReadableDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		ObjectFilter specFilter = QueryJaxbConvertor.createObjectFilter(objectDefinition, specFilterType, prismContext);
		if (specFilter == null) {
			return null;
		}
		ObjectFilterExpressionEvaluator filterEvaluator = createFilterEvaluator(principal, objectTargetDesc, autzHumanReadableDesc, task, result);
		return filterEvaluator.evaluate(specFilter);
	}


	private boolean isSelf(SubjectedObjectSelectorType spec) throws SchemaException {
		List<SpecialObjectSpecificationType> specSpecial = spec.getSpecial();
		if (specSpecial != null && !specSpecial.isEmpty()) {
			if (spec.getFilter() != null || spec.getOrgRef() != null || spec.getOrgRelation() != null || spec.getRoleRelation() != null) {
				return false;
			}
			for (SpecialObjectSpecificationType special: specSpecial) {
				if (special == SpecialObjectSpecificationType.SELF) {
					return true;
				} else {
					throw new SchemaException("Unsupported special object specification specified in authorization: "+special);
				}
			}
		}
		return false;
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

	private <O extends ObjectType> boolean matchesRoleRelation(PrismObject<O> object, ObjectReferenceType subjectRoleMembershipRef,
			RoleRelationObjectSpecificationType specRoleRelation, String autzHumanReadableDesc, String desc) throws SchemaException {
		if (!MiscSchemaUtil.compareRelation(specRoleRelation.getSubjectRelation(), subjectRoleMembershipRef.getRelation())) {
			return false;
		}
		if (BooleanUtils.isTrue(specRoleRelation.isIncludeReferenceRole()) && subjectRoleMembershipRef.getOid().equals(object.getOid())) {
			return true;
		}
		if (!BooleanUtils.isFalse(specRoleRelation.isIncludeMembers())) {
			if (!object.canRepresent(FocusType.class)) {
				return false;
			}
			for (ObjectReferenceType objectRoleMembershipRef: ((FocusType)object.asObjectable()).getRoleMembershipRef()) {
				if (!subjectRoleMembershipRef.getOid().equals(objectRoleMembershipRef.getOid())) {
					continue;
				}
				if (!MiscSchemaUtil.compareRelation(specRoleRelation.getObjectRelation(), objectRoleMembershipRef.getRelation())) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	private <O extends ObjectType> boolean isApplicableItem(Authorization autz,
			PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
		List<ItemPathType> itemPaths = autz.getItem();
		if (itemPaths == null || itemPaths.isEmpty()) {
			List<ItemPathType> exceptItems = autz.getExceptItem();
			if (exceptItems.isEmpty()) {
				// No item constraints. Applicable for all items.
				LOGGER.trace("  items empty");
				return true;
			} else {
				return isApplicableItem(autz, object, delta, exceptItems, false);
			}
		} else {
			return isApplicableItem(autz, object, delta, itemPaths, true);
		}
		
	}

	private <O extends ObjectType> boolean isApplicableItem(Authorization autz, 
			PrismObject<O> object, ObjectDelta<O> delta, List<ItemPathType> itemPaths, boolean positive) 
					throws SchemaException {
		for (ItemPathType itemPathType: itemPaths) {
			ItemPath itemPath = itemPathType.getItemPath();
			if (delta == null) {
				if (object != null) {
					if (object.containsItem(itemPath, false)) {
						if (positive) {
							LOGGER.trace("  applicable object item "+itemPath);
							return true;
						} else {
							LOGGER.trace("  excluded object item "+itemPath);
							return false;
						}
					}
				}
			} else {
				ItemDelta<?,?> itemDelta = delta.findItemDelta(itemPath);
				if (itemDelta != null && !itemDelta.isEmpty()) {
					if (positive) {
						LOGGER.trace("  applicable delta item "+itemPath);
						return true;
					} else {
						LOGGER.trace("  excluded delta item "+itemPath);
						return false;
					}
				}
			}
		}
		if (positive) {
			LOGGER.trace("  no applicable item");
			return false;
		} else {
			LOGGER.trace("  no excluded item");
			return true;
		}
	}

	/**
	 * Simple access control decision similar to that used by spring security.
	 * It is practically applicable only for simple (non-parametric) cases such as access to GUI pages.
	 * However, it supports authorization hierarchies. Therefore the ordering of elements in
	 * required actions is important.
	 */
	@Override
	public AccessDecision decideAccess(MidPointPrincipal principal, List<String> requiredActions, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		AccessDecision finalDecision = AccessDecision.DEFAULT;
		for(String requiredAction: requiredActions) {
			AccessDecision decision = isAuthorizedInternal(principal, requiredAction, null, AuthorizationParameters.EMPTY, null, null, task, result);
			if (AccessDecision.DENY.equals(decision)) {
				return AccessDecision.DENY;
			}
			if (AccessDecision.ALLOW.equals(decision)) {
				finalDecision = AccessDecision.ALLOW;
			}
		}
		return finalDecision;
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
		return "'"+ principal.getUsername()+"'";
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
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityConstraints(PrismObject<O> object, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (object == null) {
			throw new IllegalArgumentException("Cannot compile security constraints of null object");
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluating security constraints principal={}, object={}", getUsername(principal), object);
		}
		ObjectSecurityConstraintsImpl objectSecurityConstraints = new ObjectSecurityConstraintsImpl();
		Collection<Authorization> authorities = getAuthorities(principal);
		if (authorities != null) {
			for (Authorization autz: authorities) {
				String autzHumanReadableDesc = autz.getHumanReadableDesc();
				LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

				// skip action applicability evaluation. We are interested in all actions

				// object
				if (isApplicable(autz.getObject(), object, principal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
					LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
				} else {
					LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
							autzHumanReadableDesc, object);
					continue;
				}

				// skip target applicability evaluation. We do not have a target here

				objectSecurityConstraints.applyAuthorization(autz);
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluated security constraints principal={}, object={}:\n{}",
					getUsername(principal), object, objectSecurityConstraints.debugDump(1));
		}

		return objectSecurityConstraints;
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<T> searchResultType, PrismObject<O> object, ObjectFilter origFilter, String limitAuthorizationAction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluating search pre-process principal={}, searchResultType={}, object={}: orig filter {}",
				getUsername(principal), searchResultType, object, origFilter);
		}
		if (origFilter == null) {
			origFilter = AllFilter.createAll();
		}
		ObjectFilter finalFilter;
		if (phase != null) {
			finalFilter = preProcessObjectFilterInternal(principal, operationUrl, phase,
					true, searchResultType, object, true, origFilter, limitAuthorizationAction, "search pre-process", task, result);
		} else {
			ObjectFilter filterBoth = preProcessObjectFilterInternal(principal, operationUrl, null,
					false, searchResultType, object, true, origFilter, limitAuthorizationAction, "search pre-process", task, result);
			ObjectFilter filterRequest = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.REQUEST,
					false, searchResultType, object, true, origFilter, limitAuthorizationAction, "search pre-process", task, result);
			ObjectFilter filterExecution = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.EXECUTION,
					false, searchResultType, object, true, origFilter, limitAuthorizationAction, "search pre-process", task, result);
			finalFilter = ObjectQueryUtil.filterOr(filterBoth, ObjectQueryUtil.filterAnd(filterRequest, filterExecution));
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluated search pre-process principal={}, objectType={}: {}", 
				getUsername(principal), getObjectType(searchResultType), finalFilter);
		}
		if (finalFilter instanceof AllFilter) {
			// compatibility
			return null;
		}
		return finalFilter;
	}

	@Override
	public <T extends ObjectType, O extends ObjectType> boolean canSearch(String operationUrl,
			AuthorizationPhaseType phase, Class<T> searchResultType, PrismObject<O> object, boolean includeSpecial, ObjectFilter filter, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		MidPointPrincipal principal = getMidPointPrincipal();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluating search permission principal={}, searchResultType={}, object={}: filter {}",
				getUsername(principal), searchResultType, object, filter);
		}
		if (filter == null) {
			return true;
		}
		ObjectFilter finalFilter;
		if (phase != null) {
			finalFilter = preProcessObjectFilterInternal(principal, operationUrl, phase,
					true, searchResultType, object, includeSpecial, filter, null, "search permission", task, result);
		} else {
			ObjectFilter filterBoth = preProcessObjectFilterInternal(principal, operationUrl, null,
					false, searchResultType, object, includeSpecial, filter, null, "search permission", task, result);
			ObjectFilter filterRequest = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.REQUEST,
					false, searchResultType, object, includeSpecial, filter, null, "search permission", task, result);
			ObjectFilter filterExecution = preProcessObjectFilterInternal(principal, operationUrl, AuthorizationPhaseType.EXECUTION,
					false, searchResultType, object, includeSpecial, filter, null, "search permission", task, result);
			finalFilter = ObjectQueryUtil.filterOr(filterBoth, ObjectQueryUtil.filterAnd(filterRequest, filterExecution));
		}
		finalFilter = ObjectQueryUtil.simplify(finalFilter);
		boolean decision = !(finalFilter instanceof NoneFilter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AUTZ: evaluated search permission principal={}, objectType={}, evulated from filter: {}: decision={}", 
					getUsername(principal), getObjectType(searchResultType), finalFilter, decision);
		}
		return decision;
	}

	private <T extends ObjectType, O extends ObjectType> ObjectFilter preProcessObjectFilterInternal(MidPointPrincipal principal, String operationUrl,
			AuthorizationPhaseType phase, boolean includeNullPhase,
			Class<T> objectType, PrismObject<O> object, boolean includeSpecial, ObjectFilter origFilter, String limitAuthorizationAction, String desc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		Collection<Authorization> authorities = getAuthorities(principal);

		ObjectFilter securityFilterAllow = null;
		ObjectFilter securityFilterDeny = null;

		QueryAutzItemPaths queryItemsSpec = new QueryAutzItemPaths();
		queryItemsSpec.addRequiredItems(origFilter); // MID-3916
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("  phase={}, initial query items spec {}", phase, queryItemsSpec.shortDump());
		}

		boolean hasAllowAll = false;
		if (authorities != null) {
			for (GrantedAuthority authority: authorities) {
				if (authority instanceof Authorization) {
					Authorization autz = (Authorization)authority;
					String autzHumanReadableDesc = autz.getHumanReadableDesc();
					LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

					// action
					if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
						LOGGER.trace("    Authorization not applicable for operation {}", operationUrl);
						continue;
					}

					// phase
					if (autz.getPhase() == phase || (includeNullPhase && autz.getPhase() == null)) {
						LOGGER.trace("    Authorization is applicable for phases {} (continuing evaluation)", phase);
					} else {
						LOGGER.trace("    Authorization is not applicable for phase {}", phase);
						continue;
					}
					
					if (!isApplicableLimitations(autz, limitAuthorizationAction)) {
						LOGGER.trace("    Authorization is limited to other action, not applicable for operation {}", operationUrl);
						continue;
					}

					// object or target
					String objectTargetSpec;
					ObjectFilter autzObjSecurityFilter = null;
					List<OwnedObjectSelectorType> objectSpecTypes;
					if (object == null) {
						// object not present. Therefore we are looking for object here
						objectSpecTypes = autz.getObject();
						objectTargetSpec = "object";
					} else {
						// object present. Therefore we are looking for target
						objectSpecTypes = autz.getTarget();
						objectTargetSpec = "target";

						// .. but we need to decide whether this authorization is applicable to the object
						if (isApplicable(autz.getObject(), object, principal, null, "object", autzHumanReadableDesc, task, result)) {
							LOGGER.trace("    Authorization is applicable for object {}", object);
						} else {
							LOGGER.trace("    Authorization is not applicable for object {}", object);
							continue;
						}
					}

					boolean applicable = true;
					if (objectSpecTypes == null || objectSpecTypes.isEmpty()) {

						LOGGER.trace("    No {} specification in authorization (authorization is universaly applicable)", objectTargetSpec);
						autzObjSecurityFilter = AllFilter.createAll();

					} else {

						applicable = false;
						for (OwnedObjectSelectorType objectSpecType: objectSpecTypes) {
							ObjectFilter objSpecSecurityFilter = null;
							TypeFilter objSpecTypeFilter = null;
							SearchFilterType specFilterType = objectSpecType.getFilter();
							ObjectReferenceType specOrgRef = objectSpecType.getOrgRef();
							OrgRelationObjectSpecificationType specOrgRelation = objectSpecType.getOrgRelation();
							RoleRelationObjectSpecificationType specRoleRelation = objectSpecType.getRoleRelation();
							QName specTypeQName = objectSpecType.getType();
							PrismObjectDefinition<T> objectDefinition = null;

							// Type
							if (specTypeQName != null) {
                                specTypeQName = prismContext.getSchemaRegistry().qualifyTypeName(specTypeQName);
								PrismObjectDefinition<?> specObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(specTypeQName);
								if (specObjectDef == null) {
									throw new SchemaException("Unknown object type "+specTypeQName+" in "+autzHumanReadableDesc);
								}
								Class<?> specObjectClass = specObjectDef.getCompileTimeClass();
								if (!objectType.isAssignableFrom(specObjectClass)) {
									LOGGER.trace("    Authorization not applicable for object because of type mismatch, authorization {}, query {}",
											new Object[]{specObjectClass, objectType});
									continue;
								} else {
									LOGGER.trace("    Authorization is applicable for object because of type match, authorization {}, query {}",
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
								if (objectDefinition == null) {
									objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
								}
								// TODO: MID-3899
								if (AbstractRoleType.class.isAssignableFrom(objectType)) {
									objSpecSecurityFilter = applyOwnerFilterOwnerRef(new ItemPath(AbstractRoleType.F_OWNER_REF), objSpecSecurityFilter,  principal, objectDefinition);
								} else if (TaskType.class.isAssignableFrom(objectType)) {
									objSpecSecurityFilter = applyOwnerFilterOwnerRef(new ItemPath(TaskType.F_OWNER_REF), objSpecSecurityFilter,  principal, objectDefinition);
								} else {
									LOGGER.trace("    Authorization not applicable for object because it has owner specification (this is not applicable for search)");
									continue;
								}
							}

//							// Delegator
//							if (objectSpecType.getDelegator() != null) {
//								if (objectDefinition == null) {
//									objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
//								}
//								// TODO: MID-3899
//								if (UserType.class.isAssignableFrom(objectType)) { TODO
//									objSpecSecurityFilter = applyOwnerFilterOwnerRef(new ItemPath(AbstractRoleType.F_OWNER_REF), objSpecSecurityFilter,  principal, objectDefinition);
//								} else if (TaskType.class.isAssignableFrom(objectType)) {
//									objSpecSecurityFilter = applyOwnerFilterOwnerRef(new ItemPath(TaskType.F_OWNER_REF), objSpecSecurityFilter,  principal, objectDefinition);
//								} else {
//									LOGGER.trace("  Authorization not applicable for object because it has owner specification (this is not applicable for search)");
//									continue;
//								}
//							}

							applicable = true;

							// Special
							List<SpecialObjectSpecificationType> specSpecial = objectSpecType.getSpecial();
							if (specSpecial != null && !specSpecial.isEmpty()) {
								if (!includeSpecial) {
									LOGGER.trace("    Skipping authorization, because specials are present: {}", specSpecial);
									applicable = false;
								}
								if (specFilterType != null || specOrgRef != null || specOrgRelation != null || specRoleRelation != null) {
									throw new SchemaException("Both filter/org/role and special object specification specified in authorization");
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
								LOGGER.trace("    specials empty: {}", specSpecial);
							}

							// Filter
							if (specFilterType != null) {
								if (objectDefinition == null) {
									objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
								}
								ObjectFilter specFilter = parseAndEvaluateFilter(principal, objectDefinition, specFilterType, objectTargetSpec, autzHumanReadableDesc, task, result);
								if (specFilter != null) {
									ObjectQueryUtil.assertNotRaw(specFilter, "Filter in authorization object has undefined items. Maybe a 'type' specification is missing in the authorization?");
									ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in authorization object is not property-only filter");
								}
								LOGGER.trace("    applying property filter {}", specFilter);
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, specFilter);
							} else {
								LOGGER.trace("    filter empty");
							}

							// Org
							if (specOrgRef != null) {
								ObjectFilter orgFilter = QueryBuilder.queryFor(ObjectType.class, prismContext)
										.isChildOf(specOrgRef.getOid()).buildFilter();
								objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, orgFilter);
								LOGGER.trace("    applying org filter {}", orgFilter);
							} else {
								LOGGER.trace("    org empty");
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
								LOGGER.trace("    applying orgRelation filter {}", objSpecOrgRelationFilter);
							} else {
								LOGGER.trace("    orgRelation empty");
							}

							// roleRelation
							if (specRoleRelation != null) {
								ObjectFilter objSpecRoleRelationFilter = processRoleRelationFilter(principal, autz, specRoleRelation, queryItemsSpec, origFilter);
								if (objSpecRoleRelationFilter == null) {
									if (autz.maySkipOnSearch()) {
										LOGGER.trace("    not applying roleRelation filter because it is not efficient and maySkipOnSearch is set", objSpecRoleRelationFilter);
										applicable = false;
									} else {
										objSpecRoleRelationFilter = NoneFilter.createNone();
									}
								}
								if (objSpecRoleRelationFilter != null) {
									objSpecSecurityFilter = ObjectQueryUtil.filterAnd(objSpecSecurityFilter, objSpecRoleRelationFilter);
									LOGGER.trace("  applying roleRelation filter {}", objSpecRoleRelationFilter);
								}
							} else {
								LOGGER.trace("    roleRelation empty");
							}

							if (objSpecTypeFilter != null) {
								objSpecTypeFilter.setFilter(objSpecSecurityFilter);
								objSpecSecurityFilter = objSpecTypeFilter;
							}

							traceFilter("objSpecSecurityFilter", objectSpecType, objSpecSecurityFilter);
							autzObjSecurityFilter = ObjectQueryUtil.filterOr(autzObjSecurityFilter, objSpecSecurityFilter);
						}

					}

					traceFilter("autzObjSecurityFilter", autz, autzObjSecurityFilter);

					if (applicable) {
						autzObjSecurityFilter = ObjectQueryUtil.simplify(autzObjSecurityFilter);
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
							if (!ObjectQueryUtil.isNone(autzObjSecurityFilter)) {
								queryItemsSpec.collectItems(autz);
							}
						} else {
							// deny
							if (autz.hasItemSpecification()) {
								// This is a tricky situation. We have deny authorization, but it only denies access to
								// some items. Therefore we need to find the objects and then filter out the items.
								// Therefore do not add this authorization into the filter.
							} else {
								if (ObjectQueryUtil.isAll(autzObjSecurityFilter)) {
									// This is "deny all". We cannot have anything stronger than that.
									// There is no point in continuing the evaluation.
									if (LOGGER.isTraceEnabled()) {
										LOGGER.trace("AUTZ {} done: principal={}, operation={}, phase={}: deny all",
												desc, getUsername(principal), prettyActionUrl(operationUrl), phase);
									}
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

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(" Final items: {}", queryItemsSpec.shortDump());
		}
		List<ItemPath> unsatisfiedItems = queryItemsSpec.evaluateUnsatisfierItems();
		if (!unsatisfiedItems.isEmpty()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ {} done: principal={}, operation={}, phase={}: deny because items {} are not allowed",
						desc, getUsername(principal), prettyActionUrl(operationUrl), phase, unsatisfiedItems);
			}
			NoneFilter secFilter = NoneFilter.createNone();
			traceFilter("secFilter", null, secFilter);
			return secFilter;
		}

		ObjectFilter origWithAllowFilter;
		if (hasAllowAll) {
			origWithAllowFilter = origFilter;
		} else if (securityFilterAllow == null) {
			// Nothing has been allowed. This means default deny.
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ {} done: principal={}, operation={}, phase={}: default deny", 
						desc, getUsername(principal), prettyActionUrl(operationUrl), phase);
			}
			NoneFilter secFilter = NoneFilter.createNone();
			traceFilter("secFilter", null, secFilter);
			return secFilter;
		} else {
			origWithAllowFilter = ObjectQueryUtil.filterAnd(origFilter, securityFilterAllow);
		}

		if (securityFilterDeny == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ {} done: principal={}, operation={}, phase={}: allow\n  Filter:\n{}", 
						desc, getUsername(principal), prettyActionUrl(operationUrl), phase, 
						origWithAllowFilter==null?"null":origWithAllowFilter.debugDump(2));
			}
			traceFilter("origWithAllowFilter", null, origWithAllowFilter);
			return origWithAllowFilter;
		} else {
			ObjectFilter secFilter = ObjectQueryUtil.filterAnd(origWithAllowFilter, NotFilter.createNot(securityFilterDeny));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("AUTZ {} done: principal={}, operation={}, phase={}: allow (with deny clauses)\n  Filter:\n{}", desc,
						getUsername(principal), prettyActionUrl(operationUrl), phase,
						secFilter==null?"null":secFilter.debugDump(2));
			}
			traceFilter("secFilter", null, secFilter);
			return secFilter;
		}
	}

	private boolean isApplicableLimitations(Authorization autz, String limitAuthorizationAction) {
		if (limitAuthorizationAction == null) {
			return true;
		}
		AuthorizationLimitationsType limitations = autz.getLimitations();
		if (limitations == null) {
			return true;
		}
		List<String> limitationsActions = limitations.getAction();
		if (limitationsActions.isEmpty()) {
			return true;
		}
		return limitationsActions.contains(limitAuthorizationAction);
	}
	
	private boolean isApplicableRelation(Authorization autz, QName requestRelation) {
		List<QName> autzRelation = autz.getRelation();
		if (autzRelation == null || autzRelation.isEmpty()) {
			return true;
		}
		return QNameUtil.contains(autzRelation, requestRelation);
	}

	/**
	 * Very rudimentary and experimental implementation.
	 */
	private ObjectFilter processRoleRelationFilter(MidPointPrincipal principal, Authorization autz,
			RoleRelationObjectSpecificationType specRoleRelation, AutzItemPaths queryItemsSpec, ObjectFilter origFilter) {
		ObjectFilter refRoleFilter = null;
		if (BooleanUtils.isTrue(specRoleRelation.isIncludeReferenceRole())) {
			// This could mean that we will need to add filters for all roles in
			// subject's roleMembershipRef. There may be thousands of these.
			if (!autz.maySkipOnSearch()) {
				throw new UnsupportedOperationException("Inefficient roleRelation search (includeReferenceRole=true) is not supported yet");
			}
		}

		ObjectFilter membersFilter = null;
		if (!BooleanUtils.isFalse(specRoleRelation.isIncludeMembers())) {
			List<PrismReferenceValue> queryRoleRefs = getRoleOidsFromFilter(origFilter);
			if (queryRoleRefs == null || queryRoleRefs.isEmpty()) {
				// Cannot find specific role OID in original query. This could mean that we
				// will need to add filters for all roles in subject's roleMembershipRef.
				// There may be thousands of these.
				if (!autz.maySkipOnSearch()) {
					throw new UnsupportedOperationException("Inefficient roleRelation search (includeMembers=true without role in the original query) is not supported yet");
				}
			} else {
				QName subjectRelation = specRoleRelation.getSubjectRelation();
				boolean isRoleOidOk = false;
				for (ObjectReferenceType subjectRoleMembershipRef: principal.getUser().getRoleMembershipRef()) {
					if (!MiscSchemaUtil.compareRelation(subjectRelation, subjectRoleMembershipRef.getRelation())) {
						continue;
					}
					if (!PrismReferenceValue.containsOid(queryRoleRefs, subjectRoleMembershipRef.getOid())) {
						continue;
					}
					isRoleOidOk = true;
					break;
				}
				if (isRoleOidOk) {
					// There is already a good filter in the origFilter
					// TODO: mind the objectRelation
					membersFilter = AllFilter.createAll();
				} else {
					membersFilter = NoneFilter.createNone();
				}
			}
		}

		return ObjectQueryUtil.filterOr(refRoleFilter, membersFilter);
	}

	private List<PrismReferenceValue> getRoleOidsFromFilter(ObjectFilter origFilter) {
		if (origFilter == null) {
			return null;
		}
		if (origFilter instanceof RefFilter) {
			ItemPath path = ((RefFilter)origFilter).getPath();
			if (path.equals(SchemaConstants.PATH_ROLE_MEMBERSHIP_REF)) {
				return ((RefFilter)origFilter).getValues();
			}
		}
		if (origFilter instanceof AndFilter) {
			for (ObjectFilter condition: ((AndFilter)origFilter).getConditions()) {
				List<PrismReferenceValue> refs = getRoleOidsFromFilter(condition);
				if (refs != null && !refs.isEmpty()) {
					return refs;
				}
			}
		}
		return null;
	}

	private <T extends ObjectType> ObjectFilter applyOwnerFilterOwnerRef(ItemPath ownerRefPath, ObjectFilter objSpecSecurityFilter, MidPointPrincipal principal, PrismObjectDefinition<T> objectDefinition) {
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
		return objSpecSecurityFilter;
	}

	private void traceFilter(String message, Object forObj, ObjectFilter filter) {
		if (FILTER_TRACE_ENABLED) {
			LOGGER.trace("FILTER {} for {}:\n{}", message, forObj, filter==null?null:filter.debugDump(1));
		}

	}

	private String getUsername(MidPointPrincipal principal) {
		return principal==null?null:principal.getUsername();
	}
	
	private String prettyActionUrl(String fullUrl) {
		return DebugUtil.shortenUrl(AuthorizationConstants.NS_SECURITY_PREFIX, fullUrl);
	}
	
	private <O extends ObjectType> String getObjectType(Class<O> type) {
		if (type == null) {
			return null;
		}
		return type.getSimpleName();
	}

	@Override
	public <O extends ObjectType, R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(MidPointPrincipal midPointPrincipal,
			String operationUrl, PrismObject<O> object, PrismObject<R> target, OwnerResolver ownerResolver, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		ItemSecurityConstraintsImpl itemConstraints = new ItemSecurityConstraintsImpl();

		for(Authorization autz: getAuthorities(midPointPrincipal)) {
			String autzHumanReadableDesc = autz.getHumanReadableDesc();
			LOGGER.trace("  Evaluating {}", autzHumanReadableDesc);

			// First check if the authorization is applicable.

			// action
			if (!autz.getAction().contains(operationUrl) && !autz.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
				LOGGER.trace("    {} not applicable for operation {}", autzHumanReadableDesc, operationUrl);
				continue;
			}

			// phase
			if (autz.getPhase() != null && autz.getPhase() != AuthorizationPhaseType.REQUEST) {
				LOGGER.trace("    {} is not applicable for phase {} (breaking evaluation)", autzHumanReadableDesc, AuthorizationPhaseType.REQUEST);
				continue;
			}

			// object
			if (isApplicable(autz.getObject(), object, midPointPrincipal, ownerResolver, "object", autzHumanReadableDesc, task, result)) {
				LOGGER.trace("    {} applicable for object {} (continuing evaluation)", autzHumanReadableDesc, object);
			} else {
				LOGGER.trace("    {} not applicable for object {}, none of the object specifications match (breaking evaluation)",
						autzHumanReadableDesc, object);
				continue;
			}

			// target
			if (isApplicable(autz.getTarget(), target, midPointPrincipal, ownerResolver, "target", autzHumanReadableDesc, task, result)) {
				LOGGER.trace("    {} applicable for target {} (continuing evaluation)", autzHumanReadableDesc, object);
			} else {
				LOGGER.trace("    {} not applicable for target {}, none of the target specifications match (breaking evaluation)",
						autzHumanReadableDesc, object);
				continue;
			}

			// authority is applicable to this situation. now we can process the decision.
			itemConstraints.collectItems(autz);
		}

		return itemConstraints;
	}

	@Override
	public MidPointPrincipal createDonorPrincipal(MidPointPrincipal attorneyPrincipal, String attorneyAuthorizationAction, PrismObject<UserType> donor, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (attorneyPrincipal.getAttorney() != null) {
			throw new UnsupportedOperationException("Transitive attorney is not supported yet");
		}
		
		AuthorizationLimitationsCollector limitationsCollector = new AuthorizationLimitationsCollector();
		AuthorizationParameters<UserType, ObjectType> autzParams = AuthorizationParameters.Builder.buildObject(donor);
		AccessDecision decision = isAuthorizedInternal(attorneyPrincipal, attorneyAuthorizationAction, null, autzParams, null, limitationsCollector, task, result);
		if (!decision.equals(AccessDecision.ALLOW)) {
			failAuthorization(attorneyAuthorizationAction, null, autzParams, result);
		}
		
		MidPointPrincipal donorPrincipal = securityContextManager.getUserProfileService().getPrincipal(donor, limitationsCollector, result);
		donorPrincipal.setAttorney(attorneyPrincipal.getUser());
		
		// chain principals so we can easily drop the power of attorney and return back to original identity
		donorPrincipal.setPreviousPrincipal(attorneyPrincipal);
		
		return donorPrincipal;
	}

	@Override
	public <O extends ObjectType> AccessDecision determineSubitemDecision(
			ObjectSecurityConstraints securityConstraints, ObjectDelta<O> delta, PrismObject<O> currentObject, String operationUrl,
			AuthorizationPhaseType phase, ItemPath subitemRootPath) {
		return determineDeltaDecision(delta, currentObject,
				(nameOnlyItemPath, removingContainer) -> {
					if (removingContainer && isInList(nameOnlyItemPath, AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE)) {
						return null;
					}
					if (AuthorizationPhaseType.EXECUTION.equals(phase) && isInList(nameOnlyItemPath, AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)) {
						return null;
					}
					if (!subitemRootPath.isSubPathOrEquivalent(nameOnlyItemPath)) {
//						LOGGER.trace("subitem decision: {} <=> {} (not under root) : {}", subitemRootPath, nameOnlyItemPath, null);
						return null;
					}
					
					AuthorizationDecisionType authorizationDecisionType = securityConstraints.findItemDecision(nameOnlyItemPath, operationUrl, phase);
					AccessDecision decision = AccessDecision.translate(authorizationDecisionType);
//					LOGGER.trace("subitem decision: {} <=> {} : {}", subitemRootPath, nameOnlyItemPath, decision);
					return decision;
				});
	}
}
