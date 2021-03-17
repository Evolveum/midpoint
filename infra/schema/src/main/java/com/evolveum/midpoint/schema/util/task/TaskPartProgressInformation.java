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
}
