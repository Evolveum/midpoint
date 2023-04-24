/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.processors.primary;

import static com.evolveum.midpoint.audit.api.AuditEventStage.REQUEST;
import static com.evolveum.midpoint.model.api.context.ModelState.PRIMARY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.wf.impl.processors.primary.cases.CaseClosing;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.request.OpenCaseRequest;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;
import com.evolveum.midpoint.wf.impl.execution.ExecutionHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.*;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class PrimaryChangeProcessor implements ChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeProcessor.class);

    private static final String CLASS_DOT = PrimaryChangeProcessor.class.getName() + ".";
    private static final String OP_PREVIEW_OR_PROCESS_MODEL_INVOCATION = CLASS_DOT + "previewOrProcessModelInvocation";
    private static final String OP_GATHER_START_INSTRUCTIONS = CLASS_DOT + "gatherStartInstructions";
    private static final String OP_EXECUTE_START_INSTRUCTIONS = CLASS_DOT + "executeStartInstructions";

    @Autowired private ConfigurationHelper configurationHelper;
    @Autowired private ModelHelper modelHelper;
    @Autowired private StageComputeHelper stageComputeHelper;
    @Autowired private PcpGeneralHelper generalHelper;
    @Autowired private CaseMiscHelper caseMiscHelper;
    @Autowired private ExecutionHelper executionHelper;
    @Autowired protected MiscHelper miscHelper;

    @Autowired private ApprovalBeans beans;

    @Autowired private CaseEngineImpl caseEngine;

    private final List<PrimaryChangeAspect> allChangeAspects = new ArrayList<>();

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
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void removeEmptyProcesses(List<PcpStartInstruction> instructions, ModelInvocationContext<?> ctx,
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
            StageComputeHelper stageComputeHelper, ModelInvocationContext<?> ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
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
            String autoCompletionResult = evaluateAutoCompleteExpression(instruction.getCase(), stage, instruction,
                    stageComputeHelper, ctx, result);
            if (!QNameUtil.matchUri(SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP, autoCompletionResult)) {
                return false;
            }
        }
        return true;
    }

    private String evaluateAutoCompleteExpression(CaseType aCase,
            ApprovalStageDefinitionType stageDef, PcpStartInstruction instruction,
            StageComputeHelper stageComputeHelper, ModelInvocationContext<?> ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = caseMiscHelper.getDefaultVariables(
                aCase, instruction.getApprovalContext(), ctx.task.getChannel(), result);
        return stageComputeHelper.evaluateAutoCompleteExpression(stageDef, variables, ctx.task, result);
    }

    private <O extends ObjectType> List<PcpStartInstruction> gatherStartInstructions(
            @NotNull ObjectTreeDeltas<O> changesBeingDecomposed, @NotNull ModelInvocationContext<O> ctx,
            @NotNull OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException {

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

    private void logAspectResult(PrimaryChangeAspect aspect, List<? extends StartInstruction> instructions, ObjectTreeDeltas<?> changesBeingDecomposed) {
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
            CaseType case0 = instruction0 != null ? modelHelper.addCase(instruction0, result) : null;

            CaseType objectCreationCase = instruction0 != null && instruction0.isObjectCreationInstruction() ? case0 : null;

            List<CaseType> allSubcases = new ArrayList<>(instructions.size() + 1);
            CollectionUtils.addIgnoreNull(allSubcases, case0);

            List<String> casesToStart = new ArrayList<>();

            // create the regular (approval) child cases
            for (PcpStartInstruction instruction : instructions) {
                instruction.setParent(rootCase);
                CaseType subCase = modelHelper.addCase(instruction, result);
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

            modelHelper.logJobsBeforeStart(rootCase, result);

            if (case0 != null) {
                if (ModelExecuteOptions.isExecuteImmediatelyAfterApproval(ctx.modelContext.getOptions())) {
                    executionHelper.submitExecutionTask(case0, false, result);
                } else {
                    executionHelper.closeCaseInRepository(case0, result);
                }
            }

            LOGGER.trace("Starting the cases: {}", casesToStart);
            for (String caseToStart : casesToStart) {
                caseEngine.executeRequest(new OpenCaseRequest(caseToStart), ctx.task, result);
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
        return modelHelper.addRoot(instructionForRoot, result);
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

    private LensContext<?> contextCopyWithNoDelta(ModelContext<?> context) {
        LensContext<?> contextCopy = ((LensContext<?>) context).clone();
        contextCopy.getFocusContext().setPrimaryDeltaAfterStart(null);
        Collection<LensProjectionContext> projectionContexts = contextCopy.getProjectionContexts();
        for (Iterator<LensProjectionContext> iterator = projectionContexts.iterator(); iterator.hasNext(); ) {
            LensProjectionContext projectionContext = iterator.next();
            if (projectionContext.getPrimaryDelta() == null && ObjectDelta.isEmpty(projectionContext.getSyncDelta())) {
                iterator.remove();
            } else {
                projectionContext.setPrimaryDeltaAfterStart(null);
            }
        }
        contextCopy.deleteNonTransientComputationResults();
        return contextCopy;
    }
    //endregion

    //region Processing process finish event
    /**
     * This method is called OUTSIDE the workflow engine computation - i.e. changes are already committed into repository.
     */
    @Override
    public void finishCaseClosing(CaseEngineOperation operation, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {

        new CaseClosing(operation, beans)
                .finishCaseClosing(result);
    }
    //endregion

    //region Auditing

    @Override
    public void enrichCaseAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation) {
        addDeltaIfNeeded(auditEventRecord, auditEventRecord.getEventStage() == REQUEST, operation.getCurrentCase());
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation) {
        addDeltaIfNeeded(auditEventRecord, true, operation.getCurrentCase());
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(AuditEventRecord auditEventRecord, CaseEngineOperation operation) {
        addDeltaIfNeeded(auditEventRecord, true, operation.getCurrentCase());
    }

    private void addDeltaIfNeeded(AuditEventRecord auditEventRecord, boolean toApprove, CaseType aCase) {
        if (CaseTypeUtil.isApprovalCase(aCase)) { // TODO needed?
            ObjectTreeDeltas<?> deltas;
            try {
                if (toApprove) {
                    // Note that we take all deltas to approve from the case. This may be imprecise
                    // if there are some deltas added by a work item (~ additional deltas), while
                    // a work item that referred to original deltas, will be also logged with these
                    // additional deltas!
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

    @Override
    public MiscHelper getMiscHelper() {
        return miscHelper;
    }
    //endregion
}
