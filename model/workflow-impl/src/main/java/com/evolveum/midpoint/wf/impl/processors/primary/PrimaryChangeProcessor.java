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
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processors.BaseAuditHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.evolveum.midpoint.audit.api.AuditEventStage.EXECUTION;
import static com.evolveum.midpoint.audit.api.AuditEventStage.REQUEST;
import static com.evolveum.midpoint.model.api.context.ModelState.PRIMARY;
import static com.evolveum.midpoint.wf.impl.processors.primary.ObjectTreeDeltas.extractFromModelContext;
import static com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor.ExecutionMode.*;

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
    private BaseAuditHelper baseAuditHelper;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private WfTaskController wfTaskController;

    @Autowired
    private PcpRepoAccessHelper pcpRepoAccessHelper;

    @Autowired
    private ProcessInterfaceFinder processInterfaceFinder;

    @Autowired
    private MiscDataUtil miscDataUtil;

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
    public HookOperationMode processModelInvocation(ModelContext context, WfConfigurationType wfConfigurationType, Task taskFromModel, OperationResult result)
			throws SchemaException, ObjectNotFoundException {

        if (context.getState() != PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectTreeDeltas objectTreeDeltas = extractFromModelContext(context);
        if (objectTreeDeltas.isEmpty()) {
            return null;
        }

        // examine the request using process aspects

        ObjectTreeDeltas changesBeingDecomposed = objectTreeDeltas.clone();
        List<PcpChildWfTaskCreationInstruction> jobCreationInstructions = gatherStartInstructions(context, wfConfigurationType, changesBeingDecomposed, taskFromModel, result);

        // start the process(es)

        if (jobCreationInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        } else {
            return startJobs(jobCreationInstructions, context, changesBeingDecomposed, taskFromModel, result);
        }
    }

    private List<PcpChildWfTaskCreationInstruction> gatherStartInstructions(ModelContext<? extends ObjectType> context,
			WfConfigurationType wfConfigurationType, ObjectTreeDeltas changesBeingDecomposed,
			Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<PcpChildWfTaskCreationInstruction> startProcessInstructions = new ArrayList<>();

        PrimaryChangeProcessorConfigurationType processorConfigurationType =
                wfConfigurationType != null ? wfConfigurationType.getPrimaryChangeProcessor() : null;

        if (processorConfigurationType != null && Boolean.FALSE.equals(processorConfigurationType.isEnabled())) {
            LOGGER.debug("Primary change processor is disabled.");
            return startProcessInstructions;
        }

        for (PrimaryChangeAspect aspect : getActiveChangeAspects(processorConfigurationType)) {
            if (changesBeingDecomposed.isEmpty()) {      // nothing left
                break;
            }
            List<PcpChildWfTaskCreationInstruction> instructions = aspect.prepareJobCreationInstructions(
                    context, wfConfigurationType, changesBeingDecomposed, taskFromModel, result);
            logAspectResult(aspect, instructions, changesBeingDecomposed);
            if (instructions != null) {
                startProcessInstructions.addAll(instructions);
            }
        }

        // tweaking the instructions returned from aspects a bit...

        // if we are adding a new object, we have to set OBJECT_TO_BE_ADDED variable in all instructions
//        ObjectDelta focusChange = changesBeingDecomposed.getFocusChange();
//        if (focusChange != null && focusChange.isAdd() && focusChange.getObjectToAdd() != null) {
//            String objectToBeAdded;
//            try {
//                objectToBeAdded = MiscDataUtil.serializeObjectToXml(focusChange.getObjectToAdd());
//            } catch (SystemException e) {
//                throw new SystemException("Couldn't serialize object to be added to XML", e);
//            }
//            for (PcpChildWfTaskCreationInstruction instruction : startProcessInstructions) {
//                //instruction.addProcessVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED, objectToBeAdded);
//                SPRAV S TYMTO NIECO
//            }
//        }

        for (PcpChildWfTaskCreationInstruction instruction : startProcessInstructions) {
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

    private void logAspectResult(PrimaryChangeAspect aspect, List<? extends WfTaskCreationInstruction> instructions, ObjectTreeDeltas changesBeingDecomposed) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("\n---[ Aspect {} returned the following process start instructions (count: {}) ]---", aspect.getClass(), instructions == null ? "(null)" : instructions.size());
            if (instructions != null) {
                for (WfTaskCreationInstruction instruction : instructions) {
                    LOGGER.trace(instruction.debugDump(0));
                }
                LOGGER.trace("Remaining delta(s):\n{}", changesBeingDecomposed.debugDump());
            }
        }
    }

    private HookOperationMode startJobs(List<PcpChildWfTaskCreationInstruction> instructions, final ModelContext context, final ObjectTreeDeltas changesWithoutApproval, Task taskFromModel, OperationResult result) {

        try {

            // prepare root job and job0
            ExecutionMode executionMode = determineExecutionMode(instructions);
            WfTask rootWfTask = createRootJob(context, changesWithoutApproval, taskFromModel, result, executionMode);
            WfTask wfTask0 = createJob0(context, changesWithoutApproval, rootWfTask, executionMode, result);

            // start the jobs
            List<WfTask> wfTasks = new ArrayList<>(instructions.size());
            for (WfTaskCreationInstruction instruction : instructions) {
                WfTask wfTask = wfTaskController.createWfTask(instruction, rootWfTask.getTask(), result);
                wfTasks.add(wfTask);
            }

            // all jobs depend on job0 (if there is one)
            if (wfTask0 != null) {
                for (WfTask wfTask : wfTasks) {
                    wfTask0.addDependent(wfTask);
                }
                wfTask0.commitChanges(result);
            }

            // now start the tasks - and exit

            baseModelInvocationProcessingHelper.logJobsBeforeStart(rootWfTask, result);
            if (wfTask0 != null) {
                wfTask0.resumeTask(result);
            }
            rootWfTask.startWaitingForSubtasks(result);
            return HookOperationMode.BACKGROUND;

        } catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|CommunicationException|ConfigurationException|RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;

            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }

    private WfTask createRootJob(ModelContext context, ObjectTreeDeltas changesWithoutApproval, Task taskFromModel, OperationResult result, ExecutionMode executionMode)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        LensContext lensContextForRootTask = determineLensContextForRootTask(context, changesWithoutApproval, executionMode);
        WfTaskCreationInstruction instructionForRoot = baseModelInvocationProcessingHelper.createInstructionForRoot(this, context, taskFromModel, lensContextForRootTask);
        if (executionMode != ALL_IMMEDIATELY) {
            instructionForRoot.setHandlersBeforeModelOperation(WfPrepareRootOperationTaskHandler.HANDLER_URI);      // gather all deltas from child objects
            instructionForRoot.setExecuteModelOperationHandler(true);
        }
        return baseModelInvocationProcessingHelper.createRootJob(instructionForRoot, taskFromModel, result);
    }

    // Child job0 - in modes 2, 3 we have to prepare first child that executes all changes that do not require approval
    private WfTask createJob0(ModelContext context, ObjectTreeDeltas changesWithoutApproval, WfTask rootWfTask, ExecutionMode executionMode, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (changesWithoutApproval != null && !changesWithoutApproval.isEmpty() && executionMode != ALL_AFTERWARDS) {
            ModelContext modelContext = contextCopyWithDeltaReplaced(context, changesWithoutApproval);
            WfTaskCreationInstruction instruction0 = WfTaskCreationInstruction.createModelOnly(rootWfTask.getChangeProcessor(), modelContext);
            instruction0.setTaskName("Executing changes that do not require approval");
            if (context.getFocusContext().getPrimaryDelta().isAdd()) {
                instruction0.setHandlersAfterModelOperation(WfPropagateTaskObjectReferenceTaskHandler.HANDLER_URI);  // for add operations we have to propagate ObjectOID
            }
            instruction0.setCreateTaskAsSuspended();   // task0 should execute only after all subtasks are created, because when it finishes, it
            // writes some information to all dependent tasks (i.e. they must exist at that time)
            return wfTaskController.createWfTask(instruction0, rootWfTask, result);
        } else {
            return null;
        }
    }

    private LensContext determineLensContextForRootTask(ModelContext context, ObjectTreeDeltas changesWithoutApproval, ExecutionMode executionMode) throws SchemaException {
        LensContext contextForRootTask;
        if (executionMode == ALL_AFTERWARDS) {
            contextForRootTask = contextCopyWithDeltaReplaced(context, changesWithoutApproval);
        } else if (executionMode == MIXED) {
            contextForRootTask = contextCopyWithNoDelta(context);
        } else {
            contextForRootTask = null;
        }
        return contextForRootTask;
    }

    private LensContext contextCopyWithDeltaReplaced(ModelContext context, ObjectTreeDeltas changes) throws SchemaException {
        Validate.notNull(changes, "changes");
        LensContext contextCopy = ((LensContext) context).clone();

        contextCopy.replacePrimaryFocusDelta(changes.getFocusChange());
        Map<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> changeMap = changes.getProjectionChangeMap();
        Collection<ModelProjectionContext> projectionContexts = contextCopy.getProjectionContexts();
        for (ModelProjectionContext projectionContext : projectionContexts) {
            ObjectDelta<ShadowType> projectionDelta = changeMap.get(projectionContext.getResourceShadowDiscriminator());
            projectionContext.setPrimaryDelta(projectionDelta);
        }
        return contextCopy;
    }

    public LensContext contextCopyWithNoDelta(ModelContext context) {
        LensContext contextCopy = ((LensContext) context).clone();
        contextCopy.replacePrimaryFocusDelta(null);
        Collection<LensProjectionContext> projectionContexts = contextCopy.getProjectionContexts();
        for (ModelProjectionContext projectionContext : projectionContexts) {
            projectionContext.setPrimaryDelta(null);
        }
        return contextCopy;
    }

    private ExecutionMode determineExecutionMode(List<PcpChildWfTaskCreationInstruction> instructions) {
        ExecutionMode executionMode;
        if (shouldAllExecuteImmediately(instructions)) {
            executionMode = ALL_IMMEDIATELY;
        } else if (shouldAllExecuteAfterwards(instructions)) {
            executionMode = ALL_AFTERWARDS;
        } else {
            executionMode = MIXED;
        }
        return executionMode;
    }

    private boolean shouldAllExecuteImmediately(List<PcpChildWfTaskCreationInstruction> startProcessInstructions) {
        for (PcpChildWfTaskCreationInstruction instruction : startProcessInstructions) {
            if (!instruction.isExecuteApprovedChangeImmediately()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldAllExecuteAfterwards(List<PcpChildWfTaskCreationInstruction> startProcessInstructions) {
        for (PcpChildWfTaskCreationInstruction instruction : startProcessInstructions) {
            if (instruction.isExecuteApprovedChangeImmediately()) {
                return false;
            }
        }
        return true;
    }
    //endregion

    //region Processing process finish event
    @Override
    public void onProcessEnd(ProcessEvent event, WfTask wfTask, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        PcpWfTask pcpJob = new PcpWfTask(wfTask);
        PrimaryChangeAspect aspect = pcpJob.getChangeAspect();

        pcpJob.storeResultingDeltas(aspect.prepareDeltaOut(event, pcpJob, result));
        pcpJob.addApprovedBy(aspect.prepareApprovedBy(event, pcpJob, result));
    }
    //endregion

    //region Auditing
    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(WfTask wfTask, AuditEventStage stage, Map<String, Object> variables, OperationResult result) {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareProcessInstanceAuditRecord(wfTask, stage, variables, result);

        ObjectTreeDeltas<?> deltas = null;
        try {
            if (stage == REQUEST) {
                deltas = wfTaskUtil.retrieveDeltasToProcess(wfTask.getTask());
            } else {
                deltas = wfTaskUtil.retrieveResultingDeltas(wfTask.getTask());
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve delta(s) from task " + wfTask.getTask(), e);
        }
        if (deltas != null) {
            List<ObjectDelta> deltaList = deltas.getDeltaList();
            for (ObjectDelta delta : deltaList) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        }

        if (stage == EXECUTION) {
            auditEventRecord.setResult(wfTask.getAnswer());
        }

        return auditEventRecord;
    }

    @Override
    public AuditEventRecord prepareWorkItemAuditRecord(WorkItemType workItem, WfTask wfTask, TaskEvent taskEvent, AuditEventStage stage,
            OperationResult result) throws WorkflowException {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareWorkItemAuditRecord(workItem, wfTask, taskEvent, stage, result);

        ObjectTreeDeltas<?> deltas = null;
        try {
			deltas = getWfTaskUtil().retrieveDeltasToProcess(wfTask.getTask());
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve deltas to be put into audit record", e);
        }

		if (deltas != null) {
			for (ObjectDelta delta : deltas.getDeltaList()) {
				auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
			}
		}

		return auditEventRecord;
    }

    //endregion

    //region Getters and setters
    public Collection<PrimaryChangeAspect> getAllChangeAspects() {
        return allChangeAspects;
    }

    PrimaryChangeAspect getChangeAspect(Map<String, Object> variables) {
        String aspectClassName = (String) variables.get(PcpProcessVariableNames.VARIABLE_CHANGE_ASPECT);
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
        LOGGER.trace("Registering aspect implemented by {}", changeAspect.getClass());
        allChangeAspects.add(changeAspect);
    }

    WfTaskUtil getWfTaskUtil() {     // ugly hack - used in PcpJob
        return wfTaskUtil;
    }
    //endregion
}
