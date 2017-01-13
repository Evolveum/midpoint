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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemApprovalProcessStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getCacheRepositoryService;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getPrismContext;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESS_SPECIFIC_STATE;

/**
 * @author mederly
 */
public class MidpointUtil {

	public static void recordDecisionInTask(Decision decision, String taskOid) {
		try {
			ItemPath itemPath = new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESS_SPECIFIC_STATE, ItemApprovalProcessStateType.F_DECISIONS);		// assuming it already exists!
			ItemDefinition<?> itemDefinition = getPrismContext().getSchemaRegistry()
					.findContainerDefinitionByCompileTimeClass(ItemApprovalProcessStateType.class)
					.findItemDefinition(ItemApprovalProcessStateType.F_DECISIONS);
			getCacheRepositoryService().modifyObject(TaskType.class, taskOid,
					DeltaBuilder.deltaFor(TaskType.class, getPrismContext())
							.item(itemPath, itemDefinition).add(decision.toDecisionType())
							.asItemDeltas(),
					new OperationResult("dummy"));
		} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
			throw new SystemException("Couldn't record decision to the task " + taskOid + ": " + e.getMessage(), e);
		}
	}
}
