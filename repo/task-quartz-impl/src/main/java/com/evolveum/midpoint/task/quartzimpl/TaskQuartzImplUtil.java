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

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.quartz.*;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;

import javax.xml.datatype.XMLGregorianCalendar;

public class TaskQuartzImplUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskQuartzImplUtil.class);

    public static final long SINGLE_TASK_CHECK_INTERVAL = 10000;

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
		
		JobDetail job = JobBuilder.newJob(JobExecutor.class)
	      .withIdentity(TaskQuartzImplUtil.createJobKeyForTask(task))
	      .storeDurably()
          .requestRecovery()
	      .build();
		
		return job;
	}
	
	public static Trigger createTriggerForTask(Task task) throws ParseException {
		
		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
			return null;			// no triggers for such tasks
        }

        // special case - recurrent task with no schedule (means "run on demand only")
        if (task.isCycle() && (task.getSchedule() == null ||
                (task.getSchedule().getInterval() == null && task.getSchedule().getCronLikePattern() == null))) {
            return null;
        }

		TriggerBuilder<Trigger> tb = createBasicTriggerBuilderForTask(task)
		      .withIdentity(createTriggerKeyForTask(task));

        if (task.getSchedule() != null) {

            Date est = task.getSchedule().getEarliestStartTime() != null ?
                    task.getSchedule().getEarliestStartTime().toGregorianCalendar().getTime() :
                    null;

            Date lst = task.getSchedule().getLatestStartTime() != null ?
                    task.getSchedule().getLatestStartTime().toGregorianCalendar().getTime() :
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

            if (task.getSchedule().getLatestFinishTime() != null) {
                tb.endAt(task.getSchedule().getLatestFinishTime().toGregorianCalendar().getTime());
                // however, it is the responsibility of task handler to finish no later than this time
            }
        }

        boolean looselyBoundRecurrent;

        if (task.isCycle() && task.isLooselyBound()) {

            looselyBoundRecurrent = true;

        	ScheduleType sch = task.getSchedule();
        	if (sch == null) {
                return null;
        		//throw new IllegalStateException("Recurrent task " + task + " does not have a schedule.");
            }

            ScheduleBuilder sb;
        	if (sch.getInterval() != null) {
        		SimpleScheduleBuilder ssb = simpleSchedule()
        				.withIntervalInSeconds(sch.getInterval().intValue())
        				.repeatForever();
                if (sch.getMisfireAction() == null || sch.getMisfireAction() == MisfireActionType.EXECUTE_IMMEDIATELY) {
                    sb = ssb.withMisfireHandlingInstructionFireNow();
                } else if (sch.getMisfireAction() == MisfireActionType.RESCHEDULE) {
                    sb = ssb.withMisfireHandlingInstructionNextWithRemainingCount();
                } else {
                    throw new SystemException("Invalid value of misfireAction: " + sch.getMisfireAction() + " for task " + task);
                }

            } else if (sch.getCronLikePattern() != null) {
                CronScheduleBuilder csb = cronScheduleNonvalidatedExpression(sch.getCronLikePattern());			// may throw ParseException
                if (sch.getMisfireAction() == null || sch.getMisfireAction() == MisfireActionType.EXECUTE_IMMEDIATELY) {
                    sb = csb.withMisfireHandlingInstructionFireAndProceed();
                } else if (sch.getMisfireAction() == MisfireActionType.RESCHEDULE) {
                    sb = csb.withMisfireHandlingInstructionDoNothing();
                } else {
                    throw new SystemException("Invalid value of misfireAction: " + sch.getMisfireAction() + " for task " + task);
                }
        	} else {
                return null;
        		//throw new IllegalStateException("The schedule for task " + task + " is neither fixed nor cron-like one.");
            }

            tb.withSchedule(sb);
        } else {
            // even non-recurrent tasks will be triggered, to check whether they should not be restarted
            // (their trigger will be erased when these tasks will be completed)

            looselyBoundRecurrent = false;
            // tb.withSchedule(simpleSchedule().withIntervalInMilliseconds(SINGLE_TASK_CHECK_INTERVAL).repeatForever());
        }

        tb.usingJobData("schedule", scheduleFingerprint(task.getSchedule()));
        tb.usingJobData("looselyBoundRecurrent", looselyBoundRecurrent);
        tb.usingJobData("handlerUri", task.getHandlerUri());

		return tb.build();
	}

	private static TriggerBuilder<Trigger> createBasicTriggerBuilderForTask(Task task) {
		TaskType taskType = task.getTaskPrismObject().asObjectable();
		String executionGroup = taskType.getExecutionConstraints() != null
				? MiscUtil.nullIfEmpty(taskType.getExecutionConstraints().getGroup())
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


    public static ParseException validateCronExpression(String cron) {

        try {
            cronScheduleNonvalidatedExpression(cron);			// may throw ParseException
            return null;
        } catch (ParseException pe) {
            return pe;
        }
    }

}
