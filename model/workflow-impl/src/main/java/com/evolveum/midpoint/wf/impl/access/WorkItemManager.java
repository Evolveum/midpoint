/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.access;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.wf.api.CompleteAction;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static java.util.Collections.singleton;

/**
 * Takes care of
 * - authorization (including determination of the user)
 * - handling operation result
 */

@Component
public class WorkItemManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired private MiscDataUtil miscDataUtil;
    @Autowired private AuthorizationHelper authorizationHelper;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private PrismContext prismContext;
    @Autowired private WorkflowEngine workflowEngine;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_INTERFACE + "completeWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEMS = DOT_INTERFACE + "completeWorkItems";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_INTERFACE + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_INTERFACE + "releaseWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_INTERFACE + "delegateWorkItem";

    public void completeWorkItem(WorkItemId workItemId, String outcome, String comment, ObjectDelta<? extends ObjectType> additionalDelta,
		    WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
		    ConfigurationException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
        result.addParam("decision", outcome);
        result.addParam("comment", comment);
        result.addParam("additionalDelta", additionalDelta);

		try {
			MidPointPrincipal principal = getAndRecordPrincipal(result);
			LOGGER.trace("Completing work item {} with decision of {} ['{}'] by {}; cause: {}",
					workItemId, outcome, comment, toShortString(principal.getUser()), causeInformation);
			CaseWorkItemType workItem = workflowEngine.getWorkItem(workItemId, result);
			CompleteAction completeAction = new CompleteAction(workItemId, workItem, outcome, comment, additionalDelta, causeInformation);
			completeWorkItemsInternal(singleton(completeAction), principal, task, result);
		} catch (SecurityViolationException | RuntimeException | CommunicationException | ConfigurationException | SchemaException | ObjectAlreadyExistsException e) {
			result.recordFatalError("Couldn't complete the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

	@NotNull
	private MidPointPrincipal getAndRecordPrincipal(OperationResult result) throws SecurityViolationException {
		MidPointPrincipal principal = securityContextManager.getPrincipal();
		result.addContext("user", toShortString(principal.getUser()));
		return principal;
	}

	// assuming the work items in actions are fresh enough
    public void completeWorkItems(Collection<CompleteAction> actions, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
		    ConfigurationException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEMS);
		try {
			MidPointPrincipal principal = getAndRecordPrincipal(result);
			completeWorkItemsInternal(actions, principal, task, result);
		} catch (SecurityViolationException | RuntimeException | CommunicationException | ConfigurationException | SchemaException | ObjectAlreadyExistsException e) {
			result.recordFatalError("Couldn't complete work items: " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

	private void completeWorkItemsInternal(Collection<CompleteAction> actions, MidPointPrincipal principal, Task task, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, SchemaException, ObjectAlreadyExistsException {
		LOGGER.trace("Executing complete actions: {}", actions);
		for (CompleteAction action : actions) {
			if (!authorizationHelper.isAuthorized(action.getWorkItem(), AuthorizationHelper.RequestedOperation.COMPLETE, task, result)) {
				throw new SecurityViolationException("You are not authorized to complete the work item.");
			}
		}
		workflowEngine.completeWorkItems(actions, principal, result);
	}

	public void claimWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_CLAIM_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = getAndRecordPrincipal(result);
			LOGGER.trace("Claiming work item {} by {}", workItemId, toShortString(principal.getUser()));
			CaseWorkItemType workItem = workflowEngine.getWorkItem(workItemId, result);
			if (!workItem.getAssigneeRef().isEmpty()) {
				String desc;
				if (workItem.getAssigneeRef().size() == 1 && principal.getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
					desc = "the current";
				} else {
					desc = "another";
				}
				throw new SystemException("The work item is already assigned to "+desc+" user");
			}
			if (!authorizationHelper.isAuthorizedToClaim(workItem)) {
				throw new SecurityViolationException("You are not authorized to claim the selected work item.");
			}
			workflowEngine.claim(workItemId, principal, task, result);
		} catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException e) {
			result.recordFatalError("Couldn't claim the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

    public void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_RELEASE_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = getAndRecordPrincipal(result);

			LOGGER.trace("Releasing work item {} by {}", workItemId, toShortString(principal.getUser()));

			CaseWorkItemType workItem = workflowEngine.getWorkItem(workItemId, result);
            if (workItem.getAssigneeRef().isEmpty()) {
				throw new SystemException("The work item is not assigned to a user");
            }
            if (workItem.getAssigneeRef().size() > 1) {
				throw new SystemException("The work item is assigned to more than one user, so it cannot be released");
            }
			if (!principal.getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
				throw new SystemException("The work item is not assigned to the current user");
			}
            if (workItem.getCandidateRef().isEmpty()) {
            	result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "There are no candidates this work item can be offered to");
                return;
            }
            workflowEngine.unclaim(workItemId, principal, task, result);
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException e) {
            result.recordFatalError("Couldn't release work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
        } finally {
			result.computeStatusIfUnknown();
		}
    }

    // TODO when calling from model API, what should we put into escalationLevelName+DisplayName ?
	// Probably the API should look different. E.g. there could be an "Escalate" button, that would look up the
	// appropriate escalation timed action, and invoke it. We'll solve this when necessary. Until that time, be
	// aware that escalationLevelName/DisplayName are for internal use only.
	public void delegateWorkItem(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			WorkItemEscalationLevelType escalation, Duration newDuration, WorkItemEventCauseInformationType causeInformation,
			Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		OperationResult result = parentResult.createSubresult(OPERATION_DELEGATE_WORK_ITEM);
		result.addArbitraryObjectAsParam("workItemId", workItemId);
		result.addArbitraryObjectAsParam("escalation", escalation);
		result.addArbitraryObjectCollectionAsParam("delegates", delegates);
		try {
			MidPointPrincipal principal = getAndRecordPrincipal(result);

			ObjectReferenceType initiator =
					causeInformation == null || causeInformation.getType() == WorkItemEventCauseTypeType.USER_ACTION ?
							ObjectTypeUtil.createObjectRef(principal.getUser(), prismContext) : null;

			LOGGER.trace("Delegating work item {} to {}: escalation={}; cause={}", workItemId, delegates,
					escalation != null ? escalation.getName() + "/" + escalation.getDisplayName() : "none", causeInformation);

			CaseWorkItemType workItem = workflowEngine.getWorkItem(workItemId, result);
			if (!authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.DELEGATE, task, result)) {
				throw new SecurityViolationException("You are not authorized to delegate this work item.");
			}

			workflowEngine
					.executeDelegation(workItemId, delegates, method, escalation, newDuration, causeInformation,
							result, principal, initiator, task, workItem);
		} catch (SecurityViolationException | RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException e) {
			result.recordFatalError("Couldn't delegate/escalate work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			throw new IllegalStateException(e);
		} finally {
			result.computeStatusIfUnknown();
		}
	}
}
