/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Task progress counted in items.
 */
public class ItemsProgressInformation implements DebugDumpable, Serializable {

    /**
     * Number of items processed.
     */
    private final int progress;

    /**
     * Expected total number of items, if known.
     */
    private final Integer expectedTotal;

    private ItemsProgressInformation(int progress, Integer expectedTotal) {
        this.progress = progress;
        this.expectedTotal = expectedTotal;
    }

    public static ItemsProgressInformation create(int totalProgress, Integer expectedTotal) {
        return new ItemsProgressInformation(totalProgress, expectedTotal);
    }

//    public static ItemsProgressInformation fromTask(TaskType task) {
//        List<TaskType> allTasks = TaskTreeUtil.getAllTasksStream(task)
//                .filter(t -> t.getOid() != null)
//                .collect(Collectors.toList());
//        Integer expectedTotal;
//        if (task.getExpectedTotal() != null && !hasBuckets(task) && allTasks.size() <= 1) {
//            expectedTotal = task.getExpectedTotal().intValue();
//        } else {
//            expectedTotal = null;
//        }
//
//        int totalProgress = (int) allTasks.stream()
//                .map(TaskType::getProgress)
//                .filter(Objects::nonNull)
//                .mapToLong(value -> value)
//                .sum();
//        return new ItemsProgressInformation(totalProgress, expectedTotal);
//    }

    public static ItemsProgressInformation fromActivityState(ActivityStateType state) {
        if (state == null || state.getProgress() == null) {
            return null; // TODO ok?
        } else {
            // TODO expected total
            return new ItemsProgressInformation(
                    ActivityProgressUtil.getCurrentProgress(state.getProgress()), null);
        }
    }

    /**
     * We can obtain items processed from bucketing coordinator by summarizing the progress from its children.
     * (Related to given activity!)
     */
    static ItemsProgressInformation fromBucketingCoordinator(ActivityStateType state, ActivityPath activityPath,
            TaskType task, TaskResolver resolver) {
        return new ItemsProgressInformation(
                ActivityTreeUtil.getSubtasksForPath(task, activityPath, resolver).stream()
                        .mapToInt(subtask -> ActivityProgressUtil.getCurrentProgress(subtask, activityPath))
                        .sum(),
                null);
    }

    public int getProgress() {
        return progress;
    }

    public Integer getExpectedTotal() {
        return expectedTotal;
    }

    @Override
    public String toString() {
        return "ItemsProgressInformation{" +
                "progress=" + progress +
                ", expectedTotal=" + expectedTotal +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Progress", progress, indent);
        DebugUtil.debugDumpWithLabel(sb, "Expected total", expectedTotal, indent);
        return sb.toString();
    }

    public float getPercentage() {
        if (expectedTotal != null && expectedTotal > 0) {
            return (float) progress / expectedTotal;
        } else {
            return Float.NaN;
        }
    }

    public void checkConsistence() {
        stateCheck(expectedTotal == null || progress <= expectedTotal,
                "There are more completed items (%s) than expected total (%s)", progress, expectedTotal);
    }
}
