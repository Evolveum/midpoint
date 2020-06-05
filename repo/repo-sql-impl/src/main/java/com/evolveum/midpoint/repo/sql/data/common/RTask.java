/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.*;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.NeverNull;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_task", indexes = {
        @javax.persistence.Index(name = "iTaskObjectOid", columnList = "objectRef_targetOid"),
        @javax.persistence.Index(name = "iTaskNameOrig", columnList = "name_orig")},
        uniqueConstraints = @UniqueConstraint(name = "uc_task_identifier", columnNames = {"taskIdentifier"}))
@ForeignKey(name = "fk_task")
@Persister(impl = MidPointJoinedPersister.class)
public class RTask extends RObject implements OperationResultFull {

    private RPolyString nameCopy;
    private String taskIdentifier;
    private RTaskExecutionStatus executionStatus;
    private String node;
    private String category;
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

    private REmbeddedReference objectRef;
    private REmbeddedReference ownerRefTask;
    private String parent;

    private RThreadStopAction threadStopAction;
    private Set<String> dependent;
    private RTaskWaitingReason waitingReason;

    @ElementCollection
    @ForeignKey(name = "fk_task_dependent")
    @CollectionTable(name = "m_task_dependent", joinColumns = {
            @JoinColumn(name = "task_oid", referencedColumnName = "oid")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getDependent() {
        return dependent;
    }

    @Enumerated(EnumType.ORDINAL)
    public RTaskWaitingReason getWaitingReason() {
        return waitingReason;
    }

    public String getCategory() {
        return category;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RThreadStopAction getThreadStopAction() {
        return threadStopAction;
    }

    @Embedded
    public REmbeddedReference getObjectRef() {
        return objectRef;
    }

    @JaxbName(localPart = "ownerRef")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "relation", column = @Column(name = "ownerRef_relation", length = RUtil.COLUMN_LENGTH_QNAME)),
            @AttributeOverride(name = "targetOid", column = @Column(name = "ownerRef_targetOid", length = RUtil.COLUMN_LENGTH_OID)),
            @AttributeOverride(name = "type", column = @Column(name = "ownerRef_type"))
    })
    public REmbeddedReference getOwnerRefTask() {
        return ownerRefTask;
    }

