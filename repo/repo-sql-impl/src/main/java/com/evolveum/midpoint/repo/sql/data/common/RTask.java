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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.*;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UriStack;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_task")
@org.hibernate.annotations.Table(appliesTo = "m_task",
        indexes = {@Index(name = "iTaskName", columnNames = "name_norm")})
public class RTask extends RObject {

    private RPolyString name;
    private String taskIdentifier;
    private RTaskExecutionStatus executionStatus;
    private String node;
    private String category;
    private String handlerUri;
    private String otherHandlersUriStack;
    private ROperationResult result;
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar completionTimestamp;
    private Long progress;
    private RTaskRecurrence recurrence;
    private RTaskBinding binding;
    private String schedule;

    private REmbeddedReference objectRef;
    private REmbeddedReference ownerRef;
    private String parent;

    private ROperationResultStatus resultStatus;
    private String canRunOnNode;
    private RThreadStopAction threadStopAction;
    private Set<String> dependent;
    private RTaskWaitingReason waitingReason;

    @ElementCollection
    @ForeignKey(name = "fk_task_dependent")
    @CollectionTable(name = "m_task_dependent", joinColumns = {
            @JoinColumn(name = "task_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "task_id", referencedColumnName = "id")
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

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public ROperationResultStatus getResultStatus() {
        return resultStatus;
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

    public String getParent() {
        return parent;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getOtherHandlersUriStack() {
        return otherHandlersUriStack;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Column(nullable = true)
    public String getSchedule() {
        return schedule;
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

    @OneToOne(optional = true, mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ROperationResult getResult() {
        return result;
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

    public void setResultStatus(ROperationResultStatus resultStatus) {
        this.resultStatus = resultStatus;
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
    public Long getProgress() {
        return progress;
    }

    @Column(nullable = true)
    public String getTaskIdentifier() {
        return taskIdentifier;
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

    public void setProgress(Long progress) {
        this.progress = progress;
    }

    public void setRecurrence(RTaskRecurrence recurrence) {
        this.recurrence = recurrence;
    }

    public void setResult(ROperationResult result) {
        this.result = result;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public void setOtherHandlersUriStack(String otherHandlersUriStack) {
        this.otherHandlersUriStack = otherHandlersUriStack;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
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
        if (otherHandlersUriStack != null ? !otherHandlersUriStack.equals(rTask.otherHandlersUriStack) : rTask.otherHandlersUriStack != null)
            return false;
        if (ownerRef != null ? !ownerRef.equals(rTask.ownerRef) : rTask.ownerRef != null) return false;
        if (progress != null ? !progress.equals(rTask.progress) : rTask.progress != null) return false;
        if (recurrence != rTask.recurrence) return false;
        if (result != null ? !result.equals(rTask.result) : rTask.result != null) return false;
        if (schedule != null ? !schedule.equals(rTask.schedule) : rTask.schedule != null) return false;
        if (taskIdentifier != null ? !taskIdentifier.equals(rTask.taskIdentifier) : rTask.taskIdentifier != null)
            return false;
        if (resultStatus != null ? !resultStatus.equals(rTask.resultStatus) : rTask.resultStatus != null) return false;
        if (canRunOnNode != null ? !canRunOnNode.equals(rTask.canRunOnNode) : rTask.canRunOnNode != null) return false;
        if (threadStopAction != null ? !threadStopAction.equals(rTask.threadStopAction) :
                rTask.threadStopAction != null) return false;
        if (category != null ? !category.equals(rTask.category) : rTask.category != null) return false;
        if (parent != null ? !parent.equals(rTask.parent) : rTask.parent != null) return false;
        if (dependent != null ? !dependent.equals(rTask.dependent) : rTask.dependent != null) return false;
        if (waitingReason != null ? !waitingReason.equals(rTask.waitingReason) : rTask.waitingReason != null)
            return false;

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
        result1 = 31 * result1 + (otherHandlersUriStack != null ? otherHandlersUriStack.hashCode() : 0);
        result1 = 31 * result1 + (lastRunStartTimestamp != null ? lastRunStartTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (completionTimestamp != null ? completionTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (lastRunFinishTimestamp != null ? lastRunFinishTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (progress != null ? progress.hashCode() : 0);
        result1 = 31 * result1 + (recurrence != null ? recurrence.hashCode() : 0);
        result1 = 31 * result1 + (binding != null ? binding.hashCode() : 0);
        result1 = 31 * result1 + (schedule != null ? schedule.hashCode() : 0);
        result1 = 31 * result1 + (resultStatus != null ? resultStatus.hashCode() : 0);
        result1 = 31 * result1 + (canRunOnNode != null ? canRunOnNode.hashCode() : 0);
        result1 = 31 * result1 + (threadStopAction != null ? threadStopAction.hashCode() : 0);
        result1 = 31 * result1 + (category != null ? category.hashCode() : 0);
        result1 = 31 * result1 + (parent != null ? parent.hashCode() : 0);
        result1 = 31 * result1 + (waitingReason != null ? waitingReason.hashCode() : 0);

        return result1;
    }

    public static void copyToJAXB(RTask repo, TaskType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setTaskIdentifier(repo.getTaskIdentifier());
        if (repo.getExecutionStatus() != null) {
            jaxb.setExecutionStatus(repo.getExecutionStatus().getSchemaValue());
        }
        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setLastRunFinishTimestamp(repo.getLastRunFinishTimestamp());
        jaxb.setCompletionTimestamp(repo.getCompletionTimestamp());
        jaxb.setLastRunStartTimestamp(repo.getLastRunStartTimestamp());
        jaxb.setNode(repo.getNode());
        jaxb.setProgress(repo.getProgress());
        if (repo.getBinding() != null) {
            jaxb.setBinding(repo.getBinding().getSchemaValue());
        }
        if (repo.getRecurrence() != null) {
            jaxb.setRecurrence(repo.getRecurrence().getSchemaValue());
        }
        if (repo.getResultStatus() != null) {
            jaxb.setResultStatus(repo.getResultStatus().getSchemaValue());
        }
        jaxb.setCanRunOnNode(repo.getCanRunOnNode());
        if (repo.getThreadStopAction() != null) {
            jaxb.setThreadStopAction(repo.getThreadStopAction().getSchemaValue());
        }
        jaxb.setCategory(repo.getCategory());
        jaxb.setParent(repo.getParent());

        if (repo.getObjectRef() != null) {
            jaxb.setObjectRef(repo.getObjectRef().toJAXB(prismContext));
        }
        if (repo.getOwnerRef() != null) {
            jaxb.setOwnerRef(repo.getOwnerRef().toJAXB(prismContext));
        }

        if (repo.getResult() != null) {
            jaxb.setResult(repo.getResult().toJAXB(prismContext));
        }

        if (repo.getWaitingReason() != null) {
            jaxb.setWaitingReason(repo.getWaitingReason().getSchemaValue());
        }
        List types = RUtil.safeSetToList(repo.getDependent());
        if (!types.isEmpty()) {
            jaxb.getDependent().addAll(types);
        }

        try {
            jaxb.setOtherHandlersUriStack(RUtil.toJAXB(TaskType.class, new ItemPath(TaskType.F_OTHER_HANDLERS_URI_STACK),
                    repo.getOtherHandlersUriStack(), UriStack.class, prismContext));
            jaxb.setSchedule(RUtil.toJAXB(TaskType.class, new ItemPath(TaskType.F_SCHEDULE), repo.getSchedule(),
                    ScheduleType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(TaskType jaxb, RTask repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setTaskIdentifier(jaxb.getTaskIdentifier());
        repo.setExecutionStatus(RUtil.getRepoEnumValue(jaxb.getExecutionStatus(), RTaskExecutionStatus.class));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setCompletionTimestamp(jaxb.getCompletionTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setProgress(jaxb.getProgress());
        repo.setBinding(RUtil.getRepoEnumValue(jaxb.getBinding(), RTaskBinding.class));
        repo.setRecurrence(RUtil.getRepoEnumValue(jaxb.getRecurrence(), RTaskRecurrence.class));
        repo.setResultStatus(RUtil.getRepoEnumValue(jaxb.getResultStatus(), ROperationResultStatus.class));
        repo.setCanRunOnNode(jaxb.getCanRunOnNode());
        repo.setThreadStopAction(RUtil.getRepoEnumValue(jaxb.getThreadStopAction(), RThreadStopAction.class));
        repo.setCategory(jaxb.getCategory());
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), prismContext));
        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), prismContext));
        repo.setWaitingReason(RUtil.getRepoEnumValue(jaxb.getWaitingReason(), RTaskWaitingReason.class));
        repo.setDependent(RUtil.listToSet(jaxb.getDependent()));

        if (jaxb.getResult() != null) {
            ROperationResult result = new ROperationResult();
            result.setOwner(repo);
            ROperationResult.copyFromJAXB(jaxb.getResult(), result, prismContext);
            repo.setResult(result);
        }

        try {
            repo.setOtherHandlersUriStack(RUtil.toRepo(jaxb.getOtherHandlersUriStack(), prismContext));
            repo.setSchedule(RUtil.toRepo(jaxb.getSchedule(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public TaskType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        TaskType object = new TaskType();
        RUtil.revive(object, prismContext);
        RTask.copyToJAXB(this, object, prismContext);

        return object;
    }
}
