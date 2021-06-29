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
import java.util.stream.Stream;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to task progress.
 */
public class TaskProgressUtil {

    public static int getProgressForOutcome(StructuredTaskProgressType info, ItemProcessingOutcomeType outcome, boolean open) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil.getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    public static int getProgressForOutcome(TaskPartProgressOldType part, ItemProcessingOutcomeType outcome, boolean open) {
        if (part != null) {
            return getCounts(singleton(part), OutcomeKeyedCounterTypeUtil.getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    private static int getCounts(Collection<TaskPartProgressOldType> parts,
            Predicate<OutcomeKeyedCounterType> counterFilter, boolean open) {
        return parts.stream()
                .flatMap(part -> (open ? part.getOpen() : part.getClosed()).stream())
                .filter(Objects::nonNull)
                .filter(counterFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    @Deprecated // Use/adapt ActivityProgressInformation instead
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

    @Deprecated
    private static String getRealProgressDescription(TaskType task) {
        if (ActivityStateUtil.isWorkStateHolder(task)) {
            return getBucketedTaskProgressDescription(task);
        } else {
            return getPlainTaskProgressDescription(task);
        }
    }

    @Deprecated
    private static String getBucketedTaskProgressDescription(TaskType taskType) {
        int completeBuckets = getCompleteBuckets(taskType);
        Integer expectedBuckets = getExpectedBuckets(taskType);
        if (expectedBuckets == null) {
            return String.valueOf(completeBuckets);
        } else {
            return (completeBuckets*100/expectedBuckets) + "%";
        }
    }

    @Deprecated
    private static Integer getExpectedBuckets(TaskType taskType) {
        return null;//taskType.getWorkState() != null ? taskType.getWorkState().getNumberOfBuckets() : null;
    }

    @Deprecated
    private static Integer getCompleteBuckets(TaskType taskType) {
        return null; //BucketingUtil.getCompleteBucketsNumber(taskType);
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

    public static int getTotalProgress(TaskPartProgressOldType progress) {
        return getTotalProgressOpen(progress) + getTotalProgressClosed(progress);
    }

    private static int getTotalProgressClosed(TaskPartProgressOldType progress) {
        return getCounts(singleton(progress), c -> true, false);
    }

    public static int getTotalProgressOpen(TaskPartProgressOldType progress) {
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

    public static TaskPartProgressOldType getForCurrentPart(StructuredTaskProgressType progress) {
        if (progress == null) {
            return null;
        } else {
            return getForPart(progress, progress.getCurrentPartUri());
        }
    }

    public static TaskPartProgressOldType getForPart(StructuredTaskProgressType progress, String partUri) {
        if (progress == null) {
            return null;
        } else {
            return progress.getPart().stream()
                    .filter(part -> Objects.equals(part.getPartUri(), partUri))
                    .findAny().orElse(null);
        }
    }

    public static int getTotalProgressForCurrentPart(StructuredTaskProgressType progress) {
        TaskPartProgressOldType currentPart = getForCurrentPart(progress);
        return currentPart != null ? getTotalProgress(currentPart) : 0;
    }

    public static int getTotalProgressForPart(StructuredTaskProgressType progress, String partUri) {
        TaskPartProgressOldType forPart = getForPart(progress, partUri);
        return forPart != null ? getTotalProgress(forPart) : 0;
    }

    /**
     * Returns a value suitable for storing in task.progress property.
     */
    public static long getTotalProgress(StructuredTaskProgressType progress) {
        return getTotalProgressOpen(progress) + getTotalProgressClosed(progress);
    }

    public static String getCurrentPartUri(StructuredTaskProgressType structuredTaskProgress) {
        return structuredTaskProgress != null ? structuredTaskProgress.getCurrentPartUri() : null;
    }

    @Experimental
    public static StructuredTaskProgressType getStructuredProgressFromTree(TaskType task) {
        StructuredTaskProgressType aggregate = new StructuredTaskProgressType(PrismContext.get());
        Stream<TaskType> subTasks = TaskTreeUtil.getAllTasksStream(task);
        subTasks.forEach(subTask -> {
            StructuredTaskProgressType progress = null;//subTask.getStructuredProgress();
            if (progress != null) {
                StructuredTaskProgress.addTo(aggregate, progress);
            }
        });
        return aggregate;
    }
}
