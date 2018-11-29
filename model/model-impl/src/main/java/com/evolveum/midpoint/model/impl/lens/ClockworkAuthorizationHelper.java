/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.AccessDecision;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Component that deals with authorization of requests in clockwork.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ClockworkAuthorizationHelper {
	
	private static final Trace LOGGER = TraceManager.getTrace(ClockworkAuthorizationHelper.class);
	
	@Autowired private SecurityEnforcer securityEnforcer;
	@Autowired private ModelObjectResolver objectResolver;
	@Autowired private RelationRegistry relationRegistry;
	@Autowired private PrismContext prismContext;
	
	public <F extends ObjectType> void authorizeContextRequest(LensContext<F> context, Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		OperationResult result = parentResult.createMinorSubresult(Clockwork.class.getName()+".authorizeRequest");
		LOGGER.trace("Authorizing request for context");
		try {

			final LensFocusContext<F> focusContext = context.getFocusContext();
			OwnerResolver ownerResolver = new LensOwnerResolver<>(context, objectResolver, task, result);
			if (focusContext != null) {
				authorizeElementContext(context, focusContext, ownerResolver, true, task, result);
			}
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				authorizeElementContext(context, projectionContext, ownerResolver, false, task, result);
			}
			context.setRequestAuthorized(true);
			result.recordSuccess();

			LOGGER.trace("Request authorized");

		} catch (Throwable e) {
			result.recordFatalError(e);
			throw e;
		}
	}
	
	private <F extends ObjectType, O extends ObjectType> ObjectSecurityConstraints authorizeElementContext(
			LensContext<F> context,
			LensElementContext<O> elementContext,
			OwnerResolver ownerResolver,
			boolean isFocus,
			Task task,
			OperationResult result) 
					throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Authorizing request for element context {}", elementContext.getHumanReadableName());
		}
		
		ObjectDelta<O> origPrimaryDelta = elementContext.getPrimaryDelta();
		// If there is no delta then there is no request to authorize
		if (origPrimaryDelta != null) {
			
			ObjectDelta<O> primaryDeltaClone = origPrimaryDelta.clone();
			ObjectDeltaObject<O> odo = elementContext.getObjectDeltaObject();
			PrismObject<O> object = elementContext.getObjectCurrent();
			if (object == null) {
				// This may happen when object is being added.
				// But also in cases such as assignment of account and modification of
				// the same account in one operation
				object = elementContext.getObjectNew();
			}
			String operationUrl = ModelImplUtils.getOperationUrlFromDelta(primaryDeltaClone);
			ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, ownerResolver, task, result);
			if (securityConstraints == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Denied request for element context {}: null security constraints", elementContext.getHumanReadableName());
				}
				throw new AuthorizationException("Access denied");
			}

			if (isFocus) {
				// Process assignments/inducements first. If the assignments/inducements are allowed then we
				// have to ignore the assignment item in subsequent security checks
				if (object.canRepresent(FocusType.class)) {
					processAssignment(context, elementContext, primaryDeltaClone, operationUrl, FocusType.F_ASSIGNMENT, object, ownerResolver, securityConstraints, task, result);
				}
				if (object.canRepresent(AbstractRoleType.class)) {
					processAssignment(context, elementContext, primaryDeltaClone, operationUrl, AbstractRoleType.F_INDUCEMENT, object, ownerResolver, securityConstraints, task, result);
				}
			}

			// Process credential changes explicitly. There is a special authorization for that.

			if (!primaryDeltaClone.isDelete()) {
				if (primaryDeltaClone.isAdd()) {
					PrismObject<O> objectToAdd = primaryDeltaClone.getObjectToAdd();
					PrismContainer<CredentialsType> credentialsContainer = objectToAdd.findContainer(UserType.F_CREDENTIALS);
					if (credentialsContainer != null) {
						List<UniformItemPath> pathsToRemove = new ArrayList<>();
						for (Item<?,?> item: credentialsContainer.getValue().getItems()) {
							ContainerDelta<?> cdelta = new ContainerDeltaImpl(item.getPath(), (PrismContainerDefinition)item.getDefinition(), prismContext);
							cdelta.addValuesToAdd(((PrismContainer)item).getValue().clone());
							AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, cdelta);
							LOGGER.trace("AUTZ: credential add {} decision: {}", item.getPath(), cdecision);
							if (cdecision == AuthorizationDecisionType.ALLOW) {
								// Remove it from primary delta, so it will not be evaluated later
								pathsToRemove.add(item.getPath());
							} else if (cdecision == AuthorizationDecisionType.DENY) {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("Denied request for element context {}: explicit credentials deny", elementContext.getHumanReadableName());
								}
								throw new AuthorizationException("Access denied");
							} else {
								// Do nothing. The access will be evaluated later in a normal way
							}
						}
						for (UniformItemPath pathToRemove: pathsToRemove) {
							objectToAdd.removeContainer(pathToRemove);
						}
					}
				} else {
					// modify
					Collection<? extends ItemDelta<?, ?>> credentialChanges = primaryDeltaClone.findItemDeltasSubPath(UserType.F_CREDENTIALS);
					for (ItemDelta credentialChange: credentialChanges) {
						AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, credentialChange);
						LOGGER.trace("AUTZ: credential delta {} decision: {}", credentialChange.getPath(), cdecision);
						if (cdecision == AuthorizationDecisionType.ALLOW) {
							// Remove it from primary delta, so it will not be evaluated later
							primaryDeltaClone.removeModification(credentialChange);
						} else if (cdecision == AuthorizationDecisionType.DENY) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Denied request for element context {}: explicit credentials deny", elementContext.getHumanReadableName());
							}
							throw new AuthorizationException("Access denied");
						} else {
							// Do nothing. The access will be evaluated later in a normal way
						}
					}
				}
			}

			if (primaryDeltaClone != null && !primaryDeltaClone.isEmpty()) {
				// TODO: optimize, avoid evaluating the constraints twice
				securityEnforcer.authorize(operationUrl, getRequestAuthorizationPhase(context) , AuthorizationParameters.Builder.buildObjectDelta(object, primaryDeltaClone), ownerResolver, task, result);
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Authorized request for element context {}, constraints:\n{}", elementContext.getHumanReadableName(), securityConstraints.debugDump(1));
			}
			
			return securityConstraints;
		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Authorized request for element context {}, constraints=null", elementContext.getHumanReadableName());
			}
			return null;
		}
	}
	
	private <F extends ObjectType,O extends ObjectType> void processAssignment(
			LensContext<F> context,
			LensElementContext<O> elementContext,
			ObjectDelta<O> primaryDeltaClone,
			String operationUrl,
			ItemName assignmentElementQName,
			PrismObject<O> object,
			OwnerResolver ownerResolver,
			ObjectSecurityConstraints securityConstraints,
			Task task, 
			OperationResult result) 
					throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		
		PrismObject<O> currentObject = elementContext.getObjectCurrent();
		if (currentObject == null) {
			currentObject = elementContext.getObjectOld();
		}
		
		if (primaryDeltaClone.hasItemOrSubitemDelta(assignmentElementQName)) {
			AccessDecision assignmentItemDecision = determineDecisionForAssignmentItems(securityConstraints, primaryDeltaClone, currentObject, operationUrl, getRequestAuthorizationPhase(context));
			LOGGER.trace("Security decision for {} items: {}", assignmentElementQName.getLocalPart(), assignmentItemDecision);
			if (assignmentItemDecision == AccessDecision.ALLOW) {
				// Nothing to do, operation is allowed for all values
				LOGGER.debug("Allow assignment/unassignment to {} becasue access to {} container/properties is explicitly allowed", assignmentElementQName.getLocalPart(), object);
			} else if (assignmentItemDecision == AccessDecision.DENY) {
				LOGGER.debug("Deny assignment/unassignment to {} becasue access to {} container/properties is explicitly denied", assignmentElementQName.getLocalPart(), object);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Denied request for element context {}: access to {} container/properties is explicitly denied", elementContext.getHumanReadableName(), assignmentElementQName.getLocalPart());
				}
				throw new AuthorizationException("Access denied");
			} else {
				AuthorizationDecisionType allItemsDecision = securityConstraints.findAllItemsDecision(operationUrl, getRequestAuthorizationPhase(context));
				if (allItemsDecision == AuthorizationDecisionType.ALLOW) {
					// Nothing to do, operation is allowed for all values
				} else if (allItemsDecision == AuthorizationDecisionType.DENY) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Denied request for element context {}: access to {} items is explicitly denied", elementContext.getHumanReadableName(), assignmentElementQName.getLocalPart());
					}
					throw new AuthorizationException("Access denied");
				} else {
					// No blank decision for assignment modification yet
					// process each assignment individually
					authorizeAssignmentRequest(context, operationUrl, ModelAuthorizationAction.ASSIGN.getUrl(),
							assignmentElementQName,
							object, ownerResolver, securityConstraints, PlusMinusZero.PLUS, true, task, result);

					if (!primaryDeltaClone.isAdd()) {
						// We want to allow unassignment even if there are policies. Otherwise we would not be able to get
						// rid of that assignment
						authorizeAssignmentRequest(context, operationUrl, ModelAuthorizationAction.UNASSIGN.getUrl(),
								assignmentElementQName,
								object, ownerResolver, securityConstraints, PlusMinusZero.MINUS, false, task, result);
					}
				}
			}
			// assignments were authorized explicitly. Therefore we need to remove them from primary delta to avoid another
			// authorization
			if (primaryDeltaClone.isAdd()) {
				PrismObject<O> objectToAdd = primaryDeltaClone.getObjectToAdd();
				objectToAdd.removeContainer(assignmentElementQName);
			} else if (primaryDeltaClone.isModify()) {
				primaryDeltaClone.removeContainerModification(ItemName.fromQName(assignmentElementQName));
			}
		}
	}

	private <F extends ObjectType,O extends ObjectType> void authorizeAssignmentRequest(
			LensContext<F> context,
			String operationUrl,
			String assignActionUrl,
			ItemName assignmentElementQName,
			PrismObject<O> object,
			OwnerResolver ownerResolver,
			ObjectSecurityConstraints securityConstraints,
			PlusMinusZero plusMinusZero,
			boolean prohibitPolicies,
			Task task, 
			OperationResult result) 
					throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		// This is *request* authorization. Therefore we care only about primary delta.
		ObjectDelta<F> focusPrimaryDelta = context.getFocusContext().getPrimaryDelta();
		if (focusPrimaryDelta == null) {
			return;
		}
		ContainerDelta<AssignmentType> focusAssignmentDelta = focusPrimaryDelta.findContainerDelta(assignmentElementQName);
		if (focusAssignmentDelta == null) {
			return;
		}
		String operationDesc = assignActionUrl.substring(assignActionUrl.lastIndexOf('#') + 1);
		Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues = determineChangedAssignmentValues(context.getFocusContext(), assignmentElementQName, focusAssignmentDelta, plusMinusZero);
		for (PrismContainerValue<AssignmentType> changedAssignmentValue: changedAssignmentValues) {
			AssignmentType changedAssignment = changedAssignmentValue.getRealValue();
			ObjectReferenceType targetRef = changedAssignment.getTargetRef();
			if (targetRef == null || targetRef.getOid() == null) {
				// This may still be allowed by #add and #modify authorizations. We have already checked these, but there may be combinations of
				// assignments, one of the assignments allowed by #assign, other allowed by #modify (e.g. MID-4517).
				// Therefore check the items again. This is not very efficient to check it twice. But this is not a common case
				// so there should not be any big harm in suffering this inefficiency.
				AccessDecision subitemDecision = securityEnforcer.determineSubitemDecision(securityConstraints, changedAssignmentValue, operationUrl, 
						getRequestAuthorizationPhase(context), null, plusMinusZero, operationDesc);
				if (subitemDecision == AccessDecision.ALLOW) {
					LOGGER.debug("{} of policy {} to {} allowed with {} authorization", operationDesc, assignmentElementQName.getLocalPart(), object, operationUrl);
					continue;
				} else {
					LOGGER.debug("{} of non-target {} not allowed", operationDesc, assignmentElementQName.getLocalPart());
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Denied request for object {}: {} of non-target {} not allowed", object, operationDesc, assignmentElementQName.getLocalPart());
					}
					securityEnforcer.failAuthorization(operationDesc, getRequestAuthorizationPhase(context), AuthorizationParameters.Builder.buildObject(object), result);
				}
			}
			
			PrismObject<ObjectType> target;
			try {
				// We do not worry about performance here too much. The target was already evaluated. This will be retrieved from repo cache anyway.
				target = objectResolver.resolve(targetRef.asReferenceValue(), "resolving " + assignmentElementQName.getLocalPart() + " target", task, result);
			} catch (ObjectNotFoundException e) {
				LOGGER.warn("Object {} referenced as {} target in {} was not found", targetRef.asReferenceValue().getOid(), assignmentElementQName.getLocalPart(), object);
				target = null;
			}

			ObjectDelta<O> assignmentObjectDelta = object.createModifyDelta();
			ContainerDelta<AssignmentType> assignmentDelta = assignmentObjectDelta.createContainerModification(assignmentElementQName);
			// We do not care if this is add or delete. All that matters for authorization is that it is in a delta.
			assignmentDelta.addValuesToAdd(changedAssignment.asPrismContainerValue().clone());
			
			QName relation = targetRef.getRelation();
			if (relation == null) {
				relation = prismContext.getDefaultRelation();
			}
			List<OrderConstraintsType> orderConstraints = deterimneOrderConstraints(assignmentElementQName, changedAssignment);
			
			AuthorizationParameters<O,ObjectType> autzParams = new AuthorizationParameters.Builder<O,ObjectType>()
					.oldObject(object)
					.delta(assignmentObjectDelta)
					.target(target)
					.relation(relation)
					.orderConstraints(orderConstraints)
					.build();
			
			if (prohibitPolicies) {
				if (changedAssignment.getPolicyRule() != null || !changedAssignment.getPolicyException().isEmpty() || !changedAssignment.getPolicySituation().isEmpty() || !changedAssignment.getTriggeredPolicyRule().isEmpty()) {
					// This may still be allowed by #add and #modify authorizations. We have already checked these, but there may be combinations of
					// assignments, one of the assignments allowed by #assign, other allowed by #modify (e.g. MID-4517).
					// Therefore check the items again. This is not very efficient to check it twice. But this is not a common case
					// so there should not be any big harm in suffering this inefficiency.
					AccessDecision subitemDecision = securityEnforcer.determineSubitemDecision(securityConstraints, changedAssignmentValue, operationUrl, 
							getRequestAuthorizationPhase(context), null, plusMinusZero, operationDesc);
					if (subitemDecision == AccessDecision.ALLOW) {
						LOGGER.debug("{} of policy assignment to {} allowed with {} authorization", operationDesc, object, operationUrl);
						continue;
					} else {
						securityEnforcer.failAuthorization("with assignment because of policies in the assignment", getRequestAuthorizationPhase(context), autzParams, result);
					}
				}
			}

			if (securityEnforcer.isAuthorized(assignActionUrl, getRequestAuthorizationPhase(context), autzParams, ownerResolver, task, result)) {
				LOGGER.debug("{} of target {} to {} allowed with {} authorization", operationDesc, target, object, assignActionUrl);
				continue;
			}
			if (relationRegistry.isDelegation(relation)) {
				if (securityEnforcer.isAuthorized(ModelAuthorizationAction.DELEGATE.getUrl(), getRequestAuthorizationPhase(context), autzParams, ownerResolver, task, result)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} of target {} to {} allowed with {} authorization", operationDesc, target, object, ModelAuthorizationAction.DELEGATE.getUrl());
					}
					continue;
				}
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} of target {} to {} denied", operationDesc, target, object);
			}
			securityEnforcer.failAuthorization("with assignment", getRequestAuthorizationPhase(context),  autzParams, result);
		}
	}

	private List<OrderConstraintsType> deterimneOrderConstraints(QName assignmentElementQName, AssignmentType changedAssignment) {
		OrderConstraintsType orderConstraints = new OrderConstraintsType();
		if (FocusType.F_ASSIGNMENT.equals(assignmentElementQName)) {
			orderConstraints.setOrder(0);
		} else {
			List<OrderConstraintsType> assignmentOrderConstraints = changedAssignment.getOrderConstraint();
			if (!assignmentOrderConstraints.isEmpty()) {
				return assignmentOrderConstraints;
			}
			Integer assignmentOrder = changedAssignment.getOrder();
			if (assignmentOrder == null) {
				orderConstraints.setOrder(1);
			} else {
				orderConstraints.setOrder(assignmentOrder);
			}
		}
		List<OrderConstraintsType> orderConstraintsList = new ArrayList<>(1);
		orderConstraintsList.add(orderConstraints);
		return orderConstraintsList;
	}

	private <O extends ObjectType> AccessDecision determineDecisionForAssignmentItems(
			ObjectSecurityConstraints securityConstraints, ObjectDelta<O> primaryDelta, PrismObject<O> currentObject, String operationUrl,
			AuthorizationPhaseType requestAuthorizationPhase) {
		return securityEnforcer.determineSubitemDecision(securityConstraints, primaryDelta, currentObject, operationUrl, requestAuthorizationPhase, SchemaConstants.PATH_ASSIGNMENT);
	}

	private <F extends ObjectType> AuthorizationPhaseType getRequestAuthorizationPhase(LensContext<F> context) {
		if (context.isExecutionPhaseOnly()) {
			return AuthorizationPhaseType.EXECUTION;
		} else {
			return AuthorizationPhaseType.REQUEST;
		}
	}

	private <F extends ObjectType> AuthorizationDecisionType evaluateCredentialDecision(LensContext<F> context, ObjectSecurityConstraints securityConstraints, ItemDelta credentialChange) {
		return securityConstraints.findItemDecision(credentialChange.getPath().namedSegmentsOnly(),
				ModelAuthorizationAction.CHANGE_CREDENTIALS.getUrl(), getRequestAuthorizationPhase(context));
	}


	private <F extends ObjectType> Collection<PrismContainerValue<AssignmentType>> determineChangedAssignmentValues(
			LensFocusContext<F> focusContext,
			QName assignmentElementQName,
			ContainerDelta<AssignmentType> assignmentDelta,
			PlusMinusZero plusMinusZero) {
		Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues = assignmentDelta.getValueChanges(plusMinusZero);
		if (plusMinusZero == PlusMinusZero.PLUS) {
			return changedAssignmentValues;
		}
		Collection<PrismContainerValue<AssignmentType>> processedChangedAssignmentValues = new ArrayList<>(changedAssignmentValues.size());
		PrismObject<F> existingObject = focusContext.getObjectCurrentOrOld();
		PrismContainer<AssignmentType> existingAssignmentContainer = existingObject.findContainer(ItemName.fromQName(assignmentElementQName));
		for (PrismContainerValue<AssignmentType> changedAssignmentValue : changedAssignmentValues) {
			if (changedAssignmentValue.isIdOnly()) {
				if (existingAssignmentContainer != null) {
					PrismContainerValue<AssignmentType> existingAssignmentValue = existingAssignmentContainer.findValue(changedAssignmentValue.getId());
					if (existingAssignmentValue != null) {
						processedChangedAssignmentValues.add(existingAssignmentValue);
					}
				}
			} else {
				processedChangedAssignmentValues.add(changedAssignmentValue);
			}
		}
		return processedChangedAssignmentValues;
	}


}
