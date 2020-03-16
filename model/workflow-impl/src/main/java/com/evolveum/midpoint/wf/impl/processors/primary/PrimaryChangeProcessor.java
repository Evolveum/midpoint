/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.OpenCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.engine.helpers.AuditHelper;
import com.evolveum.midpoint.wf.impl.execution.CaseOperationExecutionTaskHandler;
import com.evolveum.midpoint.wf.impl.execution.ExecutionHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.*;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.audit.api.AuditEventStage.REQUEST;
import static com.evolveum.midpoint.model.api.context.ModelState.PRIMARY;

/**
 * @author mederly
 */
@Component
public class PrimaryChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    private static final String CLASS_DOT = PrimaryChangeProcessor.class.getName() + ".";
    private static final String OP_PREVIEW_OR_PROCESS_MODEL_INVOCATION = CLASS_DOT + "previewOrProcessModelInvocation";
    private static final String OP_GATHER_START_INSTRUCTIONS = CLASS_DOT + "gatherStartInstructions";
    private static final String OP_EXECUTE_START_INSTRUCTIONS = CLASS_DOT + "executeStartInstructions";

    @Autowired private ConfigurationHelper configurationHelper;
    @Autowired private ModelHelper modelHelper;
    @Autowired private AuditHelper auditHelper;
    @Autowired private StageComputeHelper stageComputeHelper;
    @Autowired private PcpGeneralHelper generalHelper;
    @Autowired private MiscHelper miscHelper;
    @Autowired private ExecutionHelper executionHelper;
    @Autowired private TaskManager taskManager;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private WorkflowEngine workflowEngine;

    private List<PrimaryChangeAspect> allChangeAspects = new ArrayList<>();

    //region Configuration
    // =================================================================================== Configuration
    @PostConstruct
    public void init() {
        configurationHelper.registerProcessor(this);
    }
    //endregion

    //region Processing model invocation
    // =================================================================================== Processing model invocation

    // beware, may damage model context during execution
    public List<PcpStartInstruction> previewModelInvocation(@NotNull ModelInvocationContext<?> context,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        List<PcpStartInstruction> rv = new ArrayList<>();
        previewOrProcessModelInvocation(context, true, rv, result);
        return rv;
    }

    @Override
    public HookOperationMode processModelInvocation(@NotNull ModelInvocationContext<?> ctx, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (ctx.modelContext.getState() == PRIMARY) {
            return previewOrProcessModelInvocation(ctx, false, null, result);
        } else {
            return null;
        }
    }

    private <O extends ObjectType> HookOperationMode previewOrProcessModelInvocation(@NotNull ModelInvocationContext<O> ctx,
            boolean previewOnly, List<PcpStartInstruction> startInstructionsHolder, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_PREVIEW_OR_PROCESS_MODEL_INVOCATION)
                .addParam("previewOnly", previewOnly)
                .build();
        try {

            if (ctx.modelContext.getFocusContext() == null) {
                return null;
            }

            PrimaryChangeProcessorConfigurationType processorConfig =
                    ctx.wfConfiguration != null ? ctx.wfConfiguration.getPrimaryChangeProcessor() : null;
            if (processorConfig != null && Boolean.FALSE.equals(processorConfig.isEnabled())) {
                LOGGER.debug("Primary change processor is disabled.");
                return null;
            }

            ObjectTreeDeltas<O> objectTreeDeltas = ctx.modelContext.getTreeDeltas();
            if (objectTreeDeltas.isEmpty()) {
                return null;
            }

            // examine the request using process aspects
            ObjectTreeDeltas<O> changesBeingDecomposed = objectTreeDeltas.clone();
            List<PcpStartInstruction> startInstructions = gatherStartInstructions(changesBeingDecomposed, ctx, result);

            // start the process(es)
            removeEmptyProcesses(startInstructions, ctx, result);
            if (startInstructions.isEmpty()) {
                LOGGER.debug("There are no workflow processes to be started, exiting.");
                return null;
            }
            if (previewOnly) {
                startInstructionsHolder.addAll(startInstructions);
                return null;
            } else {
                return executeStartInstructions(startInstructions, ctx, changesBeingDecomposed, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void removeEmptyProcesses(List<PcpStartInstruction> instructions, ModelInvocationContext ctx,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        for (Iterator<PcpStartInstruction> iterator = instructions.iterator(); iterator.hasNext(); ) {
            PcpStartInstruction instruction = iterator.next();
            if (instruction.startsWorkflowProcess() &&
                    isEmpty(instruction, stageComputeHelper, ctx, result)) {
                LOGGER.debug("Skipping empty processing instruction: {}", DebugUtil.debugDumpLazily(instruction));
                iterator.remove();
            }
        }
    }

    // skippability because of no approvers was already tested; see ApprovalSchemaHelper.shouldBeSkipped
    public boolean isEmpty(PcpStartInstruction instruction,
            StageComputeHelper stageComputeHelper, ModelInvocationContext ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ApprovalContextType actx = instruction.getApprovalContext();
        if (actx == null) {
            return true;
        }
        List<ApprovalStageDefinitionType> stages = actx.getApprovalSchema().getStage();
        // first pass: if there is any stage that is obviously not skippable, let's return false without checking the expressions
        for (ApprovalStageDefinitionType stage : stages) {
            if (stage.getAutomaticallyCompleted() == null) {
                return false;
            }
        }
        // second pass: check the conditions
        for (ApprovalStageDefinitionType stage : stages) {
            if (!SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP.equals(
                    evaluateAutoCompleteExpression(instruction.getCase(), stage, instruction, stageComputeHelper, ctx, result))) {
                return false;
            }
        }
        return true;
    }

    private String evaluateAutoCompleteExpression(CaseType aCase,
            ApprovalStageDefinitionType stageDef, PcpStartInstruction instruction,
            StageComputeHelper stageComputeHelper, ModelInvocationContext ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionVariables variables = stageComputeHelper.getDefaultVariables(aCase, instruction.getApprovalContext(), ctx.task.getChannel(), result);
        return stageComputeHelper.evaluateAutoCompleteExpression(stageDef, variables, ctx.task, result);
    }


    private <O extends ObjectType> List<PcpStartInstruction> gatherStartInstructions(
            @NotNull ObjectTreeDeltas<O> changesBeingDecomposed, @NotNull ModelInvocationContext<O> ctx,
            @NotNull OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_GATHER_START_INSTRUCTIONS)
                .setMinor()
                .build();
        try {
            PrimaryChangeProcessorConfigurationType processorConfigurationType =
                    ctx.wfConfiguration != null ? ctx.wfConfiguration.getPrimaryChangeProcessor() : null;
            List<PcpStartInstruction> startProcessInstructions = new ArrayList<>();
            for (PrimaryChangeAspect aspect : getActiveChangeAspects(processorConfigurationType)) {
                if (changesBeingDecomposed.isEmpty()) {      // nothing left
                    break;
                }
                List<PcpStartInstruction> instructions = aspect.getStartInstructions(changesBeingDecomposed, ctx, result);
                logAspectResult(aspect, instructions, changesBeingDecomposed);
                startProcessInstructions.addAll(instructions);
            }
            result.addParam("instructionsCount", startProcessInstructions.size());
            return startProcessInstructions;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private Collection<PrimaryChangeAspect> getActiveChangeAspects(PrimaryChangeProcessorConfigurationType configuration) {
        return allChangeAspects.stream()
                .filter(aspect -> aspect.isEnabled(configuration))
                .collect(Collectors.toList());
    }

    private void logAspectResult(PrimaryChangeAspect aspect, List<? extends StartInstruction> instructions, ObjectTreeDeltas changesBeingDecomposed) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("\n---[ Aspect {} returned the following process start instructions (count: {}) ]---",
                    aspect.getClass(), instructions == null ? "(null)" : instructions.size());
            if (instructions != null) {
                for (StartInstruction instruction : instructions) {
                    LOGGER.trace(instruction.debugDump(0));
                }
                LOGGER.trace("Remaining delta(s):\n{}", changesBeingDecomposed.debugDump());
            }
        }
    }

    private HookOperationMode executeStartInstructions(List<PcpStartInstruction> instructions, ModelInvocationContext<?> ctx,
            ObjectTreeDeltas<?> changesWithoutApproval, OperationResult parentResult) {
        // Note that this result cannot be minor, because we need to be able to retrieve case OID. And minor results get cut off.
        OperationResult result = parentResult.subresult(OP_EXECUTE_START_INSTRUCTIONS)
                .build();
        try {
            // prepare root case and case0
            CaseType rootCase = addRoot(ctx, result);
            PcpStartInstruction instruction0 = createInstruction0(ctx, changesWithoutApproval, rootCase);
            CaseType case0 = instruction0 != null ? modelHelper.addCase(instruction0, ctx.task, result) : null;

            CaseType objectCreationCase = instruction0 != null && instruction0.isObjectCreationInstruction() ? case0 : null;

            List<CaseType> allSubcases = new ArrayList<>(instructions.size() + 1);
            CollectionUtils.addIgnoreNull(allSubcases, case0);

            List<String> casesToStart = new ArrayList<>();

            // create the regular (approval) child cases
            for (PcpStartInstruction instruction : instructions) {
                instruction.setParent(rootCase);
                CaseType subCase = modelHelper.addCase(instruction, ctx.task, result);
                allSubcases.add(subCase);
                if (instruction.isObjectCreationInstruction()) {
                    if (objectCreationCase == null) {
                        objectCreationCase = subCase;
                    } else {
                        throw new IllegalStateException("More than one case that creates the object: " +
                                objectCreationCase + " and " + subCase);
                    }
                }
                if (instruction.startsWorkflowProcess()) {
                    casesToStart.add(subCase.getOid());
                }
            }

            // create dependencies
            for (CaseType subcase : allSubcases) {
                List<CaseType> prerequisites = new ArrayList<>();
                if (objectCreationCase != null && objectCreationCase != subcase) {
                    prerequisites.add(objectCreationCase);
                }
                if (case0 != null && case0 != objectCreationCase && case0 != subcase) {
                    prerequisites.add(case0);
                }
                generalHelper.addPrerequisites(subcase, prerequisites, result);
            }

            modelHelper.logJobsBeforeStart(rootCase, ctx.task, result);

            if (case0 != null) {
                if (ModelExecuteOptions.isExecuteImmediatelyAfterApproval(ctx.modelContext.getOptions())) {
                    submitExecutionTask(case0, false, result);
                } else {
                    executionHelper.closeCaseInRepository(case0, result);
                }
            }

            LOGGER.trace("Starting the cases: {}", casesToStart);
            for (String caseToStart : casesToStart) {
                workflowEngine.executeRequest(new OpenCaseRequest(caseToStart), ctx.task, result);
            }

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException | ConfigurationException | ExpressionEvaluationException | RuntimeException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;

            // todo rollback - e.g. delete already created cases
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private CaseType addRoot(ModelInvocationContext<?> ctx, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        LensContext<?> contextForRoot = contextCopyWithNoDelta(ctx.modelContext);
        StartInstruction instructionForRoot =
                modelHelper.createInstructionForRoot(this, ctx, contextForRoot, result);
        return modelHelper.addRoot(instructionForRoot, ctx.task, result);
    }

    private PcpStartInstruction createInstruction0(ModelInvocationContext<?> ctx, ObjectTreeDeltas<?> changesWithoutApproval,
            CaseType rootCase) throws SchemaException {
        if (changesWithoutApproval != null && !changesWithoutApproval.isEmpty()) {
            PcpStartInstruction instruction0 = PcpStartInstruction.createEmpty(this, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
            instruction0.setName("Changes that do not require approval");
            instruction0.setObjectRef(ctx);
            instruction0.setDeltasToApprove(changesWithoutApproval);
            instruction0.setResultingDeltas(changesWithoutApproval);
            instruction0.setParent(rootCase);
            instruction0.setExecuteApprovedChangeImmediately(ctx.modelContext);
            return instruction0;
        } else {
            return null;
        }
    }

//    private LensContext determineLensContextForRootCase(ModelContext context, ObjectTreeDeltas changesWithoutApproval, ExecutionMode executionMode) throws SchemaException {
//        LensContext contextForRootTask;
//        if (executionMode == ALL_AFTERWARDS) {
//            contextForRootTask = contextCopyWithDeltasReplaced(context, changesWithoutApproval);
//        } else if (executionMode == MIXED) {
//            contextForRootTask = contextCopyWithNoDelta(context);
//        } else {
//            contextForRootTask = null;
//        }
//        return contextForRootTask;
//    }
//
//    private LensContext contextCopyWithDeltasReplaced(ModelContext context, ObjectTreeDeltas changes) throws SchemaException {
//        Validate.notNull(changes, "changes");
//        LensContext contextCopy = ((LensContext) context).clone();
//
//        contextCopy.replacePrimaryFocusDelta(changes.getFocusChange());
//        Map<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> changeMap = changes.getProjectionChangeMap();
//        Collection<ModelProjectionContext> projectionContexts = contextCopy.getProjectionContexts();
//        for (ModelProjectionContext projectionContext : projectionContexts) {
//            ObjectDelta<ShadowType> projectionDelta = changeMap.get(projectionContext.getResourceShadowDiscriminator());
//            projectionContext.setPrimaryDelta(projectionDelta);
//        }
//        contextCopy.deleteSecondaryDeltas();
//        return contextCopy;
//    }

    private LensContext contextCopyWithNoDelta(ModelContext<?> context) {
        LensContext<?> contextCopy = ((LensContext<?>) context).clone();
        contextCopy.replacePrimaryFocusDelta(null);
        Collection<LensProjectionContext> projectionContexts = contextCopy.getProjectionContexts();
        for (Iterator<LensProjectionContext> iterator = projectionContexts.iterator(); iterator.hasNext(); ) {
            ModelProjectionContext projectionContext = iterator.next();
            if (projectionContext.getPrimaryDelta() == null && ObjectDelta.isEmpty(projectionContext.getSyncDelta())) {
                iterator.remove();
            } else {
                projectionContext.setPrimaryDelta(null);
            }
        }
        contextCopy.deleteSecondaryDeltas();
        return contextCopy;
    }
    //endregion

    //region Processing process finish event

    /**
     * This method is called OUTSIDE the workflow engine computation - i.e. changes are already committed into repository.
     */
    @Override
    public void onProcessEnd(EngineInvocationContext ctx, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, PreconditionViolationException {
        CaseType currentCase = ctx.getCurrentCase();

        ObjectTreeDeltas<?> deltas = prepareDeltaOut(currentCase);
        generalHelper.storeResultingDeltas(currentCase, deltas, result);
        // note: resulting deltas are not stored in currentCase object (these are in repo only)

        // here we should execute the deltas, if appropriate!

        CaseType rootCase = generalHelper.getRootCase(currentCase, result);
        if (CaseTypeUtil.isClosed(rootCase)) {
            LOGGER.debug("Root case ({}) is already closed; not starting any execution tasks for {}", rootCase, currentCase);
            executionHelper.closeCaseInRepository(currentCase, result);
        } else {
            LensContextType modelContext = rootCase.getModelContext();
            if (modelContext == null) {
                throw new IllegalStateException("No model context in root case " + rootCase);
            }
            boolean immediately = modelContext.getOptions() != null &&
                    Boolean.TRUE.equals(modelContext.getOptions().isExecuteImmediatelyAfterApproval());
            if (immediately) {
                if (deltas != null) {
                    LOGGER.debug("Case {} is approved with immediate execution -- let's start the process", currentCase);
                    boolean waiting;
                    if (!currentCase.getPrerequisiteRef().isEmpty()) {
                        ObjectQuery query = prismContext.queryFor(CaseType.class)
                                .id(currentCase.getPrerequisiteRef().stream().map(ObjectReferenceType::getOid)
                                        .toArray(String[]::new))
                                .and().not().item(CaseType.F_STATE).eq(SchemaConstants.CASE_STATE_CLOSED)
                                .build();
                        SearchResultList<PrismObject<CaseType>> openPrerequisites = repositoryService
                                .searchObjects(CaseType.class, query, null, result);
                        waiting = !openPrerequisites.isEmpty();
                        if (waiting) {
                            LOGGER.debug(
                                    "Case {} cannot be executed now because of the following open prerequisites: {} -- the execution task will be created in WAITING state",
                                    currentCase, openPrerequisites);
                        }
                    } else {
                        waiting = false;
                    }
                    submitExecutionTask(currentCase, waiting, result);
                } else {
                    LOGGER.debug("Case {} is rejected (with immediate execution) -- nothing to do here", currentCase);
                    executionHelper.closeCaseInRepository(currentCase, result);
                    executionHelper.checkDependentCases(currentCase.getParentRef().getOid(), result);
                }
            } else {
                LOGGER.debug("Case {} is completed; but execution is delayed so let's check other subcases of {}",
                        currentCase, rootCase);
                executionHelper.closeCaseInRepository(currentCase, result);
                List<CaseType> subcases = miscHelper.getSubcases(rootCase, result);
                if (subcases.stream().allMatch(CaseTypeUtil::isClosed)) {
                    LOGGER.debug("All subcases of {} are closed, so let's execute the deltas", rootCase);
                    submitExecutionTask(rootCase, false, result);
                } else {
                    LOGGER.debug("Some subcases of {} are not closed yet. Delta execution is therefore postponed.", rootCase);
                    for (CaseType subcase : subcases) {
                        LOGGER.debug(" - {}: state={} (isClosed={})", subcase, subcase.getState(),
                                CaseTypeUtil.isClosed(subcase));
                    }
                }
            }
        }
    }

    private void submitExecutionTask(CaseType aCase, boolean waiting, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        // We must do this before the task is started, because as part of task completion we set state to CLOSED.
        // So if we set state to EXECUTING after the task is started, the case might be already closed at that point.
        // (If task is fast enough.)
        executionHelper.setCaseStateInRepository(aCase, SchemaConstants.CASE_STATE_EXECUTING, result);

        Task task = taskManager.createTaskInstance("execute");
        task.setName("Execution of " + aCase.getName().getOrig());
        task.setOwner(getExecutionTaskOwner(result));
        task.setObjectRef(ObjectTypeUtil.createObjectRef(aCase, prismContext));
        task.setHandlerUri(CaseOperationExecutionTaskHandler.HANDLER_URI);
        if (waiting) {
            task.setInitialExecutionStatus(TaskExecutionStatus.WAITING);
        }
        executionHelper.setExecutionConstraints(task, aCase, result);
        taskManager.switchToBackground(task, result);
    }

    private PrismObject<UserType> getExecutionTaskOwner(OperationResult result) throws SchemaException, ObjectNotFoundException {
        return repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result);
    }

    private ObjectTreeDeltas<?> prepareDeltaOut(CaseType aCase) throws SchemaException {
        ObjectTreeDeltas<?> deltaIn = generalHelper.retrieveDeltasToApprove(aCase);
        if (ApprovalUtils.isApprovedFromUri(aCase.getOutcome())) {
            return deltaIn;
        } else {
            return null;
        }
    }


    //endregion

    //region Auditing
    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(CaseType aCase, AuditEventStage stage, ApprovalContextType wfContext, OperationResult result) {
        AuditEventRecord auditEventRecord = auditHelper.prepareProcessInstanceAuditRecord(aCase, stage, result);
        addDeltaIfNeeded(auditEventRecord, stage == REQUEST, aCase);
        return auditEventRecord;
    }

    private void addDeltaIfNeeded(AuditEventRecord auditEventRecord, boolean toApprove, CaseType aCase) {
        if (CaseTypeUtil.isApprovalCase(aCase)) {
            ObjectTreeDeltas<?> deltas;
            try {
                if (toApprove) {
                    deltas = generalHelper.retrieveDeltasToApprove(aCase);
                } else {
                    deltas = generalHelper.retrieveResultingDeltas(aCase);
                }
            } catch (SchemaException e) {
                throw new SystemException("Couldn't retrieve delta(s) from case " + aCase, e);
            }
            if (deltas != null) {
                List<ObjectDelta<? extends ObjectType>> deltaList = deltas.getDeltaList();
                for (ObjectDelta<? extends ObjectType> delta : deltaList) {
                    auditEventRecord.addDelta(new ObjectDeltaOperation<>(delta));
                }
            }
        }
    }

    @Override
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem, CaseType aCase,
            OperationResult result) {
        AuditEventRecord auditEventRecord = auditHelper.prepareWorkItemCreatedAuditRecord(workItem, aCase, result);
        addDeltaIfNeeded(auditEventRecord, true, aCase);
        return auditEventRecord;
    }

    @Override
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase, OperationResult result) {
        AuditEventRecord auditEventRecord = auditHelper.prepareWorkItemDeletedAuditRecord(workItem, cause, aCase, result);
        addDeltaIfNeeded(auditEventRecord, true, aCase);
        return auditEventRecord;
    }
    //endregion

    //region Getters and setters


    public void registerChangeAspect(PrimaryChangeAspect changeAspect, boolean first) {
        LOGGER.trace("Registering aspect implemented by {}; first={}", changeAspect.getClass(), first);
        if (first) {
            allChangeAspects.add(0, changeAspect);
        } else {
            allChangeAspects.add(changeAspect);
        }
    }

    //endregion
}
