/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 *
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

/**
 * Synchronizes midPoint repository with Quartz job store.
 *
 * The mapping between tasks in repo and jobs in Quartz looks like this:
 *
 * 1) each task corresponds to a job; job name = task oid, job group = DEFAULT
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
    private RepositoryService repositoryService;

    public TaskSynchronizer(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
        this.repositoryService = taskManager.getRepositoryService();
    }

    /**
     * Checks for consistency between Quartz job store and midPoint repository.
     * In case of conflict, the latter is taken as authoritative source.
     *
     * (For RAM Job Store, running this method at startup effectively means that tasks from midPoint repo are imported into Quartz job store.)
     *
     */
    boolean synchronizeJobStores(OperationResult result) {

        Scheduler scheduler = taskManager.getQuartzScheduler();

        LOGGER.info("Synchronizing Quartz job store with midPoint repository.");

        PagingType paging = new PagingType();
        List<PrismObject<TaskType>> tasks;
        try {
            tasks = repositoryService.searchObjects(TaskType.class, QueryUtil.createAllObjectsQuery(), paging, result);
        } catch(Exception e) {
            LoggingUtils.logException(LOGGER, "Synchronization cannot be done, because tasks cannot be listed from the repository.", e);
            return false;
        }

        LOGGER.trace("There are {} task(s) in repository", tasks.size());

        // check consistency of tasks present in repo
        Set<String> oidsInRepo = new HashSet<String>();
        int processed = 0;
        int errors = 0;
        for (PrismObject<TaskType> taskPrism : tasks) {
            oidsInRepo.add(taskPrism.getOid());
            TaskQuartzImpl task = null;
            try {
                task = (TaskQuartzImpl) taskManager.getTask(taskPrism.getOid(), result);    // in order for the task to be "fresh"
                synchronizeTask(task);
                processed++;
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Task Manager cannot synchronize task {} due to schema exception.", e, taskPrism.getOid());
                errors++;
            } catch (Exception e) {		// FIXME: correct exception handling
                LoggingUtils.logException(LOGGER, "Task Manager cannot synchronize task {}", e, task);
                errors++;
            }
        }

        // remove non-existing tasks
        int removed = 0;
        Set<JobKey> jobs = null;
        try {
            jobs = new HashSet<JobKey>(scheduler.getJobKeys(jobGroupEquals(JobKey.DEFAULT_GROUP)));
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot list jobs from Quartz scheduler, skipping second part of synchronization procedure.", e);
        }

        if (jobs != null) {
            LOGGER.trace("There are {} job(s) in Quartz job store", jobs.size());
            for (JobKey job : jobs) {
                if (!oidsInRepo.contains(job.getName())) {
                    LOGGER.info("Task " + job.getName() + " is not in repository, removing from Quartz job store.");
                    try {
                        scheduler.deleteJob(job);
                        removed++;
                    } catch (SchedulerException e) {
                        LoggingUtils.logException(LOGGER, "Cannot remove job " + job.getName() + " from Quartz job store", e);
                        errors++;
                    }
                }
            }
        }

        LOGGER.info("Synchronization of midpoint and Quartz task store finished. "
                + processed + " task(s) existing in midPoint repository successfully processed. "
                + removed + " task(s) removed from Quartz job store. Processing of "
                + errors + " task(s) failed.");

        return true;
    }

    /**
     * Task should be refreshed when entering this method.
     */
    public void synchronizeTask(TaskQuartzImpl task) throws SchedulerException {

        LOGGER.trace("Synchronizing task {}", task);

        Scheduler scheduler = taskManager.getQuartzScheduler();
        String oid = task.getOid();

        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
        TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);

        if (!scheduler.checkExists(jobKey)) {
            LOGGER.trace(" - Quartz job does not exist for a task, adding it. Task = {}", task);
            scheduler.addJob(TaskQuartzImplUtil.createJobDetailForTask(task), false);
        }

        // WAITING and CLOSED tasks should have no triggers

        boolean triggerExists = scheduler.checkExists(triggerKey);
        if (task.getExecutionStatus() == TaskExecutionStatus.WAITING || task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
            if (triggerExists) {
                LOGGER.trace(" - removing Quartz trigger for WAITING/CLOSED task {}", task);
                scheduler.unscheduleJob(TriggerKey.triggerKey(oid));
            }
        } else if (task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
            // For SUSPENDED tasks, we do nothing.
            // 1) If a trigger is mistakenly alive, we simply let it be. JobExecutor will take care of it.
            // 2) If a trigger has wrong parameters, this will be corrected on task resume.

        } else if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {

            Trigger triggerToBe;
            try {
                triggerToBe = TaskQuartzImplUtil.createTriggerForTask(task);
            } catch (ParseException e) {
                LoggingUtils.logException(LOGGER, "Cannot create a trigger for a task {} because of a cron expression parsing exception", e, this);
                throw new SystemException("Cannot a trigger for a task because of a cron expression parsing exception", e);
            }

            // if the trigger should exist and it does not...
            if (!triggerExists) {
                LOGGER.trace(" - creating trigger for a RUNNABLE task {}", task);
                scheduler.scheduleJob(triggerToBe);
            } else {

                // we have to compare trigger parameters with the task's ones
                Trigger triggerAsIs = scheduler.getTrigger(triggerKey);

                if (TaskQuartzImplUtil.triggerDataMapsDiffer(triggerAsIs, triggerToBe)) {
                    LOGGER.trace(" - existing trigger has an incompatible parameters, recreating it; task = {}", task);
                    scheduler.rescheduleJob(triggerKey, triggerToBe);
                } else {
                    LOGGER.trace(" - existing trigger is OK, leaving it as is; task = {}", task);
                    Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
                    if (state == Trigger.TriggerState.PAUSED) {
                        LOGGER.trace(" - however, the trigger is paused, resuming it; task = {}", task);
                        scheduler.resumeTrigger(triggerKey);
                    }
                }

            }

        }
    }



}
