/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.time.DurationFormatUtils;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * TODO
 */
public class TaskTypeUtil {

    public static final long NOW = 0L;
    private static final long RUNS_CONTINUALLY = -1L;
    private static final long ALREADY_PASSED = -2L;

    public static String createScheduledToRunAgain(TaskType task, List<Object> localizationObject) {
        boolean runnable = task.getSchedulingState() == TaskSchedulingStateType.READY;
        Long scheduledAfter = getScheduledToStartAgain(task);
        Long retryAfter = runnable ? getRetryAfter(task) : null;

        if (scheduledAfter == null) {
            if (retryAfter == null || retryAfter <= 0) {
                return "";
            }
        } else if (scheduledAfter == NOW) { // TODO what about retryTime?
            return runnable ? "pageTasks.now" : "pageTasks.nowForNotRunningTasks";
        } else if (scheduledAfter == RUNS_CONTINUALLY) {    // retryTime is probably null here
            return "pageTasks.runsContinually";
        } else if (scheduledAfter == ALREADY_PASSED && retryAfter == null) {
            return runnable ? "pageTasks.alreadyPassed" : "pageTasks.alreadyPassedForNotRunningTasks";
        }

        long displayTime;
        boolean displayAsRetry;
        if (retryAfter != null && retryAfter > 0 && (scheduledAfter == null || scheduledAfter < 0
                || retryAfter < scheduledAfter)) {
            displayTime = retryAfter;
            displayAsRetry = true;
        } else {
            displayTime = scheduledAfter;
            displayAsRetry = false;
        }

        String key;
        if (runnable) {
            key = displayAsRetry ? "pageTasks.retryIn" : "pageTasks.in";
        } else {
            key = "pageTasks.inForNotRunningTasks";
        }
        localizationObject.add(DurationFormatUtils.formatDurationWords(displayTime, true, true));
        return key;
    }

    private static Long getRetryAfter(TaskType task) {
        XMLGregorianCalendar nextRun = task.getNextRetryTimestamp();
        return nextRun != null ? (XmlTypeConverter.toMillis(nextRun) - System.currentTimeMillis()) : null;
    }

    public static Long getScheduledToStartAgain(TaskType task) {
        long current = System.currentTimeMillis();

        if (task.getNodeAsObserved() != null && (task.getSchedulingState() != TaskSchedulingStateType.SUSPENDED)) {

            if (TaskRecurrenceType.RECURRING != task.getRecurrence()) {
                return null;
            } else if (TaskBindingType.TIGHT == task.getBinding()) {
                return RUNS_CONTINUALLY; // runs continually; todo provide some information also in this case
            }
        }

        XMLGregorianCalendar nextRun = task.getNextRunStartTimestamp();
        Long nextRunStartTimeLong = nextRun != null ? XmlTypeConverter.toMillis(nextRun) : null;
        if (nextRunStartTimeLong == null || nextRunStartTimeLong == 0) {
            return null;
        }

        if (nextRunStartTimeLong > current + 1000) {
            return nextRunStartTimeLong - System.currentTimeMillis();
        } else if (nextRunStartTimeLong < current - 60000) {
            return ALREADY_PASSED;
        } else {
            return NOW;
        }
    }
}
