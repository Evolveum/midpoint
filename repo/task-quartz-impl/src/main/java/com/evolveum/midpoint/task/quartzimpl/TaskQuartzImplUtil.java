package com.evolveum.midpoint.task.quartzimpl;

import static org.quartz.CronScheduleBuilder.cronScheduleNonvalidatedExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.text.ParseException;

import org.quartz.*;
import org.quartz.spi.MutableTrigger;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;

public class TaskQuartzImplUtil {

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
		
		if (task.getExecutionStatus() != TaskExecutionStatus.RUNNING)
			return null;			// no triggers for such tasks

		TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
		      .withIdentity(createTriggerKeyForTask(task))
		      .forJob(createJobKeyForTask(task))
		      .startNow();

        String cronExpression = "";
        int interval = 0;
        boolean looselyBoundRecurrent;

        if (task.isCycle()) {

            looselyBoundRecurrent = true;

        	ScheduleType sch = task.getSchedule();
        	if (sch == null)
        		throw new IllegalStateException("Recurrent task " + task + " does not have a schedule.");
        	
        	if (sch.getInterval() != null) {
                interval = sch.getInterval().intValue();
        		tb.withSchedule(simpleSchedule()
        				.withIntervalInSeconds(sch.getInterval().intValue())
        				.repeatForever());
        	} else if (sch.getCronLikePattern() != null) {
                cronExpression = sch.getCronLikePattern();
				tb.withSchedule(cronScheduleNonvalidatedExpression(sch.getCronLikePattern()));			// may throw ParseException
        	} else
        		throw new IllegalStateException("The schedule for task " + task + " is neither fixed nor cron-like one.");
        } else {
            // even non-recurrent tasks will be triggered, to check whether they should not be restarted
            // (their trigger will be erased when these tasks will be completed)

            looselyBoundRecurrent = false;
            tb.withSchedule(simpleSchedule().withIntervalInMilliseconds(SINGLE_TASK_CHECK_INTERVAL).repeatForever());
        }

        tb.usingJobData("cronExpression", cronExpression);
        tb.usingJobData("interval", interval);
        tb.usingJobData("looselyBoundRecurrent", looselyBoundRecurrent);

		return tb.build();
	}

    // compares scheduling-related data maps of triggers
    static boolean triggerDataMapsDiffer(Trigger triggerAsIs, Trigger triggerToBe) {

        JobDataMap asIs = triggerAsIs.getJobDataMap();
        JobDataMap toBe = triggerToBe.getJobDataMap();

        return toBe.getString("cronExpression").equals(asIs.getString("cronExpression")) &&
                toBe.getInt("interval") == asIs.getInt("interval") &&
                toBe.getBoolean("looselyBoundRecurrent") == asIs.getBoolean("looselyBoundRecurrent");
    }


}
