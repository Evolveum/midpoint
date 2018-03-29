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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public interface CaseManagementService {

	String CLASS_NAME_WITH_DOT = CaseManagementService.class.getName() + ".";
	String COMPLETE_WORK_ITEM = CLASS_NAME_WITH_DOT + "completeWorkItem";

	/**
	 * Marks given work item as completed.
	 *
	 * @param caseOid OID of the CaseType object this work item is part of
	 * @param workItemId ID of the work item within given case
	 * @param output (Optional) output of the work item, to be recorded
	 * @param task Task in context of which the operation is to take place
	 * @param parentResult Operation result in context of which the operation is to take place
	 */
    void completeWorkItem(@NotNull String caseOid, long workItemId, @Nullable AbstractWorkItemOutputType output, @NotNull Task task, @NotNull OperationResult parentResult)
		    throws SecurityViolationException, SchemaException, ObjectNotFoundException, CommunicationException,
		    ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

}
