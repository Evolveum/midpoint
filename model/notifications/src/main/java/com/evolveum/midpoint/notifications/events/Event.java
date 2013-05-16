/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
