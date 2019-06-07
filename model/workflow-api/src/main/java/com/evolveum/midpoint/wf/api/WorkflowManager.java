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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * TODO specify and clean-up error handling
 *
 * @author mederly
 */

public interface WorkflowManager {

	//region Work items
	/**
	 * Approves or rejects a work item
	 * @param decision     true = approve, false = reject
	 */
	void completeWorkItem(WorkItemId workItemId, boolean decision, String comment, ObjectDelta additionalDelta,
			WorkItemEventCauseInformationType causeInformation, Task task,
			OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
			ExpressionEvaluationException, CommunicationException, ConfigurationException;

	void claimWorkItem(WorkItemId workItemId, Task task, OperationResult result)
			throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException;

	void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException;

	void delegateWorkItem(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
			Task task, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException, CommunicationException, ConfigurationException;
	//endregion

	//region Process instances (cases)

	void cancelCase(String caseOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException;

	//endregion

    /*
     * MISC
     * ====
     */

	public boolean isEnabled();

	// TODO remove this
	PrismContext getPrismContext();

	void registerWorkflowListener(WorkflowListener workflowListener);

	boolean isCurrentUserAuthorizedToSubmit(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem);

	boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

	ChangesByState getChangesByState(TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException;

	ChangesByState getChangesByState(TaskType childTask, TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext, OperationResult result)
			throws SchemaException, ObjectNotFoundException;

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

	PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting);
}