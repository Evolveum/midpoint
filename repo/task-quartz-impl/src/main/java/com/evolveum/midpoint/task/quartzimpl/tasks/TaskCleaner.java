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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.Operation;
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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.function.Predicate;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

@Component
public class TaskCleaner {

    private static final Trace LOGGER = TraceManager.getTrace(TaskCleaner.class);
    private static final String OP_STATISTICS = TaskManagerQuartzImpl.OP_CLEANUP_TASKS + ".statistics";

    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;
    @Autowired private TaskInstantiator taskInstantiator;
    @Autowired private TaskStateManager taskStateManager;

    public void cleanupTasks(@NotNull CleanupPolicyType policy, @NotNull Predicate<TaskType> selector,
            @NotNull RunningTask executionTask, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (policy.getMaxAge() == null) {
            return;
        }

        TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
        XMLGregorianCalendar deleteTasksClosedUpTo = timeBoundary.getBoundary();

        LOGGER.info("Starting cleanup for closed tasks deleting up to {} (duration '{}').", deleteTasksClosedUpTo,
                timeBoundary.getPositiveDuration());

        ObjectQuery obsoleteTasksQuery = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.CLOSED)
                .and().item(TaskType.F_COMPLETION_TIMESTAMP).le(deleteTasksClosedUpTo)
                .and().item(TaskType.F_PARENT).isNull()
                .build();
        List<PrismObject<TaskType>> obsoleteTasks =
                repositoryService.searchObjects(TaskType.class, obsoleteTasksQuery, null, result);

        LOGGER.debug("Found {} task tree(s) to be cleaned up", obsoleteTasks.size());

        boolean interrupted = false;
        int deleted = 0;
        int problems = 0;
        int subtasksProblems = 0;
        root: for (PrismObject<TaskType> rootTaskPrism : obsoleteTasks) {

            if (!executionTask.canRun()) {
                result.recordWarning("Interrupted");
                LOGGER.warn("Task cleanup was interrupted.");
                interrupted = true;
                break;
            }

            IterativeOperationStartInfo iterativeOperationStartInfo = new IterativeOperationStartInfo(
                    new IterationItemInformation(rootTaskPrism));
            iterativeOperationStartInfo.setSimpleCaller(true);
            Operation op = executionTask.recordIterativeOperationStart(iterativeOperationStartInfo);
            try {
                // get whole tree
                TaskQuartzImpl rootTask = taskInstantiator.createTaskInstance(rootTaskPrism, result);
                if (rootTask.isIndestructible()) {
                    LOGGER.trace("Not deleting {} as it is indestructible", rootTaskPrism);
                    op.skipped();
                    continue;
                }

                if (!selector.test(rootTaskPrism.asObjectable())) {
                    LOGGER.debug("Not deleting {} because it was rejected by the selector", rootTaskPrism);
                    op.skipped();
                    continue;
                }

                List<TaskQuartzImpl> taskTreeMembers = rootTask.listSubtasksDeeply(true, result);
                for (TaskQuartzImpl child : taskTreeMembers) {
                    if (child.isIndestructible()) {
                        LOGGER.trace("Not deleting {} as it has an indestructible child: {}", rootTask, child);
                        op.skipped();
                        continue root;
                    }
                    if (!selector.test(child.getRawTaskObject().asObjectable())) {
                        LOGGER.debug("Not deleting {} because the user has no authorization to delete one of the children: {}",
                                rootTask, child);
                        op.skipped();
                        continue root;
                    }
                }

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
                            subtasksProblems++;
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
            executionTask.incrementLegacyProgressAndStoreStatisticsIfTimePassed(result);
        }

        LOGGER.info("Task cleanup procedure " + (interrupted ? "was interrupted" : "finished")
                + ". Successfully deleted {} tasks; there were problems with deleting {} tasks.", deleted, problems);
        if (subtasksProblems > 0) {
            LOGGER.error(
                    "{} subtask(s) couldn't be deleted. Inspect that manually, otherwise they might reside in repo forever.",
                    subtasksProblems);
        }
        String suffix = interrupted ? " Interrupted." : "";
        if (problems == 0) {
            result.createSubresult(OP_STATISTICS)
                    .recordStatus(SUCCESS, "Successfully deleted " + deleted + " task(s)." + suffix);
        } else {
            result.createSubresult(OP_STATISTICS)
                    .recordPartialError("Successfully deleted " + deleted + " task(s), "
                            + "there was problems with deleting " + problems + " tasks." + suffix
                            + (subtasksProblems > 0 ?
                            (" " + subtasksProblems + " subtask(s) couldn't be deleted, please see the log.") :
                            ""));
        }
    }
}
