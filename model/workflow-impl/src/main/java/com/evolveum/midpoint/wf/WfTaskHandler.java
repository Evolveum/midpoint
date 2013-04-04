/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.wf.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.messages.QueryProcessCommand;
import org.apache.commons.lang.Validate;


import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

/**
 * @author mederly
 */
public class WfTaskHandler implements TaskHandler {

	public static final String WF_SHADOW_TASK_URI = "http://evolveum.com/wf-shadow-task-uri";

    private WfTaskUtil wfTaskUtil;
	private TaskManager taskManager;
    private WorkflowManager workflowManager;
    private ActivitiInterface activitiInterface;

    private static final Trace LOGGER = TraceManager.getTrace(WfTaskHandler.class);

    WfTaskHandler(WorkflowManager workflowManager, WfTaskUtil wfTaskUtil, ActivitiInterface activitiInterface) {
        this.workflowManager = workflowManager;
        this.wfTaskUtil = wfTaskUtil;
        this.taskManager = workflowManager.getTaskManager();
        this.activitiInterface = activitiInterface;

        LOGGER.trace("Registering with taskManager as a handler for " + WF_SHADOW_TASK_URI);
        taskManager.registerHandler(WF_SHADOW_TASK_URI, this);
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

        if (workflowManager.isEnabled()) {
		
		    // is this task already closed? (this flag is set by activiti2midpoint when it gets information about wf process termination)
            // todo: fixme this is a bit weird
		    if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
			    LOGGER.info("Task " + task.getName() + " has been flagged as closed; exiting the run() method.");
		    }
            else {
                String id = wfTaskUtil.getProcessId(task);
                if (id != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Task " + task.getName() + ": requesting status for wf process id " + id + "...");
                    }
                    queryProcessInstance(id, task, null);
                }
            }
        } else {
            LOGGER.info("Workflow management is not currently enabled, skipping the task run.");
        }

		TaskRunResult result = new TaskRunResult();
		result.setRunResultStatus(TaskRunResultStatus.FINISHED);		// finished means this run has finished, not the whole task
		return result;
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

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    void queryProcessInstance(String id, Task task, OperationResult parentResult) {

        String taskOid = task.getOid();
        Validate.notEmpty(taskOid, "Task oid must not be null or empty (task must be persistent).");

        if (parentResult == null) {
            parentResult = new OperationResult("queryProcessInstance");
        }

        QueryProcessCommand qpc = new QueryProcessCommand();
        qpc.setTaskOid(taskOid);
        qpc.setPid(id);

        try {
            activitiInterface.midpoint2activiti(qpc);
        } catch (RuntimeException e) {     // FIXME
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to query a process instance to workflow management system", e);
            parentResult.recordPartialError("Couldn't send a request to query a process instance to workflow management system", e);
        }

        parentResult.recordSuccessIfUnknown();
    }

}
