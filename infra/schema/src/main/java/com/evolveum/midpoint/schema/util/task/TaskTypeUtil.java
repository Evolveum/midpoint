/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil.hasStateOverview;

/**
 * TODO
 */
public class TaskTypeUtil {

    public static final long NOW = 0L;
    private static final long RUNS_CONTINUALLY = -1L;
    private static final long ALREADY_PASSED = -2L;

    /**
     * Returns key for localization representing execution status of a task, additionally populates {@code localizationObject}
     * with additional parameters for the localization key.
     */
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
        localizationObject.add(
                DurationFormatUtils.formatDurationWords(displayTime, true, true));

        return key;
    }

    private static Long getRetryAfter(TaskType task) {
        XMLGregorianCalendar nextRun = task.getNextRetryTimestamp();
        return nextRun != null ? (XmlTypeConverter.toMillis(nextRun) - System.currentTimeMillis()) : null;
    }

    public static Long getScheduledToStartAgain(TaskType task) {
        long current = System.currentTimeMillis();

        if (task.getNodeAsObserved() != null && task.getSchedulingState() != TaskSchedulingStateType.SUSPENDED) {
            if (!isTaskRecurring(task)) {
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

    public static boolean isAutoScalingDisabled(TaskType task) {
        return task.getAutoScaling() != null && task.getAutoScaling().getMode() == TaskAutoScalingModeType.DISABLED;
    }

    static boolean isActivityBasedRoot(@NotNull TaskType task) {
        return task.getParent() == null &&
                task.getOid() != null &&
                hasStateOverview(task);
    }

    static boolean isActivityBasedPersistentSubtask(@NotNull TaskType task) {
        return task.getParent() != null &&
                task.getOid() != null &&
                task.getActivityState() != null &&
                task.getActivityState().getLocalRoot() != null;
    }

    /**
     * Determines recurrence that is explicitly specified for a task: either "new" or "legacy" variant.
     */
    public static @Nullable TaskRecurrenceType getSpecifiedRecurrence(@NotNull TaskType task) {
        if (task.getSchedule() != null && task.getSchedule().getRecurrence() != null) {
            return task.getSchedule().getRecurrence();
        } else {
            return null;
        }
    }

    /**
     * Determines effective value of task recurrence flag: if set explicitly (either new or legacy variant) we use that one,
     * otherwise we use schedule.interval/cronLikePattern presence.
     */
    public static @NotNull TaskRecurrenceType getEffectiveRecurrence(@NotNull TaskType task) {
        return Objects.requireNonNullElseGet(
                getSpecifiedRecurrence(task),
                () -> getImplicitRecurrence(task));
    }

    /**
     * Determines implicit task recurrence - from the schedule.interval/cronLikePattern presence.
     */
    private static @NotNull TaskRecurrenceType getImplicitRecurrence(@NotNull TaskType task) {
        if (task.getSchedule() != null &&
                    (task.getSchedule().getInterval() != null || task.getSchedule().getCronLikePattern() != null)) {
            return TaskRecurrenceType.RECURRING;
        } else {
            return TaskRecurrenceType.SINGLE;
        }
    }

    public static boolean isTaskRecurring(@NotNull TaskType task) {
        return getEffectiveRecurrence(task) == TaskRecurrenceType.RECURRING;
    }
}
