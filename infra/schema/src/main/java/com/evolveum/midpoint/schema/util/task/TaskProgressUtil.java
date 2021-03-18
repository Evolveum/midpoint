/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to task progress.
 */
public class TaskProgressUtil {

    public static int getProgressForOutcome(StructuredTaskProgressType info, ItemProcessingOutcomeType outcome, boolean open) {
        if (info != null) {
            return getCounts(info.getPart(), getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    public static int getProgressForOutcome(TaskPartProgressType part, ItemProcessingOutcomeType outcome, boolean open) {
        if (part != null) {
            return getCounts(singleton(part), getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    private static int getCounts(Collection<TaskPartProgressType> parts,
            Predicate<OutcomeKeyedCounterType> counterFilter, boolean open) {
        return parts.stream()
                .flatMap(part -> (open ? part.getOpen() : part.getClosed()).stream())
                .filter(Objects::nonNull)
                .filter(counterFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    public static String getProgressDescription(TaskType task, List<Object> localizationObject) {
        Long stalledSince = task.getStalledSince() != null ? XmlTypeConverter.toMillis(task.getStalledSince()) : null;
        if (stalledSince != null) {
            localizationObject.add(new Date(stalledSince).toString());
            localizationObject.add(getRealProgressDescription(task));
            return "pageTasks.stalledSince";
        } else {
            return getRealProgressDescription(task);
        }
    }

    private static String getRealProgressDescription(TaskType task) {
        if (TaskWorkStateUtil.isWorkStateHolder(task)) {
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
        return TaskWorkStateUtil.getCompleteBucketsNumber(taskType);
    }

    public static String getPlainTaskProgressDescription(TaskType taskType) {
        Long currentProgress = taskType.getProgress();
        if (currentProgress == null && taskType.getExpectedTotal() == null) {
            return ""; // the task handler probably does not report progress at all
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

    private static Predicate<OutcomeKeyedCounterType> getCounterFilter(ItemProcessingOutcomeType outcome) {
        switch (outcome) {
            case SUCCESS:
                return OutcomeKeyedCounterTypeUtil::isSuccess;
            case FAILURE:
                return OutcomeKeyedCounterTypeUtil::isFailure;
            case SKIP:
                return OutcomeKeyedCounterTypeUtil::isSkip;
            default:
                throw new AssertionError(outcome);
        }
    }

    public static int getTotalProgress(TaskPartProgressType progress) {
        return getTotalProgressOpen(progress) + getTotalProgressClosed(progress);
    }

    private static int getTotalProgressClosed(TaskPartProgressType progress) {
        return getCounts(singleton(progress), c -> true, false);
    }

    public static int getTotalProgressOpen(TaskPartProgressType progress) {
        return getCounts(singleton(progress), c -> true, true);
    }

    public static int getTotalProgressClosed(StructuredTaskProgressType progress) {
        if (progress == null) {
            return 0;
        }
        return getCounts(progress.getPart(), c -> true, false);
    }

    public static int getTotalProgressOpen(StructuredTaskProgressType progress) {
        if (progress == null) {
            return 0;
        }
        return getCounts(progress.getPart(), c -> true, true);
    }

    public static TaskPartProgressType getCurrentPart(StructuredTaskProgressType progress) {
        return progress.getPart().stream()
                .filter(part -> Objects.equals(part.getPartUri(), progress.getCurrentPartUri()))
                .findAny().orElse(null);
    }

    public static int getTotalProgressForCurrentPart(StructuredTaskProgressType progress) {
        if (progress == null) {
            return 0;
        }
        TaskPartProgressType currentPart = getCurrentPart(progress);
        if (currentPart == null) {
            return 0;
        }
        return getTotalProgress(currentPart);
    }

    /**
     * Returns a value suitable for storing in task.progress property.
     */
    public static long getTotalProgress(StructuredTaskProgressType progress) {
        return getTotalProgressOpen(progress) + getTotalProgressClosed(progress);
    }
}
