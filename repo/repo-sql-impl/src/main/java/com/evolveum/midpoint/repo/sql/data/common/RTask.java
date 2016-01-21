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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.*;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Collection;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_task")
@Persister(impl = MidPointJoinedPersister.class)
public class RTask extends RObject<TaskType> implements OperationResult {

    private RPolyString name;
    private String taskIdentifier;
    private RTaskExecutionStatus executionStatus;
    private String node;
    private String category;
    private String handlerUri;
    //operation result
    private ROperationResultStatus status;
    //end of operation result
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar completionTimestamp;
    private RTaskRecurrence recurrence;
    private RTaskBinding binding;

    private REmbeddedReference objectRef;
    private REmbeddedReference ownerRef;
    private String parent;

    private String canRunOnNode;
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

    @Column(nullable = true)
    public String getCanRunOnNode() {
        return canRunOnNode;
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

    @Embedded
    public REmbeddedReference getOwnerRef() {
        return ownerRef;
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

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setCanRunOnNode(String canRunOnNode) {
        this.canRunOnNode = canRunOnNode;
    }

    public void setThreadStopAction(RThreadStopAction threadStopAction) {
        this.threadStopAction = threadStopAction;
    }

    public void setObjectRef(REmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    public void setOwnerRef(REmbeddedReference ownerRef) {
        this.ownerRef = ownerRef;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RTask rTask = (RTask) o;

        if (name != null ? !name.equals(rTask.name) : rTask.name != null) return false;
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
        if (ownerRef != null ? !ownerRef.equals(rTask.ownerRef) : rTask.ownerRef != null) return false;
        if (recurrence != rTask.recurrence) return false;
        if (taskIdentifier != null ? !taskIdentifier.equals(rTask.taskIdentifier) : rTask.taskIdentifier != null)
            return false;
        if (canRunOnNode != null ? !canRunOnNode.equals(rTask.canRunOnNode) : rTask.canRunOnNode != null) return false;
        if (threadStopAction != null ? !threadStopAction.equals(rTask.threadStopAction) :
                rTask.threadStopAction != null) return false;
        if (category != null ? !category.equals(rTask.category) : rTask.category != null) return false;
        if (parent != null ? !parent.equals(rTask.parent) : rTask.parent != null) return false;
        if (dependent != null ? !dependent.equals(rTask.dependent) : rTask.dependent != null) return false;
        if (waitingReason != null ? !waitingReason.equals(rTask.waitingReason) : rTask.waitingReason != null)
            return false;
        if (status != rTask.status) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (taskIdentifier != null ? taskIdentifier.hashCode() : 0);
        result1 = 31 * result1 + (executionStatus != null ? executionStatus.hashCode() : 0);
        result1 = 31 * result1 + (node != null ? node.hashCode() : 0);
        result1 = 31 * result1 + (handlerUri != null ? handlerUri.hashCode() : 0);
        result1 = 31 * result1 + (lastRunStartTimestamp != null ? lastRunStartTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (completionTimestamp != null ? completionTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (lastRunFinishTimestamp != null ? lastRunFinishTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (recurrence != null ? recurrence.hashCode() : 0);
        result1 = 31 * result1 + (binding != null ? binding.hashCode() : 0);
        result1 = 31 * result1 + (canRunOnNode != null ? canRunOnNode.hashCode() : 0);
        result1 = 31 * result1 + (threadStopAction != null ? threadStopAction.hashCode() : 0);
        result1 = 31 * result1 + (category != null ? category.hashCode() : 0);
        result1 = 31 * result1 + (parent != null ? parent.hashCode() : 0);
        result1 = 31 * result1 + (waitingReason != null ? waitingReason.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);

        return result1;
    }

    public static void copyFromJAXB(TaskType jaxb, RTask repo, PrismContext prismContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext, generatorResult);

        PrismObjectDefinition<TaskType> taskDefinition = jaxb.asPrismObject().getDefinition();

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setTaskIdentifier(jaxb.getTaskIdentifier());
        repo.setExecutionStatus(RUtil.getRepoEnumValue(jaxb.getExecutionStatus(), RTaskExecutionStatus.class));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setCompletionTimestamp(jaxb.getCompletionTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setBinding(RUtil.getRepoEnumValue(jaxb.getBinding(), RTaskBinding.class));
        repo.setRecurrence(RUtil.getRepoEnumValue(jaxb.getRecurrence(), RTaskRecurrence.class));
        repo.setCanRunOnNode(jaxb.getCanRunOnNode());
        repo.setThreadStopAction(RUtil.getRepoEnumValue(jaxb.getThreadStopAction(), RThreadStopAction.class));
        repo.setCategory(jaxb.getCategory());
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), prismContext));
        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), prismContext));
        repo.setWaitingReason(RUtil.getRepoEnumValue(jaxb.getWaitingReason(), RTaskWaitingReason.class));
        repo.setDependent(RUtil.listToSet(jaxb.getDependent()));

        RUtil.copyResultFromJAXB(taskDefinition, jaxb.F_RESULT, jaxb.getResult(), repo, prismContext);
    }

    @Override
    public TaskType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        TaskType object = new TaskType();
        RUtil.revive(object, prismContext);
        RTask.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
