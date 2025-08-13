/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedItemType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySimplifiedRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.Nullable;

/**
 * Summarized representation of a progress of an activity and its sub-activities.
 *
 * Examples:
 *
 * - 23% in 1/3
 * - 23% in 2/2 in 2/3
 *
 * TODO optimize task reading: avoid doing that for completed subtasks
 *
 * TODO i8n
 *
 * NOTE: User-oriented information on the activity can be obtained by resolving the following keys (for example):
 *
 * - Activity.name.shadows-collection="Shadows collection"
 * - Activity.explanation.shadows-collection="Collecting correlated shadows"
 *
 * The first one is used as the traditional activity name (with wording like "User recomputation", "Resource reconciliation", ...)
 * while the second one is used to display the activity in the task progress panel - what is the task doing at the moment.
 */
public class ActivityProgressInformation implements DebugDumpable, Serializable {

    /** Activity identifier. */
    @Nullable private final String activityIdentifier;

    /** Activity path. Provided here for quick orientation. Consider removing it. */
    @NotNull private final ActivityPath activityPath;

    /** Display order of the activity among its siblings. */
    @Nullable private final Integer displayOrder;

    /** Mainly to know if the activity is complete. Null means "not started yet". */
    @Nullable private final RealizationState realizationState;

    /**
     * Progress in the language of buckets.
     * (Filled-in also for single-bucket activities, although it provides no usable information for them.)
     *
     * Ignored if there are children.
     */
    private final BucketsProgressInformation bucketsProgress;

    /**
     * Progress in the language of items (total).
     *
     * Ignored if there are children.
     */
    private final ItemsProgressInformation itemsProgress;

    /** Item or items being currently processed, if any. Null means "we don't know". */
    @Nullable private final Collection<ProcessedItemType> itemsBeingProcessed;

    @NotNull final List<ActivityProgressInformation> children = new ArrayList<>();

    ActivityProgressInformation(
            @Nullable String activityIdentifier,
            @NotNull ActivityPath activityPath,
            @Nullable Integer displayOrder,
            @Nullable RealizationState realizationState,
            BucketsProgressInformation bucketsProgress,
            ItemsProgressInformation itemsProgress,
            @Nullable Collection<ProcessedItemType> itemsBeingProcessed) {
        this.activityIdentifier = activityIdentifier;
        this.activityPath = activityPath;
        this.displayOrder = displayOrder;
        this.realizationState = realizationState;
        this.bucketsProgress = bucketsProgress;
        this.itemsProgress = itemsProgress;
        this.itemsBeingProcessed = itemsBeingProcessed;
    }

    static @NotNull ActivityProgressInformation unknown(String activityIdentifier, ActivityPath activityPath) {
        return new ActivityProgressInformation(
                activityIdentifier, activityPath, null, RealizationState.UNKNOWN,
                null, null, null);
    }

    /** Identifier is estimated from the path. Use only if it needs not be precise. */
    static @NotNull ActivityProgressInformation unknown(ActivityPath activityPath) {
        return unknown(
                activityPath.isEmpty() ? activityPath.last() : null,
                activityPath);
    }

    /**
     * Prepares the information from a root task. The task may or may not have its children resolved.
     */
    public static @NotNull ActivityProgressInformation fromRootTask(@NotNull TaskType task, @NotNull InformationSource source) {
        return fromRootTask(task, TaskResolver.empty(), source);
    }

    /**
     * Prepares the information from a root task. The task may or may not have its children resolved.
     *
     * Note: the `resolver` parameter is dubious. Consider removing it.
     */
    public static @NotNull ActivityProgressInformation fromRootTask(@NotNull TaskType task,
            @NotNull TaskResolver resolver, @NotNull InformationSource source) {
        return ActivityProgressInformationBuilder.fromTask(task, ActivityPath.empty(), resolver, source);
    }

    public @Nullable String getActivityIdentifier() {
        return activityIdentifier;
    }

    public @NotNull ActivityPath getActivityPath() {
        return activityPath;
    }

    public @Nullable RealizationState getRealizationState() {
        return realizationState;
    }

    public BucketsProgressInformation getBucketsProgress() {
        return bucketsProgress;
    }

    public ItemsProgressInformation getItemsProgress() {
        return itemsProgress;
    }

