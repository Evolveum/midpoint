/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
 */
public class ActivityProgressInformation implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityProgressInformation.class);

    /**
     * Activity identifier.
     */
    private final String activityIdentifier;

    /** Activity path. */
    @NotNull private final ActivityPath activityPath;

    /**
     * Is this activity complete?
     */
    private final RealizationState realizationState;

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

    @NotNull private final List<ActivityProgressInformation> children = new ArrayList<>();

    private ActivityProgressInformation(String activityIdentifier, @NotNull ActivityPath activityPath,
            RealizationState realizationState, BucketsProgressInformation bucketsProgress,
            ItemsProgressInformation itemsProgress) {
        this.activityIdentifier = activityIdentifier;
        this.activityPath = activityPath;
        this.realizationState = realizationState;
        this.bucketsProgress = bucketsProgress;
        this.itemsProgress = itemsProgress;
    }

    private static @NotNull ActivityProgressInformation unknown(String activityIdentifier, ActivityPath activityPath) {
        return new ActivityProgressInformation(activityIdentifier, activityPath, RealizationState.UNKNOWN, null, null);
    }

    /**
     * Prepares the information from a root task. The task may or may not have its children resolved.
     */
    public static @NotNull ActivityProgressInformation fromRootTask(@NotNull TaskType task, @NotNull TaskResolver resolver) {
        return fromTask(task, ActivityPath.empty(), resolver);
    }

    public static @NotNull ActivityProgressInformation fromTask(@NotNull TaskType task, @NotNull ActivityPath activityPath,
            @NotNull TaskResolver resolver) {
        TaskActivityStateType globalState = task.getActivityState();
        if (globalState == null) {
            return unknown(null, activityPath); // TODO or "no progress"?
        }
        ActivityStateType rootActivityState = globalState.getActivity();
        if (rootActivityState == null) {
            return unknown(null, activityPath); // TODO or "no progress"?
        }
        return fromDelegatableActivityState(rootActivityState, activityPath, task, resolver);
    }

    private static @NotNull ActivityProgressInformation fromDelegatableActivityState(@NotNull ActivityStateType state,
            @NotNull ActivityPath activityPath, @NotNull TaskType task, @NotNull TaskResolver resolver) {
        if (ActivityStateUtil.isDelegated(state)) {
            return fromDelegatedActivityState(state.getIdentifier(), activityPath, getDelegatedTaskRef(state), task, resolver);
        } else {
            return fromNotDelegatedActivityState(state, activityPath, task, resolver);
        }
    }

    private static ObjectReferenceType getDelegatedTaskRef(ActivityStateType state) {
        AbstractActivityWorkStateType workState = state.getWorkState();
        return workState instanceof DelegationWorkStateType ? ((DelegationWorkStateType) workState).getTaskRef() : null;
    }

    private static @NotNull ActivityProgressInformation fromDelegatedActivityState(String activityIdentifier,
            @NotNull ActivityPath activityPath, ObjectReferenceType delegateTaskRef,
            @NotNull TaskType task, @NotNull TaskResolver resolver) {
        TaskType delegateTask = getSubtask(delegateTaskRef, task, resolver);
        if (delegateTask != null) {
            return fromTask(delegateTask, activityPath, resolver);
        } else {
            return unknown(activityIdentifier, activityPath);
        }
    }

    private static TaskType getSubtask(ObjectReferenceType subtaskRef, TaskType task, TaskResolver resolver) {
        String subTaskOid = subtaskRef != null ? subtaskRef.getOid() : null;
        if (subTaskOid == null) {
            return null;
        }
        TaskType inTask = TaskTreeUtil.findChildIfResolved(task, subTaskOid);
        if (inTask != null) {
            return inTask;
        }
        try {
            return resolver.resolve(subTaskOid);
        } catch (ObjectNotFoundException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve subtask {} of {}", e, subTaskOid, task);
            return null;
        }
    }

    private static @NotNull ActivityProgressInformation fromNotDelegatedActivityState(@NotNull ActivityStateType state,
            @NotNull ActivityPath activityPath, @NotNull TaskType task, @NotNull TaskResolver resolver) {
        String identifier = state.getIdentifier();
        RealizationState realizationState = getRealizationState(state);
        BucketsProgressInformation bucketsProgress = BucketsProgressInformation.fromActivityState(state);
        ItemsProgressInformation itemsProgress = getItemsProgress(state, activityPath, task, resolver);

        ActivityProgressInformation info
                = new ActivityProgressInformation(identifier, activityPath, realizationState, bucketsProgress, itemsProgress);
        for (ActivityStateType childState : state.getActivity()) {
            info.children.add(
                    fromDelegatableActivityState(childState, activityPath.append(childState.getIdentifier()), task, resolver));
        }
        return info;
    }

    private static ItemsProgressInformation getItemsProgress(@NotNull ActivityStateType state,
            @NotNull ActivityPath activityPath, @NotNull TaskType task, @NotNull TaskResolver resolver) {
        if (BucketingUtil.isCoordinator(state)) {
            return ItemsProgressInformation.fromBucketingCoordinator(state, activityPath, task, resolver);
        } else {
            return ItemsProgressInformation.fromActivityState(state);
        }
    }

    private static RealizationState getRealizationState(ActivityStateType state) {
        ActivityRealizationStateType rawState = state.getRealizationState();
        if (rawState == null) {
            return null;
        } else if (rawState == ActivityRealizationStateType.COMPLETE) {
            return RealizationState.COMPLETE;
        } else {
            return RealizationState.IN_PROGRESS;
        }
    }

    public String getActivityIdentifier() {
        return activityIdentifier;
    }

    public @NotNull ActivityPath getActivityPath() {
        return activityPath;
    }

    public RealizationState getRealizationState() {
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
                ", state=" + realizationState +
                ", bucketsProgress=" + bucketsProgress +
                ", totalItemsProgress=" + itemsProgress +
                ", children: " + children.size() +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        String title = String.format("%s for %s (identifier %s): %s", getClass().getSimpleName(), activityPath.toDebugName(),
                activityIdentifier, toHumanReadableString(false));
        StringBuilder sb = DebugUtil.createTitleStringBuilder(title, indent);
        sb.append("\n");
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
            return bucketsProgress.getCompletedBuckets() > 1;
        }
    }

    private String toHumanReadableStringForBucketed(boolean longForm) {
        float percentage = bucketsProgress.getPercentage();
        if (Float.isNaN(percentage)) {
            if (longForm) {
                return bucketsProgress.getCompletedBuckets() + " buckets";
            } else {
                return bucketsProgress.getCompletedBuckets() + " buckets"; // at least temporarily until we find something better
            }
        }
        if (longForm) {
            return String.format("%.1f%% (%d of %d buckets)", percentage * 100,
                    bucketsProgress.getCompletedBuckets(), bucketsProgress.getExpectedBuckets());
        } else {
            return String.format("%.1f%%", percentage * 100);
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

    private String toHumanReadableStringForNonLeaf(boolean longForm) {
        if (isComplete()) {
            return "Complete";
        }

        if (children.size() == 1) {
            return children.get(0).toHumanReadableString(longForm);
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

    private boolean isInProgress() {
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

    public ActivityProgressInformation getChild(String identifier) {
        return children.stream()
                .filter(c -> java.util.Objects.equals(c.getActivityIdentifier(), identifier))
                .findFirst().orElse(null);
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
         */
        UNKNOWN
    }
}
