/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper.ComputationResult;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.*;

/**
 *
 */
public class CloseStageAction extends InternalAction {

    private static final Trace LOGGER = TraceManager.getTrace(CloseStageAction.class);

    private static final String OP_EXECUTE = CloseStageAction.class.getName() + ".execute";

    private final ComputationResult preStageComputationResult;

    CloseStageAction(EngineInvocationContext ctx, ComputationResult preStageComputationResult) {
        super(ctx);
        this.preStageComputationResult = preStageComputationResult;
    }

    @Override
    public Action execute(OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .build();

        try {
            traceEnter(LOGGER);

            int currentStage = ctx.getCurrentStage();
            ApprovalStageDefinitionType stageDef = ctx.getCurrentStageDefinition();

            boolean approved;
            if (preStageComputationResult != null) {
                ApprovalLevelOutcomeType outcome = preStageComputationResult.getPredeterminedOutcome();
                switch (outcome) {
                    case APPROVE:
                    case SKIP:
                        approved = true;
                        break;
                    case REJECT:
                        approved = false;
                        break;
                    default:
                        throw new IllegalStateException("Unknown outcome: " + outcome);        // TODO less draconian handling
                }
                recordAutoCompletionDecision(preStageComputationResult, currentStage);
            } else {
                LOGGER.trace(
                        "****************************************** Summarizing decisions in stage {} (stage evaluation strategy = {}): ",
                        stageDef.getName(), stageDef.getEvaluationStrategy());

                List<CaseWorkItemType> workItems = ctx.getWorkItemsForStage(currentStage);

                boolean allApproved = true;
                List<CaseWorkItemType> answeredWorkItems = new ArrayList<>();
                Set<String> outcomes = new HashSet<>();
                for (CaseWorkItemType workItem : workItems) {
                    LOGGER.trace(" - {}", workItem);
                    allApproved &= ApprovalUtils.isApproved(workItem.getOutput());
                    if (workItem.getCloseTimestamp() != null && workItem.getPerformerRef() != null) {
                        answeredWorkItems.add(workItem);
                        outcomes.add(workItem.getOutput() != null ? workItem.getOutput().getOutcome() : null);
                    }
                }
                if (stageDef.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
                    if (outcomes.size() > 1) {
                        LOGGER.warn(
                                "Ambiguous outcome with firstDecides strategy in {}: {} response(s), providing outcomes of {}",
                                ApprovalContextUtil.getBriefDiagInfo(ctx.getCurrentCase()), answeredWorkItems.size(), outcomes);
                        answeredWorkItems.sort(Comparator
                                .nullsLast(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getCloseTimestamp()))));
                        CaseWorkItemType first = answeredWorkItems.get(0);
                        approved = ApprovalUtils.isApproved(first.getOutput());
                        LOGGER.warn("Possible race condition, so taking the first one: {} ({})", approved, first);
                    } else {
                        approved = ApprovalUtils.isApproved(outcomes.iterator().next()) && !outcomes.isEmpty();
                    }
                } else {
                    approved = allApproved && !answeredWorkItems.isEmpty();
                }
            }

            engine.triggerHelper.removeAllStageTriggersForWorkItem(ctx.getCurrentCase());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Closing the stage for approval process instance {} (case oid {}), stage {}: result of this stage: {}",
                        ctx.getProcessInstanceNameOrig(),
                        ctx.getCaseOid(), ApprovalContextUtil.getStageDiagName(stageDef), approved);
            }

            Action next;
            if (approved) {
                if (currentStage < ctx.getNumberOfStages()) {
                    next = new OpenStageAction(ctx);
                } else {
                    next = new CloseCaseAction(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
                }
            } else {
                next = new CloseCaseAction(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
            }

            traceExit(LOGGER, next);
            return next;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void recordAutoCompletionDecision(ComputationResult computationResult, int stageNumber) {
        StageCompletionEventType event = new StageCompletionEventType(engine.prismContext);
        event.setTimestamp(engine.clock.currentTimeXMLGregorianCalendar());
        event.setStageNumber(stageNumber);
        event.setAutomatedDecisionReason(computationResult.getAutomatedCompletionReason());
        event.setOutcome(ApprovalUtils.toUri(computationResult.getPredeterminedOutcome()));
        ctx.addEvent(event);
    }
}
