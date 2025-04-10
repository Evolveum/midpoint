/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static com.evolveum.midpoint.schema.util.task.TaskTypeUtil.isActivityBasedPersistentSubtask;
import static com.evolveum.midpoint.schema.util.task.TaskTypeUtil.isActivityBasedRoot;

import java.io.Serializable;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Contains all non-trivial task information (progress, workers, overall status, and so on) needed for the use
 * of GUI and reporting.
 *
 * This is a kind of middle layer (or "API") that isolates higher layers (GUI, reporting) from the details
 * of data representation at the level of tasks and activities. It also hides the complexities induced by
 * the transition from tasks-based approach to activities-based one, namely the existence of legacy tasks
 * that live outside of the new activity framework.
 *
 * Instances of this class can be created in one of two ways:
 *
 * 1. For a root task (either activity-based or not),
 * 2. For any subtask in hierarchy (either persistent or not, activity-based or not).
 *
 * The task(s) provided here may have subtasks resolved or not.
 * If possible, we try to do our best to extract as much information as possible.
 * But currently we are quite limited. E.g. we ignore the subtasks that were loaded.
 */
public abstract class TaskInformation implements DebugDumpable, Serializable {

    /** The original task. */
    @NotNull protected final TaskType task;

    /**
     * Workers information covering this task and its subtasks.
     */
    @NotNull protected final ActivityWorkersInformation workersInformation;

    /**
     * Result status covering this task and its subtasks.
     */
    @NotNull protected final OperationResultStatusType overallStatus;

    protected TaskInformation(
            @NotNull TaskType task,
            @NotNull ActivityWorkersInformation workersInformation,
            @NotNull OperationResultStatusType overallStatus) {
        this.task = task;
        this.workersInformation = workersInformation;
        this.overallStatus = overallStatus;
    }

    public static @NotNull TaskInformation createForTask(@NotNull TaskType task, @Nullable TaskType rootTask) {
        if (task.getParent() == null) {
            if (isActivityBasedRoot(task)) {
                return ActivityBasedTaskInformation.fromActivityBasedRootTask(task);
            } else {
                return LegacyTaskInformation.fromLegacyTask(task);
            }
        } else {
            if (isActivityBasedPersistentSubtask(task) && rootTask != null) {
                return ActivityBasedTaskInformation.fromActivityBasedSubtask(task, rootTask);
            } else {
                return LegacyTaskInformation.fromLegacyTask(task); // root is not important here
            }
        }
    }

    /**
     * Computes the overall status that should be presented to a user.
     *
     * Normally it uses the activity tree state overview. A fallback is the task.resultStatus or UNKNOWN.
     */
    static @NotNull OperationResultStatusType computeStatus(
            @Nullable ActivityStateOverviewType stateOverview,
            @Nullable OperationResultStatusType fallbackStatus,
            @NotNull ActivityWorkersInformation workers) {

        if (stateOverview == null) {
            // This is the fallback. We have no specific activity-related information.
            return notNull(fallbackStatus);
        } else if (stateOverview.getRealizationState() == ActivitySimplifiedRealizationStateType.COMPLETE) {
            // We are done. The overall result status should be adequate.
            return notNull(stateOverview.getResultStatus());
        } else if (stateOverview.getRealizationState() == null) {
            // We have not started. We know nothing. (Maybe we should return null?)
            return OperationResultStatusType.UNKNOWN;
        } else if (workers.getWorkersExecuting() > 0) {
            // Some of the workers are executing. We report either IN_PROGRESS
            // (if everything goes well), or PARTIAL_ERROR (if there are some errors).
            return OperationResultStatusType.IN_PROGRESS;
        } else {
            return notNull(stateOverview.getResultStatus());
        }
    }

    static @NotNull OperationResultStatusType notNull(@Nullable OperationResultStatusType value) {
        return MoreObjects.firstNonNull(
                value, OperationResultStatusType.UNKNOWN);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "workers", workersInformation, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "overallStatus", overallStatus, indent + 1);
        return sb.toString();
    }

    public @NotNull TaskType getTask() {
        return task;
    }

    /** Returns short description of progress of the task and its children. */
    public String getProgressDescriptionShort() {
        return getProgressDescription(false);
    }

    public abstract String getProgressDescription(boolean longForm);

    /**
     * Returns the progress of the task based on activity tree state.
     *
     * Progress (%) is based on the number of completed activities to all activities.
     * Each activity represents the same amount of work.
     *
     * @return The progress is a number between 0 and 1. If the progress is unknown, the method returns -1.
     */
    public abstract double getProgress();

    public abstract boolean isComplete();

    /**
     * Returns number of items failed to be processed by the task and its children, if known.
     *
     * BEWARE: Fatal errors (e.g. resource not found for import task) are not counted here.
     */
    public abstract Integer getAllErrors();

    /** Returns textual form of information about nodes on which the task and its children execute. */
    public String getNodesDescription() {
        return workersInformation.toHumanReadableString();
    }

    /** Returns the "stalled since" information for the task and its children. */
    public @Nullable XMLGregorianCalendar getCompletelyStalledSince() {
        return workersInformation.getCompletelyStalledSince();
    }

    /** Returns the overall status of the task with its children that is displayable to the user. */
    public @NotNull OperationResultStatusType getResultStatus() {
        return overallStatus;
    }

    /**
     * Returns the "start timestamp", whatever that means. Currently, it is the execution start for trivial tasks
     * (a single non-bucketed activity, or a legacy task); and realization start otherwise.
     */
    public abstract XMLGregorianCalendar getStartTimestamp();

    /**
     * Returns the "end timestamp" with the analogous semantics to {@link #getStartTimestamp()}.
     */
    public abstract XMLGregorianCalendar getEndTimestamp();

    public abstract Object getLiveSyncToken();

    /**
     * Overall health status of the task and it's subtasks. Not related to items/objects processing,
     * rather just task execution (e.g. failed/stalled workers).
     */
    public abstract OperationResultStatusType getTaskHealthStatus();

    /**
     * Returns the message that describes primarily the overall status (health) of the task and it's subtasks.
     *
     * Description should not be based on errors created as a result of items/objects processing,
     * just state of tasks/subtasks and their execution.
     *
     * This should be a short message that is suitable for displaying to the user/table.
     */
    public abstract LocalizableMessage getTaskHealthDescription();

    /**
     * Return list of localizable messages that contains concrete errors/warnings that
     * are related to the task health (plus it's subtasks). Collected messages might repeat,
     * therefore might not be unique. It's up to client to decide whether to filter them.
     *
     * E.g. Policy violation error localizable messages.
     */
    public abstract List<LocalizableMessage> getTaskHealthUserFriendlyMessages();

    /**
     * Return list of technical messages that contains concrete errors/warnings that
     * are related to the task health (plus it's subtasks). Collected messages might repeat,
     * therefore might not be unique. It's up to client to decide whether to filter them.
     *
     * E.g. Policy violation error messages.
     */
    public abstract List<String> getTaskHealthMessages();

    public abstract @Nullable ActivityStatePersistenceType getRootActivityStatePersistence();

    public abstract TaskResultStatus getTaskUserFriendlyStatus();
}
