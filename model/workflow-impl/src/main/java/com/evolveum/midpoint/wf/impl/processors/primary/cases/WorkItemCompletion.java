/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.LevelEvaluationStrategyType.ALL_MUST_AGREE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LevelEvaluationStrategyType.FIRST_DECIDES;

public class WorkItemCompletion {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemCompletion.class);

    @NotNull private final CaseWorkItemType workItem;
    @NotNull private final CaseEngineOperation operation;
    @NotNull private final CaseType currentCase;
    @NotNull private final ApprovalStageDefinitionType stageDef;

    public WorkItemCompletion(
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation) {
        this.workItem = workItem;
        this.operation = operation;
        this.currentCase = operation.getCurrentCase();
        this.stageDef = ApprovalContextUtil.getCurrentStageDefinitionRequired(currentCase);
    }

    public WorkItemCompletionResult process() throws SchemaException {

        logInput();

        mergeAdditionalDeltas();

        LevelEvaluationStrategyType stageEvalStrategy =
                Objects.requireNonNullElse(stageDef.getEvaluationStrategy(), ALL_MUST_AGREE);

        boolean cancelRemainingItems;
        if (stageEvalStrategy == FIRST_DECIDES) {
            LOGGER.trace("Will close the stage, because the stage evaluation strategy is 'firstDecides'.");
            cancelRemainingItems = true;
        } else if (stageEvalStrategy == ALL_MUST_AGREE && !isApproved()) {
            LOGGER.trace(
                    "Will close the stage, because the stage eval strategy is 'allMustApprove' and the decision was 'reject'.");
            cancelRemainingItems = true;
        } else {
            cancelRemainingItems = false;
        }

        return new WorkItemCompletionResultImpl(cancelRemainingItems);
    }

    /**
     * Puts deltas from the output to the deltas to approve.
     *
     * TODO why deltas to approve?
     * TODO what about non-focus (projection) deltas? they are ignored
     */
    private void mergeAdditionalDeltas() throws SchemaException {
        AbstractWorkItemOutputType output =
                Objects.requireNonNull(
                        workItem.getOutput(), () -> "No output in work item being completed: " + workItem);

        ObjectDeltaType additionalDelta =
                output instanceof WorkItemResultType
                        && ((WorkItemResultType) output).getAdditionalDeltas() != null ?
                        ((WorkItemResultType) output).getAdditionalDeltas().getFocusPrimaryDelta() : null;
        if (additionalDelta != null) {
            LOGGER.trace("Merging additional deltas from the output:\n{}", additionalDelta);
            ApprovalContextType aCtx = ApprovalContextUtil.getApprovalContextRequired(currentCase);
            aCtx.setDeltasToApprove(
                    ObjectTreeDeltas.mergeDeltas(aCtx.getDeltasToApprove(), additionalDelta));
        }
    }

    private void logInput() {
        LOGGER.trace("+++ Processing completion of work item (in approvals): outcome={}, principal={}, workItem:\n{}",
                getOutcome(), operation.getPrincipal().getFocus(), workItem.debugDumpLazily(1));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Recording decision for approval case '{}', stage {}: decision: {}",
                    currentCase, ApprovalContextUtil.getStageDiagName(stageDef), getOutcome());
        }
    }

    private String getOutcome() {
        return workItem.getOutput().getOutcome();
    }

    private boolean isApproved() {
        return ApprovalUtils.isApproved(getOutcome());
    }
}
