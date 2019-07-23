/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
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

/**
 * @author mederly
 */
abstract public class WorkflowEvent extends BaseEvent {

    @Nullable protected final ApprovalContextType approvalContext;
    @NotNull private final ChangeType changeType;
    @NotNull protected final CaseType aCase;

    WorkflowEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
		    @Nullable ApprovalContextType approvalContext, @NotNull CaseType aCase, EventHandlerType handler) {
        super(lightweightIdentifierGenerator, handler);
        this.changeType = changeType;
		this.approvalContext = approvalContext;
		this.aCase = aCase;
    }

    @NotNull
    public CaseType getCase() {
        return aCase;
    }

    public String getProcessInstanceName() {
        return aCase.getName().getOrig();
    }

    public OperationStatus getOperationStatus() {
        return outcomeToStatus(changeType, getOutcome());
    }

	protected abstract String getOutcome();

	@Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return getOperationStatus().matchesEventStatusType(eventStatusType);
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return changeTypeMatchesOperationType(changeType, eventOperationType);
    }

    public boolean isApprovalCase() {
		return ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    public boolean isManualResourceCase() {
		return ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_MANUAL_CASE.value());
    }

    public boolean isResultKnown() {
        return !isInProgress();         // for now
    }

    public boolean isApproved() {
        return isSuccess();             // for now
    }

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

	@NotNull
	public ApprovalContextType getApprovalContext() {
		return approvalContext;
	}

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
