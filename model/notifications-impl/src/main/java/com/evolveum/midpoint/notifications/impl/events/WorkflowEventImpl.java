/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.WorkflowEvent;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class WorkflowEventImpl extends BaseEventImpl implements WorkflowEvent {

    @Nullable protected final ApprovalContextType approvalContext;
    @NotNull private final ChangeType changeType;
    @NotNull protected final CaseType aCase;

    WorkflowEventImpl(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
            @Nullable ApprovalContextType approvalContext, @NotNull CaseType aCase, EventHandlerType handler) {
        super(lightweightIdentifierGenerator, handler);
        this.changeType = changeType;
        this.approvalContext = approvalContext;
        this.aCase = aCase;
    }

    @Override
    @NotNull
    public CaseType getCase() {
        return aCase;
    }

    @Override
    public String getProcessInstanceName() {
        return aCase.getName().getOrig();
    }

    @Override
    public OperationStatus getOperationStatus() {
        return outcomeToStatus(changeType, getOutcome());
    }

    protected abstract String getOutcome();

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        return getOperationStatus().matchesEventStatusType(eventStatus);
    }

    @Override
    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return changeTypeMatchesOperationType(changeType, eventOperation);
    }

    @Override
    public boolean isApprovalCase() {
        return ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    @Override
    public boolean isManualResourceCase() {
        return ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_MANUAL_CASE.value());
    }

    @Override
    public boolean isResultKnown() {
        return !isInProgress();         // for now
    }

    @Override
    public boolean isApproved() {
        return isSuccess();             // for now
    }

    @Override
    public boolean isRejected() {
        return isFailure();             // for now
    }

    private OperationStatus outcomeToStatus(ChangeType changeType, String outcome) {
        if (changeType != ChangeType.DELETE) {
            return OperationStatus.SUCCESS;
        } else {
            if (outcome == null) {
                return OperationStatus.IN_PROGRESS;
            } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
                return OperationStatus.SUCCESS;
            } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
                return OperationStatus.FAILURE;
            } else {
                return OperationStatus.OTHER;
            }
        }
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;
    }

    @Override
    @NotNull
    public ApprovalContextType getApprovalContext() {
        return approvalContext;
    }

    @Override
    @NotNull
    public CaseType getWorkflowTask() {
        return aCase;
    }

    @Override
    public String toString() {
        return "WorkflowEvent{" +
                "event=" + super.toString() +
                ", processInstanceName='" + getProcessInstanceName() + '\'' +
                ", changeType=" + changeType +
                ", outcome=" + getOutcome() +
                '}';
    }

    // This method is not used. It is here just for maven dependency plugin to detect the
    // dependency on workflow-api
    @SuppressWarnings("unused")
    private void notUsed() {
        ApprovalUtils.approvalBooleanValueFromUri("");
    }

    @Override
    protected void debugDumpCommon(StringBuilder sb, int indent) {
        super.debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "processInstanceName", getProcessInstanceName(), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "changeType", changeType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "outcome", getOutcome(), indent + 1);
    }

}
