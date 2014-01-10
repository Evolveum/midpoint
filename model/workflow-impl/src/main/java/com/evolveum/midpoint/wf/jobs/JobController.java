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

package com.evolveum.midpoint.wf.jobs;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
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
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UriStackEntry;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Manages everything related to a activiti process instance, including the task that monitors that process instance.
 *
 * This class provides a facade over ugly mess of code managing activiti + task pair describing a workflow process instance.
 *
 * @author mederly
 */
@Component
public class JobController {

    private static final Trace LOGGER = TraceManager.getTrace(JobController.class);

    private static final long TASK_START_DELAY = 5000L;
    private static final boolean USE_WFSTATUS = true;

    public static final String WAIT_FOR_TASKS_HANDLER_URI = "<<< marker for calling pushWaitForTasksHandlerUri >>>";

    private Set<ProcessListener> processListeners = new HashSet<ProcessListener>();
    private Set<WorkItemListener> workItemListeners = new HashSet<WorkItemListener>();

    //region Spring beans
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
    //endregion

    //region Job creation & re-creation
    /**
     * Starts the process instance, just as prescribed by the start process instruction.
     *
     * @param instruction
     * @param parentTask may be null
     * @param result
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */

    public Job createJob(JobCreationInstruction instruction, Job parentJob, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return createJob(instruction, parentJob.getTask(), result);
    }

    public Job createJob(JobCreationInstruction instruction, Task parentTask, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing start instruction: " + instruction.debugDump());
        }