    public @NotNull List<ActivityProgressInformation> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "identifier=" + activityIdentifier +
                ", path=" + activityPath +
                ", displayOrder=" + displayOrder +
                ", state=" + realizationState +
                ", bucketsProgress=" + bucketsProgress +
                ", totalItemsProgress=" + itemsProgress +
                ", itemsBeingProcessed=" + getItemsBeingProcessedAsString() +
                ", children: " + children.size() +
                '}';
    }

    private String getItemsBeingProcessedAsString() {
        if (itemsBeingProcessed == null) {
            return null;
        } else {
            return itemsBeingProcessed.stream()
                    .map(i -> i.getName())
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    @Override
    public String debugDump(int indent) {
        String title = String.format("%s for %s (identifier %s): %s", getClass().getSimpleName(), activityPath.toDebugName(),
                activityIdentifier, toHumanReadableString(false));
        StringBuilder sb = DebugUtil.createTitleStringBuilder(title, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "Display order", displayOrder, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Human readable string (long)", toHumanReadableString(true), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Realization state", realizationState, indent + 1);
        if (bucketsProgress != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Buckets progress", bucketsProgress, indent + 1);
        }
        if (itemsProgress != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Total items progress", itemsProgress, indent + 1);
        }
        if (itemsBeingProcessed != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Item(s) being processed", itemsBeingProcessed, indent + 1);
        } else {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Item(s) being processed", "unknown", indent + 1);
        }
        if (!children.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "Children", children, indent + 1);
        }
        return sb.toString();
    }

    // TODO create also toLocalizedString method that will provide "stalled since" information as well

    public String toHumanReadableString(boolean longForm) {
        if (children.isEmpty()) {
            return toHumanReadableStringForLeaf(longForm);
        } else {
            return toHumanReadableStringForNonLeaf(longForm);
        }
    }

    private String toHumanReadableStringForLeaf(boolean longForm) {
        if (isNotStarted()) {
            return "Not started";
        } else if (shouldUseBucketForProgressReporting()) {
            return toHumanReadableStringForBucketed(longForm);
        } else if (itemsProgress != null) {
            return toHumanReadableStringForNonBucketed(longForm);
        } else {
            return isComplete() ? "Complete" : "Processing";
        }
    }

    private boolean shouldUseBucketForProgressReporting() {
        if (bucketsProgress == null) {
            return false;
        }
        if (bucketsProgress.getExpectedBuckets() != null) {
            // - If > 1: There are some buckets expected. Even if it is a small number, we consider the task as bucketed.
            // - Otherwise: A single bucket. There is no point in showing performance information in buckets for such tasks.
            //   We will use items progress instead.
            return bucketsProgress.getExpectedBuckets() > 1;
        } else {
            // We don't know how many buckets to expect. So let's guess according to buckets completed so far.
            return bucketsProgress.getCompleteBuckets() > 1;
        }
    }

    private String toHumanReadableStringForBucketed(boolean longForm) {
        float percentage = bucketsProgress.getPercentage();
        if (Float.isNaN(percentage)) {
            if (longForm) {
                return bucketsProgress.getCompleteBuckets() + " buckets";
            } else {
                return bucketsProgress.getCompleteBuckets() + " buckets"; // at least temporarily until we find something better
            }
        }
        if (longForm) {
            return String.format("%.1f%% (%d of %d buckets)", percentage * 100,
                    bucketsProgress.getCompleteBuckets(), bucketsProgress.getExpectedBuckets());
        } else {
            return String.format("%.1f%%", percentage * 100);
        }
    }

    private String toHumanReadableStringForNonBucketed(boolean longForm) {
        return itemsProgress.toHumanReadableString(longForm);
    }

    private String toHumanReadableStringForNonLeaf(boolean longForm) {
        if (children.size() == 1) {
            return children.get(0).toHumanReadableString(longForm);
        }

        if (isComplete()) {
            return "100.0%";
        }

        List<String> partials = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            ActivityProgressInformation child = children.get(i);
            if (child.isInProgress()) {
                partials.add(child.toHumanReadableString(longForm) + " " + getPositionSuffix(i, longForm));
            } else if (child.isUnknown()) {
                partials.add("? " + getPositionSuffix(i, longForm));
            }
        }

        if (partials.isEmpty()) {
            return "?"; // something strange
        } else {
            return String.join(" & ", partials);
        }
    }

    private String getPositionSuffix(int i, boolean longForm) {
        return longForm
                ? String.format("in %d of %d", i + 1, children.size())
                : String.format("in %d/%d", i + 1, children.size());
    }

    public boolean isInProgress() {
        return realizationState == RealizationState.IN_PROGRESS;
    }

    private boolean isUnknown() {
        return realizationState == RealizationState.UNKNOWN;
    }

    public boolean isComplete() {
        return realizationState == RealizationState.COMPLETE;
    }

    public boolean isNotStarted() {
        return realizationState == null;
    }

    public void checkConsistence() {
        if (bucketsProgress != null) {
            bucketsProgress.checkConsistence();
        }
        if (itemsProgress != null) {
            itemsProgress.checkConsistence();
        }
    }

    public @Nullable Integer getDisplayOrder() {
        return displayOrder;
    }

    public ActivityProgressInformation getChild(String identifier) {
        return children.stream()
                .filter(c -> java.util.Objects.equals(c.getActivityIdentifier(), identifier))
                .findFirst().orElse(null);
    }

    public int getErrorsRecursive() {
        return getErrors() +
                children.stream()
                        .mapToInt(ActivityProgressInformation::getErrorsRecursive)
                        .sum();
    }

    public int getErrors() {
        return itemsProgress != null ? itemsProgress.getErrors() : 0;
    }

    public ActivityProgressInformation find(ActivityPath activityPath) {
        ActivityProgressInformation current = this;
        for (String identifier : activityPath.getIdentifiers()) {
            current = current.getChild(identifier);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public enum RealizationState {
        /**
         * The activity is in progress: it was started but not completed yet.
         * It may or may not be executing at this moment.
         */
        IN_PROGRESS,

        /**
         * The activity is complete.
         */
        COMPLETE,

        /**
         * The state and progress of the activity is unknown. For example, the task it was delegated to is no longer available.
         * Or, the activity is not started yet.
         */
        UNKNOWN;

        static RealizationState fromOverview(ActivitySimplifiedRealizationStateType state) {
            if (state == null) {
                return null;
            }
            return switch (state) {
                case IN_PROGRESS -> IN_PROGRESS;
                case COMPLETE -> COMPLETE;
            };
        }

        static RealizationState fromFullState(ActivityRealizationStateType state) {
            if (state == null) {
                return null;
            } else if (state == ActivityRealizationStateType.COMPLETE) {
                return RealizationState.COMPLETE;
            } else {
                // Variants of "in progress" (local, delegated, distributed)
                return RealizationState.IN_PROGRESS;
            }
        }
    }
}
