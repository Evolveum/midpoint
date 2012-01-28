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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;

/**
 * @author lazyman
 */
@Entity
@Table(name = "task")
public class RTaskType extends RExtensibleObjectType {

    private String taskIdentifier;
    //    private ObjectReferenceType ownerRef;                 //todo mapping
    private TaskExecutionStatusType executionStatus;
    private TaskExclusivityStatusType exclusivityStatus;
    private String node;
    private XMLGregorianCalendar claimExpirationTimestamp;
    private String handlerUri;
    //    private UriStack otherHandlersUriStack;                    //todo mapping
    private ROperationResultType result;
    //    private ObjectReferenceType objectRef;                //todo mapping
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar nextRunStartTime;
    private BigInteger progress;
    private TaskRecurrenceType recurrence;
    private TaskBindingType binding;
//    private ScheduleType schedule;                //todo mapping
//    private ModelOperationStateType modelOperationState;      //todo mapping

    @Enumerated(EnumType.ORDINAL)
    public TaskBindingType getBinding() {
        return binding;
    }

    public XMLGregorianCalendar getClaimExpirationTimestamp() {
        return claimExpirationTimestamp;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskExclusivityStatusType getExclusivityStatus() {
        return exclusivityStatus;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    public XMLGregorianCalendar getLastRunFinishTimestamp() {
        return lastRunFinishTimestamp;
    }

    public XMLGregorianCalendar getLastRunStartTimestamp() {
        return lastRunStartTimestamp;
    }

    public XMLGregorianCalendar getNextRunStartTime() {
        return nextRunStartTime;
    }

    public String getNode() {
        return node;
    }

    public BigInteger getProgress() {
        return progress;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskRecurrenceType getRecurrence() {
        return recurrence;
    }

    @ManyToOne
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ROperationResultType getResult() {
        return result;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public void setBinding(TaskBindingType binding) {
        this.binding = binding;
    }

    public void setClaimExpirationTimestamp(XMLGregorianCalendar claimExpirationTimestamp) {
        this.claimExpirationTimestamp = claimExpirationTimestamp;
    }

    public void setExclusivityStatus(TaskExclusivityStatusType exclusivityStatus) {
        this.exclusivityStatus = exclusivityStatus;
    }

    public void setExecutionStatus(TaskExecutionStatusType executionStatus) {
        this.executionStatus = executionStatus;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setLastRunFinishTimestamp(XMLGregorianCalendar lastRunFinishTimestamp) {
        this.lastRunFinishTimestamp = lastRunFinishTimestamp;
    }

    public void setLastRunStartTimestamp(XMLGregorianCalendar lastRunStartTimestamp) {
        this.lastRunStartTimestamp = lastRunStartTimestamp;
    }

    public void setNextRunStartTime(XMLGregorianCalendar nextRunStartTime) {
        this.nextRunStartTime = nextRunStartTime;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public void setProgress(BigInteger progress) {
        this.progress = progress;
    }

    public void setRecurrence(TaskRecurrenceType recurrence) {
        this.recurrence = recurrence;
    }

    public void setResult(ROperationResultType result) {
        this.result = result;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public static void copyToJAXB(RTaskType repo, TaskType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(TaskType jaxb, RTaskType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }

    @Override
    public TaskType toJAXB() throws DtoTranslationException {
        TaskType object = new TaskType();
        RTaskType.copyToJAXB(this, object);
        return object;
    }
}
