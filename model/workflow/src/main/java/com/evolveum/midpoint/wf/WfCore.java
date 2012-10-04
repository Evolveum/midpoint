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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.ProcessFinishedEvent;
import com.evolveum.midpoint.wf.messages.ProcessStartedEvent;
import com.evolveum.midpoint.wf.messages.StartProcessCommand;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.wf.processes.StartProcessInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ModelOperationStateType;
import org.jvnet.jaxb2_commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 21.9.2012
 * Time: 17:39
 * To change this template use File | Settings | File Templates.
 */

@Component
public class WfCore {

    private static final Trace LOGGER = TraceManager.getTrace(WfCore.class);

    @Autowired(required = true)
    private WfHook wfHook;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    @Autowired(required = true)
    private ActivitiInterface activitiInterface;

    HookOperationMode executeProcessStart(ModelContext context, Task task, OperationResult result) {

        for (ProcessWrapper wrapper : wfHook.getWrappers()) {
            LOGGER.debug("Trying wrapper: " + wrapper.getClass().getName());
            StartProcessInstruction startCommand = wrapper.startProcessIfNeeded(context, task, result);
            if (startCommand != null) {
                LOGGER.debug("Wrapper " + wrapper.getClass().getName() + " prepared the following wf process start command: " + startCommand);
                try {
                    startProcessInstance(startCommand, wrapper, task, context, result);
                } catch(Exception e) { // TODO better error handling here
                    LoggingUtils.logException(LOGGER, "Workflow process instance couldn't be started", e);
                    return HookOperationMode.ERROR;
                }
                return HookOperationMode.BACKGROUND;
            }
        }

        LOGGER.debug("No wrapper served this request, returning the FOREGROUND flag.");
        return HookOperationMode.FOREGROUND;
    }

