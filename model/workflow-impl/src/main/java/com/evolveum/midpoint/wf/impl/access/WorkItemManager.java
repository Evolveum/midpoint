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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.api.request.ClaimWorkItemsRequest;
import com.evolveum.midpoint.wf.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.wf.api.request.DelegateWorkItemsRequest;
import com.evolveum.midpoint.wf.api.request.ReleaseWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Provides basic work item actions for upper layers.
 *
 * Takes care of
 * - handling operation result
 *
 * Does NOT take care of
 *  - authorizations -- these are handled internally by WorkflowEngine, because only at that time
 *      we read the appropriate objects (cases, work items).
 */

@Component
public class WorkItemManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired private WorkflowEngine workflowEngine;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_INTERFACE + "completeWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEMS = DOT_INTERFACE + "completeWorkItems";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_INTERFACE + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_INTERFACE + "releaseWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_INTERFACE + "delegateWorkItem";

    public void completeWorkItem(WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output,
		    WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
		    ConfigurationException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
        result.addParam("decision", output.getOutcome());
        result.addParam("comment", output.getComment());

		try {
			LOGGER.trace("Completing work item {} with decision of {} ['{}']; cause: {}",
					workItemId, output.getOutcome(), output.getComment(), causeInformation);
			CompleteWorkItemsRequest request = new CompleteWorkItemsRequest(workItemId.caseOid, causeInformation);
			request.getCompletions().add(new CompleteWorkItemsRequest.SingleCompletion(workItemId.id, output));
			workflowEngine.executeRequest(request, task, result);
		} catch (SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException e) {
			result.recordFatalError("Couldn't complete the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

	/**
	 * Bulk version of completeWorkItem method. It's necessary when we need to complete more items at the same time,
	 * to provide accurate completion information (avoiding completion of the first one and automated closure of other ones).
	 */
    public void completeWorkItems(CompleteWorkItemsRequest request, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
		    ConfigurationException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEMS);
		try {
			workflowEngine.executeRequest(request, task, result);
		} catch (SecurityViolationException | RuntimeException | CommunicationException | ConfigurationException | SchemaException | ObjectAlreadyExistsException e) {
			result.recordFatalError("Couldn't complete work items: " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

    // We can eventually provide bulk version of this method as well.
	public void claimWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_CLAIM_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
		try {
			LOGGER.trace("Claiming work item {}", workItemId);
			ClaimWorkItemsRequest request = new ClaimWorkItemsRequest(workItemId.caseOid);
			request.getClaims().add(new ClaimWorkItemsRequest.SingleClaim(workItemId.id));
			workflowEngine.executeRequest(request, task, result);
		} catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
			result.recordFatalError("Couldn't claim the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	// We can eventually provide bulk version of this method as well.
    public void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
		    CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_RELEASE_WORK_ITEM);
        result.addArbitraryObjectAsParam("workItemId", workItemId);
		try {
			LOGGER.trace("Releasing work item {}", workItemId);
			ReleaseWorkItemsRequest request = new ReleaseWorkItemsRequest(workItemId.caseOid);
			request.getReleases().add(new ReleaseWorkItemsRequest.SingleRelease(workItemId.id));
			workflowEngine.executeRequest(request, task, result);
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException | ObjectAlreadyExistsException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
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

	// We can eventually provide bulk version of this method as well.
	public void delegateWorkItem(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			WorkItemEscalationLevelType escalation, Duration newDuration, WorkItemEventCauseInformationType causeInformation,
			Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		OperationResult result = parentResult.createSubresult(OPERATION_DELEGATE_WORK_ITEM);
		result.addArbitraryObjectAsParam("workItemId", workItemId);
		result.addArbitraryObjectAsParam("escalation", escalation);
		result.addArbitraryObjectCollectionAsParam("delegates", delegates);
		try {
			LOGGER.trace("Delegating work item {} to {}: escalation={}; cause={}", workItemId, delegates,
					escalation != null ? escalation.getName() + "/" + escalation.getDisplayName() : "none", causeInformation);
			DelegateWorkItemsRequest request = new DelegateWorkItemsRequest(workItemId.caseOid, causeInformation);
			request.getDelegations().add(new DelegateWorkItemsRequest.SingleDelegation(workItemId.id, delegates,
					defaultIfNull(method, WorkItemDelegationMethodType.REPLACE_ASSIGNEES), escalation, newDuration));
			workflowEngine.executeRequest(request, task, result);
		} catch (SecurityViolationException | RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException e) {
			result.recordFatalError("Couldn't delegate/escalate work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			throw new IllegalStateException(e);
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	@NotNull
	public CaseWorkItemType getWorkItem(WorkItemId id, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, id.caseOid, null, result);
		//noinspection unchecked
		PrismContainerValue<CaseWorkItemType> pcv = (PrismContainerValue<CaseWorkItemType>)
				caseObject.find(ItemPath.create(CaseType.F_WORK_ITEM, id.id));
		if (pcv == null) {
			throw new ObjectNotFoundException("No work item " + id.id + " in " + caseObject);
		}
		return pcv.asContainerable();
	}
}
