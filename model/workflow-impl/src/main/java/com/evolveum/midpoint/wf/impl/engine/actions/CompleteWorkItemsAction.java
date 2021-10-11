/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.CompleteWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class CompleteWorkItemsAction extends RequestedAction<CompleteWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(CompleteWorkItemsAction.class);

    private static final String OP_EXECUTE = CompleteWorkItemsAction.class.getName() + ".execute";

    public CompleteWorkItemsAction(EngineInvocationContext ctx, @NotNull CompleteWorkItemsRequest request) {
        super(ctx, request);
    }

    @Override
    public Action execute(OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .build();

        try {
            traceEnter(LOGGER);
            LOGGER.trace("Completions: {}", request.getCompletions());

            Set<String> outcomes = new HashSet<>();

            boolean closeOtherWorkItems = false;
            boolean approvalCase = ctx.isApprovalCase();

            ApprovalStageDefinitionType stageDef = approvalCase ? ctx.getCurrentStageDefinition() : null;
            LevelEvaluationStrategyType levelEvaluationStrategyType = approvalCase ? stageDef.getEvaluationStrategy() : null;

            XMLGregorianCalendar now = engine.clock.currentTimeXMLGregorianCalendar();
            for (CompleteWorkItemsRequest.SingleCompletion completion : request.getCompletions()) {
                CaseWorkItemType workItem = ctx.findWorkItemById(completion.getWorkItemId());

                if (!engine.authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.COMPLETE,
                        ctx.getTask(), result)) {
                    throw new SecurityViolationException("You are not authorized to complete the work item.");
                }

                if (workItem.getCloseTimestamp() != null) {
                    LOGGER.trace("Work item {} was already completed on {}", workItem.getId(), workItem.getCloseTimestamp());
                    result.recordWarning("Work item " + workItem.getId() + " was already completed on "
                            + workItem.getCloseTimestamp());
                    continue;
                }

                AbstractWorkItemOutputType output = completion.getOutput();
                String outcome = output.getOutcome();
                if (outcome != null) {
                    outcomes.add(outcome);
                }

                LOGGER.trace("+++ recordCompletionOfWorkItem ENTER: workItem={}, outcome={}", workItem, outcome);
                LOGGER.trace("======================================== Recording individual decision of {}", ctx.getPrincipal());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Recording decision for approval process instance {} (case oid {}), stage {}: decision: {}",
                            ctx.getProcessInstanceNameOrig(), ctx.getCaseOid(),
                            approvalCase ? ApprovalContextUtil.getStageDiagName(stageDef) : null,
                            output.getOutcome());
                }

                ObjectReferenceType performerRef = ObjectTypeUtil
                        .createObjectRef(ctx.getPrincipal().getFocus(), engine.prismContext);
                workItem.setOutput(output.clone());
                workItem.setPerformerRef(performerRef);
                workItem.setCloseTimestamp(now);

                if (approvalCase) {
                    boolean isApproved = ApprovalUtils.isApproved(outcome);
                    if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
                        LOGGER.trace("Finishing the stage, because the stage evaluation strategy is 'firstDecides'.");
                        closeOtherWorkItems = true;
                    } else if ((levelEvaluationStrategyType == null
                            || levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE) && !isApproved) {
                        LOGGER.trace(
                                "Finishing the stage, because the stage eval strategy is 'allMustApprove' and the decision was 'reject'.");
                        closeOtherWorkItems = true;
                    }
                } else {
                    // Operators are equivalent: if one completes the item, all items are done.
                    closeOtherWorkItems = true;
                }

                engine.workItemHelper.recordWorkItemClosure(ctx, workItem, true, request.getCauseInformation(), result);
            }

            if (closeOtherWorkItems) {
                doCloseOtherWorkItems(ctx, request.getCauseInformation(), now, result);
            }

            Action next;
            if (closeOtherWorkItems || !ctx.isAnyCurrentStageWorkItemOpen()) {
                if (approvalCase) {
                    next = new CloseStageAction(ctx, null);
                } else {
                    next = new CloseCaseAction(ctx, getOutcome(outcomes));
                }
            } else {
                next = null;
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

    // see ManualConnectorInstance.translateOutcome(..) method
    private String getOutcome(Set<String> outcomes) {
        if (outcomes.isEmpty()) {
            return OperationResultStatusType.SUCCESS.toString();
        } else if (outcomes.size() == 1) {
            return outcomes.iterator().next();
        } else {
            LOGGER.warn("Conflicting outcomes: {}", outcomes);
            return OperationResultStatusType.UNKNOWN.toString();
        }
    }

    private void doCloseOtherWorkItems(EngineInvocationContext ctx, WorkItemEventCauseInformationType causeInformation,
            XMLGregorianCalendar now, OperationResult result) throws SchemaException {
        WorkItemEventCauseTypeType causeType = causeInformation != null ? causeInformation.getType() : null;
        LOGGER.trace("+++ closeOtherWorkItems ENTER: ctx={}, cause type={}", ctx, causeType);
        for (CaseWorkItemType workItem : ctx.getCurrentCase().getWorkItem()) {
            if (workItem.getCloseTimestamp() == null) {
                workItem.setCloseTimestamp(now);
                engine.workItemHelper.recordWorkItemClosure(ctx, workItem, false, causeInformation, result);
            }
        }
        LOGGER.trace("--- closeOtherWorkItems EXIT: ctx={}", ctx);
    }
}
