/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import com.evolveum.midpoint.repo.common.activity.Activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.task.GenericTaskExecution;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsible for updating the legacy progress for the current task.
 * (Based on structured progress of all local activities.)
 */
class LegacyProgressUpdater {

    @NotNull private final ActivityTree activityTree;
    @NotNull private final GenericTaskExecution taskExecution;

    public LegacyProgressUpdater(@NotNull ActivityTree activityTree, @NotNull GenericTaskExecution taskExecution) {
        this.activityTree = activityTree;
        this.taskExecution = taskExecution;
    }

    public static void update(@NotNull ActivityState<?> activityState) {

        AbstractActivityExecution<?, ?, ?> activityExecution = activityState.getActivityExecution();
        ActivityTree activityTree = activityExecution.getActivity().getTree();
        GenericTaskExecution taskExecution = activityExecution.getTaskExecution();

        new LegacyProgressUpdater(activityTree, taskExecution)
                .updateProgress();
    }

    private void updateProgress() {
        taskExecution.getRunningTask()
                .setProgress(
                        computeProgress());
    }

    private long computeProgress() {
        AtomicLong totalProgress = new AtomicLong();
        taskExecution.getLocalRootActivity()
                .accept(activity -> totalProgress.addAndGet(getActivityProgress(activity)));
        return totalProgress.get();
    }

    private long getActivityProgress(Activity<?, ?> activity) {
        AbstractActivityExecution<?, ?, ?> execution = activity.getExecution();
        if (execution == null) {
            return 0;
        } else {
            return execution.getActivityState().getLiveProgress().getLegacyValue();
        }
    }
}
