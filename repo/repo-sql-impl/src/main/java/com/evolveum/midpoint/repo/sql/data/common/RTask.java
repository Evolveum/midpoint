/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RTaskAutoScaling;
import com.evolveum.midpoint.repo.sql.data.common.enums.*;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAutoScalingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Entity
@Table(name = "m_task", indexes = {
        @jakarta.persistence.Index(name = "iTaskObjectOid", columnList = "objectRef_targetOid"),
        @jakarta.persistence.Index(name = "iTaskNameOrig", columnList = "name_orig") },
        uniqueConstraints = @UniqueConstraint(name = "uc_task_identifier", columnNames = { "taskIdentifier" }))
@ForeignKey(name = "fk_task")
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RTask extends RObject implements ROperationResultFull {

    private RPolyString nameCopy;
    private String taskIdentifier;
    private RTaskExecutionState executionStatus;
    private String node;
    private String category; // no longer used
    private String handlerUri;
    //operation result
    private byte[] fullResult;
    private ROperationResultStatus status;
    //end of operation result
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar completionTimestamp;
    private RTaskRecurrence recurrence;
    private RTaskBinding binding;

    private RSimpleEmbeddedReference objectRef;
    private RSimpleEmbeddedReference ownerRefTask;
    private String parent;

    private RThreadStopAction threadStopAction;
    private Set<String> dependent;
    private RTaskWaitingReason waitingReason;
    private RTaskSchedulingState schedulingState;
    private RTaskAutoScaling autoScaling;

    @ElementCollection
    @ForeignKey(name = "fk_task_dependent")
    @CollectionTable(name = "m_task_dependent", joinColumns = {
            @JoinColumn(name = "task_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getDependent() {
        return dependent;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RTaskWaitingReason getWaitingReason() {
        return waitingReason;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RTaskSchedulingState getSchedulingState() {
        return schedulingState;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "mode", column = @Column(name = "autoScalingMode")),
    })
    public RTaskAutoScaling getAutoScaling() {
        return autoScaling;
    }

    public String getCategory() {
        return category;
    }

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RThreadStopAction getThreadStopAction() {
        return threadStopAction;
    }

    @Embedded
    public RSimpleEmbeddedReference getObjectRef() {
        return objectRef;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "targetType", column = @Column(name = "ownerRef_targetType"))
    })
    public RSimpleEmbeddedReference getOwnerRefTask() {
        return ownerRefTask;
    }

    @Index(name = "iParent")
    @Column(name = "parent")
    public String getParent() {
        return parent;
    }

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RTaskBinding getBinding() {
        return binding;
    }

    @JaxbName(localPart = "executionState")
    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RTaskExecutionState getExecutionStatus() {
        return executionStatus;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "schedule"), @JaxbName(localPart = "recurrence") })
    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RTaskRecurrence getRecurrence() {
        return recurrence;
    }

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    @NeverNull
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @Lob
    @NotQueryable
    @Override
    public byte[] getFullResult() {
        return fullResult;
    }

    @Override
    public void setFullResult(byte[] fullResult) {
        this.fullResult = fullResult;
    }

    public void setThreadStopAction(RThreadStopAction threadStopAction) {
        this.threadStopAction = threadStopAction;
    }

    public void setObjectRef(RSimpleEmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setOwnerRefTask(RSimpleEmbeddedReference ownerRefTask) {
        this.ownerRefTask = ownerRefTask;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getLastRunFinishTimestamp() {
        return lastRunFinishTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getCompletionTimestamp() {
        return completionTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getLastRunStartTimestamp() {
        return lastRunStartTimestamp;
    }

    @Column
    public String getNode() {
        return node;
    }

    @Column
    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    @Override
    @JaxbName(localPart = "resultStatus")
    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public ROperationResultStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setBinding(RTaskBinding binding) {
        this.binding = binding;
    }

    public void setExecutionStatus(RTaskExecutionState executionStatus) {
        this.executionStatus = executionStatus;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setLastRunFinishTimestamp(XMLGregorianCalendar lastRunFinishTimestamp) {
        this.lastRunFinishTimestamp = lastRunFinishTimestamp;
    }

    public void setCompletionTimestamp(XMLGregorianCalendar completionTimestamp) {
        this.completionTimestamp = completionTimestamp;
    }

    public void setLastRunStartTimestamp(XMLGregorianCalendar lastRunStartTimestamp) {
        this.lastRunStartTimestamp = lastRunStartTimestamp;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public void setRecurrence(RTaskRecurrence recurrence) {
        this.recurrence = recurrence;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public void setDependent(Set<String> dependent) {
        this.dependent = dependent;
    }

    public void setWaitingReason(RTaskWaitingReason waitingReason) {
        this.waitingReason = waitingReason;
    }

    public void setSchedulingState(RTaskSchedulingState schedulingState) {
        this.schedulingState = schedulingState;
    }

    public void setAutoScaling(RTaskAutoScaling autoScaling) {
        this.autoScaling = autoScaling;
    }

    // dynamically called
    public static void copyFromJAXB(TaskType jaxb, RTask repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setTaskIdentifier(jaxb.getTaskIdentifier());
        repo.setExecutionStatus(RUtil.getRepoEnumValue(jaxb.getExecutionState(), RTaskExecutionState.class));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setCompletionTimestamp(jaxb.getCompletionTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setBinding(RUtil.getRepoEnumValue(jaxb.getBinding(), RTaskBinding.class));
        repo.setRecurrence(RUtil.getRepoEnumValue(TaskTypeUtil.getEffectiveRecurrence(jaxb), RTaskRecurrence.class));
        repo.setThreadStopAction(RUtil.getRepoEnumValue(jaxb.getThreadStopAction(), RThreadStopAction.class));
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), repositoryContext.relationRegistry));
        repo.setOwnerRefTask(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.relationRegistry));
        repo.setWaitingReason(RUtil.getRepoEnumValue(jaxb.getWaitingReason(), RTaskWaitingReason.class));
        repo.setSchedulingState(RUtil.getRepoEnumValue(jaxb.getSchedulingState(), RTaskSchedulingState.class));
        repo.setDependent(RUtil.listToSet(jaxb.getDependent()));

        TaskAutoScalingType autoScaling = jaxb.getAutoScaling();
        if (autoScaling != null) {
            RTaskAutoScaling rAutoScaling = new RTaskAutoScaling();
            RTaskAutoScaling.fromJaxb(autoScaling, rAutoScaling);
            repo.setAutoScaling(rAutoScaling);
        }

        RUtil.copyResultFromJAXB(TaskType.F_RESULT, jaxb.getResult(), repo);
    }
}
