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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfHook;
import com.evolveum.midpoint.wf.WfTaskHandler;
import com.evolveum.midpoint.wf.messages.ActivitiToMidPointMessage;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Transports messages from Activiti to midPoint (originally via Camel, now directly as java calls).
 *
 * Currently almost obsolete ... we'll see what will be its fate.
 */

@Component
public class Activiti2Idm {

    @Autowired(required = true)
    ActivitiEngine activitiEngine;

    @Autowired(required = true)
    TaskManager taskManager;

    @Autowired(required = true)
    WfTaskHandler wfTaskHandler;

    private static final Trace LOGGER = TraceManager.getTrace(Activiti2Idm.class);

    public void onWorkflowMessage(ActivitiToMidPointMessage msg) {

        OperationResult result = new OperationResult("onWorkflowMessage");

		LOGGER.info("onWorkflowMessage starting.");
		try {

			if (msg instanceof ProcessEvent) {

				ProcessEvent event = (ProcessEvent) msg;
				LOGGER.info("Received ProcessEvent: " + event);
				String taskOid = event.getTaskOid();

				if (taskOid != null) {

					Task task = taskManager.getTask(taskOid, result);
					wfTaskHandler.processWorkflowMessage(event, task, result);

				} else
					throw new Exception("Got a workflow message without taskOid: " + event.toString());
			} else
				throw new Exception("Unknown message type coming from the workflow: " + msg);

		} catch (Exception e) {
			String message = "Couldn't process an event coming from the workflow management system";
			LoggingUtils.logException(LOGGER, message, e);
			result.recordFatalError(message, e);
		}
		result.computeStatus();
		LOGGER.info("onWorkflowMessage ending; operation result status = " + result.getStatus());
    }

}
