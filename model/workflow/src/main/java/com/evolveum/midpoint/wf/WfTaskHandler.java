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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.task.api.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class WfTaskHandler implements TaskHandler, InitializingBean {

	public static final String WF_SHADOW_TASK_URI = "http://evolveum.com/wf-shadow-task-uri";

	@Autowired(required = true)
	private WfHook workflowHook;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WorkflowManager workflowManager;

	private static final Trace LOGGER = TraceManager.getTrace(WfTaskHandler.class);

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
     * finish) is done within WorkflowHook.onWorkflowMessage method.
     * 
     * As for *active tasks*, these are used to monitor simple wf processes, which do
     * not send any information to midpoint by themselves. These tasks are recurrent,
     * so their run() method is periodically executed. This method simply asks the
     * WfMS for the information about the particular process id. The response is asynchronous,
     * and is processed within WorkflowHook.onWorkflowMessage method. 
     *  
     */
	@Override
	public TaskRunResult run(Task task) {

        if (workflowManager.isEnabled()) {
		
		    // is this task already closed? (this flag is set by onWorkflowMessage when it gets information about wf process termination)
		    if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
			    LOGGER.info("Task " + task.getName() + " has been flagged as closed, so exit the run() method.");
		    }
            else {
                // let us request the current task status
                // todo make this property single-valued in schema to be able to use getRealValue
                PrismProperty idProp = task.getExtension(WfTaskUtil.WFPROCESSID_PROPERTY_NAME);
                Collection<String> values = null;
                if (idProp != null) {
                    values = idProp.getRealValues(String.class);
                }
                if (values == null || values.isEmpty())
                    LOGGER.error("Process ID is not known for task " + task.getName());
                else {
                    String id = values.iterator().next();
                    //String id = (String) idProp.getRealValue(String.class);
                    LOGGER.info("Task " + task.getName() + ": requesting status for wf process id " + id + "...");
                    workflowHook.queryProcessInstance(id, task, null);
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


    @Override
	public void afterPropertiesSet() throws Exception {
		if (taskManager != null) {
			LOGGER.trace("Registering with taskManager as a handler for " + WF_SHADOW_TASK_URI);
			taskManager.registerHandler(WF_SHADOW_TASK_URI, this);
		}
		else
			LOGGER.error("Cannot register with taskManager as taskManager == null");
	}
	
	

}
