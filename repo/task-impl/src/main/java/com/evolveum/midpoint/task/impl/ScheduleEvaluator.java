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

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * Evaluates schedules. 
 * 
 * @author Radovan Semancik
 *
 */
public class ScheduleEvaluator {

	public static boolean shouldRun(TaskType taskType) {
		ScheduleType schedule = taskType.getSchedule();
		if (schedule == null) {
			// No schedule, big fun. Run right now.
			return true;
		}
		
		long lastRunTime = 0;
		if (taskType.getLastRunStartTimestamp()!=null) {
			lastRunTime = taskType.getLastRunStartTimestamp().getMillisecond();
		}
		return shouldRun(schedule,lastRunTime);
	}
	
	/**
	 * Determines whether a task is about to run given a schedule, last run time and a current time.
	 * 
	 * @param schedule
	 * @param lastRunTime
	 * @return
	 */
	public static boolean shouldRun(ScheduleType schedule, long lastRunTime) {

		// Assumes cycle type of task now
		
		long runInterval = schedule.getInterval().longValue() * 1000;
		
		if (lastRunTime == 0
					|| lastRunTime + runInterval < System
							.currentTimeMillis()) {
				return true;
		}
		
		return false;

	}

	public static long determineSleepTime(Task task) {
		// TODO Compute the time
		return 15000;
	}
	
}
