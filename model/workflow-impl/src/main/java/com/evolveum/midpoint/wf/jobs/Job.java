package com.evolveum.midpoint.wf.jobs;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import org.apache.commons.lang.Validate;

/**
 * A class that describes jobs related to workflow module:
 *  - typically, a workflow process instances
 *  - but sometimes also jobs without workflow -> e.g. tasks that carry out modifications that don't require approval.
 *
 * It points to the activiti process instance information as well as to the corresponding midPoint task.
 *
 * @author mederly
 */
public class Job {

    private JobController jobController;

    private Task task;                          // must be non-null
    private String activitiId;                  // must be non-null for Activiti-related jobs (and may be filled-in later, when activiti process is started)
    private ChangeProcessor changeProcessor;    // must be non-null

    //region Constructors and basic getters
    Job(JobController jobController, Task task, ChangeProcessor changeProcessor) {
        this(jobController, task, null, changeProcessor);
    }

    Job(JobController jobController, Task task, String activitiId, ChangeProcessor changeProcessor) {
        this.jobController = jobController;
        this.task = task;
        this.activitiId = activitiId;
        this.changeProcessor = changeProcessor;
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

    public void addDependent(Job job) {
        jobController.addDependency(this, job);
    }

    public void commitChanges(OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        task.savePendingModifications(result);
    }

    public void resumeTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        jobController.resumeTask(this, result);
    }

    public void startWaitingForSubtasks(OperationResult result) throws SchemaException, ObjectNotFoundException {
        task.startWaitingForTasksImmediate(result);
    }

    public void setWfProcessIdImmediate(String pid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        activitiId = pid;
        jobController.getWfTaskUtil().setWfProcessIdImmediate(task, pid, result);
    }

    public void setProcessInstanceFinishedImmediate(boolean value, OperationResult result) throws SchemaException, ObjectNotFoundException {
        jobController.getWfTaskUtil().setProcessInstanceFinishedImmediate(task, value, result);
    }

    public TaskExecutionStatus getTaskExecutionStatus() {
        return task.getExecutionStatus();
    }

    public void removeCurrentTaskHandlerAndUnpause(OperationResult result) throws SchemaException, ObjectNotFoundException {
        boolean wasWaiting = getTaskExecutionStatus() == TaskExecutionStatus.WAITING;
        task.finishHandler(result);
        boolean isWaiting = getTaskExecutionStatus() == TaskExecutionStatus.WAITING;
        if (wasWaiting && isWaiting) {  // if the task was not closed ... (i.e. if there are other handler(s) on the stack)
            jobController.unpauseTask(this, result);
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
}
