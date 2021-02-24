/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.QuartzUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;

import java.util.Collections;
import java.util.List;

/**
 * TODO what is this for?!
 */
public class JobStarter implements Job {

    private static final Trace LOGGER = TraceManager.getTrace(JobStarter.class);

    private static final String REDIRECT_FLAG = "redirected";
    public static final String TASK_OID = "oid";

    private static final int WAIT_MINIMUM = 1000;
    private static final int WAIT_MAXIMUM = 3000;

    private static TaskManagerQuartzImpl taskManager;
    /*
     * Ugly hack - this class is instantiated not by Spring but explicitly by Quartz.
     */
    public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl tmqi) {
        taskManager = tmqi;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String oid = (String) context.getMergedJobDataMap().get(TASK_OID);
        LOGGER.trace("Requested starting Quartz job for task {}", oid);
        if (oid == null) {
            return; // just for safety
        }

        int wait = WAIT_MINIMUM + ((int) (Math.random() * (WAIT_MAXIMUM-WAIT_MINIMUM)));
        LOGGER.trace("Waiting for {} milliseconds", wait);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted wait", e);
        }

        try {
            deleteRedirectTriggers(oid);
            Scheduler scheduler = taskManager.getBeans().localScheduler.getQuartzScheduler();
            scheduler.triggerJob(QuartzUtil.createJobKeyForTaskOid(oid),
                    new JobDataMap(Collections.singletonMap(REDIRECT_FLAG, "")));
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't trigger task {}", e, oid);
        }
    }

    private void deleteRedirectTriggers(@NotNull String oid) throws SchedulerException {
        LOGGER.trace("Going to remove obsolete redirect triggers");
        Scheduler scheduler = taskManager.getBeans().localScheduler.getQuartzScheduler();
        List<? extends Trigger> existingTriggers = scheduler.getTriggersOfJob(QuartzUtil.createJobKeyForTaskOid(oid));
        for (Trigger trigger : existingTriggers) {
            if (trigger.getJobDataMap().containsKey(REDIRECT_FLAG)) {
                LOGGER.trace("Removing obsolete redirect trigger {}", trigger);
                scheduler.unscheduleJob(trigger.getKey());
            }
        }
    }
}
