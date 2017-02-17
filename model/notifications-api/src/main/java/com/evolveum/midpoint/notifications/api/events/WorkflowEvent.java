/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
abstract public class WorkflowEvent extends BaseEvent {

    @NotNull protected final WfContextType workflowContext;
    @NotNull private final ChangeType changeType;

    public WorkflowEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
            @NotNull WfContextType workflowContext, EventHandlerType handler) {
        super(lightweightIdentifierGenerator, handler);
        this.changeType = changeType;
		this.workflowContext = workflowContext;
    }

    public String getProcessInstanceName() {
        return workflowContext.getProcessInstanceName();
    }

    public OperationStatus getOperationStatus() {
        return resultToStatus(changeType, getAnswer());
    }

	protected abstract String getAnswer();

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

    public boolean isResultKnown() {
        return !isInProgress();         // for now
    }

    public boolean isApproved() {
        return isSuccess();             // for now
    }

    public boolean isRejected() {
        return isFailure();             // for now
    }

    private OperationStatus resultToStatus(ChangeType changeType, String decision) {
        if (changeType != ChangeType.DELETE) {
            return OperationStatus.SUCCESS;
        } else {
            if (decision == null) {
                return OperationStatus.IN_PROGRESS;
            } else if (decision.equals(ApprovalUtils.DECISION_APPROVED)) {
                return OperationStatus.SUCCESS;
            } else if (decision.equals(ApprovalUtils.DECISION_REJECTED)) {
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

    public WfProcessorSpecificStateType getProcessorSpecificState() {
        return workflowContext.getProcessorSpecificState();
    }

    public WfProcessSpecificStateType getProcessSpecificState() {
		return workflowContext.getProcessSpecificState();
    }

	@NotNull
	public WfContextType getWorkflowContext() {
		return workflowContext;
	}

	public WfPrimaryChangeProcessorStateType getPrimaryChangeProcessorState() {
        WfProcessorSpecificStateType state = getProcessorSpecificState();
        if (state instanceof WfPrimaryChangeProcessorStateType) {
            return (WfPrimaryChangeProcessorStateType) state;
        } else {
            return null;
        }
    }

    // the following three methods are specific to ItemApproval process
    public ItemApprovalProcessStateType getItemApprovalProcessState() {
        WfProcessSpecificStateType state = getProcessSpecificState();
        if (state instanceof ItemApprovalProcessStateType) {
            return (ItemApprovalProcessStateType) state;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "WorkflowEvent{" +
                "event=" + super.toString() +
                ", processInstanceName='" + getProcessInstanceName() + '\'' +
                ", changeType=" + changeType +
                ", answer=" + getAnswer() +
                '}';
    }

    // This method is not used. It is here just for maven dependency plugin to detect the
    // dependency on workflow-api
    @SuppressWarnings("unused")
	private void notUsed() {
    	ApprovalUtils.approvalBooleanValue("");
    }

}
