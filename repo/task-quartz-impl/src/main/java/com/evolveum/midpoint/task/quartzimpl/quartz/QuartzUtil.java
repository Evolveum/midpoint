/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.quartz;

import static org.quartz.CronScheduleBuilder.cronScheduleNonvalidatedExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

import com.evolveum.midpoint.task.quartzimpl.run.JobExecutor;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.quartz.*;

import com.evolveum.midpoint.task.api.Task;

import javax.xml.datatype.XMLGregorianCalendar;

public class QuartzUtil {

    private static final Trace LOGGER = TraceManager.getTrace(QuartzUtil.class);
    private static final String KEY_LOOSELY_BOUND_RECURRENT = "looselyBoundRecurrent";
    private static final String KEY_HANDLER_URI = "handlerUri";
    private static final String KEY_SCHEDULE = "schedule";

    static JobKey createJobKeyForTask(Task t) {
        return new JobKey(t.getOid());
    }

    public static JobKey createJobKeyForTaskOid(String oid) {
        return new JobKey(oid);
    }

    static TriggerKey createTriggerKeyForTask(Task t) {
        return new TriggerKey(t.getOid());
    }

    static TriggerKey createTriggerKeyForTaskOid(String oid) {
        return new TriggerKey(oid);
    }

    static JobDetail createJobDetailForTask(Task task) {
        return JobBuilder.newJob(JobExecutor.class)
          .withIdentity(createJobKeyForTask(task))
          .storeDurably()
          .requestRecovery()
          .build();
    }

    /**
     * Create trigger for task
     *
     * Returned trigger can be null in following cases:
     *
     * - task is not ready ({@link TaskSchedulingStateType#READY}
     * - task is not persistent (e.g. lightweight tasks)
     * - task is "on demand" (it is configured as recurring, but without schedule)
     *
     * @param task task for which trigger should be created
     * @return Quartz trigger for the task or null if task is not eligible for trigger (see above)
     * @throws ParseException in case of invalid Cron pattern
     */
    public static Trigger createTriggerForTask(Task task) throws ParseException {

        if (task.getSchedulingState() != TaskSchedulingStateType.READY || !task.isPersistent()) {
            return null; // no triggers for such tasks
        }

        ScheduleType schedule = task.getSchedule();
        boolean recurring = task.isRecurring();
        Integer interval = schedule != null ? schedule.getInterval() : null;
        String cronLikePattern = schedule != null ? schedule.getCronLikePattern() : null;

        if (recurring && interval == null && cronLikePattern == null) {
            // special case - recurring task with no schedule (means "run on demand only")
            return null;
        }

        TriggerBuilder<Trigger> tb = createBasicTriggerBuilderForTask(task)
              .withIdentity(createTriggerKeyForTask(task));

        if (schedule != null) {

            Date est = schedule.getEarliestStartTime() != null ?
                    schedule.getEarliestStartTime().toGregorianCalendar().getTime() :
                    null;

            Date lst = schedule.getLatestStartTime() != null ?
                    schedule.getLatestStartTime().toGregorianCalendar().getTime() :
                    null;

            // endAt must not be sooner than startAt
            if (lst != null && est == null) {
                if (lst.getTime() < System.currentTimeMillis()) {
                    est = lst;      // there's no point in setting est to current time
                }
            }

            if (est != null) {
                tb.startAt(est);
            } else {
                tb.startNow();
            }

            if (lst != null) {
                tb.endAt(lst);
                // LST is checked also within JobExecutor (needed mainly for tightly-bound recurring tasks)
            }

            if (schedule.getLatestFinishTime() != null) {
                tb.endAt(schedule.getLatestFinishTime().toGregorianCalendar().getTime());
                // however, it is the responsibility of task handler to finish no later than this time
            }
        }

        boolean looselyBoundRecurring;

        if (recurring && task.isLooselyBound()) {

            looselyBoundRecurring = true;

            ScheduleBuilder<? extends Trigger> sb;
            if (interval != null) {
                SimpleScheduleBuilder ssb = simpleSchedule()
                        .withIntervalInSeconds(interval)
                        .repeatForever();
                if (schedule.getMisfireAction() == null || schedule.getMisfireAction() == MisfireActionType.EXECUTE_IMMEDIATELY) {
                    sb = ssb.withMisfireHandlingInstructionFireNow();
                } else if (schedule.getMisfireAction() == MisfireActionType.RESCHEDULE) {
                    sb = ssb.withMisfireHandlingInstructionNextWithRemainingCount();
                } else {
                    throw new SystemException("Invalid value of misfireAction: " + schedule.getMisfireAction() + " for task " + task);
                }
            } else {
                CronScheduleBuilder csb = cronScheduleNonvalidatedExpression(cronLikePattern); // may throw ParseException
                if (schedule.getMisfireAction() == null || schedule.getMisfireAction() == MisfireActionType.EXECUTE_IMMEDIATELY) {
                    sb = csb.withMisfireHandlingInstructionFireAndProceed();
                } else if (schedule.getMisfireAction() == MisfireActionType.RESCHEDULE) {
                    sb = csb.withMisfireHandlingInstructionDoNothing();
                } else {
                    throw new SystemException("Invalid value of misfireAction: " + schedule.getMisfireAction() + " for task " + task);
                }
            }
            tb.withSchedule(sb);
        } else {
            // even non-recurring tasks will be triggered, to check whether they should not be restarted
            // (their trigger will be erased when these tasks will be completed)

            looselyBoundRecurring = false;
        }

        tb.usingJobData(KEY_SCHEDULE, scheduleFingerprint(schedule));
        tb.usingJobData(KEY_LOOSELY_BOUND_RECURRENT, looselyBoundRecurring);
        tb.usingJobData(KEY_HANDLER_URI, task.getHandlerUri());

        return tb.build();
    }

