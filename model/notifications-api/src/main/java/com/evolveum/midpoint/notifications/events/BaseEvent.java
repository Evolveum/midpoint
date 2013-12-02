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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventStatusType;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class BaseEvent implements Event {

    private LightweightIdentifier id;               // randomly generated event ID
    private SimpleObjectRef requester;              // who requested this operation (null if unknown)

    // about who is this operation (null if unknown);
    // - for model notifications, this is the focus, (usually a user but may be e.g. role or other kind of object)
    // - for account notifications, this is the account owner,
    // - for workflow notifications, this is the workflow process instance object

    private SimpleObjectRef requestee;

    public BaseEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        id = lightweightIdentifierGenerator.generate();
    }

    public LightweightIdentifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ",requester=" + requester +
                ",requestee=" + requestee +
                '}';
    }

    abstract public boolean isStatusType(EventStatusType eventStatusType);
    abstract public boolean isOperationType(EventOperationType eventOperationType);
    abstract public boolean isCategoryType(EventCategoryType eventCategoryType);

    public boolean isAccountRelated() {
        return isCategoryType(EventCategoryType.ACCOUNT_EVENT);
    }

    public boolean isUserRelated() {
        return isCategoryType(EventCategoryType.USER_EVENT);
    }

    public boolean isWorkItemRelated() {
        return isCategoryType(EventCategoryType.WORK_ITEM_EVENT);
    }

    public boolean isWorkflowProcessRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_PROCESS_EVENT);
    }

    public boolean isWorkflowRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_EVENT);
    }

    public boolean isAdd() {
        return isOperationType(EventOperationType.ADD);
    }

    public boolean isModify() {
        return isOperationType(EventOperationType.MODIFY);
    }

    public boolean isDelete() {
        return isOperationType(EventOperationType.DELETE);
    }

    public boolean isSuccess() {
        return isStatusType(EventStatusType.SUCCESS);
    }

    public boolean isAlsoSuccess() {
        return isStatusType(EventStatusType.ALSO_SUCCESS);
    }

    public boolean isFailure() {
        return isStatusType(EventStatusType.FAILURE);
    }

    public boolean isOnlyFailure() {
        return isStatusType(EventStatusType.ONLY_FAILURE);
    }

    public boolean isInProgress() {
        return isStatusType(EventStatusType.IN_PROGRESS);
    }

    // requester

    public SimpleObjectRef getRequester() {
        return requester;
    }

    public String getRequesterOid() {
        return requester.getOid();
    }

    public void setRequester(SimpleObjectRef requester) {
        this.requester = requester;
    }

    // requestee

    public SimpleObjectRef getRequestee() {
        return requestee;
    }

    public String getRequesteeOid() {
        return requestee.getOid();
    }

    public void setRequestee(SimpleObjectRef requestee) {
        this.requestee = requestee;
    }

    boolean changeTypeMatchesOperationType(ChangeType changeType, EventOperationType eventOperationType) {
        switch (eventOperationType) {
            case ADD: return changeType == ChangeType.ADD;
            case MODIFY: return changeType == ChangeType.MODIFY;
            case DELETE: return changeType == ChangeType.DELETE;
            default: throw new IllegalStateException("Unexpected EventOperationType: " + eventOperationType);
        }
    }

    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        variables.put(SchemaConstants.C_EVENT, this);
        if (requester != null) {
            variables.put(SchemaConstants.C_REQUESTER, requester.resolveObjectType(result));
        }
        if (requestee != null) {
            variables.put(SchemaConstants.C_REQUESTEE, requestee.resolveObjectType(result));
        }
    }
}
