/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 14.5.2012
 * Time: 13:34
 * To change this template use File | Settings | File Templates.
 */
public class WorkItem implements Serializable {

    private String processId;
    private String processName;
    private String taskId;
    private String name;
    private String assignee;
    private String assigneeName;
    private String candidates;
    private Date createTime;

    private PrismObject<UserType> requester;
    private PrismObject<UserType> objectOld;
    private PrismObject<UserType> objectNew;
    private PrismObject<ObjectType> requestSpecificData;
    private PrismObject<ObjectType> requestCommonData;
    private PrismObject<ObjectType> trackingData;
    private PrismObject<ObjectType> additionalData;

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getCandidates() {
        return candidates;
    }

    public void setCandidates(String candidates) {
        this.candidates = candidates;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public PrismObject<UserType> getObjectNew() {
        return objectNew;
    }

    public void setObjectNew(PrismObject<UserType> objectNew) {
        this.objectNew = objectNew;
    }

    public PrismObject<UserType> getObjectOld() {
        return objectOld;
    }

    public void setObjectOld(PrismObject<UserType> objectOld) {
        this.objectOld = objectOld;
    }

    public PrismObject<ObjectType> getRequestCommonData() {
        return requestCommonData;
    }

    public void setRequestCommonData(PrismObject<ObjectType> requestCommonData) {
        this.requestCommonData = requestCommonData;
    }

    public PrismObject<UserType> getRequester() {
        return requester;
    }

    public void setRequester(PrismObject<UserType> requester) {
        this.requester = requester;
    }

    public PrismObject<ObjectType> getRequestSpecificData() {
        return requestSpecificData;
    }

    public void setRequestSpecificData(PrismObject<ObjectType> requestSpecificData) {
        this.requestSpecificData = requestSpecificData;
    }

    public void setTrackingData(PrismObject<ObjectType> trackingData) {
        this.trackingData = trackingData;
    }

    public PrismObject<ObjectType> getTrackingData() {
        return trackingData;
    }

    public void setAdditionalData(PrismObject<ObjectType> additionalData) {
        this.additionalData = additionalData;
    }

    public PrismObject<ObjectType> getAdditionalData() {
        return additionalData;
    }
}
