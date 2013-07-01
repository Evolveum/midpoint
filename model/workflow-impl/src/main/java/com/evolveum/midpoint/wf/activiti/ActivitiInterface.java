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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.ProcessInstanceController;
import com.evolveum.midpoint.wf.messages.*;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricDetailQuery;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 *  Transports messages between midPoint and Activiti. (Originally via Camel, currently using direct java calls.)
 */

@Component
public class ActivitiInterface {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiInterface.class);
    private static final String DOT_CLASS = ActivitiInterface.class.getName() + ".";

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ProcessInstanceController processInstanceController;

    /**
     * Processes a message coming from midPoint to activiti. Although currently activiti is co-located with midPoint,
     * the interface between them is designed to be more universal - based on message passing.
     *
     * We pass task and operation result objects here. It is just because it is convenient for us and we CAN do this
     * (because of co-location of activiti and midPoint). In remote versions we will eliminate this. The code
     * is written in such a way that this should not pose a problem.
     */

    public void midpoint2activiti(MidPointToActivitiMessage cmd, Task task, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(" *** A command from midPoint has arrived; class = " + cmd.getClass().getName() + " ***");
        }

        if (cmd instanceof QueryProcessCommand)
        {
            QueryProcessCommand qpc = (QueryProcessCommand) cmd;
            QueryProcessResponse qpr = new QueryProcessResponse();

            String pid = qpc.getPid();
            qpr.setPid(pid);
            qpr.setTaskOid(qpc.getTaskOid());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Querying process instance id = " + pid);
            }

            HistoryService hs = activitiEngine.getHistoryService();

            HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
                    .variableUpdates()
                    .processInstanceId(pid)
                    .orderByTime().desc();

            for (HistoricDetail hd : hdq.list())
            {
                HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
                String varname = hvu.getVariableName();
                Object value = hvu.getValue();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(" - found historic variable update: " + varname + " <- " + value);
                }
                if (!qpr.containsVariable(varname)) {
                    qpr.putVariable(varname, value);
                }
            }

            HistoricDetailQuery hdq2 = hs.createHistoricDetailQuery()
                    .formProperties()
                    .processInstanceId(pid)
                    .orderByVariableRevision().desc();
            for (HistoricDetail hd : hdq2.list())
            {
                HistoricFormProperty hfp = (HistoricFormProperty) hd;
                String varname = hfp.getPropertyId();
                Object value = hfp.getPropertyValue();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(" - found historic form property: " + varname + " <- " + value);
                }
                qpr.putVariable(varname, value);
            }

            ProcessInstance pi = activitiEngine.getProcessEngine().getRuntimeService().createProcessInstanceQuery().processInstanceId(pid).singleResult();
            qpr.setRunning(pi != null && !pi.isEnded());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Running process instance = " + pi + ", isRunning: " + qpr.isRunning());
                LOGGER.trace("Response to be sent to midPoint: " + qpr);
            }

            activiti2midpoint(qpr, task, false, result);
        }
        else if (cmd instanceof StartProcessCommand)
        {
            StartProcessCommand spic = (StartProcessCommand) cmd;

            Map<String,Object> map = new HashMap<String,Object>();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("midpointTaskOid = " + spic.getTaskOid());
            }

            map.put(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID, spic.getTaskOid());
            map.put(CommonProcessVariableNames.VARIABLE_MIDPOINT_LISTENER, new IdmExecutionListenerProxy());
            map.putAll(spic.getVariables());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("process name = " + spic.getProcessName());
            }

            RuntimeService rs = activitiEngine.getProcessEngine().getRuntimeService();

            String owner = ((StartProcessCommand) cmd).getProcessOwner();
            if (owner != null) {
                activitiEngine.getIdentityService().setAuthenticatedUserId(owner);
            }
            //String businessKey = (String) map.get(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID);
            //ProcessInstance pi = rs.startProcessInstanceByKey(spic.getProcessName(), businessKey, map);
            ProcessInstance pi = rs.startProcessInstanceByKey(spic.getProcessName(), map);

            // let us send a reply back (useful for listener-free processes)

            if (spic.isSendStartConfirmation()) {
                ProcessStartedEvent event = new ProcessStartedEvent();
                event.setTaskOid(spic.getTaskOid());
                event.setPid(pi.getProcessInstanceId());
                event.setVariablesFrom(map);
                event.setRunning(!pi.isEnded());

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Event to be sent to IDM: " + event);
                }
                activiti2midpoint(event, task, false, result);
            }
        }
        else
        {
            String message = "Unknown incoming message type: " + cmd.getClass().getName();
            LOGGER.error(message);
        }
    }

    // task and parentResult may be null e.g. if this method is called from activiti process (for "smart" processes)
    // asynchronous = true if this method is called from activiti process ("smart" processes), false if it is called as a response
    //      to either query (from periodic querying of dumb processes) or to process start instruction
    // asynchronous messages are accepted only if task state is WAITING, in order to eliminate duplicate processing of finish messages
    public void activiti2midpoint(ActivitiToMidPointMessage msg, Task task, boolean asynchronous, OperationResult parentResult) {

        OperationResult result;
        if (parentResult == null) {
            result = new OperationResult(DOT_CLASS + "activiti2midpoint");
        } else {
            result = parentResult.createSubresult(DOT_CLASS + "activiti2midpoint");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("activiti2midpoint starting.");
        }

        try {

            if (msg instanceof ProcessEvent) {

                ProcessEvent event = (ProcessEvent) msg;
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Received ProcessEvent: " + event);
                }

                String taskOid = event.getTaskOid();
                if (taskOid == null) {
                    throw new IllegalStateException("Got a workflow message without taskOid: " + event.toString());
                }

                if (task != null) {
                    if (!taskOid.equals(task.getOid())) {
                        throw new IllegalStateException("TaskOid received from the workflow (" + taskOid + ") is different from current task OID (" + task + "): " + event.toString());
                    }
                } else {
                    task = taskManager.getTask(taskOid, result);
                }

                if (asynchronous && task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
                    LOGGER.trace("Asynchronous message received in a state different from WAITING ({}), ignoring it. Task = {}", task.getExecutionStatus(), task);
                } else {
                    processInstanceController.processWorkflowMessage(event, task, result);
                }

            } else {
                throw new IllegalStateException("Unknown message type coming from the workflow: " + msg);
            }

        } catch (Exception e) {     // todo fix the exception processing
            String message = "Couldn't process an event coming from the workflow management system";
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("activiti2midpoint ending; operation result status = " + result.getStatus());
        }
    }

    public void notifyMidpointFinal(DelegateExecution execution) {
        notifyMidpoint(execution, new ProcessFinishedEvent());
    }

    public void notifyMidpoint(DelegateExecution execution) {
        notifyMidpoint(execution, new ProcessEvent());
    }

    public void notifyMidpoint(DelegateExecution execution, ProcessEvent event) {
        event.setPid(execution.getProcessInstanceId());
        event.setRunning(true);
        event.setTaskOid((String) execution.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID));
        event.setVariablesFrom(execution.getVariables());
        activiti2midpoint(event, null, true, new OperationResult(DOT_CLASS + "notifyMidpoint"));
    }
}
