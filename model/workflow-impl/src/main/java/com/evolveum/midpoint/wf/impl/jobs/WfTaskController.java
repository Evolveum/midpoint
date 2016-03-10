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
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiInterface;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.messages.*;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.evolveum.midpoint.task.api.TaskExecutionStatus.WAITING;

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
public class WfTaskController {

    private static final Trace LOGGER = TraceManager.getTrace(WfTaskController.class);

    private static final long TASK_START_DELAY = 5000L;
    private static final Object DOT_CLASS = WfTaskController.class.getName() + ".";

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
	private ProcessInterfaceFinder processInterfaceFinder;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private WorkItemProvider workItemProvider;

    @Autowired
    private PrismContext prismContext;
    //endregion

    //region Job creation & re-creation
    /**
     * Creates a job, just as prescribed by the job creation instruction.
     *
     * @param instruction the job creation instruction
     * @param parentWfTask the job that will be the parent of newly created one; it may be null
     */

    public WfTask createWfTask(WfTaskCreationInstruction instruction, WfTask parentWfTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return createWfTask(instruction, parentWfTask.getTask(), result);
    }

    /**
     * As before, but this time we know only the parent task (not a job).
     *
     * @param instruction the job creation instruction
     * @param parentTask the task that will be the parent of the task of newly created job; it may be null
     */
    public WfTask createWfTask(WfTaskCreationInstruction instruction, Task parentTask, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing start instruction: " + instruction.debugDump());
        }

