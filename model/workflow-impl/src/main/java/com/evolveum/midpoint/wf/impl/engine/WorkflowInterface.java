/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.processes.ItemApprovalProcessOrchestrator;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  Transports messages between midPoint and wf engine - OBSOLETE. (Originally via Camel, currently using direct java calls.)
 */

@Component
public class WorkflowInterface {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowInterface.class);
    private static final String DOT_CLASS = WorkflowInterface.class.getName() + ".";

	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private ItemApprovalProcessOrchestrator itemApprovalProcessOrchestrator;

	public static String createWorkItemId(String caseOid, Long workItemId) {
		return caseOid + ":" + workItemId;
	}

	public static String getCaseOidFromWorkItemId(String workItemId) {
		return parseWorkItemId(workItemId)[0];
	}

	public static long getIdFromWorkItemId(String workItemId) {
		return Long.parseLong(parseWorkItemId(workItemId)[1]);
	}

	private static String[] parseWorkItemId(@NotNull String workItemId) {
		String[] components = workItemId.split(":");
		if (components.length != 2) {
			throw new IllegalStateException("Illegal work item ID: " + workItemId);
		} else {
			return components;
		}
	}

	public void startWorkflowProcessInstance(WfTaskCreationInstruction<?,?> instruction, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

		WfContextType wfContext = instruction.getWfContext();

		ItemApprovalEngineInvocationContext ctx = new ItemApprovalEngineInvocationContext(wfContext, task, task);
		workflowEngine.startProcessInstance(ctx, itemApprovalProcessOrchestrator, result);
	}
}
