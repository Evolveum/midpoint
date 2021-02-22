package com.evolveum.midpoint.task.quartzimpl.tasks;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Manages task state changes, like suspending, resuming, closing, scheduling-now a task.
 * Basically delegates everything to the helpers.
 */
@Component
public class TaskStateManager {

    @Autowired private TaskRetriever taskRetriever;

    @Autowired private ResumeHelper resumeHelper;
    @Autowired private SuspendAndDeleteHelper suspendAndDeleteHelper;
    @Autowired private CloseHelper closeHelper;
    @Autowired private UnpauseHelper unpauseHelper;
    @Autowired private ScheduleNowHelper scheduleNowHelper;

    //region Task suspension and deletion
    public boolean suspendTask(String taskOid, long waitTime, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl task = taskRetriever.getTaskPlain(taskOid, result);
        return suspendAndDeleteHelper.suspendTask(task, waitTime, result);
    }

    public boolean suspendTask(TaskQuartzImpl task, long waitTime, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return suspendAndDeleteHelper.suspendTask(task, waitTime, result);
    }

    public boolean suspendTaskNoException(TaskQuartzImpl task, long waitTime, OperationResult result) {
        return suspendAndDeleteHelper.suspendTaskNoExceptions(task, waitTime, result);
    }

    public void suspendAndCloseTaskNoException(TaskQuartzImpl task, long waitTime, OperationResult result) {
        suspendAndDeleteHelper.suspendAndCloseTaskNoException(task, waitTime, result);
    }

    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult result) {
        return suspendAndDeleteHelper.suspendTasks(taskOids, waitForStop, result);
    }

    public boolean suspendTaskTree(String rootTaskOid, long waitTime, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return suspendAndDeleteHelper.suspendTaskTree(rootTaskOid, waitTime, result);
    }

    public void suspendAndDeleteTasks(Collection<String> taskOids, long suspendTimeout, boolean alsoSubtasks, OperationResult result) {
        suspendAndDeleteHelper.suspendAndDeleteTasks(taskOids, suspendTimeout, alsoSubtasks, result);
    }

    public void suspendAndDeleteTask(String taskOid, long suspendTimeout, boolean alsoSubtasks, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        suspendAndDeleteHelper.suspendAndDeleteTask(taskOid, suspendTimeout, alsoSubtasks, result);
    }

    public void deleteTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        suspendAndDeleteHelper.deleteTask(oid, result);
    }
    //endregion

    //region Task resuming
    public void resumeTaskTree(String rootTaskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        resumeHelper.resumeTaskTree(rootTaskOid, result);
    }

    public void resumeTask(String taskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl task = taskRetriever.getTaskPlain(taskOid, result);
        resumeHelper.resumeTask(task, result);
    }

    public void resumeTask(TaskQuartzImpl task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        resumeHelper.resumeTask(task, result);
    }
    //endregion

    //region Task unpausing
    public void unpauseTask(TaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
        unpauseHelper.unpauseTask(task, result);
    }
    //endregion

    //region Schedule now!
    public void scheduleTaskNow(String taskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl task = taskRetriever.getTaskPlain(taskOid, result);
        scheduleTaskNow(task, result);
    }

    public void scheduleTaskNow(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        scheduleNowHelper.scheduleTaskNow(task, result);
    }

    // TODO why no callers here?
    public void scheduleCoordinatorAndWorkersNow(String coordinatorOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        scheduleNowHelper.scheduleCoordinatorAndWorkersNow(coordinatorOid, result);
    }
    //endregion

    //region Task closing
    public void closeTask(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        closeHelper.closeTask(task, result);
    }

    public void unpauseIfPossible(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        unpauseHelper.unpauseTaskIfPossible(task, result);
    }
    //endregion

    //region Misc
    static void clearTaskOperationResult(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        OperationResult emptyTaskResult = new OperationResult("run");
        emptyTaskResult.setStatus(OperationResultStatus.IN_PROGRESS);
        ((TaskQuartzImpl) task).setResultImmediate(emptyTaskResult, result);
    }
    //endregion
}
