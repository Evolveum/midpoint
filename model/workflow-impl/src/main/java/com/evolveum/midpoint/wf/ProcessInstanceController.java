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

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.ProcessFinishedEvent;
import com.evolveum.midpoint.wf.messages.ProcessStartedEvent;
import com.evolveum.midpoint.wf.messages.StartProcessCommand;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.WorkflowResult;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.taskHandlers.WfPrepareChildOperationTaskHandler;
import com.evolveum.midpoint.wf.taskHandlers.WfProcessInstanceShadowTaskHandler;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.*;

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

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ActivitiInterface activitiInterface;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private MiscDataUtil miscDataUtil;

    private Set<ProcessListener> processListeners = new HashSet<ProcessListener>();
    private Set<WorkItemListener> workItemListeners = new HashSet<WorkItemListener>();

//    @PostConstruct
//    public void init() {
//        registerProcessListener(workflowListener);
//        registerWorkItemListener(workflowListener);
//    }

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

        checkTaskCleanness(task, false);

        wfTaskUtil.setTaskNameIfEmpty(task, instruction.getTaskName());
        setDefaultTaskOwnerIfEmpty(task, result);
        task.setCategory(TaskCategory.WORKFLOW);

        // push the handlers
        if (instruction.isExecuteImmediately()) {
            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);

            if (instruction.startsWorkflowProcess()) {
                task.pushHandlerUri(WfPrepareChildOperationTaskHandler.HANDLER_URI, null, null);
                task.pushWaitForTasksHandlerUri();
            }
        }
        if (instruction.startsWorkflowProcess()) {
            pushProcessShadowHandler(instruction.isSimple(), task, result);
        }
    }

    private void checkTaskCleanness(Task task, boolean noHandlers) {
        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but this one is persistent; task = " + task);
        }

        if (noHandlers && task.getHandlerUri() != null) {
            throw new IllegalStateException("Workflow-related task should have no handler URI at this moment; task = " + task + ", existing handler URI = " + task.getHandlerUri());
        }
    }


    /**
     * Makes a task active, i.e. a task that actively queries wf process instance about its status.
     *
     *          OR
     *
     * Creates a passive task, i.e. a task that stores information received from WfMS about a process instance.
     *
     * We expect task to be transient at this moment!
     */
    private void pushProcessShadowHandler(boolean active, Task t, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (active) {

            ScheduleType schedule = new ScheduleType();
            schedule.setInterval(wfConfiguration.getProcessCheckInterval());
            schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(new Date(System.currentTimeMillis() + TASK_START_DELAY)));
            t.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, schedule, TaskBinding.LOOSE);

        } else {

            t.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, new ScheduleType(), null);		// note that this handler will not be actively used (at least for now)
            t.makeWaiting();
        }
    }


    private void setDefaultTaskOwnerIfEmpty(Task t, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (t.getOwner() == null) {
            t.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));
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
        //spc.setSendStartConfirmation(instruction.isSimple());	// for simple processes we should get wrapper-generated start events
        spc.setSendStartConfirmation(true);     // we always want to get start confirmation
        spc.setVariablesFrom(instruction.getProcessVariables());
        spc.setProcessOwner(task.getOwner().getOid());

        try {
            activitiInterface.midpoint2activiti(spc, task, result);
            auditProcessStart(spc, task, result);
            notifyProcessStart(spc, task, result);
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

    private void auditProcessStart(StartProcessCommand spc, Task task, OperationResult result) {
        auditProcessStartEnd(spc.getVariables(), task, AuditEventStage.REQUEST, result);
    }

    private void auditProcessEnd(ProcessEvent event, Task task, OperationResult result) {
        auditProcessStartEnd(event.getVariables(), task, AuditEventStage.EXECUTION, result);
    }

    private void auditProcessStartEnd(Map<String,Object> variables, Task task, AuditEventStage stage, OperationResult result) {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        auditEventRecord.setEventStage(stage);

        String requesterOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        if (requesterOid != null) {
            try {
                auditEventRecord.setInitiator(miscDataUtil.getRequester(variables, result));
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            }
        }

        PrismObject<GenericObjectType> processInstanceObject = new PrismObject<GenericObjectType>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        processInstanceObject.asObjectable().setName(new PolyStringType((String) variables.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME)));
        processInstanceObject.asObjectable().setOid(wfTaskUtil.getProcessId(task));
        auditEventRecord.setTarget(processInstanceObject);

        List<ObjectDelta<Objectable>> deltas = null;
        try {
            if (stage == AuditEventStage.REQUEST) {
                deltas = wfTaskUtil.retrieveDeltasToProcess(task);
            } else {
                deltas = wfTaskUtil.retrieveResultingDeltas(task);
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta(s) from task " + task, e);
        }
        if (deltas != null) {
            for (ObjectDelta delta : deltas) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        }

        if (stage == AuditEventStage.EXECUTION) {
            auditEventRecord.setResult((String) variables.get(CommonProcessVariableNames.VARIABLE_WF_ANSWER));
        }
        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);
        auditService.audit(auditEventRecord, task);
    }

    private void notifyProcessStart(StartProcessCommand spc, Task task, OperationResult result) {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceStart((String) spc.getVariables().get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME), spc.getVariables(), result);
        }
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

        // let us record process id (if unknown or when getting "process started" event)
        if (wfTaskUtil.getProcessId(task) == null || event instanceof ProcessStartedEvent) {
            wfTaskUtil.setWfProcessIdImmediate(task, event.getPid(), result);
        }

        // should we finish this task?
        if (event instanceof ProcessFinishedEvent || !event.isRunning()) {
            ChangeProcessor changeProcessor = wfTaskUtil.getChangeProcessor(task);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Calling finishProcess on " + changeProcessor);
            }
            changeProcessor.finishProcess(event, task, result);
            wfTaskUtil.setProcessInstanceFinishedImmediate(task, true, result);

            auditProcessEnd(event, task, result);
            notifyProcessEnd(event, task, result);

            // passive tasks can be 'let go' at this point
            if (task.getExecutionStatus() == TaskExecutionStatus.WAITING) {

                // similar code for passive tasks is in WfProcessShadowTaskHandler
                // todo fix this
                OperationResult taskResult = task.getResult();
                if (result.isUnknown()) {
                    result.computeStatus();
                }
                taskResult.recordStatus(result.getStatus(), result.getMessage(), result.getCause());
                task.setResultImmediate(taskResult, result);

                task.finishHandler(result);

                // if the task was not closed ... (i.e. if there are other handler(s) on the stack)
                if (task.getExecutionStatus() == TaskExecutionStatus.WAITING) {
                    taskManager.unpauseTask(task, result);
                }
            }
        }
    }

    private void notifyProcessEnd(ProcessEvent event, Task task, OperationResult result) {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceEnd((String) event.getVariables().get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
                    event.getVariables(), (String) event.getVariables().get(CommonProcessVariableNames.VARIABLE_WF_ANSWER), result);
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

                wfTaskUtil.setLastDetails(task, status);
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

        Map<String,Object> variables = getVariablesSorted(event);

        for (Map.Entry<String,Object> entry: variables.entrySet()) {
            if (!first)
                sb.append("; ");
            sb.append(entry.getKey() + "=" + entry.getValue());
            first = false;
        }

        return sb.toString();
    }

    public void registerProcessListener(ProcessListener processListener) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registering process listener " + processListener);
        }
        processListeners.add(processListener);
    }

    public void registerWorkItemListener(WorkItemListener workItemListener) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registering work item listener " + workItemListener);
        }
        workItemListeners.add(workItemListener);
    }

    public void notifyWorkItemCreated(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables) {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCreation(workItemName, assigneeOid, processInstanceName, processVariables);
        }
    }

    public void notifyWorkItemCompleted(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables, String decision) {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCompletion(workItemName, assigneeOid, processInstanceName, processVariables, decision);
        }
    }

}
