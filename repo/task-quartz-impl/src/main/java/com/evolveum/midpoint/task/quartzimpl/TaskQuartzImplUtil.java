package com.evolveum.midpoint.task.quartzimpl;

import static org.quartz.CronScheduleBuilder.cronScheduleNonvalidatedExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.MisfireActionType;
import org.quartz.*;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;

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
		
		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE)
			return null;			// no triggers for such tasks

		TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
		      .withIdentity(createTriggerKeyForTask(task))
		      .forJob(createJobKeyForTask(task));

        if (task.getSchedule() != null) {

            if (task.getSchedule().getEarliestStartTime() != null) {
                tb.startAt(task.getSchedule().getEarliestStartTime().toGregorianCalendar().getTime());
            } else {
                tb.startNow();
            }

            if (task.getSchedule().getLatestStartTime() != null) {
                tb.endAt(task.getSchedule().getLatestStartTime().toGregorianCalendar().getTime());
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
        	if (sch == null)
        		throw new IllegalStateException("Recurrent task " + task + " does not have a schedule.");

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
        	} else
        		throw new IllegalStateException("The schedule for task " + task + " is neither fixed nor cron-like one.");

            tb.withSchedule(sb);
        } else {
            // even non-recurrent tasks will be triggered, to check whether they should not be restarted
            // (their trigger will be erased when these tasks will be completed)

            looselyBoundRecurrent = false;
            // tb.withSchedule(simpleSchedule().withIntervalInMilliseconds(SINGLE_TASK_CHECK_INTERVAL).repeatForever());
        }

        tb.usingJobData("schedule", scheduleFingerprint(task.getSchedule()));
        tb.usingJobData("looselyBoundRecurrent", looselyBoundRecurrent);

		return tb.build();
	}

    public static Trigger createTriggerNowForTask(Task task) {

        TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                .forJob(createJobKeyForTask(task)).startNow();

        return tb.build();
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

    // compares scheduling-related data maps of triggers
    public static boolean triggerDataMapsDiffer(Trigger triggerAsIs, Trigger triggerToBe) {

        JobDataMap asIs = triggerAsIs.getJobDataMap();
        JobDataMap toBe = triggerToBe.getJobDataMap();

        boolean scheduleDiffer = !toBe.getString("schedule").equals(asIs.getString("schedule"));
        boolean lbrDiffer = toBe.getBoolean("looselyBoundRecurrent") != asIs.getBoolean("looselyBoundRecurrent");

        if (scheduleDiffer) {
            LOGGER.trace("trigger data maps differ in schedule: triggerAsIs.schedule = " + asIs.getString("schedule") + ", triggerToBe.schedule = " + toBe.getString("schedule"));
        }
        if (lbrDiffer) {
            LOGGER.trace("trigger data maps differ in looselyBoundRecurrent: triggerAsIs = " + asIs.getString("looselyBoundRecurrent") + ", triggerToBe = " + toBe.getString("looselyBoundRecurrent"));
        }
        return scheduleDiffer || lbrDiffer;
    }


    public static List<String> tasksToOperationResult(Collection<Task> tasks) {
        List<String> retval = new ArrayList<String>();
        for (Task t : tasks) {
            retval.add(t.getOid() + ": " + t.getName());
        }
        return retval;
    }
}
