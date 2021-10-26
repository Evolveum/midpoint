/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.repo.common.activity.Activity;

import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsible for updating the legacy progress for the current task.
 * (Based on structured progress of all local activities.)
 */
public class LegacyProgressUpdater {

    @NotNull private final ActivityBasedTaskRun taskRun;

    private LegacyProgressUpdater(@NotNull ActivityBasedTaskRun taskRun) {
        this.taskRun = taskRun;
    }

    public static void update(@NotNull CurrentActivityState<?> activityState) {
        AbstractActivityRun<?, ?, ?> activityRun = activityState.getActivityRun();
        ActivityBasedTaskRun taskRun = activityRun.getTaskRun();

        new LegacyProgressUpdater(taskRun)
                .updateProgress();
    }

    public static long compute(@NotNull ActivityBasedTaskRun taskRun) {
        return new LegacyProgressUpdater(taskRun)
                .computeProgress();
    }

    private void updateProgress() {
        taskRun.getRunningTask()
                .setLegacyProgress(
                        computeProgress());
    }

    private long computeProgress() {
        AtomicLong totalProgress = new AtomicLong();
        taskRun.getLocalRootActivity()
                .accept(activity -> totalProgress.addAndGet(getActivityProgress(activity)));
        return totalProgress.get();
    }

    // TODO what about completed activities?
    private long getActivityProgress(Activity<?, ?> activity) {
        AbstractActivityRun<?, ?, ?> run = activity.getRun();
        if (run == null) {
            return 0;
        } else {
            return run.getActivityState().getLiveProgress().getLegacyValue();
        }
    }
}
