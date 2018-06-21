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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.impl.messages.QueryProcessCommand;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author mederly
 */
@Component
@DependsOn({ "taskManager" })
public class WfProcessInstanceShadowTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/process-instance-shadow/handler-3";
    private static final String DOT_CLASS = WfProcessInstanceShadowTaskHandler.class.getName() + ".";

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    @Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WfConfiguration wfConfiguration;

    @Autowired(required = true)
    private ActivitiInterface activitiInterface;

    private static final Trace LOGGER = TraceManager.getTrace(WfProcessInstanceShadowTaskHandler.class);

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }

    /*
     * There are two kinds of wf process-watching tasks: passive and active.
     *
     * *Passive tasks* are used when wf processes are sophisticated enough to send events
     * about their state changes (using e.g. listeners or custom java tasks). In that case
     * we simply use midpoint tasks as holders of information coming within these events.
     *
     * In the future, we will implement the original idea that when the workflow process
     * instance finishes, the task status will be changed to RUNNABLE, and then this task
     * will be picked up by TaskManager to be run. This handler will be then called.
     * However, as for now, all processing (including post-processing after wf process
     * finish) is done within WorkflowHook.activiti2midpoint method.
     *
     * As for *active tasks*, these are used to monitor simple wf processes, which do
     * not send any information to midpoint by themselves. These tasks are recurrent,
     * so their run() method is periodically executed. This method simply asks the
     * WfMS for the information about the particular process id. The response is asynchronous,
     * and is processed within WorkflowHook.activiti2midpoint method.
     *
     */
	@Override
	public TaskRunResult run(Task task) {

        if (wfConfiguration.isEnabled()) {

		    // is this task already closed? (this flag is set by activiti2midpoint when it gets information about wf process termination)
            // todo: fixme this is a bit weird
		    if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
			    LOGGER.info("Task {} has been flagged as closed; exiting the run() method.", task);
		    }
            else {
                String id = wfTaskUtil.getProcessId(task);
                if (id != null) {
					LOGGER.debug("Task {}: requesting status for wf process id {}", task, id);
                    queryProcessInstance(id, task, task.getResult());
                }
            }
        } else {
            LOGGER.info("Workflow management is not currently enabled, skipping the task run.");
        }

        TaskRunResult result = new TaskRunResult();

        TaskRunResultStatus runResultStatus;
        if (wfTaskUtil.isProcessInstanceFinished(task)) {
            runResultStatus = TaskRunResultStatus.FINISHED_HANDLER;
        } else {
            runResultStatus = TaskRunResultStatus.FINISHED;             // finished means this run has finished, not the whole task
        }

        result.setOperationResult(task.getResult());            // todo fix this
		result.setRunResultStatus(runResultStatus);
		return result;
	}

    private void queryProcessInstance(String id, Task task, OperationResult parentResult) {

        String taskOid = task.getOid();
        Validate.notEmpty(taskOid, "Task oid must not be null or empty (task must be persistent).");

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "queryProcessInstance");

        QueryProcessCommand qpc = new QueryProcessCommand();
        qpc.setTaskOid(taskOid);
        qpc.setPid(id);

        try {
            activitiInterface.queryActivitiProcessInstance(qpc, task, result);
        } catch (RuntimeException|ObjectNotFoundException|ObjectAlreadyExistsException|SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't send a request to query a process instance to workflow management system", e);
            result.recordPartialError("Couldn't send a request to query a process instance to workflow management system", e);
        } finally {
			result.computeStatusIfUnknown();
		}
    }


    @Override
	public Long heartbeat(Task task) {
		return null;		// null - as *not* to record progress (which would overwrite operationResult!)
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }
}