    /**
     * Starts a process instance in WfMS.
     */
    private void startProcessInstance(StartProcessInstruction startInstruction, ProcessWrapper wrapper, Task task, ModelContext context,
                                      OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(" === startProcessInstance starting ===");
        }

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but it is persistent; task = " + task);
        }

        // first, create in-memory task instance
        if (startInstruction.isSimple()) {
            wfTaskUtil.prepareActiveTask(task, startInstruction.getTaskName(), parentResult);
        } else {
            wfTaskUtil.preparePassiveTask(task, startInstruction.getTaskName(), parentResult);
        }

        wfTaskUtil.setProcessWrapper(task, wrapper);
        ModelOperationStateType state = new ModelOperationStateType();
        state.setOperationData(SerializationUtil.toString(context));
        task.setModelOperationState(state);

        taskManager.switchToBackground(task, parentResult);

        // perhaps more useful would be state 'workflow process instance creation HAS BEEN requested';
        // however, if we record process state AFTER the request is sent, it is possible
        // that the response would come even before we log the request
        wfTaskUtil.recordProcessState(task, "Workflow process instance creation is being requested.", "", null, parentResult);

        // prepare and send the start process instance command
        StartProcessCommand spc = new StartProcessCommand();
        spc.setTaskOid(task.getOid());
        spc.setProcessName(startInstruction.getProcessName());
        spc.setSendStartConfirmation(startInstruction.isSimple());	// for simple processes we should get wrapper-generated start events
        spc.setVariablesFrom(startInstruction.getProcessVariables());
        spc.setProcessOwner(task.getOwner().getOid());      // todo: is this ok?
        spc.addVariable(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER, wrapper.getClass().getName());

        try {
            activitiInterface.idm2activiti(spc);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            wfTaskUtil.recordProcessState(task, "Workflow process instance creation could not be requested: " + e, "", null, parentResult);
            parentResult.recordPartialError("Couldn't send a request to start a process instance to workflow management system: " + e.getMessage(), e);
        }

        // final
        parentResult.recordSuccessIfUnknown();

        LOGGER.trace(" === startProcessInstance ending ===");
    }

    /*
      * Post-processing
      *
      * For active tasks:
      *  - we throw away this wrapper, so the ModelOperationTaskHandler (next one on wrapper stack) will be invoked
      * For passive tasks:
      *  - we change task status to RUNNING, RELEASED, so that TaskManager will invoke WorkflowHookTaskHandler
      *
      * FIXME: correctly deal with the situation when more than one message with 'answer' present arrives
      * (probably do not invoke post-processing when task executionStatus == CLOSED)
      */

    void finishProcessing(ProcessEvent event, Task task, OperationResult result)
    {
        LOGGER.trace("finishProcessing called for task " + task + ", event " + event);

        if (task == null) {
            try {
                task = taskManager.getTask(event.getTaskOid(), result);
            } catch (ObjectNotFoundException e) {   // todo: fixme - temporary "solution"
                throw new SystemException(e);
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        ModelOperationStateType state = task.getModelOperationState();
        if (state == null || StringUtils.isEmpty(state.getOperationData())) {
            throw new IllegalStateException("The task does not contain model operation context; task = " + task);
        }
        ModelContext context;
        try {
            context = (ModelContext) SerializationUtil.fromString(state.getOperationData());
        } catch (IOException e) {
            throw new IllegalStateException("Model Context could not be fetched from the task due to IOException; task = " + task, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Model Context could not be fetched from the task due to ClassNotFoundException; task = " + task, e);
        }

        ProcessWrapper wrapper = wfTaskUtil.getProcessWrapper(task, wfHook.getWrappers());
        wrapper.finishProcess(context, event, task, result);
        try {
            state.setOperationData(SerializationUtil.toString(context));
            task.setModelOperationState(state);
            task.finishHandler(result);
            if (task.getExecutionStatus() == TaskExecutionStatus.WAITING) {
                taskManager.unpauseTask(task, result);
            }
        } catch (ObjectNotFoundException e) {
            throw new IllegalStateException(e);         // todo fixme
        } catch (SchemaException e) {
            throw new IllegalStateException(e);         // todo fixme
        } catch (IOException e) {
            throw new SystemException(e);               // todo fixme (serialization error)
        }

//		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING)

//		// let us mark the task result as SUCCESS
//		OperationResult or = task.getResult();		// 'or' should really be non-null here
//		if (or != null) {
//			or.recordSuccess();
//			wfTaskUtil.setTaskResult(task.getOid(), or);
//		}
    }

    /**
     * Processes a message got from workflow engine - either synchronously (while waiting for
     * replies after sending - i.e. in a thread that requested the operation), or asynchronously
     * (directly from activiti2midpoint, in a separate thread).
     *
     * @param event an event got from workflow engine
     * @param task a task instance (should be as current as possible)
     * @param result
     * @throws Exception
     */
    public void processWorkflowMessage(ProcessEvent event, Task task, OperationResult result) throws Exception {

        String pid = event.getPid();
        //String answer = event.getWfAnswer();

        // let us generate description if there is none
        String description = event.getState();
        if (description == null || description.isEmpty())
        {
            if (event instanceof ProcessStartedEvent) {
                description = "Workflow process instance has been created (process id " + pid + ")";
            } else if (event instanceof ProcessFinishedEvent) {
                description = "Workflow process instance has ended (process id " + pid + ")";
            } else {
                description = "Workflow process instance has proceeded further (process id " + pid + ")";
            }
        }

        // record the process state
        wfTaskUtil.recordProcessState(task, description, null, event, result);

        // let us record process id (when getting "process started" event)
        if (event instanceof ProcessStartedEvent) {
            wfTaskUtil.setWfProcessId(task, event.getPid(), result);
        }

        // should we finish this task?
        if (event instanceof ProcessFinishedEvent || !event.isRunning()) {
            finishProcessing(event, task, result);
        }

    }


    ProcessWrapper findProcessWrapper(Map<String, Object> vars, String id, OperationResult result) throws WorkflowException {
        String wrapperName = (String) vars.get(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER);
        if (wrapperName == null) {
            String m = "No process wrapper name found for wf process " + id;
            result.recordFatalError(m);
            throw new WorkflowException(m);
        }
        Exception e1;
        try {
            return (ProcessWrapper) Class.forName(wrapperName).newInstance();
        } catch (InstantiationException e) {
            e1 = e;
        } catch (IllegalAccessException e) {
            e1 = e;
        } catch (ClassNotFoundException e) {
            e1 = e;
        }
        String m = "Cannot instantiate workflow process wrapper " + wrapperName;
        result.recordFatalError(m, e1);
        throw new WorkflowException(m, e1);
    }

}
