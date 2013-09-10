package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobContext;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;

import java.util.List;

/**
 * @author mederly
 */
public class PrimaryChangeProcessorJob {

    private Job delegate;

    PrimaryChangeProcessorJob(Job delegate) {
        this.delegate = delegate;
    }

    //region Delegation
    public String getActivitiId() {
        return delegate.getActivitiId();
    }

    public void removeCurrentTaskHandlerAndUnpause(OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.removeCurrentTaskHandlerAndUnpause(result);
    }

    public ChangeProcessor getChangeProcessor() {
        return delegate.getChangeProcessor();
    }

    public TaskExecutionStatus getTaskExecutionStatus() {
        return delegate.getTaskExecutionStatus();
    }

    public void startWaitingForSubtasks(OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.startWaitingForSubtasks(result);
    }

    public void commitChanges(OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        delegate.commitChanges(result);
    }

    public Task getTask() {
        return delegate.getTask();
    }

    public void computeTaskResultIfUnknown(OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.computeTaskResultIfUnknown(result);
    }

    public void setProcessInstanceFinishedImmediate(boolean value, OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.setProcessInstanceFinishedImmediate(value, result);
    }

    public void setWfProcessIdImmediate(String pid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.setWfProcessIdImmediate(pid, result);
    }

    public void resumeTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        delegate.resumeTask(result);
    }

    public void addDependent(Job job) {
        delegate.addDependent(job);
    }
    //endregion

    public PrimaryApprovalProcessWrapper getProcessWrapper() {
        return getWfTaskUtil().getProcessWrapper(getTask(), getPcp().getProcessWrappers());
    }

    private PrimaryChangeProcessor getPcp() {
        return (PrimaryChangeProcessor) getChangeProcessor();
    }

    private WfTaskUtil getWfTaskUtil() {
        return getPcp().getWfTaskUtil();
    }

    public void storeResultingDeltas(List<ObjectDelta<Objectable>> deltas) throws SchemaException {
        getWfTaskUtil().storeResultingDeltas(deltas, getTask());
    }

    public void addApprovedBy(List<ObjectReferenceType> approvedBy) throws SchemaException {
        getWfTaskUtil().addApprovedBy(getTask(), approvedBy);
    }

    public List<ObjectDelta<Objectable>> retrieveDeltasToProcess() throws SchemaException {
        return getWfTaskUtil().retrieveDeltasToProcess(getTask());
    }
}
