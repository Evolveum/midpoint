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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.jobs.Job;
import com.evolveum.midpoint.wf.impl.jobs.JobController;
import com.evolveum.midpoint.wf.impl.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processors.BaseAuditHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseExternalizationHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mederly
 */
@Component
public class PrimaryChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    @Autowired
    private PcpConfigurationHelper pcpConfigurationHelper;

    @Autowired
    private BaseConfigurationHelper baseConfigurationHelper;

    @Autowired
    private BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;

    @Autowired
    private BaseExternalizationHelper baseExternalizationHelper;

    @Autowired
    private BaseAuditHelper baseAuditHelper;

    @Autowired
    private PcpExternalizationHelper pcpExternalizationHelper;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private JobController jobController;

    @Autowired
    private PcpRepoAccessHelper pcpRepoAccessHelper;

    @Autowired
    private ProcessInterfaceFinder processInterfaceFinder;

    public static final String UNKNOWN_OID = "?";

    Set<PrimaryChangeAspect> allChangeAspects = new HashSet<>();

    public enum ExecutionMode {
        ALL_AFTERWARDS, ALL_IMMEDIATELY, MIXED;
    }

    //region Configuration
    // =================================================================================== Configuration
    @PostConstruct
    public void init() {
        baseConfigurationHelper.registerProcessor(this);
    }
    //endregion

    //region Processing model invocation
    // =================================================================================== Processing model invocation

    @Override
    public HookOperationMode processModelInvocation(ModelContext context, WfConfigurationType wfConfigurationType, Task taskFromModel, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<? extends ObjectType> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // examine the request using process aspects

        ObjectDelta<? extends ObjectType> changeBeingDecomposed = change.clone();
        List<PcpChildJobCreationInstruction> jobCreationInstructions = gatherStartInstructions(context, wfConfigurationType, changeBeingDecomposed, taskFromModel, result);

        // start the process(es)

        if (jobCreationInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        } else {
            return startJobs(jobCreationInstructions, context, changeBeingDecomposed, taskFromModel, result);
        }
    }

    private List<PcpChildJobCreationInstruction> gatherStartInstructions(ModelContext<? extends ObjectType> context,
                                                                         WfConfigurationType wfConfigurationType,
                                                                         ObjectDelta<? extends ObjectType> changeBeingDecomposed,
                                                                         Task taskFromModel, OperationResult result) throws SchemaException {
        List<PcpChildJobCreationInstruction> startProcessInstructions = new ArrayList<>();

        PrimaryChangeProcessorConfigurationType processorConfigurationType =
                wfConfigurationType.getPrimaryChangeProcessor();

        if (processorConfigurationType != null && Boolean.FALSE.equals(processorConfigurationType.isEnabled())) {
            LOGGER.debug("Primary change processor is disabled.");
            return startProcessInstructions;
        }

        for (PrimaryChangeAspect aspect : getActiveChangeAspects(processorConfigurationType)) {
            if (changeBeingDecomposed.isEmpty()) {      // nothing left
                break;
            }
            List<PcpChildJobCreationInstruction> instructions = aspect.prepareJobCreationInstructions(
                    context, wfConfigurationType, changeBeingDecomposed, taskFromModel, result);
            logAspectResult(aspect, instructions);
            if (instructions != null) {
                startProcessInstructions.addAll(instructions);
            }
        }

        // tweaking the instructions returned from aspects a bit...

        // if we are adding a new object, we have to set OBJECT_TO_BE_ADDED variable in all instructions
        if (changeBeingDecomposed.isAdd() && changeBeingDecomposed.getObjectToAdd() != null) {
            String objectToBeAdded;
            try {
                objectToBeAdded = MiscDataUtil.serializeObjectToXml(changeBeingDecomposed.getObjectToAdd());
            } catch (SystemException e) {
                throw new SystemException("Couldn't serialize object to be added to XML", e);
            }
            for (PcpChildJobCreationInstruction instruction : startProcessInstructions) {
                instruction.addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED, objectToBeAdded);
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

    private Collection<PrimaryChangeAspect> getActiveChangeAspects(PrimaryChangeProcessorConfigurationType processorConfigurationType) {
        Collection<PrimaryChangeAspect> rv = new HashSet<>();
        for (PrimaryChangeAspect aspect : getAllChangeAspects()) {
            if (aspect.isEnabled(processorConfigurationType)) {
                rv.add(aspect);
            }
        }
        return rv;
    }

    private void logAspectResult(PrimaryChangeAspect aspect, List<? extends JobCreationInstruction> instructions) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Aspect " + aspect.getClass() + " returned the following process start instructions (count: " + (instructions == null ? "(null)" : instructions.size()) + "):");
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
        if (changeWithoutApproval != null && !changeWithoutApproval.isEmpty() && executionMode != ExecutionMode.ALL_AFTERWARDS) {
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
        if (change != null) {
            contextCopy.replacePrimaryFocusDeltas(Arrays.asList(change));
        } else {
            contextCopy.replacePrimaryFocusDeltas(null);
        }
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
        PrimaryChangeAspect aspect = pcpJob.getChangeAspect();

        pcpJob.storeResultingDeltas(aspect.prepareDeltaOut(event, pcpJob, result));
        pcpJob.addApprovedBy(aspect.prepareApprovedBy(event, pcpJob, result));
        pcpJob.commitChanges(result);
    }
    //endregion

    @Override
    public PrismObject<? extends ProcessInstanceState> externalizeProcessInstanceState(Map<String, Object> variables) throws JAXBException, SchemaException {

        PrismObject<ProcessInstanceState> processInstanceStatePrismObject = baseExternalizationHelper.externalizeState(variables);
        processInstanceStatePrismObject.asObjectable().setProcessorSpecificState(pcpExternalizationHelper.externalizeState(variables));
        processInstanceStatePrismObject.asObjectable().setProcessSpecificState(getChangeAspect(variables).externalizeProcessInstanceState(variables));
        return processInstanceStatePrismObject;
    }

    @Override
    public PrismObject<? extends WorkItemContents> externalizeWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {
        return pcpExternalizationHelper.externalizeWorkItemContents(task, processInstanceVariables, result);
    }
    //endregion

    //region Auditing
    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(Map<String, Object> variables, Job job, AuditEventStage stage, OperationResult result) {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareProcessInstanceAuditRecord(variables, job, stage, result);

        List<ObjectDelta<Objectable>> deltas = null;
        try {
            if (stage == AuditEventStage.REQUEST) {
                deltas = wfTaskUtil.retrieveDeltasToProcess(job.getTask());
            } else {
                deltas = wfTaskUtil.retrieveResultingDeltas(job.getTask());
            }
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta(s) from task " + job.getTask(), e);
        }
        if (deltas != null) {
            for (ObjectDelta delta : deltas) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        }

        if (stage == AuditEventStage.EXECUTION) {
            auditEventRecord.setResult(processInterfaceFinder.getProcessInterface(variables).getAnswer(variables));
        }

        return auditEventRecord;
    }

    @Override
    public AuditEventRecord prepareWorkItemAuditRecord(TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareWorkItemAuditRecord(taskEvent, stage, result);
        ObjectDelta delta;
        try {
            delta = pcpRepoAccessHelper.getObjectDelta(taskEvent.getVariables(), true);
            if (delta != null) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        } catch (JAXBException|SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta to be approved", e);
        }
        return auditEventRecord;
    }

    //endregion

    //region Getters and setters
    public Collection<PrimaryChangeAspect> getAllChangeAspects() {
        return allChangeAspects;
    }

    PrimaryChangeAspect getChangeAspect(Map<String, Object> variables) {
        String aspectClassName = (String) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_ASPECT);
        return findPrimaryChangeAspect(aspectClassName);
    }

    public PrimaryChangeAspect findPrimaryChangeAspect(String name) {

        // we can search either by bean name or by aspect class name (experience will show what is the better way)
        if (getBeanFactory().containsBean(name)) {
            return getBeanFactory().getBean(name, PrimaryChangeAspect.class);
        }
        for (PrimaryChangeAspect w : allChangeAspects) {
            if (name.equals(w.getClass().getName())) {
                return w;
            }
        }
        throw new IllegalStateException("Aspect " + name + " is not registered.");
    }

    public void registerChangeAspect(PrimaryChangeAspect changeAspect) {
        allChangeAspects.add(changeAspect);
    }

    WfTaskUtil getWfTaskUtil() {     // ugly hack - used in PcpJob
        return wfTaskUtil;
    }
    //endregion
}
