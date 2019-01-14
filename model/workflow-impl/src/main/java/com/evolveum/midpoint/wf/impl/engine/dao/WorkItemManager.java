/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.engine.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

/**
 * @author mederly
 */

@Component
public class WorkItemManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired private MiscDataUtil miscDataUtil;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private PrismContext prismContext;
    @Autowired private WorkItemProvider workItemProvider;
    @Autowired private WfTaskController wfTaskController;
    @Autowired private TaskManager taskManager;
    @Autowired private WorkflowEngine workflowEngine;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_INTERFACE + "completeWorkItem";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_INTERFACE + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_INTERFACE + "releaseWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_INTERFACE + "delegateWorkItem";

    public void completeWorkItem(String workItemId, String outcome, String comment, ObjectDelta additionalDelta,
			WorkItemEventCauseInformationType causeInformation, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
		    ConfigurationException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEM);
        result.addParam("workItemId", workItemId);
        result.addParam("decision", outcome);
        result.addParam("comment", comment);
        result.addParam("additionalDelta", additionalDelta);

		try {
			final String userDescription = toShortString(securityContextManager.getPrincipal().getUser());
			result.addContext("user", userDescription);

			LOGGER.trace("Completing work item {} with decision of {} ['{}'] by {}; cause: {}",
					workItemId, outcome, comment, userDescription, causeInformation);
			WorkItemType workItem = workflowEngine.getFullWorkItem(workItemId, result);
			// TODO: is this OK? Creating new task?
			com.evolveum.midpoint.task.api.Task opTask = taskManager.createTaskInstance(OPERATION_COMPLETE_WORK_ITEM);
			if (!miscDataUtil.isAuthorized(workItem, MiscDataUtil.RequestedOperation.COMPLETE, opTask, result)) {
				throw new SecurityViolationException("You are not authorized to complete this work item.");
			}
			workflowEngine.completeWorkItem(workItemId, workItem, outcome, comment, additionalDelta, causeInformation, result);
		} catch (SecurityViolationException | RuntimeException | CommunicationException | ConfigurationException | SchemaException | ObjectAlreadyExistsException e) {
			result.recordFatalError("Couldn't complete the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

    public void claimWorkItem(String workItemId, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_CLAIM_WORK_ITEM);
        result.addParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = securityContextManager.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Claiming work item {} by {}", workItemId, toShortString(principal.getUser()));
			}

			WorkItemType workItem = workflowEngine.getFullWorkItem(workItemId, result);
			if (workItem == null) {
				throw new ObjectNotFoundException("The work item does not exist");
			}
			if (!workItem.getAssigneeRef().isEmpty()) {
				String desc;
				if (workItem.getAssigneeRef().size() == 1 && principal.getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
					desc = "the current";
				} else {
					desc = "another";
				}
				throw new SystemException("The work item is already assigned to "+desc+" user");
			}
			if (!miscDataUtil.isAuthorizedToClaim(workItem)) {
				throw new SecurityViolationException("You are not authorized to claim the selected work item.");
			}
			workflowEngine.claim(workItemId, principal.getOid(), result);
		} catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException e) {
			result.recordFatalError("Couldn't claim the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

    public void releaseWorkItem(String workItemId, OperationResult parentResult)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_RELEASE_WORK_ITEM);
        result.addParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = securityContextManager.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Releasing work item {} by {}", workItemId, toShortString(principal.getUser()));
			}

			WorkItemType workItem = workflowEngine.getFullWorkItem(workItemId, result);
            if (workItem == null) {
				throw new ObjectNotFoundException("The work item does not exist");
            }
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
            workflowEngine.unclaim(workItemId, result);
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
	public void delegateWorkItem(String workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			WorkItemEscalationLevelType escalation, Duration newDuration, WorkItemEventCauseInformationType causeInformation,
			OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		OperationResult result = parentResult.createSubresult(OPERATION_DELEGATE_WORK_ITEM);
		result.addParam("workItemId", workItemId);
		result.addArbitraryObjectAsParam("escalation", escalation);
		result.addArbitraryObjectCollectionAsParam("delegates", delegates);
		try {
			MidPointPrincipal principal = securityContextManager.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			ObjectReferenceType initiator =
					causeInformation == null || causeInformation.getType() == WorkItemEventCauseTypeType.USER_ACTION ?
							ObjectTypeUtil.createObjectRef(principal.getUser(), prismContext) : null;

			LOGGER.trace("Delegating work item {} to {}: escalation={}; cause={}", workItemId, delegates,
					escalation != null ? escalation.getName() + "/" + escalation.getDisplayName() : "none", causeInformation);

			// TODO: is this OK? Creating new task?
			com.evolveum.midpoint.task.api.Task opTask = taskManager.createTaskInstance(OPERATION_DELEGATE_WORK_ITEM);

			WorkItemType workItem = workItemProvider.getWorkItem(workItemId, result);
			if (!miscDataUtil.isAuthorized(workItem, MiscDataUtil.RequestedOperation.DELEGATE, opTask, result)) {
				throw new SecurityViolationException("You are not authorized to delegate this work item.");
			}

			workflowEngine
					.executeDelegation(workItemId, delegates, method, escalation, newDuration, causeInformation, result, principal,
					initiator, opTask,
					workItem);
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
