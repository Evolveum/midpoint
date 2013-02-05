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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ModelOperationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UriStack;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

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
    private RTaskExclusivityStatusType exclusivityStatus;
    private String node;
    @QueryAttribute
    private String category;
    private XMLGregorianCalendar claimExpirationTimestamp;
    private String handlerUri;
    private String otherHandlersUriStack;
    private ROperationResult result;
    private XMLGregorianCalendar lastRunStartTimestamp;
    private XMLGregorianCalendar lastRunFinishTimestamp;
    private XMLGregorianCalendar nextRunStartTime;
    private Long progress;
    private RTaskRecurrenceType recurrence;
    private RTaskBindingType binding;
    private String schedule;
    private String modelOperationState;

    private REmbeddedReference objectRef;
    private REmbeddedReference ownerRef;
    @QueryAttribute
    private String parent;

    private ROperationResultStatusType resultStatus;
    private String canRunOnNode;
    private RThreadStopActionType threadStopAction;

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
    @Column(nullable = true)
    public String getModelOperationState() {
        return modelOperationState;
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
    public RTaskExclusivityStatusType getExclusivityStatus() {
        return exclusivityStatus;
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

    public XMLGregorianCalendar getClaimExpirationTimestamp() {
        return claimExpirationTimestamp;
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
    public XMLGregorianCalendar getNextRunStartTime() {
        return nextRunStartTime;
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

    public void setClaimExpirationTimestamp(XMLGregorianCalendar claimExpirationTimestamp) {
        this.claimExpirationTimestamp = claimExpirationTimestamp;
    }

    public void setExclusivityStatus(RTaskExclusivityStatusType exclusivityStatus) {
        this.exclusivityStatus = exclusivityStatus;
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

    public void setNextRunStartTime(XMLGregorianCalendar nextRunStartTime) {
        this.nextRunStartTime = nextRunStartTime;
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

    public void setModelOperationState(String modelOperationState) {
        this.modelOperationState = modelOperationState;
    }

    public void setOtherHandlersUriStack(String otherHandlersUriStack) {
        this.otherHandlersUriStack = otherHandlersUriStack;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RTask rTask = (RTask) o;

        if (name != null ? !name.equals(rTask.name) : rTask.name != null) return false;
        if (binding != rTask.binding) return false;
        if (claimExpirationTimestamp != null ? !claimExpirationTimestamp.equals(rTask.claimExpirationTimestamp) : rTask.claimExpirationTimestamp != null)
            return false;
        if (exclusivityStatus != rTask.exclusivityStatus) return false;
        if (executionStatus != rTask.executionStatus) return false;
        if (handlerUri != null ? !handlerUri.equals(rTask.handlerUri) : rTask.handlerUri != null) return false;
        if (lastRunFinishTimestamp != null ? !lastRunFinishTimestamp.equals(rTask.lastRunFinishTimestamp) : rTask.lastRunFinishTimestamp != null)
            return false;
        if (lastRunStartTimestamp != null ? !lastRunStartTimestamp.equals(rTask.lastRunStartTimestamp) : rTask.lastRunStartTimestamp != null)
            return false;
        if (modelOperationState != null ? !modelOperationState.equals(rTask.modelOperationState) : rTask.modelOperationState != null)
            return false;
        if (nextRunStartTime != null ? !nextRunStartTime.equals(rTask.nextRunStartTime) : rTask.nextRunStartTime != null)
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

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (taskIdentifier != null ? taskIdentifier.hashCode() : 0);
        result1 = 31 * result1 + (executionStatus != null ? executionStatus.hashCode() : 0);
        result1 = 31 * result1 + (exclusivityStatus != null ? exclusivityStatus.hashCode() : 0);
        result1 = 31 * result1 + (node != null ? node.hashCode() : 0);
        result1 = 31 * result1 + (claimExpirationTimestamp != null ? claimExpirationTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (handlerUri != null ? handlerUri.hashCode() : 0);
        result1 = 31 * result1 + (otherHandlersUriStack != null ? otherHandlersUriStack.hashCode() : 0);
        result1 = 31 * result1 + (lastRunStartTimestamp != null ? lastRunStartTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (lastRunFinishTimestamp != null ? lastRunFinishTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (nextRunStartTime != null ? nextRunStartTime.hashCode() : 0);
        result1 = 31 * result1 + (progress != null ? progress.hashCode() : 0);
        result1 = 31 * result1 + (recurrence != null ? recurrence.hashCode() : 0);
        result1 = 31 * result1 + (binding != null ? binding.hashCode() : 0);
        result1 = 31 * result1 + (schedule != null ? schedule.hashCode() : 0);
        result1 = 31 * result1 + (modelOperationState != null ? modelOperationState.hashCode() : 0);
        result1 = 31 * result1 + (resultStatus != null ? resultStatus.hashCode() : 0);
        result1 = 31 * result1 + (canRunOnNode != null ? canRunOnNode.hashCode() : 0);
        result1 = 31 * result1 + (threadStopAction != null ? threadStopAction.hashCode() : 0);
        result1 = 31 * result1 + (category != null ? category.hashCode() : 0);
        result1 = 31 * result1 + (parent != null ? parent.hashCode() : 0);

        return result1;
    }

    public static void copyToJAXB(RTask repo, TaskType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setTaskIdentifier(repo.getTaskIdentifier());
        jaxb.setClaimExpirationTimestamp(repo.getClaimExpirationTimestamp());
        if (repo.getExclusivityStatus() != null) {
            jaxb.setExclusivityStatus(repo.getExclusivityStatus().getStatus());
        }
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
        jaxb.setNextRunStartTime(repo.getNextRunStartTime());
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

        try {
            jaxb.setModelOperationState(RUtil.toJAXB(TaskType.class, new ItemPath(TaskType.F_MODEL_OPERATION_STATE),
                    repo.getModelOperationState(), ModelOperationStateType.class, prismContext));
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
        repo.setClaimExpirationTimestamp(jaxb.getClaimExpirationTimestamp());
        repo.setExclusivityStatus(RTaskExclusivityStatusType.toRepoType(jaxb.getExclusivityStatus()));
        repo.setExecutionStatus(RTaskExecutionStatusType.toRepoType(jaxb.getExecutionStatus()));
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setLastRunFinishTimestamp(jaxb.getLastRunFinishTimestamp());
        repo.setLastRunStartTimestamp(jaxb.getLastRunStartTimestamp());
        repo.setNode(jaxb.getNode());
        repo.setProgress(jaxb.getProgress());
        repo.setBinding(RTaskBindingType.toRepoType(jaxb.getBinding()));
        repo.setNextRunStartTime(jaxb.getNextRunStartTime());
        repo.setRecurrence(RTaskRecurrenceType.toRepoType(jaxb.getRecurrence()));
        repo.setResultStatus(ROperationResultStatusType.toRepoType(jaxb.getResultStatus()));
        repo.setCanRunOnNode(jaxb.getCanRunOnNode());
        repo.setThreadStopAction(RThreadStopActionType.toRepoType(jaxb.getThreadStopAction()));
        repo.setCategory(jaxb.getCategory());
        repo.setParent(jaxb.getParent());

        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), prismContext));
        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), prismContext));

        if (jaxb.getResult() != null) {
            ROperationResult result = new ROperationResult();
            result.setOwner(repo);
            ROperationResult.copyFromJAXB(jaxb.getResult(), result, prismContext);
            repo.setResult(result);
        }

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
        RUtil.revive(object, prismContext);
        RTask.copyToJAXB(this, object, prismContext);

        return object;
    }
}
