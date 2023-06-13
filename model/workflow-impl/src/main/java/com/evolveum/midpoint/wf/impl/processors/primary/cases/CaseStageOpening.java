/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.cases.api.temporary.ComputationMode;
import com.evolveum.midpoint.schema.MidpointParsingMigrator;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;
import com.evolveum.midpoint.wf.impl.util.ComputationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Opens the approval stage.
 *
 * Responsibilities:
 *
 * 1. invokes the stage computer to get a list of approvers (or predetermined outcome),
 * 2. creates work items (if applicable)
 */
public class CaseStageOpening extends AbstractCaseStageProcessing {

    private static final Trace LOGGER = TraceManager.getTrace(CaseStageOpening.class);

    /** Work items that should be put into the case for this stage. */
    @NotNull private final List<CaseWorkItemType> newWorkItems = new ArrayList<>();

    public CaseStageOpening(@NotNull CaseEngineOperation operation, @NotNull ApprovalBeans beans) {
        super(operation, beans);
    }

    public @NotNull StageOpeningResult process(OperationResult result) throws SchemaException {

        ComputationResult preStageComputationResult =
                beans.stageComputeHelper.computeStageApprovers(
                        stageDef,
                        currentCase,
                        () -> beans.caseMiscHelper.getDefaultVariables(
                                currentCase,
                                approvalContext,
                                task.getChannel(),
                                result),
                        ComputationMode.EXECUTION,
                        task,
                        result);

        logPreStageComputationResult(preStageComputationResult);

        if (preStageComputationResult.predeterminedOutcome == null) {
            createWorkItems(preStageComputationResult, result);
        }

        return new ApprovalStageOpeningResultImpl(
                preStageComputationResult.createStageClosingInformation(),
                newWorkItems,
                stageDef.getTimedActions());
    }

    private void logPreStageComputationResult(ComputationResult preStageComputationResult) {
        if (LOGGER.isDebugEnabled()) {
            if (preStageComputationResult.noApproversFound()) {
                LOGGER.debug("No approvers at the stage '{}' for process {} (case oid {}) - outcome-if-no-approvers is {}",
                        stageDef.getName(), currentCase.getName(), currentCase.getOid(), stageDef.getOutcomeIfNoApprovers());
            }
            LOGGER.debug("Approval process instance {} (case oid {}), stage {}: predetermined outcome: {}, approvers: {}",
                    currentCase.getName(), currentCase.getOid(),
                    ApprovalContextUtil.getStageDiagName(stageDef),
                    preStageComputationResult.getPredeterminedOutcome(),
                    preStageComputationResult.getApproverRefs());
        }
    }

    private void createWorkItems(ComputationResult preStageComputationResult, OperationResult result) {
        Set<ObjectReferenceType> approverRefs = preStageComputationResult.approverRefs;
        assert !approverRefs.isEmpty();
        XMLGregorianCalendar createTimestamp = beans.clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar deadline = getDeadline(createTimestamp);
        for (ObjectReferenceType approverRef : approverRefs) {
            newWorkItems.add(
                    createWorkItem(createTimestamp, deadline, approverRef, result));
        }
    }

    private @Nullable XMLGregorianCalendar getDeadline(XMLGregorianCalendar createTimestamp) {
        if (stageDef.getDuration() != null) {
            XMLGregorianCalendar deadline = (XMLGregorianCalendar) createTimestamp.clone();
            deadline.add(stageDef.getDuration());
            return deadline;
        } else {
            return null;
        }
    }

    private @NotNull CaseWorkItemType createWorkItem(
            @NotNull XMLGregorianCalendar createTimestamp,
            @Nullable XMLGregorianCalendar deadline,
            @NotNull ObjectReferenceType approverRef,
            @NotNull OperationResult result) {
        CaseWorkItemType workItem = new CaseWorkItemType()
                .name(currentCase.getName())
                .stageNumber(stageDef.getNumber())
                .createTimestamp(createTimestamp)
                .deadline(deadline);
        if (approverRef.getType() == null) {
            approverRef.setType(UserType.COMPLEX_TYPE);
        }
        setAssigneesOrCandidates(workItem, approverRef);
        computeAdditionalInformation(workItem, result);
        return workItem;
    }

    private void setAssigneesOrCandidates(CaseWorkItemType workItem, ObjectReferenceType approverRef) {
        if (isUser(approverRef)) {
            workItem.setOriginalAssigneeRef(approverRef.clone());
            workItem.getAssigneeRef().add(approverRef.clone());
        } else if (isAbstractRole(approverRef)) {
            workItem.getCandidateRef().add(approverRef.clone());
            // todo what about originalAssigneeRef? should we fill it?
        } else {
            throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType() + " in " + approverRef);
        }
    }

    // TODO isn't there a method in schema registry for this?
    private boolean isAbstractRole(ObjectReferenceType approverRef) {
        return QNameUtil.match(RoleType.COMPLEX_TYPE, approverRef.getType())
                || QNameUtil.match(OrgType.COMPLEX_TYPE, approverRef.getType())
                || QNameUtil.match(ServiceType.COMPLEX_TYPE, approverRef.getType())
                || QNameUtil.match(ArchetypeType.COMPLEX_TYPE, approverRef.getType());
    }

    private boolean isUser(ObjectReferenceType approverRef) {
        return QNameUtil.match(UserType.COMPLEX_TYPE, approverRef.getType());
    }

    private void computeAdditionalInformation(CaseWorkItemType workItem, OperationResult result) {
        if (stageDef.getAdditionalInformation() != null) {
            try {
                VariablesMap variables = beans.caseMiscHelper.getDefaultVariables(
                        currentCase, approvalContext, task.getChannel(), result);
                workItem.getAdditionalInformation().addAll(
                        beans.expressionEvaluationHelper.evaluateExpression(
                                stageDef.getAdditionalInformation(),
                                variables,
                                "additional information expression",
                                InformationType.class,
                                InformationType.COMPLEX_TYPE,
                                true,
                                this::createInformationType,
                                operation.getTask(),
                                result));
            } catch (Throwable t) {
                throw new SystemException("Couldn't evaluate additional information expression in " + operation, t);
            }
        }
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
}
