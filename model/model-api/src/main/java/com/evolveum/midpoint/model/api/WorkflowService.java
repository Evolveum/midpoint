/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationRequestType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author mederly
 */
public interface WorkflowService {

	//region Work items
	/**
	 * Approves or rejects a work item
	 */
	void completeWorkItem(@NotNull WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output, @NotNull Task task, @NotNull OperationResult parentResult)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

	/**
	 * Approves or rejects a work item.
	 * Additional delta is here present in "prism" form (not as ObjectDeltaType), to simplify the life for clients.
	 * It is applied only if the output signals that work item is approved.
	 */
	void completeWorkItem(WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output, ObjectDelta additionalDelta,
			Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

	void claimWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
		    CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
		    CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void delegateWorkItem(WorkItemId workItemId, WorkItemDelegationRequestType delegationRequest,
		    Task task, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException,
		    SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    //endregion

	//region Cases
	void cancelCase(String caseOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException;
	//endregion


}