    private static TriggerBuilder<Trigger> createBasicTriggerBuilderForTask(Task task) {
        TaskExecutionConstraintsType executionConstraints = task.getExecutionConstraints();
        String executionGroup = executionConstraints != null
                ? MiscUtil.nullIfEmpty(executionConstraints.getGroup())
                : null;
        return TriggerBuilder.newTrigger()
                .forJob(createJobKeyForTask(task))
                .executionGroup(executionGroup);
    }

    static Trigger createTriggerNowForTask(Task task) {
        return createBasicTriggerBuilderForTask(task)
                .startNow()
                .build();
    }

    public static Trigger createTriggerForTask(Task task, long startAt) {
        return createBasicTriggerBuilderForTask(task)
                .startAt(new Date(startAt))
                .build();
    }

    private static long xmlGCtoMillis(XMLGregorianCalendar gc) {
        return gc != null ? gc.toGregorianCalendar().getTimeInMillis() : 0L;
    }

    // quick hack, todo: do seriously
    private static String scheduleFingerprint(ScheduleType scheduleType) {
        if (scheduleType == null) {
            return "";
        } else {
            return scheduleType.getInterval() + " $$ " +
                    scheduleType.getCronLikePattern() + " $$ " +
                    xmlGCtoMillis(scheduleType.getEarliestStartTime()) + " $$ " +
                    xmlGCtoMillis(scheduleType.getLatestStartTime()) + " $$ " +
                    xmlGCtoMillis(scheduleType.getLatestFinishTime()) + " $$ " +
                    scheduleType.getMisfireAction() + " $$";
        }
    }

    static boolean triggersDiffer(Trigger triggerAsIs, Trigger triggerToBe) {
        return !Objects.equals(triggerAsIs.getExecutionGroup(), triggerToBe.getExecutionGroup())
                || triggerDataMapsDiffer(triggerAsIs, triggerToBe);
    }

    // compares scheduling-related data maps of triggers
    private static boolean triggerDataMapsDiffer(Trigger triggerAsIs, Trigger triggerToBe) {

        JobDataMap asIs = triggerAsIs.getJobDataMap();
        JobDataMap toBe = triggerToBe.getJobDataMap();

        boolean scheduleDiffer = !toBe.getString(KEY_SCHEDULE).equals(asIs.getString(KEY_SCHEDULE));
        boolean lbrDiffer = toBe.getBoolean(KEY_LOOSELY_BOUND_RECURRENT) != asIs.getBoolean(KEY_LOOSELY_BOUND_RECURRENT);
        String tbh = toBe.getString(KEY_HANDLER_URI);
        String aih = asIs.getString(KEY_HANDLER_URI);
        boolean handlersDiffer = !Objects.equals(tbh, aih);

        if (LOGGER.isTraceEnabled()) {
            if (scheduleDiffer) {
                LOGGER.trace("trigger data maps differ in schedule: triggerAsIs.schedule = {}, triggerToBe.schedule = {}",
                        asIs.getString(KEY_SCHEDULE), toBe.getString(KEY_SCHEDULE));
            }
            if (lbrDiffer) {
                LOGGER.trace("trigger data maps differ in looselyBoundRecurrent: triggerAsIs = {}, triggerToBe = {}",
                        asIs.getBoolean(KEY_LOOSELY_BOUND_RECURRENT), toBe.getBoolean(KEY_LOOSELY_BOUND_RECURRENT));
            }
            if (handlersDiffer) {
                LOGGER.trace("trigger data maps differ in handlerUri: triggerAsIs = {}, triggerToBe = {}", aih, tbh);
            }
        }
        return scheduleDiffer || lbrDiffer || handlersDiffer;
    }
}
