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

import java.util.*;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.activiti.Idm2Activiti;
import com.evolveum.midpoint.wf.wrappers.WfProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ModelOperationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceFinishedEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceStartedEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessVariable;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfQueryProcessInstanceCommandType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfStartProcessInstanceCommandType;
import org.springframework.stereotype.Component;

@Component
public class WfHook implements ChangeHook {

    public static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private WfTaskUtil taskUtil;

    @Autowired(required = true)
    private Idm2Activiti idm2Activiti;

    private List<WfProcessWrapper> wrappers = new ArrayList<WfProcessWrapper>();

    @Override
    public HookOperationMode preChangePrimary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
                                              OperationResult result) {

        Validate.notNull(changes);
        Validate.notNull(task);
        Validate.notNull(result);

        LOGGER.trace("=====================================================================");
        LOGGER.trace("preChangePrimary: " + changes.toString());

        if (changes.size() != 1)
            return reportHookError(result, "Invalid number of objectDeltas in preChangePrimary (" + changes.size() + "), expected 1");

        return executeProcessStart(ModelOperationStageType.PRIMARY, changes, task, result);
    }

    public void registerWfProcessWrapper(WfProcessWrapper starter) {
        LOGGER.trace("Registering process wrapper: " + starter.getClass().getName());
        wrappers.add(starter);
    }

    @Override
    public HookOperationMode preChangeSecondary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
                                                OperationResult result) {
        LOGGER.trace("=====================================================================");
        LOGGER.trace("preChangeSecondary: " + dump(changes));

        return executeProcessStart(ModelOperationStageType.SECONDARY, changes, task, result);
    }

    @Override
    public HookOperationMode postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result) {
        LOGGER.trace("=====================================================================");
        LOGGER.trace("postChange: " + changes.toString());
        for (ObjectDelta<?> change : changes)
            LOGGER.trace(change.debugDump());
        return HookOperationMode.FOREGROUND;
    }


    private HookOperationMode executeProcessStart(ModelOperationStageType stage, Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
                                                  OperationResult result) {

        for (WfProcessWrapper wrapper : wrappers) {
            LOGGER.debug("Trying wrapper: " + wrapper.getClass().getName());
            WfProcessStartCommand startCommand = wrapper.startProcessIfNeeded(stage, changes, task);
            if (startCommand != null) {
                LOGGER.debug("Wrapper " + wrapper.getClass().getName() + " prepared the following wf process start command: " + startCommand);
                try {
                    startProcessInstance(startCommand, wrapper, task, result);
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

    private String dump(Collection<ObjectDelta<? extends ObjectType>> changes) {
        StringBuffer sb = new StringBuffer();
        for (ObjectDelta<?> change : changes) {
            sb.append(change.debugDump());
            sb.append('\n');
        }
        return sb.toString();
    }


//    private HookOperationMode startWfProcessOperationExecution(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult parentResult) {
//
//        String operationName = "" + task.getModelOperationState().getOperationName();
//        String operationDetails = dump(changes);
//
//        LOGGER.info("Called startWfProcessOperationExecution(operationName = " + operationName + ")");
//
//        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".startWfProcessOperationExecution");
//        result.addParam("changes", changes);
//        result.addParam("task", task);
//
//        Map<String,String> processVariables = new HashMap<String,String>();
//        processVariables.put("description", operationName);
//        processVariables.put("details", operationDetails);
//        startProcessInstance(IS_PROCESS_SIMPLE_OPERATION_EXECUTION, PROCESS_NAME_OPERATION_EXECUTION, INVTYPE_OPERATION_EXECUTION,
//                processVariables,
//                "for an execution of " + operationName,
//                task, result);
//        return HookOperationMode.BACKGROUND;
//    }

//    @SuppressWarnings("unused")
//    private HookOperationMode startWfProcessRoleAdd(ObjectDelta<?> change, Task task, OperationResult parentResult) {
//
//        Property roleNameProperty = change.getObjectToAdd().findProperty(SchemaConstants.C_NAME);
//        if (roleNameProperty == null)
//            return reportHookError(parentResult, "Role name is not known.");
//        String roleName = roleNameProperty.getValue(String.class).getValue();
//
//        LOGGER.info("Called startWfProcessRoleAdd(roleName = " + roleName + ")");
//
//        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".startWfProcessRoleAdd");
//        result.addParam("roleName", roleName);
//
//        Map<String,String> processVariables = new HashMap<String,String>();
//        processVariables.put("role", roleName);
//        startProcessInstance(IS_PROCESS_SIMPLE_ADD_ROLE, PROCESS_NAME_ADD_ROLE, INVTYPE_ADD_ROLE,
//                processVariables,
//                "for a creation of a role " + roleName,
//                task, result);
//        return HookOperationMode.BACKGROUND;
//    }

    /**
     * A convenience method for reporting errors in wf hooks.
     *
     * @param result
     * @param message
     * @return
     */
    private HookOperationMode reportHookError(OperationResult result, String message) {
        LOGGER.error(message);
        result.recordFatalError(message);
        return HookOperationMode.ERROR;
    }

    /**
     * Starts a process instance of WfMS.
     */
    private void startProcessInstance(WfProcessStartCommand startCommand, WfProcessWrapper wrapper, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        LOGGER.trace(" === startProcessInstance starting ===");

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but it is persistent; task = " + task);
        }

        // first, create in-memory task instance
        String taskName = "Workflow process-watching task " + (long) (Math.random() * 1000000.0);
        if (startCommand.isSimple())
            taskUtil.prepareActiveTask(task, taskName, parentResult);
        else
            taskUtil.preparePassiveTask(task, taskName, parentResult);

        taskUtil.setProcessWrapper(task, wrapper);

        taskManager.switchToBackground(task, parentResult);

        // taskUtil.setInvocationType(task, invtype, parentResult);

        // perhaps more useful would be state 'workflow proces instance creation HAS BEEN requested';
        // however, if we record process state AFTER the request is sent, it is possible
        // that the response would come even before we log the request
        taskUtil.recordProcessState(task, "Workflow process instance creation is being requested.", "", null, parentResult);

        // prepare and send the start process instance command
        WfStartProcessInstanceCommandType spicmd = new WfStartProcessInstanceCommandType();
        spicmd.setMidpointTaskOid(task.getOid());
        spicmd.setWfProcessName(startCommand.getProcessName());
        spicmd.setSendStartConfirmation(startCommand.isSimple());	// for simple processes we should get wrapper-generated start events
        setProcessVariables(spicmd, startCommand.getProcessVariables());

        try {
            idm2Activiti.idm2activiti(spicmd);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            taskUtil.recordProcessState(task, "Workflow process instance creation could not be requested: " + e, "", null, parentResult);
            parentResult.recordPartialError("Couldn't send a request to start a process instance to workflow management system", e);
        }

        // final
        parentResult.recordSuccessIfUnknown();

        LOGGER.trace(" === startProcessInstance ending ===");
    }

    private void setProcessVariables(WfStartProcessInstanceCommandType spicmd, Map<String, String> variables) {
        for (Map.Entry<String, String> e : variables.entrySet()) {
            WfProcessVariable var = new WfProcessVariable();
            var.setName(e.getKey());
            var.setValue(e.getValue());
            spicmd.getWfProcessVariable().add(var);
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

    void finishProcessing(WfProcessInstanceEventType event, Task task, OperationResult result) throws Exception
    {
        if (task == null)
            task = taskManager.getTask(event.getMidpointTaskOid(), result);

        WfProcessWrapper wrapper = taskUtil.getProcessWrapper(task, wrappers);
        wrapper.finishProcess(event, task, result);


//		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING)

//		// let us mark the task result as SUCCESS
//		OperationResult or = task.getResult();		// 'or' should really be non-null here
//		if (or != null) {
//			or.recordSuccess();
//			taskUtil.setTaskResult(task.getOid(), or);
//		}
    }

    void queryProcessInstance(String id, Task task, OperationResult parentResult)
    {
        String taskOid = task.getOid();
        Validate.notEmpty(taskOid, "Task oid must not be null or empty (task must be persistent).");

        if (parentResult == null)
            parentResult = new OperationResult("queryProcessInstance");

        WfQueryProcessInstanceCommandType qpicmd = new WfQueryProcessInstanceCommandType();
        qpicmd.setMidpointTaskOid(taskOid);
        qpicmd.setWfProcessInstanceId(id);

        try {
            idm2Activiti.idm2activiti(qpicmd);
//            wfInterface.sendMessageToWorkflowWithReply(
//                    new ObjectFactory().createWfQueryProcessInstanceCommand(qpicmd),
//                    task, QUERY_TIMEOUT, parentResult);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to query a process instance to workflow management system", e);
            parentResult.recordPartialError("Couldn't send a request to query a process instance to workflow management system", e);
        }

        parentResult.recordSuccessIfUnknown();
    }

    /**
     * Processes a message got from workflow engine - either synchronously (while waiting for
     * replies after sending - i.e. in a thread that requested the operation), or asynchronously
     * (directly from onWorkflowMessage, in a separate thread).
     *
     * @param event an event got from workflow engine
     * @param task a task instance (should be as current as possible)
     * @param result
     * @throws Exception
     */
    public void processWorkflowMessage(WfProcessInstanceEventType event, Task task, OperationResult result) throws Exception {

        String pid = event.getWfProcessInstanceId();
        //String answer = event.getWfAnswer();

        // let us generate description if there is none
        String description = event.getWfStateDescription();
        if (description == null || description.isEmpty())
        {
            if (event instanceof WfProcessInstanceStartedEventType)
                description = "Workflow process instance has been created (process id " + pid + ")";
            else if (event instanceof WfProcessInstanceFinishedEventType)
                description = "Workflow process instance has been ended (process id " + pid + ")";
            else if (event instanceof WfProcessInstanceEventType)
                description = "Workflow process instance has proceeded further (process id " + pid + ")";
        }

        // record the process state
        String details = taskUtil.dumpVariables(event);
        taskUtil.recordProcessState(task, description, details, event, result);

        // let us record process id (when getting "process started" event)
        if (event instanceof WfProcessInstanceStartedEventType)
            taskUtil.setWfProcessId(task, event.getWfProcessInstanceId(), result);

        // should we finish this task (do we have an answer or has the process instance ended)?
        if (event instanceof WfProcessInstanceFinishedEventType)                // was:  || answer != null
            finishProcessing(event, task, result);

    }
}
