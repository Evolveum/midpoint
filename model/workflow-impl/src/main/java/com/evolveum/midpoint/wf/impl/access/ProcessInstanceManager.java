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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */

@Component
public class ProcessInstanceManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceManager.class);

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;
	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private RepositoryService repositoryService;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_INTERFACE + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_INTERFACE + "deleteProcessInstance";

    public void closeCase(String caseOid, Task task, OperationResult parentResult)
		    throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);
        result.addParam("caseOid", caseOid);
        try {
            workflowEngine.closeCase(caseOid, task, result);
        } catch (RuntimeException | SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException e) {
            result.recordFatalError("Case couldn't be stopped: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteCase(String caseOid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);
        result.addParam("caseOid", caseOid);
        try {
            workflowEngine.deleteCase(caseOid, parentResult);
        } catch (RuntimeException e) {
            result.recordFatalError("Case couldn't be deleted: " + e.getMessage(), e);
			throw e;
        } finally {
			result.computeStatusIfUnknown();
		}
    }

}
