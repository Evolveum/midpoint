/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.quartz.CronScheduleBuilder.cronScheduleNonvalidatedExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

import org.quartz.*;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;

import javax.xml.datatype.XMLGregorianCalendar;

public class TaskQuartzImplUtil {

    private static final Trace LOGGER = TraceManager.getTrace(TaskQuartzImplUtil.class);

    public static JobKey createJobKeyForTask(Task t) {
        return new JobKey(t.getOid());
    }

    public static JobKey createJobKeyForTaskOid(String oid) {
        return new JobKey(oid);
    }

    public static TriggerKey createTriggerKeyForTask(Task t) {
        return new TriggerKey(t.getOid());
    }

    public static TriggerKey createTriggerKeyForTaskOid(String oid) {
        return new TriggerKey(oid);
    }

    public static JobDetail createJobDetailForTask(Task task) {
        return JobBuilder.newJob(JobExecutor.class)
          .withIdentity(TaskQuartzImplUtil.createJobKeyForTask(task))
          .storeDurably()
          .requestRecovery()
          .build();
    }

    public static Trigger createTriggerForTask(Task task) throws ParseException {

        if (task.getExecutionState() != TaskExecutionStateType.RUNNABLE) {
            return null;            // no triggers for such tasks
        }

        ScheduleType schedule = task.getSchedule();
        boolean recurring = task.isRecurring();
        Integer interval = schedule != null ? schedule.getInterval() : null;
        String cronLikePattern = schedule != null ? schedule.getCronLikePattern() : null;

        if (recurring && interval == null && cronLikePattern == null) {
            // special case - recurrent task with no schedule (means "run on demand only")
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
                // LST is checked also within JobExecutor (needed mainly for tightly-bound recurrent tasks)
            }

            if (schedule.getLatestFinishTime() != null) {
                tb.endAt(schedule.getLatestFinishTime().toGregorianCalendar().getTime());
                // however, it is the responsibility of task handler to finish no later than this time
            }
        }

        boolean looselyBoundRecurrent;

        if (recurring && task.isLooselyBound()) {

            looselyBoundRecurrent = true;

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
                assert cronLikePattern != null;
                CronScheduleBuilder csb = cronScheduleNonvalidatedExpression(cronLikePattern);            // may throw ParseException
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
            // even non-recurrent tasks will be triggered, to check whether they should not be restarted
            // (their trigger will be erased when these tasks will be completed)

            looselyBoundRecurrent = false;
        }

        tb.usingJobData("schedule", scheduleFingerprint(schedule));
        tb.usingJobData("looselyBoundRecurrent", looselyBoundRecurrent);
        tb.usingJobData("handlerUri", task.getHandlerUri());

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

    public static Trigger createTriggerNowForTask(Task task) {
        return createBasicTriggerBuilderForTask(task)
                .startNow()
                .build();
    }

    public static Trigger createTriggerForTask(Task task, long startAt) {
        return createBasicTriggerBuilderForTask(task)
                .startAt(new Date(startAt))
                .build();
    }

    public static long xmlGCtoMillis(XMLGregorianCalendar gc) {
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

    public static boolean triggersDiffer(Trigger triggerAsIs, Trigger triggerToBe) {
        return !Objects.equals(triggerAsIs.getExecutionGroup(), triggerToBe.getExecutionGroup())
                || triggerDataMapsDiffer(triggerAsIs, triggerToBe);
    }

    // compares scheduling-related data maps of triggers
    private static boolean triggerDataMapsDiffer(Trigger triggerAsIs, Trigger triggerToBe) {

        JobDataMap asIs = triggerAsIs.getJobDataMap();
        JobDataMap toBe = triggerToBe.getJobDataMap();

        boolean scheduleDiffer = !toBe.getString("schedule").equals(asIs.getString("schedule"));
        boolean lbrDiffer = toBe.getBoolean("looselyBoundRecurrent") != asIs.getBoolean("looselyBoundRecurrent");
        String tbh = toBe.getString("handlerUri");
        String aih = asIs.getString("handlerUri");
        //LOGGER.trace("handlerUri: asIs = " + aih + ", toBe = " + tbh);
        boolean handlersDiffer = tbh != null ? !tbh.equals(aih) : aih == null;

        if (LOGGER.isTraceEnabled()) {
            if (scheduleDiffer) {
                LOGGER.trace("trigger data maps differ in schedule: triggerAsIs.schedule = " + asIs.getString("schedule") + ", triggerToBe.schedule = " + toBe.getString("schedule"));
            }
            if (lbrDiffer) {
                LOGGER.trace("trigger data maps differ in looselyBoundRecurrent: triggerAsIs = " + asIs.getBoolean("looselyBoundRecurrent") + ", triggerToBe = " + toBe.getBoolean("looselyBoundRecurrent"));
            }
            if (handlersDiffer) {
                LOGGER.trace("trigger data maps differ in handlerUri: triggerAsIs = " + aih + ", triggerToBe = " + tbh);
            }
        }
        return scheduleDiffer || lbrDiffer || handlersDiffer;
    }

    static ParseException validateCronExpression(String cron) {
        try {
            cronScheduleNonvalidatedExpression(cron);            // may throw ParseException
            return null;
        } catch (ParseException pe) {
            return pe;
        }
    }
}