        Task task = createTask(instruction, parentTask, result);
        Job job = new Job(this, task, instruction.getChangeProcessor());
        if (!instruction.isNoProcess()) {
            startWorkflowProcessInstance(job, instruction, result);
        }
        return job;
    }

    public Job recreateJob(Task task) throws SchemaException, ObjectNotFoundException {
        return new Job(this, task, wfTaskUtil.getProcessId(task), wfTaskUtil.getChangeProcessor(task));
    }

    public Job recreateChildJob(Task subtask, Job parentJob) {
        return new Job(this, subtask, wfTaskUtil.getProcessId(subtask), parentJob.getChangeProcessor());
    }

    public Job recreateRootJob(Task task) {
        return new Job(this, task, wfTaskUtil.getChangeProcessor(task));
    }
    //endregion

    //region Working with midPoint tasks
    /*************************** WORKING WITH TASKS ***************************/

    private Task createTask(JobCreationInstruction instruction, Task parentTask, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ChangeProcessor changeProcessor = instruction.getChangeProcessor();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("createTask starting; parent task = " + parentTask);
        }

        Task task;
        if (parentTask != null) {
            task = parentTask.createSubtask();
        } else {
            task = taskManager.createTaskInstance();
            setTaskOwner(task, instruction.getTaskOwner());
        }

        // initial execution state
        if (instruction.isCreateSuspended() && instruction.isCreateWaiting()) {
            throw new IllegalStateException("Both createSuspended and createWaiting attributes are set to TRUE.");
        }
        if (instruction.isCreateSuspended()) {
            task.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);
        } else if (instruction.isCreateWaiting()) {
            task.setInitialExecutionStatus(TaskExecutionStatus.WAITING);
        }

        if (instruction.getTaskObject() != null) {
            task.setObjectRef(instruction.getTaskObject().getOid(), instruction.getTaskObject().getDefinition().getTypeName());
        } else if (parentTask != null && parentTask.getObjectRef() != null) {
            task.setObjectRef(parentTask.getObjectRef());
        }
        wfTaskUtil.setChangeProcessor(task, changeProcessor);
        wfTaskUtil.setTaskNameIfEmpty(task, instruction.getTaskName());
        setDefaultTaskOwnerIfEmpty(task, result);
        task.setCategory(TaskCategory.WORKFLOW);

        // push the handlers, beginning with these that should execute last
        pushHandlers(task, instruction.getHandlersAfterModelOperation());
        if (instruction.isExecuteModelOperationHandler()) {
            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);
        }
        pushHandlers(task, instruction.getHandlersBeforeModelOperation());
        pushHandlers(task, instruction.getHandlersAfterWfProcess());
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

    private void setTaskOwner(Task task, PrismObject<UserType> owner) {
        if (owner == null) {
            throw new IllegalStateException("Couldn't create a job task because the owner is not set.");
        }
        task.setOwner(owner);
    }

    // handlers are stored in the list in the order they should be executed; so the last one has to be pushed first
    private void pushHandlers(Task task, List<UriStackEntry> handlers) {
        for (int i = handlers.size()-1; i >= 0; i--) {
            UriStackEntry entry = handlers.get(i);
            if (WAIT_FOR_TASKS_HANDLER_URI.equals(entry.getHandlerUri())) {
                task.pushWaitForTasksHandlerUri();
            } else {
                if (!entry.getExtensionDelta().isEmpty()) {
                    throw new UnsupportedOperationException("handlers with extension delta set are not supported yet");
                }
                task.pushHandlerUri(entry.getHandlerUri(), entry.getSchedule(), TaskBinding.fromTaskType(entry.getBinding()), (ItemDelta) null);
            }
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

    /**
     * Beware, in order to make the change permanent, it is necessary to call commitChanges on
     * "executesFirst". It is advisable not to modify underlying tasks between 'addDependent'
     * and 'commitChanges' because of the savePendingModifications() mechanism that is used here.
     *
     * @param executesFirst
     * @param executesSecond
     */
    public void addDependency(Job executesFirst, Job executesSecond) {
        Validate.notNull(executesFirst.getTask());
        Validate.notNull(executesSecond.getTask());
        LOGGER.trace("Setting dependency of {} on 'task0' {}", executesSecond, executesFirst);
        executesFirst.getTask().addDependent(executesSecond.getTask().getTaskIdentifier());
    }

    public void resumeTask(Job job, OperationResult result) throws SchemaException, ObjectNotFoundException {
        taskManager.resumeTask(job.getTask(), result);
    }

    public void unpauseTask(Job job, OperationResult result) throws SchemaException, ObjectNotFoundException {
        taskManager.unpauseTask(job.getTask(), result);
    }

    //endregion

    //region Working with Activiti process instances
    /*************************** WORKING WITH ACTIVITI ***************************/

    private void startWorkflowProcessInstance(Job job, JobCreationInstruction instruction, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("startWorkflowProcessInstance starting; instruction = " + instruction);
        }

        Task task = job.getTask();

        // perhaps more useful would be state 'workflow process instance creation HAS BEEN requested';
        // however, if we record process state AFTER the request is sent, it is possible
        // that the response would come even before we log the request
        recordProcessInstanceState(job, "Workflow process instance creation is being requested.", null, result);

        // prepare and send the start process instance command
        StartProcessCommand spc = new StartProcessCommand();
        spc.setTaskOid(task.getOid());
        spc.setProcessName(instruction.getProcessDefinitionKey());
        spc.setSendStartConfirmation(true);     // we always want to get start confirmation
        spc.setVariablesFrom(instruction.getProcessVariables());
        spc.setProcessOwner(task.getOwner().getOid());

        try {
            activitiInterface.midpoint2activiti(spc, task, result);
            auditProcessStart(spc, job, result);
            notifyProcessStart(spc, job, result);
        } catch (RuntimeException e) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't send a request to start a process instance to workflow management system", e);
            recordProcessInstanceState(job, "Workflow process instance creation could not be requested: " + e, null, result);
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
     * @param task a wf-monitoring task instance (should be as current as possible)
     * @param result
     * @throws Exception
     */
    public void processWorkflowMessage(ProcessEvent event, Task task, OperationResult result) throws Exception {

        Job job = recreateJob(task);

        recordProcessInstanceState(job, getStateDescription(event), event, result);

        // let us record process id (if unknown or when getting "process started" event)
        if (job.getActivitiId() == null || event instanceof ProcessStartedEvent) {
            job.setWfProcessIdImmediate(event.getPid(), result);
        }

        // should we finish this task?
        if (event instanceof ProcessFinishedEvent || !event.isRunning()) {
            processFinishedEvent(event, job, result);
        }
    }

    private void processFinishedEvent(ProcessEvent event, Job job, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        LOGGER.trace("processFinishedEvent starting");
        LOGGER.trace("Calling onProcessEnd on {}", job.getChangeProcessor());
        job.getChangeProcessor().onProcessEnd(event, job, result);

        job.setProcessInstanceFinishedImmediate(true, result);

        auditProcessEnd(event, job, result);
        notifyProcessEnd(event, job, result);

        // passive tasks can be 'let go' at this point
        if (job.getTaskExecutionStatus() == TaskExecutionStatus.WAITING) {
            job.computeTaskResultIfUnknown(result);
            job.removeCurrentTaskHandlerAndUnpause(result);            // removes WfProcessInstanceShadowTaskHandler
        }
        LOGGER.trace("processFinishedEvent done");
    }

    private String getStateDescription(ProcessEvent event) {
        String pid = event.getPid();
        String stateDescription = (String) event.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_STATE);
        if (stateDescription == null || stateDescription.isEmpty()) {
            if (event instanceof ProcessStartedEvent) {
                stateDescription = "Workflow process instance has been created (process id " + pid + ")";
            } else if (event instanceof ProcessFinishedEvent) {
                stateDescription = "Workflow process instance has ended (process id " + pid + ")";
            } else {
                stateDescription = "Workflow process instance has proceeded further (process id " + pid + ")";
            }
        }
        return stateDescription;
    }

    private void recordProcessInstanceState(Job job, String stateDescription, ProcessEvent event, OperationResult parentResult) {
        LOGGER.trace("recordProcessInstanceState starting.");
        Task task = job.getTask();
        try {
            task.setDescription(stateDescription);
            if (event != null) {
                wfTaskUtil.setWfLastVariables(task, dumpVariables(event));
            }
            if (USE_WFSTATUS) {
                wfTaskUtil.addWfStatus(task, prepareValueForWfStatusProperty(stateDescription));
            }
            wfTaskUtil.setLastDetails(task, stateDescription);
            task.savePendingModifications(parentResult);
        } catch (Exception ex) {            // todo
            LoggingUtils.logException(LOGGER, "Couldn't record information from WfMS into task {}", ex, task);
            parentResult.recordFatalError("Couldn't record information from WfMS into task " + task, ex);
        }
        LOGGER.trace("recordProcessInstanceState ending.");
    }

    private String prepareValueForWfStatusProperty(String stateDescription) {
        // statusTsDt (for wfStatus): [<timestamp>: <formatted datetime>] <status description>
        // (timestamp is to enable easy sorting, [] to easy parsing)

        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        return "[" + d.getTime() + ": " + df.format(d) + "] " + stateDescription;
    }

    // variables should be sorted in order for dumpVariables produce nice output
    private Map<String,Object> getVariablesSorted(ProcessEvent event) {
        TreeMap<String,Object> variables = new TreeMap<String,Object>();
        if (event.getVariables() != null) {
            variables.putAll(event.getVariables());
        }
        return variables;
    }

    // Returns the content of process variables in more-or-less human readable format.
    // Sorts variables according to their names, in order to be able to decide whether
    // anything has changed since last event coming from the process.
    private String dumpVariables(ProcessEvent event) {

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

    //endregion

    //region Auditing and notifications
    private void auditProcessStart(StartProcessCommand spc, Job job, OperationResult result) {
        auditProcessStartEnd(spc.getVariables(), job, AuditEventStage.REQUEST, result);
    }

    private void auditProcessEnd(ProcessEvent event, Job job, OperationResult result) {
        auditProcessStartEnd(event.getVariables(), job, AuditEventStage.EXECUTION, result);
    }

    private void auditProcessStartEnd(Map<String,Object> variables, Job job, AuditEventStage stage, OperationResult result) {

        Task task = job.getTask();

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
        processInstanceObject.asObjectable().setOid(job.getActivitiId());
        auditEventRecord.setTarget(processInstanceObject);

        // todo try to retrieve delta(s) only if applicable (e.g. not when using GeneralChangeProcessor)
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

    private void notifyProcessStart(StartProcessCommand spc, Job job, OperationResult result) {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceStart((String) spc.getVariables().get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME), spc.getVariables(), result);
        }
    }

    private void notifyProcessEnd(ProcessEvent event, Job job, OperationResult result) {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceEnd((String) event.getVariables().get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
                    event.getVariables(), (String) event.getVariables().get(CommonProcessVariableNames.VARIABLE_WF_ANSWER), result);
        }
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

    public void auditWorkItemEvent(Map<String,Object> variables, String taskId, String workItemName, String assigneeOid, AuditEventStage stage, OperationResult result) {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORK_ITEM);
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

        PrismObject<GenericObjectType> workItemObject = new PrismObject<GenericObjectType>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        workItemObject.asObjectable().setName(new PolyStringType(workItemName));
        workItemObject.asObjectable().setOid(taskId);
        auditEventRecord.setTarget(workItemObject);

        if (stage == AuditEventStage.REQUEST) {
            if (assigneeOid != null) {
                try {
                    auditEventRecord.setTargetOwner(repositoryService.getObject(UserType.class, assigneeOid, null, result));
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                }
            }
        } else {
            MidPointPrincipal principal = MiscDataUtil.getPrincipalUser();
            if (principal != null && principal.getUser() != null) {
                auditEventRecord.setTargetOwner(principal.getUser().asPrismObject());
            }
        }

        ObjectDelta delta;
        try {
            delta = miscDataUtil.getObjectDelta(variables, result, true);
            if (delta != null) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        } catch (JAXBException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta to be approved", e);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta to be approved", e);
        }

        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);
        if (stage == AuditEventStage.EXECUTION) {
            auditEventRecord.setResult((String) variables.get(CommonProcessVariableNames.FORM_FIELD_DECISION));
            auditEventRecord.setMessage((String) variables.get(CommonProcessVariableNames.FORM_FIELD_COMMENT));
        }

        Task shadowTask = null;
        try {
            shadowTask = miscDataUtil.getShadowTask(variables, result);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
        }

        auditService.audit(auditEventRecord, shadowTask);
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
    //endregion

    //region Getters and setters
    public WfTaskUtil getWfTaskUtil() {
        return wfTaskUtil;
    }


    //endregion
}
