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

package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.messages.ActivitiToMidPointMessage;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.ProcessFinishedEvent;
import com.evolveum.midpoint.wf.impl.messages.ProcessStartedEvent;
import com.evolveum.midpoint.wf.impl.messages.StartProcessCommand;
import com.evolveum.midpoint.wf.impl.messages.TaskCompletedEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskCreatedEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Manages everything related to a activiti process instance, including the task that monitors that process instance.
 *
 * This class provides a facade over ugly mess of code managing activiti + task pair describing a workflow process instance.
 *
 * For working with tasks only (e.g. not touching Job structure) it uses wfTaskUtil.
 *
 * @author mederly
 */
@Component
public class JobController {

    private static final Trace LOGGER = TraceManager.getTrace(JobController.class);

    private static final long TASK_START_DELAY = 5000L;
    private static final boolean USE_WFSTATUS = true;
    private static final Object DOT_CLASS = JobController.class.getName() + ".";

    private Set<ProcessListener> processListeners = new HashSet<>();
    private Set<WorkItemListener> workItemListeners = new HashSet<>();

    //region Spring beans
    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ActivitiInterface activitiInterface;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private MiscDataUtil miscDataUtil;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private WorkItemProvider workItemProvider;
    //endregion

    //region Job creation & re-creation
    /**
     * Creates a job, just as prescribed by the job creation instruction.
     *
     * @param instruction the job creation instruction
     * @param parentJob the job that will be the parent of newly created one; it may be null
     */

    public Job createJob(JobCreationInstruction instruction, Job parentJob, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return createJob(instruction, parentJob.getTask(), result);
    }

    /**
     * As before, but this time we know only the parent task (not a job).
     *
     * @param instruction the job creation instruction
     * @param parentTask the task that will be the parent of the task of newly created job; it may be null
     */
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

    /**
     * Re-creates a job, based on existing task information.
     *
     * @param task a task from task-processInstance pair
     * @return recreated job
     */
    public Job recreateJob(Task task) throws SchemaException, ObjectNotFoundException {
        return new Job(this, task, wfTaskUtil.getProcessId(task), wfTaskUtil.getChangeProcessor(task));
    }

    /**
     * Re-creates a child job, knowing the task and the parent job.
     *
     * @param subtask a task from task-processInstance pair
     * @param parentJob the parent job
     * @return recreated job
     */
    public Job recreateChildJob(Task subtask, Job parentJob) {
        return new Job(this, subtask, wfTaskUtil.getProcessId(subtask), parentJob.getChangeProcessor());
    }

    /**
     * Re-creates a root job, based on existing task information. Does not try to find the wf process instance.
     */
    public Job recreateRootJob(Task task) {
        return new Job(this, task, wfTaskUtil.getChangeProcessor(task));
    }
    //endregion

