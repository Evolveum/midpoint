/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.messages.QueryProcessCommand;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This handler takes all changes from child tasks and puts them into model context of this task.
 *
 * @author mederly
 */
public class WfRootTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/wf-root-task-uri";

    private static final Trace LOGGER = TraceManager.getTrace(WfRootTaskHandler.class);

    WfRootTaskHandler(WorkflowManager workflowManager) {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        workflowManager.getTaskManager().registerHandler(HANDLER_URI, this);
    }

	@Override
	public TaskRunResult run(Task task) {

        OperationResult result = task.getResult();

//        ModelContext rootContext = wfTaskUtil.retrieveModelContext(task, result);
//        ObjectDelta<?>
//
//        List<Task> children = task.listSubtasks(result);
//
//        for (Task child : children) {
//            if (child.getExecutionStatus() != TaskExecutionStatus.CLOSED) {
//                throw new IllegalStateException("Child task " + child + " is not in CLOSED state; its state is " + child.getExecutionStatus());
//            }
//            ModelContext childContext = wfTaskUtil.retrieveModelContext(child, result);
//
//        }

		TaskRunResult runResult = new TaskRunResult();
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		return runResult;
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

}
