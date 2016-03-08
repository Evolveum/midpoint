package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.ObjectTreeDeltas;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.task.api.TaskExecutionStatus.WAITING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_STATE;

/**
 * A class that describes wf-enabled tasks (plus tasks that do not carry wf process, like "task0" executing changes that do not need approvals)
 *
 * It points to the activiti process instance information as well as to the corresponding midPoint task.
 *
 * @author mederly
 */
public class WfTask {

    private WfTaskController wfTaskController;

    private Task task;                          // must be non-null
    private String activitiId;                  // must be non-null for Activiti-related jobs (and may be filled-in later, when activiti process is started)
    private ChangeProcessor changeProcessor;    // must be non-null

    //region Constructors and basic getters
    WfTask(WfTaskController wfTaskController, Task task, ChangeProcessor changeProcessor) {
        this(wfTaskController, task, null, changeProcessor);
    }

    WfTask(WfTaskController wfTaskController, Task task, String activitiId, ChangeProcessor changeProcessor) {
        Validate.notNull(task, "Task");
        Validate.notNull(changeProcessor, "Change processor");
        this.wfTaskController = wfTaskController;
        this.task = task;
        this.activitiId = activitiId;
        this.changeProcessor = changeProcessor;
    }

    protected WfTask(WfTask original) {
        this.wfTaskController = original.wfTaskController;
        this.task = original.task;
        this.activitiId = original.activitiId;
        this.changeProcessor = original.changeProcessor;
    }

    public String getActivitiId() {
        return activitiId;
    }

    public Task getTask() {
        return task;
    }

    public ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }
    //endregion


    @Override
    public String toString() {
        return "WfTask{" +
                "task=" + task +
                ", activitiId='" + activitiId + '\'' +
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

    public void setWfProcessId(String pid) throws SchemaException {
		activitiId = pid;
        wfTaskController.getWfTaskUtil().setWfProcessId(task, pid);
    }

    public void setProcessInstanceFinishedImmediate(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        wfTaskController.getWfTaskUtil().setProcessInstanceFinishedImmediate(task, result);
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

    public ModelContext retrieveModelContext(OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        return getWfTaskUtil().retrieveModelContext(task, result);
    }

    public List<WfTask> listChildren(OperationResult result) throws SchemaException {
        List<WfTask> wfTasks = new ArrayList<WfTask>();
        for (Task subtask : task.listSubtasks(result)) {
            wfTasks.add(wfTaskController.recreateChildWfTask(subtask, this));
        }
        return wfTasks;
    }

    public ObjectTreeDeltas retrieveResultingDeltas() throws SchemaException {
        return getWfTaskUtil().retrieveResultingDeltas(task);
    }

    public void deleteModelOperationContext(OperationResult result) throws SchemaException, ObjectNotFoundException {
        getWfTaskUtil().deleteModelOperationContext(task, result);
    }

    public void storeModelContext(ModelContext modelContext) throws SchemaException {
        getWfTaskUtil().storeModelContext(task, modelContext);
    }

    public List<WfTask> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<WfTask> wfTasks = new ArrayList<WfTask>();
        for (Task subtask : task.listDependents(result)) {
            wfTasks.add(wfTaskController.recreateWfTask(subtask));
        }
        return wfTasks;
    }

    public WfTask getParentJob(OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task parentTask = task.getParentTask(result);
        return wfTaskController.recreateWfTask(parentTask);
    }

    public String getRequesterOid() {
        if (task.getWorkflowContext() == null || task.getWorkflowContext().getRequesterRef() == null) {
            return null;
        } else {
			return task.getWorkflowContext().getRequesterRef().getOid();
		}
    }

	void setProcessInstanceState(String stateDescription) throws SchemaException {
		task.addModification(DeltaBuilder.deltaFor(TaskType.class, getPrismContext()).item(F_WORKFLOW_CONTEXT, F_STATE).replace(stateDescription).asItemDelta());
	}

	private PrismContext getPrismContext() {
		return wfTaskController.getPrismContext();
	}
}
