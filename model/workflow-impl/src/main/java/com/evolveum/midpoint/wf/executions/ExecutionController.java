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

package com.evolveum.midpoint.wf.executions;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
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
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.ProcessFinishedEvent;
import com.evolveum.midpoint.wf.messages.ProcessStartedEvent;
import com.evolveum.midpoint.wf.messages.StartProcessCommand;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.taskHandlers.WfPrepareChildOperationTaskHandler;
import com.evolveum.midpoint.wf.taskHandlers.WfProcessInstanceShadowTaskHandler;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UriStackEntry;
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
 * This class provides a facade over ugly mess of code managing activiti + task pair describing a workflow process instance.
 *
 * @author mederly
 */
@Component
public class ExecutionController {

    private static final Trace LOGGER = TraceManager.getTrace(ExecutionController.class);

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

    /**
     * Beware, in order to make the change permanent, it is necessary to call commitDependencies on
     * "executesFirst". It is advisable not to modify underlying tasks between 'addDependency'
     * and 'commitDependencies' because of the savePendingModifications() mechanism that is used here.
     *
     * @param executesFirst
     * @param executesSecond
     */
    public void addDependency(Execution executesFirst, Execution executesSecond) {
        Validate.notNull(executesFirst.getTask());
        Validate.notNull(executesSecond.getTask());
        LOGGER.trace("Setting dependency of {} on 'task0' {}", executesSecond, executesFirst);
        executesFirst.getTask().addDependent(executesSecond.getTask().getTaskIdentifier());
    }

    public void commitDependencies(Execution execution, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        Validate.notNull(execution.getTask());
        execution.getTask().savePendingModifications(result);
    }

    public void resume(Execution execution, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Validate.notNull(execution.getTask());
        taskManager.resumeTask(execution.getTask(), result);
    }

    public interface TaskCustomizer {
        void customizeAfterCreation(Task childTask, StartInstruction instruction, OperationResult result) throws SchemaException;
        void customizeBeforePersisting(Task childTask, StartInstruction instruction, OperationResult result) throws SchemaException;
    }

    /**
     * Starts the process instance, just as prescribed by the start process instruction.
     *
     * @param instruction
     * @param task
     * @param customizerBefore Code that will customize child task.
     * @param result
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */



    public Execution startExecution(StartInstruction instruction, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing start instruction: " + instruction.debugDump());
        }

        Task childTask = createChildTask(instruction, result);
        Execution execution = new Execution(instruction.getExecutionContext(), childTask);
        if (!instruction.isNoProcess()) {
            startWorkflowProcessInstance(execution, instruction, result);
        }
        return execution;
    }


    /*************************** WORKING WITH TASKS ***************************/

    private Task createChildTask(StartInstruction instruction, OperationResult result) throws SchemaException, ObjectNotFoundException {

        Task rootTask = instruction.getExecutionContext().getParentTask();
        ChangeProcessor changeProcessor = instruction.getExecutionContext().getChangeProcessor();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("createChildTask starting; root task = " + rootTask);
        }

        Task task = rootTask.createSubtask();

        if (instruction.isCreateSuspended()) {
            task.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);
        }

        if (rootTask.getObjectRef() != null) {
            task.setObjectRef(rootTask.getObjectRef());
        }
        wfTaskUtil.setChangeProcessor(task, changeProcessor);
        wfTaskUtil.setTaskNameIfEmpty(task, instruction.getTaskName());
        setDefaultTaskOwnerIfEmpty(task, result);
        task.setCategory(TaskCategory.WORKFLOW);

        // push the handlers, beginning with these that should execute last
        List<UriStackEntry> handlersLast = instruction.getHandlersLast();
        for (int i = instruction.getHandlersLast().size()-1; i >= 0; i--) {
            UriStackEntry entry = handlersLast.get(i);
            if (!entry.getExtensionDelta().isEmpty()) {
                throw new UnsupportedOperationException("handlersLast with extension delta set is not supported yet");
            }
            task.pushHandlerUri(entry.getHandlerUri(), entry.getSchedule(), TaskBinding.fromTaskType(entry.getBinding()), (ItemDelta) null);
        }

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

        // put task variables
        for (Item item : instruction.getTaskVariables().values()) {
            task.setExtensionItem(item);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving workflow monitoring/execution task: " + task.dump());
        }

        taskManager.switchToBackground(task, result);

        return task;
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


    /*************************** WORKING WITH ACTIVITI ***************************/

    private void startWorkflowProcessInstance(Execution execution, StartInstruction instruction, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("startWorkflowProcessInstance starting; instruction = " + instruction);
        }

        Task task = execution.getTask();

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
