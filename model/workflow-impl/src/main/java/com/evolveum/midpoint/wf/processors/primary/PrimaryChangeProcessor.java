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
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.processors.BaseExternalizationHelper;
import com.evolveum.midpoint.wf.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.processors.primary.wrapper.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.GeneralChangeApprovalWorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.PrimaryApprovalProcessInstanceState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessInstanceState;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class PrimaryChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    @Autowired
    private PcpConfigurationHelper pcpConfigurationHelper;

    @Autowired
    private BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;

    @Autowired
    private BaseExternalizationHelper baseExternalizationHelper;

    @Autowired
    private PcpExternalizationHelper pcpExternalizationHelper;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private JobController jobController;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private MiscDataUtil miscDataUtil;

    public static final String UNKNOWN_OID = "?";

    List<PrimaryApprovalProcessWrapper> processWrappers;

    public enum ExecutionMode {
        ALL_AFTERWARDS, ALL_IMMEDIATELY, MIXED;
    }

    //region Configuration
    // =================================================================================== Configuration
    @PostConstruct
    public void init() {
        pcpConfigurationHelper.configure(this);
    }

    public void setProcessWrappers(List<PrimaryApprovalProcessWrapper> processWrappers) {
        this.processWrappers = processWrappers;
    }
    //endregion

    //region Processing model invocation
    // =================================================================================== Processing model invocation

    @Override
    public HookOperationMode processModelInvocation(ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // examine the request using process wrappers

        ObjectDelta<? extends ObjectType> changeBeingDecomposed = change.clone();
        List<PcpChildJobCreationInstruction> jobCreationInstructions = gatherStartInstructions(context, changeBeingDecomposed, taskFromModel, result);

        // start the process(es)

        if (jobCreationInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        } else {
            return startJobs(jobCreationInstructions, context, changeBeingDecomposed, taskFromModel, result);
        }
    }

    private List<PcpChildJobCreationInstruction> gatherStartInstructions(ModelContext context, ObjectDelta<? extends ObjectType> changeBeingDecomposed, Task taskFromModel, OperationResult result) throws SchemaException {
        List<PcpChildJobCreationInstruction> startProcessInstructions = new ArrayList<>();

        for (PrimaryApprovalProcessWrapper wrapper : processWrappers) {
            List<PcpChildJobCreationInstruction> instructions = wrapper.prepareJobCreationInstructions(context, changeBeingDecomposed, taskFromModel, result);
            logWrapperResult(wrapper, instructions);
            if (instructions != null) {
                startProcessInstructions.addAll(instructions);
            }
        }

        // tweaking the instructions returned from wrappers a bit...

        // if we are adding a new object, we have to set OBJECT_TO_BE_ADDED variable in all instructions
        if (changeBeingDecomposed.isAdd()) {
            String objectToBeAdded;
            try {
                objectToBeAdded = MiscDataUtil.serializeObjectToXml(changeBeingDecomposed.getObjectToAdd());
            } catch (SystemException e) {
                throw new SystemException("Couldn't serialize object to be added to XML", e);
            }
            for (PcpChildJobCreationInstruction instruction : startProcessInstructions) {
                instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED, objectToBeAdded);
            }
        }

        for (PcpChildJobCreationInstruction instruction : startProcessInstructions) {
            if (instruction.startsWorkflowProcess() && instruction.isExecuteApprovedChangeImmediately()) {
                // if we want to execute approved changes immediately in this instruction, we have to wait for
                // task0 (if there is any) and then to update our model context with the results (if there are any)
                instruction.addHandlersAfterWfProcessAtEnd(WfTaskUtil.WAIT_FOR_TASKS_HANDLER_URI, WfPrepareChildOperationTaskHandler.HANDLER_URI);
            }
        }

        return startProcessInstructions;
    }

    private void logWrapperResult(PrimaryApprovalProcessWrapper wrapper, List<? extends JobCreationInstruction> instructions) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Wrapper " + wrapper.getClass() + " returned the following process start instructions (count: " + (instructions == null ? "(null)" : instructions.size()) + "):");
            if (instructions != null) {
                for (JobCreationInstruction instruction : instructions) {
                    LOGGER.trace(instruction.debugDump(0));
                }
            }
        }
    }

    private HookOperationMode startJobs(List<PcpChildJobCreationInstruction> instructions, final ModelContext context, final ObjectDelta<? extends ObjectType> changeWithoutApproval, Task taskFromModel, OperationResult result) {

        try {

            // prepare root job and job0
            ExecutionMode executionMode = determineExecutionMode(instructions);
            Job rootJob = createRootJob(context, changeWithoutApproval, taskFromModel, result, executionMode);
            Job job0 = createJob0(context, changeWithoutApproval, rootJob, executionMode, result);

            // start the jobs
            List<Job> jobs = new ArrayList<>(instructions.size());
            for (JobCreationInstruction instruction : instructions) {
                Job job = jobController.createJob(instruction, rootJob.getTask(), result);
                jobs.add(job);
            }

            // all jobs depend on job0 (if there is one)
            if (job0 != null) {
                for (Job job : jobs) {
                    job0.addDependent(job);
                }
                job0.commitChanges(result);
            }

            // now start the tasks - and exit

            baseModelInvocationProcessingHelper.logJobsBeforeStart(rootJob, result);
            if (job0 != null) {
                job0.resumeTask(result);
            }
            rootJob.startWaitingForSubtasks(result);
            return HookOperationMode.BACKGROUND;

        } catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|CommunicationException|ConfigurationException|RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;

            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }

    private Job createRootJob(ModelContext context, ObjectDelta<? extends ObjectType> changeWithoutApproval, Task taskFromModel, OperationResult result, ExecutionMode executionMode) throws SchemaException, ObjectNotFoundException {
        LensContext contextForRootTask = determineContextForRootTask(context, changeWithoutApproval, executionMode);
        JobCreationInstruction instructionForRoot = baseModelInvocationProcessingHelper.createInstructionForRoot(this, context, taskFromModel, contextForRootTask);
        if (executionMode != ExecutionMode.ALL_IMMEDIATELY) {
            instructionForRoot.setHandlersBeforeModelOperation(WfPrepareRootOperationTaskHandler.HANDLER_URI);      // gather all deltas from child objects
            instructionForRoot.setExecuteModelOperationHandler(true);
        }
        return baseModelInvocationProcessingHelper.createRootJob(instructionForRoot, taskFromModel, result);
    }

    // Child job0 - in modes 2, 3 we have to prepare first child that executes all changes that do not require approval
    private Job createJob0(ModelContext context, ObjectDelta<? extends ObjectType> changeWithoutApproval, Job rootJob, ExecutionMode executionMode, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (!changeWithoutApproval.isEmpty() && executionMode != ExecutionMode.ALL_AFTERWARDS) {
            ModelContext modelContext = contextCopyWithDeltaReplaced(context, changeWithoutApproval);
            JobCreationInstruction instruction0 = JobCreationInstruction.createModelOperationChildJob(rootJob, modelContext);
            instruction0.setTaskName("Executing changes that do not require approval");
            if (context.getFocusContext().getPrimaryDelta().isAdd()) {
                instruction0.setHandlersAfterModelOperation(WfPropagateTaskObjectReferenceTaskHandler.HANDLER_URI);  // for add operations we have to propagate ObjectOID
            }
            instruction0.setCreateTaskAsSuspended(true);   // task0 should execute only after all subtasks are created, because when it finishes, it
            // writes some information to all dependent tasks (i.e. they must exist at that time)
            return jobController.createJob(instruction0, rootJob, result);
        } else {
            return null;
        }
    }

    private LensContext determineContextForRootTask(ModelContext context, ObjectDelta<? extends ObjectType> changeWithoutApproval, ExecutionMode executionMode) throws SchemaException {
        LensContext contextForRootTask;
        if (executionMode == ExecutionMode.ALL_AFTERWARDS) {
            contextForRootTask = contextCopyWithDeltaReplaced(context, changeWithoutApproval);
        } else if (executionMode == ExecutionMode.MIXED) {
            contextForRootTask = contextCopyWithNoDelta(context);
        } else {
            contextForRootTask = null;
        }
        return contextForRootTask;
    }

    private LensContext contextCopyWithDeltaReplaced(ModelContext context, ObjectDelta<? extends ObjectType> change) throws SchemaException {
        LensContext contextCopy = ((LensContext) context).clone();
        contextCopy.replacePrimaryFocusDeltas(Arrays.asList(change));
        return contextCopy;
    }

    public LensContext contextCopyWithNoDelta(ModelContext context) {
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

    private ExecutionMode determineExecutionMode(List<PcpChildJobCreationInstruction> instructions) {
        ExecutionMode executionMode;
        if (shouldAllExecuteImmediately(instructions)) {
            executionMode = ExecutionMode.ALL_IMMEDIATELY;
        } else if (shouldAllExecuteAfterwards(instructions)) {
            executionMode = ExecutionMode.ALL_AFTERWARDS;
        } else {
            executionMode = ExecutionMode.MIXED;
        }
        return executionMode;
    }

    private boolean shouldAllExecuteImmediately(List<PcpChildJobCreationInstruction> startProcessInstructions) {
        for (PcpChildJobCreationInstruction instruction : startProcessInstructions) {
            if (!instruction.isExecuteApprovedChangeImmediately()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldAllExecuteAfterwards(List<PcpChildJobCreationInstruction> startProcessInstructions) {
        for (PcpChildJobCreationInstruction instruction : startProcessInstructions) {
            if (instruction.isExecuteApprovedChangeImmediately()) {
                return false;
            }
        }
        return true;
    }
    //endregion

    //region Processing process finish event
    @Override
    public void onProcessEnd(ProcessEvent event, Job job, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        PcpJob pcpJob = new PcpJob(job);
        PrimaryApprovalProcessWrapper wrapper = pcpJob.getProcessWrapper();

        pcpJob.storeResultingDeltas(wrapper.prepareDeltaOut(event, pcpJob, result));
        pcpJob.addApprovedBy(wrapper.getApprovedBy(event));
        pcpJob.commitChanges(result);
    }
    //endregion

    //region User interaction
    public PrismObject<? extends QuestionFormType> getQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getProcessWrapper(variables).getRequestSpecificData(task, variables, result);
    }

    public PrismObject<? extends ObjectType> getRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getProcessWrapper(variables).getRelatedObject(task, variables, result);
    }

    @Override
    public String getProcessInstanceDetailsPanelName(WfProcessInstanceType processInstance) {
        PrimaryApprovalProcessInstanceState state = (PrimaryApprovalProcessInstanceState) processInstance.getState();
        String wrapperName = state.getMidPointProcessWrapper();
        Validate.notNull(wrapperName, "There's no change processor name among the process instance variables");
        PrimaryApprovalProcessWrapper wrapper = findProcessWrapper(wrapperName);
        return wrapper.getProcessInstanceDetailsPanelName(processInstance);
    }

    @Override
    public PrismObject<? extends ProcessInstanceState> externalizeInstanceState(Map<String, Object> variables) throws JAXBException, SchemaException {
        PrismObject<? extends PrimaryApprovalProcessInstanceState> state = getProcessWrapper(variables).externalizeInstanceState(variables);
        pcpExternalizationHelper.externalizeState(state, variables);
        baseExternalizationHelper.externalizeState(state, variables);
        return state;
    }

    @Override
    public PrismObject<? extends WorkItemContents> prepareWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {

        PrismObject<? extends GeneralChangeApprovalWorkItemContents> wicPrism = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(GeneralChangeApprovalWorkItemContents.class).instantiate();
        GeneralChangeApprovalWorkItemContents wic = wicPrism.asObjectable();

        PrismObject<? extends ObjectType> objectBefore = miscDataUtil.getObjectBefore(processInstanceVariables, prismContext, result);
        if (objectBefore != null) {
            wic.setObjectOld(objectBefore.asObjectable());
            if (objectBefore.getOid() != null) {
                wic.setObjectOldRef(MiscSchemaUtil.createObjectReference(objectBefore.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        wic.setObjectDelta(miscDataUtil.getObjectDeltaType(processInstanceVariables, true));

        PrismObject<? extends ObjectType> objectAfter = miscDataUtil.getObjectAfter(processInstanceVariables, wic.getObjectDelta(), objectBefore, prismContext, result);
        if (objectAfter != null) {
            wic.setObjectNew(objectAfter.asObjectable());
            if (objectAfter.getOid() != null) {
                wic.setObjectNewRef(MiscSchemaUtil.createObjectReference(objectAfter.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        PrismObject<? extends ObjectType> relatedObject = getRelatedObject(task, processInstanceVariables, result);
        if (relatedObject != null) {
            wic.setRelatedObject(relatedObject.asObjectable());
            if (relatedObject.getOid() != null) {
                wic.setRelatedObjectRef(MiscSchemaUtil.createObjectReference(relatedObject.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        wic.setQuestionForm(asObjectable(getQuestionForm(task, processInstanceVariables, result)));
        return wicPrism;
    }

    private <T> T asObjectable(PrismObject<? extends T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }


    //endregion

    //region Getters and setters
    public List<PrimaryApprovalProcessWrapper> getProcessWrappers() {
        return processWrappers;
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

    WfTaskUtil getWfTaskUtil() {     // ugly hack - used in PcpJob
        return wfTaskUtil;
    }
    //endregion
}
