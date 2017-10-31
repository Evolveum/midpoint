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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Collection;
import java.util.List;

/**
 * TODO specify and clean-up error handling
 *
 * @author mederly
 */

public interface WorkflowManager {

    /*
     * Work items
     * ==========
     */

	<T extends Containerable> Integer countContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult result)
			throws SchemaException;

	<T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
			throws SchemaException;

    /*
     * CHANGING THINGS
     * ===============
     */

	/**
	 * Approves or rejects a work item (without supplying any further information).
	 * @param taskId       identifier of activiti task backing the work item
	 * @param decision     true = approve, false = reject
	 * @param comment
	 * @param additionalDelta
	 * @param causeInformation
	 * @param parentResult
	 */
	void completeWorkItem(String taskId, boolean decision, String comment, ObjectDelta additionalDelta,
			WorkItemEventCauseInformationType causeInformation, OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

	void claimWorkItem(String workItemId, OperationResult result) throws ObjectNotFoundException, SecurityViolationException;

	void releaseWorkItem(String workItemId, OperationResult result) throws SecurityViolationException, ObjectNotFoundException;

	void delegateWorkItem(String workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

	void stopProcessInstance(String instanceId, String username, OperationResult parentResult);

    /*
     * MISC
     * ====
     */

	public boolean isEnabled();

	// TODO remove this
	PrismContext getPrismContext();

	void registerProcessListener(ProcessListener processListener);

	void registerWorkItemListener(WorkItemListener workItemListener);

	Collection<ObjectReferenceType> getApprovedBy(Task task, OperationResult result) throws SchemaException;

	Collection<String> getApproverComments(Task task, OperationResult result) throws SchemaException;

	boolean isCurrentUserAuthorizedToSubmit(WorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	boolean isCurrentUserAuthorizedToClaim(WorkItemType workItem);

	boolean isCurrentUserAuthorizedToDelegate(WorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	// doesn't throw any exceptions - these are logged and stored into the operation result
	<T extends ObjectType> void augmentTaskObject(PrismObject<T> object, Collection<SelectorOptions<GetOperationOptions>> options,
			Task task, OperationResult result);

	// doesn't throw any exceptions - these are logged and stored into the operation result
	<T extends ObjectType> void augmentTaskObjectList(SearchResultList<PrismObject<T>> list,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result);

	ChangesByState getChangesByState(TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException;

	ChangesByState getChangesByState(TaskType childTask, TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, OperationResult result)
			throws SchemaException, ObjectNotFoundException;

	void synchronizeWorkflowRequests(OperationResult parentResult);

	void cleanupActivitiProcesses(OperationResult parentResult) throws SchemaException;

	/**
	 * Retrieves information about actual or expected execution of an approval schema.
	 * (So, this is restricted to approvals using this mechanism.)
	 *
	 * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
	 *
	 * @param taskOid OID of an approval task that should be analyzed
	 * @param opTask task under which this operation is carried out
	 * @param parentResult operation result
	 */
	ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String taskOid, Task opTask, OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException;

	/**
	 * Retrieves information about expected approval schema and its execution.
	 * (So, this is restricted to approvals using this mechanism.)
	 *
	 * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
	 *
	 * @param modelContext model context with the projector run already carried out (so the policy rules are evaluated)
	 * @param opTask task under which this operation is carried out
	 * @param parentResult operation result
	 */
	List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask, OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException;
}