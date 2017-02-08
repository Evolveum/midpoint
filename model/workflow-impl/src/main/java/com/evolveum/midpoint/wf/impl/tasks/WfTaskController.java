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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.wf.impl.messages.*;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNotificationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author mederly
 */
@Component
public class WfTaskController {

    private static final Trace LOGGER = TraceManager.getTrace(WfTaskController.class);

    public static final long TASK_START_DELAY = 5000L;
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
    private AuditService auditService;

    @Autowired
    private MiscDataUtil miscDataUtil;

	@Autowired
	private ProcessInterfaceFinder processInterfaceFinder;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private PrismContext prismContext;
    //endregion

    //region Job creation & re-creation
    /**
     * Creates a background task, just as prescribed by the task creation instruction.
     *  @param instruction the job creation instruction
     * @param parentWfTask the job that will be the parent of newly created one; it may be null
	 * @param wfConfigurationType
	 */

    public WfTask submitWfTask(WfTaskCreationInstruction instruction, WfTask parentWfTask, WfConfigurationType wfConfigurationType,
			OperationResult result) throws SchemaException, ObjectNotFoundException {
        return submitWfTask(instruction, parentWfTask.getTask(), wfConfigurationType, null, result);
    }

    /**
     * As before, but this time we know only the parent task (not a job).
     *  @param instruction the job creation instruction
     * @param parentTask the task that will be the parent of the task of newly created job; it may be null
	 * @param wfConfigurationType
	 */
    public WfTask submitWfTask(WfTaskCreationInstruction instruction, Task parentTask, WfConfigurationType wfConfigurationType,
			String channelOverride, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing start instruction:\n{}", instruction.debugDump());
        }
        Task task = submitTask(instruction, parentTask, wfConfigurationType, channelOverride, result);
		WfTask wfTask = recreateWfTask(task, instruction.getChangeProcessor());
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
		return recreateWfTask(task, wfTaskUtil.getChangeProcessor(task));
	}

    public WfTask recreateWfTask(Task task, ChangeProcessor changeProcessor) {
		String processInstanceId = wfTaskUtil.getProcessId(task);
		if (changeProcessor instanceof PrimaryChangeProcessor) {
			return new PcpWfTask(this, task, processInstanceId, changeProcessor);
		} else {
			return new WfTask(this, task, processInstanceId, changeProcessor);
		}
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

    private Task submitTask(WfTaskCreationInstruction instruction, Task parentTask, WfConfigurationType wfConfigurationType, String channelOverride, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Task wfTask = instruction.createTask(this, parentTask, wfConfigurationType);
		if (channelOverride != null) {
			wfTask.setChannel(channelOverride);
		}
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Switching workflow root or child task to background:\n{}", wfTask.debugDump());
        }
        taskManager.switchToBackground(wfTask, result);
        return wfTask;
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

    private void startWorkflowProcessInstance(WfTask wfTask, WfTaskCreationInstruction<?,?> instruction, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(DOT_CLASS + "startWorkflowProcessInstance");
        try {
			LOGGER.trace("startWorkflowProcessInstance starting; instruction = {}", instruction);
			Task task = wfTask.getTask();

			StartProcessCommand spc = new StartProcessCommand();
			spc.setProcessName(instruction.getProcessName());
			spc.setProcessInstanceName(instruction.getProcessInstanceName());
			spc.setSendStartConfirmation(instruction.isSendStartConfirmation());
			spc.setVariablesFrom(instruction.getAllProcessVariables());
			spc.addVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID, task.getOid());
			spc.setProcessOwner(task.getOwner().getOid());

			activitiInterface.startActivitiProcessInstance(spc, task, result);
            auditProcessStart(wfTask, spc.getVariables(), result);
            notifyProcessStart(wfTask, result);
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

		if (wfTask.getProcessInstanceId() == null) {
			wfTask.setWfProcessId(event.getPid());
		}

		Map<String, Object> variables = event.getVariables();

		// update state description
		ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(variables);
		String stateDescription = pmi.getState(variables);
		if (stateDescription == null || stateDescription.isEmpty()) {
			if (event instanceof ProcessStartedEvent) {
				stateDescription = "Approval process has been started";
			} else if (event instanceof ProcessFinishedEvent) {
				stateDescription = "Approval process has finished";
			} else {
				stateDescription = null;
			}
		}
        if (stateDescription != null) {
            wfTask.setProcessInstanceState(stateDescription);
        }
        wfTask.setProcessInstanceStageInformation(pmi.getStageNumber(variables), pmi.getStageCount(variables),
				pmi.getStageName(variables), pmi.getStageDisplayName(variables));

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
		wfTask.setProcessInstanceEndTimestamp();
		wfTask.setAnswer(event.getAnswer());			// TODO or should we do this on each process event?
		wfTask.commitChanges(result);

        auditProcessEnd(wfTask, event, result);
        notifyProcessEnd(wfTask, result);

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

	// workItem contains taskRef, assignee, candidates resolved (if possible)
	@SuppressWarnings("unchecked")
    public void onTaskEvent(WorkItemType workItem, TaskEvent taskEvent, OperationResult result) throws WorkflowException, SchemaException {

		final TaskType shadowTaskType = (TaskType) ObjectTypeUtil.getObjectFromReference(workItem.getTaskRef());
		if (shadowTaskType == null) {
			LOGGER.warn("No task in workItem " + workItem + ", audit and notifications couldn't be performed.");
			return;
		}
		final Task shadowTask = taskManager.createTaskInstance(shadowTaskType.asPrismObject(), result);
		final WfTask wfTask = recreateWfTask(shadowTask);

		// auditing & notifications
        if (taskEvent instanceof TaskCreatedEvent) {
            auditWorkItemEvent(workItem, wfTask, taskEvent, AuditEventStage.REQUEST, result);
            try {
                notifyWorkItemCreated(workItem, wfTask, result);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send notification about work item create event", e);
            }
        } else if (taskEvent instanceof TaskDeletedEvent) {
            auditWorkItemEvent(workItem, wfTask, taskEvent, AuditEventStage.EXECUTION, result);
            try {
                notifyWorkItemDeleted(workItem, wfTask, result);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't audit work item complete event", e);
            }
        }
    }
    //endregion

    //region Auditing and notifications
    private void auditProcessStart(WfTask wfTask, Map<String, Object> variables, OperationResult result) {
        auditProcessStartEnd(wfTask, AuditEventStage.REQUEST, variables, result);
    }

    private void auditProcessEnd(WfTask wfTask, ProcessEvent event, OperationResult result) {
        auditProcessStartEnd(wfTask, AuditEventStage.EXECUTION, event.getVariables(), result);
    }

    private void auditProcessStartEnd(WfTask wfTask, AuditEventStage stage, Map<String, Object> variables, OperationResult result) {
        AuditEventRecord auditEventRecord = wfTask.getChangeProcessor().prepareProcessInstanceAuditRecord(wfTask, stage, variables, result);
        auditService.audit(auditEventRecord, wfTask.getTask());
    }

    private void notifyProcessStart(WfTask wfTask, OperationResult result) throws SchemaException {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceStart(wfTask.getTask(), result);
        }
    }

    private void notifyProcessEnd(WfTask wfTask, OperationResult result) throws SchemaException {
        for (ProcessListener processListener : processListeners) {
            processListener.onProcessInstanceEnd(wfTask.getTask(), result);
        }
    }

    private void notifyWorkItemCreated(WorkItemType workItem, WfTask wfTask, OperationResult result) throws SchemaException {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemCreation(workItem, wfTask.getTask(), result);
        }
    }

    public void executeWorkItemNotificationAction(WorkItemType workItem, WorkItemNotificationActionType notificationAction,
			WfTask wfTask, OperationResult result) throws SchemaException {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemNotificationAction(workItem, notificationAction, wfTask.getTask(), result);
        }
    }

    private void notifyWorkItemDeleted(WorkItemType workItem, WfTask wfTask, OperationResult result) throws SchemaException {
        for (WorkItemListener workItemListener : workItemListeners) {
            workItemListener.onWorkItemDeletion(workItem, wfTask.getTask(), result);
        }
    }

	// workItem contains taskRef, assignee, candidates resolved (if possible)
    private void auditWorkItemEvent(WorkItemType workItem, WfTask wfTask, TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException {
        AuditEventRecord auditEventRecord = getChangeProcessor(taskEvent).prepareWorkItemAuditRecord(workItem, wfTask, taskEvent, stage, result);
        auditService.audit(auditEventRecord, wfTask.getTask());
    }

    public void registerProcessListener(ProcessListener processListener) {
		LOGGER.trace("Registering process listener {}", processListener);
        processListeners.add(processListener);
    }

    public void registerWorkItemListener(WorkItemListener workItemListener) {
		LOGGER.trace("Registering work item listener {}", workItemListener);
        workItemListeners.add(workItemListener);
    }
    //endregion

    //region Getters and setters
    public WfTaskUtil getWfTaskUtil() {
        return wfTaskUtil;
    }

	public MiscDataUtil getMiscDataUtil() {
		return miscDataUtil;
	}

	public PrismContext getPrismContext() {
        return prismContext;
    }

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public WfConfiguration getWfConfiguration() {
		return wfConfiguration;
	}

	//endregion
}