    //region Working with midPoint tasks

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
            wfTaskUtil.setTaskOwner(task, instruction.getTaskOwner());
        }

        // initial execution state
        if (instruction.isCreateTaskAsSuspended() && instruction.isCreateTaskAsWaiting()) {
            throw new IllegalStateException("Both createSuspended and createWaiting attributes are set to TRUE.");
        }
        if (instruction.isCreateTaskAsSuspended()) {
            task.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);
        } else if (instruction.isCreateTaskAsWaiting()) {
            task.setInitialExecutionStatus(TaskExecutionStatus.WAITING);
        }

        if (instruction.getTaskObject() != null) {
            task.setObjectRef(instruction.getTaskObject().getOid(), instruction.getTaskObject().getDefinition().getTypeName());
        } else if (parentTask != null && parentTask.getObjectRef() != null) {
            task.setObjectRef(parentTask.getObjectRef());
        }
        wfTaskUtil.setChangeProcessor(task, changeProcessor);
        wfTaskUtil.setTaskNameIfEmpty(task, instruction.getTaskName());
        wfTaskUtil.setDefaultTaskOwnerIfEmpty(task, result, this);
        task.setCategory(TaskCategory.WORKFLOW);

        // push the handlers, beginning with these that should execute last
        wfTaskUtil.pushHandlers(task, instruction.getHandlersAfterModelOperation());
        if (instruction.isExecuteModelOperationHandler()) {
            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);
        }
        wfTaskUtil.pushHandlers(task, instruction.getHandlersBeforeModelOperation());
        wfTaskUtil.pushHandlers(task, instruction.getHandlersAfterWfProcess());
        if (instruction.startsWorkflowProcess()) {
            wfTaskUtil.pushProcessShadowHandler(instruction.isSimple(), task, TASK_START_DELAY, result);
        }

        // put task variables
        for (Item item : instruction.getTaskVariables().values()) {
            task.setExtensionItem(item);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving workflow monitoring/execution task: " + task.debugDump());
        }

        taskManager.switchToBackground(task, result);

        return task;
    }

    /**
     * Beware, in order to make the change permanent, it is necessary to call commitChanges on
     * "executesFirst". It is advisable not to modify underlying tasks between 'addDependency'
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
        spc.setSendStartConfirmation(instruction.isSendStartConfirmation());
        spc.setVariablesFrom(instruction.getProcessVariables());
        spc.setProcessOwner(task.getOwner().getOid());

        try {
            activitiInterface.midpoint2activiti(spc, task, result);
            auditProcessStart(spc, job, result);
            notifyProcessStart(spc, job, result);
        } catch (JAXBException|SchemaException|RuntimeException e) {
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
     * @param message an event got from workflow engine
     * @param task
     * @param asynchronous
     * @param result
     * @throws Exception
     */
    // TODO fix exceptions list
    public void processWorkflowMessage(ActivitiToMidPointMessage message, Task task, boolean asynchronous, OperationResult result) throws SchemaException, ObjectNotFoundException, WorkflowException, ObjectAlreadyExistsException, JAXBException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Received ActivitiToMidPointMessage: " + message);
        }
        if (message instanceof ProcessEvent) {
            Task task1 = getTaskFromEvent((ProcessEvent) message, task, result);
            if (asynchronous && task1.getExecutionStatus() != TaskExecutionStatus.WAITING) {
                LOGGER.trace("Asynchronous message received in a state different from WAITING (actual state: {}), ignoring it. Task = {}", task1.getExecutionStatus(), task1);
            } else {
                processProcessEvent((ProcessEvent) message, task1, result);
            }
        } else if (message instanceof TaskEvent) {
            processTaskEvent((TaskEvent) message, result);
        } else {
            throw new IllegalStateException("Unknown kind of event from Activiti: " + message.getClass());
        }
    }

    private Task getTaskFromEvent(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

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
        return task;
    }

    private void processProcessEvent(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, JAXBException {

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

    private void processFinishedEvent(ProcessEvent event, Job job, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, JAXBException {
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
        String stateDescription = event.getState();
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

    private ChangeProcessor getChangeProcessor(Map<String,Object> variables) {
        String cpName = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR);
        Validate.notNull(cpName, "Change processor is not defined among process instance variables");
        return wfConfiguration.findChangeProcessor(cpName);
    }

    private ChangeProcessor getChangeProcessor(WorkItemType workItemType) {
        String cpName = workItemType.getChangeProcessor();
        Validate.notNull(cpName, "Change processor is not defined among process instance variables");
        return wfConfiguration.findChangeProcessor(cpName);
    }

    private ChangeProcessor getChangeProcessor(TaskEvent taskEvent) {
        return getChangeProcessor(taskEvent.getVariables());
    }

    //endregion

    //region Processing work item (task) events
    private void processTaskEvent(TaskEvent taskEvent, OperationResult result) throws WorkflowException {

        // auditing & notifications

        if (taskEvent instanceof TaskCreatedEvent) {
            auditWorkItemEvent(taskEvent, AuditEventStage.REQUEST, result);
            try {
                notifyWorkItemCreated(
                        taskEvent.getTaskName(),
                        taskEvent.getAssigneeOid(),
                        taskEvent.getProcessInstanceName(),
                        taskEvent.getVariables());
            } catch (JAXBException|SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't send notification about work item create event", e);
            }
        } else if (taskEvent instanceof TaskCompletedEvent) {
            auditWorkItemEvent(taskEvent, AuditEventStage.EXECUTION, result);
            try {
                notifyWorkItemCompleted(
                        taskEvent.getTaskName(),
                        taskEvent.getAssigneeOid(),
                        taskEvent.getProcessInstanceName(),
                        taskEvent.getVariables(),
                        (String) taskEvent.getVariables().get(CommonProcessVariableNames.FORM_FIELD_DECISION));
            } catch (JAXBException|SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't audit work item complete event", e);
            }
        }
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
        AuditEventRecord auditEventRecord = getChangeProcessor(variables).prepareProcessInstanceAuditRecord(variables, job, stage, result);
        auditService.audit(auditEventRecord, job.getTask());
    }

    private void notifyProcessStart(StartProcessCommand spc, Job job, OperationResult result) throws JAXBException, SchemaException {
        PrismObject<? extends ProcessInstanceState> state = job.getChangeProcessor().externalizeProcessInstanceState(spc.getVariables());
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceStart(state, result);
        }
    }

    private void notifyProcessEnd(ProcessEvent event, Job job, OperationResult result) throws JAXBException, SchemaException {
        PrismObject<? extends ProcessInstanceState> state = job.getChangeProcessor().externalizeProcessInstanceState(event.getVariables());
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceEnd(state, result);
        }
    }

    private void notifyWorkItemCreated(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables) throws JAXBException, SchemaException {
        ChangeProcessor cp = getChangeProcessor(processVariables);
        PrismObject<? extends ProcessInstanceState> state = cp.externalizeProcessInstanceState(processVariables);
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCreation(workItemName, assigneeOid, state);
        }
    }

    private void notifyWorkItemCompleted(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables, String decision) throws JAXBException, SchemaException {
        ChangeProcessor cp = getChangeProcessor(processVariables);
        PrismObject<? extends ProcessInstanceState> state = cp.externalizeProcessInstanceState(processVariables);
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCompletion(workItemName, assigneeOid, state, decision);
        }
    }

    private void auditWorkItemEvent(TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException {

        Task shadowTask;
        try {
            String taskOid = (String) taskEvent.getVariables().get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
            if (taskOid == null) {
                LOGGER.error("Shadow task OID is unknown for work item " + taskEvent.getDebugName() + ", no audit record will be produced.");
                return;
            }
            shadowTask = taskManager.getTask(taskOid, result);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
            return;
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
            return;
        }

        AuditEventRecord auditEventRecord = getChangeProcessor(taskEvent).prepareWorkItemAuditRecord(taskEvent, stage, result);
        auditService.audit(auditEventRecord, shadowTask);
    }

    private String getDebugName(WorkItemType workItemType) {
        return workItemType.getName() + " (id " + workItemType.getWorkItemId() + ")";
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
