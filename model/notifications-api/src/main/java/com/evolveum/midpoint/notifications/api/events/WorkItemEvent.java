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
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Map;

/**
 * @author mederly
 */
public class WorkItemEvent extends WorkflowEvent {

    @NotNull private final WorkItemType workItem;
    private final SimpleObjectRef assignee;

    public WorkItemEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, ChangeType changeType, @NotNull WorkItemType workItem,
			@Nullable SimpleObjectRef assignee, WfContextType workflowContext) {
        super(lightweightIdentifierGenerator, changeType, workflowContext);
        Validate.notNull(workItem);
        this.workItem = workItem;
		this.assignee = assignee;
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

    @Override
    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
        variables.put(SchemaConstants.C_ASSIGNEE, assignee != null ? assignee.resolveObjectType(result, false) : null);
        variables.put(SchemaConstants.C_WORK_ITEM, workItem);
    }

	@Override
	protected String getAnswer() {
		DecisionType decision = workItem.getDecision();
		if (decision == null) {
			return null;
		}
		return decision.getResultAsString();
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