    @Index(name = "iParent")
    @Column(name = "parent")
    public String getParent() {
        return parent;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RTaskBinding getBinding() {
        return binding;
    }

    @Enumerated(EnumType.ORDINAL)
    public RTaskExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    @Enumerated(EnumType.ORDINAL)
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

    public void setObjectRef(REmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setOwnerRefTask(REmbeddedReference ownerRefTask) {
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

    @Column(nullable = true)
    public XMLGregorianCalendar getLastRunFinishTimestamp() {
        return lastRunFinishTimestamp;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getCompletionTimestamp() {
        return completionTimestamp;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getLastRunStartTimestamp() {
        return lastRunStartTimestamp;
    }

    @Column(nullable = true)
    public String getNode() {
        return node;
    }

    @Column(nullable = true)
    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    @JaxbName(localPart = "resultStatus")
    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setBinding(RTaskBinding binding) {
        this.binding = binding;
    }

    public void setExecutionStatus(RTaskExecutionStatus executionStatus) {
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RTask rTask = (RTask) o;

        if (nameCopy != null ? !nameCopy.equals(rTask.nameCopy) : rTask.nameCopy != null) return false;
        if (binding != rTask.binding) return false;
        if (executionStatus != rTask.executionStatus) return false;
        if (handlerUri != null ? !handlerUri.equals(rTask.handlerUri) : rTask.handlerUri != null) return false;
        if (lastRunFinishTimestamp != null ? !lastRunFinishTimestamp.equals(rTask.lastRunFinishTimestamp) : rTask.lastRunFinishTimestamp != null)
            return false;
        if (completionTimestamp != null ? !completionTimestamp.equals(rTask.completionTimestamp) : rTask.completionTimestamp != null)
            return false;
        if (lastRunStartTimestamp != null ? !lastRunStartTimestamp.equals(rTask.lastRunStartTimestamp) : rTask.lastRunStartTimestamp != null)
            return false;
        if (node != null ? !node.equals(rTask.node) : rTask.node != null) return false;
        if (objectRef != null ? !objectRef.equals(rTask.objectRef) : rTask.objectRef != null) return false;
        if (ownerRefTask != null ? !ownerRefTask.equals(rTask.ownerRefTask) : rTask.ownerRefTask != null) return false;
        if (recurrence != rTask.recurrence) return false;
        if (taskIdentifier != null ? !taskIdentifier.equals(rTask.taskIdentifier) : rTask.taskIdentifier != null)
            return false;
        if (threadStopAction != null ? !threadStopAction.equals(rTask.threadStopAction) :
                rTask.threadStopAction != null) return false;
        if (category != null ? !category.equals(rTask.category) : rTask.category != null) return false;
        if (parent != null ? !parent.equals(rTask.parent) : rTask.parent != null) return false;
        if (dependent != null ? !dependent.equals(rTask.dependent) : rTask.dependent != null) return false;
        if (waitingReason != null ? !waitingReason.equals(rTask.waitingReason) : rTask.waitingReason != null)
            return false;
        if (status != rTask.status) return false;
        if (fullResult != null ? !Arrays.equals(fullResult, rTask.fullResult) : rTask.fullResult != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (nameCopy != null ? nameCopy.hashCode() : 0);
        result1 = 31 * result1 + (taskIdentifier != null ? taskIdentifier.hashCode() : 0);
        result1 = 31 * result1 + (executionStatus != null ? executionStatus.hashCode() : 0);
        result1 = 31 * result1 + (node != null ? node.hashCode() : 0);
        result1 = 31 * result1 + (handlerUri != null ? handlerUri.hashCode() : 0);
        result1 = 31 * result1 + (lastRunStartTimestamp != null ? lastRunStartTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (completionTimestamp != null ? completionTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (lastRunFinishTimestamp != null ? lastRunFinishTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (recurrence != null ? recurrence.hashCode() : 0);
        result1 = 31 * result1 + (binding != null ? binding.hashCode() : 0);
        result1 = 31 * result1 + (threadStopAction != null ? threadStopAction.hashCode() : 0);
        result1 = 31 * result1 + (category != null ? category.hashCode() : 0);
        result1 = 31 * result1 + (parent != null ? parent.hashCode() : 0);
        result1 = 31 * result1 + (waitingReason != null ? waitingReason.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (fullResult != null ? Arrays.hashCode(fullResult) : 0);

        return result1;
    }

    // dynamically called
    public static void copyFromJAXB(TaskType jaxb, RTask repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyAssignmentHolderInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setTaskIdentifier(jaxb.getTaskIdentifier());
        repo.setExecutionStatus(RUtil.getRepoEnumValue(jaxb.getExecutionStatus(), RTaskExecutionStatus.class));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setCompletionTimestamp(jaxb.getCompletionTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setBinding(RUtil.getRepoEnumValue(jaxb.getBinding(), RTaskBinding.class));
        repo.setRecurrence(RUtil.getRepoEnumValue(jaxb.getRecurrence(), RTaskRecurrence.class));
        repo.setThreadStopAction(RUtil.getRepoEnumValue(jaxb.getThreadStopAction(), RThreadStopAction.class));
        repo.setCategory(jaxb.getCategory());
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), repositoryContext.relationRegistry));
        repo.setOwnerRefTask(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.relationRegistry));
        repo.setWaitingReason(RUtil.getRepoEnumValue(jaxb.getWaitingReason(), RTaskWaitingReason.class));
        repo.setDependent(RUtil.listToSet(jaxb.getDependent()));

//        WfContextType wfc = jaxb.getApprovalContext();
//        if (wfc != null) {
//            repo.setWfRequesterRef(RUtil.jaxbRefToEmbeddedRepoRef(wfc.getRequesterRef(), repositoryContext.relationRegistry));
//            repo.setWfObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(wfc.getObjectRefOrClone(), repositoryContext.relationRegistry));
//            repo.setWfTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(wfc.getTargetRef(), repositoryContext.relationRegistry));
//            repo.setWfStartTimestamp(wfc.getStartTimestamp());
//            repo.setWfEndTimestamp(wfc.getEndTimestamp());
//        }

        RUtil.copyResultFromJAXB(TaskType.F_RESULT, jaxb.getResult(), repo, repositoryContext.prismContext);
    }
}
