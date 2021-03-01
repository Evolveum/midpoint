/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.util.TimeBoundary;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

@Component
public class TaskCleaner {

    private static final Trace LOGGER = TraceManager.getTrace(TaskCleaner.class);
    public static final String OP_STATISTICS = TaskManagerQuartzImpl.OP_CLEANUP_TASKS + ".statistics";

    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;
    @Autowired private TaskInstantiator taskInstantiator;
    @Autowired private TaskStateManager taskStateManager;

    public void cleanupTasks(CleanupPolicyType policy, RunningTask executionTask, OperationResult result) throws SchemaException {
        if (policy.getMaxAge() == null) {
            return;
        }

        TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
        XMLGregorianCalendar deleteTasksClosedUpTo = timeBoundary.getBoundary();

        LOGGER.info("Starting cleanup for closed tasks deleting up to {} (duration '{}').", deleteTasksClosedUpTo,
                timeBoundary.getPositiveDuration());

        ObjectQuery obsoleteTasksQuery = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStateType.CLOSED)
                .and().item(TaskType.F_COMPLETION_TIMESTAMP).le(deleteTasksClosedUpTo)
                .and().item(TaskType.F_PARENT).isNull()
                .build();
        List<PrismObject<TaskType>> obsoleteTasks = repositoryService.searchObjects(TaskType.class, obsoleteTasksQuery, null, result);

        LOGGER.debug("Found {} task tree(s) to be cleaned up", obsoleteTasks.size());

        boolean interrupted = false;
        int deleted = 0;
        int problems = 0;
        int bigProblems = 0;
        for (PrismObject<TaskType> rootTaskPrism : obsoleteTasks) {

            if (!executionTask.canRun()) {
                result.recordWarning("Interrupted");
                LOGGER.warn("Task cleanup was interrupted.");
                interrupted = true;
                break;
            }

            IterativeOperationStartInfo iterativeOperationStartInfo = new IterativeOperationStartInfo(
                    new IterationItemInformation(rootTaskPrism), SchemaConstants.CLOSED_TASKS_CLEANUP_TASK_PART_URI);
            iterativeOperationStartInfo.setStructuredProgressCollector(executionTask);
            Operation op = executionTask.recordIterativeOperationStart(iterativeOperationStartInfo);
            try {
                // get whole tree
                TaskQuartzImpl rootTask = taskInstantiator.createTaskInstance(rootTaskPrism, result);
                List<TaskQuartzImpl> taskTreeMembers = rootTask.listSubtasksDeeply(true, result);
                taskTreeMembers.add(rootTask);

                LOGGER.trace("Removing task {} along with its {} children.", rootTask, taskTreeMembers.size() - 1);

                Throwable lastProblem = null;
                for (Task task : taskTreeMembers) {
                    try {
                        // TODO use repository service only - the task should be closed now
                        taskStateManager.deleteTask(task.getOid(), result);
                        deleted++;
                    } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete obsolete task {}", e, task);
                        lastProblem = e;
                        problems++;
                        if (!task.getTaskIdentifier().equals(rootTask.getTaskIdentifier())) {
                            bigProblems++;
                        }
                    }
                }
                // approximate solution (as the problem might be connected to a subtask)
                if (lastProblem != null) {
                    op.failed(lastProblem);
                } else {
                    op.succeeded();
                }
            } catch (Throwable t) {
                op.failed(t);
                throw t;
            }
            // structured progress is incremented with iterative operation reporting
            executionTask.incrementProgressAndStoreStatsIfNeeded();
        }

        LOGGER.info("Task cleanup procedure " + (interrupted ? "was interrupted" : "finished")
                + ". Successfully deleted {} tasks; there were problems with deleting {} tasks.", deleted, problems);
        if (bigProblems > 0) {
            LOGGER.error(
                    "{} subtask(s) couldn't be deleted. Inspect that manually, otherwise they might reside in repo forever.",
                    bigProblems);
        }
        String suffix = interrupted ? " Interrupted." : "";
        if (problems == 0) {
            result.createSubresult(OP_STATISTICS)
                    .recordStatus(SUCCESS, "Successfully deleted " + deleted + " task(s)." + suffix);
        } else {
            result.createSubresult(OP_STATISTICS)
                    .recordPartialError("Successfully deleted " + deleted + " task(s), "
                            + "there was problems with deleting " + problems + " tasks." + suffix
                            + (bigProblems > 0 ?
                            (" " + bigProblems + " subtask(s) couldn't be deleted, please see the log.") :
                            ""));
        }
    }
}
