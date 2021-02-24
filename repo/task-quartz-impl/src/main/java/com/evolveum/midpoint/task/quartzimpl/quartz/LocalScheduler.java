/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.quartz;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.LocalNodeState;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.run.JobExecutor;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskGroupExecutionLimitationType;

/**
 * Interfaces with local Quartz scheduler.
 * Concerned mainly with stopping threads and querying their state.
 *
 * - SHOULD NOT update task objects themselves.
 * - Even it SHOULD NOT query task state. All of this has to be done by callers.
 */
@Component
public class LocalScheduler {

    private static final Trace LOGGER = TraceManager.getTrace(LocalScheduler.class);

    private static final String CLASS_DOT = LocalScheduler.class.getName() + ".";
    private static final String OP_UNSCHEDULE_TASK = CLASS_DOT + "unscheduleTask";
    private static final String OP_PAUSE_TASK_JOB = CLASS_DOT + "pauseTaskJob";
    private static final String OP_DELETE_TASK_FROM_QUARTZ = CLASS_DOT + "deleteTaskFromQuartz";

    @Autowired private LocalNodeState localNodeState;
    @Autowired private QuartzInitializationHelper quartzInitializationHelper;

    /**
     * Local Quartz scheduler instance. Does not need to be synchronized, because it is set only once on initialization.
     */
    private Scheduler quartzScheduler;

    public void setQuartzScheduler(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    public Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    //region Scheduler state management
    /**
     * Prepares Quartz scheduler but does not start it.
     * (That is done during post initialization.)
     */
    public void initializeScheduler() throws TaskManagerInitializationException {
        quartzInitializationHelper.initializeScheduler();
    }

    /**
     * Starts the (previously initialized) Quartz scheduler.
     */
    public void startScheduler() {
        // TODO is this correct place to check this?
        if (localNodeState.isInErrorState()) {
            throw new SystemException("Cannot start the scheduler, because Task Manager is in error "
                    + "state (" + localNodeState.getErrorState() + ")");
        }

        try {
            LOGGER.info("Starting the Quartz scheduler");
            quartzScheduler.start();
            LOGGER.debug("Quartz scheduler started.");
        } catch (SchedulerException e) {
            throw new SystemException("Cannot (re)start Quartz scheduler.", e);
        }
    }

    /**
     * Returns true if the scheduler is active i.e. it scheduling the tasks.
     *
     * Intentionally does not propagate internal exceptions.
     */
    public boolean isRunningChecked() {
        try {
            return quartzScheduler != null && quartzScheduler.isStarted() &&
                    !quartzScheduler.isInStandbyMode() &&
                    !quartzScheduler.isShutdown();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot determine the state of the Quartz scheduler", e);
            return false;
        }
    }

    /**
     * Returns true if the scheduler is active i.e. it scheduling the tasks.
     */
    public boolean isRunning() {
        if (quartzScheduler == null) {
            return false;
        }
        try {
            return quartzScheduler.isStarted() && !quartzScheduler.isInStandbyMode() && !quartzScheduler.isShutdown();
        } catch (SchedulerException e) {
            throw new SystemException("Couldn't determine Quartz scheduler state: " + e.getMessage(), e);
        }
    }

    public void stopScheduler(OperationResult result) {
        pauseScheduler(result);
    }

    /**
     * Intentionally does not propagate internal exceptions.
     */
    public void pauseScheduler(OperationResult result) {
        LOGGER.info("Putting Quartz scheduler into standby mode");
        try {
            quartzScheduler.standby();
        } catch (SchedulerException e1) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't put local Quartz scheduler into standby mode", e1);
            result.recordFatalError("Couldn't put local Quartz scheduler into standby mode", e1);
        }
    }

    /**
     * Intentionally does not propagate internal exceptions.
     */
    public void shutdownScheduler() {
        LOGGER.info("Shutting down Quartz scheduler");
        try {
            if (quartzScheduler != null && !quartzScheduler.isShutdown()) {
                quartzScheduler.shutdown(true);
            }
            LOGGER.info("Quartz scheduler was shut down");
        } catch (SchedulerException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not shutdown Quartz scheduler, continuing", e);
        }
    }
    //endregion

