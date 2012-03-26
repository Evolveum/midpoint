package com.evolveum.midpoint.task.quartzimpl;

import static org.quartz.CronScheduleBuilder.cronScheduleNonvalidatedExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.text.ParseException;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.MutableTrigger;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;

public class TaskQuartzImplUtil {

	public static JobKey createJobKeyForTask(Task t) {
    	return new JobKey(t.getOid());
    }
	
	public static TriggerKey createTriggerKeyForTask(Task t) {
    	return new TriggerKey(t.getOid());
    }

	public static JobDetail createJobDetailForTask(Task task) {
		
		JobDetail job = JobBuilder.newJob(JobExecutor.class)
	      .withIdentity(TaskQuartzImplUtil.createJobKeyForTask(task))
	      .storeDurably()
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

        if (task.isCycle() && task.isLooselyBound()) {			// tightly-bound tasks are executed without schedule (their repeating is done within the JobExecutor)
        	
        	ScheduleType sch = task.getSchedule();
        	if (sch == null)
        		throw new IllegalStateException("Recurrent task " + task + " does not have a schedule.");
        	
        	if (sch.getInterval() != null) {
        		tb.withSchedule(simpleSchedule()
        				.withIntervalInSeconds(sch.getInterval().intValue())
        				.repeatForever());
        	} else if (sch.getCronLikePattern() != null) {
				tb.withSchedule(cronScheduleNonvalidatedExpression(sch.getCronLikePattern()));			// may throw ParseException
        	} else
        		throw new IllegalStateException("The schedule for task " + task + " is neither fixed nor cron-like one.");
        }

		return tb.build();
	}

}
