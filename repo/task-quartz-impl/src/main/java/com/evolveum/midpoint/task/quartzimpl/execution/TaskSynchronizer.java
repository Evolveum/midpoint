/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

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
 *
 *
 * @author Pavol Mederly
 */

public class TaskSynchronizer {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskSynchronizer.class);

    private TaskManagerQuartzImpl taskManager;

    public TaskSynchronizer(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Checks for consistency between Quartz job store and midPoint repository.
     * In case of conflict, the latter is taken as authoritative source.
     *
     * (For RAM Job Store, running this method at startup effectively means that tasks from midPoint repo are imported into Quartz job store.)
     *
     */
    boolean synchronizeJobStores(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".synchronizeJobStores");

        Scheduler scheduler = taskManager.getExecutionManager().getQuartzScheduler();

        LOGGER.info("Synchronizing Quartz job store with midPoint repository.");

        List<PrismObject<TaskType>> tasks;
        try {
            tasks = getRepositoryService().searchObjects(TaskType.class, new ObjectQuery(), null, result);
        } catch(SchemaException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Synchronization cannot be done, because tasks cannot be listed from the repository.", e);
            return false;
        }

        LOGGER.trace("There are {} task(s) in repository", tasks.size());

        // check consistency of tasks present in repo
        Set<String> oidsInRepo = new HashSet<>();
        int processed = 0;
        int changed = 0;
        int errors = 0;
        for (PrismObject<TaskType> taskPrism : tasks) {
            if (taskPrism.getOid() == null) {
                LOGGER.error("Skipping task with no OID: {}", taskPrism);
                errors++;
                continue;
            }
            oidsInRepo.add(taskPrism.getOid());
            TaskQuartzImpl task;
            try {
                task = (TaskQuartzImpl) taskManager.getTask(taskPrism.getOid(), result);    // in order for the task to be "fresh"
                if (synchronizeTask(task, result)) {
                    changed++;      // todo are we sure that we increment this counter only for successfully processed tasks? we hope so :)
                }
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Task Manager cannot synchronize task {} due to schema exception.", e, taskPrism.getOid());
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task Manager cannot synchronize task {} because it does not exist", e, taskPrism.getOid());
            }

            if (result.getLastSubresultStatus() == OperationResultStatus.SUCCESS) {
                processed++;
            } else {
                errors++;
            }
        }

        // remove non-existing tasks
        int removed = 0;
        Set<JobKey> jobs = null;
        try {
            jobs = new HashSet<>(scheduler.getJobKeys(jobGroupEquals(JobKey.DEFAULT_GROUP)));
        } catch (SchedulerException e) {
            String message = "Cannot list jobs from Quartz scheduler, skipping second part of synchronization procedure.";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordPartialError(message, e);
        }

        if (jobs != null) {
            LOGGER.trace("There are {} job(s) in Quartz job store", jobs.size());
            for (JobKey job : jobs) {
                if (!oidsInRepo.contains(job.getName()) && !RemoteNodesManager.STARTER_JOB_KEY.equals(job)) {
                    LOGGER.info("Task " + job.getName() + " is not in repository, removing from Quartz job store.");
                    try {
                        scheduler.deleteJob(job);
                        removed++;
                    } catch (SchedulerException e) {
                        String message = "Cannot remove job " + job.getName() + " from Quartz job store";
                        LoggingUtils.logUnexpectedException(LOGGER, message, e);
                        result.createSubresult("deleteQuartzJob").recordPartialError(message, e);
                        errors++;
                    }
                }
            }
        }

        String resultMessage = "Synchronization of midpoint and Quartz task store finished. "
                + processed + " task(s) existing in midPoint repository successfully processed, resulting in " + changed + " updated Quartz job(s). "
                + removed + " task(s) removed from Quartz job store. Processing of "
                + errors + " task(s) failed.";

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
            return false;               // transient tasks are not scheduled via Quartz!
        }

        boolean changed = false;
        String message = "";

        OperationResult result = parentResult.createSubresult(TaskSynchronizer.class.getName() + ".synchronizeTask");
        result.addArbitraryObjectAsParam("task", task);

        try {

            LOGGER.trace("Synchronizing task {}; isRecreateQuartzTrigger = {}", task, task.isRecreateQuartzTrigger());

            Scheduler scheduler = taskManager.getExecutionManager().getQuartzScheduler();
            String oid = task.getOid();

            JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
            TriggerKey standardTriggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);

            boolean waitingOrClosedOrSuspended = task.getExecutionStatus() == TaskExecutionStatus.WAITING
					|| task.getExecutionStatus() == TaskExecutionStatus.CLOSED
					|| task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED;

            if (!scheduler.checkExists(jobKey) && !waitingOrClosedOrSuspended) {
                String m1 = "Quartz job does not exist for a task, adding it. Task = " + task;
                message += "[" + m1 + "] ";
                LOGGER.trace(" - " + m1);
                scheduler.addJob(TaskQuartzImplUtil.createJobDetailForTask(task), false);
                changed = true;
            }

            // WAITING and CLOSED tasks should have no triggers; SUSPENDED tasks should have no extra triggers

            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            boolean standardTriggerExists = triggers.stream().anyMatch(t -> t.getKey().equals(standardTriggerKey));
            if (waitingOrClosedOrSuspended) {
				for (Trigger trigger : triggers) {
					if (task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED || !trigger.getKey().equals(standardTriggerKey)) {
						String m1 = "Removing Quartz trigger " + trigger.getKey() + " for WAITING/CLOSED/SUSPENDED task " + task;
						message += "[" + m1 + "] ";
						LOGGER.trace(" - " + m1);
						scheduler.unscheduleJob(trigger.getKey());
						changed = true;
					} else {
						// For SUSPENDED tasks, we keep the standard trigger untouched. We want to preserve original
						// scheduled time. (This might or might not be what the user wants ... but it has been so for so
						// many years, so let's not change it now.)
						//
						// It's harmless to keep the standard trigger, because:
						// 1) If a trigger is mistakenly alive, JobExecutor will take care of it.
						// 2) If a trigger has wrong parameters, this will be corrected on task resume.
					}
				}
            } else if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                Trigger triggerToBe;
                try {
                    triggerToBe = TaskQuartzImplUtil.createTriggerForTask(task);
                } catch (ParseException e) {
                    String message2 = "Cannot create a trigger for a task " + this + " because of a cron expression parsing exception";
                    LoggingUtils.logUnexpectedException(LOGGER, message2, e);
                    result.recordFatalError(message2, e);
                    // TODO: implement error handling correctly
                    throw new SystemException("Cannot a trigger for a task because of a cron expression parsing exception", e);
                }
                if (triggerToBe == null) {
                    if (standardTriggerExists) {
                    	// TODO what about non-standard triggers?
						// These may be legal here (e.g. for a manually-run recurring task waiting to get a chance to run)
                        String m1 = "Removing standard Quartz trigger for RUNNABLE task that should not have it; task = " + task;
                        message += "[" + m1 + "] ";
                        LOGGER.trace(" - " + m1);
                        scheduler.unscheduleJob(TriggerKey.triggerKey(oid));
                        changed = true;
                    }
                } else {
                    // if the trigger should exist and it does not...
                    if (!standardTriggerExists) {
                        String m1 = "Creating standard trigger for a RUNNABLE task " + task;
                        LOGGER.trace(" - " + m1);
                        message += "[" + m1 + "] ";
                        scheduler.scheduleJob(triggerToBe);
                        changed = true;
                    } else {
                        // we have to compare trigger parameters with the task's ones
                        Trigger triggerAsIs = scheduler.getTrigger(standardTriggerKey);
						if (task.isRecreateQuartzTrigger() || TaskQuartzImplUtil.triggerDataMapsDiffer(triggerAsIs, triggerToBe)) {
                            String m1 = "Existing trigger has incompatible parameters or was explicitly requested to be recreated; recreating it. Task = " + task;
                            LOGGER.trace(" - " + m1);
                            message += "[" + m1 + "] ";
                            scheduler.rescheduleJob(standardTriggerKey, triggerToBe);
                            changed = true;
                        } else {
                            String m1 = "Existing trigger is OK, leaving it as is; task = " + task;
                            LOGGER.trace(" - " + m1);
                            message += "[" + m1 + "] ";
                            Trigger.TriggerState state = scheduler.getTriggerState(standardTriggerKey);
                            if (state == Trigger.TriggerState.PAUSED) {
                                String m2 = "However, the trigger is paused, resuming it; task = " + task;
                                LOGGER.trace(" - " + m2);
                                message += "[" + m2 + "] ";
                                scheduler.resumeTrigger(standardTriggerKey);
                                changed = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {         // todo make this more specific (originally here was SchedulerException but e.g. for negative repeat intervals here we get unchecked IllegalArgumentException...)
            String message2 = "Cannot synchronize repository/Quartz Job Store information for task " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message2, e);
            result.recordFatalError(message2, e);
        }

        if (result.isUnknown()) {
            result.computeStatus();
            result.recordStatus(result.getStatus(), message);
        }

        return changed;
    }

    private RepositoryService getRepositoryService() {
        return taskManager.getRepositoryService();
    }


}