    //region Execution limitations
    public void setLocalExecutionLimitations(TaskExecutionLimitationsType limitations) {
        try {
            Map<String, Integer> newLimits = new HashMap<>();
            if (limitations != null) {
                for (TaskGroupExecutionLimitationType limit : limitations.getGroupLimitation()) {
                    newLimits.put(MiscUtil.nullIfEmpty(limit.getGroupName()), limit.getLimit());
                }
            } else {
                // no limits -> everything is allowed
            }
            Map<String, Integer> oldLimits = quartzScheduler.getExecutionLimits(); // just for the logging purposes
            quartzScheduler.setExecutionLimits(newLimits);
            if (oldLimits == null || !new HashMap<>(oldLimits).equals(newLimits)) {
                LOGGER.info("Quartz scheduler execution limits set to: {} (were: {})", newLimits, oldLimits);
            }
        } catch (SchedulerException e) {
            // should never occur, as local scheduler shouldn't throw such exceptions
            throw new SystemException("Couldn't set local Quartz scheduler execution capabilities: " + e.getMessage(), e);
        }
    }
    //endregion

    //region Stopping the tasks
    /**
     * Interrupts a task in a standard way, i.e. calls {@link JobExecutor#interrupt()} method (via Quartz).
     * That method may or may not call {@link Thread#interrupt()} immediately; depending on the configuration.
     */
    public void stopLocalTaskRunInStandardWay(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(LocalScheduler.class.getName() + ".stopLocalTaskRun");
        result.addParam("task", oid);
        try {
            LOGGER.info("Stopping local task {} run", oid);
            quartzScheduler.interrupt(QuartzUtil.createJobKeyForTaskOid(oid));
        } catch (UnableToInterruptJobException e) {
            String message = "Unable to interrupt the task " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
            // However we continue in this case
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Calls {@link Thread#interrupt()} on a local thread hosting task with a given OID.
     */
    public void stopLocalTaskRunByInterrupt(String oid) {
        LOGGER.trace("Trying to find and interrupt a local execution thread for task {} (if it exists).", oid);
        try {
            List<JobExecutionContext> jecs = quartzScheduler.getCurrentlyExecutingJobs();
            for (JobExecutionContext jec : jecs) {
                String oid1 = jec.getJobDetail().getKey().getName();
                if (oid.equals(oid1)) {
                    Job job = jec.getJobInstance();
                    if (job instanceof JobExecutor) {
                        JobExecutor jobExecutor = (JobExecutor) job;
                        jobExecutor.sendThreadInterrupt();
                    }
                    break;
                }
            }
        } catch (SchedulerException e1) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot find the currently executing job for the task {}", e1, oid);
            // Ignoring the exception
        }
    }
    //endregion

    //region Querying the tasks/jobs
    public boolean isTaskThreadActiveLocally(String oid) {
        try {
            for (JobExecutionContext jec : quartzScheduler.getCurrentlyExecutingJobs()) {
                if (oid.equals(jec.getJobDetail().getKey().getName())) {
                    return true;
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get the list of currently executing jobs", e);
            return false;
        }
        return false;
    }

    public Set<String> getLocallyRunningTasksOids(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(LocalScheduler.class.getName() + ".getLocallyRunningTasksOids");
        try {
            List<JobExecutionContext> jobs = quartzScheduler.getCurrentlyExecutingJobs();
            Set<String> oids = jobs.stream().map(ec -> ec.getJobDetail().getKey().getName()).collect(Collectors.toSet());
            result.recordSuccess();
            return oids;
        } catch (Throwable t) {
            String message = "Cannot get the list of currently executing jobs on local node.";
            result.recordFatalError(message, t);
            LoggingUtils.logUnexpectedException(LOGGER, message, t);
            return Collections.emptySet(); // todo or throw an exception?
        }
    }

    @Nullable
    public Thread getLocalTaskThread(@NotNull String oid) {
        try {
            for (JobExecutionContext jec : quartzScheduler.getCurrentlyExecutingJobs()) {
                if (oid.equals(jec.getJobDetail().getKey().getName())) {
                    Job job = jec.getJobInstance();
                    if (job instanceof JobExecutor) {
                        return ((JobExecutor) job).getExecutingThread();
                    }
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get the list of currently executing jobs", e);
        }
        return null;
    }

    public NextStartTimes getNextStartTimes(@NotNull String oid, boolean retrieveNextRunStartTime,
            boolean retrieveRetryTime, OperationResult result) {
        try {
            if (retrieveNextRunStartTime && !retrieveRetryTime) {
                Trigger standardTrigger = quartzScheduler.getTrigger(QuartzUtil.createTriggerKeyForTaskOid(oid));
                result.recordSuccess();
                return new NextStartTimes(standardTrigger, null);
            } else if (retrieveNextRunStartTime || retrieveRetryTime) {
                List<? extends Trigger> triggers = quartzScheduler
                        .getTriggersOfJob(QuartzUtil.createJobKeyForTaskOid(oid));
                Trigger standardTrigger = null;
                Trigger nextRetryTrigger = null;
                for (Trigger trigger : triggers) {
                    if (oid.equals(trigger.getKey().getName())) {
                        standardTrigger = trigger;
                    } else {
                        if (willOccur(trigger) && (nextRetryTrigger == null || isBefore(trigger, nextRetryTrigger))) {
                            nextRetryTrigger = trigger;
                        }
                    }
                }
                result.recordSuccess();
                return new NextStartTimes(
                        retrieveNextRunStartTime ? standardTrigger : null,
                        nextRetryTrigger);        // retrieveRetryTime is always true here
            } else {
                return new NextStartTimes(null, null);        // shouldn't occur
            }
        } catch (SchedulerException e) {
            String message = "Cannot determine next start times for task with OID " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
            return null;
        }
    }

    // null means "never"
    private boolean isBefore(Trigger t1, Trigger t2) {
        Date date1 = t1.getNextFireTime();
        Date date2 = t2.getNextFireTime();
        return date1 != null
                && (date2 == null || date1.getTime() < date2.getTime());
    }

    private boolean willOccur(Trigger t) {
        return t.getNextFireTime() != null && t.getNextFireTime().getTime() >= System.currentTimeMillis();
    }
    //endregion

    //region Scheduling and unscheduling tasks
    public void unscheduleTask(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_UNSCHEDULE_TASK)
                .addArbitraryObjectAsParam("task", task)
                .build();
        JobKey jobKey = QuartzUtil.createJobKeyForTask(task);
        try {
            for (Trigger trigger : quartzScheduler.getTriggersOfJob(jobKey)) {
                quartzScheduler.unscheduleJob(trigger.getKey());
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot unschedule task {}", e, task);
            result.recordFatalError("Cannot unschedule task " + task, e);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Removes task from quartz. On error, creates a subresult in parent OperationResult. (On success, does nothing to keep ORs from becoming huge.)
     *
     * @param oid Task OID
     */
    public void deleteTaskFromQuartz(String oid, boolean checkIfExists, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_DELETE_TASK_FROM_QUARTZ)
                .setMinor()
                .addParam("oid", oid)
                .addParam("checkIfExists", checkIfExists)
                .build();
        try {
            LOGGER.trace("Going to delete task from quartz: oid={}, checkIfExists={}", oid, checkIfExists);
            JobKey jobKey = QuartzUtil.createJobKeyForTaskOid(oid);
            if (checkIfExists) {
                if (!quartzScheduler.checkExists(jobKey)) {
                    result.recordNotApplicable();
                    return;
                }
            }
            quartzScheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            String message = "Couldn't delete task " + oid + " from Quartz job store";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
        } catch (Throwable t) {
            result.recordFatalError(t);
            // Deletion is a process that needs to continue
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public void addTriggerNowForTask(Task task, OperationResult result) {
        Trigger now = QuartzUtil.createTriggerNowForTask(task);
        try {
            quartzScheduler.scheduleJob(now);
        } catch (SchedulerException e) {
            String message = "Task " + task + " cannot be scheduled: " + e.getMessage();
            result.recordFatalError(message, e);
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
        }
    }

    // TODO reconsider throwing SchedulerException
    public void addJobIfNeeded(TaskQuartzImpl task) throws SchedulerException {
        if (!quartzScheduler.checkExists(QuartzUtil.createJobKeyForTask(task))) {
            quartzScheduler.addJob(QuartzUtil.createJobDetailForTask(task), false);
        }
    }

    public void pauseTaskJob(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_PAUSE_TASK_JOB);
        try {
            JobKey jobKey = QuartzUtil.createJobKeyForTask(task);
            TriggerKey standardTriggerKey = QuartzUtil.createTriggerKeyForTask(task);
            for (Trigger trigger : quartzScheduler.getTriggersOfJob(jobKey)) {
                if (standardTriggerKey.equals(trigger.getKey())) {
                    LOGGER.trace("Suspending {}: pausing standard trigger {}", task, trigger);
                    quartzScheduler.pauseTrigger(trigger.getKey());
                } else {
                    LOGGER.trace("Suspending {}: deleting non-standard trigger {}", task, trigger);
                    quartzScheduler.unscheduleJob(trigger.getKey());
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot pause job for task {}", e, task);
            result.recordFatalError("Cannot pause job for task " + task, e);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // TODO what to do with the SchedulerException?
    public void rescheduleLater(RunningTaskQuartzImpl task, long startAt) throws SchedulerException {
        Trigger trigger = QuartzUtil.createTriggerForTask(task, startAt);
        quartzScheduler.scheduleJob(trigger);
    }
    //endregion
}
