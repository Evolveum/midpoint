/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.quartz;

import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskMigrator;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronizes midPoint repository with Quartz job store.
 *
 * The mapping between tasks in repo and jobs in Quartz looks like this:
 *
 * 1) each task corresponds to a job; job name = task oid, job group = DEFAULT
 *
 *    TODO: should waiting/closed tasks have their jobs? At minimum, we do not create them if they do not exist
 *
 * 2) each task has 0, 1 or 2 triggers
 *    - if task is RUNNABLE, it has 1 or 2 triggers
 *      (one trigger is standard, with trigger name = task oid, trigger group = DEFAULT;
 *      second trigger is optional, used by Quartz when recovering the task)
 *    - if task is WAITING or CLOSED, it has 0 triggers
 *    - if task is SUSPENDED, it can have 0 to 2 triggers
 *      - 0 if the job was created when task was in SUSPENDED state
 *      - 1 or 2 if a RUNNABLE task was SUSPENDED (triggers were kept in order to be un-paused when task is resumed)
 */
@Component
public class TaskSynchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(TaskSynchronizer.class);

    private static final String OP_SYNCHRONIZE_TASK = TaskSynchronizer.class.getName() + ".synchronizeTask";

    @Autowired private TaskManagerQuartzImpl taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private TaskMigrator taskMigrator;

    /**
     * Checks for consistency between Quartz job store and midPoint repository.
     * In case of conflict, the latter is taken as authoritative source.
     *
     * (For RAM Job Store, running this method at startup effectively means that tasks from midPoint repo are imported
     * into Quartz job store.)
     *
     */
    public boolean synchronizeJobStores(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".synchronizeJobStores");

        LOGGER.info("Synchronizing Quartz job store with midPoint repository.");

        // Init the set with all jobs in Quartz. Jobs which are present also in midPoint will be removed from this set
        // during midPoint tasks synchronization bellow.
        Set<JobKey> jobsNotInMidPointRepo = getQuartzJobs(result);
        SynchronizationStats stats = new SynchronizationStats();
        ResultHandler<TaskType> foundTaskHandler = taskSynchronizationHandler(stats,
                knownJobsRemover(jobsNotInMidPointRepo));
        boolean synchronizationIsSuccessful = synchronizeTasksFromMidPointStore(foundTaskHandler, result);
        if (!synchronizationIsSuccessful) {
            return false;
        }

        // remove non-existing tasks
        if (!jobsNotInMidPointRepo.isEmpty()) {
            removeJobsFromQuartz(jobsNotInMidPointRepo, stats, result);
        }

        String resultMessage = stats.toResultMessage();

        LOGGER.info(resultMessage);
        if (result.isUnknown()) {
            result.recordStatus(OperationResultStatus.SUCCESS, resultMessage);
        }

        return true;
    }

    /**
     * Task should be refreshed when entering this method.
     *
     * @return true if task info in Quartz was updated
     */
    public boolean synchronizeTask(TaskQuartzImpl task, OperationResult parentResult) {

        if (!task.isPersistent()) {
            return false; // transient tasks are not scheduled via Quartz!
        }

        boolean changed = false;
        StringBuilder message = new StringBuilder();

        OperationResult result = parentResult.createSubresult(OP_SYNCHRONIZE_TASK);
        result.addArbitraryObjectAsParam("task", task);
        try {

            taskMigrator.migrateIfNeeded(task, result);

            LOGGER.trace("Synchronizing task {}; isRecreateQuartzTrigger = {}", task, task.isRecreateQuartzTrigger());

            Scheduler scheduler = localScheduler.getQuartzScheduler();
            String oid = task.getOid();

            JobKey jobKey = QuartzUtil.createJobKeyForTask(task);
            TriggerKey standardTriggerKey = QuartzUtil.createTriggerKeyForTask(task);

            TaskSchedulingStateType schedulingState = task.getSchedulingState();
            boolean waitingOrClosedOrSuspended = schedulingState == TaskSchedulingStateType.WAITING
                    || schedulingState == TaskSchedulingStateType.CLOSED
                    || schedulingState == TaskSchedulingStateType.SUSPENDED;

            // Add task to quartz if it is not there yet and it is in READY state
            if (!scheduler.checkExists(jobKey) && !waitingOrClosedOrSuspended) {
                String m1 = "Quartz job does not exist for a task, adding it. Task = " + task;
                message.append("[").append(m1).append("] ");
                LOGGER.trace(" - {}", m1);
                scheduler.addJob(QuartzUtil.createJobDetailForTask(task), false);
                changed = true;
            }

            // Remove task triggers if task is closed, suspended or waiting
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (waitingOrClosedOrSuspended) {
                for (Trigger trigger : triggers) {
                    // CLOSED tasks should have no triggers; SUSPENDED and WAITING tasks should have no extra triggers
                    if (schedulingState == TaskSchedulingStateType.CLOSED || !trigger.getKey().equals(standardTriggerKey)) {
                        String m1 = "Removing Quartz trigger " + trigger.getKey() + " for WAITING/CLOSED/SUSPENDED task " + task;
                        message.append("[").append(m1).append("] ");
                        LOGGER.trace(" - {}", m1);
                        scheduler.unscheduleJob(trigger.getKey());
                        changed = true;
                    } else {
                        // For SUSPENDED/WAITING tasks, we keep the standard trigger untouched. We want to preserve original
                        // scheduled time. (This might or might not be what the user wants ... but it has been so for so
                        // many years, so let's not change it now.)
                        //
                        // It's harmless to keep the standard trigger, because:
                        // 1) If a trigger is mistakenly alive, JobExecutor will take care of it.
                        // 2) If a trigger has wrong parameters, this will be corrected on task resume/unpause.
                    }
                }
            // Synchronize task triggers if task can be triggered
            } else if (schedulingState == TaskSchedulingStateType.READY) {
                // Create trigger based on current task state
                Trigger triggerToBe;
                try {
                    triggerToBe = QuartzUtil.createTriggerForTask(task);
                } catch (ParseException e) {
                    String message2 = "Cannot create a trigger for a task " + task + " because of a cron expression "
                            + "parsing exception";
                    LoggingUtils.logUnexpectedException(LOGGER, message2, e);
                    result.recordFatalError(message2, e);
                    // TODO: implement error handling correctly
                    throw new SystemException("Cannot a trigger for a task because of a cron expression parsing exception", e);
                }

                // Remove trigger, if task should not be triggered (triggerToBe is null)
                boolean standardTriggerExists = triggers.stream().anyMatch(t -> t.getKey().equals(standardTriggerKey));
                if (triggerToBe == null) {
                    // Trigger will be null in cases when task is not in ready state, or it is not persistent, or it
                    // is recurring, but does not have any schedule (on-demand task)
                    if (standardTriggerExists) {
                        // TODO what about non-standard triggers?
                        // These may be legal here (e.g. for a manually-run recurring task waiting to get a chance to run)
                        String m1 = "Removing standard Quartz trigger for RUNNABLE task that should not have it; task = " + task;
                        message.append("[").append(m1).append("] ");
                        LOGGER.trace(" - " + m1);
                        scheduler.unscheduleJob(standardTriggerKey);
                        changed = true;
                    }

                // Synchronize trigger with existing triggers in quartz
                } else {
                    // Add trigger to quartz if it does not exist
                    if (!standardTriggerExists) {
                        String m1 = "Creating standard trigger for a RUNNABLE task " + task;
                        LOGGER.trace(" - " + m1);
                        message.append("[").append(m1).append("] ");
                        scheduler.scheduleJob(triggerToBe);
                        changed = true;
                    } else {
                        // Replace old quartz trigger, if it is different from the current one
                        Trigger triggerAsIs = scheduler.getTrigger(standardTriggerKey);
                        if (task.isRecreateQuartzTrigger() || QuartzUtil.triggersDiffer(triggerAsIs, triggerToBe)) {
                            String m1 = "Existing trigger has incompatible parameters or was explicitly requested to be recreated; recreating it. Task = " + task;
                            LOGGER.trace(" - " + m1);
                            message.append("[").append(m1).append("] ");
                            scheduler.rescheduleJob(standardTriggerKey, triggerToBe);
                            changed = true;

                        // Resume quartz trigger if it is paused
                        } else {
                            String m1 = "Existing trigger is OK, leaving it as is; task = " + task;
                            LOGGER.trace(" - " + m1);
                            message.append("[").append(m1).append("] ");
                            Trigger.TriggerState state = scheduler.getTriggerState(standardTriggerKey);
                            if (state == Trigger.TriggerState.PAUSED) {
                                String m2 = "However, the trigger is paused, resuming it; task = " + task;
                                LOGGER.trace(" - " + m2);
                                message.append("[").append(m2).append("] ");
                                scheduler.resumeTrigger(standardTriggerKey);
                                changed = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            String message2 = "Cannot synchronize repository/Quartz Job Store information for task " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message2, e);
            result.recordFatalError(message2, e);
        } finally {
            if (result.isUnknown()) {
                result.computeStatus();
                result.recordStatus(result.getStatus(), message.toString());
            }
        }

        LOGGER.trace("synchronizeTask finishing (changed: {}) for {}", changed, task);
        return changed;
    }

    private boolean synchronizeTasksFromMidPointStore(ResultHandler<TaskType> synchronizationHandler,
            OperationResult result) {
        try {
            // RepositoryService#countObject method throws checked exception, so we can not simply use logger's fluent
            // api with argument supplier.
            if (LOGGER.isTraceEnabled()) {
                int tasksCount = this.repositoryService.countObjects(TaskType.class, null, null, result);
                LOGGER.trace("There are {} task(s) in repository", tasksCount);
            }

            this.repositoryService.searchObjectsIterative(TaskType.class, null, synchronizationHandler, null, true,
                    result);
            return true;
        } catch(SchemaException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Synchronization cannot be done, because tasks cannot be "
                    + "listed from the repository.", e);
            return false;
        }
    }

    private Set<JobKey> getQuartzJobs(OperationResult result) {
        try {
            Set<JobKey> jobsInQuartz = this.localScheduler.getQuartzScheduler()
                    .getJobKeys(jobGroupEquals(JobKey.DEFAULT_GROUP));
            LOGGER.trace("There are {} job(s) in Quartz job store", jobsInQuartz.size());
            return jobsInQuartz;
        } catch (SchedulerException e) {
            String message = "Cannot list jobs from Quartz scheduler, skipping second part of synchronization procedure.";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordPartialError(message, e);
            return Collections.emptySet();
        }
    }

    private ResultHandler<TaskType> taskSynchronizationHandler(SynchronizationStats stats,
            Consumer<String> taskOidConsumer) {
        return (prismTask, result)-> {
            if (prismTask.getOid() == null) {
                LOGGER.error("Skipping task with no OID: {}", prismTask);
                stats.incErrors();
                return true;
            }

            taskOidConsumer.accept(prismTask.getOid());

            try {
                // Even though we process tasks in small batches (thus they should be current), fetch current task state
                // again. Just to be safe, because task synchronization can take some time depending on the task size.
                TaskQuartzImpl task = taskManager.getTaskPlain(prismTask.getOid(), result);
                if (synchronizeTask(task, result)) {
                    stats.incChanged();
                }
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Task Manager cannot synchronize task {} due to schema "
                        + "exception.", e, prismTask.getOid());
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task Manager cannot synchronize task {} because it does not exist",
                        e, prismTask.getOid());
            }

            if (result.getLastSubresultStatus() == OperationResultStatus.SUCCESS) {
                stats.incSuccessfullyProcessed();
            } else {
                stats.incErrors();
            }
            return true;
        };
    }

    private void removeJobsFromQuartz(Set<JobKey> jobsNotInMidPointRepo, SynchronizationStats stats,
            OperationResult result) {

        final Scheduler scheduler = this.localScheduler.getQuartzScheduler();
        for (JobKey job : jobsNotInMidPointRepo) {
            LOGGER.info("Task {} is not in repository, removing from Quartz job store.", job.getName());
            try {
                scheduler.deleteJob(job);
                stats.incRemoved();
            } catch (SchedulerException e) {
                String message = "Cannot remove job " + job.getName() + " from Quartz job store";
                LoggingUtils.logUnexpectedException(LOGGER, message, e);
                result.createSubresult("deleteQuartzJob").recordPartialError(message, e);
                stats.incErrors();
            }
        }
    }

    private Consumer<String> knownJobsRemover(Set<JobKey> jobsFromQuartz) {
        return oid -> {
            jobsFromQuartz.remove(QuartzUtil.createJobKeyForTaskOid(oid));
        };
    }

    private static class SynchronizationStats {
        private int processed;
        private int changed;
        private int removed;
        private int errors;

        void incSuccessfullyProcessed() {
            this.processed++;
        }

        void incChanged() {
            this.changed++;
        }

        void incRemoved() {
            this.removed++;
        }

        void incErrors() {
            this.errors++;
        }

        String toResultMessage() {
            return """
                    Synchronization of midpoint and Quartz tasks store finished. Processing of %d task(s) existing in \
                    midPoint repository has been successful, while processing of %d task(s) has failed. %d task(s) has \
                    been updated and %d task(s) has been removed from Quartz job store, because they are not present \
                    in midPoint repository."
                    """.formatted(this.processed, this.changed, this.removed, this.errors);
        }
    }
}
