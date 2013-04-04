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

import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
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
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.taskHandlers.WfPrepareChildOperationTaskHandler;
import com.evolveum.midpoint.wf.taskHandlers.WfProcessShadowTaskHandler;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manages everything related to a activiti process instance, including the task that monitors that process instance.
 *
 * @author mederly
 */
@Component
public class ProcessInstanceController {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessInstanceController.class);

    private static final long TASK_START_DELAY = 5000L;
    private static final boolean USE_WFSTATUS = true;

    @Autowired(required = true)
    private WorkflowManager workflowManager;    // used to get wf configuration and a list of change processors

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ActivitiInterface activitiInterface;

    @Autowired(required = true)
    private RepositoryService repositoryService;

    public void startProcessInstance(StartProcessInstruction instruction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        prepareChildTask(instruction, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving workflow monitoring/execution task: " + task.dump());
        }

        saveChildTask(task, result);

        if (!instruction.isNoProcess()) {
            startWorkflowProcessInstance(instruction, task, result);
        }

    }

    /*************************** WORKING WITH TASKS ***************************/

    private void prepareChildTask(StartProcessInstruction instruction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareChildTask starting; task = " + task);
        }

        checkTaskCleanness(task);

        wfTaskUtil.setTaskNameIfEmpty(task, instruction.getTaskName());
        setDefaultTaskOwnerIfEmpty(task, result);
        task.setCategory(TaskCategory.WORKFLOW);

        if (instruction.isExecuteImmediately()) {
            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);
            task.pushHandlerUri(WfPrepareChildOperationTaskHandler.HANDLER_URI, null, null);
            task.pushWaitForTasksHandlerUri();
        }
        if (instruction.startsWorkflowProcess()) {
            if (instruction.isSimple()) {
                prepareActiveWfShadowTask(task, result);
            } else {
                preparePassiveWfShadowTask(task, result);
            }
        }
    }

    private void checkTaskCleanness(Task task) {
        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but this one is persistent; task = " + task);
        }

        if (task.getHandlerUri() != null) {
            throw new IllegalStateException("Workflow-related task should have no handler URI at this moment; task = " + task + ", existing handler URI = " + task.getHandlerUri());
        }
    }


    /**
     * Makes a task active, i.e. a task that actively queries wf process instance about its status.
     * We expect task to be transient at this moment!
     */
    private void prepareActiveWfShadowTask(Task t, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ScheduleType schedule = new ScheduleType();
        schedule.setInterval(workflowManager.getWfConfiguration().getProcessCheckInterval());
        schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(new Date(System.currentTimeMillis() + TASK_START_DELAY)));
        t.pushHandlerUri(WfProcessShadowTaskHandler.HANDLER_URI, schedule, TaskBinding.LOOSE);
    }

    /**
     * Creates a passive task, i.e. a task that stores information received from WfMS about a process instance.
     * We expect task to be transient at this moment!
     */

    private void preparePassiveWfShadowTask(Task t, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        t.pushHandlerUri(WfProcessShadowTaskHandler.HANDLER_URI, new ScheduleType(), null);		// note that this handler will not be used (at least for now)
        t.makeWaiting();
    }

    private void setDefaultTaskOwnerIfEmpty(Task t, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (t.getOwner() == null) {
            t.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), result));
        }
    }

    private void prepareAndSaveChildTask(StartProcessInstruction instruction, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        prepareChildTask(instruction, task, result);
        saveChildTask(task, result);
    }

    private void saveChildTask(Task task, OperationResult result) {
        taskManager.switchToBackground(task, result);
    }

    /*************************** WORKING WITH ACTIVITI ***************************/

    private void startWorkflowProcessInstance(StartProcessInstruction instruction, Task task, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("startWorkflowProcessInstance starting; instruction = " + instruction);
        }

        // perhaps more useful would be state 'workflow process instance creation HAS BEEN requested';
        // however, if we record process state AFTER the request is sent, it is possible
        // that the response would come even before we log the request
        recordProcessState(task, "Workflow process instance creation is being requested.", "", null, result);

        // prepare and send the start process instance command
        StartProcessCommand spc = new StartProcessCommand();
        spc.setTaskOid(task.getOid());
        spc.setProcessName(instruction.getProcessName());
        spc.setSendStartConfirmation(instruction.isSimple());	// for simple processes we should get wrapper-generated start events
        spc.setVariablesFrom(instruction.getProcessVariables());
        spc.setProcessOwner(task.getOwner().getOid());

        try {
            activitiInterface.midpoint2activiti(spc, task, result);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            recordProcessState(task, "Workflow process instance creation could not be requested: " + e, "", null, result);
            result.recordPartialError("Couldn't send a request to start a process instance to workflow management system: " + e.getMessage(), e);
            throw new SystemException("Workflow process instance creation could not be requested", e);
        }

        // final
        result.recordSuccessIfUnknown();

        LOGGER.trace("startWorkflowProcessInstance finished");
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
        recordProcessState(task, description, null, event, result);

        // let us record process id (when getting "process started" event)
        if (event instanceof ProcessStartedEvent) {
            wfTaskUtil.setWfProcessIdImmediate(task, event.getPid(), result);
        }

        // should we finish this task?
        if (event instanceof ProcessFinishedEvent || !event.isRunning()) {
            ChangeProcessor changeProcessor = wfTaskUtil.getChangeProcessor(task, workflowManager.getChangeProcessors());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Calling finishProcess on " + changeProcessor);
            }
            changeProcessor.finishProcess(event, task, result);
            wfTaskUtil.setProcessInstanceFinishedImmediate(task, true, result);

            // todo for passive tasks we should change the task status manually....
        }
    }

    /**
     * Sets a task description to a given text.
     *
     * param oid OID of the task. Must not be null, i.e. task should be already persisted.
     * @param status Description to be set.
     * @param parentResult
     */
    private void recordProcessState(Task task, String status, String defaultDetails, ProcessEvent event,
                            OperationResult parentResult) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("recordProcessState starting.");
        }

        String oid = task.getOid();

        Validate.notEmpty(oid, "Task oid must not be null or empty (task must be persistent).");
        if (parentResult == null) {
            parentResult = new OperationResult("recordProcessState");
        }

        String pid = wfTaskUtil.getProcessId(task);

        // statusTsDt (for wfStatus): [<timestamp>: <formatted datetime>] <status description>
        // (timestamp is to enable easy sorting, [] to easy parsing)
        // statusDt (for OperationResult): only formatted datetime + status description

        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        String statusTsDt = "[" + d.getTime() + ": " + df.format(d) + "] " + status;
        String statusDt = "[" + df.format(d) + "] " + status;

		/*
		 * Let us construct and execute the modification request.
		 */
        try {

            ObjectModificationType modification = new ObjectModificationType();
            modification.setOid(oid);

			/*
			 * (1) Change the task description.
			 */
            task.setDescription(status);

            /*
             * (2) Store the variables from the event.
             */

            Map<String,Object> variables = null;
            if (event != null) {
                variables = getVariablesSorted(event);
                wfTaskUtil.setWfLastVariables(task, dumpVariables(event));
            }

            /*
             * (3) Determine whether to record a change in the process
             */

            boolean recordStatus = true;
//            String details = "";
//
//            if (event != null && pid != null) {
//                PrimaryApprovalProcessWrapper wrapper = wfCore.findProcessWrapper(variables, pid, parentResult);
//                if (wrapper != null) {
//                    details = wrapper.getProcessSpecificDetailsForTask(pid, variables);
//
//                    String lastDetails = getLastDetails(task);
//				    if (lastDetails != null) {
//                        if (LOGGER.isTraceEnabled()) {
//                            LOGGER.trace("currentDetails = " + details);
//                            LOGGER.trace("lastDetails = " + lastDetails);
//                        }
//					    if (lastDetails.equals(details)) {
//						    recordStatus = false;
//					    }
//				    }
//			    }
//            }

            if (recordStatus) {

				/*
				 * (4) Add a record to wfStatus extension element
				 */
                if (USE_WFSTATUS) {
                    wfTaskUtil.addWfStatus(task, statusTsDt);
                }

                /*
                 * (5) Add Last Details information.
                 */

//                PrismProperty wfLastDetailsProperty = wfLastDetailsPropertyDefinition.instantiate();
//                PrismPropertyValue<String> newValue = new PrismPropertyValue<String>(details);
//                wfLastDetailsProperty.setValue(newValue);
//                task.setExtensionProperty(wfLastDetailsProperty);

				/*
				 * (6) Create an operationResult sub-result with the status line and details from wrapper
				 */
                OperationResult or = task.getResult();

//				String ordump = or == null ? null : or.dump();
//				LOGGER.info("-------------------------------------------------------------------");
//				LOGGER.info("Original operation result: " + ordump);

                if (or == null)
                    or = new OperationResult("Task");

                OperationResult sub = or.createSubresult(statusDt);
//				sub.recordStatus(OperationResultStatus.SUCCESS, details);

//				LOGGER.info("-------------------------------------------------------------------");
//				LOGGER.info("Setting new operation result: " + or.dump());

                task.setResult(or);
            }


			/*
			 * Now execute the modification
			 */
            task.savePendingModifications(parentResult);

        } catch (Exception ex) {            // todo
            LoggingUtils.logException(LOGGER, "Couldn't record information from WfMS into task " + oid, ex);
            parentResult.recordFatalError("Couldn't record information from WfMS into task " + oid, ex);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("recordProcessState ending.");
        }
    }

    // variables should be sorted in order for dumpVariables produce nice output
    private Map<String,Object> getVariablesSorted(ProcessEvent event) {
        TreeMap<String,Object> variables = new TreeMap<String,Object>();
        if (event.getVariables() != null) {
            variables.putAll(event.getVariables());
        }
        return variables;
    }
    /*
     * Returns the content of process variables in more-or-less human readable format.
     * Sorts variables according to their names, in order to be able to decide whether
     * anything has changed since last event coming from the process.
     */
    private String dumpVariables(ProcessEvent event)
    {
        StringBuffer sb = new StringBuffer();
        boolean first = true;

//		if (event.getAnswer() != null)
//			sb.append("[answer from workflow: " + event.getAnswer() + "] ");

        Map<String,Object> variables = getVariablesSorted(event);
//		for (WfProcessVariable var : event.getVariables())
//			if (!var.getName().startsWith("midpoint"))
//				variables.put(var.getName(), var.getValue());

        for (Map.Entry<String,Object> entry: variables.entrySet()) {
            if (!first)
                sb.append("; ");
            sb.append(entry.getKey() + "=" + entry.getValue());
            first = false;
        }

        return sb.toString();
    }


}
