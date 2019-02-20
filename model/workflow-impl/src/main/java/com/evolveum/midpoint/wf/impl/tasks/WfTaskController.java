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

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.engine.WorkflowInterface;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages everything related to a activiti process instance, including the task that monitors that process instance.
 *
 * This class provides a facade over ugly mess of code managing activiti + task pair describing a workflow process instance.
 *
 * @author mederly
 */
@Component
public class WfTaskController {

	//region Attributes
    private static final Trace LOGGER = TraceManager.getTrace(WfTaskController.class);

    private static final Object DOT_CLASS = WfTaskController.class.getName() + ".";


    @Autowired private WfTaskUtil wfTaskUtil;
    @Autowired private TaskManager taskManager;
    @Autowired private WorkflowInterface workflowInterface;
    @Autowired private MiscDataUtil miscDataUtil;
    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelInteractionService modelInteractionService;
    //endregion

    //region Job creation & re-creation
    /**
     * Creates a background task, just as prescribed by the task creation instruction.
     * @param instruction the wf task creation instruction
     * @param parentWfTask the wf task that will be the parent of newly created one; it may be null
	 */
    public WfTask submitWfTask(WfTaskCreationInstruction instruction, WfTask parentWfTask, WfConfigurationType wfConfigurationType,
			OperationResult result) throws SchemaException {
        return submitWfTask(instruction, parentWfTask.getTask(), wfConfigurationType, null, result);
    }

    /**
     * As above, but this time we know only the parent task (not a wf task).
     * @param instruction the wf task creation instruction
     * @param parentTask the task that will be the parent of the task of newly created wf-task; it may be null
	 */
    public WfTask submitWfTask(WfTaskCreationInstruction instruction, Task parentTask, WfConfigurationType wfConfigurationType,
			String channelOverride, OperationResult result) throws SchemaException {
	    LOGGER.trace("Processing start instruction:\n{}", instruction.debugDumpLazily());
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

    private WfTask recreateWfTask(Task task, ChangeProcessor changeProcessor) {
		String processInstanceId = wfTaskUtil.getCaseOid(task);
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
        return new WfTask(this, subtask, wfTaskUtil.getCaseOid(subtask), parentWfTask.getChangeProcessor());
    }

    /**
     * Re-creates a root job, based on existing task information. Does not try to find the wf process instance.
     */
    public WfTask recreateRootWfTask(Task task) {
        return new WfTask(this, task, wfTaskUtil.getChangeProcessor(task));
    }
    //endregion

    //region Working with midPoint tasks

    private Task submitTask(WfTaskCreationInstruction instruction, Task parentTask, WfConfigurationType wfConfigurationType, String channelOverride, OperationResult result) throws SchemaException {
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
     * and 'commitChanges' because of the flushPendingModifications() mechanism that is used here.
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
	    try {
		    taskManager.unpauseTask(wfTask.getTask(), result);
	    } catch (PreconditionViolationException e) {
		    throw new SystemException("Task " + wfTask + " cannot be unpaused because it is no longer in WAITING state (should not occur)");
	    }
    }
    //endregion

    //region Working with workflow process instances

    private void startWorkflowProcessInstance(WfTask wfTask, WfTaskCreationInstruction<?,?> instruction, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(DOT_CLASS + "startWorkflowProcessInstance");
        try {
			LOGGER.trace("startWorkflowProcessInstance starting; instruction = {}", instruction);
			Task task = wfTask.getTask();
			workflowInterface.startWorkflowProcessInstance(instruction, task, result);
        } catch (SchemaException|RuntimeException|ObjectNotFoundException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't send a request to start a process instance to workflow management system", e);
			result.recordFatalError("Couldn't send a request to start a process instance to workflow management system: " + e.getMessage(), e);
            throw new SystemException("Workflow process instance creation could not be requested", e);
        } finally {
			result.computeStatusIfUnknown();
		}
        LOGGER.trace("startWorkflowProcessInstance finished");
    }

    //endregion

    //region Processing work item (task) events


	public List<ObjectReferenceType> getAssigneesAndDeputies(WorkItemType workItem, WfTask wfTask, OperationResult result)
			throws SchemaException {
    	return getAssigneesAndDeputies(workItem, wfTask.getTask(), result);
	}

	public List<ObjectReferenceType> getAssigneesAndDeputies(WorkItemType workItem, Task task, OperationResult result)
			throws SchemaException {
    	List<ObjectReferenceType> rv = new ArrayList<>();
    	rv.addAll(workItem.getAssigneeRef());
		rv.addAll(modelInteractionService.getDeputyAssignees(workItem, task, result));
		return rv;
	}
	//endregion

    //region Auditing and notifications


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
