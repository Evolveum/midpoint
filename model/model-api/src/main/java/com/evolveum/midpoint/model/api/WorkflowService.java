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
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType;

import java.util.List;

/**
 * @author mederly
 */
public interface WorkflowService {

	//region Work items
    /**
     * Approves or rejects a work item (without supplying any further information).
     * @param taskId identifier of work item
     * @param decision true = approve, false = reject
     */
    void completeWorkItem(WorkItemId workItemId, boolean decision, String comment, ObjectDelta additionalDelta,
		    Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException,
		    ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    void claimWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
		    CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult parentResult)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
		    CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void delegateWorkItem(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
		    Task task, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException,
		    SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    //endregion

	//region Cases
	void cancelCase(String caseOid, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException;
	//endregion


}
