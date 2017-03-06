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
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processors.BaseAuditHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.audit.api.AuditEventStage.REQUEST;
import static com.evolveum.midpoint.model.api.context.ModelState.PRIMARY;
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

    private List<PrimaryChangeAspect> allChangeAspects = new ArrayList<>();

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
    public HookOperationMode processModelInvocation(@NotNull ModelContext<?> context, WfConfigurationType wfConfigurationType,
			@NotNull Task taskFromModel, @NotNull OperationResult result)
			throws SchemaException, ObjectNotFoundException {

        if (context.getState() != PRIMARY || context.getFocusContext() == null) {
            return null;
        }

		PrimaryChangeProcessorConfigurationType processorConfigurationType =
				wfConfigurationType != null ? wfConfigurationType.getPrimaryChangeProcessor() : null;

		if (processorConfigurationType != null && Boolean.FALSE.equals(processorConfigurationType.isEnabled())) {
			LOGGER.debug("Primary change processor is disabled.");
			return null;
		}

		ObjectTreeDeltas objectTreeDeltas = baseModelInvocationProcessingHelper.extractTreeDeltasFromModelContext(context);
        if (objectTreeDeltas.isEmpty()) {
            return null;
        }

        // examine the request using process aspects

        ObjectTreeDeltas changesBeingDecomposed = objectTreeDeltas.clone();
        ModelInvocationContext ctx = new ModelInvocationContext(getPrismContext(), context, wfConfigurationType, taskFromModel);
        List<PcpChildWfTaskCreationInstruction> childTaskInstructions = gatherStartInstructions(changesBeingDecomposed, ctx, result);

        // start the process(es)

        if (childTaskInstructions.isEmpty()) {
            LOGGER.trace("There are no workflow processes to be started, exiting.");
            return null;
        }
		return submitTasks(childTaskInstructions, context, changesBeingDecomposed, taskFromModel, wfConfigurationType, result);
    }

	private List<PcpChildWfTaskCreationInstruction> gatherStartInstructions(@NotNull ObjectTreeDeltas changesBeingDecomposed,
			ModelInvocationContext ctx, @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {

        PrimaryChangeProcessorConfigurationType processorConfigurationType =
                ctx.wfConfiguration != null ? ctx.wfConfiguration.getPrimaryChangeProcessor() : null;

        List<PcpChildWfTaskCreationInstruction> startProcessInstructions = new ArrayList<>();
        for (PrimaryChangeAspect aspect : getActiveChangeAspects(processorConfigurationType)) {
            if (changesBeingDecomposed.isEmpty()) {      // nothing left
                break;
            }
            List<PcpChildWfTaskCreationInstruction> instructions = aspect.prepareTasks(changesBeingDecomposed, ctx, result);
            logAspectResult(aspect, instructions, changesBeingDecomposed);
            if (instructions != null) {
                startProcessInstructions.addAll(instructions);
            }
        }
        return startProcessInstructions;
    }

	private Collection<PrimaryChangeAspect> getActiveChangeAspects(PrimaryChangeProcessorConfigurationType processorConfigurationType) {
        Collection<PrimaryChangeAspect> rv = getAllChangeAspects().stream()
				.filter(aspect -> aspect.isEnabled(processorConfigurationType)).collect(Collectors.toList());
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

    private HookOperationMode submitTasks(List<PcpChildWfTaskCreationInstruction> instructions, final ModelContext context,
			final ObjectTreeDeltas changesWithoutApproval, Task taskFromModel, WfConfigurationType wfConfigurationType, OperationResult result) {

        try {

			ExecutionMode executionMode = determineExecutionMode(instructions);

			// prepare root task and task0
            WfTask rootWfTask = submitRootTask(context, changesWithoutApproval, taskFromModel, executionMode, wfConfigurationType, result);
            WfTask wfTask0 = submitTask0(context, changesWithoutApproval, rootWfTask, executionMode, wfConfigurationType, result);

            // start the jobs
            List<WfTask> wfTasks = new ArrayList<>(instructions.size());
            for (PcpChildWfTaskCreationInstruction instruction : instructions) {
				if (instruction.startsWorkflowProcess() && instruction.isExecuteApprovedChangeImmediately()) {
					// if we want to execute approved changes immediately in this instruction, we have to wait for
					// task0 (if there is any) and then to update our model context with the results (if there are any)
					// TODO CONSIDER THIS... when OID is no longer transferred
					instruction.addHandlersAfterWfProcessAtEnd(WfTaskUtil.WAIT_FOR_TASKS_HANDLER_URI, WfPrepareChildOperationTaskHandler.HANDLER_URI);
				}
				WfTask wfTask = wfTaskController.submitWfTask(instruction, rootWfTask.getTask(), wfConfigurationType, null, result);
                wfTasks.add(wfTask);
            }

            // all jobs depend on job0 (if there is one)
            if (wfTask0 != null) {
                for (WfTask wfTask : wfTasks) {
                    wfTask0.addDependent(wfTask);
                }
                wfTask0.commitChanges(result);
            }

            baseModelInvocationProcessingHelper.logJobsBeforeStart(rootWfTask, result);
            rootWfTask.startWaitingForSubtasks(result);
            return HookOperationMode.BACKGROUND;

        } catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|CommunicationException|ConfigurationException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;

            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }

    private WfTask submitRootTask(ModelContext context, ObjectTreeDeltas changesWithoutApproval, Task taskFromModel, ExecutionMode executionMode,
			WfConfigurationType wfConfigurationType, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        LensContext lensContextForRootTask = determineLensContextForRootTask(context, changesWithoutApproval, executionMode);
        WfTaskCreationInstruction instructionForRoot = baseModelInvocationProcessingHelper.createInstructionForRoot(this, context, taskFromModel, lensContextForRootTask, result);
		if (executionMode != ALL_IMMEDIATELY) {
			instructionForRoot.setHandlersBeforeModelOperation(WfPrepareRootOperationTaskHandler.HANDLER_URI);      // gather all deltas from child objects
		}
		return baseModelInvocationProcessingHelper.submitRootTask(instructionForRoot, taskFromModel, wfConfigurationType, result);
    }

    // Child task0 - in modes 2, 3 we have to prepare first child that executes all changes that do not require approval
    private WfTask submitTask0(ModelContext context, ObjectTreeDeltas changesWithoutApproval, WfTask rootWfTask, ExecutionMode executionMode,
			WfConfigurationType wfConfigurationType, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (changesWithoutApproval != null && !changesWithoutApproval.isEmpty() && executionMode != ALL_AFTERWARDS) {
            ModelContext task0context = contextCopyWithDeltasReplaced(context, changesWithoutApproval);
            WfTaskCreationInstruction instruction0 = WfTaskCreationInstruction.createModelOnly(rootWfTask.getChangeProcessor(), task0context);
            instruction0.setTaskName("Executing changes that do not require approval");
			return wfTaskController.submitWfTask(instruction0, rootWfTask, wfConfigurationType, result);
        } else {
            return null;
        }
    }

    private LensContext determineLensContextForRootTask(ModelContext context, ObjectTreeDeltas changesWithoutApproval, ExecutionMode executionMode) throws SchemaException {
        LensContext contextForRootTask;
        if (executionMode == ALL_AFTERWARDS) {
            contextForRootTask = contextCopyWithDeltasReplaced(context, changesWithoutApproval);
        } else if (executionMode == MIXED) {
            contextForRootTask = contextCopyWithNoDelta(context);
        } else {
            contextForRootTask = null;
        }
        return contextForRootTask;
    }

    private LensContext contextCopyWithDeltasReplaced(ModelContext context, ObjectTreeDeltas changes) throws SchemaException {
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
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareProcessInstanceAuditRecord(wfTask, stage, result);

        ObjectTreeDeltas<?> deltas;
        try {
            if (stage == REQUEST) {
                deltas = wfTaskUtil.retrieveDeltasToProcess(wfTask.getTask());
            } else {
                deltas = wfTaskUtil.retrieveResultingDeltas(wfTask.getTask());
            }
        } catch (SchemaException e) {
            throw new SystemException("Couldn't retrieve delta(s) from task " + wfTask.getTask(), e);
        }
        if (deltas != null) {
            List<ObjectDelta<?>> deltaList = deltas.getDeltaList();
            for (ObjectDelta delta : deltaList) {
                auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
            }
        }
        return auditEventRecord;
    }

    @Override
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(WorkItemType workItem, TaskEvent taskEvent, WfTask wfTask,
            OperationResult result) throws WorkflowException {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareWorkItemCreatedAuditRecord(workItem, wfTask, result);
        try {
            addDeltasToEventRecord(auditEventRecord,
                    getWfTaskUtil().retrieveDeltasToProcess(wfTask.getTask()));
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve deltas to be put into audit record", e);
        }
		return auditEventRecord;
    }

    @Override
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(WorkItemType workItem, WorkItemEventCauseInformationType cause,
            WorkItemResultType workItemResult, TaskEvent taskEvent, WfTask wfTask,
            OperationResult result) throws WorkflowException {
        AuditEventRecord auditEventRecord = baseAuditHelper.prepareWorkItemDeletedAuditRecord(workItem, taskEvent,
                cause, workItemResult, wfTask, result);
        try {
        	// TODO - or merge with original deltas?
        	if (workItemResult != null && workItemResult.getOutcome() == WorkItemOutcomeType.APPROVE
					&& workItemResult.getAdditionalDeltas() != null) {
				addDeltasToEventRecord(auditEventRecord,
						ObjectTreeDeltas.fromObjectTreeDeltasType(workItemResult.getAdditionalDeltas(), getPrismContext()));
			}
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve deltas to be put into audit record", e);
        }
		return auditEventRecord;
    }

    private void addDeltasToEventRecord(AuditEventRecord auditEventRecord, ObjectTreeDeltas<?> deltas) {
        if (deltas != null) {
			for (ObjectDelta<?> delta : deltas.getDeltaList()) {
				auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
			}
		}
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

    public void registerChangeAspect(PrimaryChangeAspect changeAspect, boolean first) {
        LOGGER.trace("Registering aspect implemented by {}; first={}", changeAspect.getClass(), first);
		if (first) {
			allChangeAspects.add(0, changeAspect);
		} else {
			allChangeAspects.add(changeAspect);
		}
    }

    WfTaskUtil getWfTaskUtil() {     // ugly hack - used in PcpJob
        return wfTaskUtil;
    }
    //endregion
}