        Task task = createBackgroundTask(instruction, parentTask, result);
        WfTask wfTask = new WfTask(this, task, instruction.getChangeProcessor());
        if (!instruction.isNoProcess()) {
            startWorkflowProcessInstance(wfTask, instruction, result);
        }
        return wfTask;
    }

    /**
     * Re-creates a job, based on existing task information.
     *
     * @param task a task from task-processInstance pair
     * @return recreated job
     */
    public WfTask recreateWfTask(Task task) {
        return new WfTask(this, task, wfTaskUtil.getProcessId(task), wfTaskUtil.getChangeProcessor(task));
    }

    /**
     * Re-creates a child job, knowing the task and the parent job.
     *
     * @param subtask a task from task-processInstance pair
     * @param parentWfTask the parent job
     * @return recreated job
     */
    public WfTask recreateChildWfTask(Task subtask, WfTask parentWfTask) {
        return new WfTask(this, subtask, wfTaskUtil.getProcessId(subtask), parentWfTask.getChangeProcessor());
    }

    /**
     * Re-creates a root job, based on existing task information. Does not try to find the wf process instance.
     */
    public WfTask recreateRootWfTask(Task task) {
        return new WfTask(this, task, wfTaskUtil.getChangeProcessor(task));
    }
    //endregion

    //region Working with midPoint tasks

    private Task createBackgroundTask(WfTaskCreationInstruction instruction, Task parentTask, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ChangeProcessor changeProcessor = instruction.getChangeProcessor();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("createBackgroundTask starting; parent task = " + parentTask);
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
            task.setInitialExecutionStatus(WAITING);
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

        // put model context + task variables
        if (instruction.getTaskModelContext() != null) {
            task.setModelOperationContext(((LensContext) instruction.getTaskModelContext()).toLensContextType());
        }
        instruction.tailorTask(task);

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
    public void addDependency(WfTask executesFirst, WfTask executesSecond) {
        Validate.notNull(executesFirst.getTask());
        Validate.notNull(executesSecond.getTask());
        LOGGER.trace("Setting dependency of {} on 'task0' {}", executesSecond, executesFirst);
        executesFirst.getTask().addDependent(executesSecond.getTask().getTaskIdentifier());
    }

    public void resumeTask(WfTask wfTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        taskManager.resumeTask(wfTask.getTask(), result);
    }

    public void unpauseTask(WfTask wfTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        taskManager.unpauseTask(wfTask.getTask(), result);
    }
    //endregion

    //region Working with Activiti process instances

    private void startWorkflowProcessInstance(WfTask wfTask, WfTaskCreationInstruction instruction, OperationResult parentResult) {

		OperationResult result = parentResult.createSubresult(DOT_CLASS + ".startWorkflowProcessInstance");

        try {
			LOGGER.trace("startWorkflowProcessInstance starting; instruction = {}", instruction);

			Task task = wfTask.getTask();

			StartProcessCommand spc = new StartProcessCommand();
			spc.setTaskOid(task.getOid());
			spc.setProcessName(instruction.getProcessDefinitionKey());
			spc.setProcessInstanceName(instruction.getProcessInstanceName());
			spc.setSendStartConfirmation(instruction.isSendStartConfirmation());
			spc.setVariablesFrom(instruction.getProcessVariables());
			spc.setProcessOwner(task.getOwner().getOid());

			activitiInterface.startActivitiProcessInstance(spc, task, result);
            auditProcessStart(spc, wfTask, result);
            notifyProcessStart(spc, wfTask, result);
        } catch (SchemaException|RuntimeException|ObjectNotFoundException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send a request to start a process instance to workflow management system", e);
			try {
				wfTask.setProcessInstanceState("Workflow process instance creation could not be requested: " + e);
			} catch (SchemaException e1) {
				throw new SystemException(e1);		// never occurs
			}
			result.recordFatalError("Couldn't send a request to start a process instance to workflow management system: " + e.getMessage(), e);
            throw new SystemException("Workflow process instance creation could not be requested", e);
        } finally {
			result.computeStatusIfUnknown();
		}

        LOGGER.trace("startWorkflowProcessInstance finished");
    }

    public void onProcessEvent(ProcessEvent event, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        WfTask wfTask = recreateWfTask(task);

		LOGGER.trace("Updating instance state and activiti process instance ID in task {}", task);

		if (wfTask.getActivitiId() == null) {
			wfTask.setWfProcessId(event.getPid());
		}

		// update state description
		ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(event.getVariables());
		String stateDescription = pmi.getState(event.getVariables());
		if (stateDescription == null || stateDescription.isEmpty()) {
			if (event instanceof ProcessStartedEvent) {
				stateDescription = "Workflow process instance has been created (process id " + event.getPid() + ")";
			} else if (event instanceof ProcessFinishedEvent) {
				stateDescription = "Workflow process instance has ended (process id " + event.getPid() + ")";
			} else {
				stateDescription = "Workflow process instance has proceeded further (process id " + event.getPid() + ")";
			}
		}
		wfTask.setProcessInstanceState(stateDescription);

		wfTask.commitChanges(result);

		if (event instanceof ProcessFinishedEvent || !event.isRunning()) {
            onProcessFinishedEvent(event, wfTask, result);
        }
    }

    private void onProcessFinishedEvent(ProcessEvent event, WfTask wfTask, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        LOGGER.trace("onProcessFinishedEvent starting");
        LOGGER.trace("Calling onProcessEnd on {}", wfTask.getChangeProcessor());
        wfTask.getChangeProcessor().onProcessEnd(event, wfTask, result);
		wfTask.setProcessInstanceFinishedImmediate(result);

        auditProcessEnd(event, wfTask, result);
        notifyProcessEnd(event, wfTask, result);

        // passive tasks can be 'let go' at this point
        if (wfTask.getTaskExecutionStatus() == WAITING) {
            wfTask.computeTaskResultIfUnknown(result);
            wfTask.removeCurrentTaskHandlerAndUnpause(result);            // removes WfProcessInstanceShadowTaskHandler
        }
        LOGGER.trace("onProcessFinishedEvent done");
    }

    private ChangeProcessor getChangeProcessor(Map<String,Object> variables) {
        String cpName = (String) variables.get(CommonProcessVariableNames.VARIABLE_CHANGE_PROCESSOR);
        Validate.notNull(cpName, "Change processor is not defined among process instance variables");
        return wfConfiguration.findChangeProcessor(cpName);
    }

    private ChangeProcessor getChangeProcessor(TaskEvent taskEvent) {
        return getChangeProcessor(taskEvent.getVariables());
    }

    //endregion

    //region Processing work item (task) events
    public void onTaskEvent(TaskEvent taskEvent, OperationResult result) throws WorkflowException {

        // auditing & notifications
        if (taskEvent instanceof TaskCreatedEvent) {
            auditWorkItemEvent(taskEvent, AuditEventStage.REQUEST, result);
            try {
                notifyWorkItemCreated(
                        taskEvent.getTaskName(),
                        taskEvent.getAssigneeOid(),
                        taskEvent.getProcessInstanceName(),
                        taskEvent.getVariables());
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send notification about work item create event", e);
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
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't audit work item complete event", e);
            }
        }
    }
    //endregion

    //region Auditing and notifications
    private void auditProcessStart(StartProcessCommand spc, WfTask wfTask, OperationResult result) {
        auditProcessStartEnd(spc.getVariables(), wfTask, AuditEventStage.REQUEST, result);
    }

    private void auditProcessEnd(ProcessEvent event, WfTask wfTask, OperationResult result) {
        auditProcessStartEnd(event.getVariables(), wfTask, AuditEventStage.EXECUTION, result);
    }

    private void auditProcessStartEnd(Map<String,Object> variables, WfTask wfTask, AuditEventStage stage, OperationResult result) {
        AuditEventRecord auditEventRecord = getChangeProcessor(variables).prepareProcessInstanceAuditRecord(variables, wfTask, stage, result);
        auditService.audit(auditEventRecord, wfTask.getTask());
    }

    private void notifyProcessStart(StartProcessCommand spc, WfTask wfTask, OperationResult result) throws SchemaException {
        PrismObject<? extends ProcessInstanceState> state = wfTask.getChangeProcessor().externalizeProcessInstanceState(spc.getVariables());
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceStart(state, result);
        }
    }

    private void notifyProcessEnd(ProcessEvent event, WfTask wfTask, OperationResult result) throws SchemaException {
        PrismObject<? extends ProcessInstanceState> state = wfTask.getChangeProcessor().externalizeProcessInstanceState(event.getVariables());
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceEnd(state, result);
        }
    }

    private void notifyWorkItemCreated(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables) throws SchemaException {
        ChangeProcessor cp = getChangeProcessor(processVariables);
        PrismObject<? extends ProcessInstanceState> state = cp.externalizeProcessInstanceState(processVariables);
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCreation(workItemName, assigneeOid, state);
        }
    }

    private void notifyWorkItemCompleted(String workItemName, String assigneeOid, String processInstanceName, Map<String,Object> processVariables, String decision) throws SchemaException {
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

//    private String getDebugName(WorkItemType workItemType) {
//        return workItemType.getName() + " (id " + workItemType.getWorkItemId() + ")";
//    }

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

    public PrismContext getPrismContext() {
        return prismContext;
    }

    //endregion
}
