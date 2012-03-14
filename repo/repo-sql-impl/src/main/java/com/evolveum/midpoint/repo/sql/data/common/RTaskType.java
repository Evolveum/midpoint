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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;

/**
 * @author lazyman
 */
@Entity
@Table(name = "task")
@ForeignKey(name = "fk_task")
public class RTaskType extends RObjectType {

    private String taskIdentifier;
    private RObjectReferenceType ownerRef;
    private TaskExecutionStatusType executionStatus;
    private TaskExclusivityStatusType exclusivityStatus;
    private String node;
    private XMLGregorianCalendar claimExpirationTimestamp;
    private String handlerUri;
    private String otherHandlersUriStack;
    private ROperationResultType result;
    private RObjectReferenceType objectRef;
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar nextRunStartTime;
    private BigInteger progress;
    private TaskRecurrenceType recurrence;
    private TaskBindingType binding;
    private String schedule;
    private String modelOperationState;

    @Type(type = "org.hibernate.type.TextType")
    public String getModelOperationState() {
        return modelOperationState;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getOtherHandlersUriStack() {
        return otherHandlersUriStack;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getSchedule() {
        return schedule;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getOwnerRef() {
        return ownerRef;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getObjectRef() {
        return objectRef;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskBindingType getBinding() {
        return binding;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskExclusivityStatusType getExclusivityStatus() {
        return exclusivityStatus;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    @Enumerated(EnumType.ORDINAL)
    public TaskRecurrenceType getRecurrence() {
        return recurrence;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ROperationResultType getResult() {
        return result;
    }

    public XMLGregorianCalendar getClaimExpirationTimestamp() {
        return claimExpirationTimestamp;
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

    public void setObjectRef(RObjectReferenceType objectRef) {
        this.objectRef = objectRef;
    }

    public void setOwnerRef(RObjectReferenceType ownerRef) {
        this.ownerRef = ownerRef;
    }

    public void setModelOperationState(String modelOperationState) {
        this.modelOperationState = modelOperationState;
    }

    public void setOtherHandlersUriStack(String otherHandlersUriStack) {
        this.otherHandlersUriStack = otherHandlersUriStack;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public static void copyToJAXB(RTaskType repo, TaskType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setTaskIdentifier(repo.getTaskIdentifier());
        jaxb.setClaimExpirationTimestamp(repo.getClaimExpirationTimestamp());
        jaxb.setExclusivityStatus(repo.getExclusivityStatus());
        jaxb.setExecutionStatus(repo.getExecutionStatus());
        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setLastRunFinishTimestamp(repo.getLastRunFinishTimestamp());
        jaxb.setLastRunStartTimestamp(repo.getLastRunStartTimestamp());
        jaxb.setNode(repo.getNode());
        jaxb.setProgress(repo.getProgress());
        jaxb.setBinding(repo.getBinding());
        jaxb.setNextRunStartTime(repo.getNextRunStartTime());
        jaxb.setRecurrence(repo.getRecurrence());

        if (repo.getObjectRef() != null) {
            jaxb.setObjectRef(repo.getObjectRef().toJAXB(prismContext));
        }
        if (repo.getOwnerRef() != null) {
            jaxb.setOwnerRef(repo.getOwnerRef().toJAXB(prismContext));
        }
        if (repo.getResult() != null) {
            jaxb.setResult(repo.getResult().toJAXB(prismContext));
        }

        try {
            jaxb.setModelOperationState(RUtil.toJAXB(repo.getModelOperationState(), ModelOperationStateType.class, prismContext));
            jaxb.setOtherHandlersUriStack(RUtil.toJAXB(repo.getOtherHandlersUriStack(), UriStack.class, prismContext));
            jaxb.setSchedule(RUtil.toJAXB(repo.getSchedule(), ScheduleType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(TaskType jaxb, RTaskType repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyFromJAXB(jaxb, repo, prismContext);

        repo.setTaskIdentifier(jaxb.getTaskIdentifier());
        repo.setClaimExpirationTimestamp(jaxb.getClaimExpirationTimestamp());
        repo.setExclusivityStatus(jaxb.getExclusivityStatus());
        repo.setExecutionStatus(jaxb.getExecutionStatus());
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setProgress(jaxb.getProgress());
        repo.setBinding(jaxb.getBinding());
        repo.setNextRunStartTime(jaxb.getNextRunStartTime());
        repo.setRecurrence(jaxb.getRecurrence());

        repo.setObjectRef(RUtil.jaxbRefToRepo(jaxb.getObjectRef(), repo, prismContext));
        repo.setOwnerRef(RUtil.jaxbRefToRepo(jaxb.getOwnerRef(), repo, prismContext));
//        repo.setResult(RUtil.jaxbResultToRepo(repo, jaxb.getResult(), prismContext));

        try {
            repo.setModelOperationState(RUtil.toRepo(jaxb.getModelOperationState(), prismContext));
            repo.setOtherHandlersUriStack(RUtil.toRepo(jaxb.getOtherHandlersUriStack(), prismContext));
            repo.setSchedule(RUtil.toRepo(jaxb.getSchedule(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public TaskType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        TaskType object = new TaskType();
        RTaskType.copyToJAXB(this, object, prismContext);
        RUtil.revive(object.asPrismObject(), TaskType.class, prismContext);

        return object;
    }
}
