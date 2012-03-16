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
package com.evolveum.midpoint.task.impl;

import it.sauronsoftware.cron4j.Predictor;

import java.util.Date;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * Evaluates schedules. 
 * 
 * @author Radovan Semancik
 *
 */
public class ScheduleEvaluator {

	private static final transient Trace LOGGER = TraceManager.getTrace(ScheduleEvaluator.class);

	/**
	 * Determines whether the task should be started now (used by the task scanner).
	 * For recurring tasks it uses determineSleepTime (that uses pre-computed nextRunStartTime).
	 * For single-run tasks it signals immediate run.
	 *    
	 * @param taskType
	 * @return
	 */
	public static boolean shouldRun(TaskType taskType) {
		if (TaskRecurrenceType.RECURRING.equals(taskType.getRecurrence()))
			return determineSleepTime(taskType) <= 0;
		else
			return true;
	}
	
	/**
	 * Determines whether this task has missed its scheduled start.
	 * It can occur for tasks scheduled to a fixed moment (currently using cron-like specification).
	 * 
	 * @param taskType
	 * @return
	 */
	public static boolean missedScheduledStart(TaskType taskType) {
		if (TaskRecurrenceType.RECURRING.equals(taskType.getRecurrence()) &&
				taskType.getSchedule() != null &&
				taskType.getSchedule().getCronLikePattern() != null &&
				taskType.getSchedule().getMissedScheduleTolerance() != null) {
			
			if (taskType.getNextRunStartTime() == null) {
				return true;	// if it was not scheduled yet, and we have (any) defined tolerance, we should reschedule
			}
			
			// otherwise let us look whether we are behind tolerance window
			long nextRunTime = XmlTypeConverter.toMillis(taskType.getNextRunStartTime());
			long tolerance = taskType.getSchedule().getMissedScheduleTolerance().longValue() * 1000L;
			return System.currentTimeMillis() > nextRunTime + tolerance;
			
		} else {
			return false;		// ok, we have not missed the scheduled start (perhaps there was none :)
		}
	}

	/**
	 * Determines how long to sleep until next run of this (cyclic) tasks.
	 * Uses pre-computed nextRunStartTime.
	 * 
	 * @param task
	 * @return 0 when nextRunStartTime is not defined
	 */
	public static long determineSleepTime(Task task) {
		return determineSleepTime(task.getNextRunStartTime(), task.getName());
	}
	
	/**
	 * Determines how long to sleep until next run of this (cyclic) tasks.
	 * Uses pre-computed nextRunStartTime.
	 * 
	 * @param task
	 * @return 0 when nextRunStartTime is not defined
	 */
	public static long determineSleepTime(TaskType taskType) {
		long nextRunTime;
		if (taskType.getNextRunStartTime() != null)
			nextRunTime = taskType.getNextRunStartTime().toGregorianCalendar().getTimeInMillis();
		else
			nextRunTime = 0;
		return determineSleepTime(nextRunTime, taskType.getName());
	}
	
	private static long determineSleepTime(long nextRunTime, String taskName) {
		long delta;
		if (nextRunTime > 0)
			delta = nextRunTime - System.currentTimeMillis();
		else
			delta = 0;			// if nextRunTime is 0L, run immediately
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("determineSleepTime for " + taskName + " = " + delta + " (next run time: " + new Date(nextRunTime) + ")");
		return delta > 0 ? delta : 0;
	}

	/**
	 * Determines the time when a task should run (for recurrent tasks).
	 * It is usually called after the previous run finishes.  
	 * 
	 * @param taskImpl
	 * @return
	 */
	public static Long determineNextRunStartTime(Task task) {
		Long retval = null;
		if (task.isCycle())
			retval = determineNextRunStartTime(task.getSchedule(), task.getLastRunStartTimestamp(), task.getName());
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("determineNextRunStartTime for task " + task + " = " + retval + (retval!=null ? " (" + new Date(retval) + ")" : ""));
		return retval;
	}

	public static long determineNextRunStartTime(TaskType taskType) {
		long retval = 0L;
		if (TaskRecurrenceType.RECURRING.equals(taskType.getRecurrence())) {
			long lastStarted;
			if (taskType.getLastRunStartTimestamp() != null)
				lastStarted = taskType.getLastRunStartTimestamp().toGregorianCalendar().getTimeInMillis();
			else
				lastStarted = 0;
			retval = determineNextRunStartTime(taskType.getSchedule(), lastStarted, taskType.getName());
		}
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("determineNextRunStartTime for taskType " + taskType.getName() + " = " + retval + " (" + new Date(retval) + ")");
		return retval;
	}
	
	
	private static Long determineNextRunStartTime(ScheduleType schedule, long lastStarted, String taskName) {
		if (schedule.getInterval() != null) {
			if (lastStarted == 0)
				return System.currentTimeMillis();		// run now!
			else
				return lastStarted + schedule.getInterval().longValue() * 1000; 
		}
		else if (schedule.getCronLikePattern() != null) {
			Predictor p = new Predictor(schedule.getCronLikePattern());
			return p.nextMatchingTime();
		}
		else {
//			LOGGER.error("Missing task schedule: no interval nor cron-like specification for recurring task: " + taskName + ", determining next run to be 'never'");
//			return new Date(9999, 1, 1).getTime();		// run never, TODO: better handle "never run" situation
			LOGGER.error("Missing task schedule: no interval nor cron-like specification for recurring task: " + taskName + ", determining next run to be 'now'");
			return 0L;
		}
	}

}
