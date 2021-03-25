/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.MidpointParsingMigrator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.engine.helpers.DelayedNotification;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
class OpenStageAction extends InternalAction {

    private static final Trace LOGGER = TraceManager.getTrace(OpenStageAction.class);

    private static final String OP_EXECUTE = OpenStageAction.class.getName() + ".execute";

    OpenStageAction(EngineInvocationContext ctx) {
        super(ctx);
    }

    @Override
    public Action execute(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_EXECUTE)
                .setMinor()
                .build();

        try {
            traceEnter(LOGGER);

            Action next;

            int numberOfStages = ctx.getNumberOfStages();
            int currentStage = ctx.getCurrentStage();
            if (currentStage == numberOfStages) {
                next = new CloseCaseAction(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
            } else {
                int stageToBe = currentStage + 1;
                ctx.getCurrentCase().setStageNumber(stageToBe);
                ApprovalStageDefinitionType stageDef = ctx.getCurrentStageDefinition();

                StageComputeHelper.ComputationResult preStageComputationResult =
                        engine.stageComputeHelper.computeStageApprovers(stageDef,
                                ctx.getCurrentCase(),
                                () -> engine.stageComputeHelper
                                        .getDefaultVariables(ctx.getCurrentCase(), ctx.getWfContext(), ctx.getTask().getChannel(),
                                                result),
                                StageComputeHelper.ComputationMode.EXECUTION, ctx.getTask(), result);

                ApprovalLevelOutcomeType predeterminedOutcome = preStageComputationResult.getPredeterminedOutcome();
                Set<ObjectReferenceType> approverRefs = preStageComputationResult.getApproverRefs();
                logPreStageComputationResult(preStageComputationResult, stageDef, predeterminedOutcome, approverRefs);

                if (predeterminedOutcome != null) {
                    next = new CloseStageAction(ctx, preStageComputationResult);
                } else {
                    assert !approverRefs.isEmpty();
                    XMLGregorianCalendar createTimestamp = engine.clock.currentTimeXMLGregorianCalendar();
                    XMLGregorianCalendar deadline;
                    if (stageDef.getDuration() != null) {
                        deadline = (XMLGregorianCalendar) createTimestamp.clone();
                        deadline.add(stageDef.getDuration());
                    } else {
                        deadline = null;
                    }
                    AtomicLong idCounter = new AtomicLong(
                            defaultIfNull(ctx.getCurrentCase().asPrismObject().getHighestId(), 0L) + 1);
                    for (ObjectReferenceType approverRef : approverRefs) {
                        CaseWorkItemType workItem = createWorkItem(stageDef, createTimestamp, deadline, idCounter, approverRef,
                                result);
                        prepareAuditAndNotifications(workItem, result, ctx, engine);
                        createTriggers(workItem, stageDef, result);
                        ctx.getCurrentCase().getWorkItem().add(workItem);
                    }
                    next = null;            // at least one work item was created, so we must wait for its completion
                }
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

    private void createTriggers(CaseWorkItemType workItem, ApprovalStageDefinitionType stageDef, OperationResult result) {
        engine.triggerHelper.createTriggersForTimedActions(ctx.getCurrentCase(), workItem.getId(), 0,
                XmlTypeConverter.toDate(workItem.getCreateTimestamp()),
                XmlTypeConverter.toDate(workItem.getDeadline()),
                stageDef.getTimedActions(),
                result);
    }

    static void prepareAuditAndNotifications(CaseWorkItemType workItem, OperationResult result,
            EngineInvocationContext ctx, WorkflowEngine engine) {
        AuditEventRecord auditEventRecord = engine.primaryChangeProcessor.prepareWorkItemCreatedAuditRecord(workItem,
                ctx.getCurrentCase(), result);
        ctx.addAuditRecord(auditEventRecord);
        try {
            List<ObjectReferenceType> assigneesAndDeputies = engine.miscHelper.getAssigneesAndDeputies(workItem, ctx.getTask(), result);
            for (ObjectReferenceType assigneesOrDeputy : assigneesAndDeputies) {
                // we assume originalAssigneeRef == assigneeRef in this case
                ctx.prepareNotification(new DelayedNotification.ItemCreation(ctx.getCurrentCase(), workItem, null, null, assigneesOrDeputy));
            }
            WorkItemAllocationChangeOperationInfo operationInfo =
                    new WorkItemAllocationChangeOperationInfo(null, Collections.emptyList(), assigneesAndDeputies);
            ctx.prepareNotification(new DelayedNotification.AllocationChangeNew(ctx.getCurrentCase(), workItem, operationInfo, null));
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare notification about work item create event", e);
        }
    }

    @NotNull
    private CaseWorkItemType createWorkItem(ApprovalStageDefinitionType stageDef, XMLGregorianCalendar createTimestamp,
            XMLGregorianCalendar deadline, AtomicLong idCounter, ObjectReferenceType approverRef, OperationResult result) {
        CaseWorkItemType workItem = new CaseWorkItemType(engine.prismContext)
                .name(ctx.getProcessInstanceName())
                .id(idCounter.getAndIncrement())
                .stageNumber(stageDef.getNumber())
                .createTimestamp(createTimestamp)
                .deadline(deadline);
        if (approverRef.getType() == null) {
            approverRef.setType(UserType.COMPLEX_TYPE);
        }
        if (QNameUtil.match(UserType.COMPLEX_TYPE, approverRef.getType())) {
            workItem.setOriginalAssigneeRef(approverRef.clone());
            workItem.getAssigneeRef().add(approverRef.clone());
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, approverRef.getType()) ||
                QNameUtil.match(OrgType.COMPLEX_TYPE, approverRef.getType()) ||
                QNameUtil.match(ServiceType.COMPLEX_TYPE, approverRef.getType())) {
            workItem.getCandidateRef().add(approverRef.clone());
            // todo what about originalAssigneeRef?
        } else {
            throw new IllegalStateException(
                    "Unsupported type of the approver: " + approverRef.getType() + " in " + approverRef);
        }
        if (stageDef.getAdditionalInformation() != null) {
            try {
                VariablesMap variables = engine.stageComputeHelper
                        .getDefaultVariables(ctx.getCurrentCase(), ctx.getWfContext(), ctx.getChannel(), result);
                List<InformationType> additionalInformation = engine.expressionEvaluationHelper
                        .evaluateExpression(stageDef.getAdditionalInformation(), variables,
                                "additional information expression", InformationType.class,
                                InformationType.COMPLEX_TYPE,
                                true, this::createInformationType, ctx.getTask(), result);
                workItem.getAdditionalInformation().addAll(additionalInformation);
            } catch (Throwable t) {
                throw new SystemException("Couldn't evaluate additional information expression in " + ctx, t);
            }
        }
        return workItem;
    }

    private InformationType createInformationType(Object o) {
        if (o == null || o instanceof InformationType) {
            return (InformationType) o;
        } else if (o instanceof String) {
            return MidpointParsingMigrator.stringToInformationType((String) o);
        } else {
            throw new IllegalArgumentException("Object cannot be converted into InformationType: " + o);
        }
    }

    private void logPreStageComputationResult(StageComputeHelper.ComputationResult preStageComputationResult,
            ApprovalStageDefinitionType stageDef,
            ApprovalLevelOutcomeType predeterminedOutcome,
            Set<ObjectReferenceType> approverRefs) {
        if (LOGGER.isDebugEnabled()) {
            if (preStageComputationResult.noApproversFound()) {
                LOGGER.debug("No approvers at the stage '{}' for process {} (case oid {}) - outcome-if-no-approvers is {}",
                        stageDef.getName(),
                        ctx.getProcessInstanceNameOrig(), ctx.getCurrentCase().getOid(), stageDef.getOutcomeIfNoApprovers());
            }
            LOGGER.debug("Approval process instance {} (case oid {}), stage {}: predetermined outcome: {}, approvers: {}",
                    ctx.getProcessInstanceNameOrig(), ctx.getCurrentCase().getOid(),
                    ApprovalContextUtil.getStageDiagName(stageDef), predeterminedOutcome, approverRefs);
        }
    }
}
