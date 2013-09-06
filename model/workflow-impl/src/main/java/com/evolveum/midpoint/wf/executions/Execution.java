package com.evolveum.midpoint.wf.executions;

import com.evolveum.midpoint.task.api.Task;

/**
 * A class that describes executions related to workflow module:
 *  - typically, a workflow process instances
 *  - but sometimes also executions without workflow -> e.g. tasks that carry out modifications that don't require approval.
 *
 * It points to the activiti process instance information as well as to the corresponding midPoint task.
 *
 * @author mederly
 */
public class Execution {

    private ExecutionContext executionContext;

    private String activitiId;          // must be non-null for Activiti-related executions
    private String taskOid;             // must be non-null
    private Task task;                  // may be null if the task is not loaded yet

    public Execution(ExecutionContext executionContext, Task task) {
        this.executionContext = executionContext;
        setTask(task);
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public String getActivitiId() {
        return activitiId;
    }

    public void setActivitiId(String activitiId) {
        this.activitiId = activitiId;
    }

    public String getTaskOid() {
        return taskOid;
    }

    public void setTaskOid(String taskOid) {
        this.taskOid = taskOid;
        if (taskOid == null || !taskOid.equals(task.getOid())) {
            task = null;
        }
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
        this.taskOid = task.getOid();
    }
}
