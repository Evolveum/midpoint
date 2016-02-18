package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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

import java.util.ArrayList;
import java.util.List;

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

    protected Job(Job original) {
        this.jobController = original.jobController;
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
        return "Job{" +
                "task=" + task +
                ", activitiId='" + activitiId + '\'' +
                ", changeProcessor=" + changeProcessor +
                '}';
    }

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

    public boolean hasModelContext() {
        return getWfTaskUtil().hasModelContext(task);
    }

    private WfTaskUtil getWfTaskUtil() {
        return jobController.getWfTaskUtil();
    }

    public ModelContext retrieveModelContext(OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        return getWfTaskUtil().retrieveModelContext(task, result);
    }

    public List<Job> listChildren(OperationResult result) throws SchemaException {
        List<Job> jobs = new ArrayList<Job>();
        for (Task subtask : task.listSubtasks(result)) {
            jobs.add(jobController.recreateChildJob(subtask, this));
        }
        return jobs;
    }

    public ObjectTreeDeltas retrieveResultingDeltas() throws SchemaException {
        return getWfTaskUtil().retrieveResultingDeltas(task);
    }

    public void setSkipModelContextProcessingProperty(OperationResult result) throws SchemaException, ObjectNotFoundException {
        getWfTaskUtil().setSkipModelContextProcessingProperty(task, result);
    }

    public void storeModelContext(ModelContext modelContext) throws SchemaException {
        getWfTaskUtil().storeModelContext(task, modelContext);
    }

    public List<Job> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<Job> jobs = new ArrayList<Job>();
        for (Task subtask : task.listDependents(result)) {
            jobs.add(jobController.recreateJob(subtask));
        }
        return jobs;
    }

    public Job getParentJob(OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task parentTask = task.getParentTask(result);
        return jobController.recreateJob(parentTask);
    }
}
