package com.evolveum.midpoint.task.quartzimpl.jobstore;

import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobDetailImpl;
import org.quartz.spi.OperableTrigger;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.JobExecutor;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

public class RepoJobStoreUtil {

    public static final String TASK_PROPERTY = "Task";

	public static OperableTrigger createTriggerForTask(Task t) {
    	
        TriggerBuilder tb = newTrigger()
        .withDescription(t.getName() + " - trigger")
        .withPriority(1)
        .startAt(new Date(80, 0, 1))			// FIXME: hack...
        .endAt(null)
        .withIdentity(t.getOid())
        .forJob(createJobKeyForTask(t))
        .usingJobData(createJobDataMapForTask(t));
        
        if (t.isCycle() && t.isLooselyBound()) {			// tightly-bound tasks are executed without schedule (their repeating is done within the JobExecutor)
        	
        	ScheduleType sch = t.getSchedule();
        	if (sch == null)
        		throw new IllegalStateException("Recurrent task " + t + " does not have a schedule.");
        	
        	if (sch.getInterval() != null) {
        		tb.withSchedule(simpleSchedule().withIntervalInSeconds(sch.getInterval().intValue()).repeatForever());
        	} else if (sch.getCronLikePattern() != null) {
        		// TODO: add this
        		throw new UnsupportedOperationException();
        	} else
        		throw new IllegalStateException("The schedule for task " + t + " is neither fixed nor cron-like one.");
        }

		OperableTrigger trigger = (OperableTrigger) tb.build();

//		trigger.setMisfireInstruction(misFireInstr);
		trigger.setNextFireTime(t.getNextRunStartTime() != null ? new Date(t.getNextRunStartTime()) : new Date(System.currentTimeMillis()-1000));	// FIXME
		trigger.setPreviousFireTime(t.getLastRunStartTimestamp() != null ? new Date(t.getLastRunStartTimestamp()) : null);		// TODO: this is only an approximation

		//setTriggerStateProperties(trigger, triggerProps);

		return trigger;
	}
    
    public static JobDataMap createJobDataMapForTask(Task t) {
    	Map<String,Object> m = new HashMap<String,Object>();
    	m.put(TASK_PROPERTY, t);
    	return new JobDataMap(m);
	}

	public static JobKey createJobKeyForTask(Task t) {
    	return new JobKey(t.getOid());
    }
	
	public static Task getTaskFromTrigger(Trigger trigger) {
		return (Task) trigger.getJobDataMap().get(TASK_PROPERTY);
	}
	
	public static void markTriggerWrapperComplete(TriggerWrapper tw, OperationResult result) throws JobPersistenceException {
		tw.state = TriggerWrapper.STATE_COMPLETE;
		try {
			getTaskFromTrigger(tw.trigger).setExecutionStatusImmediate(TaskExecutionStatus.CLOSED, result);
		} catch (Exception e) {
			throw new JobPersistenceException("Cannot mark trigger as completed", e);
		}
	}

	public static void markTriggerWrapperAcquired(TriggerWrapper tw, OperationResult result) throws JobPersistenceException {
		tw.state = TriggerWrapper.STATE_ACQUIRED;
		try {
			getTaskFromTrigger(tw.trigger).setExclusivityStatusImmediate(TaskExclusivityStatus.CLAIMED, result);
		} catch (Exception e) {
			throw new JobPersistenceException("Cannot mark trigger as acquired", e);
		}
	}

	public static Task createTaskFromTaskPrism(TaskManagerQuartzImpl taskManagerImpl, PrismObject<TaskType> taskPrism, OperationResult result) throws JobPersistenceException {
		try {
			return taskManagerImpl.createTaskInstance(taskPrism, result);
		} catch (SchemaException e) {
			throw new JobPersistenceException("Cannot create Task instance from TaskType", e);
		}
	}

	public static JobDetail createJobForTask(Task task) {
		
       	JobDetailImpl job = new JobDetailImpl();
//        job.setName(task.getName());
		job.setKey(createJobKeyForTask(task));
        job.setDescription(task.getName());
        job.setJobClass(JobExecutor.class);
        job.setDurability(true);
        job.setRequestsRecovery(true);

        Map<String,Object> map = new HashMap<String,Object>();
        map.put(RepoJobStoreUtil.TASK_PROPERTY, task);
        job.setJobDataMap(new JobDataMap(map));
        return job;
	}

}
