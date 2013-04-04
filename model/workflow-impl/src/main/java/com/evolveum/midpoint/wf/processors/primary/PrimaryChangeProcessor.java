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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.ProcessInstanceController;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WfRootTaskHandler;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public abstract class PrimaryChangeProcessor implements ChangeProcessor, BeanNameAware {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    @Autowired
    WfTaskUtil wfTaskUtil;

    @Autowired
    TaskManager taskManager;

    @Autowired
    ProcessInstanceController processInstanceController;

    @Autowired
    private WfConfiguration wfConfiguration;

    String beanName;

    List<PrimaryApprovalProcessWrapper> processWrappers;

    public enum ExecutionMode {
        ALL_AFTERWARDS, ALL_IMMEDIATELY, MIXED;
    }

    @PostConstruct
    public void init() {
        Validate.notNull(beanName, "Bean name was not set correctly.");
        processWrappers = wfConfiguration.getPrimaryChangeProcessorWrappers(beanName);
    }

    @Override
    public void setBeanName(String name) {
        LOGGER.trace("Setting bean name to " + name);
        this.beanName = name;
    }

    @Override
    public HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<Objectable> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // examine the request using process wrappers

        ObjectDelta<Objectable> changeBeingDecomposed = (ObjectDelta<Objectable>) change.clone();
        List<StartProcessInstructionForPrimaryStage> startProcessInstructions =
                gatherStartProcessInstructions(context, changeBeingDecomposed, task, result);

        if (startProcessInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        }

        // start the process(es)

        return startProcesses(startProcessInstructions, context, changeBeingDecomposed, task, result);
    }

    private boolean shouldAllExecuteImmediately(List<StartProcessInstructionForPrimaryStage> startProcessInstructions) {
        for (StartProcessInstructionForPrimaryStage instruction : startProcessInstructions) {
            if (!instruction.isExecuteImmediately()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldAllExecuteAfterwards(List<StartProcessInstructionForPrimaryStage> startProcessInstructions) {
        for (StartProcessInstructionForPrimaryStage instruction : startProcessInstructions) {
            if (instruction.isExecuteImmediately()) {
                return false;
            }
        }
        return true;
    }


    private List<StartProcessInstructionForPrimaryStage> gatherStartProcessInstructions(ModelContext context, ObjectDelta<Objectable> changeBeingDecomposed, Task task, OperationResult result) {
        List<StartProcessInstructionForPrimaryStage> startProcessInstructions = new ArrayList<StartProcessInstructionForPrimaryStage>();

        for (PrimaryApprovalProcessWrapper wrapper : processWrappers) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Calling wrapper " + wrapper.getClass() + "...");
            }
            List<StartProcessInstructionForPrimaryStage> processes = wrapper.prepareProcessesToStart(context, changeBeingDecomposed, task, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Wrapper " + wrapper.getClass() + " returned the following process start instructions (count: " + processes.size() + "):");
                for (StartProcessInstructionForPrimaryStage startProcessInstruction : processes) {
                    LOGGER.trace(startProcessInstruction.debugDump(0));
                }
            }
            if (processes != null) {
                startProcessInstructions.addAll(processes);
            }
        }
        return startProcessInstructions;
    }

    private HookOperationMode startProcesses(List<StartProcessInstructionForPrimaryStage> startProcessInstructions, final ModelContext context, ObjectDelta<Objectable> changeWithoutApproval, Task rootTask, OperationResult result) {

        Throwable failReason;

        try {

            /*
             *  For Alternative (mode) 1 (all changes executed after all approvals) we put reduced model context into root task.
             *  For Alternative (mode) 2 (all changes executed immediately) we put reduced model into first (MOTH-only) subtask.
             *  For Alternative (mode) 3 (general) we put empty model context into root task.
             *
             *  allImmediately => mode 2
             *  allAfterwards => mode 1
             *  otherwise => mode 3
             */
            boolean allExecuteImmediately = shouldAllExecuteImmediately(startProcessInstructions);
            boolean allExecuteAfterwards = shouldAllExecuteAfterwards(startProcessInstructions);
            ExecutionMode executionMode = allExecuteImmediately ? ExecutionMode.ALL_IMMEDIATELY :
                                (allExecuteAfterwards ? ExecutionMode.ALL_AFTERWARDS : ExecutionMode.MIXED);

            if (executionMode == ExecutionMode.ALL_AFTERWARDS) {
                ((LensContext) context).replacePrimaryFocusDelta(changeWithoutApproval);
            } else {
                ((LensContext) context).replacePrimaryFocusDelta(ObjectDelta.createEmptyDelta(
                        changeWithoutApproval.getObjectTypeClass(), changeWithoutApproval.getOid(), changeWithoutApproval.getPrismContext(), ChangeType.MODIFY));
            }

            prepareAndSaveRootTask(executionMode, context, rootTask, result);

            // in modes 2, 3 we have to prepare first child that executes all changes that do not require approval
            if (executionMode == ExecutionMode.ALL_IMMEDIATELY || executionMode == ExecutionMode.MIXED) {
                StartProcessInstructionForPrimaryStage instruction0 = new StartProcessInstructionForPrimaryStage();
                instruction0.setNoProcess(true);
                instruction0.setExecuteImmediately(true);
                instruction0.setTaskName(new PolyStringType("Executing changes that do not require approval"));
                instruction0.setDelta(changeWithoutApproval);
                startProcessInstructions.add(0, instruction0);
            }

            for (final StartProcessInstructionForPrimaryStage instruction : startProcessInstructions) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processing start instruction: " + instruction.debugDump());
                }

                Task childTask = rootTask.createSubtask();

                if (instruction.startsWorkflowProcess()) {
                    wfTaskUtil.setProcessWrapper(childTask, instruction.getWrapper());
                    wfTaskUtil.setChangeProcessor(childTask, this);
                    wfTaskUtil.storeDeltasToProcess(instruction.getDeltas(), childTask);        // will be processed by wrapper on wf process termination

                    // if this has to be executed directly, we have to provide a model context for the execution
                    if (instruction.isExecuteImmediately()) {
                        // actually, context should be emptied anyway; but to be sure, let's do it here as well
                        LensContext contextCopy = ((LensContext) context).clone();
                        contextCopy.replacePrimaryFocusDelta(ObjectDelta.createEmptyDelta(
                                changeWithoutApproval.getObjectTypeClass(), changeWithoutApproval.getOid(), changeWithoutApproval.getPrismContext(), ChangeType.MODIFY));
                        wfTaskUtil.storeModelContext(childTask, contextCopy);
                    }

                } else {
                    // we have to put deltas into model context, as it will be processed directly by ModelOperationTaskHandler
                    LensContext contextCopy = ((LensContext) context).clone();
                    contextCopy.replacePrimaryFocusDeltas(instruction.getDeltas());
                    wfTaskUtil.storeModelContext(childTask, contextCopy);
                }

                processInstanceController.startProcessInstance(instruction, childTask, result);
            }

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException e) {
            failReason = e;
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (RuntimeException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", failReason);
        result.recordFatalError("Workflow process(es) could not be started: " + failReason, failReason);
        return HookOperationMode.ERROR;

        // todo rollback - at least close open tasks, maybe stop workflow process instances
    }

    // if rootContext == null, we do not put ModelOperationTaskHandler & WfRootTaskHandler onto the stack
    void prepareAndSaveRootTask(ExecutionMode mode, ModelContext rootContext, Task task, OperationResult result) throws SchemaException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareAndSaveRootTask starting; mode = " + mode + ", task = " + task + ", model context to be stored = " + (rootContext != null ? rootContext.debugDump() : "none"));
        }

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but this one is persistent; task = " + task);
        }

        if (task.getHandlerUri() != null) {
            throw new IllegalStateException("Workflow-related task should have no handler URI at this moment; task = " + task + ", existing handler URI = " + task.getHandlerUri());
        }

        wfTaskUtil.setTaskNameIfEmpty(task, new PolyStringType("Workflow task"));      // todo meaningful name
        task.setCategory(TaskCategory.WORKFLOW);

        if (mode != ExecutionMode.ALL_IMMEDIATELY) {

            task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, new ScheduleType(), null);
            task.pushHandlerUri(WfRootTaskHandler.HANDLER_URI, new ScheduleType(), null);
            try {
                wfTaskUtil.storeModelContext(task, rootContext);
            } catch (SchemaException e) {
                throw new SchemaException("Couldn't put model context into root workflow task " + task, e);
            }
        }
        // otherwise, we put no handler here, as the sole purpose of the task is to wait for its children

        try {
            task.waitForSubtasks(5, result);     // todo change subtasking mechanism
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Couldn't mark the task as waiting for subtasks: " + e, e);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Couldn't mark the task as waiting for subtasks: " + e, e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving root task: " + task.dump());
        }

        taskManager.switchToBackground(task, result);
    }

    @Override
    public void finishProcess(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        PrimaryApprovalProcessWrapper wrapper = wfTaskUtil.getProcessWrapper(task, processWrappers);
        List<ObjectDelta<Objectable>> deltas = wrapper.prepareDeltaOut(event, task, result);
        wfTaskUtil.storeResultingDeltas(deltas, task);

        if (wfTaskUtil.hasModelContext(task)) {
            ModelContext modelContext = wfTaskUtil.retrieveModelContext(task, result);
            ObjectDelta primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
            if (primaryDelta == null || (primaryDelta.isModify() && primaryDelta.getModifications().size() == 0)) {
                modelContext.getFocusContext().setPrimaryDelta(ObjectDelta.summarize(deltas));
                wfTaskUtil.storeModelContext(task, modelContext);
            } else {
                throw new IllegalStateException("Object delta in model context in task " + task + " should have been empty, but it isn't");
            }
        }

        task.savePendingModifications(result);
    }


}
