/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.task.api.TaskExecutionStatus.WAITING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.*;

/**
 * A class that describes wf-enabled tasks (plus tasks that do not carry wf process, like "task0" executing changes that do not need approvals)
 *
 * It points to the case object as well as to the corresponding midPoint task.
 *
 * @author mederly
 */
public class WfTask {

    @NotNull private final WfTaskController wfTaskController;
    @NotNull private final Task task;
    @NotNull private final ChangeProcessor changeProcessor;
    private String caseOid;                     // must be non-null for workflow-related tasks (and may be filled-in later, when activiti process is started)

    //region Constructors and basic getters
    WfTask(WfTaskController wfTaskController, Task task, ChangeProcessor changeProcessor) {
        this(wfTaskController, task, null, changeProcessor);
    }

    protected WfTask(@NotNull WfTaskController wfTaskController, @NotNull Task task, String caseOid, @NotNull ChangeProcessor changeProcessor) {
        this.wfTaskController = wfTaskController;
        this.task = task;
        this.caseOid = caseOid;
        this.changeProcessor = changeProcessor;
    }

    protected WfTask(WfTask original) {
        this.wfTaskController = original.wfTaskController;
        this.task = original.task;
        this.caseOid = original.caseOid;
        this.changeProcessor = original.changeProcessor;
    }

    public String getCaseOid() {
        return caseOid;
    }

    @NotNull
    public Task getTask() {
        return task;
    }

    @NotNull
    public ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }
    //endregion

    @Override
    public String toString() {
        return "WfTask{" +
                "task=" + task +
                ", caseOid='" + caseOid + '\'' +
                ", changeProcessor=" + changeProcessor +
                '}';
    }

    public void addDependent(WfTask wfTask) {
        wfTaskController.addDependency(this, wfTask);
    }

    public void commitChanges(OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        task.savePendingModifications(result);
    }

    public void resumeTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        wfTaskController.resumeTask(this, result);
    }

    public void startWaitingForSubtasks(OperationResult result) throws SchemaException, ObjectNotFoundException {
        task.startWaitingForTasksImmediate(result);
    }

    public void setCaseOid(String oid) throws SchemaException {
		caseOid = oid;
        task.addModification(
                getPrismContext().deltaFor(TaskType.class)
                        .item(F_WORKFLOW_CONTEXT, F_CASE_OID).replace(oid)
                        .asItemDelta());
    }

    public void setProcessInstanceEndTimestamp() throws SchemaException {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        task.addModification(
                getPrismContext().deltaFor(TaskType.class)
                        .item(F_WORKFLOW_CONTEXT, F_END_TIMESTAMP).replace(now)
                        .asItemDelta());
    }

    public TaskExecutionStatus getTaskExecutionStatus() {
        return task.getExecutionStatus();
    }

    public void removeCurrentTaskHandlerAndUnpause(OperationResult result) throws SchemaException, ObjectNotFoundException {
        boolean wasWaiting = getTaskExecutionStatus() == WAITING;
        task.finishHandler(result);
        boolean isWaiting = getTaskExecutionStatus() == WAITING;
        if (wasWaiting && isWaiting) {  // if the task was not closed ... (i.e. if there are other handler(s) on the stack)
            wfTaskController.unpauseTask(this, result);
        }
    }

    public void computeTaskResultIfUnknown(OperationResult result) throws SchemaException, ObjectNotFoundException {
        OperationResult taskResult = task.getResult();
        if (result.isUnknown()) {
            result.computeStatus();
        }
        taskResult.recordStatus(result.getStatus(), result.getMessage(), result.getCause());
        task.setResultImmediate(taskResult, result);
    }

    public boolean hasModelContext() {
        return getWfTaskUtil().hasModelContext(task);
    }

    private WfTaskUtil getWfTaskUtil() {
        return wfTaskController.getWfTaskUtil();
    }

    public ModelContext retrieveModelContext(OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getWfTaskUtil().getModelContext(task, result);
    }

    public List<WfTask> listChildren(OperationResult result) throws SchemaException {
        List<WfTask> wfTasks = new ArrayList<>();
        for (Task subtask : task.listSubtasks(result)) {
            wfTasks.add(wfTaskController.recreateChildWfTask(subtask, this));
        }
        return wfTasks;
    }

    public ObjectTreeDeltas retrieveResultingDeltas() throws SchemaException {
        return getWfTaskUtil().retrieveResultingDeltas(task);
    }

    public void deleteModelOperationContext() throws SchemaException, ObjectNotFoundException {
        getWfTaskUtil().deleteModelOperationContext(task);
    }

    public void storeModelContext(ModelContext modelContext, boolean reduced) throws SchemaException {
        getWfTaskUtil().storeModelContext(task, modelContext, reduced);
    }

    public List<WfTask> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<WfTask> wfTasks = new ArrayList<>();
        for (Task subtask : task.listDependents(result)) {
            wfTasks.add(wfTaskController.recreateWfTask(subtask));
        }
        return wfTasks;
    }

    public WfTask getParentJob(OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task parentTask = task.getParentTask(result);
        return wfTaskController.recreateWfTask(parentTask);
    }

	private PrismContext getPrismContext() {
		return wfTaskController.getPrismContext();
	}

	public String getProcessInstanceName() {
		return task.getWorkflowContext() != null ? task.getWorkflowContext().getProcessInstanceName() : null;
	}

	public String getOutcome() {
		return task.getWorkflowContext() != null ? task.getWorkflowContext().getOutcome() : null;
	}

	public String getAnswerNice() {
    	return ApprovalUtils.makeNiceFromUri(getOutcome());
	}

	public String getCompleteStageInfo() {
    	return WfContextUtil.getCompleteStageInfo(task.getWorkflowContext());
	}

	@SuppressWarnings("unchecked")
	public PrismObject<UserType> getRequesterIfExists(OperationResult result) {
		if (task.getWorkflowContext() == null || task.getWorkflowContext().getRequesterRef() == null) {
			return null;
		}
		ObjectReferenceType requesterRef = task.getWorkflowContext().getRequesterRef();
		return (PrismObject<UserType>) wfTaskController.getMiscDataUtil().resolveAndStoreObjectReference(requesterRef, result);
	}

    public void setOutcome(String outcome) throws SchemaException {
        task.addModifications(getPrismContext().deltaFor(TaskType.class)
                .item(F_WORKFLOW_CONTEXT, F_OUTCOME).replace(outcome)
                .asItemDeltas());
    }

    public void setProcessInstanceStageInformation(Integer stageNumber)
			throws SchemaException {
        task.addModifications(getPrismContext().deltaFor(TaskType.class)
				.item(F_WORKFLOW_CONTEXT, F_STAGE_NUMBER).replace(stageNumber)
				.asItemDeltas());
    }
}
