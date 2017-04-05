/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.wf.api.WorkItemOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import java.util.Map;

/**
 * @author mederly
 */
public class WorkItemEvent extends WorkflowEvent {

    @NotNull protected final WorkItemType workItem;
    // (Currently) Each work item event is related to at most one assignee. So, if a work item has more assignees,
	// more events will be generated. This might change in a future.
	protected final SimpleObjectRef assignee;
	/**
	 * User who "pressed the button". I.e. the one that really approved, rejected or delegated/escalated a work item.
	 * In case of automated actions (completion, delegation/escalation) this is not filled-in.
	 */
	protected final SimpleObjectRef initiator;
	protected final WorkItemOperationInfo operationInfo;
	protected final WorkItemOperationSourceInfo sourceInfo;
	protected final Duration timeBefore;

    WorkItemEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
			@NotNull WorkItemType workItem,
			@Nullable SimpleObjectRef assignee, @Nullable SimpleObjectRef initiator,
			@Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
			@NotNull WfContextType workflowContext,
			@Nullable EventHandlerType handler, @Nullable Duration timeBefore) {
        super(lightweightIdentifierGenerator, changeType, workflowContext, handler);
	    Validate.notNull(workItem);
        this.workItem = workItem;
		this.assignee = assignee;
		this.initiator = initiator;
		this.operationInfo = operationInfo;
		this.sourceInfo = sourceInfo;
	    this.timeBefore = timeBefore;
    }

    public String getWorkItemName() {
        return workItem.getName();
    }

	@NotNull
	public WorkItemType getWorkItem() {
		return workItem;
	}

	@Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_EVENT || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    public SimpleObjectRef getAssignee() {
        return assignee;
    }

	public SimpleObjectRef getInitiator() {
		return initiator;
	}

	public WorkItemOperationKindType getOperationKind() {
		return operationInfo != null ? operationInfo.getOperationKind() : null;
	}

	public AbstractWorkItemActionType getSource() {
		return sourceInfo != null ? sourceInfo.getSource() : null;
	}

	public WorkItemEventCauseInformationType getCause() {
		return sourceInfo != null ? sourceInfo.getCause() : null;
	}

	public Duration getTimeBefore() {
		return timeBefore;
	}

	public WorkItemOperationInfo getOperationInfo() {
		return operationInfo;
	}

	public WorkItemOperationSourceInfo getSourceInfo() {
		return sourceInfo;
	}

	@Override
    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
        variables.put(SchemaConstants.C_ASSIGNEE, assignee != null ? assignee.resolveObjectType(result, false) : null);
        variables.put(SchemaConstants.C_WORK_ITEM, workItem);
    }

    public WorkItemResultType getWorkItemResult() {
    	return workItem.getResult();
	}

	@Override
	public String getOutcome() {
    	WorkItemResultType result = getWorkItemResult();
		return result != null ? result.getOutcome() : null;
	}

	@Override
    public String toString() {
        return "WorkflowProcessEvent{" +
                "workflowEvent=" + super.toString() +
                ", workItemName=" + getWorkItemName() +
                ", assignee=" + assignee +
                '}';

    }


}
