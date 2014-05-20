package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;

import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class JobContext {

    private String parentTaskOid;
    private Task parentTask;
    private ChangeProcessor changeProcessor;

    public JobContext(Task parentTask, ChangeProcessor changeProcessor) {
        Validate.notNull(parentTask);
        Validate.notNull(changeProcessor);
        this.parentTask = parentTask;
        this.parentTaskOid = parentTask.getOid();       // may be null, however
        this.changeProcessor = changeProcessor;
    }

    public String getParentTaskOid() {
        return parentTaskOid;
    }

    public void setParentTaskOid(String parentTaskOid) {
        this.parentTaskOid = parentTaskOid;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    public ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    public void setChangeProcessor(ChangeProcessor changeProcessor) {
        this.changeProcessor = changeProcessor;
    }
}
