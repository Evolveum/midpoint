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
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core methods of the workflow module.
 *
 * @author mederly
 */

@Component
public class WfCore {

    private static final Trace LOGGER = TraceManager.getTrace(WfCore.class);

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    @Autowired(required = true)
    private WorkflowManager workflowManager;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ActivitiInterface activitiInterface;

    private List<ChangeProcessor> changeProcessors = new ArrayList<ChangeProcessor>();

    private List<ProcessWrapper> wrappers = new ArrayList<ProcessWrapper>();


    // point of configuration [in future, it will be done more intelligently ;)]
    // beware, the order of change processors might be important!

    @PostConstruct
    public void initialize() {
        changeProcessors.add(new PrimaryUserChangeProcessor(workflowManager));
    }

    /**
     * Looks whether wf process instance should be started. If so, starts it and returns "background" HookOperationMode.
     * Uses first applicable change processor.
     *
     * @param context ModelContext describing current operation
     * @param task
     * @param result
     * @return HookOperationMode.BACKGROUND if a process was started; .FOREGROUND if not; .ERROR in case of error
     */
    HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) {

        for (ChangeProcessor changeProcessor : changeProcessors) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Trying change processor: " + changeProcessor.getClass().getName());
            }
            try {
                HookOperationMode hookOperationMode = changeProcessor.startProcessesIfNeeded(context, task, result);
                if (hookOperationMode != null) {
                    return hookOperationMode;
                }
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Schema exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                result.recordFatalError("Schema exception while running change processor " + changeProcessor.getClass(), e);
                return HookOperationMode.ERROR;
            } catch (RuntimeException e) {
                LoggingUtils.logException(LOGGER, "Runtime exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                result.recordFatalError("Runtime exception while running change processor " + changeProcessor.getClass(), e);
                return HookOperationMode.ERROR;
            }
        }

//            StartProcessInstruction startCommand = wrapper.prepareStartCommandIfApplicable(context, task, result);
//            if (startCommand != null) {
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug("Wrapper " + wrapper.getClass().getName() + " prepared the following wf process start command: " + startCommand);
//                }
//                try {
//                    startProcessInstance(startCommand, wrapper, task, context, result);
//                    result.recordSuccessIfUnknown();
//                    return HookOperationMode.BACKGROUND;
//                } catch (Exception e) { // TODO better error handling here (will be done with java7)
//                    LoggingUtils.logException(LOGGER, "Workflow process instance couldn't be started", e);
//                    result.recordFatalError("Workflow process instance couldn't be started", e);
//                    return HookOperationMode.ERROR;
//                }
//            }
//        }

        LOGGER.trace("No change processor caught this request, returning the FOREGROUND flag.");
        result.recordSuccess();
        return HookOperationMode.FOREGROUND;
    }


    // if rootContext == null, we do not put ModelOperationTaskHandler & WfRootTaskHandler onto the stack
    public void prepareRootTask(ModelContext rootContext, Task task, OperationResult result) throws SchemaException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareRootTask starting; task = " + task);
        }

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but this one is persistent; task = " + task);
        }

        if (task.getHandlerUri() != null) {
            throw new IllegalStateException("Workflow-related task should have no handler URI at this moment; task = " + task + ", existing handler URI = " + task.getHandlerUri());
        }

        if (rootContext != null) {

            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);
            task.pushHandlerUri(WfRootTaskHandler.HANDLER_URI, null, null);
            try {
                wfTaskUtil.storeModelContext(task, rootContext, result);
            } catch (SchemaException e) {
                throw new SchemaException("Couldn't put model context into root workflow task " + task, e);
            }
        }

        try {
            task.waitForSubtasks(null, result);
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Couldn't mark the task as waiting for subtasks: " + e, e);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Couldn't mark the task as waiting for subtasks: " + e, e);
        }
        taskManager.switchToBackground(task, result);

    }



    /**
     * Starts a process instance in WfMS.
     */
    private void startProcessInstance(StartProcessInstruction startInstruction, ProcessWrapper wrapper, Task task, ModelContext context,
                                      OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {


        // first, create in-memory task instance
        if (startInstruction.isSimple()) {
            wfTaskUtil.prepareActiveTask(task, startInstruction.getTaskName(), parentResult);
        } else {
            wfTaskUtil.preparePassiveTask(task, startInstruction.getTaskName(), parentResult);
        }

        wfTaskUtil.setProcessWrapper(task, wrapper);
        wfTaskUtil.storeModelContext(task, context, parentResult);

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
        spc.setProcessOwner(task.getOwner().getOid());
        spc.addVariable(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER, wrapper.getClass().getName());

        try {
            activitiInterface.midpoint2activiti(spc);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            wfTaskUtil.recordProcessState(task, "Workflow process instance creation could not be requested: " + e, "", null, parentResult);
            parentResult.recordPartialError("Couldn't send a request to start a process instance to workflow management system: " + e.getMessage(), e);
            wfTaskUtil.markTaskAsClosed(task, parentResult);
            throw new SystemException("Workflow process instance creation could not be requested", e);
        }

        // final
        parentResult.recordSuccessIfUnknown();

        LOGGER.trace(" === startProcessInstance ending ===");
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

    private void finishProcessing(ProcessEvent event, Task task, OperationResult result)
    {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("finishProcessing called for task " + task + ", event " + event);
        }

        if (task == null) {
            try {
                task = taskManager.getTask(event.getTaskOid(), result);
            } catch (ObjectNotFoundException e) {   // todo: fixme - temporary "solution"
                throw new SystemException(e);
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        }

        ModelContext context = null;
        try {
            context = wfTaskUtil.getModelContext(task, result);
        } catch (SchemaException e) {
            // todo better error reporting
            throw new IllegalStateException("Model Context could not be fetched from the task due to SchemaException; task = " + task, e);
        }

        ProcessWrapper wrapper = wfTaskUtil.getProcessWrapper(task, wrappers);
        wrapper.finishProcess(context, event, task, result);
        try {
            wfTaskUtil.storeModelContext(task, context, result);
            task.finishHandler(result);
            if (task.getExecutionStatus() == TaskExecutionStatus.WAITING) {
                taskManager.unpauseTask(task, result);
            }
        } catch (ObjectNotFoundException e) {
            throw new IllegalStateException(e);         // todo fixme
        } catch (SchemaException e) {
            throw new IllegalStateException(e);         // todo fixme
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Task after finishProcessing() = " + task.dump());
        }

//		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING)

//		// let us mark the task result as SUCCESS
//		OperationResult or = task.getResult();		// 'or' should really be non-null here
//		if (or != null) {
//			or.recordSuccess();
//			wfTaskUtil.setTaskResult(task.getOid(), or);
//		}
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

    public void registerWfProcessWrapper(ProcessWrapper starter) {
        LOGGER.trace("Registering process wrapper: " + starter.getClass().getName());
        wrappers.add(starter);
    }
    
}
