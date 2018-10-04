/**
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
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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
		LOGGER.trace("Authorizing request");
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
	
	private <F extends ObjectType, O extends ObjectType> ObjectSecurityConstraints authorizeElementContext(LensContext<F> context, LensElementContext<O> elementContext,
			OwnerResolver ownerResolver, boolean isFocus, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		ObjectDelta<O> primaryDelta = elementContext.getPrimaryDelta();
		// If there is no delta then there is no request to authorize
		if (primaryDelta != null) {
			PrismObject<O> currentObject = elementContext.getObjectCurrent();
			if (currentObject == null) {
				currentObject = elementContext.getObjectOld();
			}
			primaryDelta = primaryDelta.clone();
			ObjectDeltaObject<O> odo = elementContext.getObjectDeltaObject();
			PrismObject<O> object = elementContext.getObjectCurrent();
			if (object == null) {
				// This may happen when object is being added.
				// But also in cases such as assignment of account and modification of
				// the same account in one operation
				object = elementContext.getObjectNew();
			}
			String operationUrl = ModelImplUtils.getOperationUrlFromDelta(primaryDelta);
			ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, ownerResolver, task, result);
			if (securityConstraints == null) {
				throw new AuthorizationException("Access denied");
			}

			if (isFocus) {
				// Process assignments first. If the assignments are allowed then we
				// have to ignore the assignment item in subsequent security checks
				if (primaryDelta.hasItemOrSubitemDelta(SchemaConstants.PATH_ASSIGNMENT)) {
					AccessDecision assignmentItemDecision = determineDecisionForAssignmentItems(securityConstraints, primaryDelta, currentObject, operationUrl, getRequestAuthorizationPhase(context));
					LOGGER.trace("Security decision for assignment items: {}", assignmentItemDecision);
					if (assignmentItemDecision == AccessDecision.ALLOW) {
						// Nothing to do, operation is allowed for all values
						LOGGER.debug("Allow assignment/unassignment to {} becasue access to assignment container/properties is explicitly allowed", object);
					} else if (assignmentItemDecision == AccessDecision.DENY) {
						LOGGER.debug("Deny assignment/unassignment to {} becasue access to assignment container/properties is explicitly denied", object);
						throw new AuthorizationException("Access denied");
					} else {
						AuthorizationDecisionType allItemsDecision = securityConstraints.findAllItemsDecision(operationUrl, getRequestAuthorizationPhase(context));
						if (allItemsDecision == AuthorizationDecisionType.ALLOW) {
							// Nothing to do, operation is allowed for all values
						} else if (allItemsDecision == AuthorizationDecisionType.DENY) {
							throw new AuthorizationException("Access denied");
						} else {
							// No blank decision for assignment modification yet
							// process each assignment individually
							authorizeAssignmentRequest(context, operationUrl, ModelAuthorizationAction.ASSIGN.getUrl(),
									object, ownerResolver, securityConstraints, PlusMinusZero.PLUS, true, task, result);

							if (!primaryDelta.isAdd()) {
								// We want to allow unassignment even if there are policies. Otherwise we would not be able to get
								// rid of that assignment
								authorizeAssignmentRequest(context, operationUrl, ModelAuthorizationAction.UNASSIGN.getUrl(),
										object, ownerResolver, securityConstraints, PlusMinusZero.MINUS, false, task, result);
							}
						}
					}
					// assignments were authorized explicitly. Therefore we need to remove them from primary delta to avoid another
					// authorization
					if (primaryDelta.isAdd()) {
						PrismObject<O> objectToAdd = primaryDelta.getObjectToAdd();
						objectToAdd.removeContainer(FocusType.F_ASSIGNMENT);
					} else if (primaryDelta.isModify()) {
						primaryDelta.removeContainerModification(FocusType.F_ASSIGNMENT);
					}
				}
			}

			// Process credential changes explicitly. There is a special authorization for that.

			if (!primaryDelta.isDelete()) {
				if (primaryDelta.isAdd()) {
					PrismObject<O> objectToAdd = primaryDelta.getObjectToAdd();
					PrismContainer<CredentialsType> credentialsContainer = objectToAdd.findContainer(UserType.F_CREDENTIALS);
					if (credentialsContainer != null) {
						List<ItemPath> pathsToRemove = new ArrayList<>();
						for (Item<?,?> item: credentialsContainer.getValue().getItems()) {
							ContainerDelta<?> cdelta = new ContainerDelta(item.getPath(), (PrismContainerDefinition)item.getDefinition(), prismContext);
							cdelta.addValuesToAdd(((PrismContainer)item).getValue().clone());
							AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, cdelta);
							LOGGER.trace("AUTZ: credential add {} decision: {}", item.getPath(), cdecision);
							if (cdecision == AuthorizationDecisionType.ALLOW) {
								// Remove it from primary delta, so it will not be evaluated later
								pathsToRemove.add(item.getPath());
							} else if (cdecision == AuthorizationDecisionType.DENY) {
								throw new AuthorizationException("Access denied");
							} else {
								// Do nothing. The access will be evaluated later in a normal way
							}
						}
						for (ItemPath pathToRemove: pathsToRemove) {
							objectToAdd.removeContainer(pathToRemove);
						}
					}
				} else {
					// modify
					Collection<? extends ItemDelta<?, ?>> credentialChanges = primaryDelta.findItemDeltasSubPath(new ItemPath(UserType.F_CREDENTIALS));
					for (ItemDelta credentialChange: credentialChanges) {
						AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, credentialChange);
						LOGGER.trace("AUTZ: credential delta {} decision: {}", credentialChange.getPath(), cdecision);
						if (cdecision == AuthorizationDecisionType.ALLOW) {
							// Remove it from primary delta, so it will not be evaluated later
							primaryDelta.removeModification(credentialChange);
						} else if (cdecision == AuthorizationDecisionType.DENY) {
							throw new AuthorizationException("Access denied");
						} else {
							// Do nothing. The access will be evaluated later in a normal way
						}
					}
				}
			}

			if (primaryDelta != null && !primaryDelta.isEmpty()) {
				// TODO: optimize, avoid evaluating the constraints twice
				securityEnforcer.authorize(operationUrl, getRequestAuthorizationPhase(context) , AuthorizationParameters.Builder.buildObjectDelta(object, primaryDelta), ownerResolver, task, result);
			}

			return securityConstraints;
		} else {
			return null;
		}
	}

	private <F extends ObjectType,O extends ObjectType> void authorizeAssignmentRequest(
			LensContext<F> context,
			String operationUrl,
			String assignActionUrl,
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
		ContainerDelta<AssignmentType> focusAssignmentDelta = focusPrimaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
		if (focusAssignmentDelta == null) {
			return;
		}
		String operationDesc = assignActionUrl.substring(assignActionUrl.lastIndexOf('#') + 1);
		Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues = determineChangedAssignmentValues(context.getFocusContext(), focusAssignmentDelta, plusMinusZero);
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
					LOGGER.debug("{} of policy assignment to {} allowed with {} authorization", operationDesc, object, operationUrl);
					continue;
				} else {
					LOGGER.debug("{} of non-target assignment not allowed", operationDesc);
					securityEnforcer.failAuthorization(operationDesc, getRequestAuthorizationPhase(context), AuthorizationParameters.Builder.buildObject(object), result);
				}
			}
			// We do not worry about performance here too much. The target was already evaluated. This will be retrieved from repo cache anyway.
			PrismObject<ObjectType> target = objectResolver.resolve(targetRef.asReferenceValue(), "resolving assignment target", task, result);

			ObjectDelta<O> assignmentObjectDelta = object.createModifyDelta();
			ContainerDelta<AssignmentType> assignmentDelta = assignmentObjectDelta.createContainerModification(FocusType.F_ASSIGNMENT);
			// We do not care if this is add or delete. All that matters for authorization is that it is in a delta.
			assignmentDelta.addValuesToAdd(changedAssignment.asPrismContainerValue().clone());
			QName relation = targetRef.getRelation();
			if (relation == null) {
				relation = prismContext.getDefaultRelation();
			}
			AuthorizationParameters<O,ObjectType> autzParams = new AuthorizationParameters.Builder<O,ObjectType>()
					.oldObject(object)
					.delta(assignmentObjectDelta)
					.target(target)
					.relation(relation)
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


	private <F extends ObjectType> Collection<PrismContainerValue<AssignmentType>> determineChangedAssignmentValues(LensFocusContext<F> focusContext,
			ContainerDelta<AssignmentType> assignmentDelta, PlusMinusZero plusMinusZero) {
		Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues = assignmentDelta.getValueChanges(plusMinusZero);
		if (plusMinusZero == PlusMinusZero.PLUS) {
			return changedAssignmentValues;
		}
		Collection<PrismContainerValue<AssignmentType>> processedChangedAssignmentValues = new ArrayList<>(changedAssignmentValues.size());
		PrismObject<F> existingObject = focusContext.getObjectCurrentOrOld();
		PrismContainer<AssignmentType> existingAssignmentContainer = existingObject.findContainer(FocusType.F_ASSIGNMENT);
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
