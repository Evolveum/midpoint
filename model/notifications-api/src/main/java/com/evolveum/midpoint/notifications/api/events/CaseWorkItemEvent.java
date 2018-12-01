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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author mederly
 */
public class CaseWorkItemEvent extends CaseManagementEvent {

    @NotNull protected final CaseWorkItemType workItem;
	@NotNull protected final SimpleObjectRef assignee;

    public CaseWorkItemEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
		    @NotNull ChangeType changeType, @NotNull CaseWorkItemType workItem, @NotNull SimpleObjectRef assignee,
		    @NotNull CaseType aCase) {
        super(lightweightIdentifierGenerator, changeType, aCase);
        this.workItem = workItem;
		this.assignee = assignee;
    }

    public String getWorkItemName() {
        return workItem.getName();
    }

	@NotNull
	public CaseWorkItemType getWorkItem() {
		return workItem;
	}

	@Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_EVENT || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    @NotNull
    public SimpleObjectRef getAssignee() {
        return assignee;
    }

	@Override
    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
        variables.put(SchemaConstants.C_ASSIGNEE, assignee.resolveObjectType(result, false));
        variables.put(SchemaConstants.C_WORK_ITEM, workItem);
    }

	@Override
	public boolean isRelatedToItem(ItemPath itemPath) {
		return false;       // TODO reconsider
	}

	@Override
	public String toString() {
		return "CaseWorkItemEvent{" +
				"workItem=" + workItem +
				", assignee=" + assignee +
				", aCase=" + aCase +
				'}';
	}

	@SuppressWarnings("Duplicates")
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
		debugDumpCommon(sb, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "workItemName", getWorkItemName(), indent + 1);
		DebugUtil.debugDumpWithLabelToString(sb, "assignee", assignee, indent + 1);
		return sb.toString();
	}
}
