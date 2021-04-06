/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.io.Serializable;

public class TaskPartProgressInformation implements DebugDumpable, Serializable {

    /**
     * Part URI (in case of internally partitioned tasks) or handler URI (for physically partitioned tasks).
     *
     * TODO reconsider
     */
    private final String partOrHandlerUri;

    /**
     * Is this part complete?
     */
    private final boolean complete;

    /**
     * Progress in the language of buckets.
     * (Filled-in also for single-bucket tasks, although it provides no usable information for them.)
     */
    private final BucketsProgressInformation bucketsProgress;

    /**
     * Progress in the language of items (total).
     */
    private final ItemsProgressInformation itemsProgress;

    private TaskPartProgressInformation(String partOrHandlerUri, boolean complete, BucketsProgressInformation bucketsProgress,
            ItemsProgressInformation itemsProgress) {
        this.partOrHandlerUri = partOrHandlerUri;
        this.complete = complete;
        this.bucketsProgress = bucketsProgress;
        this.itemsProgress = itemsProgress;
    }

    /**
     * Task is a persistent subtask of partitioned master.
     * It can be bucketed. But we assume no structured progress here.
     */
    public static TaskPartProgressInformation fromPersistentSubtask(TaskType task) {
        String partUri = task.getHandlerUri();
        boolean complete = task.getExecutionStatus() == TaskExecutionStateType.CLOSED;
        BucketsProgressInformation bucketsProgress = BucketsProgressInformation.fromWorkState(task.getWorkState());
        ItemsProgressInformation itemsProgress = ItemsProgressInformation.fromTask(task);
        return new TaskPartProgressInformation(partUri, complete, bucketsProgress, itemsProgress);
    }

    /**
     * Owning task is internally partitioned. We assume no buckets nor subtasks here.
     */
    public static TaskPartProgressInformation fromPartProgress(TaskPartProgressType progress, Integer expectedTotal) {
        String partUri = progress.getPartUri();
        boolean complete = Boolean.TRUE.equals(progress.isComplete());
        int totalProgress = TaskProgressUtil.getTotalProgress(progress);
        ItemsProgressInformation itemsProgress = ItemsProgressInformation.create(totalProgress, expectedTotal);
        return new TaskPartProgressInformation(partUri, complete, null, itemsProgress);
    }

    /**
     * Task is not internally partitioned. It can be bucketed and can have subtasks.
     */
    static TaskPartProgressInformation fromSimpleTask(TaskType task) {
        boolean complete = TaskWorkStateUtil.isAllWorkComplete(task);
        BucketsProgressInformation bucketsProgress = BucketsProgressInformation.fromWorkState(task.getWorkState());
        ItemsProgressInformation itemsProgress = ItemsProgressInformation.fromTask(task);
        return new TaskPartProgressInformation(null, complete, bucketsProgress, itemsProgress);
    }

    public String getPartOrHandlerUri() {
        return partOrHandlerUri;
    }

    public boolean isComplete() {
        return complete;
    }

    public BucketsProgressInformation getBucketsProgress() {
        return bucketsProgress;
    }

    public ItemsProgressInformation getItemsProgress() {
        return itemsProgress;
    }

    @Override
    public String toString() {
        return "TaskPartProgressInformation{" +
                "partUri=" + partOrHandlerUri +
                ",complete=" + complete +
                ", bucketsProgress=" + bucketsProgress +
                ", totalItemsProgress=" + itemsProgress +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Part URI", partOrHandlerUri, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Complete", complete, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Buckets progress", bucketsProgress, indent);
        DebugUtil.debugDumpWithLabel(sb, "Total items progress", itemsProgress, indent);
        return sb.toString();
    }

    /**
     * Related to {@link TaskWorkStateUtil#hasBuckets(TaskType)}. But unfortunately we do not have the full information
     * here, in particular we don't see the actual buckets. This should be probably fixed in the pre-processing phase
     * i.e. when {@link TaskPartProgressInformation} is created.
     */
    @SuppressWarnings("RedundantIfStatement")
    private boolean shouldUseBucketForProgressReporting() {
        if (bucketsProgress == null) {
            return false;
        }
        if (bucketsProgress.getExpectedBuckets() != null) {
            if (bucketsProgress.getExpectedBuckets() > 1) {
                // There are some buckets expected. Even if it is a small number, we consider the task as bucketed.
                return true;
            } else {
                // A single bucket. There is no point in showing performance information in buckets for such tasks.
                // We will use items progress instead.
                return false;
            }
        } else {
            // We don't know how many buckets to expect. So let's guess according to buckets completed so far.
            return bucketsProgress.getCompletedBuckets() > 1;
        }
    }

    public String toHumanReadableString(boolean longForm) {
        if (shouldUseBucketForProgressReporting()) {
            return toHumanReadableStringForBucketed(longForm);
        } else if (itemsProgress != null) {
            return toHumanReadableStringForNonBucketed(longForm);
        } else {
            return "?"; // TODO
        }
    }

    private String toHumanReadableStringForNonBucketed(boolean longForm) {
        float percentage = itemsProgress.getPercentage();
        if (Float.isNaN(percentage)) {
            return String.valueOf(itemsProgress.getProgress());
        }
        if (longForm) {
            return String.format("%.1f%% (%d of %d)", percentage * 100,
                    itemsProgress.getProgress(), itemsProgress.getExpectedTotal());
        } else {
            return String.format("%.1f%%", percentage * 100);
        }
    }

    private String toHumanReadableStringForBucketed(boolean longForm) {
        float percentage = bucketsProgress.getPercentage();
        if (Float.isNaN(percentage)) {
            if (longForm) {
                return bucketsProgress.getCompletedBuckets() + " buckets";
            } else {
                return bucketsProgress.getCompletedBuckets() + "b";
            }
        }
        if (longForm) {
            return String.format("%.1f%% (%d of %d buckets)", percentage * 100,
                    bucketsProgress.getCompletedBuckets(), bucketsProgress.getExpectedBuckets());
        } else {
            return String.format("%.1f%%", percentage * 100);
        }
    }

    public void checkConsistence() {
        if (bucketsProgress != null) {
            bucketsProgress.checkConsistence();
        }
        if (itemsProgress != null) {
            itemsProgress.checkConsistence();
        }
    }
}
