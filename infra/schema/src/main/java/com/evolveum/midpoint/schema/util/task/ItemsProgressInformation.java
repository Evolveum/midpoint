/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static com.evolveum.midpoint.util.MiscUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.HIDDEN;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task progress counted in items.
 */
public class ItemsProgressInformation implements DebugDumpable, Serializable {

    /**
     * Number of items processed.
     */
    private final int progress;

    /**
     * Number of items whose processing resulted in an error.
     */
    private final int errors;

    /**
     * Expected total number of items, if known.
     */
    private final Integer expectedProgress;

    private ItemsProgressInformation(int progress, int errors, Integer expectedProgress) {
        this.progress = progress;
        this.errors = errors;
        this.expectedProgress = expectedProgress;
    }

    public static @Nullable ItemsProgressInformation fromOverview(@NotNull ActivityStateOverviewType overview) {
        if (overview.getTask().isEmpty() || overview.getProgressInformationVisibility() == HIDDEN) {
            return null;
        }

        Accumulator accumulator = new Accumulator();
        overview.getTask().forEach(t -> accumulator.add(t.getProgress()));
        return accumulator.toProgressInformation();
    }

    static ItemsProgressInformation fromFullState(@NotNull ActivityStateType state,
            @NotNull ActivityPath activityPath, @NotNull TaskType task, @NotNull TaskResolver resolver) {
        if (BucketingUtil.isCoordinator(state)) {
            return fromBucketingCoordinator(activityPath, task, resolver);
        } else {
            return fromSingleFullState(state);
        }
    }

    private static ItemsProgressInformation fromSingleFullState(ActivityStateType state) {
        if (state == null || state.getProgress() == null) {
            return null;
        }

        ActivityProgressType progress = state.getProgress();
        return new ItemsProgressInformation(
                ActivityProgressUtil.getCurrentProgress(progress),
                ActivityProgressUtil.getCurrentErrors(progress),
                progress.getExpectedTotal());
    }

    /**
     * We can obtain items processed from bucketing coordinator by summarizing the progress from its children.
     * (Related to given activity!)
     */
    private static ItemsProgressInformation fromBucketingCoordinator(ActivityPath activityPath,
            TaskType task, TaskResolver resolver) {
        return new ItemsProgressInformation(
                ActivityTreeUtil.getSubtasksForPath(task, activityPath, resolver).stream()
                        .mapToInt(subtask -> ActivityProgressUtil.getCurrentProgress(subtask, activityPath))
                        .sum(),
                ActivityTreeUtil.getSubtasksForPath(task, activityPath, resolver).stream()
                        .mapToInt(subtask -> ActivityProgressUtil.getCurrentErrors(subtask, activityPath))
                        .sum(),
                null); // TODO use expectedProgress when available for bucketed coordinators
    }

    static ItemsProgressInformation fromLegacyTask(@NotNull TaskType task) {
        return new ItemsProgressInformation(
                (int) or0(task.getProgress()),
                0,
                toInteger(task.getExpectedTotal()));
    }

    public int getProgress() {
        return progress;
    }

    public int getErrors() {
        return errors;
    }

    public Integer getExpectedProgress() {
        return expectedProgress;
    }

    @Override
    public String toString() {
        return "ItemsProgressInformation{" +
                "progress=" + progress +
                ", expected=" + expectedProgress +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Progress", progress, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Errors", errors, indent);
        DebugUtil.debugDumpWithLabel(sb, "Expected progress", expectedProgress, indent);
        return sb.toString();
    }

    public float getPercentage() {
        if (expectedProgress != null && expectedProgress > 0) {
            return (float) progress / expectedProgress;
        } else {
            return Float.NaN;
        }
    }

    public void checkConsistence() {
        stateCheck(expectedProgress == null || progress <= expectedProgress,
                "There are more completed items (%s) than expected total (%s)", progress, expectedProgress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemsProgressInformation that = (ItemsProgressInformation) o;
        return progress == that.progress &&
                errors == that.errors &&
                Objects.equals(expectedProgress, that.expectedProgress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(progress, errors, expectedProgress);
    }

    public @NotNull String toHumanReadableString(boolean longForm) {
        float percentage = getPercentage();
        if (Float.isNaN(percentage)) {
            return String.valueOf(progress);
        }
        if (longForm) {
            return String.format("%.1f%% (%d of %d)", percentage * 100, progress, expectedProgress);
        } else {
            return String.format("%.1f%%", percentage * 100);
        }
    }

    private static class Accumulator {

        boolean someProgressPresent;
        int progress;
        int errors;
        Integer expected;

        public void add(@Nullable ItemsProgressOverviewType overview) {
            if (overview != null) {
                someProgressPresent = true;
                progress += or0(overview.getSuccessfullyProcessed()) +
                        or0(overview.getFailed()) +
                        or0(overview.getSkipped());
                errors += or0(overview.getFailed());
                if (overview.getExpectedTotal() != null) {
                    expected = or0(expected) + overview.getExpectedTotal();
                }
            }
        }

        public ItemsProgressInformation toProgressInformation() {
            return someProgressPresent ?
                    new ItemsProgressInformation(progress, errors, expected) : null;
        }
    }
}
