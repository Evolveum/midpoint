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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.*;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
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
import java.util.HashSet;
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

    @QueryAttribute(polyString = true)
    private RPolyString name;
    @QueryAttribute
    private String taskIdentifier;
    @QueryAttribute(enumerated = true)
    private RTaskExecutionStatusType executionStatus;
    private String node;
    @QueryAttribute
    private String category;
    private String handlerUri;
    private String otherHandlersUriStack;
    private ROperationResult result;
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private Long progress;
    private RTaskRecurrenceType recurrence;
    private RTaskBindingType binding;
    private String schedule;

    private REmbeddedReference objectRef;
    private REmbeddedReference ownerRef;
    @QueryAttribute
    private String parent;

    private ROperationResultStatusType resultStatus;
    private String canRunOnNode;
    private RThreadStopActionType threadStopAction;
    @QueryAttribute(multiValue = true)
    private Set<String> dependent;
    @QueryAttribute(enumerated = true)
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
    public ROperationResultStatusType getResultStatus() {
        return resultStatus;
    }

    public String getCategory() {
        return category;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RThreadStopActionType getThreadStopAction() {
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
    public RTaskBindingType getBinding() {
        return binding;
    }

    @Enumerated(EnumType.ORDINAL)
    public RTaskExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    @Enumerated(EnumType.ORDINAL)
    public RTaskRecurrenceType getRecurrence() {
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

    public void setResultStatus(ROperationResultStatusType resultStatus) {
        this.resultStatus = resultStatus;
    }

    public void setThreadStopAction(RThreadStopActionType threadStopAction) {
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

    public void setBinding(RTaskBindingType binding) {
        this.binding = binding;
    }

    public void setExecutionStatus(RTaskExecutionStatusType executionStatus) {
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

    public void setNode(String node) {
        this.node = node;
    }

    public void setProgress(Long progress) {
        this.progress = progress;
    }

    public void setRecurrence(RTaskRecurrenceType recurrence) {
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
        if (waitingReason != null ? !waitingReason.equals(rTask.waitingReason) : rTask.waitingReason != null) return false;

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
            jaxb.setExecutionStatus(repo.getExecutionStatus().getStatus());
        }
        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setLastRunFinishTimestamp(repo.getLastRunFinishTimestamp());
        jaxb.setLastRunStartTimestamp(repo.getLastRunStartTimestamp());
        jaxb.setNode(repo.getNode());
        jaxb.setProgress(repo.getProgress());
        if (repo.getBinding() != null) {
            jaxb.setBinding(repo.getBinding().getBinding());
        }
        if (repo.getRecurrence() != null) {
            jaxb.setRecurrence(repo.getRecurrence().getRecurrence());
        }
        if (repo.getResultStatus() != null) {
            jaxb.setResultStatus(repo.getResultStatus().getStatus());
        }
        jaxb.setCanRunOnNode(repo.getCanRunOnNode());
        if (repo.getThreadStopAction() != null) {
            jaxb.setThreadStopAction(repo.getThreadStopAction().getAction());
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
            jaxb.setWaitingReason(repo.getWaitingReason().getReason());
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
        repo.setExecutionStatus(RTaskExecutionStatusType.toRepoType(jaxb.getExecutionStatus()));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setProgress(jaxb.getProgress());
        repo.setBinding(RTaskBindingType.toRepoType(jaxb.getBinding()));
        repo.setRecurrence(RTaskRecurrenceType.toRepoType(jaxb.getRecurrence()));
        repo.setResultStatus(ROperationResultStatusType.toRepoType(jaxb.getResultStatus()));
        repo.setCanRunOnNode(jaxb.getCanRunOnNode());
        repo.setThreadStopAction(RThreadStopActionType.toRepoType(jaxb.getThreadStopAction()));
        repo.setCategory(jaxb.getCategory());
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), prismContext));
        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), prismContext));
        repo.setWaitingReason(RTaskWaitingReason.toRepoType(jaxb.getWaitingReason()));
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
