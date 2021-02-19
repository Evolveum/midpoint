/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class TaskUtil {

    public static final long RUNS_CONTINUALLY = -1L;
    public static final long ALREADY_PASSED = -2L;
    public static final long NOW = 0L;

    public static List<String> tasksToOids(List<? extends Task> tasks) {
        return tasks.stream().map(Task::getOid).collect(Collectors.toList());
    }

    public static Task findByIdentifier(@NotNull String identifier, @NotNull Collection<Task> tasks) {
        return tasks.stream().filter(t -> identifier.equals(t.getTaskIdentifier())).findFirst().orElse(null);
    }

    /**
     * The methods below should be in class like "TaskUtilForProvisioning". For simplicity let's keep them here for now.
      */
    public static boolean isDryRun(Task task) throws SchemaException {
        return findExtensionItemValueInThisOrParent(task, SchemaConstants.MODEL_EXTENSION_DRY_RUN, false);
    }

    public static boolean findExtensionItemValueInThisOrParent(Task task, QName path, boolean defaultValue) throws SchemaException {
        Boolean rawValue = findExtensionItemValueInThisOrParent(task, path);
        return rawValue != null ? rawValue : defaultValue;
    }

    private static Boolean findExtensionItemValueInThisOrParent(Task task, QName path) throws SchemaException {
        Boolean value = findExtensionItemValue(task, path);
        if (value != null) {
            return value;
        }
        if (task instanceof RunningTask) {
            RunningTask runningTask = (RunningTask) task;
            if (runningTask.isLightweightAsynchronousTask() && runningTask.getParentForLightweightAsynchronousTask() != null) {
                return findExtensionItemValue(runningTask.getParentForLightweightAsynchronousTask(), path);
            }
        }
        return null;
    }

    private static Boolean findExtensionItemValue(Task task, QName path) throws SchemaException {
        Validate.notNull(task, "Task must not be null.");
        PrismProperty<Boolean> item = task.getExtensionPropertyOrClone(ItemName.fromQName(path));
        if (item == null || item.isEmpty()) {
            return null;
        }
        if (item.getValues().size() > 1) {
            throw new SchemaException("Unexpected number of values for option '" + path + "'.");
        }
        return item.getValues().iterator().next().getValue();
    }

    public static String createScheduledToRunAgain(TaskType task, List<Object> localizationObject) {
        boolean runnable = task.getExecutionStatus() == TaskExecutionStateType.RUNNABLE || task.getExecutionStatus() == TaskExecutionStateType.RUNNING; // TODO switch to scheduling state
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
        Long retryAt = nextRun != null ? (XmlTypeConverter.toMillis(nextRun) - System.currentTimeMillis()) : null;
        return retryAt;
    }

    public static Long getScheduledToStartAgain(TaskType task) {
        long current = System.currentTimeMillis();

        if (task.getNodeAsObserved() != null && (task.getExecutionStatus() != TaskExecutionStateType.SUSPENDED)) {

            if (TaskRecurrenceType.RECURRING != task.getRecurrence()) {
                return null;
            } else if (TaskBindingType.TIGHT == task.getBinding()) {
                return RUNS_CONTINUALLY;             // runs continually; todo provide some information also in this case
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

    public static String getProgressDescription(TaskType task, List<Object> localizationObject) {
        Long stalledSince = task.getStalledSince() != null ? XmlTypeConverter.toMillis(task.getStalledSince()) : null;
        if (stalledSince != null) {
            localizationObject.add(new Date(stalledSince).toLocaleString());
            localizationObject.add(getRealProgressDescription(task));
            return "pageTasks.stalledSince";
        } else {
            return getRealProgressDescription(task);
        }
    }

    private static String getRealProgressDescription(TaskType task) {
        if (TaskTypeUtil.isWorkStateHolder(task)) {
            return getBucketedTaskProgressDescription(task);
        } else {
            return getPlainTaskProgressDescription(task);
        }
    }

    private static String getBucketedTaskProgressDescription(TaskType taskType) {
        int completeBuckets = getCompleteBuckets(taskType);
        Integer expectedBuckets = getExpectedBuckets(taskType);
        if (expectedBuckets == null) {
            return String.valueOf(completeBuckets);
        } else {
            return (completeBuckets*100/expectedBuckets) + "%";
        }
    }

    private static Integer getExpectedBuckets(TaskType taskType) {
        return taskType.getWorkState() != null ? taskType.getWorkState().getNumberOfBuckets() : null;
    }

    private static Integer getCompleteBuckets(TaskType taskType) {
        return TaskWorkStateTypeUtil.getCompleteBucketsNumber(taskType);
    }


    private static String getPlainTaskProgressDescription(TaskType taskType) {
        Long currentProgress = taskType.getProgress();
        if (currentProgress == null && taskType.getExpectedTotal() == null) {
            return "";      // the task handler probably does not report progress at all
        } else {
            StringBuilder sb = new StringBuilder();
            if (currentProgress != null){
                sb.append(currentProgress);
            } else {
                sb.append("0");
            }
            if (taskType.getExpectedTotal() != null) {
                sb.append("/").append(taskType.getExpectedTotal());
            }
            return sb.toString();
        }
    }
}
