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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.executions.Execution;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.executions.StartInstruction;
import com.evolveum.midpoint.wf.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.taskHandlers.WfPropagateTaskObjectReferenceTaskHandler;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.taskHandlers.WfPrepareRootOperationTaskHandler;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.BeansException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class PrimaryChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    public static final String UNKNOWN_OID = "?";

    private static final String KEY_WRAPPER = "wrapper";

    private static final List<String> LOCALLY_KNOWN_KEYS = Arrays.asList(KEY_WRAPPER);

    List<PrimaryApprovalProcessWrapper> processWrappers;

    public enum ExecutionMode {
        ALL_AFTERWARDS, ALL_IMMEDIATELY, MIXED;
    }

    @PostConstruct
    public void init() {
        initializeBaseProcessor(LOCALLY_KNOWN_KEYS);
        processWrappers = getPrimaryChangeProcessorWrappers();
        for (PrimaryApprovalProcessWrapper processWrapper : processWrappers) {
            processWrapper.setChangeProcessor(this);
        }
    }

    private List<PrimaryApprovalProcessWrapper> getPrimaryChangeProcessorWrappers() {

        Configuration c = getProcessorConfiguration();

        List<PrimaryApprovalProcessWrapper> retval = new ArrayList<PrimaryApprovalProcessWrapper>();

        String[] wrappers = c.getStringArray(KEY_WRAPPER);
        if (wrappers == null || wrappers.length == 0) {
            LOGGER.warn("No wrappers defined for primary change processor " + getBeanName());
        } else {
            for (String wrapperName : wrappers) {
                LOGGER.trace("Searching for wrapper " + wrapperName);
                try {
                    PrimaryApprovalProcessWrapper wrapper = (PrimaryApprovalProcessWrapper) getBeanFactory().getBean(wrapperName);
                    retval.add(wrapper);
                } catch(BeansException e) {
                    throw new SystemException("Process wrapper " + wrapperName + " could not be found.", e);
                }
            }
            LOGGER.debug("Resolved " + retval.size() + " process wrappers for primary change processor " + getBeanName());
        }
        return retval;
    }

    @Override
    public HookOperationMode processModelInvocation(ModelContext context, Task task, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // examine the request using process wrappers

        ObjectDelta<? extends ObjectType> changeBeingDecomposed = change.clone();
        List<StartInstruction> startProcessInstructions =
                gatherStartProcessInstructions(context, changeBeingDecomposed, task, result);

        if (startProcessInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        }

        // start the process(es)

        return startProcesses(startProcessInstructions, context, changeBeingDecomposed, task, result);
    }

    private List<StartInstruction> gatherStartProcessInstructions(ModelContext context, ObjectDelta<? extends ObjectType> changeBeingDecomposed, Task task, OperationResult result) throws SchemaException {
        List<StartInstruction> startProcessInstructions = new ArrayList<StartInstruction>();

        for (PrimaryApprovalProcessWrapper wrapper : processWrappers) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Calling wrapper " + wrapper.getClass() + "...");
            }
            List<StartInstruction> instructions = wrapper.prepareProcessesToStart(context, changeBeingDecomposed, task, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Wrapper " + wrapper.getClass() + " returned the following process start instructions (count: " + (instructions == null ? "(null)" : instructions.size()) + "):");
                if (instructions != null) {
                    for (StartInstruction instruction : instructions) {
                        LOGGER.trace(instruction.debugDump(0));
                    }
                }
            }
            if (instructions != null) {
                startProcessInstructions.addAll(instructions);
            }
        }

        // if we are adding a new object, we have to set OBJECT_TO_BE_ADDED variable in all instructions
        if (changeBeingDecomposed.isAdd()) {
            for (StartInstruction instruction : startProcessInstructions) {
                String objectToBeAdded = null;
                try {
                    objectToBeAdded = MiscDataUtil.serializeObjectToXml(changeBeingDecomposed.getObjectToAdd());
                } catch (SystemException e) {
                    throw new SystemException("Couldn't serialize object to be added to XML", e);
                }
                instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED, objectToBeAdded);
            }
        }

        return startProcessInstructions;
    }

    private HookOperationMode startProcesses(List<StartInstruction> instructions, final ModelContext context, final ObjectDelta<? extends ObjectType> changeWithoutApproval, Task rootTask, OperationResult result) {

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
            boolean allExecuteImmediately = shouldAllExecuteImmediately(instructions);
            boolean allExecuteAfterwards = shouldAllExecuteAfterwards(instructions);
            ExecutionMode executionMode = allExecuteImmediately ? ExecutionMode.ALL_IMMEDIATELY :
                                (allExecuteAfterwards ? ExecutionMode.ALL_AFTERWARDS : ExecutionMode.MIXED);

            LensContext contextForRootTask;
            if (executionMode == ExecutionMode.ALL_AFTERWARDS) {
                contextForRootTask = ((LensContext) context).clone();
                contextForRootTask.replacePrimaryFocusDelta(changeWithoutApproval);
            } else if (executionMode == ExecutionMode.MIXED) {
                contextForRootTask = prepareContextWithNoDelta((LensContext) context);
            } else {
                contextForRootTask = null;
            }

            // to which object (e.g. user) is the task related?
            PrismObject taskObject = context.getFocusContext().getObjectNew();
            if (taskObject != null && taskObject.getOid() == null) {
                taskObject = null;
            }

            // handlers for root task (good to set before prepareAndSaveRootTask, as after that method
            // the root task is stored into repository)

            if (executionMode != ExecutionMode.ALL_IMMEDIATELY) {
                rootTask.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, new ScheduleType(), null);
                rootTask.pushHandlerUri(WfPrepareRootOperationTaskHandler.HANDLER_URI, new ScheduleType(), null);
                try {
                    wfTaskUtil.storeModelContext(rootTask, contextForRootTask);
                } catch (SchemaException e) {
                    throw new SchemaException("Couldn't put model context into root workflow task " + rootTask, e);
                }
            }
            prepareAndSaveRootTask(contextForRootTask, rootTask, prepareRootTaskName(context), taskObject, result);




            StartInstruction instruction0;

            // in modes 2, 3 we have to prepare first child that executes all changes that do not require approval
            // we establish dependency links from this child to all other children, so the
            if (executionMode == ExecutionMode.ALL_IMMEDIATELY || executionMode == ExecutionMode.MIXED) {
                instruction0 = new StartInstruction(rootTask, this);
                instruction0.setNoProcess(true);
                instruction0.setExecuteImmediately(true);
                instruction0.setTaskName(new PolyStringType("Executing changes that do not require approval"));
                instruction0.addTaskModelContext(contextCopyWithDeltaReplaced(context, changeWithoutApproval));     // this will be processed directly by ModelOperationTaskHandler

                if (context.getFocusContext().getPrimaryDelta().isAdd()) {
                    // for add operations we have to propagate ObjectOID
                    instruction0.executeLast(WfPropagateTaskObjectReferenceTaskHandler.HANDLER_URI, null, null);    // this handler will be called AFTER model operation is carried out
                }
                instruction0.setCreateSuspended(true);   // task0 should execute only after all subtasks are created, because when it finishes, it
                                                         // writes some information to all dependent tasks (i.e. they must exist at that time)
                instructions.add(0, instruction0);       // it is nice to have it at the beginning
            } else {
                instruction0 = null;
            }

            Execution execution0 = null;

            // start the executions
            List<Execution> executions = new ArrayList<Execution>(instructions.size());
            for (StartInstruction instruction : instructions) {

                Execution execution = executionController.startExecution(instruction, result);

                executions.add(execution);
                if (instruction == instruction0) {
                    execution0 = execution;
                }
            }

            // all executions depend on execution0 (if there is one)
            if (execution0 != null) {
                for (Execution execution : executions) {
                    if (execution != execution0) {
                        executionController.addDependency(execution0, execution);
                    }
                }
                executionController.commitDependencies(execution0, result);
            }

            logTasksBeforeStart(rootTask, result);

            if (execution0 != null) {
                executionController.resume(execution0, result);
            }

            // now all children are created, we can start waiting
            rootTask.startWaitingForTasksImmediate(result);

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException e) {
            failReason = e;
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (ObjectAlreadyExistsException e) {
            failReason = e;
        } catch (RuntimeException e) {
            failReason = e;
        } catch (CommunicationException e) {
            failReason = e;
        } catch (ConfigurationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", failReason);
        result.recordFatalError("Workflow process(es) could not be started: " + failReason, failReason);
        return HookOperationMode.ERROR;

        // todo rollback - at least close open tasks, maybe stop workflow process instances
    }

    private LensContext contextCopyWithDeltaReplaced(ModelContext context, ObjectDelta<? extends ObjectType> changeWithoutApproval) throws SchemaException {
        LensContext contextCopy = ((LensContext) context).clone();
        contextCopy.replacePrimaryFocusDeltas(Arrays.asList(changeWithoutApproval));
        return contextCopy;
    }

    public LensContext prepareContextWithNoDelta(LensContext context) {
        ObjectDelta<? extends ObjectType> changeAsPrototype = context.getFocusContext().getPrimaryDelta();
        LensContext contextCopy = ((LensContext) context).clone();
        contextCopy.replacePrimaryFocusDelta(
                ObjectDelta.createEmptyDelta(
                        changeAsPrototype.getObjectTypeClass(),
                        changeAsPrototype.getOid() == null ? UNKNOWN_OID : changeAsPrototype.getOid(),
                        changeAsPrototype.getPrismContext(),
                        ChangeType.MODIFY));
        return contextCopy;
    }

    private boolean shouldAllExecuteImmediately(List<StartInstruction> startProcessInstructions) {
        for (StartInstruction instruction : startProcessInstructions) {
            if (!instruction.isExecuteImmediately()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldAllExecuteAfterwards(List<StartInstruction> startProcessInstructions) {
        for (StartInstruction instruction : startProcessInstructions) {
            if (instruction.isExecuteImmediately()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void finishProcess(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        PrimaryApprovalProcessWrapper wrapper = wfTaskUtil.getProcessWrapper(task, processWrappers);

        // deltaOut
        List<ObjectDelta<Objectable>> deltas = wrapper.prepareDeltaOut(event, task, result);
        wfTaskUtil.storeResultingDeltas(deltas, task);

        // approvedBy
        wfTaskUtil.addApprovedBy(task, wrapper.getApprovedBy(event));

        task.savePendingModifications(result);
    }

    @Override
    public PrismObject<? extends ObjectType> getRequestSpecificData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getProcessWrapper(variables).getRequestSpecificData(task, variables, result);
    }

    @Override
    public PrismObject<? extends ObjectType> getAdditionalData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getProcessWrapper(variables).getAdditionalData(task, variables, result);
    }

    private PrimaryApprovalProcessWrapper getProcessWrapper(Map<String, Object> variables) {
        String wrapperClassName = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER);
        return findProcessWrapper(wrapperClassName);
    }

    public PrimaryApprovalProcessWrapper findProcessWrapper(String name) {
        for (PrimaryApprovalProcessWrapper w : processWrappers) {
            if (name.equals(w.getClass().getName())) {
                return w;
            }
        }
        throw new IllegalStateException("Wrapper " + name + " is not registered.");
    }

    @Override
    public String getProcessInstanceDetailsPanelName(ProcessInstance processInstance) {
        String wrapperName = (String) processInstance.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER);
        Validate.notNull(wrapperName, "There's no change processor name among the process instance variables");
        PrimaryApprovalProcessWrapper wrapper = findProcessWrapper(wrapperName);
        return wrapper.getProcessInstanceDetailsPanelName(processInstance);
    }


}
