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

package com.evolveum.midpoint.notifications.events;

import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.notifications.SimpleObjectRef;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author mederly
 */
public class WorkItemEvent extends WorkflowEvent {

    private String workItemName;
    private SimpleObjectRef assignee;

    public WorkItemEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, ChangeType changeType) {
        super(lightweightIdentifierGenerator, changeType);
    }

    public String getWorkItemName() {
        return workItemName;
    }

    public void setWorkItemName(String workItemName) {
        this.workItemName = workItemName;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_EVENT || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    public SimpleObjectRef getAssignee() {
        return assignee;
    }

    public void setAssigneeOid(String oid) {
        this.assignee = new SimpleObjectRef(oid);
    }

    @Override
    public void createExpressionVariables(Map<QName, Object> variables, NotificationsUtil notificationsUtil, OperationResult result) {
        super.createExpressionVariables(variables, notificationsUtil, result);
        variables.put(SchemaConstants.C_ASSIGNEE, notificationsUtil.getObjectType(assignee, result));
    }

    @Override
    public String toString() {
        return "WorkflowProcessEvent{" +
                "workflowEvent=" + super.toString() +
                ", workItemName=" + workItemName +
                ", assignee=" + assignee +
                '}';

    }


}
