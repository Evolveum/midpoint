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

import com.evolveum.midpoint.notifications.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public abstract class Event {

    private String id;                              // randomly generated event ID
    private OperationStatus operationStatus;        // status of the operation
    private UserType requester;
    private UserType requestee;

    public Event() {
        id = System.currentTimeMillis() + ":" + (long) (Math.random() * 100000000);
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", operationStatus=" + operationStatus +
                '}';
    }

    public boolean isStatusType(EventStatusType eventStatusType) {
        switch (eventStatusType) {
            case SUCCESS: return operationStatus == OperationStatus.SUCCESS;
            case FAILURE: return operationStatus == OperationStatus.FAILURE;
            case IN_PROGRESS: return operationStatus == OperationStatus.IN_PROGRESS;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

    public abstract boolean isOperationType(EventOperationType eventOperationType);
    public abstract boolean isCategoryType(EventCategoryType eventCategoryType);

    public boolean isAccountRelated() {
        return isCategoryType(EventCategoryType.ACCOUNT_OPERATION);
    }

    public boolean isUserRelated() {
        return isCategoryType(EventCategoryType.USER_OPERATION);
    }

    public boolean isWorkItemRelated() {
        return isCategoryType(EventCategoryType.WORK_ITEM_OPERATION);
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

    public boolean isFailure() {
        return isStatusType(EventStatusType.FAILURE);
    }

    public boolean isInProgress() {
        return isStatusType(EventStatusType.IN_PROGRESS);
    }

    public UserType getRequester() {
        return requester;
    }

    public void setRequester(UserType requester) {
        this.requester = requester;
    }

    public UserType getRequestee() {
        return requestee;
    }

    public void setRequestee(UserType requestee) {
        this.requestee = requestee;
    }
}
